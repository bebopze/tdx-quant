package com.bebopze.tdx.quant.common.domain.dto.topblock;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;


/**
 * -
 *
 * @author: bebopze
 * @date: 2025/9/25
 */
@Data
@AllArgsConstructor
public class TopBlock2DTO {


    private String blockCode;
    private String blockName;

    private int topDay;


    // 当日最强
    private List<TopStockDTO> topStockList;


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    @AllArgsConstructor
    public static class TopStockDTO {
        private String stockCode;
        private String stockName;

        private double rps三线和;
    }


}