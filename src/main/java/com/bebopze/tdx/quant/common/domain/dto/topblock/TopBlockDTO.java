package com.bebopze.tdx.quant.common.domain.dto.topblock;

import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

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


    // 涨停数量
    private int ztCount;
    // 跌停数量
    private int dtCount;


    /**
     * 主线板块 上榜天数
     */
    private int topDays;


    // 主线板块  ->  主线个股 列表
    private List<TopStock> topStockList;
    // 主线个股 数量
    private int topStockSize;


    // -----------------------------------------------------------------------------------------------------------------


    public int getTopStockSize() {
        return topStockList.size();
    }

    public int getZtCount() {
        return CollectionUtils.isEmpty(topStockList) ? 0 : (int) topStockList.stream().filter(TopStock::isZtFlag).count();
    }

    public int getDtCount() {
        return CollectionUtils.isEmpty(topStockList) ? 0 : (int) topStockList.stream().filter(TopStock::isDtFlag).count();
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Data
    public static class TopStock {
        private String stockCode;
        private String stockName;
        private String xueqiuMarket;

        // 是否 涨停/跌停
        private boolean ztFlag;
        private boolean dtFlag;

        /**
         * 主线个股 上榜天数
         */
        private int topDays;


        public String getXueqiuMarket() {
            return StockMarketEnum.getXueqiuMarket(stockCode);
        }
    }


    // -----------------------------------------------------------------------------------------------------------------


    // 涨幅
    private TopChangePctDTO changePctDTO;


    // -----------------------------------------------------------------------------------------------------------------


    // 持仓详情（当前板块 -> 持仓个股 列表）
    // List<CcStockInfo> ccStockInfoList;


    // 持仓板块 汇总统计（板块仓位、盈亏、数量、...）
    // CcBlockInfo ccBlockInfo;


    // -----------------------------------------------------------------------------------------------------------------


    // 板块详情（去除 kline_his、ext_data_his）
    // BaseBlockDO blockDO;


    // 板块 - kline
    // KlineDTO klineDTO;


    // 板块 - extData 指标
    // ExtDataDTO extDataDTO;


}