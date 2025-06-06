package com.bebopze.tdx.quant.common.convert;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataDTO;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.stream.Collectors;


/**
 * ext_data_his   ->   dtoList
 *
 * @author: bebopze
 * @date: 2025/5/23
 */
@Slf4j
public class ConvertStockExtData {


    @SneakyThrows
    public static double[] dto2Arr(String extData) {


        // 2025-05-23, 10, 20, 50, 120, 250
        // 日期,rps10,rps20,rps50,rps120,rps250
        String[] row_arr = extData.split(",", -1);


        ExtDataDTO dto = new ExtDataDTO();


        // 获取目标类的 Class 对象
        Class<?> dtoClass = dto.getClass();


        // 按类（ExtDataDTO） 字段顺序     读取 -> set
        Field[] fields = dtoClass.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {

            Field field = fields[i];
            log.debug("Index: {} | Name: {} | Type: {}", i, field.getName(), field.getType().getSimpleName());


            // 设置字段可访问（如果字段是 private）
            field.setAccessible(true);

            // 通过字段名称生成 set 方法名（如 setName）
            String setMethodName = "set" + StringUtils.capitalize(field.getName());

            // 获取 set 方法
            Method setMethod = dtoClass.getMethod(setMethodName, field.getType());


            // 自动类型转换
            Object convertVal = TypeConverter.convert(row_arr[i], field.getType());


            // 调用 set方法  并传入值
            setMethod.invoke(dto, convertVal);
        }


        return null;
    }


    @SneakyThrows
    public static ExtDataDTO str2DTO(String extData) {


        // 2025-05-23, 10, 20, 50, 120, 250
        // 日期,rps10,rps20,rps50,rps120,rps250
        String[] row_arr = extData.split(",", -1);


        ExtDataDTO dto = new ExtDataDTO();


        // 获取目标类的 Class 对象
        Class<?> dtoClass = dto.getClass();


        // 按类（ExtDataDTO） 字段顺序     读取 -> set
        Field[] fields = dtoClass.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {

            Field field = fields[i];
            log.debug("Index: {} | Name: {} | Type: {}", i, field.getName(), field.getType().getSimpleName());


            // 设置字段可访问（如果字段是 private）
            field.setAccessible(true);

            // 通过字段名称生成 set 方法名（如 setName）
            String setMethodName = "set" + StringUtils.capitalize(field.getName());

            // 获取 set 方法
            Method setMethod = dtoClass.getMethod(setMethodName, field.getType());


            // 自动类型转换
            Object convertVal = TypeConverter.convert(row_arr[i], field.getType());


            // 调用 set方法  并传入值
            setMethod.invoke(dto, convertVal);
        }


        return dto;
    }


    private static double of(String valStr) {
        if (valStr == null || valStr.isEmpty()) {
            return Double.NaN;
        }
        return Double.parseDouble(valStr);
    }


    public static List<ExtDataDTO> str2DTO(List<String> extDataList) {
        if (CollectionUtils.isEmpty(extDataList)) {
            return null;
        }
        return extDataList.stream().map(ConvertStockExtData::str2DTO).collect(Collectors.toList());
    }


    /**
     * 最近N日 行情
     *
     * @param extDataList
     * @param limit       最近N日
     * @return
     */
    public static List<ExtDataDTO> str2DTO(List<String> extDataList, int limit) {

        int size = extDataList.size();
        if (size > limit) {
            List<String> subList = extDataList.subList(size - limit, size);
            return str2DTO(subList);
        }

        return str2DTO(extDataList);
    }


    public static List<ExtDataDTO> extDataHis2DTOList(String extDataHis) {
        List<String> klineList = JSON.parseArray(extDataHis, String.class);
        List<ExtDataDTO> dtoList = ConvertStockExtData.str2DTO(klineList);
        return dtoList;
    }

    /**
     * 直接从   klineHis 字符串   取值
     *
     * @param extDataHis BaseStockDO / BaseBlockDO     ->     extDataHis 字段值
     * @param fieldName
     * @return
     */
    public static double[] fieldValArr(String extDataHis, String fieldName) {
        return ConvertStockExtData.fieldValArr(extDataHis2DTOList(extDataHis), fieldName);
    }

    /**
     * 反射取值
     *
     * @param dtoList
     * @param fieldName KlineDTO 的 字段名
     * @return
     */
    @SneakyThrows
    public static double[] fieldValArr(List<ExtDataDTO> dtoList, String fieldName) {

        int size = dtoList.size();
        double[] arr = new double[size];


        // 一次性查找 Field，并设置可访问
        Field field = FieldUtils.getDeclaredField(KlineDTO.class, fieldName, true);


        // 遍历 取值
        for (int i = 0; i < size; i++) {
            ExtDataDTO dto = dtoList.get(i);


            Object value = field.get(dto);
            if (value == null) {

                // null -> 0
                arr[i] = Double.NaN;

            } else if (value instanceof Number) {

                arr[i] = ((Number) value).doubleValue();

            } else {
                throw new IllegalArgumentException(
                        String.format("字段 %s 的类型为 %s，无法转换为 double", fieldName, value.getClass().getSimpleName()));
            }
        }

        return arr;
    }


    public static Object[] objFieldValArr(String extDataHis, String fieldName) {
        return ConvertStockExtData.objFieldValArr(extDataHis2DTOList(extDataHis), fieldName);
    }

    @SneakyThrows
    public static Object[] objFieldValArr(List<ExtDataDTO> dTOList, String fieldName) {

        int size = dTOList.size();
        Object[] arr = new Object[size];


        // 一次性查找 Field，并设置可访问
        Field field = FieldUtils.getDeclaredField(KlineDTO.class, fieldName, true);


        // 遍历 取值
        for (int i = 0; i < size; i++) {
            ExtDataDTO dto = dTOList.get(i);


            Object value = field.get(dto);
            arr[i] = value;
        }

        return arr;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {


        List<String> list = Lists.newArrayList("1", "2", "3", "4", "5", "6", "7", "8", "9");
        str2DTO(list, 10);


        String extData = "2025-05-23,10,20,50,120,250";


        ExtDataDTO dto = str2DTO(extData);
        System.out.println(dto);
    }


}
