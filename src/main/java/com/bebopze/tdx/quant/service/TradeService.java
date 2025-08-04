package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.dto.RevokeOrderResultDTO;
import com.bebopze.tdx.quant.common.domain.param.QuickBuyPositionParam;
import com.bebopze.tdx.quant.common.domain.param.TradeBSParam;
import com.bebopze.tdx.quant.common.domain.param.TradeRevokeOrdersParam;
import com.bebopze.tdx.quant.common.domain.trade.resp.GetOrdersDataResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;

import java.util.List;


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
     * @return
     */
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


    /**
     * 一键买入（调仓换股）    =>     清仓（old） ->  买入（new）
     */
    void quickBuyNewPosition(List<QuickBuyPositionParam> newPositionList);

    /**
     * @param newPositionList
     */
    void quickAvgBuyNewPosition(List<QuickBuyPositionParam> newPositionList);


    /**
     * 总账户（以此刻   融+担 = 净x2   ->   为100%基准）   =>   一键 等比减仓（等比卖出）
     *
     * @param newPositionRate 新仓位
     */
    void totalAccount__equalRatioSellPosition(double newPositionRate);

    /**
     * 当前持仓（以 此刻持仓市值 为100%基准）   =>   一键 等比减仓（等比卖出）
     *
     * @param newPositionRate 新仓位
     */
    void currPosition__equalRatioSellPosition(double newPositionRate);


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
     * @param new_marginRate 取款金额（T+1 隔日7点可取）
     */
    void quickLowerFinancing(double new_marginRate);
}