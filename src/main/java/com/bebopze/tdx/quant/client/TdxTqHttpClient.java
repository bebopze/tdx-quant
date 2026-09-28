package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.PropsUtil;
import com.google.gson.Gson;
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
     * 专用 Gson 实例（避免 num -> String）：
     *
     * 1、保持 num 原始类型：禁止将 params 中的 int -> Str  （避免 通达信 Python程序  ->  参数类型 异常报错：TPyth_TdxWServer_Main_New）
     * 2、与项目全局 Fastjson2 配置完全隔离（避免 num -> String）
     */
    private static final Gson GSON = new Gson();


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


        // 通达信 TQ  ->  Python程序 int类型【传入String】 参数映射 BUG : 通达信 Python 服务  直接报错：TPyth_TdxWServer_Main_New


        //  TdxTqHttpClient.call     >>>
        //
        //  method : get_stock_list ,
        //
        //  params : {"market":"103","list_type":"1"} ,   // list_type -> int （传 字符串 "1"  ->  通达信 Python 服务  直接报错：TPyth_TdxWServer_Main_New）
        //
        //  result : {"Error":"RPC处理异常:TPyth_TdxWServer_Main_New","ErrorId":"10","run_id":"-1"}


        JsonNode body = Unirest.post(TQ_BASE_URL)
                               .header("Content-Type", "application/json; charset=UTF-8")
                               // ⚠️ 此处禁用 Fastjson2  =>  开启了 全局配置：num -> Str（通达信 TQ  ->  Python程序 int类型【传入String】 参数映射 BUG : TPyth_TdxWServer_Main_New）
                               .body(GSON.toJson(rpcRequest))
                               .asJson()
                               .getBody();


        JSONObject root = JSON.parseObject(body.toString());
        JSONObject result = root.containsKey("result") ? root.getJSONObject("result") : root;


        log.info("TdxTqHttpClient.call     >>>     method : {} , params : {} , result : {} , time : {}",
                 method, GSON.toJson(params), GSON.toJson(result), DateTimeUtil.formatNow2Hms(start));


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