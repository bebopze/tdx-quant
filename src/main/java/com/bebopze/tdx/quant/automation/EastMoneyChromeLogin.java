package com.bebopze.tdx.quant.automation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.util.SleepUtils;
import com.bebopze.tdx.quant.llm.CaptchaRecognizer;
import com.bebopze.tdx.quant.common.util.PropsUtil;
import com.bebopze.tdx.quant.service.DataService;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.stream.Collectors;


/**
 * 使用本机 Chrome 完成东方财富证券网页登录（LLM自动识别 图形验证码）
 *
 * @author: bebopze
 * @date: 2026/8/23
 */
@Slf4j
@Component
public final class EastMoneyChromeLogin {


    // 登录页面 URL
    private static final String LOGIN_URL = "https://jywg.18.cn/Login";
    // 买入页面 URL
    private static final String BUY_URL = "https://jywg.18.cn/MarginTrade/Buy";


    // 验证码 API
    private static final String CAPTCHA_API = "/Login/YZM";
    // 登录 API
    private static final String LOGIN_API = "/Login/Authentication";
    // 持仓 API
    private static final String POSITION_API = "/MarginSearch/queryCreditNewPosV1";


    // 验证码图片 保存路径
    private static final Path CAPTCHA_IMAGE_PATH = Path.of("tdx_zip", "验证码.png");


    // 登录页面 表单选择器（资金账号、密码、验证码、在线时间、登录按钮、系统公告）
    private static final String ACCOUNT_SELECTOR = "#txtZjzh";
    private static final String PASSWORD_SELECTOR = "#txtPwd";
    private static final String CAPTCHA_SELECTOR = "#txtValidCode";
    private static final String CAPTCHA_IMAGE_SELECTOR = "#imgValidCode";
    private static final String ONLINE_3_HOURS_SELECTOR = "#rdsc45";
    private static final String LOGIN_BUTTON_SELECTOR = "#btnConfirm";
    private static final String NOTICE_SELECTOR = ".popup-component-body";
    private static final String NOTICE_CONFIRM_SELECTOR = ".vbtn-confirm";


    /**
     * 重试次数
     */
    private static final int MAX_LOGIN_RETRY = 5;
    private static final double NOTICE_WAIT_MILLIS = 3_000;


    @Value("${eastmoney.username}")
    private /*static final*/ String ACCOUNT; // = PropsUtil.getProperty("eastmoney.username");

    @Value("${eastmoney.password}")
    private /*static final*/ String PASSWORD; // = PropsUtil.getProperty("eastmoney.password");


    @Autowired
    private CaptchaRecognizer captchaRecognizer;

    @Autowired
    private DataService dataService;


    public void runLoginWorkflow() {

        try (Playwright playwright = Playwright.create()) {


            // 启动 Chrome 浏览器（访客模式）
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                                                                   .setChannel("chrome")
                                                                   .setHeadless(false));


            runLoginWorkflow(browser.newPage(), ACCOUNT, PASSWORD);
        }
    }


    void runLoginWorkflow(Page page, String account, String password) {


        // 设置超时时间
        page.setDefaultTimeout(10_000);
        // 设置页面导航超时时间
        page.setDefaultNavigationTimeout(15_000);
        // 导航到 登录页面
        Response response = page.navigate(LOGIN_URL);
        log.info("✅ [登录页面 {}] 加载完成，HTTP状态码 : {}", page.url(), response.status());


        // 关闭 公告弹框
        closeNoticeWindowIfPresent(page);


        JSONObject result = null;
        for (int retry = 1; retry <= MAX_LOGIN_RETRY; retry++) {

            // 刷新并下载 验证码图片
            Path captchaFile = refreshAndDownloadCaptcha(page);

            // 识别验证码（手动识别/大模型识别）
            // String captcha = inputCaptchaCode(captchaFile);
            String captcha = captchaRecognizer.recognizeCaptcha(captchaFile);
            log.info("✅ 大模型 验证码识别结果: {}", captcha);


            // 校验
            // validateCaptcha(captcha);


            // 填写控件、选择3小时在线时间，并等待登录接口响应
            result = fillSubmitAndReadResult(page, account, password, captcha);
            log.info("✅ 登录接口   >>>   result : {}", result);


            // suc
            if (result.getInteger("Status") == 0) {
                log.info("✅✅✅✅✅ ================ 登录成功");
                break;
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


            log.error("❌ 证券登录失败   >>>   errMsg : {} ", result.getString("Message"));
            log.info("❌ 验证信息未通过，请查看刷新后的验证码后重试（{}/" + MAX_LOGIN_RETRY + "）", retry + 1);
        }


        // suc
        if (result.getInteger("Status") != 0) {
            throw new IllegalStateException("❌ 证券登录失败   >>>   errMsg : " + result.getString("Message"));
        }


        // 刷新 Cookie
        refreshCookie(page);


        // 停留10秒方便查看效果，生产环境可删除
        // SleepUtils.winSleep(10_000);
    }

    private void refreshCookie(Page page) {


        // 打开信用买入页，抓取 持仓API   ->   解析 validatekey参数 和 cookie
        Response response = openBuyPageAndFetchPositions(page);
        log.info("✅ [信用买入页面 {}] 加载完成，HTTP状态码 : {}", page.url(), response.status());


        // validatekey
        String url = response.request().url();
        log.info("持仓API接口 : {}", url);

        String validatekey = url.split("validatekey=")[1];
        log.info("validatekey : {}", validatekey);


        // cookie
        String cookies = parseAndRefreshCookie(response);


        // save2DB
        dataService.eastmoneyRefreshSession(validatekey, cookies);
    }


    /**
     * 关闭 公告弹框
     *
     * 1、读取并关闭 系统公告 弹窗（系统维护 通知弹窗）
     * 2、无 系统公告时，忽略
     */
    static void closeNoticeWindowIfPresent(Page page) {
        Locator notice = page.locator(NOTICE_SELECTOR).first();
        try {
            notice.waitFor(new Locator.WaitForOptions()
                                   .setState(WaitForSelectorState.VISIBLE)
                                   .setTimeout(NOTICE_WAIT_MILLIS));
        } catch (RuntimeException noNotice) {
            return;
        }

        String message = notice.innerText().trim();
        log.info("检测到系统公告：{}", message);
        page.locator(NOTICE_CONFIRM_SELECTOR).first().click();
    }


    /**
     * 填写控件、选择3小时在线时间，并等待登录接口响应
     */
    static JSONObject fillSubmitAndReadResult(Page page, String account, String password, String captcha) {
        SleepUtils.randomSleep(100, 1_000);


        // 填写账户
        page.locator(ACCOUNT_SELECTOR).fill(account.trim());
        // 填写密码
        page.locator(PASSWORD_SELECTOR).fill(password);
        // 填写验证码
        page.locator(CAPTCHA_SELECTOR).fill(captcha.trim().toUpperCase(Locale.ROOT));
        // 选择3小时在线时间
        page.locator(ONLINE_3_HOURS_SELECTOR).check();


        log.info("✅ 资金账号、密码、验证码、3小时在线   输入完成");


        // 点击登录按钮，等待登录接口响应
        Response response = page.waitForResponse(
                candidate -> candidate.url().contains(LOGIN_API),
                () -> page.locator(LOGIN_BUTTON_SELECTOR).click());

        log.info("✅ 登录按钮已点击，等待登录接口响应...");


        return parseLoginResponse(response.status(), response.text());
    }


    /**
     * 刷新并下载 验证码图片
     *
     * @param page
     * @return
     */
    static Path refreshAndDownloadCaptcha(Page page) {

        // 点击刷新，等待验证码接口响应（返回：验证码图片）
        Response response = page.waitForResponse(
                candidate -> candidate.url().contains(CAPTCHA_API),
                () -> page.locator(CAPTCHA_IMAGE_SELECTOR).click());

        // 保存 验证码图片
        return saveCaptchaImage(CAPTCHA_IMAGE_PATH, response.status(), response.body());
    }

    /**
     * 保存 验证码图片
     *
     * @param imagePath  验证码图片 保存路径
     * @param httpStatus 验证码接口 响应状态码
     * @param imageBytes 验证码图片 字节数组
     * @return
     */
    static Path saveCaptchaImage(Path imagePath, int httpStatus, byte[] imageBytes) {
        if (httpStatus < 200 || httpStatus >= 300) {
            throw new IllegalStateException("验证码接口请求失败，HTTP " + httpStatus);
        }
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalStateException("验证码接口返回了空图片");
        }

        Path normalized = imagePath.toAbsolutePath().normalize();
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new IllegalStateException("验证码接口返回内容不是有效图片");
            }
            Files.createDirectories(normalized.getParent());
            if (!ImageIO.write(image, "png", normalized.toFile())) {
                throw new IllegalStateException("当前 JDK 不支持写入 PNG 图片");
            }
            System.out.println("验证码图片已保存：" + normalized);
            return normalized;
        } catch (IOException exception) {
            throw new IllegalStateException("保存验证码图片失败: " + normalized, exception);
        }
    }


    /**
     * 登录成功后 打开信用买入页，抓取 持仓API   ->   解析 validatekey参数 和 cookie
     */
    static Response openBuyPageAndFetchPositions(Page page) {
        // 等待页面加载完成
        SleepUtils.sleep(500);


        Response response = page.waitForResponse(
                candidate -> candidate.url().contains(POSITION_API),
                () -> page.navigate(BUY_URL));


        if (!response.ok()) {
            throw new IllegalStateException("持仓接口请求失败，HTTP " + response.status());
        }
        if (!hasNonBlankQueryParameter(response.url(), "validatekey")) {
            throw new IllegalStateException("持仓接口请求缺少认证参数");
        }

        return response;
    }


    /**
     * 解析并刷新 Cookie
     */
    private static String parseAndRefreshCookie(Response response) {

        BrowserContext context = response.frame().page().context();
        String cookies = context.cookies(response.url())
                                .stream()
                                .map(cookie -> cookie.name + "=" + cookie.value)
                                .collect(Collectors.joining(";"));


        log.info("cookies : {}", cookies);
        return cookies;
    }


    static JSONObject parseLoginResponse(int httpStatus, String responseBody) {
        if (httpStatus < 200 || httpStatus >= 300) {
            throw new IllegalStateException("登录接口请求失败，HTTP " + httpStatus);
        }

        try {
            JSONObject json = JSON.parseObject(responseBody);
            log.info("登录接口返回   >>>   resp: {}", responseBody);
            if (json == null) {
                throw new IllegalStateException("登录接口返回内容为空");
            }

            return json;

        } catch (JSONException exception) {
            throw new IllegalStateException("登录接口返回内容不是合法 JSON", exception);
        }
    }


    static boolean hasNonBlankQueryParameter(String url, String parameterName) {
        try {
            String rawQuery = URI.create(url).getRawQuery();
            if (rawQuery == null || rawQuery.isBlank()) {
                return false;
            }
            for (String pair : rawQuery.split("&")) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2 && parameterName.equals(keyValue[0])
                        && !keyValue[1].isBlank()) {
                    return true;
                }
            }
            return false;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }


    /**
     * 手动输入 验证码
     */
    public static String inputCaptchaCode(Path captchaFile) {
        try {
            log.info("请查看图片，[手动输入] 4位数字验证码：");
            String captchaCode = new BufferedReader(new InputStreamReader(System.in)).readLine();
            return captchaCode;
        } catch (IOException exception) {
            throw new IllegalStateException("输入验证码失败", exception);
        }
    }

    /**
     * 校验 验证码（4位纯数字）
     */
    private static void validateCaptcha(String captcha) {
        if (captcha == null || !captcha.trim().matches("[0-9]{4}")) {
            throw new IllegalArgumentException("验证码必须为 4 位纯数字");
        }
    }


}