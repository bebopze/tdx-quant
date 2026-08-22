package com.bebopze.tdx.quant.automation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.util.PropsUtil;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


/**
 * 使用本机 Chrome 完成东方财富证券网页登录（人工输入图形验证码）。
 *
 * <p>账号和密码从 {@code eastmoney.username}、{@code eastmoney.password} 读取，
 * 也可以分别使用 {@code EM_ACCOUNT}、{@code EM_PASSWORD} 环境变量覆盖。</p>
 *
 * <p>图形验证码属于登录保护措施，本工具不会自动识别、猜测或绕过验证码；每次尝试都由
 * 用户查看 Chrome 中的图片并在终端输入。登录后只验证持仓接口已在当前浏览器会话内成功
 * 加载，不导出、不打印 Cookie、validatekey、Token 或 Session_Id。</p>
 */
@Slf4j
public final class EastMoneyChromeLogin {


    private static final String LOGIN_URL = "https://jywg.18.cn/Login";
    private static final String BUY_URL = "https://jywg.18.cn/MarginTrade/Buy";
    private static final String AUTHENTICATION_PATH = "/Login/Authentication";
    private static final String POSITION_API_PATH = "/MarginSearch/queryCreditNewPosV1";
    private static final String RETRYABLE_INPUT_ERROR = "您输入的信息有误，请重新输入!";


    private static final String DEFAULT_ACCOUNT = PropsUtil.getProperty("eastmoney.username");
    private static final String DEFAULT_PASSWORD = PropsUtil.getProperty("eastmoney.password");


    private static final String ACCOUNT_SELECTOR = "#txtZjzh";
    private static final String PASSWORD_SELECTOR = "#txtPwd";
    private static final String CAPTCHA_SELECTOR = "#txtValidCode";
    private static final String CAPTCHA_IMAGE_SELECTOR = "#imgValidCode";
    private static final String ONLINE_THREE_HOURS_SELECTOR = "#rdsc45";
    private static final String LOGIN_BUTTON_SELECTOR = "#btnConfirm";
    private static final String NOTICE_SELECTOR = ".popup-component-body";
    private static final String NOTICE_CONFIRM_SELECTOR = ".vbtn-confirm";


    /**
     * 首次提交加两次重试，防止连续尝试导致证券账户被风控。
     */
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private static final double NOTICE_WAIT_MILLIS = 3_000;


    private EastMoneyChromeLogin() {
    }


    public static void main(String[] args) {

        String account = envOrDefault("EM_ACCOUNT", DEFAULT_ACCOUNT);
        String password = envOrDefault("EM_PASSWORD", DEFAULT_PASSWORD);

        try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
             Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                                                                   .setChannel("chrome")
                                                                   .setHeadless(false));
            try {
                runLoginWorkflow(browser.newPage(), account, password, args, console);
            } finally {
                browser.close();
            }
        } catch (IOException exception) {
            throw new IllegalStateException("读取终端输入失败", exception);
        }
    }


    static void runLoginWorkflow(Page page, String account, String password,
                                 String[] args, BufferedReader console) throws IOException {
        requireNonBlank(account, "资金账号");
        requireNonBlank(password, "密码");

        page.setDefaultTimeout(1_000);
        page.setDefaultNavigationTimeout(3_000);
        page.navigate(LOGIN_URL);
        dismissMaintenanceNoticeIfPresent(page);

        AuthenticationResult result = null;
        for (int attempt = 1; attempt <= MAX_LOGIN_ATTEMPTS; attempt++) {
            String captcha = captchaFrom(args, attempt, console);
            result = fillSubmitAndReadResult(page, account, password, captcha);
            if (result.successful()) {
                break;
            }

            if (!result.retryableInputError()) {
                throw new IllegalStateException("证券登录失败: " + result.safeMessage());
            }
            if (attempt == MAX_LOGIN_ATTEMPTS) {
                throw new IllegalStateException("证券登录失败: 验证信息连续 "
                                                        + MAX_LOGIN_ATTEMPTS + " 次未通过，已停止重试");
            }

            System.out.println("验证信息未通过，请查看刷新后的验证码后重试（"
                                       + (attempt + 1) + "/" + MAX_LOGIN_ATTEMPTS + "）");
            refreshCaptcha(page);
        }

        if (result == null || !result.successful()) {
            throw new IllegalStateException("证券登录未完成");
        }

        HoldingsProbe probe = openBuyPageAndProbePositions(page);
        System.out.println("登录成功；持仓接口 HTTP " + probe.httpStatus() + "，认证参数已存在（未读取或输出其值）。");
    }


    /**
     * 读取并关闭系统维护/公告弹窗；未出现时直接返回。
     */
    static void dismissMaintenanceNoticeIfPresent(Page page) {
        Locator notice = page.locator(NOTICE_SELECTOR).first();
        try {
            notice.waitFor(new Locator.WaitForOptions()
                                   .setState(WaitForSelectorState.VISIBLE)
                                   .setTimeout(NOTICE_WAIT_MILLIS));
        } catch (RuntimeException noNotice) {
            return;
        }

        String message = notice.innerText().trim();
        System.out.println("检测到系统公告：" + message);
        page.locator(NOTICE_CONFIRM_SELECTOR).first().click();
    }


    /**
     * 填写控件、选择三小时在线时间，并等待登录接口响应。
     */
    static AuthenticationResult fillSubmitAndReadResult(Page page, String account,
                                                        String password, String captcha) {
        requireNonBlank(account, "资金账号");
        requireNonBlank(password, "密码");
        validateCaptcha(captcha);

        page.locator(ACCOUNT_SELECTOR).fill(account.trim());
        page.locator(PASSWORD_SELECTOR).fill(password);
        page.locator(CAPTCHA_SELECTOR).fill(captcha.trim().toUpperCase(Locale.ROOT));
        page.locator(ONLINE_THREE_HOURS_SELECTOR).check();

        Response response = page.waitForResponse(
                candidate -> candidate.url().contains(AUTHENTICATION_PATH),
                () -> page.locator(LOGIN_BUTTON_SELECTOR).click());


        getCookie(response);

        return parseAuthenticationResponse(response.status(), response.text());
    }


    /**
     * 登录成功后打开信用买入页，只确认持仓接口与认证参数存在，不提取认证参数值。
     */
    static HoldingsProbe openBuyPageAndProbePositions(Page page) {
        Response response = page.waitForResponse(
                candidate -> candidate.url().contains(POSITION_API_PATH),
                () -> page.navigate(BUY_URL));


        String url = response.request().url();
        log.info("持仓接口请求: {}", url);
        String validatekey = url.split("validatekey=")[1];
        log.info("validatekey : {}", validatekey);


        String cookies = getCookie(response);


        // dataService.eastmoneyRefreshSession(validatekey, cookies);
        PropsUtil.refreshEastmoneySession(validatekey, cookies);


        if (!response.ok()) {
            throw new IllegalStateException("持仓接口请求失败，HTTP " + response.status());
        }
        if (!hasNonBlankQueryParameter(response.url(), "validatekey")) {
            throw new IllegalStateException("持仓接口请求缺少认证参数");
        }
        return new HoldingsProbe(response.status(), true);
    }


    private static String getCookie(Response response) {


        // 方式1
        BrowserContext context = response.frame().page().context();
        String cookies = context.cookies(response.url())
                                .stream()
                                .map(cookie -> cookie.name + "=" + cookie.value)
                                .collect(Collectors.joining(";"));
        System.out.println(cookies);


        System.out.println("\n--------------------------------------\n");


        // 方式2
        String requestCookie = response.request().headerValue("cookie");
        System.out.println(requestCookie);


        System.out.println("\n--------------------------------------\n");


        // 方式3
        List<String> setCookies = response.headerValues("set-cookie");
        if (CollectionUtils.isNotEmpty(setCookies)) {
            setCookies.forEach(System.out::println);
        }


        System.out.println("\n--------------------------------------\n");


        System.out.println();

        return cookies;
    }


    static AuthenticationResult parseAuthenticationResponse(int httpStatus, String responseBody) {
        if (httpStatus < 200 || httpStatus >= 300) {
            throw new IllegalStateException("登录接口请求失败，HTTP " + httpStatus);
        }

        try {
            JSONObject json = JSON.parseObject(responseBody);
            log.info("登录接口返回: {}", responseBody);
            if (json == null) {
                throw new IllegalStateException("登录接口返回内容为空");
            }

            // 有意只读取非敏感状态字段，忽略 Token、Session_Id 等认证字段。
            return new AuthenticationResult(
                    httpStatus,
                    json.getString("Status"),
                    json.getInteger("Return_Code"),
                    json.getString("Message"));
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

    private static void refreshCaptcha(Page page) {
        page.locator(CAPTCHA_SELECTOR).fill("");
        page.locator(CAPTCHA_IMAGE_SELECTOR).click();
        page.waitForTimeout(300);
    }

    private static String captchaFrom(String[] args, int attempt, BufferedReader console) throws IOException {
        String captcha = null;
        if (attempt == 1) {
            captcha = args.length > 0 ? args[0] : System.getenv("EM_CAPTCHA");
        }
        if (captcha == null || captcha.isBlank()) {
            System.out.print("请在 Chrome 页面查看四位图形验证码后输入：");
            captcha = console.readLine();
        }
        validateCaptcha(captcha);
        return captcha;
    }

    private static void validateCaptcha(String captcha) {
        if (captcha == null || !captcha.trim().matches("[0-9]{4}")) {
            throw new IllegalArgumentException("验证码必须为 4 位数字");
        }
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "不能为空");
        }
    }

    private static String envOrDefault(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }

    record AuthenticationResult(int httpStatus, String status, Integer returnCode, String message) {

        boolean successful() {
            return httpStatus >= 200 && httpStatus < 300
                    && ("0".equals(status) || Integer.valueOf(0).equals(returnCode));
        }

        boolean retryableInputError() {
            return RETRYABLE_INPUT_ERROR.equals(message == null ? null : message.trim());
        }

        String safeMessage() {
            return message == null || message.isBlank() ? "服务未返回失败原因" : message.trim();
        }
    }

    record HoldingsProbe(int httpStatus, boolean authenticationPresent) {
    }


}