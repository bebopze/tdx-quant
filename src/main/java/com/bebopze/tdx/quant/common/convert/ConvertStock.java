package com.bebopze.tdx.quant.common.convert;

import com.bebopze.tdx.quant.common.domain.dto.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import lombok.extern.slf4j.Slf4j;

import java.util.List;


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
        }


        return arrDTO;
    }


    public static ExtDataArrDTO dtoList2Arr2(List<ExtDataDTO> dtoList) {
        int size = dtoList.size();

        ExtDataArrDTO arrDTO = new ExtDataArrDTO(size);


        for (int i = 0; i < size; i++) {
            ExtDataDTO dto = dtoList.get(i);


            arrDTO.date[i] = dto.getDate();


            arrDTO.rps10_arr[i] = dto.getRps10();
            arrDTO.rps20_arr[i] = dto.getRps20();
            arrDTO.rps50_arr[i] = dto.getRps50();
            arrDTO.rps120_arr[i] = dto.getRps120();
            arrDTO.rps250_arr[i] = dto.getRps250();


            try {
                arrDTO.ssf_arr[i] = of(dto.getSSF());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            try {
                arrDTO.SSF多[i] = of(dto.getSSF多());
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }

            arrDTO.N日新高[i] = of(dto.getN日新高());
            arrDTO.均线预萌出[i] = of(dto.get均线预萌出());
            arrDTO.均线萌出[i] = of(dto.get均线萌出());
            arrDTO.大均线多头[i] = of(dto.get大均线多头());

            arrDTO.月多[i] = of(dto.get月多());
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