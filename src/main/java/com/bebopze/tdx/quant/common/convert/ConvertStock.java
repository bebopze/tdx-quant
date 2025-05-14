package com.bebopze.tdx.quant.common.convert;


import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;

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


    public static void main(String[] args) {

        String kline = "2025-05-13,21.06,21.45,21.97,20.89,8455131,18181107751.03,5.18,2.98,0.62,6.33";


        KlineDTO klineDTO = str2DTO(kline);
        System.out.println(klineDTO);
    }

}