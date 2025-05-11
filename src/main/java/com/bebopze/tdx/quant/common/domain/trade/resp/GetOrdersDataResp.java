package com.bebopze.tdx.quant.common.domain.trade.resp;

import lombok.Data;

import java.io.Serializable;


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
    private String Wtsl;
    // 委托状态（未报/已报/已撤/部成/已成/废单）
    private String Wtzt;
    // 委托价格（12.340）
    private String Wtjg;
    // 成交数量（0）
    private String Cjsl;
    // 成交金额（0）
    private String Cjje;


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
    private String Cjjg;
    // 信用交易类型-备注【交易类别】（卖出担保品）
    private String Xyjylbbz;

}