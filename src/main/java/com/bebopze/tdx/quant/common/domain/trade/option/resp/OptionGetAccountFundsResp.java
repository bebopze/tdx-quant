package com.bebopze.tdx.quant.common.domain.trade.option.resp;

import com.bebopze.tdx.quant.common.util.NumUtil;
import lombok.Data;

import java.util.List;


/**
 * ETF期权   -   账户资金（汇总）
 *
 * @author: bebopze
 * @date: 2026/11/9
 */
@Data
public class OptionGetAccountFundsResp {


    //   {
    //     "Status": 0,
    //     "Count": 1,
    //     "Errcode": 0,
    //     "Data": [
    //       {
    //         "Ccyk": "-2890.6060",         // 持仓盈亏
    //         "Dfzj": "0.0000",             //
    //         "Dtzybzj": "0.0000",          //
    //         "Fdyk": "-2890.6060",         // 账户 浮动总盈亏
    //         "Hbdm": "0",
    //         "Khdm": "",                   // 客户代码（信用账户邮箱）
    //         "Nbjg": "5408",
    //         "Sz": "146073.3840",           // 账户 持仓总市值
    //         "Xqsdbzj": "0.0000",
    //         "Xqzjdjje": "0.0000",
    //         "Yjfzlx": "0.0000",
    //         "Zczh": "",                     // 期权账号
    //         "Zczz": "182479.2440",          // 账户总资产
    //         "Zjdjje": "0.0000",
    //         "Zjdtky": "36405.8600",         //
    //         "Zjfydjje": "0.0000",
    //         "Zjjdje": "0.0000",
    //         "Zjjydjje": "0.0000",
    //         "Zjjyjdje": "0.0000",
    //         "Zjjyzcje": "0.0000",
    //         "Zjjyztje": "0.0000",
    //         "Zjkqje": "36404.8600",
    //         "Zjkyje": "36405.8600",         // 可用资金（现金资产/可取金额）
    //         "Zjsjky": "36405.8600",         //
    //         "Zjye": "36405.8600",           //
    //         "Zjzc": "36405.8600",           //
    //         "Zjzrye": "36405.8600",         //
    //         "Zjzt": "0",
    //         "Zybzj": "0.0000",
    //         "ZybzjEX": "0.0000"
    //       }
    //     ]
    //   }


    // 持仓盈亏
    private double Ccyk;
    private double Dfzj;
    private double Dtzybzj;


    // 账户 浮动总盈亏
    private double Fdyk;


    private double Hbdm;


    private String Khdm;
    private String Nbjg;


    // 账户 持仓总市值
    private double Sz;


    private double Xqsdbzj;
    private double Xqzjdjje;
    private double Yjfzlx;
    private double Zczh;


    // 账户总资产
    private double Zczz;


    private double Zjdjje;
    private double Zjdtky;
    private double Zjfydjje;
    private double Zjjdje;
    private double Zjjydjje;
    private double Zjjyjdje;
    private double Zjjyzcje;
    private double Zjjyztje;
    private double Zjkqje;


    // 可用资金（现金资产/可取金额）
    private double Zjkyje;


    private double Zjsjky;
    private double Zjye;
    private double Zjzc;
    private double Zjzrye;
    private double Zjzt;
    private double Zybzj;
    private double ZybzjEX;


    // -----------------------------------------------------------------------------------------------------------------


    // ------------- 自定义 扩展字段


    /**
     * 期权持仓 - 详情列表
     */
    private List<OptionGetGGQQPositionResp> optionPositionList;


    // ------------------------------------ 账户 当日盈亏/当日盈亏率


    // 当日盈亏
    private Double dailyProfit;
    // 当日盈亏比例 = 当日盈亏 / 昨日市值
    private Double dailyProfitRatio;


    // 每日盈亏额（账户持仓汇总，与 curProfit 同口径）
    public double getDailyProfit() {
        double sumCurProfit = optionPositionList.stream().mapToDouble(OptionGetGGQQPositionResp::getDailyProfit).sum();
        return NumUtil.of(sumCurProfit);
    }

    // 每日盈亏率（小数，如 0.0203 = 2.03%）
    public double getDailyProfitRatio() {
        // 昨日收盘总市值  =  昨日收盘价 x 持仓数量 x 合约单位
        double prevMarketValue = optionPositionList.stream().mapToDouble(e -> e.getKlineDTO().getPrevClose() * e.getHysjcc() * e.getHydw()).sum();

        return NumUtil.of(getDailyProfit() / prevMarketValue);
    }


    // --------------------------------- 期权 总盈亏/总盈亏率


    // 总盈亏额（账户持仓盈亏 Ccyk）
    private Double totalProfit;

    public double getTotalProfit() {
        return Ccyk;
    }


    // 账户总成本 = 账户总市值 - 账户持仓盈亏
    private Double totalCost;

    public double getTotalCost() {
        return NumUtil.of(Sz - Ccyk);
    }


    // 账户总盈亏比例  =  账户总盈亏 / 账户总成本
    private Double totalProfitRatio;

    public double getTotalProfitRatio() {
        return NumUtil.of(getTotalProfit() / getTotalCost());
    }


    // ---------------------------------


    /**
     * 期权持仓 总持仓比例 = 持仓总市值 / 账户总资产
     */
    private double totalAccount__actTotalPosRatio;

    public double getTotalAccount__actTotalPosRatio() {
        totalAccount__actTotalPosRatio = NumUtil.of(Sz / Zczz);
        return totalAccount__actTotalPosRatio;
    }


    /**
     * 剩余 可买仓位（%）  =   可用总资金  /  账户总资产
     */
    private double max_buyPosPct;

    public double getMax_buyPosPct() {
        max_buyPosPct = NumUtil.of(Zjkyje / Zczz * 100);
        return max_buyPosPct;
    }

}