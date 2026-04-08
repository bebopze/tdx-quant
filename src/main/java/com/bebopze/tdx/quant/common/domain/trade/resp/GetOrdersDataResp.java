package com.bebopze.tdx.quant.common.domain.trade.resp;

import com.bebopze.tdx.quant.common.constant.StockMarketEnum;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.common.util.StockUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private transient String Gddm;
    // 信用交易类型（6-担保买入; 7-卖出; a-融资买入;   [A-融券卖出];）
    private String Xyjylx; // 历史委托列表 有值
    // -
    private String Dwc;


    // 成交价格（0.000000）
    private double Cjjg;
    // 信用交易类型-备注【交易类别】（买入担保品、融资开仓、卖出担保品）
    private String Xyjylbbz; // 今日委托列表 有值


    // -----------------------------------------------------------------------------------------------------------------


    public String getXyjylbbz() {
        return Xyjylbbz == null ? Xyjylx : Xyjylbbz;
    }

    public String getXyjylx() {
        return Xyjylx == null || Xyjylx.equals("0") ? Xyjylbbz : Xyjylx;
    }


    // -----------------------------------------------------------------------------------------------------------------


    // ------------------------ 历史委托单


    // 委托编号（长）
    private String Htxh;


    // ------------------------ 自定义


    /**
     * 仓位占比（仅 当日委托单有效）
     */
    private Double posPct;
    // 账户净资产（仅 当日委托单有效）
    private double netAsset;


    public Double getPosPct() {
        // 金额
        double amount = getWtje();
        // 仓位占比
        return netAsset == 0 || Double.isNaN(netAsset) ? null : NumUtil.of(amount / netAsset * 100, 2);
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


    // ----------------------------------------------------- 自定义 -----------------------------------------------------


    // 昨日收盘价
    private double prevClosePrice;
    // 今日涨停价
    private double ztPrice;
    // 今日跌停价
    private double dtPrice;


    // 当前收盘价
    private double closePrice;
    // 下一个交易日 涨停价
    private double nextDateZtPrice;
    // 下一个交易日 跌停价
    private double nextDateDtPrice;


    // 东方财富 交易所（SA / HA / B）
    private String market;
    // 东方财富 证券类型 - 代码（ 0-股票 / E-ETF / R-创业板 / W-科创板 / J-北交所 / ... ）
    private String stktype_ex;


    // 证券类型：1-股票；2-ETF；
    private Integer stockType;


    public double getZtPrice() {
        return StockUtil.ztPrice(prevClosePrice, Zqdm, Zqmc);
    }

    public double getDtPrice() {
        return StockUtil.dtPrice(prevClosePrice, Zqdm, Zqmc);
    }

    public double getNextDateZtPrice() {
        return StockUtil.ztPrice(closePrice, Zqdm, Zqmc);
    }

    public double getNextDateDtPrice() {
        return StockUtil.dtPrice(closePrice, Zqdm, Zqmc);
    }


    public String getMarket() {
        return StockMarketEnum.getEastMoneyMarketByStockCode(Zqdm);
    }

    public String getStktype_ex() {
        return StockUtil.stktype_ex(Zqdm, Zqmc);
    }


    public Integer getStockType() {
        return StockTypeEnum.getTypeByStockCode(Zqdm);
    }

}