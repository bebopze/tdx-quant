package com.bebopze.tdx.quant.common.domain.dto.topblock;

import lombok.Data;

import java.util.List;


/**
 * 主线个股（主线-月多2）
 *
 * @author: bebopze
 * @date: 2025/9/25
 */
@Data
public class TopStockDTO {


    private String stockCode;

    private String stockName;

    /**
     * 主线个股 上榜天数
     */
    private int days;


    private List<TopBlock> topBlockList;
    private int topBlockSize;


    public int getTopBlockSize() {
        return topBlockList.size();
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    public static class TopBlock {
        private String blockCode;
        private String blockName;

        /**
         * 主线板块 上榜天数
         */
        private int topDays;
    }


}