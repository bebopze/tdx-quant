package com.bebopze.tdx.quant.common.domain.kline;

import com.bebopze.tdx.quant.common.util.NumUtil;
import lombok.Data;


/**
 * 全A行情   -   分页 列表查询                         // 新浪财经
 *
 * @author: bebopze
 * @date: 2025/8/3
 */
@Data
public class StockKlineListSinaResp {


    // {
    //     "symbol": "sz300308",
    //     "code": "300308",
    //     "name": "中际旭创",
    //     "trade": "528.010",
    //     "pricechange": -15.21,
    //     "changepercent": -2.8,
    //     "buy": "527.990",
    //     "sell": "528.000",
    //     "settlement": "543.220",
    //     "open": "537.000",
    //     "high": "558.770",
    //     "low": "525.790",
    //     "volume": 36501620,
    //     "amount": 19834732303,
    //     "ticktime": "13:58:00",
    //     "per": 111.867,
    //     "pb": 22.232,
    //     "mktcap": 58668159.153534,
    //     "nmc": 58371530.950082,
    //     "turnoverratio": 3.30182
    // }


    // ------------------------------------------------------------


    // 交易所 + 股票code（深证：sh/sz/bj + 股票code）
    private String symbol;

    // 股票code
    private String code;

    // 股票name
    private String name;

    // 最新价（Close）
    private double trade;

    // 涨跌额（-15.21）
    private double pricechange;

    // 涨跌幅(%)（-2.8）
    private double changepercent;

    // 买1价（527.990）
    private double buy;

    // 卖1价（528.000）
    private double sell;

    // 昨日收盘价（543.220）
    private double settlement;

    // 开盘价（537.000）
    private double open;

    // 最高价（558.770）
    private double high;

    // 最低价（525.790）
    private double low;

    // 成交量（36501620）
    private long volume;

    // 成交额（元）
    private double amount;

    // 时间戳（13:58:00）
    private String ticktime;

    // 市盈率(动态)（111.867）
    private double per;

    // 市净率(动态)（22.232）
    private double pb;

    // 总市值(万)（58668159.153534）
    private double mktcap;

    // 流通市值(万)（58371530.950082）
    private double nmc;

    // 换手率（3.30182%）
    private double turnoverratio;


    // ------------------------------------------------------------ 自定义 ----------------------------------------------


    // 振幅       (H/L - 1) x 100%
    public double getRangePct() {
        return Double.isNaN(high) || Double.isNaN(low) || high == 0 || low == 0 ?
                0 :
                NumUtil.of((high / low - 1) * 100);
    }

}