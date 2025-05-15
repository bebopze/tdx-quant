package com.bebopze.tdx.quant.common.convert;

import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import lombok.SneakyThrows;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author: bebopze
 * @date: 2025/5/15
 */
public class ConvertStock {


    public static KlineDTO str2DTO(String kline) {


        // 2025-05-13,21.06,21.45,21.97,20.89,8455131,18181107751.03,5.18,2.98,0.62,6.33
        // 日期,O,C,H,L,VOL,AMO,振幅,涨跌幅,涨跌额,换手率

        String[] klineArr = kline.split(",");


        KlineDTO dto = new KlineDTO();

        dto.setDate(klineArr[0]);

        dto.setOpen(BigDecimal.valueOf(new Double(klineArr[1])));
        dto.setClose(BigDecimal.valueOf(new Double(klineArr[2])));
        dto.setHigh(BigDecimal.valueOf(new Double(klineArr[3])));
        dto.setLow(BigDecimal.valueOf(new Double(klineArr[4])));

        dto.setVol(Long.valueOf(klineArr[5]));
        dto.setAmo(BigDecimal.valueOf(new Double(klineArr[6])));

        dto.setRange_pct(BigDecimal.valueOf(new Double(klineArr[7])));
        dto.setChange_pct(BigDecimal.valueOf(new Double(klineArr[8])));
        dto.setChange_price(BigDecimal.valueOf(new Double(klineArr[9])));
        dto.setTurnover_pct(BigDecimal.valueOf(new Double(klineArr[10])));


        return dto;
    }


    public static List<KlineDTO> str2DTO(List<String> klines) {
        return klines.stream().map(ConvertStock::str2DTO).collect(Collectors.toList());
    }


    /**
     * 最近N日 行情
     *
     * @param klines
     * @param limit  最近N日
     * @return
     */
    public static List<KlineDTO> str2DTO(List<String> klines, int limit) {

        int size = klines.size();
        if (size > limit) {
            List<String> subKlines = klines.subList(size - limit, size);
            return str2DTO(subKlines);
        }

        return str2DTO(klines);
    }


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
