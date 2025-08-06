package com.bebopze.tdx.quant.common.convert;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/**
 * kline_his   ->   dtoList
 *
 * @author: bebopze
 * @date: 2025/5/15
 */
@Slf4j
public class ConvertStockKline {


    /**
     * 通过反射，将 KlineDTO 类的字段按声明顺序封装成一个 Object[] 数组
     *
     *
     * - 字段顺序 必须保留（Java反射 默认 返回字段顺序为 声明顺序）
     *
     * @param dto
     * @return
     */
    @SneakyThrows
    public static Object[] dto2Arr(KlineDTO dto) {
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
    public static KlineDTO kline2DTO(String kline) {


        // 2025-05-13,21.06,21.45,21.97,20.89,8455131,18181107751.03,5.18,2.98,0.62,6.33
        // 日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率

        String[] klineArr = kline.split(",", -1);


        KlineDTO dto = new KlineDTO();
        Field[] fields = dto.getClass().getDeclaredFields();


        for (int i = 0; i < klineArr.length; i++) {
            String valStr = klineArr[i];


            Field field = fields[i];
            field.setAccessible(true);


            Object typeVal = TypeConverter.convert(valStr, field.getType());
            field.set(dto, typeVal);
        }


//        // ---------------------------------------- check
//
//
//        KlineDTO dto2 = new KlineDTO();
//
//        dto2.setDate(ofDate(klineArr[0]));
//
//        dto2.setOpen(of(klineArr[1]));
//        dto2.setHigh(of(klineArr[2]));
//        dto2.setLow(of(klineArr[3]));
//        dto2.setClose(of(klineArr[4]));
//
//
//        dto2.setVol(Long.valueOf(klineArr[5]));
//        dto2.setAmo(of(klineArr[6]));
//
//        dto2.setRange_pct(of(klineArr[7]));
//        dto2.setChange_pct(of(klineArr[8]));
//        dto2.setChange_price(of(klineArr[9]));
//        dto2.setTurnover_pct(of(klineArr[10]));
//
//
//        Assert.isTrue(Objects.equals(JSON.toJSONString(dto), JSON.toJSONString(dto2)),
//                      String.format("dot : %s , dot2 : %s ", JSON.toJSONString(dto), JSON.toJSONString(dto2)));
//
//
//        // ----------------------------------------


        return dto;
    }


    // -----------------------------------------------------------------------------------------------------------------


    private static LocalDate ofDate(String valStr) {
        if (valStr == null || valStr.isEmpty()) {
            return null;
        }
        return DateTimeUtil.parseDate_yyyy_MM_dd(valStr);
    }

    private static Double of(String valStr) {
        if (valStr == null || valStr.isEmpty()) {
            return null;
        }
        return BigDecimal.valueOf(new Double(valStr)).doubleValue();
    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                                         kline（String） -> DTO
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


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
        if (CollectionUtils.isEmpty(klines)) {
            return Lists.newArrayList();
        }


        int size = klines.size();
        if (size > limit) {
            List<String> subKlines = ListUtil.lastN(klines, limit);
            return klines2DTOList(subKlines);
        }

        return klines2DTOList(klines);
    }

    public static List<KlineDTO> klines2DTOList(List<String> klines) {
        if (CollectionUtils.isEmpty(klines)) {
            return Lists.newArrayList();
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
    public static TreeMap<LocalDate, Double> fieldDatePriceMap(List<KlineDTO> klineDTOList,
                                                               String fieldName) {


        int size = klineDTOList.size();


        // 一次性查找 Field，并设置可访问
        Field kField = FieldUtils.getDeclaredField(KlineDTO.class, "date", true);
        Field vField = FieldUtils.getDeclaredField(KlineDTO.class, fieldName, true);


        // 遍历 取值
        TreeMap<LocalDate, Double> map = new TreeMap<>();
        for (int i = 0; i < size; i++) {
            KlineDTO dto = klineDTOList.get(i);


            LocalDate key = (LocalDate) kField.get(dto);
            double value = ((Number) vField.get(dto)).doubleValue();

            map.put(key, value);
        }

        return map;
    }


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


    public static LocalDate[] dateFieldValArr(List<KlineDTO> klineDTOList, String fieldName) {
        Object[] arr = objFieldValArr(klineDTOList, fieldName);


        LocalDate[] new_arr = new LocalDate[arr.length];
        for (int i = 0; i < arr.length; i++) {
            new_arr[i] = (LocalDate) arr[i];
        }

        return new_arr;
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


    // -----------------------------------------------------------------------------------------------------------------
    //                                         DTO -> kline（String）
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    public static String dtoList2JsonStr(List<KlineDTO> dtoList) {
        List<String> strList = dtoList2StrList(dtoList);
        return JSON.toJSONString(strList);
    }

    public static List<String> dtoList2StrList(List<KlineDTO> dtoList) {
        List<Object[]> arrList = dtoList2ArrList(dtoList);

        List<String> klines = Lists.newArrayList();
        for (Object[] arr : arrList) {
            String kline = Arrays.stream(arr).map(ConvertStockKline::typeConvert).collect(Collectors.joining(","));
            klines.add(kline);
        }

        return klines;
    }


    public static List<Object[]> dtoList2ArrList(List<KlineDTO> dtoList) {
        List<Object[]> arrList = Lists.newArrayList();

        for (int i = 0; i < dtoList.size(); i++) {
            KlineDTO dto = dtoList.get(i);

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

//        List<String> list = Lists.newArrayList("1", "2", "3", "4", "5", "6", "7", "8", "9");
//        str2DTO(list, 10);


//        String kline = "2025-05-13,21.06,21.45,21.97,20.89,8455131,18181107751.03,5.18,2.98,0.62,6.33";
//
//
//        KlineDTO klineDTO = str2DTO(kline);
//        System.out.println(klineDTO);
    }


}
