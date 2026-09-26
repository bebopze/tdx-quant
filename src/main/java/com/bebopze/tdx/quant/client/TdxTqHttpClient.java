package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.PropsUtil;
import kong.unirest.core.JsonNode;
import kong.unirest.core.Unirest;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 通达信原生 tqcenter HTTP JSON-RPC 客户端
 *
 *
 * 通达信量化平台   -   https://help.tdx.com.cn/quant/docs/markdown/mindoc-1hdhbmi50d038.html
 *
 *
 * 该接口使用通达信自定义的 {@code id/method/params} 信封，不是标准 MCP {@code tools/list} / {@code tools/call} 协议
 *
 * @author bebopze
 * @date 2026/9/26
 */
@Slf4j
public class TdxTqHttpClient {


    // 通达信 TQ 基础URL
    private static final String TQ_BASE_URL = PropsUtil.getProperty("tdx.tq.base-url");


    /**
     * 请求 ID 计数器
     */
    private static final AtomicLong requestId = new AtomicLong();


    /**
     * 调用 TQ 方法并返回 result，业务错误会直接抛出
     */
    public static JSONObject call(String method, Map<String, Object> params) {
        long start = System.currentTimeMillis();


        long id = requestId.incrementAndGet();
        Map<String, Object> rpcRequest = Map.of(
                "id", id,
                "method", method,
                "params", params
        );


        JsonNode body = Unirest.post(TQ_BASE_URL)
                               .header("Content-Type", "application/json; charset=UTF-8")
                               .body(JSON.toJSONString(rpcRequest))
                               .asJson()
                               .getBody();


        JSONObject root = JSON.parseObject(body.toString());
        JSONObject result = root.containsKey("result") ? root.getJSONObject("result") : root;


        log.info("TdxTqHttpClient.call     >>>     method : {} , params : {} , result : {} , time : {}",
                 method, JSON.toJSONString(params), JSON.toJSONString(result), DateTimeUtil.formatNow2Hms(start));


        checkResultErr(result);
        return result;
    }


    /**
     * 校验 API 返回的业务异常
     *
     * @param result
     */
    private static void checkResultErr(Object result) {
        if (!(result instanceof JSONObject json)) {
            return;
        }

        String errorId = firstNonBlank(json.getString("ErrorId"), json.getString("errorId"));
        if (errorId == null || "0".equals(errorId)) {
            return;
        }

        String msg = firstNonBlank(
                json.getString("Msg"),
                json.getString("Error"),
                json.getString("ErrorMsg"),
                json.getString("message"),
                "未知错误"
        );
        throw new BizException("通达信 TQ 业务异常[" + errorId + "]: " + msg);
    }


    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }


}