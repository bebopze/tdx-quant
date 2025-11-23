package com.bebopze.tdx.quant.common.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;


/**
 * JSON 工具类
 *
 * @author: bebopze
 * @date: 2025/11/22
 */
@Slf4j
public class JsonUtil {


    /**
     * 安全的JSON序列化方法，避免FastJSON2的ArrayIndexOutOfBoundsException异常
     *
     * @param object 要序列化的对象
     * @return 序列化后的JSON字符串
     */
    public static String toJSONString(Object object) {
        if (object == null) {
            return null;
        }


        try {
            // 首先尝试使用默认方式序列化
            return JSON.toJSONString(object);

        } catch (Exception ex) {
            log.warn("FastJSON序列化异常，使用备用方案     >>>     obj : {} , errMsg : {}", object, ex.getMessage(), ex);

            try {
                // 如果失败，尝试使用其他特性
                return JSON.toJSONString(object, JSONWriter.Feature.WriteMapNullValue);

            } catch (Exception ex2) {

                // String json = new Gson().toJson(object);
                log.error("FastJSON序列化失败     >>>     obj : {} , errMsg : {}", object, ex2.getMessage(), ex2);
                // 如果都失败了，返回空数组或空对象
                return object instanceof Collection ? "[]" : "{}";
            }
        }
    }


}