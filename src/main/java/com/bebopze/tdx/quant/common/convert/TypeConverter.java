package com.bebopze.tdx.quant.common.convert;


import com.bebopze.tdx.quant.common.util.DateTimeUtil;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.Date;

/**
 * @author: bebopze
 * @date: 2025/5/23
 */
public class TypeConverter {


    public static Object convert(Object value, Class<?> targetType) {
        if (value == null) return null;


        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }

        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(value.toString());
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(value.toString());
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(value.toString());
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            if (value.equals("1")) {
                value = true;
            } else if (value.equals("0")) {
                value = false;
            }
            return Boolean.parseBoolean(value.toString());
        } else if (targetType == String.class) {
            return value.toString();
        } else if (targetType == LocalDate.class) {
            return DateTimeUtil.parseDate_yyyy_MM_dd(value.toString());
        } else if (targetType.isEnum()) {
            // 处理枚举类型（如：value 是字符串，转换为枚举）
            return Enum.valueOf((Class<Enum>) targetType, value.toString());
        } else {
            throw new IllegalArgumentException("Unsupported target type: " + targetType.getName());
        }
    }


}