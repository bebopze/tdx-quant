package com.bebopze.tdx.quant.common.domain.dto.trade;

import com.bebopze.tdx.quant.common.constant.StockLimitEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.util.StockUtil;
import lombok.Data;


/**
 * 个股 - 行情快照（批量拉取 全A 实时行情）
 *
 * @author: bebopze
 * @date: 2025/8/3
 */
@Data
public class StockSnapshotKlineDTO extends KlineDTO {


    private String stockCode;

    private String stockName;


    /**
     * 涨跌幅 限制
     */
    private int chgPctLimit;


    /**
     * 昨日 收盘价
     */
    private double prevClose;
    /**
     * 今日 涨停价
     */
    private double ztPrice;
    /**
     * 今日 跌停价
     */
    private double dtPrice;


    /**
     * 是否 涨停
     */
    private boolean ztFlag;
    /**
     * 是否 跌停
     */
    private boolean dtFlag;


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 涨跌幅 限制
     *
     * @return
     */
    public int getChgPctLimit() {
        return StockLimitEnum.getChgPctLimit(stockCode, stockName);
    }


    public double getZtPrice() {
        return StockUtil.ztPrice(prevClose, stockCode, stockName);
    }

    public double getDtPrice() {
        return StockUtil.dtPrice(prevClose, stockCode, stockName);
    }


    public boolean isZtFlag() {
        return getClose() >= ztPrice && ztPrice > 0; // T+0类 ETF（涨跌停价格 东方财富API 返回0）
    }

    public boolean isDtFlag() {
        return getClose() <= dtPrice && dtPrice > 0; // T+0类 ETF（涨跌停价格 东方财富API 返回0）
    }


}