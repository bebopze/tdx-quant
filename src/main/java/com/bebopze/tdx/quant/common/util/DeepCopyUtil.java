package com.bebopze.tdx.quant.common.util;

import com.alibaba.fastjson2.JSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import lombok.SneakyThrows;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;


/**
 * 深拷贝
 *
 * @author: bebopze
 * @date: 2025/8/29
 */
public class DeepCopyUtil {


    /**
     * 使用序列化进行深拷贝
     *
     * 注意：对象必须实现 Serializable 接口
     *
     * @param object
     * @param <T>
     * @return
     */
    public static <T extends Serializable> T deepCopy(T object) {
        if (object == null) {
            return null;
        }
        return SerializationUtils.clone(object);
    }


    /**
     * 使用 JSON 序列化进行深拷贝
     */
    @SneakyThrows
    public static <T> T deepCopy(T object, Class<T> clazz) {
        if (object == null) {
            return null;
        }


        // JSON
        String json = JSON.toJSONString(object);
        return JSON.to(clazz, json);
    }


    /**
     * 使用 Jackson 序列化进行深拷贝
     */
    @SneakyThrows
    public static <T> T deepCopy2(T object, Class<T> clazz) {
        if (object == null) {
            return null;
        }


        // JSON
        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(object);
        return objectMapper.readValue(json, clazz);
    }


    /**
     * 使用 Gson 进行深拷贝
     */
    public static <T> T deepCopy3(T object, Class<T> clazz) {
        if (object == null) {
            return null;
        }

        Gson gson = new Gson();
        String json = gson.toJson(object);
        return gson.fromJson(json, clazz);
    }


}
