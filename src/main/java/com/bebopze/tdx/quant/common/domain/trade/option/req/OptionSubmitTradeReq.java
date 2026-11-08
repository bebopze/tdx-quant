package com.bebopze.tdx.quant.common.domain.trade.option.req;

import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.constant.option.OptionMarketTypeEnum;
import com.bebopze.tdx.quant.common.constant.option.OptionPriceTypeEnum;
import com.bebopze.tdx.quant.common.constant.option.OptionTradeTypeEnum;
import lombok.Data;


/**
 * ETF期权 - BS交易
 *
 * @author: bebopze
 * @date: 2026/11/7
 */
@Data
public class OptionSubmitTradeReq {


    // Hybh     10012265
    // Amount   1
    // Price    0.0185
    // Zqyw     400
    // Ywxw     130
    // Jybk     15


    // 合约编号
    private String Hybh;

    // 委托数量
    private int Amount;

    // 委托价格
    private double Price;


    // 交易类型（400-买入开仓；401-卖出平仓；402-卖出开仓；403-买入平仓；）
    private int Zqyw;


    // 报价方式（130-LMT限价；131-LOK限价即全成否则撤；132-FAL市价剩转限价；133-FOK市价即全成否则撤；134-FAK市价即成剩撤；）
    private int Ywxw;


    // 交易所（05-深交所；15-上交所；）
    private String Jybk;


    // -------------------------------------------


    // ETF代码（标的证券）
    private transient String etfCode;


    // 交易类型 枚举（自动映射   ->   Zqyw）
    private transient OptionTradeTypeEnum optionTradeTypeEnum;
    // 报价方式 枚举（自动映射   ->   Ywxw）
    private transient OptionPriceTypeEnum optionPriceTypeEnum;
    // 交易所 枚举（自动映射   ->   Jybk）
    private transient OptionMarketTypeEnum optionMarketTypeEnum;


    // -----------------------------------------------------------------------------------------------------------------


    public int getZqyw() {
        return optionTradeTypeEnum.getZqyw();
    }

    public int getYwxw() {
        return optionPriceTypeEnum.getYwxw();
    }


    public String getJybk() {

        StockMarketEnum stockMarketEnum = StockMarketEnum.getByStockCode(etfCode);
        optionMarketTypeEnum = stockMarketEnum == StockMarketEnum.SH ? OptionMarketTypeEnum.SH : OptionMarketTypeEnum.SZ;

        return optionMarketTypeEnum.getJybk();
    }


}