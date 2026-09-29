package com.bebopze.tdx.quant.task.script;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.util.PropsUtil;
import com.bebopze.tdx.quant.llm.CaptchaRecognizer;
import com.bebopze.tdx.quant.service.DataService;
import jakarta.annotation.PostConstruct;
import kong.unirest.core.*;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.task.script.EastMoneyChromeLogin.*;


/**
 * 东方财富 登录方案1   ->   Login API     -     https://github.com/wmo-v/eastmoneyapi/blob/master/client/client.go#L77
 *
 * @author: bebopze
 * @date: 2026/9/29
 */
@Slf4j
@Component
public class EastMoneyLogin {


    // 基础 URL
    private static final String BASE_URL = "https://jywg.18.cn";


    // 验证码 API
    private static final String CAPTCHA_API = "/Login/YZM";
    // 登录 API
    private static final String LOGIN_API = "/Login/Authentication";
    // 持仓 API
    private static final String POSITION_API = "/MarginSearch/queryCreditNewPosV1";


    // 买入页面 URL
    private static final String BUY_URL = "https://jywg.18.cn/MarginTrade/Buy";


    // 资金账号
    private static final String ACCOUNT = PropsUtil.getProperty("eastmoney.username");
    // 账号密码
    private static final String PASSWORD = PropsUtil.getProperty("eastmoney.password");


    // 东方财富 RSA 公钥
    private static final String PUB_PEM =
            """
                    MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDHdsyxT66pDG4p73yope7jxA92
                    c0AT4qIJ/xtbBcHkFPK77upnsfDTJiVEuQDH+MiMeb+XhCLNKZGp0yaUU6GlxZdp
                    +nLW8b7Kmijr3iepaDhcbVTsYBWchaWUXauj9Lrhz58/6AE/NF0aMolxIGpsi+ST
                    2hSHPu3GSXMdhPCkWQIDAQAB""";


    private static final PublicKey PUBLIC_KEY = initPublicKey();


    @Autowired
    private CaptchaRecognizer captchaRecognizer;

    @Autowired
    private DataService dataService;


    @TotalTime
    @PostConstruct
    public void login() {


        // 运行登录工作流
        HttpResponse<String> response = runLoginWorkflow();


        // 获取并刷新 validateKey 和 Cookie
        fetchRefreshValidateKeyAndCookie(response.getCookies());
    }


    private HttpResponse<String> runLoginWorkflow() {


        JSONObject result = null;
        for (int retry = 1; retry <= MAX_LOGIN_RETRY; retry++) {


            // 验证码 随机数
            double randNumber = randNumber();
            log.info("✅ 生成随机数，用于刷新 验证码图片     >>>     randNumber : {}", randNumber);


            // 刷新并下载 验证码图片
            Path captchaFile = refreshAndDownloadCaptcha(randNumber);

            // 识别验证码（大模型识别）
            String identifyCode = captchaRecognizer.recognizeCaptcha(captchaFile);
            log.info("✅ 大模型 验证码识别结果: {}", identifyCode);


            // 校验
            if (!validateCaptcha(identifyCode)) {
                log.warn("❌ 验证码未通过，请查看刷新后的验证码后重试（{}/" + MAX_LOGIN_RETRY + "）", retry + 1);
                continue;
            }


            // 登录接口
            HttpResponse<String> response = fillSubmitAndReadResult(ACCOUNT, PASSWORD, randNumber, identifyCode);
            result = parseLoginResponse(response.getStatus(), response.getBody());
            log.info("✅ 登录接口     >>>     result : {}", result);


            // suc
            if (result.getInteger("Status") == 0) {
                log.info("✅✅✅✅✅ ================ 登录成功");
                return response;
            }


            // 登录接口 response
            //
            //
            // {
            //  "Message": "",
            //  "Status": 0,
            //  "Errcode": 0,
            //  "Data": [
            //    {
            //      "khmc": "张三",
            //      "Date": "20260908",
            //      "Time": "015924",
            //      "Syspm1": "567000000001",
            //      "Syspm2": "5670",
            //      "Syspm3": "",
            //      "Syspm_ex": "4"
            //    }
            //  ]
            // }


            if (retry >= MAX_LOGIN_RETRY) {
                throw new IllegalStateException("❌ 证券登录失败: 验证信息连续 " + retry + " 次未通过，已停止重试");
            }


            log.error("❌ 证券登录失败     >>>     errMsg : {} ", result.getString("Message"));
            log.warn("❌ 验证信息未通过，请查看刷新后的验证码后重试（{}/" + MAX_LOGIN_RETRY + "）", retry + 1);
        }


        // fail
        throw new BizException("❌ 证券登录失败     >>>     errMsg : " + result.getString("Message"));
    }


    private HttpResponse<String> fillSubmitAndReadResult(String account,
                                                         String password,
                                                         double randNumber,
                                                         String identifyCode) {


        // 登录参数
        Map<String, Object> params = Map.of("userId", account,
                                            "password", encrypt(password),
                                            "randNumber", randNumber,
                                            "identifyCode", identifyCode,
                                            "duration", "1800",
                                            "authCode", "",
                                            "type", "Z",
                                            "secInfo", "");


        // 登录请求
        HttpResponse<String> response = Unirest.post(BASE_URL + LOGIN_API)
                                               .header("Content-Type", "application/x-www-form-urlencoded")
                                               .fields(params)
                                               .asString();


        log.info("url : {} , params : {} , response : {}", response.getRequestSummary().getUrl(), JSON.toJSONString(params), response.getBody());


        return response;
    }


    private void fetchRefreshValidateKeyAndCookie(Cookies _cookies) {


        // 从响应中获取 Cookie
        String cookies = _cookies.stream()
                                 .map(c -> c.getName() + "=" + c.getValue())
                                 .collect(Collectors.joining(";"));

        log.info("fetchRefreshValidateKeyAndCookie     >>>     cookies : {}", cookies);


        // -------------------------------------------------------------------------------------------------------------


        HttpResponse<String> response_1 = Unirest.get(BUY_URL)
                                                 .cookie(_cookies)
                                                 .asString();


        Document doc = Jsoup.parse(response_1.getBody());
        Element target = doc.selectFirst("#em_validatekey");

        if (target == null) {
            throw new RuntimeException("无法找到目标节点");
        }

        String validateKey = target.attr("value");
        if (validateKey.isEmpty()) {
            throw new RuntimeException("目标节点，没有value属性");
        }


        log.info("fetchRefreshValidateKeyAndCookie     >>>     validateKey : {}", validateKey);


        // -------------------------------------------------------------------------------------------------------------


        // save2DB
        dataService.eastmoneyRefreshSession(validateKey, cookies);
    }


    /**
     * 刷新并下载 验证码图片
     *
     * @param randNumber 随机数
     * @return
     */
    private static Path refreshAndDownloadCaptcha(double randNumber) {

        // 获取 验证码图片
        HttpResponse<byte[]> response = Unirest.get(BASE_URL + CAPTCHA_API + "?randNum=" + randNumber)
                                               .asBytes();

        // 保存 验证码图片
        return saveCaptchaImage(CAPTCHA_IMAGE_PATH, response.getStatus(), response.getBody());
    }


    /**
     * 对应 东方财富 js 的 encrypt ：RSA/ECB/PKCS1Padding + Base64
     */
    private static String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, PUBLIC_KEY);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("RSA 加密失败", e);
        }
    }

    /**
     * 初始化 RSA 公钥
     *
     * @return
     */
    private static PublicKey initPublicKey() {
        try {
            byte[] der = Base64.getDecoder().decode(PUB_PEM.replaceAll("\\s", ""));
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("解析 RSA 公钥失败", e);
        }
    }


    /**
     * 生成随机数   ->   [0, 1)
     *
     * @return
     */
    private static double randNumber() {
        // [0, 1)
        return ThreadLocalRandom.current().nextDouble();
    }


}