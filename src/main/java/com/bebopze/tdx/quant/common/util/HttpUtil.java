package com.bebopze.tdx.quant.common.util;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.domain.BaseExEnum;
import com.google.common.collect.Lists;
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

import java.util.List;
import java.util.Map;


/**
 * HttpClient
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
@Slf4j
public class HttpUtil {


    // 默认超时：连接 5s，读取 10s
    private static final RequestConfig DEFAULT_CONFIG = RequestConfig.custom().setConnectTimeout(5000).setSocketTimeout(10000).build();

    private static final CloseableHttpClient CLIENT = HttpClients.custom().setDefaultRequestConfig(DEFAULT_CONFIG).build();


    /**
     * 发送 GET 请求
     *
     * @param url     完整请求 URL
     * @param headers 可选请求头
     * @return 响应体字符串
     */
    @SneakyThrows
    public static String doGet(String url, Map<String, String> headers) {

        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/135.0.0.0 Safari/537.36");

        if (MapUtils.isNotEmpty(headers)) {
            headers.forEach(httpGet::addHeader);
        }

        int[] status = {0};
        ResponseHandler<String> handler = response -> {
            status[0] = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            return entity != null ? EntityUtils.toString(entity, "UTF-8") : null;
        };


        String result = CLIENT.execute(httpGet, handler);

        log.info("status : {}", status[0]);

        if (result.contains("<!DOCTYPE HTML PUBLIC")) {
            log.error("doGet     cookie过期，请重新登录！   >>>     url : {}, result : {}", url, JSON.toJSONString(result));
            throw new BizException(BaseExEnum.TREAD_EM_COOKIE_EXPIRED);
        } else {
            log.info("doGet     >>>     url : {} , result : {}", url, JSON.toJSONString(result));
        }

        return result;
    }


    /**
     * 发送 POST 请求，默认 Content-Type 为 application/x-www-form-urlencoded
     *
     * @param url      完整请求 URL
     * @param formData 请求体字符串（JSON  或  表单格式）
     * @param headers  可选请求头
     * @return 响应体字符串
     */
    @SneakyThrows
    public static String doPost(String url,
                                Map<String, Object> formData,
                                Map<String, String> headers) {


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
        httpPost.setEntity(new UrlEncodedFormEntity(form, "UTF-8"));


        try (CloseableHttpResponse response = CLIENT.execute(httpPost)) {
            HttpEntity entity = response.getEntity();


            int statusCode = response.getStatusLine().getStatusCode();
            log.info("doPost     >>>     statusCode : {}", statusCode);

            String result = entity != null ? EntityUtils.toString(entity, "UTF-8") : "";


            if (result.contains("<h2>Object moved to <a href=\"/LogIn/ExitLogin?returl=")) {
                log.error("doPost     cookie过期，请重新登录！   >>>     url : {} , formData : {}, result : {}", url, JSON.toJSONString(formData), JSON.toJSONString(result));
                throw new BizException(BaseExEnum.TREAD_EM_COOKIE_EXPIRED);
            } else {
                log.info("doPost     >>>     url : {} , formData : {}, result : {}", url, JSON.toJSONString(formData), JSON.toJSONString(result));
            }


            return result;
        }
    }

}
