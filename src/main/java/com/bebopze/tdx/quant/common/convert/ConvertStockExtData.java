package com.bebopze.tdx.quant.common.convert;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.util.BoolUtil;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.convert.ConvertStockKline.ofDate;


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


        // ----------------------------------------- 反射 ---------------------------------------------------------------


        List<Object> result = Lists.newArrayList();


        Field[] fields = dto.getClass().getDeclaredFields();

        for (Field field : fields) {
            // 设置字段可访问（如果字段是 private）
            field.setAccessible(true);

            Object value = field.get(dto);
            result.add(value);
        }


        return result.toArray();


        // ----------------------------------------- 无反射（高性能） -----------------------------------------------------


//        if (dto == null) {
//            return new Object[0];
//        }
//
//
//        // 按照 ExtDataDTO 类中字段的声明顺序，显式调用 getter 方法获取值
//        // 请根据 ExtDataDTO 实际的字段和 getter 方法名称进行调整
//        Object[] result = {
//                dto.getDate(),
//                dto.getRps10(),
//                dto.getRps20(),
//                dto.getRps50(),
//                dto.getRps120(),
//                dto.getRps250(),
//                dto.getMA5(),
//                dto.getMA10(),
//                dto.getMA20(),
//                dto.getMA30(),
//                dto.getMA50(),
//                dto.getMA60(),
//                dto.getMA100(),
//                dto.getMA120(),
//                dto.getMA150(),
//                dto.getMA200(),
//                dto.getMA250(),
//                dto.getC_SSF_偏离率(),
//                dto.getH_SSF_偏离率(),
//                dto.getC_MA5_偏离率(),
//                dto.getC_MA15_偏离率(),
//                dto.getC_MA20_偏离率(),
//                dto.getH_MA20_偏离率(),
//                dto.getC_MA25_偏离率(),
//                dto.getC_MA30_偏离率(),
//                dto.getC_MA40_偏离率(),
//                dto.getC_MA50_偏离率(),
//                dto.getMA60(),
//                dto.getMA100(),
//                dto.getMA120(),
//                dto.getMA150(),
//                dto.getMA200(),
//                dto.getMA250(),
//                dto.getC_MA60_偏离率(),
//                dto.getC_MA100_偏离率(),
//                dto.getC_MA120_偏离率(),
//                dto.getC_MA150_偏离率(),
//                dto.getC_MA200_偏离率(),
//                dto.get高位爆量上影大阴(),
//                dto.get涨停(),
//                dto.get跌停(),
//                dto.getXZZB(),
//                dto.getMA5多(),
//                dto.getMA5空(),
//                dto.getMA10多(),
//                dto.getMA10空(),
//                dto.getMA20多(),
//                dto.getMA20空(),
//                dto.getSSF多(),
//                dto.getSSF空(),
//                dto.get上MA20(),
//                dto.get下MA20(),
//                dto.get上SSF(),
//                dto.get下SSF(),
//                dto.getN60日新高(),
//                dto.getN100日新高(),
//                dto.get历史新高(),
//                dto.get百日新高(),
//                dto.get月多(),
//                dto.get均线预萌出(),
//                dto.get均线萌出(),
//                dto.get小均线多头(),
//                dto.get大均线多头(),
//                dto.get均线大多头(),
//                dto.get均线极多头(),
//                dto.getRPS红(),
//                dto.getRPS一线红(),
//                dto.getRPS双线红(),
//                dto.getRPS三线红(),
//                dto.getKlineType()
//        };
//
//
//        return result;
    }


    @SneakyThrows
    public static ExtDataDTO str2DTO(String extData) {


        // 2025-05-23, 10, 20, 50, 120, 250
        // 日期,rps10,rps20,rps50,rps120,rps250
        String[] extDataArr = extData.split(",", -1);


        ExtDataDTO dto = new ExtDataDTO();


        // ----------------------------------------- 反射 ---------------------------------------------------------------


        Field[] fields = dto.getClass().getDeclaredFields();


        for (int i = 0; i < extDataArr.length; i++) {
            String valStr = extDataArr[i];


            Field field = fields[i];
            field.setAccessible(true);


            Object typeVal = TypeConverter.convert(valStr, field.getType());
            field.set(dto, typeVal);
        }


        return dto;


        // ----------------------------------------- 无反射（高性能） -----------------------------------------------------


//        int i = 0;
//
//
//        dto.setDate(ofDate(extDataArr[i++]));
//        dto.setRps10(of(extDataArr[i++]));
//        dto.setRps20(of(extDataArr[i++]));
//        dto.setRps50(of(extDataArr[i++]));
//        dto.setRps120(of(extDataArr[i++]));
//        dto.setRps250(of(extDataArr[i++]));
//        dto.setMA5(of(extDataArr[i++]));
//        dto.setMA10(of(extDataArr[i++]));
//        dto.setMA20(of(extDataArr[i++]));
//        dto.setMA30(of(extDataArr[i++]));
//        dto.setMA50(of(extDataArr[i++]));
//        dto.setMA60(of(extDataArr[i++]));
//        dto.setMA100(of(extDataArr[i++]));
//        dto.setMA120(of(extDataArr[i++]));
//        dto.setMA150(of(extDataArr[i++]));
//        dto.setMA200(of(extDataArr[i++]));
//        dto.setMA250(of(extDataArr[i++]));
//        dto.setC_SSF_偏离率(of(extDataArr[i++]));
//        dto.setH_SSF_偏离率(of(extDataArr[i++]));
//        dto.setC_MA5_偏离率(of(extDataArr[i++]));
//        dto.setC_MA15_偏离率(of(extDataArr[i++]));
//        dto.setC_MA20_偏离率(of(extDataArr[i++]));
//        dto.setH_MA20_偏离率(of(extDataArr[i++]));
//        dto.setC_MA25_偏离率(of(extDataArr[i++]));
//        dto.setC_MA30_偏离率(of(extDataArr[i++]));
//        dto.setC_MA40_偏离率(of(extDataArr[i++]));
//        dto.setC_MA50_偏离率(of(extDataArr[i++]));
//        dto.setMA60(of(extDataArr[i++]));
//        dto.setMA100(of(extDataArr[i++]));
//        dto.setMA120(of(extDataArr[i++]));
//        dto.setMA150(of(extDataArr[i++]));
//        dto.setMA200(of(extDataArr[i++]));
//        dto.setMA250(of(extDataArr[i++]));
//        dto.setC_MA60_偏离率(of(extDataArr[i++]));
//        dto.setC_MA100_偏离率(of(extDataArr[i++]));
//        dto.setC_MA120_偏离率(of(extDataArr[i++]));
//        dto.setC_MA150_偏离率(of(extDataArr[i++]));
//        dto.setC_MA200_偏离率(of(extDataArr[i++]));
//        dto.set高位爆量上影大阴(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set涨停(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set跌停(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setXZZB(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setMA5多(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setMA5空(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setMA10多(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setMA10空(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setMA20多(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setMA20空(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setSSF多(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setSSF空(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set上MA20(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set下MA20(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set上SSF(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set下SSF(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setN60日新高(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setN100日新高(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set历史新高(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set百日新高(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set月多(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set均线预萌出(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set均线萌出(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set小均线多头(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set大均线多头(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set均线大多头(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.set均线极多头(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setRPS红(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setRPS一线红(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setRPS双线红(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setRPS三线红(BoolUtil.intStr2Bool(extDataArr[i++]));
//        dto.setKlineType(Integer.parseInt(extDataArr[i++]));
//
//        return dto;
    }


    @SneakyThrows
    public static Map<String, Boolean> toBooleanMap(ExtDataDTO dto) {

        Map<String, Boolean> map = Maps.newHashMap();
        if (dto == null) {
            return map;
        }


        Class<?> clazz = dto.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {

            // 只处理 Boolean 类型（注意不是 boolean）
            if (field.getType() != Boolean.class) {
                continue;
            }

            field.setAccessible(true);
            Boolean value = (Boolean) field.get(dto);

            map.put(field.getName(), value == null ? false : value);
        }


        return map;
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
