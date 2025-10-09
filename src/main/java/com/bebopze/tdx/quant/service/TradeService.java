package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.constant.TradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.trade.RevokeOrderResultDTO;
import com.bebopze.tdx.quant.common.domain.param.QuickBuyPositionParam;
import com.bebopze.tdx.quant.common.domain.param.TradeBSParam;
import com.bebopze.tdx.quant.common.domain.param.TradeRevokeOrdersParam;
import com.bebopze.tdx.quant.common.domain.trade.resp.GetOrdersDataResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;

import java.util.List;
import java.util.Set;


/**
 * BS接口
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
public interface TradeService {


    /**
     * 我的持仓
     *
     * @param blockInfo 是否返回 板块info：true/false
     * @return
     */
    QueryCreditNewPosResp queryCreditNewPosV2(boolean blockInfo);

    QueryCreditNewPosResp queryCreditNewPosV2();


    /**
     * 实时行情
     *
     * @param stockCode
     * @return
     */
    SHSZQuoteSnapshotResp SHSZQuoteSnapshot(String stockCode);

    /**
     * 下单 - B/S
     *
     * @param param
     * @return
     */
    Integer bs(TradeBSParam param);


    /**
     * 当日委托单 列表
     *
     * @return
     */
    List<GetOrdersDataResp> getOrdersData();

    /**
     * 全部 可撤单列表
     *
     * @return
     */
    List<GetOrdersDataResp> getRevokeList();


    /**
     * 批量撤单
     *
     * @param paramList
     * @return
     */
    List<RevokeOrderResultDTO> revokeOrders(List<TradeRevokeOrdersParam> paramList);


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 一键清仓     =>     先撤单（如果有[未成交]-[卖单]） ->  再全部卖出
     */
    void quickClearPosition();


    // --------------------------------------------


    /**
     * 一键 等比卖出     =>     指定 个股列表
     *
     * @param sellStockCodeSet 指定卖出 个股列表
     * @param sellPosPct       指定卖出 持仓比例（%）
     * @param currPricePct     （当前价格）涨跌幅比例%
     * @param changePricePct   （昨日收盘价）涨跌幅比例%
     */
    void quickSellPosition(Set<String> sellStockCodeSet, double sellPosPct, double currPricePct, double changePricePct);

    /**
     * 一键清仓     =>     指定 个股列表
     *
     * @param clearStockCodeSet 指定清仓 个股列表
     */
    void quickClearPosition(Set<String> clearStockCodeSet);


    /**
     * 一键买入     =>     指定 个股列表          ->          当前 剩余资金 买入（不清仓 -> old）
     *
     * @param newPositionList 指定买入 个股列表
     */
    void quickBuyPosition(List<QuickBuyPositionParam> newPositionList);


    /**
     * 一键买入（调仓换股）    =>     清仓（old） ->  买入（new）
     *
     * @param newPositionList
     */
    void quickClearAndBuyNewPosition(List<QuickBuyPositionParam> newPositionList);


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 一键 等比买入（调仓换股）    =>     清仓（old） ->  买入（new）
     *
     * @param buyStockCodeSet
     */
    void quickClearAndAvgBuyNewPosition(Set<String> buyStockCodeSet);


    /**
     * 一键 等比买入（调仓换股）    =>     降低手续费（保留exist  ->  买入new）
     *
     * @param buyStockCodeSet
     * @param buyPosPct
     * @param currPricePct
     * @param changePricePct
     */
    void keepExistBuyNew(Set<String> buyStockCodeSet, double buyPosPct, double currPricePct, double changePricePct);


    /**
     * 成本估算
     *
     * @param buyStockCodeSet
     * @param buyPosPct
     * @param currPricePct
     * @param changePricePct
     */
    Object buyCost(Set<String> buyStockCodeSet, double buyPosPct, double currPricePct, double changePricePct);


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 总账户（以此刻   融+担 = 净x2   ->   为100%基准）   =>   一键 等比减仓（等比卖出）
     *
     * @param newPositionRate 新仓位
     */
    void totalAccount__eqRatioSellPosition(double newPositionRate);

    /**
     * 当前持仓（以 此刻持仓市值 为100%基准）   =>   一键 等比减仓（等比卖出）
     *
     * @param newPositionRate 新仓位
     */
    void currPos__eqRatioSellPosition(double newPositionRate);


    /**
     * 一键撤单   =>   撤除所有 [未成交 -> 未报/已报/部成] 委托单
     */
    void quickCancelOrder();


    /**
     * 一键再融资（   一键清仓 -> 重置融资     =>     一键融资再买入 -> 一键担保再买入   =>   新剩余 担保资金   ）
     */
    void quickResetFinancing();

    /**
     * 一键取款（   担保比例 >= 300%     ->     隔日 可取款   ）
     *
     * @param transferAmount 取款金额（T+1 隔日7点可取）
     */
    void quickLowerFinancing(double transferAmount);


    // -----------------------------------------------------------------------------------------------------------------


    void quickETF(String stockCode, double priceRangePct, int rangeTotal, double amount, TradeTypeEnum tradeTypeEnum);
}