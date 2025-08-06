package com.bebopze.tdx.quant.common.convert;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * -
 *
 * @author: bebopze
 * @date: 2025/6/10
 */
@Slf4j
public class ConvertStock {


    public static KlineArrDTO dtoList2Arr(List<KlineDTO> dtoList) {
        int size = dtoList.size();

        KlineArrDTO arrDTO = new KlineArrDTO(size);


        for (int i = 0; i < size; i++) {
            KlineDTO dto = dtoList.get(i);

            arrDTO.date[i] = dto.getDate();
            arrDTO.open[i] = dto.getOpen();
            arrDTO.high[i] = dto.getHigh();
            arrDTO.low[i] = dto.getLow();
            arrDTO.close[i] = dto.getClose();
            arrDTO.vol[i] = dto.getVol();
            arrDTO.amo[i] = dto.getAmo();


            arrDTO.dateCloseMap.put(dto.getDate(), dto.getClose());
        }


        // ---------------------- check


        // KlineArrDTO arrDTO_2 = _dtoList2Arr(dtoList);

        // Assert.isTrue(Objects.equals(JSON.toJSONString(arrDTO), JSON.toJSONString(arrDTO_2)), "arrDTO != arrDTO_2");


        // ----------------------


        return arrDTO;
    }


    /**
     * 反射（通过 fieldName   ->   关联）
     *
     * @param dtoList
     * @return
     */
    @SneakyThrows
    public static KlineArrDTO _dtoList2Arr(List<KlineDTO> dtoList) {
        int size = dtoList.size();


        Map<String, Field> kline___fieldMap = Arrays.stream(KlineDTO.class.getDeclaredFields())
                                                    .peek(field -> field.setAccessible(true))
                                                    .collect(Collectors.toMap(
                                                            Field::getName,
                                                            Function.identity()
                                                    ));


        // List -> arr

        Map<String, List<Object>> fieldName_valList_map = Maps.newHashMap();

        for (int i = 0; i < size; i++) {
            KlineDTO dto = dtoList.get(i);


            kline___fieldMap.forEach((fieldName, field) -> {

                Object val = null;
                try {
                    val = field.get(dto);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }

                fieldName_valList_map.computeIfAbsent(fieldName, k -> Lists.newArrayList()).add(val);
            });
        }


        // -------


        KlineArrDTO arrDTO = new KlineArrDTO(size);
        Field[] arr_fields = arrDTO.getClass().getDeclaredFields();


        for (Field arrField : arr_fields) {
            arrField.setAccessible(true);
            String fieldName = arrField.getName();


            // 通过 fieldName   ->   关联
            Field field = kline___fieldMap.get(fieldName);
            List<Object> valList = fieldName_valList_map.get(fieldName);


            Object typeValArr = TypeConverter.convertList(valList, arrField.getType().getComponentType());
            arrField.set(arrDTO, typeValArr);
        }


        // 触发 -> fill Map
        arrDTO.getDateCloseMap();


        return arrDTO;
    }


    public static ExtDataArrDTO dtoList2Arr2(List<ExtDataDTO> dtoList) {
        int size = dtoList.size();

        ExtDataArrDTO arrDTO = new ExtDataArrDTO(size);


        for (int i = 0; i < size; i++) {
            ExtDataDTO dto = dtoList.get(i);


            arrDTO.date[i] = dto.getDate();


            arrDTO.rps10[i] = dto.getRps10();
            arrDTO.rps20[i] = dto.getRps20();
            arrDTO.rps50[i] = dto.getRps50();
            arrDTO.rps120[i] = dto.getRps120();
            arrDTO.rps250[i] = dto.getRps250();


            arrDTO.SSF[i] = of(dto.getSSF());


            arrDTO.中期涨幅[i] = of(dto.get中期涨幅());
            arrDTO.高位爆量上影大阴[i] = of(dto.get高位爆量上影大阴());


            arrDTO.MA20多[i] = of(dto.getMA20多());
            arrDTO.MA20空[i] = of(dto.getSSF空());
            arrDTO.SSF多[i] = of(dto.getSSF多());
            arrDTO.SSF空[i] = of(dto.getSSF空());

            arrDTO.N日新高[i] = of(dto.getN日新高());
            arrDTO.均线预萌出[i] = of(dto.get均线预萌出());
            arrDTO.均线萌出[i] = of(dto.get均线萌出());
            arrDTO.大均线多头[i] = of(dto.get大均线多头());

            arrDTO.月多[i] = of(dto.get月多());
            arrDTO.RPS红[i] = of(dto.getRPS红());
            arrDTO.RPS三线红[i] = of(dto.getRPS三线红());
        }


        return arrDTO;
    }


    private static double of(Double value) {
        return null == value ? Double.NaN : value;
    }

    private static boolean of(Boolean value) {
        return null != value && value;
    }


}