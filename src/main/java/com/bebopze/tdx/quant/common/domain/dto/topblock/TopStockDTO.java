package com.bebopze.tdx.quant.common.domain.dto.topblock;

import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.domain.trade.resp.CcStockInfo;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;


/**
 * 主线个股（主线-月多2）
 *
 * @author: bebopze
 * @date: 2025/9/25
 */
@Data
public class TopStockDTO {


    // 当前日期（基准日）
    private LocalDate date;


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


    // -----------------------------------------------------------------------------------------------------------------


    // 上榜日期、涨幅
    TopChangePctDTO changePctDTO;


    // -----------------------------------------------------------------------------------------------------------------


    // 持仓详情
    CcStockInfo ccStockInfo;


    // -----------------------------------------------------------------------------------------------------------------


    // 个股详情（去除 kline_his、ext_data_his）
    BaseStockDO stockDO;


    // 板块 - kline
    KlineDTO klineDTO;


    // 板块 - extData 指标
    ExtDataDTO extDataDTO;


}