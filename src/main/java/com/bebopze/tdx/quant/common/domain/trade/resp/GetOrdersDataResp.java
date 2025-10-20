package com.bebopze.tdx.quant.common.domain.trade.resp;

import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalTime;


/**
 * 当日 - 委托单
 *
 * @author: bebopze
 * @date: 2025/5/11
 */
@Data
public class GetOrdersDataResp implements Serializable {


    //   {
    //       "Message": null,
    //       "Status": 0,
    //       "Data": [
    //         {
    //           "Wtrq": "20250512",
    //           "Wtsj": "060613",
    //           "Zqdm": "588050",
    //           "Zqmc": "科创ETF",
    //           "Mmsm": "证券卖出",
    //
    //           "Wtsl": "100",
    //           "Wtzt": "已撤",
    //           "Wtjg": "12.340",
    //           "Cjsl": "0",
    //           "Cjje": "0.00",
    //
    //           "Market": "HA",
    //           "Wtbh": "5418",
    //           "Gddm": "E060000001",
    //           "Xyjylx": "0",
    //           "Dwc": "",
    //
    //           "Cjjg": "0.000000",
    //           "Xyjylbbz": "卖出担保品"
    //         }
    //       ]
    //   }


    // 委托日期（20250512）
    private String Wtrq;
    // 委托时间（060613  ->  06:06:13）
    private String Wtsj;
    // 证券代码（588050）
    private String Zqdm;
    // 证券名称（科创ETF）
    private String Zqmc;
    // 买卖说明【委托方向】（证券卖出）
    private String Mmsm;


    // 委托数量（100）
    private int Wtsl;
    // 委托状态（未报/已报/已撤/部成/已成/废单）
    private String Wtzt;
    // 委托价格（12.340）
    private double Wtjg;
    // 成交数量（0）
    private int Cjsl;
    // 成交金额（0）
    private double Cjje;


    // 交易所（HA）
    private String Market;
    // 委托编号（5418）
    private String Wtbh;
    // 股东代码（E060000001）
    private String Gddm;
    // 0（已撤）
    // 信用交易类型（6-担保买入; 7-卖出; a-融资买入;   [A-融券卖出];）
    private String Xyjylx;
    // -
    private String Dwc;


    // 成交价格（0.000000）
    private double Cjjg;
    // 信用交易类型-备注【交易类别】（卖出担保品）
    private String Xyjylbbz;


    // -----------------------------------------------------------------------------------------------------------------


    // ------------------------ 历史委托单


    // 委托编号（长）
    private String Htxh;


    // ------------------------ 自定义


    /**
     * 仓位占比（仅 当日委托单有效）
     */
    private double posPct;
    // 账户净资产（仅 当日委托单有效）
    private double netAsset;


    public double getPosPct() {
        // 金额
        double amount = getWtje();
        // 仓位占比
        return netAsset == 0 ? Double.NaN : NumUtil.of(amount / netAsset * 100, 2);
    }


    // -----------------------------------------------------------------------------------------------------------------


    // "Wtrq": "20250911",
    // "Wtsj": "144843",


//    public LocalDate getWtrq() {
//        return DateTimeUtil.parseDate_yyyyMMdd(Wtrq);
//    }


    public LocalTime getWtsj() {
        return DateTimeUtil.parseTime_HHmmss(Wtsj);
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 委托金额 = 委托价格 x 委托数量
     *
     * @return
     */
    public double getWtje() {
        return NumUtil.of(Wtjg * Wtsl);
    }


}