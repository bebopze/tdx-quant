package com.bebopze.tdx.quant.common.convert;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;


/**
 * ext_data_his   ->   dtoList
 *
 * @author: bebopze
 * @date: 2025/5/23
 */
@Slf4j
public class ConvertStockExtData {


    /**
     * 通过反射，将 ExtDataDTO 类的字段按声明顺序封装成一个 Object[] 数组
     *
     *
     * - 字段顺序 必须保留（Java反射 默认 返回字段顺序为 声明顺序）
     *
     * @param dto
     * @return
     */
    @SneakyThrows
    public static Object[] dto2Arr(ExtDataDTO dto) {
        List<Object> result = new ArrayList<>();

        Field[] fields = dto.getClass().getDeclaredFields();

        for (Field field : fields) {
            // 设置字段可访问（如果字段是 private）
            field.setAccessible(true);

            Object value = field.get(dto);
            result.add(value);
        }

        return result.toArray();
    }


    @SneakyThrows
    public static ExtDataDTO str2DTO(String extData) {


        // 2025-05-23, 10, 20, 50, 120, 250
        // 日期,rps10,rps20,rps50,rps120,rps250
        String[] extDataArr = extData.split(",", -1);


        ExtDataDTO dto = new ExtDataDTO();
        Field[] fields = dto.getClass().getDeclaredFields();


        for (int i = 0; i < extDataArr.length; i++) {
            String valStr = extDataArr[i];


            Field field = fields[i];
            field.setAccessible(true);


            Object typeVal = TypeConverter.convert(valStr, field.getType());
            field.set(dto, typeVal);
        }


        return dto;
    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                         extData（String） -> DTO
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    public static List<ExtDataDTO> strList2DTOList(List<String> extDataList) {
        if (CollectionUtils.isEmpty(extDataList)) {
            return Collections.emptyList();
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
    public static List<ExtDataDTO> strList2DTOList(List<String> extDataList, int limit) {

        int size = extDataList.size();
        if (size > limit) {
            List<String> subList = ListUtil.lastN(extDataList, limit);
            return strList2DTOList(subList);
        }

        return strList2DTOList(extDataList);
    }


    public static List<ExtDataDTO> extDataHis2DTOList(String extDataHis) {
        List<String> klineList = JSON.parseArray(extDataHis, String.class);
        List<ExtDataDTO> dtoList = ConvertStockExtData.strList2DTOList(klineList);
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
     * @param fieldName ExtDataDTO 的 字段名
     * @return
     */
    @SneakyThrows
    public static double[] fieldValArr(List<ExtDataDTO> dtoList, String fieldName) {

        int size = dtoList.size();
        double[] arr = new double[size];


        // 一次性查找 Field，并设置可访问
        Field field = FieldUtils.getDeclaredField(ExtDataDTO.class, fieldName, true);


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
        Field field = FieldUtils.getDeclaredField(ExtDataDTO.class, fieldName, true);


        // 遍历 取值
        for (int i = 0; i < size; i++) {
            ExtDataDTO dto = dTOList.get(i);


            Object value = field.get(dto);
            arr[i] = value;
        }

        return arr;
    }


    // -----------------------------------------------------------------------------------------------------------------


    private static double of(String valStr) {
        if (valStr == null || valStr.isEmpty()) {
            return Double.NaN;
        }
        return Double.parseDouble(valStr);
    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                         DTO -> extData（String）
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    public static String dtoList2JsonStr(List<ExtDataDTO> dtoList) {
        List<String> strList = dtoList2StrList(dtoList);
        return JSON.toJSONString(strList);
    }

    public static List<String> dtoList2StrList(List<ExtDataDTO> dtoList) {
        List<Object[]> arrList = dtoList2ArrList(dtoList);

        List<String> extDatas = Lists.newArrayList();
        for (Object[] arr : arrList) {
            String extDataStr = Arrays.stream(arr).map(ConvertStockExtData::typeConvert).collect(Collectors.joining(","));
            extDatas.add(extDataStr);
        }

        return extDatas;
    }


    public static List<Object[]> dtoList2ArrList(List<ExtDataDTO> dtoList) {
        List<Object[]> arrList = Lists.newArrayList();

        for (int i = 0; i < dtoList.size(); i++) {
            ExtDataDTO dto = dtoList.get(i);

            // 按 DTO类 字段顺序  ->  Object[]
            Object[] arr = dto2Arr(dto);
            arrList.add(arr);
        }

        return arrList;
    }


    private static String typeConvert(Object obj) {
        if (obj == null) {
            return "";
        }

        if (obj instanceof Boolean) {
            return (Boolean) obj ? "1" : "0";
        }

        return obj.toString();
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {


        List<String> list = Lists.newArrayList("1", "2", "3", "4", "5", "6", "7", "8", "9");
        strList2DTOList(list, 10);


        String extData = "2025-05-23,10,20,50,120,250";


        ExtDataDTO dto = str2DTO(extData);
        System.out.println(dto);
    }


}
