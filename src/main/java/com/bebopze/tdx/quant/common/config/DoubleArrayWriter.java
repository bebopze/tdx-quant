package com.bebopze.tdx.quant.common.config;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONFactory;
import com.alibaba.fastjson2.JSONWriter;
import com.alibaba.fastjson2.writer.ObjectWriter;
import com.google.common.collect.Maps;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;


/**
 * double -> 序列化 保留2位小数
 *
 * @author: bebopze
 * @date: 2025/5/21
 */
public class DoubleArrayWriter implements ObjectWriter<double[]> {


    static {
        // 注册到全局，确保 JSON.toJSONString(arr) 会调用此序列化器
        JSONFactory.getDefaultObjectWriterProvider()
                .register(double[].class, new DoubleArrayWriter());
    }


    @Override
    public void write(JSONWriter writer, Object object, Object fieldName, Type fieldType, long features) {

        if (object == null) {
            writer.writeNull();
            return;
        }


        double[] arr = (double[]) object;

        // 开始数组
        writer.startArray();
        for (int i = 0; i < arr.length; i++) {

            // 在第 1 个元素之后插入逗号
            if (i > 0) {
                // 手动写入逗号分隔符
                writer.writeRaw(',');
            }


            // NaN
            if (Double.isNaN(arr[i])) {
                writer.writeNull();
            } else {

                // 写入格式化后的值
                BigDecimal bd = BigDecimal.valueOf(arr[i]).setScale(2, RoundingMode.HALF_UP);
                writer.writeDouble(bd.doubleValue());
            }
        }


        // 结束数组
        writer.endArray();
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {

        double[] arr = {98.64534336782691, 98.36312323612418, 98.19379115710254};
        double[] arr2 = new double[]{31.044214487300096, 31.138287864534338, 31.26999059266228};

        Map map = Maps.newHashMap();
        map.put("arr", arr);
        map.put("arr2", arr2);


        String arrJson = JSON.toJSONString(arr);
        String mapJson = JSON.toJSONString(map);

        String arrJson2 = JSON.toJSONString(arr2);


        System.out.println(arrJson);
        System.out.println(arrJson2);
        System.out.println(mapJson);
    }


}