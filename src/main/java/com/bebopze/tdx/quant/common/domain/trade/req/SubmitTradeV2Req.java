package com.bebopze.tdx.quant.common.domain.trade.req;

import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import lombok.Data;

import java.math.BigDecimal;


/**
 * 买入（融资/担保） / 卖出
 *
 * @author: bebopze
 * @date: 2025/4/29
 */
@Data
public class SubmitTradeV2Req {


    // stockCode: 588050       股票代码
    // stockName: 科创ETF       股票名称
    // price: 1.031            价格
    // amount: 100             数量
    // tradeType: B            B：买入 / S：卖出
    // xyjylx: 6               担保买入-6; 卖出-7; 融资买入-a;   [融券卖出-A];
    // market: HA              市场（HA：沪A / SA：深A / B：北交所）


    private String stockCode;

    private String stockName;

    // 价格
    private BigDecimal price;
    // 数量（100 x N       ->       150 : 不允许将整股拆成零股来卖）
    private Integer amount;


    // ------------------------------------------- 根据 tradeTypeEnum / stockCode   ->   自动计算 填充


    private String tradeType;

    private String xyjylx;


    private String market;


    // -------------------------------------------


    // 买卖 枚举（自动映射   ->   tradeType、xyjylx）
    private transient TradeTypeEnum tradeTypeEnum;


    // -----------------------------------------------------------------------------------------------


    public String getTradeType() {
        return tradeType != null ? tradeType :
                tradeTypeEnum != null ? tradeTypeEnum.getEastMoneyTradeType() : null;
    }

    public String getXyjylx() {
        return xyjylx != null ? xyjylx :
                tradeTypeEnum != null ? tradeTypeEnum.getXyjylx() : null;
    }


    public String getMarket() {
        if (market == null) {
            // 市场（HA-沪A / SA-深A / B-北交所）
            String market = StockMarketEnum.getEastMoneyMarketByStockCode(stockCode);
            return market == null ? StockMarketEnum.SH.getEastMoneyMarket() : market;
        }
        return market;
    }

}