package com.bebopze.tdx.quant.common.util;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.domain.BaseExEnum;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;


/**
 * HttpClient工具 带可配置重试（指数退回 + 抖动）
 *
 *
 * - 默认对 GET 请求做重试（可通过重载修改次数/基数）
 * - POST 默认不重试（提供 doPostWithRetry 可选）
 * - 保留原先的 cookie 过期判定逻辑
 *
 * 注意：并发场景（例如每 500ms 拉 100 只），不建议把重试次数设得过高，以免请求堆积。
 */
@Slf4j
public class HttpUtil {


    // ---------- retry 默认配置 ----------
    private static final int DEFAULT_MAX_RETRY = 3;          // 包含第一次尝试
    private static final long DEFAULT_RETRY_SLEEP_MS = 1000; // base 毫秒


    // 默认超时：连接 5s，读取 20s
    private static final RequestConfig DEFAULT_CONFIG = RequestConfig.custom()
                                                                     .setConnectTimeout(5000)
                                                                     .setSocketTimeout(20000)
                                                                     .build();

    private static final CloseableHttpClient CLIENT = HttpClients.custom()
                                                                 .setDefaultRequestConfig(DEFAULT_CONFIG)
                                                                 .build();


    // ---------- 计算退避时间（指数退避 + 抖动） ----------
    private static long calcBackoffMillis(int attempt, long baseMs) {
        // attempt 从 1 开始；退避 = base * 2^(attempt-1) + random(0, base)
        // 防止位移溢出：限制最大指数
        int exp = Math.max(0, Math.min(30, attempt - 1));
        long expo = baseMs * (1L << exp);
        long jitter = ThreadLocalRandom.current().nextLong(0, baseMs + 1L);
        return expo + jitter;
    }


    // --------------------- doGet（默认） ---------------------

    @SneakyThrows
    public static String doGet(String url) {
        return doGet(url, Maps.newHashMap(), DEFAULT_MAX_RETRY, DEFAULT_RETRY_SLEEP_MS);
    }

    @SneakyThrows
    public static String doGet(String url, Map<String, String> headers) {
        return doGet(url, headers, DEFAULT_MAX_RETRY, DEFAULT_RETRY_SLEEP_MS);
    }

    /**
     * 带重试参数的 doGet 实现
     *
     * @param url         完整 URL
     * @param headers     可选 headers
     * @param maxRetries  最大尝试次数（包含第一次）
     * @param baseSleepMs 退避基数（毫秒）
     * @return 响应字符串
     */
    @SneakyThrows
    public static String doGet(String url, Map<String, String> headers, int maxRetries, long baseSleepMs) {

        int attempt = 0;

        while (true) {
            attempt++;


            int[] status = {0};
            ResponseHandler<String> handler = response -> {
                status[0] = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                return entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8.name()) : null;
            };

            String result = "";
            try {
                HttpGet httpGet = new HttpGet(url);
                httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36");

                if (MapUtils.isNotEmpty(headers)) {
                    headers.forEach(httpGet::addHeader);
                }

                result = CLIENT.execute(httpGet, handler);
                log.info("status : {}", status[0]);

                if (result != null && result.contains("<!DOCTYPE HTML PUBLIC")) {
                    log.error("doGet     cookie过期，请重新登录！   >>>     url : {}, result : {}", url, result);
                    throw new BizException(BaseExEnum.TREAD_EM_COOKIE_EXPIRED);
                }

                log.info("doGet     >>>     url : {} , result length : {}", url, result == null ? 0 : result.length());
                return result;

            } catch (Exception ex) {

                retryCheck(ex, url, maxRetries, baseSleepMs, attempt);
            }
        }
    }


    // --------------------- doGet2（保留原来的 header） ---------------------


    @SneakyThrows
    public static String doGet2(String url, Map<String, String> headers) {
        return doGet2(url, headers, DEFAULT_MAX_RETRY, DEFAULT_RETRY_SLEEP_MS);
    }


    @SneakyThrows
    public static String doGet2(String url, Map<String, String> headers, int maxRetries, long baseSleepMs) {

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                HttpGet httpGet = new HttpGet(url);

                httpGet.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
                httpGet.setHeader("Accept-Encoding", "gzip");
                httpGet.setHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,zh-TW;q=0.7");
                httpGet.setHeader("Cache-Control", "max-age=0");
                httpGet.setHeader("Connection", "keep-alive");
                httpGet.setHeader("Host", "push2.eastmoney.com");
                httpGet.setHeader("Sec-Fetch-Dest", "document");
                httpGet.setHeader("Sec-Fetch-Mode", "navigate");
                httpGet.setHeader("Sec-Fetch-Site", "none");
                httpGet.setHeader("Sec-Fetch-User", "?1");
                httpGet.setHeader("Upgrade-Insecure-Requests", "1");
                httpGet.setHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36");
                httpGet.setHeader("sec-ch-ua", "\"Chromium\";v=\"142\", \"Google Chrome\";v=\"142\", \"Not_A Brand\";v=\"99\"");
                httpGet.setHeader("sec-ch-ua-mobile", "?0");
                httpGet.setHeader("sec-ch-ua-platform", "\"macOS\"");

                // 你的 cookie 读取方法
                httpGet.setHeader("Cookie", PropsUtil.getCookie2());

                if (MapUtils.isNotEmpty(headers)) {
                    headers.forEach(httpGet::setHeader);
                }

                int[] status = {0};
                ResponseHandler<String> handler = response -> {
                    status[0] = response.getStatusLine().getStatusCode();
                    HttpEntity entity = response.getEntity();
                    return entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8.name()) : null;
                };

                String result = CLIENT.execute(httpGet, handler);
                log.info("status : {}", status[0]);

                if (result != null && result.contains("<!DOCTYPE HTML PUBLIC")) {
                    log.error("doGet2     cookie过期，请重新登录！   >>>     url : {}, result : {}", url, result);
                    throw new BizException(BaseExEnum.TREAD_EM_COOKIE_EXPIRED);
                }
                log.info("doGet2     >>>     url : {} , result length : {}", url, result == null ? 0 : result.length());
                return result;

            } catch (Exception ex) {
                retryCheck(ex, url, maxRetries, baseSleepMs, attempt);
            }
        }
    }


    // --------------------- doPost（默认不重试） ---------------------


    @SneakyThrows
    public static String doPost(String url, Map<String, Object> formData, Map<String, String> headers) {

        HttpPost httpPost = new HttpPost(url);

        // header
        httpPost.addHeader("Content-Type", ContentType.APPLICATION_FORM_URLENCODED.getMimeType());
        httpPost.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36");

        if (MapUtils.isNotEmpty(headers)) {
            headers.forEach(httpPost::addHeader);
        }

        // form-data
        List<NameValuePair> form = Lists.newArrayList();
        if (MapUtils.isNotEmpty(formData)) {
            formData.forEach((k, v) -> {
                form.add(new BasicNameValuePair(k, String.valueOf(v)));
            });
        }
        httpPost.setEntity(new UrlEncodedFormEntity(form, StandardCharsets.UTF_8.name()));


        try (CloseableHttpResponse response = CLIENT.execute(httpPost)) {
            HttpEntity entity = response.getEntity();

            int statusCode = response.getStatusLine().getStatusCode();
            log.info("doPost     >>>     statusCode : {}", statusCode);

            String result = entity != null ? EntityUtils.toString(entity, StandardCharsets.UTF_8.name()) : "";

            // 你的原有逻辑：检测登录超时或错误
            if (result.contains("该登录已超时") || result.contains("字段不能为空") || result.contains("<h2>Object moved to <a href=\"/LogIn/ExitLogin?returl=")) {
                log.error("doPost     cookie过期，请重新登录！   >>>     url : {} , formData : {}, result : {}", url, JSON.toJSONString(formData), result);
                throw new BizException(BaseExEnum.TREAD_EM_COOKIE_EXPIRED);
            } else {
                log.info("doPost     >>>     url : {} , formData : {}, result length : {}", url, JSON.toJSONString(formData), result.length());
            }

            return result;
        }
    }


    // --------------------- 可选：对 POST 做重试（仅在幂等场景使用） ---------------------


    @SneakyThrows
    public static String doPostWithRetry(String url,
                                         Map<String, Object> formData,
                                         Map<String, String> headers,
                                         int maxRetries,
                                         long baseSleepMs) {

        int attempt = 0;
        while (true) {
            attempt++;

            try {
                return doPost(url, formData, headers);
            } catch (Exception ex) {
                retryCheck(ex, url, maxRetries, baseSleepMs, attempt);
            }
        }
    }


    /**
     * 重试检查
     *
     * @param ex
     * @param url
     * @param maxRetries
     * @param baseSleepMs
     * @param attempt
     * @throws Exception
     */
    private static void retryCheck(Exception ex,
                                   String url,
                                   int maxRetries,
                                   long baseSleepMs,
                                   int attempt) throws Exception {

        // 业务异常不做重试
        if (ex instanceof BizException) {
            throw ex;
        }


        boolean isRetryable = ex instanceof IOException;
        if (!isRetryable) {
            log.error("retry - non-retryable exception, url: {} , err: {}", url, ex.getMessage(), ex);
            throw ex;
        }

        if (attempt >= maxRetries) {
            log.error("retry - reached maxRetries={} for url: {}. lastErr: {}", maxRetries, url, ex.getMessage(), ex);
            throw ex;
        }


        long sleepMs = calcBackoffMillis(attempt, baseSleepMs);
        log.warn("retry - attempt {}/{} failed for url: {} , err: {} -> sleep {} ms then retry", attempt, maxRetries, url, ex.getMessage(), sleepMs);
        SleepUtils.sleep(sleepMs);
    }


}