package com.bebopze.tdx.quant.common.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.alibaba.fastjson2.reader.ObjectReaderProvider;
import com.alibaba.fastjson2.JSONReader;
import lombok.Data;

import java.lang.reflect.Type;
import java.math.BigDecimal;


/**
 * fastjson2    全局拦截所有 BigDecimal 字段的反序列化
 *
 * 将 空串、空格串 或 "-" 转为 null
 *
 * @author: bebopze
 * @date: 2025/5/20
 */
public class StringToBigDecimalReader implements ObjectReader<BigDecimal> {


    static {
        ObjectReaderProvider provider = JSONFactory.getDefaultObjectReaderProvider();
        provider.register(BigDecimal.class, new StringToBigDecimalReader());
    }


    @Override
    public BigDecimal readObject(JSONReader reader, Type fieldType, Object fieldName, long features) {

        // 如果是 null，返回 null
        if (reader.readIfNull()) {
            return null;
        }

        // 读取字符串
        String text = reader.readString();

        // 特殊字符串 "-" -> 返回 null
        if ("-".equals(text) || "null".equals(text) || text.trim().isEmpty()) {
            return null;
        }

        // 其他情况尝试构造 BigDecimal
        return new BigDecimal(text);
    }


//    @Override
//    public long getFeatures() {
//        // 保持默认特性即可
//        return 0;
//    }


    // -----------------------------------------------------------------------------------------------------------------

    public static void main(String[] args) {


        String str = "{\n" +
                "  \"k1\": null,\n" +
                "  \"k2\": \"-\",\n" +
                "  \"k3\": \"   \",\n" +
                "  \"k4\": \"null\",\n" +
                "  \"k5\": \"01.23000\"\n" +
                "}";


        Num num = JSON.parseObject(str, Num.class);


        System.out.println(num);
    }


    @Data
    public static class Num {
        private BigDecimal k1;
        private BigDecimal k2;
        private BigDecimal k3;
        private BigDecimal k4;
        private BigDecimal k5;
    }


}