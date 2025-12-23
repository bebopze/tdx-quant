package com.bebopze.tdx.quant.common.convert;

import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


/**
 * List<Kline / ExtData>（列表）    ->     KlineArr / ExtDataArr（序列）
 *
 * @author: bebopze
 * @date: 2025/6/10
 */
@Slf4j
public class ConvertStock {


    // -----------------------------------------------------------------------------------------------------------------
    //                               List<Kline>（列表）    ->     KlineArr（序列）
    // -----------------------------------------------------------------------------------------------------------------


    /**
     * List<Kline>（列表）    ->     KlineArr（序列）
     *
     * @param dtoList
     * @return
     */
    public static KlineArrDTO kline__dtoList2Arr(List<KlineDTO> dtoList) {
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

            arrDTO.range_pct[i] = dto.getRange_pct();
            arrDTO.change_pct[i] = dto.getChange_pct();
            arrDTO.change_price[i] = dto.getChange_price();
            arrDTO.turnover_pct[i] = of(dto.getTurnover_pct());


            arrDTO.dateCloseMap.put(dto.getDate(), dto.getClose());
        }


        // ---------------------- check


        // KlineArrDTO arrDTO_2 = _dtoList2Arr(dtoList);

        // Assert.isTrue(Objects.equals(JSON.toJSONString(arrDTO), JSON.toJSONString(arrDTO_2)), "arrDTO != arrDTO_2");


        // ----------------------


        return arrDTO;
    }


    /**
     * List<Kline>（列表）    ->     KlineArr（序列）                           // 反射 实现（通过 fieldName   ->   关联）
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

                Object val;
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


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------
    //                               List<ExtData>（列表）    ->     ExtDataArr（序列）
    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * List<ExtData>（列表）    ->     ExtDataArr（序列）
     *
     * @param dtoList
     * @return
     */
    public static ExtDataArrDTO extData__dtoList2Arr(List<ExtDataDTO> dtoList) {
        int size = dtoList.size();

        ExtDataArrDTO arrDTO = new ExtDataArrDTO(size);


        for (int i = 0; i < size; i++) {
            ExtDataDTO dto = dtoList.get(i);


            // ---------------------------------------------------


            arrDTO.date[i] = dto.getDate();


            // ---------------------------------------------------


            arrDTO.rps10[i] = dto.getRps10();
            arrDTO.rps20[i] = dto.getRps20();
            arrDTO.rps50[i] = dto.getRps50();
            arrDTO.rps120[i] = dto.getRps120();
            arrDTO.rps250[i] = dto.getRps250();


            // ---------------------------------------------------


            arrDTO.MA5[i] = of(dto.getMA5());
            arrDTO.MA10[i] = of(dto.getMA10());
            arrDTO.MA20[i] = of(dto.getMA20());
            arrDTO.MA30[i] = of(dto.getMA30());
            arrDTO.MA50[i] = of(dto.getMA50());
            arrDTO.MA60[i] = of(dto.getMA60());
            arrDTO.MA100[i] = of(dto.getMA100());
            arrDTO.MA120[i] = of(dto.getMA120());
            arrDTO.MA150[i] = of(dto.getMA150());
            arrDTO.MA200[i] = of(dto.getMA200());
            arrDTO.MA250[i] = of(dto.getMA250());


            // ---------------------------------------------------


            arrDTO.SSF[i] = of(dto.getSSF());
            arrDTO.SAR[i] = of(dto.getSAR());


            // ---------------------------------------------------


            arrDTO.RPS三线和[i] = of(dto.getRPS三线和());
            arrDTO.RPS五线和[i] = of(dto.getRPS五线和());


            // ---------------------------------------------------


            arrDTO.中期涨幅N5[i] = of(dto.get中期涨幅N5());
            arrDTO.中期涨幅N10[i] = of(dto.get中期涨幅N10());
            arrDTO.中期涨幅N20[i] = of(dto.get中期涨幅N20());
            arrDTO.中期涨幅N30[i] = of(dto.get中期涨幅N30());
            arrDTO.中期涨幅N50[i] = of(dto.get中期涨幅N50());
            arrDTO.中期涨幅N60[i] = of(dto.get中期涨幅N60());
            arrDTO.中期涨幅N100[i] = of(dto.get中期涨幅N100());
            arrDTO.中期涨幅N120[i] = of(dto.get中期涨幅N120());
            arrDTO.中期涨幅N150[i] = of(dto.get中期涨幅N150());
            arrDTO.中期涨幅N200[i] = of(dto.get中期涨幅N200());
            arrDTO.中期涨幅N250[i] = of(dto.get中期涨幅N250());


            // ---------------------------------------------------


            arrDTO.N3日涨幅[i] = of(dto.getN3日涨幅());
            arrDTO.N5日涨幅[i] = of(dto.getN5日涨幅());
            arrDTO.N10日涨幅[i] = of(dto.getN10日涨幅());
            arrDTO.N20日涨幅[i] = of(dto.getN20日涨幅());
            arrDTO.N30日涨幅[i] = of(dto.getN30日涨幅());
            arrDTO.N50日涨幅[i] = of(dto.getN50日涨幅());
            arrDTO.N60日涨幅[i] = of(dto.getN60日涨幅());
            arrDTO.N100日涨幅[i] = of(dto.getN100日涨幅());
            arrDTO.N120日涨幅[i] = of(dto.getN120日涨幅());
            arrDTO.N150日涨幅[i] = of(dto.getN150日涨幅());
            arrDTO.N200日涨幅[i] = of(dto.getN200日涨幅());
            arrDTO.N250日涨幅[i] = of(dto.getN250日涨幅());


            // ---------------------------------------------------


            arrDTO.中期调整幅度[i] = of(dto.get中期调整幅度());
            arrDTO.中期调整天数[i] = of(dto.get中期调整天数());
            arrDTO.中期调整幅度2[i] = of(dto.get中期调整幅度2());
            arrDTO.中期调整天数2[i] = of(dto.get中期调整天数2());


            // ---------------------------------------------------


            arrDTO.短期支撑线[i] = of(dto.get短期支撑线());
            arrDTO.中期支撑线[i] = of(dto.get中期支撑线());
            arrDTO.长期支撑线[i] = of(dto.get长期支撑线());


            // ---------------------------------------------------


            arrDTO.C_SSF_偏离率[i] = of(dto.getC_SSF_偏离率());
            arrDTO.H_SSF_偏离率[i] = of(dto.getH_SSF_偏离率());


            // ---------------------------------------------------


            arrDTO.C_MA5_偏离率[i] = of(dto.getC_MA5_偏离率());
            arrDTO.H_MA5_偏离率[i] = of(dto.getH_MA5_偏离率());

            arrDTO.C_MA10_偏离率[i] = of(dto.getC_MA10_偏离率());
            arrDTO.H_MA10_偏离率[i] = of(dto.getH_MA10_偏离率());

            arrDTO.C_MA20_偏离率[i] = of(dto.getC_MA20_偏离率());
            arrDTO.H_MA20_偏离率[i] = of(dto.getH_MA20_偏离率());

            arrDTO.C_MA30_偏离率[i] = of(dto.getC_MA30_偏离率());
            arrDTO.H_MA30_偏离率[i] = of(dto.getH_MA30_偏离率());

            arrDTO.C_MA50_偏离率[i] = of(dto.getC_MA50_偏离率());
            arrDTO.H_MA50_偏离率[i] = of(dto.getH_MA50_偏离率());

            arrDTO.C_MA60_偏离率[i] = of(dto.getC_MA60_偏离率());
            arrDTO.H_MA60_偏离率[i] = of(dto.getH_MA60_偏离率());

            arrDTO.C_MA100_偏离率[i] = of(dto.getC_MA100_偏离率());
            arrDTO.H_MA100_偏离率[i] = of(dto.getH_MA100_偏离率());

            arrDTO.C_MA120_偏离率[i] = of(dto.getC_MA120_偏离率());
            arrDTO.H_MA120_偏离率[i] = of(dto.getH_MA120_偏离率());

            arrDTO.C_MA150_偏离率[i] = of(dto.getC_MA150_偏离率());
            arrDTO.H_MA150_偏离率[i] = of(dto.getH_MA150_偏离率());

            arrDTO.C_MA200_偏离率[i] = of(dto.getC_MA200_偏离率());
            arrDTO.H_MA200_偏离率[i] = of(dto.getH_MA200_偏离率());

            arrDTO.C_MA250_偏离率[i] = of(dto.getC_MA250_偏离率());
            arrDTO.H_MA250_偏离率[i] = of(dto.getH_MA250_偏离率());


            // ---------------------------------------------------


            arrDTO.上影大阴[i] = of(dto.get上影大阴());
            arrDTO.高位爆量上影大阴[i] = of(dto.get高位爆量上影大阴());


            arrDTO.涨停[i] = of(dto.get涨停());
            arrDTO.跌停[i] = of(dto.get跌停());


            // ---------------------------------------------------


            arrDTO.XZZB[i] = of(dto.getXZZB());
            arrDTO.BSQJ[i] = of(dto.getBSQJ());


            // ---------------------------------------------------


            arrDTO.MA5多[i] = of(dto.getMA5多());
            arrDTO.MA5空[i] = of(dto.getMA5空());
            arrDTO.MA10多[i] = of(dto.getMA10多());
            arrDTO.MA10空[i] = of(dto.getMA10空());
            arrDTO.MA20多[i] = of(dto.getMA20多());
            arrDTO.MA20空[i] = of(dto.getMA20空());
            arrDTO.SSF多[i] = of(dto.getSSF多());
            arrDTO.SSF空[i] = of(dto.getSSF空());


            arrDTO.上MA20[i] = of(dto.get上MA20());
            arrDTO.下MA20[i] = of(dto.get下MA20());
            arrDTO.上SSF[i] = of(dto.get上SSF());
            arrDTO.下SSF[i] = of(dto.get下SSF());


            // ---------------------------------------------------


            arrDTO.N60日新高[i] = of(dto.getN60日新高());
            arrDTO.N100日新高[i] = of(dto.getN100日新高());
            arrDTO.历史新高[i] = of(dto.get历史新高());


            arrDTO.百日新高[i] = of(dto.get百日新高());


            // ---------------------------------------------------


            arrDTO.月多[i] = of(dto.get月多());
            arrDTO.均线预萌出[i] = of(dto.get均线预萌出());
            arrDTO.均线萌出[i] = of(dto.get均线萌出());
            arrDTO.小均线多头[i] = of(dto.get小均线多头());
            arrDTO.大均线多头[i] = of(dto.get大均线多头());
            arrDTO.均线大多头[i] = of(dto.get均线大多头());
            arrDTO.均线极多头[i] = of(dto.get均线极多头());


            // ---------------------------------------------------


            arrDTO.RPS红[i] = of(dto.getRPS红());
            arrDTO.RPS一线红[i] = of(dto.getRPS一线红());
            arrDTO.RPS双线红[i] = of(dto.getRPS双线红());
            arrDTO.RPS三线红[i] = of(dto.getRPS三线红());


            // ---------------------------------------------------


            arrDTO.首次三线红[i] = of(dto.get首次三线红());
            arrDTO.口袋支点[i] = of(dto.get口袋支点());


            // ---------------------------------------------------


            arrDTO.klineType[i] = of(dto.getKlineType());


            // ---------------------------------------------------
        }


        return arrDTO;
    }


    // -----------------------------------------------------------------------------------------------------------------


    private static int of(Integer value) {
        return null == value ? 0 : value;
    }

    private static double of(Double value) {
        return null == value ? Double.NaN : value;
    }

    private static boolean of(Boolean value) {
        return null != value && value;
    }


}