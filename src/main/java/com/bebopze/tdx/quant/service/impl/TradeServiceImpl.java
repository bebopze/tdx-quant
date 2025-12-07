package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.EastMoneyTradeAPI;
import com.bebopze.tdx.quant.client.KlineAPI;
import com.bebopze.tdx.quant.common.cache.PosStockCache;
import com.bebopze.tdx.quant.common.cache.TopBlockCache;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.constant.*;
import com.bebopze.tdx.quant.common.domain.BaseExEnum;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopBlockDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopChangePctDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopStockDTO;
import com.bebopze.tdx.quant.common.domain.dto.trade.RevokeOrderResultDTO;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.domain.param.QuickBuyPositionParam;
import com.bebopze.tdx.quant.common.domain.param.TradeBSParam;
import com.bebopze.tdx.quant.common.domain.param.TradeRevokeOrdersParam;
import com.bebopze.tdx.quant.common.domain.trade.req.QueryCreditHisOrderV2Req;
import com.bebopze.tdx.quant.common.domain.trade.req.RevokeOrdersReq;
import com.bebopze.tdx.quant.common.domain.trade.req.SubmitTradeV2Req;
import com.bebopze.tdx.quant.common.domain.trade.resp.*;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.common.util.SleepUtils;
import com.bebopze.tdx.quant.common.util.StockUtil;
import com.bebopze.tdx.quant.parser.writer.TdxBlockNewReaderWriter;
import com.bebopze.tdx.quant.service.TopBlockService;
import com.bebopze.tdx.quant.service.TradeService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.AccountConst.*;


/**
 * BS（融资账户）
 *
 * @author: bebopze
 * @date: 2025/5/4
 */
@Slf4j
@Service
public class TradeServiceImpl implements TradeService {


    @Autowired
    private PosStockCache posStockCache;

    @Autowired
    private TopBlockService topBlockService;

    @Autowired
    private TopBlockCache topBlockCache;


    @Override
    public QueryCreditNewPosResp queryCreditNewPosV2(boolean blockInfo) {

        QueryCreditNewPosResp resp = EastMoneyTradeAPI.queryCreditNewPosV2();


        // block info
        if (blockInfo) {
            resp.getStocks().parallelStream().forEach(stock -> {

                PosStockCache dto = posStockCache.get(stock.getStkcode());
                // BaseStockDTO baseStockDTO = dto.getBaseStockDTO();
                stock.setBlockInfoDTO(dto.getStockBlockInfoDTO());
                // stock.setPreClose(dto.getBaseStockDTO().getPreClose().doubleValue());
                stock.setPrevClose(PosStockCache.getPrevClose(stock.getStkcode()));
            });


            // 是否主线（S信号）
            fillTopBlockInfo(resp.getStocks());
        }


        return resp;
    }


    private void fillTopBlockInfo(List<CcStockInfo> stocks) {


        LocalDate today = LocalDate.now();


        // ------------------------- 主线板块 列表

        List<TopBlockDTO> topBlockList = topBlockService.topBlockList(today, TopTypeEnum.AUTO.type).getTopBlockDTOList();

        // info列表
        List<TopChangePctDTO> topBlockDataList = topBlockList.stream().map(TopBlockDTO::getChangePctDTO).collect(Collectors.toList());
        // code - 上榜天数
        Map<String, Integer> topBlock__codeCountMap = topBlockList.stream().collect(Collectors.toMap(TopBlockDTO::getBlockCode, TopBlockDTO::getTopDays));


        // ------------------------- 主线个股 列表

        List<TopStockDTO> topStockList = topBlockService.topStockList(today, TopTypeEnum.AUTO.type).getTopStockDTOList();
        // code - topStock
        Map<String, TopStockDTO> code_topStock__Map = Maps.uniqueIndex(topStockList, TopStockDTO::getStockCode);


        // -------------------------------------------------------------------------------------------------------------


        stocks.parallelStream()
              .forEach(stock -> {
                  String stockCode = stock.getStkcode();


                  TopStockDTO topStockDTO = code_topStock__Map.get(stockCode);
                  if (topStockDTO != null) {

                      // 当前 主线个股  ->  主线板块 列表
                      stock.setTopBlockList(topStockDTO.getTopBlockList());
                      stock.setTopStockFlag(true);   // 主线个股

                  } else {

                      // 当前 非主线个股  ->  主线板块 列表（可能属于 主线板块  ->  但是 个股形态 已走弱  ->  跌出 主线个股）
                      List<TopStockDTO.TopBlock> stock__topBlockList = topBlockCache.getTopBlockList(stockCode, topBlockDataList, topBlock__codeCountMap);
                      stock.setTopBlockList(stock__topBlockList);
                      stock.setTopStockFlag(false);   // 非主线个股
                  }


//                  // S信号
//                  List<SellStrategyEnum> strategyEnums = Lists.newArrayList();
//                  strategyEnums.add(SellStrategyEnum.SSF空);
//                  strategyEnums.add(SellStrategyEnum.MA20空);
//                  stock.setSellSignalList(strategyEnums);
              });
    }


    @Override
    public QueryCreditNewPosResp queryCreditNewPosV2() {
        return queryCreditNewPosV2(false);
    }

    @Override
    public Set<String> getPosStockCodeSet(boolean checkLogin) {

        try {
            return queryCreditNewPosV2().getStocks().stream().map(CcStockInfo::getStkcode).collect(Collectors.toSet());

        } catch (BizException e) {

            // 只有“不需要登录”时才吞掉 NOT_LOGIN，否则继续抛
            if (!checkLogin && Objects.equals(BaseExEnum.TREAD_EM_COOKIE_EXPIRED.getCode(), e.getCode())) {
                log.warn("getPosStockCodeSet     >>>     东方财富 - cookie过期，返回空持仓集合");
                return Collections.emptySet();
            }

            throw e;
        }
    }


    @Override
    public SHSZQuoteSnapshotResp SHSZQuoteSnapshot(String stockCode) {
        SHSZQuoteSnapshotResp dto = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
        return dto;
    }


    @Override
    public Integer bs(TradeBSParam param) {

        SubmitTradeV2Req req = convert2Req(param);


        // 委托编号
        Integer wtdh = EastMoneyTradeAPI.submitTradeV2(req);
        return wtdh;
    }


    @Override
    public List<GetOrdersDataResp> getOrdersData() {
        List<GetOrdersDataResp> respList = EastMoneyTradeAPI.getOrdersData();


        // -------------------- 仓位占比
        double netAsset = queryCreditNewPosV2().getNetasset().doubleValue();
        respList.forEach(e -> e.setNetAsset(netAsset));


        // -------------------- 实时行情 / 涨跌停价格（自动计算）
        respList.parallelStream().forEach(e -> {
            // 实时行情
            StockSnapshotKlineDTO klineDTO = KlineAPI.kline(e.getZqdm());

            e.setPrevClosePrice(klineDTO.getPrevClose());
            e.setClosePrice(klineDTO.getClose());
        });


        return respList;
    }


    @Override
    public List<GetOrdersDataResp> queryCreditHisOrderV2(LocalDate startDate, LocalDate endDate) {


        // ---------------------------------- 当日委托单


        List<GetOrdersDataResp> today__ordersDataList = Lists.newArrayList();
        if (DateTimeUtil.between(LocalDate.now(), startDate, endDate)) {
            today__ordersDataList = getOrdersData();
        }


        // ---------------------------------- 历史委托单


        QueryCreditHisOrderV2Req req = new QueryCreditHisOrderV2Req();
        req.setSt(startDate);
        req.setEt(endDate);
        req.setQqhs(500);


        List<GetOrdersDataResp> respList = EastMoneyTradeAPI.queryCreditHisOrderV2(req);


        // ---------------------------------- 历史 + 当日


        if (CollectionUtils.isNotEmpty(today__ordersDataList)) {
            today__ordersDataList.forEach(today -> respList.removeIf(his -> key(his).equals(key(today))));

            respList.addAll(today__ordersDataList);
        }


        return respList.stream()
                       .sorted(Comparator.comparing(GetOrdersDataResp::getWtrq).reversed())
                       .sorted(Comparator.comparing(GetOrdersDataResp::getWtsj).reversed())
                       .collect(Collectors.toList());
    }

    private String key(GetOrdersDataResp e) {
        //  key： 股票代码_委托编号_委托日期_委托时间
        return e.getZqdm() + "_" + e.getWtbh() + "_" + e.getWtrq() + "_" + e.getWtsj();
    }


    @Override
    public List<GetOrdersDataResp> getRevokeList() {


        // 1、查询 全部委托单
        List<GetOrdersDataResp> ordersData = getOrdersData();


        // 2、全部可撤单   ->   [未成交]
        List<GetOrdersDataResp> revokeList = ordersData.stream()
                                                       .filter(e -> {
                                                           // 委托状态（未报/已报/已撤/部成/已成/废单）
                                                           String wtzt = e.getWtzt();

                                                           // 已成交   ->   已撤/已成/废单
                                                           // 未成交   ->   未报/已报/部成
                                                           return "未报".equals(wtzt) || "已报".equals(wtzt) || "部成".equals(wtzt);
                                                       })
                                                       .collect(Collectors.toList());

        return revokeList;
    }


    @Override
    public List<RevokeOrderResultDTO> revokeOrders(List<TradeRevokeOrdersParam> paramList) {


        // 限流用（当日委托单列表 -> map）
        Map<String, String> wtbh_stockCode_Map = Maps.newHashMap();
        getOrdersData().forEach(e -> {
            String wtrq = e.getWtrq();
            String wtbh = e.getWtbh();
            String zqdm = e.getZqdm();

            // 委托日期_委托编号   -   股票代码
            wtbh_stockCode_Map.put(wtrq + "_" + wtbh, zqdm);
        });


        // -------------------------------------------------------------------------------------------------------------


        // 总撤单数
        int size = paramList.size();
        // 1次 N单
        int N = 5;


        List<RevokeOrderResultDTO> dtoList = Lists.newArrayList();
        for (int i = 0; i < size; ) {

            // 1次 N单
            List<TradeRevokeOrdersParam> subParamList = paramList.subList(i, Math.min(i += N, size));


            // 批量撤单
            RevokeOrdersReq req = convert2Req(subParamList);
            List<RevokeOrderResultDTO> resultDTOS = EastMoneyTradeAPI.revokeOrders(req, wtbh_stockCode_Map);
            log.info("revokeOrders     >>>     paramList : {} , resultDTOS : {}", JSON.toJSONString(subParamList), JSON.toJSONString(resultDTOS));


            dtoList.addAll(resultDTOS);
        }


        return dtoList;
    }


// -----------------------------------------------------------------------------------------------------------------


    /**
     * 一键清仓     =>     先撤单（如果有[未成交]-[卖单]） ->  再全部卖出
     */
    @Override
    public void quickClearPosition() {

        // 1、未成交   ->   一键撤单
        quickCancelOrder();


        // 2、我的持仓
        QueryCreditNewPosResp posResp = queryCreditNewPosV2();


        // 3、一键清仓
        quick__clearPosition(posResp.getStocks());
    }


    /**
     * 一键清仓     =>     指定 个股列表
     *
     * @param clearStockCodeSet 指定清仓 个股列表
     */
    @Override
    public void quickClearPosition(Set<String> clearStockCodeSet) {

        // 1、我的持仓
        QueryCreditNewPosResp posResp = queryCreditNewPosV2();


        // 2、从持仓个股中   过滤出   ->   清仓 个股列表
        List<CcStockInfo> sell__stockInfoList = posResp.getStocks().stream().filter(e -> clearStockCodeSet.contains(e.getStkcode())).collect(Collectors.toList());


        // 3、一键清仓
        quick__clearPosition(sell__stockInfoList);
    }

    /**
     * 一键 等比卖出     =>     指定 个股列表
     *
     * @param sellStockCodeSet 指定卖出 个股列表
     * @param sellPosPct       指定卖出 持仓比例
     * @param currPricePct     （当前价格）涨跌幅比例%
     * @param prevPricePct     （昨日收盘价）涨跌幅比例%
     */
    @Override
    public void quickSellPosition(Set<String> sellStockCodeSet,
                                  double sellPosPct,
                                  double currPricePct,
                                  double prevPricePct) {


        // 以 currPricePct 为准
        // currPricePct = currPricePct != 0 ? currPricePct : StockUtil.r1ToR0(changePricePct);


        // -------------------------------------------------------------------------------------------------------------


        // 1、我的持仓
        QueryCreditNewPosResp posResp = queryCreditNewPosV2();


        // 2、从持仓个股中   过滤出   ->   卖出 个股列表
        List<CcStockInfo> sell__stockInfoList = posResp.getStocks().stream().filter(e -> sellStockCodeSet.contains(e.getStkcode())).collect(Collectors.toList());


        // 3、一键减仓
        quick__sellPosition(sell__stockInfoList, sellPosPct, currPricePct, prevPricePct);
    }


    /**
     * 一键买入     =>     指定 个股列表
     *
     * @param newPositionList 指定买入 个股列表
     */
    @Override
    public void quickBuyPosition(List<QuickBuyPositionParam> newPositionList) {


        // 1、check持仓比例、计算持仓数量
        checkAncCalcQty__newPositionList(newPositionList, false);


        // 2、组装   param -> PosResp
        List<CcStockInfo> new__positionList = convert__newPositionList(newPositionList);


        // 3、一键清仓（卖old）
        // quickClearPosition();


        // 4、一键买入（买new）
        quick__buyAgain(new__positionList);


        // 5、有多余金额   ->   补充下单
        quick__buyAgain2(new__positionList);
    }

    private void quick__buyAgain2(List<CcStockInfo> new__positionList) {


    }


    @Override
    public void quickClearAndBuyNewPosition(List<QuickBuyPositionParam> newPositionList) {


        // 拉取 实时行情（全A/ETF）    ->     B参数 补全：name、price、...
        // List<QuickBuyPositionParam> newPositionList = StrategyServiceImpl.convert__newPositionList(buyStockCodeSet);


        // 1、check  持仓比例
        checkAncCalcQty__newPositionList(newPositionList, true);


        // 2、组装   param -> PosResp
        List<CcStockInfo> new__positionList = convert__newPositionList(newPositionList);


        // 3、一键清仓（卖old）
        quickClearPosition();


        // 4、一键买入（买new）
        quick__buyAgain(new__positionList);
    }


    @Override
    public void quickClearAndAvgBuyNewPosition(Set<String> buyStockCodeSet) {
        Assert.notEmpty(buyStockCodeSet, "buyStockCodeSet不能为空");


        // 拉取 实时行情（全A/ETF）    ->     B参数 补全：name、price、...
        List<QuickBuyPositionParam> newPositionList = StrategyServiceImpl.convert__newPositionList(buyStockCodeSet);


        // 等比
        int avgPosPct = 100 / buyStockCodeSet.size();
        newPositionList.forEach(e -> e.setPosPct(avgPosPct));


        // 一键   清仓->买入
        quickClearAndBuyNewPosition(newPositionList);
    }


    @Override
    public void keepExistBuyNew(Set<String> buyStockCodeSet,
                                double buyPosPct,
                                double currPricePct,
                                double prevPricePct) {

        Assert.notEmpty(buyStockCodeSet, "buyStockCodeSet不能为空");


        // 拉取 实时行情（全A/ETF）    ->     B参数 补全：name、price、...
        List<QuickBuyPositionParam> newPositionList = StrategyServiceImpl.convert__newPositionList(buyStockCodeSet, currPricePct, prevPricePct);


        // 等比（buyPosPct  ->  指定 买入总仓位比例  ∈  0~200%）
        double avgPosPct = buyPosPct / newPositionList.size();
        newPositionList.forEach(e -> e.setPosPct(avgPosPct));


        // 一键买入（保留exist  ->  买入new）
        keepExistBuyNew(newPositionList);
    }


    @Override
    public Object buyCost(Set<String> buyStockCodeSet, double buyPosPct, double currPricePct, double prevPricePct) {


        return null;
    }


    /**
     * 一键买入（保留exist  ->  买入new）
     *
     * @param newPositionList
     */
    private void keepExistBuyNew(List<QuickBuyPositionParam> newPositionList) {


        // 待买入
        Map<String, Double> buy__code_posPct_map = newPositionList.stream().collect(Collectors.toMap(QuickBuyPositionParam::getStockCode, QuickBuyPositionParam::getPosPct));
        Map<String, QuickBuyPositionParam> buy__code_entity_map = newPositionList.stream().collect(Collectors.toMap(QuickBuyPositionParam::getStockCode, Function.identity()));


        // 清仓列表
        List<CcStockInfo> clearList = Lists.newArrayList();
        // 减仓列表
        List<CcStockInfo> sellList = Lists.newArrayList();


        // -------------------------------------------------------------------------------------------------------------


        // 我的持仓
        QueryCreditNewPosResp posResp = queryCreditNewPosV2();


        // 过滤   ->   清仓列表、减仓列表、待买入列表
        posResp.getStocks().forEach(e -> {

            String stockCode = e.getStkcode();
            double old_posPct = e.getPosratio().doubleValue() * 100;


            Double new_posPct = buy__code_posPct_map.getOrDefault(stockCode, 0.0);


            // IN待买入（已持有 -> 保留）
            if (new_posPct > 0) {
                new_posPct = new_posPct - old_posPct;

                // 待买入%  <  已持有%
                if (new_posPct < 0) {

                    // 减仓
                    e.setPosratio(BigDecimal.valueOf(-1 * new_posPct));
                    sellList.add(e);

                    // 不再买入 新仓位
                    buy__code_posPct_map.remove(stockCode);

                } else {
                    // 买入新仓位
                    buy__code_posPct_map.put(stockCode, new_posPct);
                }
            }


            // 清仓（NOT IN   ->   待买入）
            else {
                clearList.add(e);
            }
        });


        // -------------------------------------------------------------------------------------------------------------


        // ---------------------------- TODO 特殊处理


        // 移除 ETF（ETF 一律视为手动操作   ->   暂不参与BS策略）
        clearList.removeIf(e -> StockTypeEnum.isETF(e.getStkcode()));
        sellList.removeIf(e -> StockTypeEnum.isETF(e.getStkcode()));


        // ----------------------------


        // TODO   清仓
//        quick__clearPosition(clearList);


        // TODO   减仓
//        quick__clearPosition(sellList);


        // 新买入
        List<QuickBuyPositionParam> newBuyPositionList = Lists.newArrayList();
        buy__code_posPct_map.forEach((stockCode, posPct) -> {
            QuickBuyPositionParam entity = buy__code_entity_map.get(stockCode);


            QuickBuyPositionParam newBuyPosition = new QuickBuyPositionParam();
            newBuyPosition.setStockCode(stockCode);
            newBuyPosition.setStockName(entity.getStockName());
            newBuyPosition.setPosPct(posPct);
            newBuyPosition.setPrice(entity.getPrice());
            newBuyPosition.setQuantity(entity.getQuantity());

            newBuyPositionList.add(newBuyPosition);
        });


        // -------------------------------------------------------------------------------------------------------------


        // 一键买入
        quickBuyPosition(newBuyPositionList);
    }


    @Override
    public void totalAccount__eqRatioSellPosition(double newPositionRate) {
        Assert.isTrue(newPositionRate < 1, String.format("newPositionRate=[%s]必须<1", newPositionRate));


        // 1、我的持仓
        QueryCreditNewPosResp posResp = queryCreditNewPosV2();


        // 2、check     ->     两融账户 等比减仓
        check___totalAccount__equalRatioSellPosition(posResp, newPositionRate);


        // 3、当前持仓 等比减仓比例   =   1  -  new总仓位 / 实际总仓位
        double sellRate = 1 - newPositionRate / posResp.getTotalAccount__actTotalPosRatio();


        // 4、持仓列表  ->  等比减仓
        equalRatio_sellPosition(posResp.getStocks(), sellRate);
    }


    @Override
    public void currPos__eqRatioSellPosition(double newPositionRate) {
        Assert.isTrue(newPositionRate < 1, String.format("positionRate=[%s]必须<1", newPositionRate));


        // 1、我的持仓
        QueryCreditNewPosResp posResp = queryCreditNewPosV2();


        // 2、check     ->     当前持仓 等比减仓
        // check___currPosition__equalRatioSellPosition(posResp, newPositionRate);


        // 3、当前持仓 等比减仓比例   =   1 - new仓位
        double sellRate = 1 - newPositionRate;


        // 4、持仓列表  ->  等比减仓
        equalRatio_sellPosition(posResp.getStocks(), sellRate);
    }


    /**
     * 持仓列表  ->  等比减仓
     *
     * @param positionList 持仓列表
     * @param sellRate     卖出比例
     */
    private void equalRatio_sellPosition(List<CcStockInfo> positionList, double sellRate) {

        positionList.forEach(e -> {


            // --------------------------------------------------


            // 可用数量
            int stkavl = e.getStkavl();
            if (stkavl == 0) {
                log.debug("equalRatio_sellPosition - 忽略     >>>     [{}-{}]可用数量为：{}", e.getStkcode(), e.getStkname(), stkavl);
                return;
            }


            // -------------------------------------------------- 价格精度


            // 个股   ->   价格 2位小数
            // ETF   ->   价格 3位小数
            int scale = priceScale(e.getStktype_ex());


            // --------------------------------------------------


            TradeBSParam param = new TradeBSParam();
            param.setStockCode(e.getStkcode());
            param.setStockName(e.getStkname());
            param.setMarket(e.getMarket());


            // S价格 -> 最低价（买5价 -> 确保100%成交）  =>   C x 99.5%
            BigDecimal price = e.getLastprice().multiply(BigDecimal.valueOf(0.995)).setScale(scale, RoundingMode.HALF_UP);
            param.setPrice(price);


            // ---------- 减仓数量

            // 减仓金额  =  当前市值 x sellRate
            double sell_marketValue = e.getMktval().doubleValue() * sellRate;

            // 减仓数量  =  减仓金额 / 价格
            int qty = (int) (sell_marketValue / price.doubleValue());

            qty = StockUtil.quantity(qty, stkavl);
            param.setAmount(qty);


            // 卖出
            param.setTradeType(TradeTypeEnum.SELL.getTradeType());


            try {

                // 下单 -> 委托编号
                Integer wtbh = bs(param);
                log.info("equalRatio_sellPosition - [卖出]下单SUC     >>>     param : {} , wtbh : {}", JSON.toJSONString(param), wtbh);

            } catch (Exception ex) {
                // SELL 失败
                log.error("equalRatio_sellPosition - [卖出]下单FAIL     >>>     param : {} , errMsg : {}", JSON.toJSONString(param), ex.getMessage(), ex);
            }
        });
    }


    /**
     * check     ->     两融账户 等比减仓
     *
     * @param posResp
     * @param newPositionRate
     */
    private void check___totalAccount__equalRatioSellPosition(QueryCreditNewPosResp posResp, double newPositionRate) {

        // 实际总仓位（融+担）     0.9567123   ->   95.67%
        double actTotalPosRatio = posResp.getTotalAccount__actTotalPosRatio();
        Assert.isTrue(actTotalPosRatio < newPositionRate, String.format("当前两融账户（融+担=净x2）： 实际总仓位=[%s] < new总仓位=[%s] ， 无需减仓！", actTotalPosRatio, newPositionRate));


        // 减仓差值  >=  5%（一次减仓   最少5%）
        double rate_diff = actTotalPosRatio - newPositionRate;
        Assert.isTrue(rate_diff > 0.05, String.format("当前两融账户（融+担=净x2）： 实际总仓位=[%s]，new总仓位=[%s]，减仓比例=[%s]过小，需大于5%%", actTotalPosRatio, newPositionRate, rate_diff));
    }


    @Override
    public void quickCancelOrder() {


        // 1、查询 全部委托单
        List<GetOrdersDataResp> ordersData = getOrdersData();


        // 2、convert   撤单paramList
        List<TradeRevokeOrdersParam> paramList = convert2ParamList(ordersData);


        // 3、批量撤单
        revokeOrders(paramList);


        // 等待成交   ->   1s
        SleepUtils.sleep(1000);
    }


    @Override
    public void quickResetFinancing() {


        // 1、我的持仓
        QueryCreditNewPosResp posResp = queryCreditNewPosV2();


        // 2、预校验
        preCheck__resetFinancing(posResp);


        // TODO   3、入库   =>   异常中断 -> 可恢复
        // save2DB(posResp);
        log.info("quickResetFinancing     >>>     posResp : {}", JSON.toJSONString(posResp));


        // 4、一键清仓
        quickClearPosition();


        // 等待成交   ->   1.5s
        SleepUtils.winSleep(1500);


        // 5、check/retry   =>   [一键清仓]-委托单 状态
        checkAndRetry___clearPosition__OrdersStatus(3);


        // 6、一键再买入
        quick__buyAgain(posResp.getStocks());
    }


    @Override
    public void quickLowerFinancing(double transferAmount) {
        // 担保比例 >= 300%     ->     隔日 可取款


        // 1、我的持仓
        QueryCreditNewPosResp posResp = queryCreditNewPosV2();


        // 2、预校验
        preCheck__lowerFinancing(posResp, transferAmount);


        // 3、新仓位比例
        double currPos_newPositionRate = calcNewPositionRate__quickLowerFinancing(posResp, transferAmount);


        // 4、等比减仓（只涉及到 SELL   ->   无2次重复买入     =>     减免2次BS的 交易费）
        // totalAccount__equalRatioSellPosition(new_actTotalPosRatio);
        currPos__eqRatioSellPosition(currPos_newPositionRate);


        // 5、手动   ->   【现金还款】


        // -------------------------------------------------------------------------------------------------------------


//        // 1、我的持仓
//        QueryCreditNewPosResp posResp = queryCreditNewPosV2();
//
//
//        // 2、预校验  ->  重新 计算分配  new_总市值  ->  计算 new_个股市值（new_数量）
//        QueryCreditNewPosResp new_posResp = preCheck__lowerFinancing(posResp, transferAmount);
//
//
//        // TODO   3、入库   =>   异常中断 -> 可恢复
//        // save2DB(posResp);
//        log.info("quickLowerFinancing     >>>     posResp : {}", JSON.toJSONString(posResp));
//
//
//        // 4、一键清仓
//        quickClearPosition();
//
//
//        // 等待成交   ->   1.5s
//        SleepUtils.winSleep(1500);
//
//
//        // 5、check/retry   =>   [一键清仓]-委托单 状态
//        checkAndRetry___clearPosition__OrdersStatus(3);
//
//
//        // 6、一键再买入
//        quick__buyAgain(new_posResp.getStocks());
    }


// -----------------------------------------------------------------------------------------------------------------


    @Override
    public void quickETF(String stockCode,
                         double priceRangePct,
                         int rangeTotal,
                         double amount,
                         TradeTypeEnum tradeTypeEnum) {


        // 实时行情：买5 / 卖5
        SHSZQuoteSnapshotResp resp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
        String stockName = resp.getName();
        double buy1 = resp.getFivequote().getBuy1();


        // -------------------------------------------------------------------------------------------------------------


        double price = buy1;

        // A股/ETF   ->   价格精度
        int priceScale = StockUtil.priceScale(stockCode);


        // S -> 价格正向（阶梯 加价卖出）；B -> 价格负向（阶梯 降价买入）；
        int sign = tradeTypeEnum.equals(TradeTypeEnum.SELL) ? +1 : -1;


        // -------------------------------------------------------------------------------------------------------------


        // 价格区间   ->   10档
        for (int i = 0; i < rangeTotal; i++) {


            // price = buy1 * (1 ± 0.5% * i);
            // price = buy1 * (1 + sign * priceRangePct * 0.01 * i);


            if (i > 0) {
                // price x (1 ± 0.5%)
                price = price * (1 + sign * priceRangePct * 0.01);
            }
            // System.out.println(i + "   " + price);


            int qty = (int) (amount / price);
            int quantity = StockUtil.quantity(qty, stockName);


            // -----------------------------------------------------------


            TradeBSParam param = new TradeBSParam();
            param.setStockCode(stockCode);
            param.setStockName(stockName);
            param.setPrice(NumUtil.double2Decimal(price, priceScale));
            param.setAmount(quantity);
            param.setTradeType(tradeTypeEnum.getTradeType());


            Integer wtbh = bs(param);


            SleepUtils.randomSleep(500);
        }
    }


// -----------------------------------------------------------------------------------------------------------------


    /**
     * 预校验   =>   担保比例/仓位/负债比例     ->     严格限制 极限仓位 标准
     *
     * @param posResp
     */
    private void preCheck__resetFinancing(QueryCreditNewPosResp posResp) {


        // 总资产 = 净资产 + 总负债 = 总市值 + 可用资金
        BigDecimal totalasset = posResp.getTotalasset();
        // 净资产
        BigDecimal netasset = posResp.getNetasset();
        // 总负债
        BigDecimal totalliability = posResp.getTotalliability();

        // 总市值 = 总资产 - 可用资金
        double totalmkval = posResp.getTotalmkval().doubleValue();
        // 可用资金 = 总资产 - 总市值
        double avalmoney = posResp.getAvalmoney().doubleValue();


        // ---------------------------------------------------


        // 维持担保比例（230.63%）  =   总资产 / 总负债
        double realrate = posResp.getRealrate().doubleValue();
        // 实时担保比例（230.58%）  =   总市值 / 总负债
        BigDecimal marginrates = posResp.getMarginrates();


        // 强制：维持担保比例>200%     =>     否则，一律不准 [极限加仓]
        Assert.isTrue(realrate > 2, String.format("禁止[极限加仓]     >>>     总负债=[%s] , 净资产=[%s]", ofStr(totalliability), ofStr(netasset)));


        // ---------------------------------------------------


        // 总仓位（176.55%）  =   总市值 / 净资产
        double posratio = posResp.getPosratio().doubleValue();


        // 强制：总仓位<200%     =>     否则，一律不准 [极限加仓]
        Assert.isTrue(posratio < 2, String.format("禁止[极限加仓]     >>>     总负债=[%s] , 净资产=[%s]", ofStr(totalliability), ofStr(netasset)));


        // ---------------------------------------------------


        // 总 可用市值（当日 可SELL）  >   总市值 * 95%
        double total__avlMarketValue = posResp.getStocks().stream()
                                              // 可用市值  =  价格 x 可用数量
                                              .map(e -> e.getLastprice().doubleValue() * e.getStkavl())
                                              .reduce(0.0, Double::sum);


//        Assert.isTrue(total__avlMarketValue > totalmkval * 0.95,
//                      String.format("禁止[极限加仓]     >>>     总可用市值=[%s]  <  总市值=[%s] x 95%%", ofStr(total__avlMarketValue), ofStr(totalmkval)));


        // ---------------------------------------------------

        // 总负债 < 净资产
        double rate = totalliability.doubleValue() / netasset.doubleValue();

        // 强制：总负债<净资产     =>     否则，一律不准 [极限加仓]
        Assert.isTrue(rate < 1, String.format("禁止[极限加仓]     >>>     总负债=[%s] , 净资产=[%s]", ofStr(totalliability), ofStr(netasset)));


        // --------------------------------------------------- 交易时间段 限制


        preCheck__tradeTime();
    }


    private void preCheck__lowerFinancing(QueryCreditNewPosResp posResp, double transferAmount) {
        Assert.isTrue(transferAmount >= 50000, String.format("取款金额=[%s]<50000，不够交易费的😶", ofStr(transferAmount)));


        preCheck__tradeTime();


        // --------------------------------------------------- 总资产


        // 总资产  =  净资产 + 总负债  =  总市值 + 可用资金
        // double totalasset = posResp.getTotalasset().doubleValue();


        // ------------ 总资产  =  净资产 + 总负债


        // 净资产
        double netasset = posResp.getNetasset().doubleValue();
        // 总负债
        // double totalliability = posResp.getTotalliability().doubleValue();


        // ------------ 总资产  =  总市值 + 可用资金


        // 总市值  =  总资产 - 可用资金  =  净资产 + 总负债 - 可用资金
        // double totalmkval = posResp.getTotalmkval().doubleValue();
        // 可用资金  =  总资产 - 总市值
        // double avalmoney = posResp.getAvalmoney().doubleValue();


        // --------------------------------------------------- 可取资金


        // 可取资金  =  总资产 - 总负债 x 300% = （总资产 - 总负债） -  总负债 x 200%
        // 可取资金  =  净资产 - 总负债 x 200%
        // double accessmoney = posResp.getAccessmoney().doubleValue();


        // ---------------------------------------------------

        // new_总负债  =  （净资产 - 可取资金）/ 200%

        // double new__totalliability = (netasset - transferAmount) / 2;


        // ---------------------------------------------------


        // 维持担保比例（230.63%）  =   总资产 / 总负债
        // double realrate = posResp.getRealrate().doubleValue();
        // 实时担保比例（230.58%）  =   总市值 / 总负债
        // double marginrates = posResp.getMarginrates().doubleValue();


        // -------------------------------------------------------------------------------------------------------------


        // --------------------------------------------------- transferAmount


        // 强制限制 最大可取额度   ->   净资产 x 10%
        double maxTransferAmount = netasset / 10;

        Assert.isTrue(transferAmount < maxTransferAmount,
                      String.format("[取款金额：%s] > [最大取款金额（净资产x10%%）：%s]", ofStr(transferAmount), ofStr(maxTransferAmount)));


        // --------------------------------------------------- new_融资额度  ->  new_总市值


//        // new_净资产  =  净资产 - 可取资金
//        double new__netasset = netasset - transferAmount;
//
//
//        // new_融资额度（new_总负债）  =  （净资产 - 可取资金）/ 200%
//        // new_融资额度（new_总负债）  =   new_净资产 / 200%
//        double new__totalliability = new__netasset / 2;
//
//
//        // new_总市值  =  new_净资产  +  new_总负债
//        double new__totalmkval = new__netasset + new__totalliability;
//
//
//        // -------------------------------------------------------------------------------------------------------------
//
//
//        // --------------------------------------------------- new_posResp
//
//
//        QueryCreditNewPosResp new_posResp = new QueryCreditNewPosResp();
//        BeanUtils.copyProperties(posResp, new_posResp);
//
//        // new_总负债
//        new_posResp.setTotalliability(of(new__totalliability));
//        // new_总市值
//        new_posResp.setTotalmkval(of(new__totalmkval));
//        // new_总资产 = new_总市值
//        new_posResp.setTotalasset(of(new__totalmkval));
//
//
//        new_posResp.getStocks().forEach(e -> {
//
//
//            // 个股仓位（0.0106592   ->   1.07%）  =   个股市值 / 净资产
//            double posratio = e.getPosratio().doubleValue();
//
//
//            // ----------------------------------
//
//
//            // new_个股市值  =  new_净资产  x  个股仓位
//            double new__mktval = new__netasset * posratio;
//            e.setMktval(of(new__mktval));
//
//
//            // new_个股数量  =  new_个股市值  /  个股价格
//            int qty = (int) (new__mktval / e.getLastprice().doubleValue());
//            e.setStkavl(StockUtil.quantity(qty));
//        });
//
//
//        return new_posResp;
    }


    /**
     * 计算 新仓位比例
     *
     * @param posResp
     * @param transferAmount
     * @return
     */
    private double calcNewPositionRate__quickLowerFinancing(QueryCreditNewPosResp posResp, double transferAmount) {


        // --------------------------------------- old


        // old_净资产
        double old_netasset = posResp.getNetasset().doubleValue();
        // old_总市值
        double old_totalmkval = posResp.getTotalmkval().doubleValue();

        // old_融资负债
        double old_ftotaldebts = posResp.getFtotaldebts().doubleValue();


        // --------------------------------------- new


        // new_净资产  =  old_净资产 - 取款金额
        double new_netasset = old_netasset - transferAmount;


        // new_总市值  =  new_净资产 * 1.5
        double new_totalMarketValue = new_netasset * 1.5;

        // new_负债  =  new_净资产 / 2
        double new_ftotaldebts = new_netasset / 2;


        // min_现金还款  =  old_负债 - new_负债
        double min_repayment = old_ftotaldebts - new_ftotaldebts;


        // ------------------------------------------------------------------------------ 当前持仓


        // new_仓位   =   new_总市值 / old_总市值
        double currPos_newPositionRate = new_totalMarketValue / old_totalmkval;


        // -------------------------------------------------------------------------------------------------------------


        Assert.isTrue(currPos_newPositionRate < 1,
                      String.format("当前[取款=%s] -> [无需减仓] ： 当前[净资产=%s，总市值=%s] ，取款后【净资产=%s，最大总市值=%s】 ， 将当前[负债=%s -降低至-> %s] -> [现金还款=%s]即可取款",
                                    transferAmount, old_netasset, old_totalmkval, new_netasset, new_totalMarketValue, old_ftotaldebts, new_ftotaldebts, of(min_repayment)));


        // -------------------------------------------------------------------------------------------------------------


        // --------------------------------------- 总仓位（融+单 = 净x2）


        double totalAccount__oldPositionRate = posResp.getTotalAccount__actTotalPosRatio();
        double totalAccount__newPositionRate = new_totalMarketValue / posResp.getTotalAccount__actTotalMoney();

        double currPos_newPositionRate_2 = totalAccount__newPositionRate / totalAccount__oldPositionRate;


        // Assert.isTrue(TdxFunCheck.equals(currPos_newPositionRate, currPos_newPositionRate_2),
        //               String.format("newPositionRate=%s, newPositionRate_2=%s", currPos_newPositionRate, currPos_newPositionRate_2));


        // -------------------------------------------------------------------------------------------------------------


        // return totalAccount__newPositionRate;     ->     totalAccount__equalRatioSellPosition(newPositionRate);
        return currPos_newPositionRate;           // ->     currPosition__equalRatioSellPosition(newPositionRate);
    }


    /**
     * 交易时间段 限制         9:35 - 11:29  /  13:00 - 14:56
     */
    private static void preCheck__tradeTime() {

        LocalTime now = LocalTime.now();


        //  9:35 - 11:29
        LocalTime start_1 = LocalTime.of(9, 35);
        LocalTime end_1 = LocalTime.of(11, 29);

        // 13:00 - 14:56
        LocalTime start_2 = LocalTime.of(13, 00);
        LocalTime end_2 = LocalTime.of(14, 56);


        Assert.isTrue(DateTimeUtil.between(now, start_1, end_1) || DateTimeUtil.between(now, start_2, end_2),
                      String.format("当前时间:[%s]非交易时间", now.format(DateTimeUtil.HH_mm_ss)));
    }


    private void ___preCheck__lowerFinancing(QueryCreditNewPosResp posResp,
                                             double transferAmount,
                                             double new_marginRate) {


        Assert.isTrue(transferAmount > 50000, String.format("取款金额=[%s]<50000，不够交易费的😶", transferAmount));


        // --------------------------------------------------- 总资产


        // 总资产  =  净资产 + 总负债  =  总市值 + 可用资金
        double totalasset = posResp.getTotalasset().doubleValue();


        // ------------ 总资产  =  净资产 + 总负债


        // 净资产
        double netasset = posResp.getNetasset().doubleValue();
        // 总负债
        double totalliability = posResp.getTotalliability().doubleValue();


        // ------------ 总资产  =  总市值 + 可用资金


        // 总市值  =  总资产 - 可用资金  =  净资产 + 总负债 - 可用资金
        double totalmkval = posResp.getTotalmkval().doubleValue();
        // 可用资金  =  总资产 - 总市值
        double avalmoney = posResp.getAvalmoney().doubleValue();


        // --------------------------------------------------- 可取资金


        // 可取资金  =  总资产 - 总负债 x 300% = （总资产 - 总负债） -  总负债 x 200%
        // 可取资金  =  净资产 - 总负债 x 200%
        double accessmoney = posResp.getAccessmoney().doubleValue();


        // ---------------------------------------------------

        // 总负债  =  （净资产 - 可取资金）/ 200%

        double new__totalliability = (netasset - transferAmount) / 2;


        // ---------------------------------------------------


        // 维持担保比例（230.63%）  =   总资产 / 总负债
        double realrate = posResp.getRealrate().doubleValue();
        // 实时担保比例（230.58%）  =   总市值 / 总负债
        double marginrates = posResp.getMarginrates().doubleValue();


        // 强制：维持担保比例>200%     =>     否则，一律不准 [极限加仓]
        Assert.isTrue(realrate > 2, String.format("禁止[极限加仓]     >>>     总负债=[%s] , 净资产=[%s]", totalliability, netasset));


        // -------------------------------------------------------------------------------------------------------------


        // 强制限制 最大可取额度   ->   净资产 x 10%
        double maxTransferAmount = netasset / 10;
        Assert.isTrue(transferAmount > maxTransferAmount,
                      String.format("[取款金额：%s] > [最大取款金额：%s]  ->  [净资产：%s] / 10", transferAmount, maxTransferAmount, netasset));


        // --------------------------------------------------- new_marginRate


        // --------------------------------------------------- 降低 实时担保比例     =>     new_实时担保比例 ↓   ->   计算 new_总负债 ↓


        // 总负债  =  总市值 / 实时担保比例 = （净资产 + 可用 + 总负债）  / 实时担保比例
        // 总负债  =  (净资产 + 可用) ÷ (实时担保比例 – 1)
        totalliability = (netasset + avalmoney) / (new_marginRate - 1);


        // -------------------------------------------------------------------------------------------------------------


        // --------------------------------------------------- transferAmount


        // ---------------------------------------------------


        // 总负债  =  总市值 / 实时担保比例 = （净资产 + 总负债）  / 实时担保比例
        // 总负债  =  (净资产 - 取款金额) ÷ (实时担保比例 – 1)
        totalliability = (netasset - transferAmount) / (new_marginRate - 1);


        // ---------------------------------------------------


        // 总仓位（176.55%）  =   总市值 / 净资产
        double posratio = posResp.getPosratio().doubleValue();


        // 强制：总仓位<200%     =>     否则，一律不准 [极限加仓]
        Assert.isTrue(posratio < 2, String.format("禁止[极限加仓]     >>>     总负债=[%s] , 净资产=[%s]", totalliability, netasset));


        // ---------------------------------------------------

        // 总负债 < 净资产
        double rate = totalliability / netasset;

        // 强制：总负债<净资产     =>     否则，一律不准 [极限加仓]
        Assert.isTrue(rate < 1, String.format("禁止[极限加仓]     >>>     总负债=[%s] , 净资产=[%s]", totalliability, netasset));


        // --------------------------------------------------- 交易时间段 限制


        LocalTime now = LocalTime.now();

        //  9:35 ~ 11:29
        LocalTime start_1 = LocalTime.of(9, 35);
        LocalTime end_1 = LocalTime.of(11, 29);

        // 13:00 ~ 14:56
        LocalTime start_2 = LocalTime.of(13, 00);
        LocalTime end_2 = LocalTime.of(14, 56);


        Assert.isTrue(DateTimeUtil.between(now, start_1, end_1) || DateTimeUtil.between(now, start_2, end_2),
                      String.format("当前时间:[%s]非交易时间", now.format(DateTimeUtil.HH_mm_ss)));
    }


    /**
     * 一键清仓
     *
     * @param sellStockInfoList 清仓 个股列表
     */
    private void quick__clearPosition(List<CcStockInfo> sellStockInfoList) {
        quick__sellPosition(sellStockInfoList, 100.0, 0.0, 0.0);
    }

    /**
     * 一键减仓/清仓
     *
     * @param sellStockInfoList 卖出 个股列表
     * @param sellPosPct        卖出 持仓比例（%）
     * @param currPricePct      （当前价格）涨跌幅比例%
     * @param prevPricePct      （昨日收盘价）涨跌幅比例%
     */
    private void quick__sellPosition(List<CcStockInfo> sellStockInfoList,
                                     double sellPosPct,
                                     double currPricePct,
                                     double prevPricePct) {

        sellStockInfoList.forEach(e -> {


            String stockCode = e.getStkcode();


            Integer stkavl = e.getStkavl();
            // 当日 新买入   ->   忽略
            if (stkavl == 0) {
                log.debug("quick__clearPosition - 当日[新买入]/当日[已挂单] -> 忽略     >>>     stock : [{}-{}]", stockCode, e.getStkname());
                return;
            }


            // -------------------------------------------------- 价格精度

            // 个股   ->   价格 2位小数
            // ETF   ->   价格 3位小数
            int scale = priceScale(e.getStktype_ex());


            // -------------------------------------------------- 卖出数量（sellPct）


            // 卖出数量  =  持仓数量 x sellPct
            int sellQty = (int) (e.getStkbal() * sellPosPct * 0.01);

            // 取整百
            sellQty = StockUtil.quantity(sellQty, stockCode);

            // 卖出数量  <=  可用数量
            sellQty = Math.min(sellQty, stkavl);


            // --------------------------------------------------


            TradeBSParam param = new TradeBSParam();
            param.setStockCode(stockCode);
            param.setStockName(e.getStkname());
            param.setMarket(e.getMarket());


            // S价格策略
            double _price = sellPriceStrategyPct(e, currPricePct, prevPricePct);
            // S价格 -> 最低价（买5价 -> 确保100%成交）  =>   C x 99.5%
            BigDecimal price = BigDecimal.valueOf(_price).multiply(BigDecimal.valueOf(0.995)).setScale(scale, RoundingMode.HALF_UP);
            param.setPrice(price);

            // S数量（S  ->  持仓数量 x sellPct）
            param.setAmount(sellQty);

            // 卖出
            param.setTradeType(TradeTypeEnum.SELL.getTradeType());


            try {

                // 下单 -> 委托编号
                Integer wtbh = bs(param);
                log.info("quick__clearPosition - [卖出]下单SUC     >>>     param : {} , wtbh : {}", JSON.toJSONString(param), wtbh);

            } catch (Exception ex) {
                // SELL 失败
                log.error("quick__clearPosition - [卖出]下单FAIL     >>>     param : {} , errMsg : {}", JSON.toJSONString(param), ex.getMessage(), ex);


                String errMsg = ex.getMessage();


//                // TODO   委托价格超过跌停价格
//                if (errMsg.contains("委托价格超过跌停价格")) {
//
//                    e.setZtPrice(e.getLastprice());
//                    e.setDtPrice(e.getLastprice());
//
//
//                    SHSZQuoteSnapshotResp resp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
//                    e.setZtPrice(resp.getTopprice());
//                    e.setDtPrice(resp.getBottomprice());
//                } else


                // 下单异常：委托价格超过涨停价格
                if (errMsg.contains("委托价格超过涨停价格")) {
                    // 清仓价甩卖   ->   不会发生
                }
                // 下单异常：当前时间不允许做该项业务
                else if (errMsg.contains("当前时间不允许做该项业务")) {
                    // 盘后交易   ->   不会发生
                } else {

                }


                // [ERROR] 2025-11-03 08:17:12.738 [http-nio-7001-exec-2] TradeServiceImpl - 担保再买入 - [担保买入]   =>   下单FAIL     >>>     stock : [300175-ST朗源] , posStock : {"costprice":"0","curProfitratio":"0","curprofit":"0","dtPrice":"0","inTopBlock":"false","isApplyStg":"0","lastprice":"6.389","market":"","mktval":"5750.084","needGyjg":"0","needJzd":"0","posratio":"0","preClose":"6.389","profit":"0","profitratio":"0","rzDebt":"","stkRzBal":"","stkZyBal":"0","stkavl":"900","stkbal":"0","stkclasses":"","stkcode":"300175","stkname":"ST朗源","stktype":"","stktype_ex":"0","topBlockList":[],"topStock":"false","ztPrice":"0"} , param : {"amount":"900","market":"","price":"6.42","stockCode":"300175","stockName":"ST朗源","tradeType":"2"} , errMsg : 下单异常：该证券账户[0606068888]无此权限[04 开通风险警示证券买入权限]
                //com.bebopze.tdx.quant.common.config.BizException: 下单异常：该证券账户[0606068888]无此权限[04 开通风险警示证券买入权限]
            }

        });
    }


    /**
     * S价格策略
     *
     * @param e
     * @param currPricePct 按当前价格涨跌幅%
     * @param prevPricePct 按昨日收盘价涨跌幅%
     * @return
     */
    private double sellPriceStrategyPct(CcStockInfo e, double currPricePct, double prevPricePct) {
        if (prevPricePct != 0) {
            // 昨日收盘价
            return e.getPrevClose() * (1 + prevPricePct * 0.01);
        }
        // 当前价格
        return e.getLastprice().doubleValue() * (1 + currPricePct * 0.01);
    }


    /**
     * 一键再买入
     *
     * @param positionList
     */
    private void quick__buyAgain(List<CcStockInfo> positionList) {


        // 仓位占比 倒序
        List<CcStockInfo> sort__positionList = positionList.stream()
                                                           .sorted(Comparator.comparing(CcStockInfo::getMktval).reversed())
                                                           .collect(Collectors.toList());


        // --------------------------------------------------


        // 融资买入 -> SUC
        Set<String> rzSucCodeList = Sets.newHashSet();

        // 融资买入 -> FAIL  =>  待 担保买入
        Set<String> rzFailCodeList = Sets.newHashSet();


        // --------------------------------------------------------------------


        // ------------------------------ 1、融资再买入

        // 融资买
        buy_rz(sort__positionList, rzSucCodeList, rzFailCodeList);


        // ------------------------------ 2、担保再买入


        // 担保买
        buy_zy(sort__positionList, rzSucCodeList, rzFailCodeList);


        // ------------------------------ 3、新空余 担保资金


        QueryCreditNewPosResp bsAfter__posResp = queryCreditNewPosV2();

        // 可用资金
        BigDecimal avalmoney = bsAfter__posResp.getAvalmoney();


        log.info("quick__buyAgain     >>>     avalmoney : {} , bsAfter__positionList : {}", avalmoney, JSON.toJSONString(sort__positionList));
    }


    /**
     * 融资再买入
     *
     * @param sort__positionList
     * @param rzSucCodeList      融资买入 -> SUC
     * @param rzFailCodeList     融资买入 -> FAIL  =>  待 担保买入
     */
    private void buy_rz(List<CcStockInfo> sort__positionList,

                        Set<String> rzSucCodeList,
                        Set<String> rzFailCodeList) {


        sort__positionList.forEach(e -> {


            String stockCode = e.getStkcode();


            // -------------------------------------------------- 价格精度


            // 个股   ->   价格 2位小数
            // ETF   ->   价格 3位小数
            int scale = priceScale(e.getStktype_ex());


            // -------------------------------------------------- 融资买入 - 参数


            TradeBSParam param = new TradeBSParam();
            param.setStockCode(stockCode);
            param.setStockName(e.getStkname());
            param.setMarket(e.getMarket());


            // B价格 -> 最高价（卖5价 -> 确保100%成交）  =>   C x 100.5%
            BigDecimal price = e.getLastprice().multiply(BigDecimal.valueOf(1.005)).setScale(scale, RoundingMode.HALF_UP);
            // BigDecimal test_price = e.getLastprice().multiply(BigDecimal.valueOf(0.95)).setScale(scale, RoundingMode.HALF_UP);
            param.setPrice(price);

            // 数量（B数量 = S数量 -> 可用数量）
            param.setAmount(StockUtil.quantity(e.getStkavl(), stockCode));
            // 融资买入
            param.setTradeType(TradeTypeEnum.RZ_BUY.getTradeType());


            // -------------------------------------------------- 融资买入


            try {

                // 下单  ->  委托编号
                Integer wtbh = bs(param);
                log.info("[融资买入]-下单SUC     >>>     param : {} , wtbh : {}", JSON.toJSONString(param), wtbh);


                // 融资买入 -> SUC
                if (wtbh != null) {
                    rzSucCodeList.add(stockCode);
                } else {
                    rzFailCodeList.add(stockCode);
                }


            } catch (Exception ex) {


//                // TODO   下单异常：
//
//                // !!证券停牌!!
//
//                // 证券非交易所融资融券标的证券
//                // 证券非融资标的证券
//
//
//                // 委托价格超过涨停价格
//                // 委托价格超过跌停价格
//
//                // 保证金可用余额不足,尚需1234.56
//
//                String errMsg = ex.getMessage();
//                if (errMsg.contains("委托价格超过涨停价格")) {
//
//                    e.setZtPrice(e.getLastprice());
//                    e.setDtPrice(e.getLastprice());
//
//
//                    SHSZQuoteSnapshotResp resp = EastMoneyTradeAPI.SHSZQuoteSnapshot(stockCode);
//                    e.setZtPrice(resp.getTopprice());
//                    e.setDtPrice(resp.getBottomprice());
//                }


                // 非融资类 个股     ->     只支持 担保买入
                rzFailCodeList.add(stockCode);


                log.error("[融资买入]-下单FAIL     >>>     param : {} , errMsg : {}", JSON.toJSONString(param), ex.getMessage(), ex);
            }
        });
    }


    /**
     * 担保再买入
     *
     * @param sort__positionList
     * @param rzSucCodeList      融资买入 -> SUC
     * @param rzFailCodeList     融资买入 -> FAIL  =>  待 担保买入
     */
    private void buy_zy(List<CcStockInfo> sort__positionList,

                        Set<String> rzSucCodeList,
                        Set<String> rzFailCodeList) {


        List<CcStockInfo> FAIL_LIST = Lists.newArrayList();


        // --------------------------------------------------------------------------


        sort__positionList.forEach(e -> {


            String stockCode = e.getStkcode();


            // -------------------------------------------------- 价格精度


            // 个股   ->   价格 2位小数
            // ETF   ->   价格 3位小数
            int scale = priceScale(e.getStktype_ex());


            // -------------------------------------------------- 融资买入 - 参数


            // 已融资买入
            if (rzSucCodeList.contains(stockCode)) {
                log.info("担保再买入 - 忽略   =>   已[融资买入] SUC     >>>     stock : [{}-{}] , posStock : {}",
                         stockCode, e.getStkname(), JSON.toJSONString(e));
                return;
            }


//            // 待 担保买入  ->  NOT
//            if (!rzFailCodeList.contains(stockCode)) {
//                log.error("担保再买入 - err     >>>     stock : [{}-{}] , posStock : {}",
//                          stockCode, e.getStkname(), JSON.toJSONString(e));
//                return;
//            }


            // -------------------------------------------------- 担保买入 - 参数


            log.info("担保再买入 - [担保买入]   =>   下单start     >>>     stock : [{}-{}] , posStock : {}",
                     stockCode, e.getStkname(), JSON.toJSONString(e));


            TradeBSParam param = new TradeBSParam();
            param.setStockCode(e.getStkcode());
            param.setStockName(e.getStkname());
            param.setMarket(e.getMarket());


            // B价格 -> 最高价（卖5价 -> 确保100%成交）  =>   C x 100.5%
            BigDecimal price = e.getLastprice().multiply(BigDecimal.valueOf(1.005)).setScale(scale, RoundingMode.HALF_UP);
            // BigDecimal test_price = e.getLastprice().multiply(BigDecimal.valueOf(0.9)).setScale(scale, RoundingMode.HALF_UP);
            param.setPrice(price);

            // 数量（B数量 = S数量 -> 可用数量）
            param.setAmount(StockUtil.quantity(e.getStkavl(), stockCode));
            // 担保买入
            param.setTradeType(TradeTypeEnum.ZY_BUY.getTradeType());


            // -------------------------------------------------- 担保买入


            try {

                // 委托编号
                Integer wtbh = bs(param);

                log.info("担保再买入 - [担保买入]   =>   下单SUC     >>>     stock : [{}-{}] , posStock : {} , param : {} , wtbh : {}",
                         stockCode, e.getStkname(), JSON.toJSONString(e), JSON.toJSONString(param), wtbh);


            } catch (Exception ex) {

                FAIL_LIST.add(e);

                log.error("担保再买入 - [担保买入]   =>   下单FAIL     >>>     stock : [{}-{}] , posStock : {} , param : {} , errMsg : {}",
                          stockCode, e.getStkname(), JSON.toJSONString(e), JSON.toJSONString(param), ex.getMessage(), ex);
            }
        });


        // TODO     FAIL_LIST -> retry
        if (CollectionUtils.isNotEmpty(FAIL_LIST)) {

            log.error("担保再买入 - [担保买入]   =>   下单FAIL     >>>     FAIL_LIST : {}", JSON.toJSONString(FAIL_LIST));


            // handle__FAIL_LIST(FAIL_LIST);
            // DBB-FAIL（担保B-FAIL）
            TdxBlockNewReaderWriter.write("DBB-FAIL", FAIL_LIST.stream().map(CcStockInfo::getStkcode).collect(Collectors.toList()));
        }
    }


    private boolean buyAgain__preCheck() {


        // 1、当前持仓
        QueryCreditNewPosResp now__posResp = queryCreditNewPosV2();


        now__posResp.getStocks().forEach(e -> {
            // 可用数量
            Integer stkavl = e.getStkavl();
            if (stkavl > 0) {

            }
        });


        // 总市值
        double totalmkval = now__posResp.getTotalmkval().doubleValue();
        if (totalmkval == 1000) {
            return true;
        }


        // 总仓位     2.3567123   ->   235.67%
        double posratio = now__posResp.getPosratio().doubleValue();
        // 总仓位<5%
        if (posratio <= 0.05) {
            return true;
        }


        // 2、check   ->   全部[卖单]->[已成交]
        List<CcStockInfo> stocks = now__posResp.getStocks();
        if (CollectionUtils.isEmpty(stocks)) {
            return true;
        }


        log.warn("quick__buyAgain  -  check SELL委托单     >>>     {}", JSON.toJSONString(stocks));


        return true;
    }


    /**
     * check/retry   =>   [一键清仓]-委托单 状态
     *
     * @param retry 最大重试次数
     */
    private void checkAndRetry___clearPosition__OrdersStatus(int retry) {
        if (--retry < 0) {
            return;
        }


        // 1、全部委托单
        List<GetOrdersDataResp> ordersData = getOrdersData();


        // 2、check
        boolean flag = true;
        for (GetOrdersDataResp e : ordersData) {

            // 委托状态（未报/已报/已撤/部成/已成/废单）
            String wtzt = e.getWtzt();


            // 已成交   ->   已撤/已成/废单
            // 未成交   ->   未报/已报/部成
            if ("未报".equals(wtzt) || "已报".equals(wtzt) || "部成".equals(wtzt)) {
                flag = false;
                break;
            }
        }


        // --------------------------------


        // wait
        SleepUtils.winSleep();


        // --------------------------------


        // 存在   [未成交]-[SELL委托单]   ->   retry
        if (!flag) {

            // 先撤单 -> 再全部卖出
            quickClearPosition();

            // 再次 check
            checkAndRetry___clearPosition__OrdersStatus(retry);
        }
    }


    /**
     * convert   撤单paramList
     *
     * @param ordersData
     * @return
     */
    private List<TradeRevokeOrdersParam> convert2ParamList(List<GetOrdersDataResp> ordersData) {

        // 2、convert   撤单paramList
        List<TradeRevokeOrdersParam> paramList = Lists.newArrayList();
        ordersData.forEach(e -> {


            // 委托状态（未报/已报/已撤/部成/已成/废单）
            String wtzt = e.getWtzt();


            // 过滤  ->  已成/已撤/废单
            if ("已成".equals(wtzt) || "已撤".equals(wtzt) || "废单".equals(wtzt)) {
                return;
            }


            log.warn("quick__cancelOrder - [未成交]->[撤单]     >>>     stock : [{}-{}] , wtbh : {} , wtzt : {} , order : {}",
                     e.getZqdm(), e.getZqmc(), e.getWtbh(), wtzt, JSON.toJSONString(e));


            // -----------------------------------------


            TradeRevokeOrdersParam param = new TradeRevokeOrdersParam();
            // 日期（20250511）
            param.setDate(e.getWtrq());
            // 委托编号
            param.setWtdh(e.getWtbh());


            paramList.add(param);
        });


        return paramList;
    }


// -----------------------------------------------------------------------------------------------------------------


    /**
     * 股票价格 精度     ->     A股-2位小数；ETF-3位小数；
     *
     * @param stktypeEx
     * @return
     */
    private int priceScale(String stktypeEx) {

        // ETF   ->   价格 3位小数
        int scale = 2;


        if (stktypeEx.equals("E")) {
            scale = 3;
        } else {
            // 个股   ->   价格 2位小数
            scale = 2;
        }

        return scale;
    }


// -----------------------------------------------------------------------------------------------------------------


    /**
     * 下单 -> B/S
     *
     * @param param
     * @return
     */
    private SubmitTradeV2Req convert2Req(TradeBSParam param) {

        SubmitTradeV2Req req = new SubmitTradeV2Req();
        req.setStockCode(param.getStockCode());
        req.setStockName(param.getStockName());
        req.setPrice(param.getPrice().doubleValue());
        req.setAmount(param.getAmount());


        // B/S
        TradeTypeEnum tradeTypeEnum = TradeTypeEnum.getByTradeType(param.getTradeType());
        req.setTradeType(tradeTypeEnum.getEastMoneyTradeType());
        req.setXyjylx(tradeTypeEnum.getXyjylx());


        // 市场（HA-沪A / SA-深A / B-北交所）
        String market = param.getMarket() != null ? param.getMarket() : StockMarketEnum.getEastMoneyMarketByStockCode(param.getStockCode());
        req.setMarket(market == null ? StockMarketEnum.SH.getEastMoneyMarket() : market);


        req.setTradeTypeEnum(tradeTypeEnum);

        return req;
    }


    /**
     * 撤单
     *
     * @param paramList
     * @return
     */
    private RevokeOrdersReq convert2Req(List<TradeRevokeOrdersParam> paramList) {
        List<String> revokeList = Lists.newArrayList();


        for (TradeRevokeOrdersParam param : paramList) {

            // 委托日期
            String date = StringUtils.isEmpty(param.getDate()) ? DateTimeUtil.format_yyyyMMdd(LocalDate.now()) : param.getDate();

            // 委托日期_委托编号
            String revoke = date + "_" + param.getWtdh();

            revokeList.add(revoke);
        }


        RevokeOrdersReq req = new RevokeOrdersReq();
        req.setRevokes(String.join(",", revokeList));

        return req;
    }


// -----------------------------------------------------------------------------------------------------------------


    /**
     * param -> CcStockInfo
     *
     * @param newPositionList
     * @return
     */
    private List<CcStockInfo> convert__newPositionList(List<QuickBuyPositionParam> newPositionList) {


        return newPositionList.stream().map(e -> {
                                  CcStockInfo stockInfo = new CcStockInfo();


                                  //   TradeBSParam param = new TradeBSParam();
                                  //   param.setStockCode(stockCode);
                                  //   param.setStockName(e.getStkname());
                                  //
                                  //   // B价格 -> 最高价（卖5价 -> 确保100%成交）  =>   C x 100.5%
                                  //   BigDecimal price = e.getLastprice().multiply(BigDecimal.valueOf(1.005)).setScale(scale, RoundingMode.HALF_UP);
                                  //   param.setPrice(price);
                                  //
                                  //   // 数量（B数量 = S数量 -> 可用数量）
                                  //   param.setAmount(StockUtil.quantity(e.getStkavl()));
                                  //   // 融资买入
                                  //   param.setTradeType(TradeTypeEnum.RZ_BUY.getTradeType());


                                  stockInfo.setStkcode(e.getStockCode());
                                  stockInfo.setStkname(e.getStockName());

                                  // 价格
                                  stockInfo.setLastprice(of(e.getPrice()));
                                  // 数量
                                  stockInfo.setStkavl(e.getQuantity());


                                  // 股票/ETF   ->   计算 price 精度
                                  stockInfo.setStktype_ex(StockUtil.stktype_ex(e.getStockCode(), e.getStockName()));
                                  // 市值
                                  stockInfo.setMktval(e.getMarketValue());


                                  return stockInfo;
                              })
                              .collect(Collectors.toList());

    }


    /**
     * check  持仓比例     是否合理     ->     否则，自动重新计算 仓位比例
     * -                             ->     计算 持仓数量
     *
     * @param newPositionList 待买入 新持仓列表
     * @param clearPosition   是否 清仓旧持仓
     */
    private void checkAncCalcQty__newPositionList(List<QuickBuyPositionParam> newPositionList, boolean clearPosition) {


        // check     =>     防止 [误操作] -> [清仓]
        Assert.notEmpty(newPositionList, "[调仓换股]个股不能为空，【清仓】请用 -> [一键清仓]");


        // -------------------------------------------------------------------------------------------------------------


        // 1、我的持仓
        QueryCreditNewPosResp old_posResp = queryCreditNewPosV2();


        // -------------------------------------------------------------------------------------------------------------


        // 剩余 可买仓位
        double max_buyPosPct = max_buyPosPct(old_posResp, clearPosition);


        // -------------------------------------------------------------------------------------------------------------


        // 待买入 总仓位
        double preBuy_totalPosPct = preBuy_totalPosPct(newPositionList);


        // -------------------------------------------------------------------------------------------------------------


        // 1、check -> 1、个股仓位限制（<=5%）
        //            2、待买入 总仓位   <=   可买仓位
        //
        // 2、重新计算分配 个股待买入仓位
        checkAndFixNewPosPct(old_posResp, newPositionList, max_buyPosPct, preBuy_totalPosPct);


        // -------------------------------------------------------------------------------------------------------------


        // 可用总资金（融资上限） 计算
        double maxBuyCap = maxBuyCap(old_posResp, clearPosition);


        // -------------------------------------------------------------------------------------------------------------


        // 持仓数量 计算
        newPosQuantity(newPositionList, maxBuyCap);
    }


    /**
     * 剩余 可买仓位
     *
     * @param old_posResp   （old）已买入 持仓详情
     * @param clearPosition 是否清仓
     * @return
     */
    private double max_buyPosPct(QueryCreditNewPosResp old_posResp, boolean clearPosition) {

        // 剩余 可买仓位
        double max_buyPosPct;


        // 一键清仓
        if (clearPosition) {

            // 剩余 可买仓位   =   最大总仓位限制  -  当前 总仓位
            max_buyPosPct = ACCOUNT__POS_PCT_LIMIT - 0;

        } else {

            // 剩余 可买仓位   =   最大总仓位限制  -  当前 总仓位
            // max_buyPosPct = ACCOUNT_POS_PCT_LIMIT - old_posResp.getPosratio().doubleValue();
            max_buyPosPct = old_posResp.getMax_buyPosPct();
        }


        return max_buyPosPct;
    }

    /**
     * 待买入 总仓位
     *
     * @param newPositionList
     * @return
     */
    private double preBuy_totalPosPct(List<QuickBuyPositionParam> newPositionList) {


        // --------------------- 单只个股 仓位   ->   最大5%
        newPositionList.forEach(e -> e.setPosPct(Math.min(e.getPosPct(), STOCK__POS_PCT_LIMIT)));


        // ---------------------  待买入 总仓位   =   new 仓位累加
        double preBuy_totalPosPct = newPositionList.stream()
                                                   .map(QuickBuyPositionParam::getPosPct)
                                                   .reduce(0.0, Double::sum);


        return preBuy_totalPosPct;
    }


    /**
     * - 1、check   ->   1、个股仓位限制（<=5%）
     * -                2、待买入 总仓位   <=   可买仓位
     * -
     * - 2、重新计算分配 个股待买入仓位
     *
     * @param old_posResp        old（已持有） 持仓详情
     * @param newPositionList    new（预买入） 持仓详情
     * @param max_buyPosPct      最大   可买仓位
     * @param preBuy_totalPosPct 待买入 总仓位
     */
    private void checkAndFixNewPosPct(QueryCreditNewPosResp old_posResp,
                                      List<QuickBuyPositionParam> newPositionList,
                                      double max_buyPosPct,
                                      double preBuy_totalPosPct) {


        // ---------------------------- 实际 仓位占比（如果 仓位累加 > 可买仓位   ->   自动触发 根据仓位数值 重新计算比例）


        // 待买入总仓位  >  可买仓位     =>     根据仓位数值  ->  重新计算 仓位比例
        if (preBuy_totalPosPct > max_buyPosPct) {

            log.warn("check__newPositionList  ->  触发 仓位比例 重新计算     >>>     待买入总仓位=[{}%] > 可买仓位=[{}%]",
                     of(preBuy_totalPosPct), of(max_buyPosPct));
        }


        // ---------------------------- old

        // old     ->     stockCode - posInfo
        Map<String, CcStockInfo> oldPosMap = old_posResp.getStocks().stream()
                                                        .collect(Collectors.toMap(CcStockInfo::getStkcode, Function.identity()));


        // -------------------------------------------------------------------------------------------------------------


        double new_preBuy_totalPosPct = 0;


        // 根据仓位数值  ->  重新计算 仓位比例
        for (QuickBuyPositionParam e : newPositionList) {


            // ------------------------------------ 当前个股 -> 已持有仓位

            // 当前个股 -> 新买入仓位
            double old_posPct = 0;

            CcStockInfo stockInfo = oldPosMap.get(e.getStockCode());
            if (stockInfo != null) {
                old_posPct = stockInfo.getPosratio().doubleValue() * 100;
            }


            // ------------------------------------ 当前个股 -> 新买入仓位


            // 当前个股 -> 新买入仓位
            double new_posPct = e.getPosPct();


            // 待买入总仓位  >  可买仓位     =>     根据仓位数值  ->  重新计算 仓位比例
            if (preBuy_totalPosPct > max_buyPosPct) {

                // 个股 实际可买仓位   =  （个股待买入 / 总待买入） x  可买仓位
                new_posPct = e.getPosPct() / preBuy_totalPosPct * max_buyPosPct;
            }


            // ------------------------------------ 个股 总仓位限制 <= 5%


            // 个股 总仓位限制 <= 5%
            new_posPct = Math.max(Math.min(new_posPct, STOCK__POS_PCT_LIMIT - old_posPct), 0);


            e.setPosPct(new_posPct);


            // ------------------------------------


            new_preBuy_totalPosPct += new_posPct;
        }


        log.info("check__newPositionList - 重新校验计算后     >>>     待买入总仓位=[{}%] , 可买仓位=[{}%] , newPositionList : {}",
                 of(new_preBuy_totalPosPct), of(max_buyPosPct), JSON.toJSONString(newPositionList));
    }


    /**
     * 可用总资金
     *
     * @param old_posResp   （old）已买入 持仓详情
     * @param clearPosition 是否清仓
     * @return
     */
    private double maxBuyCap(QueryCreditNewPosResp old_posResp, boolean clearPosition) {

        // 可用总资金
        double maxBuyCap;


        if (clearPosition) {

            // （清仓）总资金  =  融资上限 = 净资产 x 2.1                理论上最大融资比例 125%  ->  这里取 110%（实际最大可融比例 110%~115%）
            // maxBuyCap = old_posResp.getNetasset().doubleValue() * MAX_RZ_RATE;
            maxBuyCap = old_posResp.getMax_TotalCap();

        } else {

            // （当前）总资金  =  可用保证金（可融）  +   可用资金（担）
            // maxBuyCap = old_posResp.getMarginavl().doubleValue() + old_posResp.getAvalmoney().doubleValue();
            maxBuyCap = old_posResp.getMax_buyCap();
        }


        return maxBuyCap;
    }


    /**
     * 持仓数量 计算
     *
     * @param newPositionList
     * @param maxBuyCap
     */
    private void newPosQuantity(List<QuickBuyPositionParam> newPositionList, double maxBuyCap) {


        // qty = 0
        List<QuickBuyPositionParam> qty0_newPositionList = Lists.newArrayList();


        // ------------------------------------------------ 持仓数量 计算


        for (QuickBuyPositionParam e : newPositionList) {
            // 价格
            double price = e.getPrice();

            // 数量 = 可用总资金 * 仓位占比 / 价格
            int qty = (int) (maxBuyCap * e.getPosRate() / price);


            // qty 规则计算
            qty = StockUtil.quantity(qty, e.getStockCode());
            e.setQuantity(qty);


            // 资金不足
            if (qty == 0) {
                qty0_newPositionList.add(e);
            }
        }


        // -------------------------------------------------------------------------------------------------------------


        // removeAll   ->   qty = 0
        newPositionList.removeAll(qty0_newPositionList);


        if (CollectionUtils.isNotEmpty(qty0_newPositionList)) {

            log.info("newPosQuantity - 资金不足 -> removeAll qty=0     >>>     size : {} , qty_0 : {}",
                     qty0_newPositionList.size(), JSON.toJSONString(qty0_newPositionList));


            // --------------------------------------------------------- TODO   DEL


            // TODO   B策略 -> 买入失败（qty=0）      ->       写回TDX（B策略-qty0）
            TdxBlockNewReaderWriter.write("BCL-QTY0", qty0_newPositionList.stream().map(QuickBuyPositionParam::getStockCode).collect(Collectors.toList()));

            // TODO   B策略 -> 买入成功（qty>0）      ->       写回TDX（B策略-SUC）
            TdxBlockNewReaderWriter.write("BCL-SUC", newPositionList.stream().map(QuickBuyPositionParam::getStockCode).collect(Collectors.toList()));
        }
    }


// -----------------------------------------------------------------------------------------------------------------


    public static BigDecimal of(double value) {
        return NumUtil.double2Decimal(value);
    }

    public static String ofStr(Number value) {
        return NumUtil.str(value);
    }


}