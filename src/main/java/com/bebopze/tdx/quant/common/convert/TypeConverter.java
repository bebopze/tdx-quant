package com.bebopze.tdx.quant.common.convert;

import com.bebopze.tdx.quant.common.util.DateTimeUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;


/**
 * 类型转换
 *
 * @author: bebopze
 * @date: 2025/5/23
 */
public class TypeConverter {


    public static Object convert(Object value, Class<?> targetType) {
        if (value == null || (Objects.equals("", value) && targetType != String.class)) {
            return null;
        }


        if (targetType.isAssignableFrom(value.getClass())) {
            return value;
        }


        String valStr = value.toString();


        if (targetType == Integer.class || targetType == int.class) {
            return Integer.parseInt(valStr);
        } else if (targetType == Long.class || targetType == long.class) {
            return Long.parseLong(valStr);
        } else if (targetType == Double.class || targetType == double.class) {
            return Double.parseDouble(valStr);
        } else if (targetType == Boolean.class || targetType == boolean.class) {
            boolean flag = false;
            if ("1".equals(value)) {
                flag = true;
            } else if ("0".equals(value)) {
                flag = false;
            }
            return flag;
        } else if (targetType == String.class) {
            return valStr;
        } else if (targetType == LocalDate.class) {
            return DateTimeUtil.parseDate_yyyy_MM_dd(valStr);
        } else if (targetType == LocalDateTime.class) {
            return DateTimeUtil.parseTime_yyyy_MM_dd(valStr);
        } else if (targetType.isEnum()) {
            // 处理枚举类型（如：value 是字符串，转换为枚举）
            return Enum.valueOf((Class<Enum>) targetType, valStr);
        } else {
            throw new IllegalArgumentException("Unsupported target type: " + targetType.getName());
        }
    }


    public static Object convertList(List<Object> valueList, Class<?> targetType) {

        if (targetType == Long.class) {
            return valueList.stream().toArray(Long[]::new);
        } else if (targetType == long.class) {
            return valueList.stream().mapToLong(e -> (long) e).toArray();
        } else if (targetType == Integer.class) {
            return valueList.stream().toArray(Integer[]::new);
        } else if (targetType == int.class) {
            return valueList.stream().mapToInt(e -> (int) e).toArray();
        } else if (targetType == Double.class) {
            return valueList.stream().toArray(Double[]::new);
        } else if (targetType == double.class) {
            return valueList.stream().mapToDouble(e -> (double) e).toArray();
        } else if (targetType == Boolean.class) {
            return valueList.stream().toArray(Boolean[]::new);
        } else if (targetType == boolean.class) {
            return valueList.stream().map(e -> (boolean) e).toArray();
        } else if (targetType == LocalDate.class) {
            return valueList.stream().toArray(LocalDate[]::new);
        }

        return null;
    }


}
