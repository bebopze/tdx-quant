package com.bebopze.tdx.quant.common.domain.dto.backtest;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


/**
 * 买入策略 - result记录
 *
 * @author: bebopze
 * @date: 2025/5/27
 */
@Data
public class BuyStockStrategyResultDTO implements Serializable {


    private Long id;

    private LocalDateTime dateTime;


    private List<String> stockCodeList;


    private List<BuyStockDTO> buyStockDTOList;


    @Data
    public static class BuyStockDTO {

        private Long stockCode;
        private String stockName;


        // 时间
        private LocalDateTime dateTime;
        // 成交价
        private BigDecimal buyPrice;
        // 数量
        private int buyVol;
    }


}
