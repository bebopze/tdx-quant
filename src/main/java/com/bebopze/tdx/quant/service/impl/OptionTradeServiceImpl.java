package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.client.EastMoneyOptionTradeAPI;
import com.bebopze.tdx.quant.client.KlineAPI;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.constant.*;
import com.bebopze.tdx.quant.common.constant.option.OptionPriceTypeEnum;
import com.bebopze.tdx.quant.common.constant.option.OptionTradeTypeEnum;
import com.bebopze.tdx.quant.common.domain.BaseExEnum;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.domain.param.OptionTradeBSParam;
import com.bebopze.tdx.quant.common.domain.param.OptionTradeRevokeOrdersParam;
import com.bebopze.tdx.quant.common.domain.param.QuickBuyPositionParam;
import com.bebopze.tdx.quant.common.domain.trade.option.req.*;
import com.bebopze.tdx.quant.common.domain.trade.option.resp.*;
import com.bebopze.tdx.quant.common.domain.trade.resp.QueryCreditNewPosResp;
import com.bebopze.tdx.quant.common.domain.trade.resp.SHSZQuoteSnapshotResp;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.common.util.SleepUtils;
import com.bebopze.tdx.quant.common.util.StockUtil;
import com.bebopze.tdx.quant.service.OptionTradeService;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.AccountConst.ACCOUNT__POS_PCT_LIMIT;
import static com.bebopze.tdx.quant.common.constant.AccountConst.STOCK__POS_PCT_LIMIT;


/**
 * ETF期权 - BS交易
 *
 * @author: bebopze
 * @date: 2026/11/11
 */
@Slf4j
@Service
public class OptionTradeServiceImpl implements OptionTradeService {


    @Override
    public OptionGetAccountFundsResp getAccountFundsAndPositions() {

        // 资金账户
        OptionGetAccountFundsResp resp = EastMoneyOptionTradeAPI.getAccountFunds();


        // 持仓列表
        List<OptionGetGGQQPositionResp> positionList = EastMoneyOptionTradeAPI.getGGQQPosition();
        positionList.forEach(e -> e.setAccountAssets(resp.getZczz()));

        resp.setOptionPositionList(positionList);


        return resp;
    }


    @Override
    public Set<String> getPosStockCodeSet(boolean checkLogin) {

        try {
            return getAccountFundsAndPositions().getOptionPositionList().stream().map(OptionGetGGQQPositionResp::getHybm).collect(Collectors.toSet());

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
    public List<SearchOptionDictResp> searchOptionDict(String targetCode, String callOrPut, String id) {
        List<SearchOptionDictResp> optionList = EastMoneyOptionTradeAPI.searchOptionDict(new SearchOptionDictReq(targetCode, callOrPut, id));
        return optionList;
    }


    @Override
    public SHSZQuoteSnapshotResp SHSZQuoteSnapshot(String stockCode) {
        SHSZQuoteSnapshotResp dto = EastMoneyOptionTradeAPI.SHSZQuoteSnapshot(stockCode);
        return dto;
    }

    @Override
    public OptionGetMaxTradeCountResp getMaxTradeCount(OptionGetMaxTradeCountReq req) {
        OptionGetMaxTradeCountResp resp = EastMoneyOptionTradeAPI.getMaxTradeCount(req);
        return resp;
    }


    @Override
    public Integer bs(OptionTradeBSParam param) {

        OptionSubmitTradeReq req = convert2Req(param);


        // 委托编号
        Integer wtdh = EastMoneyOptionTradeAPI.submitTrade(req);
        return wtdh;
    }


    @Override
    public List<OptionGetOrderResp> getTodayOrders() {
        List<OptionGetOrderResp> respList = EastMoneyOptionTradeAPI.getTodayOrders();


        // -------------------- remove 打新申购（新股/新债  ->  配售申购/公开发行申购）
        // respList.removeIf(e -> "配售申购".equals(e.getMmsm()) || "公开发行申购".equals(e.getMmsm()) || e.getMmsm().contains("申购"));


        // -------------------- 仓位占比
        double accountAssets = getAccountFundsAndPositions().getZczz();
        respList.forEach(e -> e.setAccountAssets(accountAssets));


        // -------------------- 实时行情 / 涨跌停价格（自动计算）
        respList.parallelStream().forEach(e -> {
            // 实时行情
            StockSnapshotKlineDTO klineDTO = KlineAPI.klineCache(e.getHybm());

            // e.setPrevClosePrice(klineDTO.getPrevClose());
            // e.setClosePrice(klineDTO.getClose());
        });


        return respList;
    }


    @Override
    public List<OptionGetOrderResp> getHisOrders(LocalDate startDate, LocalDate endDate) {


        // ---------------------------------- 当日委托单


        List<OptionGetOrderResp> today__ordersDataList = Lists.newArrayList();
        if (DateTimeUtil.between(LocalDate.now(), startDate, endDate)) {
            today__ordersDataList = getTodayOrders();
        }


        // ---------------------------------- 历史委托单


        OptionGetHisOrdersReq req = new OptionGetHisOrdersReq();
        req.setSt(startDate);
        req.setEt(endDate);
        req.setQqhs(50);


        List<OptionGetOrderResp> respList = EastMoneyOptionTradeAPI.getHisOrders(req);


        // ---------------------------------- 历史 + 当日


        if (CollectionUtils.isNotEmpty(today__ordersDataList)) {
            today__ordersDataList.forEach(today -> respList.removeIf(his -> key(his).equals(key(today))));

            respList.addAll(today__ordersDataList);
        }


        return respList.stream()
                       .sorted(Comparator.comparing(OptionGetOrderResp::getWtrq).reversed())
                       .sorted(Comparator.comparing(OptionGetOrderResp::getWtsj).reversed())
                       .collect(Collectors.toList());
    }

    private String key(OptionGetOrderResp e) {
        //  key： ETF标的代码_合约代码_合同编号_委托日期_委托时间
        return e.getBdzqdm() + "_" + e.getHybm() + "_" + e.getHtxh() + "_" + e.getWtrq() + "_" + e.getWtsj();
    }


    @Override
    public List<OptionGetOrderResp> getRevokeList() {


        // 1、查询 全部委托单
        List<OptionGetOrderResp> ordersData = getTodayOrders();


        // 2、全部可撤单   ->   [未成交]
        List<OptionGetOrderResp> revokeList = ordersData.stream()
                                                        .filter(e -> {
                                                            // 委托状态（未报/已报/已撤/部成/部撤/已成/废单）
                                                            String wtzt = e.getWtzt();


                                                            // 已成交   ->   已撤/已成/废单/部撤
                                                            // 未成交   ->   待报/未报/已报/部成
                                                            return BSOrderStatusEnum.unfilled(wtzt);
                                                        })
                                                        .collect(Collectors.toList());

        return revokeList;
    }


    @Override
    public List<OptionRevokeOrderResultDTO> revokeOrders(List<OptionTradeRevokeOrdersParam> paramList) {


        // 总撤单数
        int size = paramList.size();
        // 1次 N单
        int N = 5;


        List<OptionRevokeOrderResultDTO> dtoList = Lists.newArrayList();
        for (int i = 0; i < size; ) {

            // 1次 N单
            List<OptionTradeRevokeOrdersParam> subParamList = paramList.subList(i, Math.min(i += N, size));


            // 批量撤单
            OptionRevokeOrdersReq req = convert2Req(subParamList);
            List<OptionRevokeOrderResultDTO> resultDTOS = EastMoneyOptionTradeAPI.revokeOrders(req);
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
        OptionGetAccountFundsResp posResp = getAccountFundsAndPositions();


        // 3、一键清仓
        quick__clearPosition(posResp.getOptionPositionList());
    }


    /**
     * 一键清仓     =>     指定 个股列表
     *
     * @param clearStockCodeSet 指定清仓 合约列表
     */
    @Override
    public void quickClearPosition(Set<String> clearStockCodeSet) {

        // 1、我的持仓
        OptionGetAccountFundsResp posResp = getAccountFundsAndPositions();


        // 2、从持仓个股中   过滤出   ->   清仓 合约列表
        List<OptionGetGGQQPositionResp> sell__stockInfoList = posResp.getOptionPositionList().stream().filter(e -> clearStockCodeSet.contains(e.getHybm())).collect(Collectors.toList());


        // 3、一键清仓
        quick__clearPosition(sell__stockInfoList);
    }

    /**
     * 一键 等比卖出     =>     指定 合约列表
     *
     * @param sellHybmSet  指定卖出 合约列表
     * @param sellPosPct   指定卖出 持仓比例
     * @param currPricePct （当前价格）涨跌幅比例%
     * @param prevPricePct （昨日收盘价）涨跌幅比例%
     */
    @Override
    public void quickSellPosition(Set<String> sellHybmSet,
                                  double sellPosPct,
                                  double currPricePct,
                                  double prevPricePct) {


        // 以 currPricePct 为准
        // currPricePct = currPricePct != 0 ? currPricePct : StockUtil.r1ToR0(changePricePct);


        // -------------------------------------------------------------------------------------------------------------


        // 1、我的持仓
        OptionGetAccountFundsResp posResp = getAccountFundsAndPositions();


        // 2、从持仓个股中   过滤出   ->   卖出 合约列表
        List<OptionGetGGQQPositionResp> sell__stockInfoList = posResp.getOptionPositionList().stream().filter(e -> sellHybmSet.contains(e.getHybm())).collect(Collectors.toList());


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
        List<OptionGetGGQQPositionResp> new__positionList = convert__newPositionList(newPositionList);


        // 3、一键清仓（卖old）
        // quickClearPosition();


        // 4、一键买入（买new）
        quick__buyAgain(new__positionList);


        // 5、有多余金额   ->   补充下单
        quick__buyAgain2(new__positionList);
    }

    private void quick__buyAgain2(List<OptionGetGGQQPositionResp> new__positionList) {


    }


    @Override
    public void quickClearAndBuyNewPosition(List<QuickBuyPositionParam> newPositionList) {


        // 拉取 实时行情（全A/ETF）    ->     B参数 补全：name、price、...
        // List<QuickBuyPositionParam> newPositionList = StrategyServiceImpl.convert__newPositionList(buyStockCodeSet);


        // 1、check  持仓比例
        checkAncCalcQty__newPositionList(newPositionList, true);


        // 2、组装   param -> PosResp
        List<OptionGetGGQQPositionResp> new__positionList = convert__newPositionList(newPositionList);


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
                                double singleStockMaxPosPct,
                                double currPricePct,
                                double prevPricePct) {

        Assert.notEmpty(buyStockCodeSet, "buyStockCodeSet不能为空");


        // 拉取 实时行情（全A/ETF）    ->     B参数 补全：name、price、...
        List<QuickBuyPositionParam> newPositionList = StrategyServiceImpl.convert__newPositionList(buyStockCodeSet, singleStockMaxPosPct, currPricePct, prevPricePct);


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
        List<OptionGetGGQQPositionResp> clearList = Lists.newArrayList();
        // 减仓列表
        List<OptionGetGGQQPositionResp> sellList = Lists.newArrayList();


        // -------------------------------------------------------------------------------------------------------------


        // 我的持仓
        OptionGetAccountFundsResp posResp = getAccountFundsAndPositions();


        // 过滤   ->   清仓列表、减仓列表、待买入列表
        posResp.getOptionPositionList().forEach(e -> {

            String stockCode = e.getBdzqdm();
            double old_posPct = e.getPosRatio() * 100;


            Double new_posPct = buy__code_posPct_map.getOrDefault(stockCode, 0.0);


            // IN待买入（已持有 -> 保留）
            if (new_posPct > 0) {
                new_posPct = new_posPct - old_posPct;

                // 待买入%  <  已持有%
                if (new_posPct < 0) {

                    // 减仓
                    e.setPosRatio(-1 * new_posPct);
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


//        // 移除 ETF（ETF 一律视为手动操作   ->   暂不参与BS策略）
//        clearList.removeIf(e -> StockTypeEnum.isETF(e.getStkcode()));
//        sellList.removeIf(e -> StockTypeEnum.isETF(e.getStkcode()));


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
        OptionGetAccountFundsResp posResp = getAccountFundsAndPositions();


        // 2、check     ->     等比减仓
        check___totalAccount__equalRatioSellPosition(posResp, newPositionRate);


        // 3、当前持仓 等比减仓比例   =   1  -  new总仓位 / 实际总仓位
        double sellRate = 1 - newPositionRate / posResp.getTotalAccount__actTotalPosRatio();


        // 4、持仓列表  ->  等比减仓
        equalRatio_sellPosition(posResp.getOptionPositionList(), sellRate);
    }


    @Override
    public void currPos__eqRatioSellPosition(double newPositionRate) {
        Assert.isTrue(newPositionRate < 1, String.format("positionRate=[%s]必须<1", newPositionRate));


        // 1、我的持仓
        OptionGetAccountFundsResp posResp = getAccountFundsAndPositions();


        // 2、check     ->     当前持仓 等比减仓
        // check___currPosition__equalRatioSellPosition(posResp, newPositionRate);


        // 3、当前持仓 等比减仓比例   =   1 - new仓位
        double sellRate = 1 - newPositionRate;


        // 4、持仓列表  ->  等比减仓
        equalRatio_sellPosition(posResp.getOptionPositionList(), sellRate);
    }


    /**
     * 持仓列表  ->  等比减仓
     *
     * @param positionList 持仓列表
     * @param sellRate     卖出比例
     */
    private void equalRatio_sellPosition(List<OptionGetGGQQPositionResp> positionList, double sellRate) {

        positionList.forEach(e -> {


            // --------------------------------------------------


            String etfCode = e.getBdzqdm();


            // 合约编码（10011565）
            String hybm = e.getHybm();
            // 合约代码（588000C2612M01900）
            String hydm = e.getHydm();
            // 合约名称（科创50购12月1900）
            String hyjc = e.getHyjc();


            // 可用数量
            int stkavl = e.getHysjcc();
            if (stkavl == 0) {
                log.debug("equalRatio_sellPosition - 忽略     >>>     [{} {}-{}-{}]可用数量为：{}", etfCode, hybm, hydm, hyjc, stkavl);
                return;
            }


            // -------------------------------------------------- 价格精度


            // 个股   ->   价格 2位小数
            // ETF   ->   价格 3位小数
            int scale = 4;
            // int scale = priceScale(e.getStktype_ex());


            // --------------------------------------------------


            OptionTradeBSParam param = new OptionTradeBSParam();
            param.setEtfCode(etfCode);
            param.setEtfName(null);
            param.setHybh(hybm);


            // S价格 -> 最低价（买5价 -> 确保100%成交）  =>   C x 99.5%
            BigDecimal price = BigDecimal.valueOf(e.getCurrentPrice()).multiply(BigDecimal.valueOf(0.995)).setScale(scale, RoundingMode.HALF_UP);
            param.setPrice(price);


            // ---------- 减仓数量

            // 减仓金额  =  当前市值 x sellRate
            double sell_marketValue = e.getHysz() * sellRate;

            // 减仓数量  =  减仓金额 / 价格
            int qty = (int) (sell_marketValue / price.doubleValue());

            qty = StockUtil.quantity(qty, stkavl);
            param.setAmount(qty);


            // 卖出平仓
            param.setOptionTradeTypeEnum(OptionTradeTypeEnum.SELL_CLOSE);
            // 报价方式（LMT限价）
            param.setOptionPriceTypeEnum(OptionPriceTypeEnum.LMT);


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
     * check     ->     期权账户 等比减仓
     *
     * @param posResp
     * @param newPositionRate
     */
    private void check___totalAccount__equalRatioSellPosition(OptionGetAccountFundsResp posResp,
                                                              double newPositionRate) {


        // 实际总仓位（融+担）     0.9567123   ->   95.67%
        double actTotalPosRatio = posResp.getTotalAccount__actTotalPosRatio();
        Assert.isTrue(actTotalPosRatio < newPositionRate, String.format("当前期权账户：实际总仓位=[%s] < new总仓位=[%s] ， 无需减仓！", actTotalPosRatio, newPositionRate));


        // 减仓差值  >=  5%（一次减仓   最少5%）
        double rate_diff = actTotalPosRatio - newPositionRate;
        Assert.isTrue(rate_diff > 0.05, String.format("当前期权账户：实际总仓位=[%s]，new总仓位=[%s]，减仓比例=[%s]过小，需大于5%%", actTotalPosRatio, newPositionRate, rate_diff));
    }


    @Override
    public void quickCancelOrder() {


        // 1、查询 全部委托单
        List<OptionGetOrderResp> ordersData = getTodayOrders();


        // 2、convert   撤单paramList
        List<OptionTradeRevokeOrdersParam> paramList = convert2ParamList(ordersData);


        // 3、批量撤单
        revokeOrders(paramList);


        // 等待成交   ->   1s
        SleepUtils.sleep(1000);
    }


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public void quickOption(String stockCode,
                            String hybm,
                            double priceRangePct,
                            int rangeTotal,
                            double amount,
                            OptionTradeTypeEnum optionTradeTypeEnum) {


        // 实时行情：买5 / 卖5
        SHSZQuoteSnapshotResp resp = EastMoneyOptionTradeAPI.SHSZQuoteSnapshot(hybm);
        String stockName = resp.getName();
        double buy1 = resp.getFivequote().getBuy1();


        // -------------------------------------------------------------------------------------------------------------


        double price = buy1;

        // A股/ETF   ->   价格精度
        int priceScale = StockUtil.priceScale(stockCode);


        // S -> 价格正向（阶梯 加价卖出）；B -> 价格负向（阶梯 降价买入）；
        int sign = optionTradeTypeEnum.equals(OptionTradeTypeEnum.SELL_CLOSE) ? +1 : -1;


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


            OptionTradeBSParam param = new OptionTradeBSParam();
            param.setEtfCode(stockCode);
            param.setEtfName(stockName);
            param.setHybh(hybm);
            param.setAmount(quantity);
            param.setPrice(NumUtil.double2Decimal(price, priceScale));


            param.setOptionTradeTypeEnum(optionTradeTypeEnum);
            param.setOptionPriceTypeEnum(OptionPriceTypeEnum.LMT);


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
     * 交易时间段 限制         9:30 - 11:29  /  13:00 - 14:56
     */
    private static void preCheck__tradeTime() {

        LocalTime now = LocalTime.now();


        //  9:30 - 11:29
        LocalTime start_1 = LocalTime.of(9, 30);
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

        //  9:30 ~ 11:29
        LocalTime start_1 = LocalTime.of(9, 30);
        LocalTime end_1 = LocalTime.of(11, 29);

        // 13:00 ~ 14:56
        LocalTime start_2 = LocalTime.of(13, 00);
        LocalTime end_2 = LocalTime.of(14, 56);


        Assert.isTrue(DateTimeUtil.between(now, start_1, end_1) || DateTimeUtil.between(now, start_2, end_2),
                      String.format("当前时间:[%s]非交易时间", now.format(DateTimeUtil.HH_mm_ss)));
    }


    /**
     * 从持仓个股中   过滤出   ->   非[涨跌停] 个股列表
     *
     * @param stocks
     * @return
     */
    private List<OptionGetGGQQPositionResp> filter_ztd__stockList(List<OptionGetGGQQPositionResp> stocks) {
        return stocks;


//        List<CcStockInfo> filter_ztd__stockList = stocks.stream()
//                                                        .filter(e -> {
//
//                                                            boolean ztFlag = e.getKlineDTO().isZtFlag();
//                                                            boolean dtFlag = e.getKlineDTO().isDtFlag();
//                                                            double open = e.getKlineDTO().getOpen();
//                                                            double mktval = e.getMktval();
//
//
//                                                            if (ztFlag || dtFlag) {
//                                                                log.warn("过滤 [涨跌停]个股     >>>     [{}-{}] , [{}]", e.getStkcode(), e.getStkname(), ztFlag ? "涨停" : "跌停");
//                                                                return false;
//                                                            }
//
//                                                            if (mktval <= 0 || open <= 0) {
//                                                                log.warn("过滤 [无效/未上市/停牌]个股     >>>     [{}-{}]", e.getStkcode(), e.getStkname());
//                                                                return false;
//                                                            }
//
//
//                                                            return true;
//                                                        })
//                                                        .collect(Collectors.toList());
//
//
//        return filter_ztd__stockList;
    }


    /**
     * 一键清仓
     *
     * @param sellStockInfoList 清仓 个股列表
     */
    private void quick__clearPosition(List<OptionGetGGQQPositionResp> sellStockInfoList) {
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
    private void quick__sellPosition(List<OptionGetGGQQPositionResp> sellStockInfoList,
                                     double sellPosPct,
                                     double currPricePct,
                                     double prevPricePct) {

        sellStockInfoList.forEach(e -> {


            String etfCode = e.getBdzqdm();
            String hybm = e.getHybm();


            // 可用数量
            Integer stkavl = e.getHysjcc();
//            // 当日 新买入   ->   忽略
//            if (stkavl == 0) {
//                log.debug("quick__clearPosition - 当日[新买入]/当日[已挂单] -> 忽略     >>>     stock : [{}-{}]", etfCode, etfName);
//                return;
//            }


            // -------------------------------------------------- 价格精度

            // 个股      ->    价格 2位小数
            // ETF      ->    价格 3位小数
            // ETF期权   ->   价格 4位小数
            int scale = 4;
            // int scale = priceScale(e.getStktype_ex());


            // -------------------------------------------------- 卖出数量（sellPct）


            // 卖出数量  =  持仓数量 x sellPct
            int sellQty = (int) (stkavl * sellPosPct * 0.01);

            // 取整百
            // sellQty = StockUtil.quantity(sellQty, etfCode);

            // 卖出数量  <=  可用数量
            sellQty = Math.min(sellQty, stkavl);


            // --------------------------------------------------


            OptionTradeBSParam param = new OptionTradeBSParam();
            param.setEtfCode(etfCode);
            param.setEtfName(null);
            param.setHybh(e.getHybm());


            // S价格策略
            double _price = sellPriceStrategyPct(e, currPricePct, prevPricePct);
            // S价格 -> 最低价（买5价 -> 确保100%成交）  =>   C x 99.5%
            BigDecimal price = BigDecimal.valueOf(_price).multiply(BigDecimal.valueOf(0.995)).setScale(scale, RoundingMode.HALF_UP);
            param.setPrice(price);

            // S数量（S  ->  持仓数量 x sellPct）
            param.setAmount(sellQty);

            // 卖出平仓
            param.setOptionTradeTypeEnum(OptionTradeTypeEnum.SELL_CLOSE);
            // 报价方式（LMT限价）
            param.setOptionPriceTypeEnum(OptionPriceTypeEnum.LMT);


            try {

                // 下单 -> 委托编号
                Integer wtbh = bs(param);
                log.info("quick__clearPosition - [卖出]下单SUC     >>>     param : {} , wtbh : {}", JSON.toJSONString(param), wtbh);

            } catch (Exception ex) {
                // SELL 失败
                log.error("quick__clearPosition - [卖出]下单FAIL     >>>     param : {} , errMsg : {}", JSON.toJSONString(param), ex.getMessage(), ex);


                String errMsg = ex.getMessage();


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
    private double sellPriceStrategyPct(OptionGetGGQQPositionResp e, double currPricePct, double prevPricePct) {
        // 实时行情
        SHSZQuoteSnapshotResp shszQuoteSnapshotResp = SHSZQuoteSnapshot(e.getHybm());

        if (prevPricePct != 0) {
            // 昨日收盘价
            return shszQuoteSnapshotResp.getFivequote().getYesClosePrice() * (1 + prevPricePct * 0.01);
            // return e.getKlineDTO().getPrevClose() * (1 + prevPricePct * 0.01);
        }
        // 当前价格
        return shszQuoteSnapshotResp.getRealtimequote().getCurrentPrice() * (1 + currPricePct * 0.01);
        // return e.getLastprice().doubleValue() * (1 + currPricePct * 0.01);
    }


    /**
     * 一键再买入
     *
     * @param positionList
     */
    private void quick__buyAgain(List<OptionGetGGQQPositionResp> positionList) {


        // 仓位占比 倒序（B顺序：最大持仓 -> 最小持仓）
        List<OptionGetGGQQPositionResp> descSort__positionList = positionList.stream()
                                                                             .sorted(Comparator.comparing(OptionGetGGQQPositionResp::getHysz).reversed())
                                                                             .collect(Collectors.toList());


        // --------------------------------------------------


        // 融资买入 -> SUC
        Set<String> rzSucCodeList = Sets.newHashSet();

        // 融资买入 -> FAIL  =>  待 担保买入
        Set<String> rzFailCodeList = Sets.newHashSet();


        // --------------------------------------------------------------------


        // ------------------------------ 1、融资再买入

        // 融资买
        // buy_rz(descSort__positionList, rzSucCodeList, rzFailCodeList);


        // ------------------------------ 2、担保再买入


        // 担保买
        buy_zy(descSort__positionList);


        // ------------------------------ 3、新空余 担保资金


        OptionGetAccountFundsResp bsAfter__posResp = getAccountFundsAndPositions();

        // 可用资金
        double avalmoney = bsAfter__posResp.getZjkyje();


        log.info("quick__buyAgain     >>>     avalmoney : {} , bsAfter__positionList : {}", avalmoney, JSON.toJSONString(descSort__positionList));
    }


    /**
     * 担保再买入
     *
     * @param sort__positionList
     */
    private void buy_zy(List<OptionGetGGQQPositionResp> sort__positionList) {


        List<OptionGetGGQQPositionResp> FAIL_LIST = Lists.newArrayList();


        // --------------------------------------------------------------------------


        sort__positionList.forEach(e -> {


            // 标的证券code（ETF代码）
            String stockCode = e.getBdzqdm();

            // 合约编码
            String Hybm = e.getHybm();
            // 合约代码
            String Hydm = e.getHydm();
            // 合约名称
            String Hyjc = e.getHyjc();


            // -------------------------------------------------- 价格精度


            // 个股   ->   价格 2位小数
            // ETF   ->   价格 3位小数
            // 期权  ->   价格 4位小数
            int scale = 4;
            // int scale = priceScale(e.getStktype_ex());


            // -------------------------------------------------- 买入 - 参数


            log.info("担保再买入 - [担保买入]   =>   下单start     >>>     stock : [{}-{}] , posStock : {}",
                     stockCode, e.getHybm(), JSON.toJSONString(e));


            OptionTradeBSParam param = new OptionTradeBSParam();
            param.setEtfCode(e.getBdzqdm());
            param.setEtfName(null);
            param.setHybh(e.getHybm());


            // B价格 -> 最高价（卖5价 -> 确保100%成交）  =>   C x 100.5%
            BigDecimal price = BigDecimal.valueOf(e.getCurrentPrice()).multiply(BigDecimal.valueOf(1.005)).setScale(scale, RoundingMode.HALF_UP);
            param.setPrice(price);

            // 数量（B数量 = S数量 -> 可用数量）
            param.setAmount(StockUtil.quantity(e.getHykysl(), stockCode));


            // 买入开仓
            param.setOptionTradeTypeEnum(OptionTradeTypeEnum.BUY_OPEN);
            // 报价方式（LMT限价）
            param.setOptionPriceTypeEnum(OptionPriceTypeEnum.LMT);


            // -------------------------------------------------- 买入


            try {

                // 委托编号
                Integer wtbh = bs(param);

                log.info("担保再买入 - [担保买入]   =>   下单SUC     >>>     stock : [{}-{}-{}-{}] , posStock : {} , param : {} , wtbh : {}",
                         stockCode, Hybm, Hydm, Hyjc, JSON.toJSONString(e), JSON.toJSONString(param), wtbh);


            } catch (Exception ex) {

                FAIL_LIST.add(e);

                log.error("担保再买入 - [担保买入]   =>   下单FAIL     >>>     stock : [{}-{}-{}-{}] , posStock : {} , param : {} , errMsg : {}",
                          stockCode, Hybm, Hydm, Hyjc, JSON.toJSONString(e), JSON.toJSONString(param), ex.getMessage(), ex);
            }
        });


        // TODO     FAIL_LIST -> retry
        if (CollectionUtils.isNotEmpty(FAIL_LIST)) {

            log.error("担保再买入 - [担保买入]   =>   下单FAIL     >>>     FAIL_LIST : {}", JSON.toJSONString(FAIL_LIST));


            // handle__FAIL_LIST(FAIL_LIST);
            // DBB-FAIL（担保B-FAIL）
            // TdxBlockNewReaderWriter.write("DBB-FAIL", FAIL_LIST.stream().map(CcStockInfo::getStkcode).collect(Collectors.toList()));
        }
    }


//    private boolean buyAgain__preCheck() {
//
//
//        // 1、当前持仓
//        QueryCreditNewPosResp now__posResp = getAccountFundsAndPositions();
//
//
//        now__posResp.getStocks().forEach(e -> {
//            // 可用数量
//            Integer stkavl = e.getStkavl();
//            if (stkavl > 0) {
//
//            }
//        });
//
//
//        // 总市值
//        double totalmkval = now__posResp.getTotalmkval().doubleValue();
//        if (totalmkval == 1000) {
//            return true;
//        }
//
//
//        // 总仓位     2.3567123   ->   235.67%
//        double posratio = now__posResp.getPosratio().doubleValue();
//        // 总仓位<5%
//        if (posratio <= 0.05) {
//            return true;
//        }
//
//
//        // 2、check   ->   全部[卖单]->[已成交]
//        List<CcStockInfo> stocks = now__posResp.getStocks();
//        if (CollectionUtils.isEmpty(stocks)) {
//            return true;
//        }
//
//
//        log.warn("quick__buyAgain  -  check SELL委托单     >>>     {}", JSON.toJSONString(stocks));
//
//
//        return true;
//    }


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
        List<OptionGetOrderResp> ordersData = getTodayOrders();


        // 2、check
        boolean flag = true;
        for (OptionGetOrderResp e : ordersData) {

            // 委托状态（未报/已报/已撤/部成/部撤/已成/废单）
            String wtzt = e.getWtzt();


            // 已成交   ->   已撤/已成/废单/部撤
            // 未成交   ->   待报/未报/已报/部成
            if (BSOrderStatusEnum.unfilled(wtzt)) {
                log.warn("checkAndRetry___clearPosition__OrdersStatus  -  存在  [未成交]-[SELL委托单] -> [待报/未报/已报/部成]     >>>     retry : {} , wtzt : {} , orderData : {}", retry, wtzt, JSON.toJSONString(e));

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


            // ----------------- 全部撤单


            // 0、先撤单 -> 再全部卖出
            quickCancelOrder();


            // ----------------- 再次拉取  最新持仓详情  ->  然后过滤


            // 1、我的持仓
            OptionGetAccountFundsResp posResp = getAccountFundsAndPositions();
            // 4、从持仓个股中   过滤出   ->   非[涨跌停] 个股列表
            List<OptionGetGGQQPositionResp> filter_ztd__stockList = filter_ztd__stockList(posResp.getOptionPositionList());
            // 仓位占比 正序（S顺序：最小持仓 -> 最大持仓）
            List<OptionGetGGQQPositionResp> ascSort__positionList = filter_ztd__stockList.stream().sorted(Comparator.comparing(OptionGetGGQQPositionResp::getHysz)).collect(Collectors.toList());


            // ----------------- 再次 卖出


            // 5、一键清仓（S顺序：最小持仓 -> 最大持仓）
            quick__clearPosition(ascSort__positionList);


            // --------------------------------------------- 递归 check


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
    private List<OptionTradeRevokeOrdersParam> convert2ParamList(List<OptionGetOrderResp> ordersData) {

        // 2、convert   撤单paramList
        List<OptionTradeRevokeOrdersParam> paramList = Lists.newArrayList();
        ordersData.forEach(e -> {


            // 委托状态（未报/已报/已撤/部成/部撤/已成/废单）
            String wtzt = e.getWtzt();


            // 过滤  已成交   ->   已撤/已成/废单/部撤
            if (BSOrderStatusEnum.filled(wtzt)) {
                return;
            }


            log.warn("quick__cancelOrder - [未成交]->[撤单]     >>>     stock : [{}-{} {}-{}-{}] , wtbh : {} , wtzt : {} , order : {}",
                     e.getBdzqdm(), e.getBdzqmc(), e.getHybm(), e.getHydm(), e.getHyjc(), e.getHtxh(), wtzt, JSON.toJSONString(e));


            // -----------------------------------------


            OptionTradeRevokeOrdersParam param = new OptionTradeRevokeOrdersParam();
            // 交易所（05-深交所；15-上交所；）
            param.setJybk(e.getJybk());
            // 委托编号
            param.setHtxh(e.getHtxh());


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
        // ETF齐全   ->   价格 4位小数
        return 4;


//        // ETF   ->   价格 3位小数
//        int scale = 2;
//
//
//        if (stktypeEx.equals("E")) {
//            scale = 3;
//        } else {
//            // 个股   ->   价格 2位小数
//            scale = 2;
//        }
//
//        return scale;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 下单 -> B/S
     *
     * @param param
     * @return
     */
    private OptionSubmitTradeReq convert2Req(OptionTradeBSParam param) {


        OptionSubmitTradeReq req = new OptionSubmitTradeReq();
        req.setEtfCode(param.getEtfCode());
        req.setHybh(param.getHybh());
        req.setAmount(param.getAmount());
        req.setPrice(param.getPrice());


        // B/S
        req.setOptionTradeTypeEnum(param.getOptionTradeTypeEnum());
        req.setOptionPriceTypeEnum(param.getOptionPriceTypeEnum());


        return req;
    }


    /**
     * 撤单
     *
     * @param paramList
     * @return
     */
    private OptionRevokeOrdersReq convert2Req(List<OptionTradeRevokeOrdersParam> paramList) {
        List<String> revokeList = Lists.newArrayList();


        for (OptionTradeRevokeOrdersParam param : paramList) {

            // 委托编号-交易所
            String revoke = param.getJybk() + "-" + param.getHtxh();

            revokeList.add(revoke);
        }


        OptionRevokeOrdersReq req = new OptionRevokeOrdersReq();
        req.setWtbhs(String.join(",", revokeList));

        return req;
    }


// -----------------------------------------------------------------------------------------------------------------


    /**
     * param -> CcStockInfo
     *
     * @param newPositionList
     * @return
     */
    private List<OptionGetGGQQPositionResp> convert__newPositionList(List<QuickBuyPositionParam> newPositionList) {


        return newPositionList.stream().map(e -> {
                                  OptionGetGGQQPositionResp stockInfo = new OptionGetGGQQPositionResp();


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


                                  stockInfo.setBdzqdm(e.getStockCode());
                                  // stockInfo.setStkname(e.getStockName());

                                  // 价格
                                  stockInfo.setCurrentPrice(e.getPrice());
                                  // 数量
                                  stockInfo.setHysjcc(e.getQuantity());
                                  stockInfo.setHykysl(e.getQuantity());


                                  // 股票/ETF   ->   计算 price 精度
                                  // stockInfo.setStktype_ex(StockUtil.stktype_ex(e.getStockCode(), e.getStockName()));
                                  // 市值
                                  stockInfo.setHysz(NumUtil.decimal2Double(e.getMarketValue()));


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
        OptionGetAccountFundsResp old_posResp = getAccountFundsAndPositions();


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


        // 净资产   ->   所有的仓位%  都是针对 [净资产] 进行计算的❗❗❗
        double netasset = old_posResp.getZczz();


        // -------------------------------------------------------------------------------------------------------------


        // 持仓数量 计算
        newPosQuantity(newPositionList, netasset);
    }


    /**
     * 剩余 可买仓位
     *
     * @param old_posResp   （old）已买入 持仓详情
     * @param clearPosition 是否清仓
     * @return
     */
    private double max_buyPosPct(OptionGetAccountFundsResp old_posResp, boolean clearPosition) {

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
    private void checkAndFixNewPosPct(OptionGetAccountFundsResp old_posResp,
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

        // old     ->     合约代码 - posInfo
        Map<String, OptionGetGGQQPositionResp> oldPosMap = old_posResp.getOptionPositionList().stream()
                                                                      .collect(Collectors.toMap(OptionGetGGQQPositionResp::getHydm, Function.identity()));


        // -------------------------------------------------------------------------------------------------------------


        double new_preBuy_totalPosPct = 0;


        // 根据仓位数值  ->  重新计算 仓位比例
        for (QuickBuyPositionParam e : newPositionList) {


            // ------------------------------------ 当前个股 -> 已持有仓位

            // 当前个股 -> 已持有仓位
            double old_posPct = 0;

            OptionGetGGQQPositionResp stockInfo = oldPosMap.get(e.getStockCode());
            if (stockInfo != null) {
                old_posPct = stockInfo.getPosRatio() * 100;
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
     * 持仓数量 计算
     *
     * @param newPositionList
     * @param netasset        净资产   ->   所有的仓位%  都是针对 [净资产] 进行计算的❗❗❗
     */
    private void newPosQuantity(List<QuickBuyPositionParam> newPositionList, double netasset) {


        // qty = 0
        List<QuickBuyPositionParam> qty0_newPositionList = Lists.newArrayList();


        // ------------------------------------------------ 持仓数量 计算


        for (QuickBuyPositionParam e : newPositionList) {
            // 价格
            double price = e.getPrice();

            // 数量 = 可用总资金 * 仓位占比 / 价格   ❌❌❌

            // 数量 = 净资产 * 仓位占比 / 价格   ✅✅✅   ->   所有的仓位%  都是针对 [净资产] 进行计算的❗❗❗
            int qty = (int) (netasset * e.getPosRate() / price);


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


//            // --------------------------------------------------------- TODO   DEL
//
//
//            // TODO   B策略 -> 买入失败（qty=0）      ->       写回TDX（B策略-qty0）
//            TdxBlockNewReaderWriter.write("BCL-QTY0", qty0_newPositionList.stream().map(QuickBuyPositionParam::getStockCode).collect(Collectors.toList()));
//
//            // TODO   B策略 -> 买入成功（qty>0）      ->       写回TDX（B策略-SUC）
//            TdxBlockNewReaderWriter.write("BCL-SUC", newPositionList.stream().map(QuickBuyPositionParam::getStockCode).collect(Collectors.toList()));
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