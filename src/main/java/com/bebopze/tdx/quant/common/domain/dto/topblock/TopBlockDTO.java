package com.bebopze.tdx.quant.common.domain.dto.topblock;

import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.domain.trade.resp.CcStockInfo;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;


/**
 * 主线板块（主线-月多2）
 *
 * @author: bebopze
 * @date: 2025/9/25
 */
@Data
public class TopBlockDTO {


    // 当前日期（基准日）
    private LocalDate date;


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


    // -----------------------------------------------------------------------------------------------------------------


    // 涨幅
    TopChangePctDTO changePctDTO;


    // -----------------------------------------------------------------------------------------------------------------


    // 持仓详情（当前板块 -> 持仓个股 列表）
    List<CcStockInfo> ccStockInfoList;


    // 持仓板块 汇总统计（板块仓位、盈亏、数量、...）
    // CcBlockInfo ccBlockInfo;


    // -----------------------------------------------------------------------------------------------------------------


    // 板块详情（去除 kline_his、ext_data_his）
    BaseBlockDO blockDO;


    // 板块 - kline
    KlineDTO klineDTO;


    // 板块 - extData 指标
    ExtDataDTO extDataDTO;


}