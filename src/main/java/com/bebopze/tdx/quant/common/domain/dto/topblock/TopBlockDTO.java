package com.bebopze.tdx.quant.common.domain.dto.topblock;

import lombok.Data;

import java.util.List;


/**
 * 主线板块（主线-月多2）
 *
 * @author: bebopze
 * @date: 2025/9/25
 */
@Data
public class TopBlockDTO {


    private String blockCode;

    private String blockName;

    /**
     * 主线板块 上榜天数
     */
    private int topDays;


    private List<TopStock> topStockList;
    private int topStockSize;


    public int getTopStockSize() {
        return topStockList.size();
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    public static class TopStock {
        private String stockCode;
        private String stockName;

        /**
         * 主线个股 上榜天数
         */
        private int topDays;
    }


}