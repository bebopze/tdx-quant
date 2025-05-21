package com.bebopze.tdx.quant.common.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriterPrimitiveImpl;
import org.springframework.context.annotation.Configuration;

import java.lang.reflect.Type;
import java.math.BigDecimal;


/**
 * FastJson2
 *
 * @author: bebopze
 * @date: 2025/5/20
 */
@Configuration
public class FastJson2Config {


    static {

        // 自定义 解析器
        registerProvider();


        JSON.configWriterDateFormat("yyyy-MM-dd HH:mm:ss");
        JSON.configReaderDateFormat("yyyy-MM-dd HH:mm:ss");


        JSON.config(JSONWriter.Feature.WriteNullNumberAsZero,   // Long等Number类型值为空序列化为0
                    JSONWriter.Feature.WriteNullBooleanAsFalse, // Boolean类型值为空序列化为false
                    JSONWriter.Feature.WriteLongAsString,       // Long类型序列化为字符串
                    JSONWriter.Feature.WriteNullListAsEmpty,    // List类型值为空序列化为[]
                    JSONWriter.Feature.WriteBigDecimalAsPlain,  // BigDecimal类型序列化为非科学计数法
                    // JSONWriter.Feature.PrettyFormat,         // 格式化输出
                    JSONWriter.Feature.WriteNullStringAsEmpty); // String类型值为空序列化为""

        JSON.config(JSONReader.Feature.AllowUnQuotedFieldNames); // 字段名称支持没有双引号的反序列化
        // 当序列化的值为字符串时直接输出字符串，而不是再加一层引号或双引号，如`JSON.toJSONString("test")`直接输出"test"，而不是"\"test\""


        JSON.register(String.class, new ObjectWriterPrimitiveImpl<String>() {

            @Override
            public void write(JSONWriter jsonWriter, Object object, Object fieldName, Type fieldType, long features) {
                jsonWriter.writeRaw((String) object);
            }
        });
    }


    // @PostConstruct
    public static void registerProvider() {
        JSONFactory.getDefaultObjectReaderProvider()
                .register(BigDecimal.class, new StringToBigDecimalReader());

        JSONFactory.getDefaultObjectWriterProvider()
                .register(double[].class, new DoubleArrayWriter());
    }


}