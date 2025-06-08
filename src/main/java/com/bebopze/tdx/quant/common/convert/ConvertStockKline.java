package com.bebopze.tdx.quant.common.convert;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import lombok.SneakyThrows;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;


/**
 * kline_his   ->   dtoList
 *
 * @author: bebopze
 * @date: 2025/5/15
 */
public class ConvertStockKline {


    public static KlineDTO kline2DTO(String kline) {


        // 2025-05-13,21.06,21.45,21.97,20.89,8455131,18181107751.03,5.18,2.98,0.62,6.33
        // 日期,O,C,H,L,VOL,AMO,振幅,涨跌幅,涨跌额,换手率

        String[] klineArr = kline.split(",", -1);


        KlineDTO dto = new KlineDTO();

        dto.setDate(klineArr[0]);

        dto.setOpen(of(klineArr[1]));
        dto.setClose(of(klineArr[2]));
        dto.setHigh(of(klineArr[3]));
        dto.setLow(of(klineArr[4]));

        dto.setVol(Long.valueOf(klineArr[5]));
        dto.setAmo(of(klineArr[6]));

        dto.setRange_pct(of(klineArr[7]));
        dto.setChange_pct(of(klineArr[8]));
        dto.setChange_price(of(klineArr[9]));
        dto.setTurnover_pct(of(klineArr[10]));

        return dto;
    }


    private static BigDecimal of(String valStr) {
        if (valStr == null || valStr.isEmpty()) {
            return null;
        }
        return BigDecimal.valueOf(new Double(valStr));
    }


    public static List<KlineDTO> str2DTOList(String klineHis) {
        List<String> klineList = JSON.parseArray(klineHis, String.class);
        return klines2DTOList(klineList, klineList.size());
    }

    public static List<KlineDTO> str2DTOList(String klineHis, Integer limit) {
        List<String> klineList = JSON.parseArray(klineHis, String.class);
        return klines2DTOList(klineList, limit);
    }


    /**
     * 最近N日 行情
     *
     * @param klines
     * @param limit  最近N日
     * @return
     */
    public static List<KlineDTO> klines2DTOList(List<String> klines, int limit) {

        int size = klines.size();
        if (size > limit) {
            List<String> subKlines = klines.subList(size - limit, size);
            return klines2DTOList(subKlines);
        }

        return klines2DTOList(klines);
    }

    public static List<KlineDTO> klines2DTOList(List<String> klines) {
        if (CollectionUtils.isEmpty(klines)) {
            return Collections.emptyList();
        }
        return klines.stream().map(ConvertStockKline::kline2DTO).collect(Collectors.toList());
    }


    /**
     * 直接从   klineHis 字符串   取值
     *
     * @param klineHis  BaseStockDO / BaseBlockDO     ->     klineHis 字段值
     * @param fieldName
     * @return
     */
    public static double[] fieldValArr(String klineHis, String fieldName) {
        return ConvertStockKline.fieldValArr(str2DTOList(klineHis), fieldName);
    }

    /**
     * 反射取值
     *
     * @param klineDTOList
     * @param fieldName    KlineDTO 的 字段名
     * @return
     */
    @SneakyThrows
    public static double[] fieldValArr(List<KlineDTO> klineDTOList, String fieldName) {

        int size = klineDTOList.size();
        double[] arr = new double[size];


        // 一次性查找 Field，并设置可访问
        Field field = FieldUtils.getDeclaredField(KlineDTO.class, fieldName, true);


        // 遍历 取值
        for (int i = 0; i < size; i++) {
            KlineDTO dto = klineDTOList.get(i);


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


    @SneakyThrows
    public static TreeMap<String, Double> fieldDatePriceMap(List<KlineDTO> klineDTOList,
                                                            String fieldName) {


        int size = klineDTOList.size();


        // 一次性查找 Field，并设置可访问
        Field kField = FieldUtils.getDeclaredField(KlineDTO.class, "date", true);
        Field vField = FieldUtils.getDeclaredField(KlineDTO.class, fieldName, true);


        // 遍历 取值
        TreeMap<String, Double> map = new TreeMap<>();
        for (int i = 0; i < size; i++) {
            KlineDTO dto = klineDTOList.get(i);


            String key = (String) kField.get(dto);
            double value = ((Number) vField.get(dto)).doubleValue();

            map.put(key, value);
        }

        return map;
    }

//    @SneakyThrows
//    public static String[] strFieldValArr(List<KlineDTO> klineDTOList, String fieldName) {
//
//        int size = klineDTOList.size();
//        String[] arr = new String[size];
//
//
//        // 一次性查找 Field，并设置可访问
//        Field field = FieldUtils.getDeclaredField(KlineDTO.class, fieldName, true);
//
//
//        // 遍历 取值
//        for (int i = 0; i < size; i++) {
//            KlineDTO dto = klineDTOList.get(i);
//
//
//            Object value = field.get(dto);
//            if (value == null) {
//
//                arr[i] = null;
//
//            } else if (value instanceof String) {
//
//                arr[i] = (String) value;
//
//            } else {
//                throw new IllegalArgumentException(
//                        String.format("字段 %s 的类型为 %s，无法转换为 String", fieldName, value.getClass().getSimpleName()));
//            }
//        }
//
//
//        return arr;
//    }


    @SneakyThrows
    public static Object[] objFieldValArr(List<KlineDTO> klineDTOList, String fieldName) {

        int size = klineDTOList.size();
        Object[] arr = new Object[size];


        // 一次性查找 Field，并设置可访问
        Field field = FieldUtils.getDeclaredField(KlineDTO.class, fieldName, true);


        // 遍历 取值
        for (int i = 0; i < size; i++) {
            KlineDTO dto = klineDTOList.get(i);


            Object value = field.get(dto);
            arr[i] = value;
        }

        return arr;
    }


    public static String[] strFieldValArr(List<KlineDTO> klineDTOList, String fieldName) {
        Object[] arr = objFieldValArr(klineDTOList, fieldName);


        String[] new_arr = new String[arr.length];
        for (int i = 0; i < arr.length; i++) {
            new_arr[i] = (String) arr[i];
        }

        return new_arr;
    }

    public static long[] longFieldValArr(List<KlineDTO> klineDTOList, String fieldName) {
        Object[] arr = objFieldValArr(klineDTOList, fieldName);


        long[] new_arr = new long[arr.length];
        for (int i = 0; i < arr.length; i++) {
            new_arr[i] = (long) arr[i];
        }

        return new_arr;
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {

//        List<String> list = Lists.newArrayList("1", "2", "3", "4", "5", "6", "7", "8", "9");
//        str2DTO(list, 10);


//        String kline = "2025-05-13,21.06,21.45,21.97,20.89,8455131,18181107751.03,5.18,2.98,0.62,6.33";
//
//
//        KlineDTO klineDTO = str2DTO(kline);
//        System.out.println(klineDTO);
    }


}
