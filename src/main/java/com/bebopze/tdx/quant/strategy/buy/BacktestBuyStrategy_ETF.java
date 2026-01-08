package com.bebopze.tdx.quant.strategy.buy;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.BlockNewIdEnum;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.service.TopBlockService;
import com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy;
import com.bebopze.tdx.quant.strategy.sell.BacktestSellStrategy;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.btCompareDTO;
import static com.bebopze.tdx.quant.strategy.buy.ScoreSort.scoreSort__RPS;


/**
 * 回测 - B策略（高抛低吸  ->  C_MA_偏离率）
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestBuyStrategy_ETF implements BuyStrategy {


    @Autowired
    private MarketService marketService;

    @Autowired
    private TopBlockService topBlockService;

    @Autowired
    private BacktestBuyStrategyA backtestBuyStrategyA;

    @Lazy
    @Autowired
    private BacktestStrategy backtestStrategy;

    @Lazy
    @Autowired
    private BacktestSellStrategy backtestSellStrategy;

    @Autowired
    private BuyStrategy__ConCombiner__TopStock buyStrategy__conCombiner__topStock;


    @Override
    public String key() {
        return "ETF";
    }


    /**
     * 买入策略   =   大盘（70%） +  主线板块（25%） +  个股买点（5%）
     *
     * @param topBlockStrategyEnum 主线策略
     * @param buyConSet            B策略
     * @param data                 全量行情
     * @param tradeDate            交易日期
     * @param buy_infoMap          买入个股-交易信号
     * @param posRate              当前 总仓位
     * @param ztFlag               是否涨停（打板）
     * @return
     */
    @TotalTime
    @Override
    public List<String> rule(TopBlockStrategyEnum topBlockStrategyEnum,
                             Set<String> buyConSet,
                             BacktestCache data,
                             LocalDate tradeDate,
                             Map<String, String> buy_infoMap,
                             double posRate,
                             Boolean ztFlag) {


        // -------------------------------------------------------------------------------------------------------------
        //                                                1、大盘牛熊
        // -------------------------------------------------------------------------------------------------------------


        QaMarketMidCycleDO marketInfo = data.marketCache.get(tradeDate, k -> marketService.marketInfo(tradeDate));
        Assert.notNull(marketInfo, "[大盘量化]数据为空：" + tradeDate);

        // 大盘-牛熊：1-牛市；2-熊市；
        Integer marketBullBearStatus = marketInfo.getMarketBullBearStatus();
        // 大盘-中期顶底：1-底部；2- 底->顶；3-顶部；4- 顶->底；
        Integer marketMidStatus = marketInfo.getMarketMidStatus();


        // 1-牛市
        if (marketBullBearStatus == 1) {
            // TODO   ->   牛市策略   ->   个股 B/S策略 、 主线板块 策略
        }
        // 2-熊市
        else if (marketMidStatus == 2) {
            // TODO   ->   熊市策略   ->   个股 B/S策略 、 主线板块 策略
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                                2、主线板块
        // -------------------------------------------------------------------------------------------------------------


        long start_1 = System.currentTimeMillis();
        Set<String> topBlockCodeSet = topBlock(topBlockStrategyEnum, data, tradeDate); // 板块-月多2
        log.info("BacktestBuyStrategyC - topBlock     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_1));


        // ---------------------------------------------
        // 板块-月多2     +     涨停TOP1 + 百日新高TOP1
        if (btCompareDTO.get().isTop1TopBlockFlag()) {
            topBlockCodeSet = top1__topBlockCodeSet__Cache(topBlockStrategyEnum, data, topBlockCodeSet, tradeDate);
        }


        // -------------------------------------------------------------------------------------------------------------
        //                                                3、（强势）个股
        // -------------------------------------------------------------------------------------------------------------


        // B策略   ->   强势个股
        long start_2 = System.currentTimeMillis();
        Set<String> buy__topStock__codeSet = buy__topETF__codeSet(buyConSet, data, tradeDate, buy_infoMap, ztFlag);
        log.info("BacktestBuyStrategy_ETF - buy__topETF__codeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_2));


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股 -> IN 主线板块
        // -------------------------------------------------------------------------------------------------------------


//        // TODO   强势ETF   ->   IN 主线板块
//        long start_3 = System.currentTimeMillis();
//        Set<String> inTopBlock__stockCodeSet = inTopBlock__stockCodeSet(topBlockCodeSet, buy__topStock__codeSet, data, tradeDate);
//        log.info("BacktestBuyStrategy_ETF - inTopBlock__stockCodeSet     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_3));


        Set<String> inTopBlock__stockCodeSet = buy__topStock__codeSet;


        // -------------------------------------------------------------------------------------------------------------


        // 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）

        long start_4 = System.currentTimeMillis();
        backtestBuyStrategyA.buyStrategy_ETF(inTopBlock__stockCodeSet, data, tradeDate, buy_infoMap, posRate);
        log.info("BacktestBuyStrategy_ETF - buyStrategy_ETF     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_4));


        // -------------------------------------------------------------------------------------------------------------


        // B策略 - S策略   相互冲突bug       =>       以 S策略 为准       ->       出现 S信号 个股不能买入（buyList -> 剔除）
        // backtestStrategy.buy_sell__signalConflict(data, tradeDate, inTopBlock__stockCodeList);


        // -------------------------------------------------------------------------------------------------------------


        // 按照 规则打分 -> sort
        long start_5 = System.currentTimeMillis();
        List<String> sort__stockCodeList = scoreSort__RPS(inTopBlock__stockCodeSet, data, tradeDate, btCompareDTO.get().getScoreSortN());
        log.info("BacktestBuyStrategy_ETF - scoreSort     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_5));


        return sort__stockCodeList;
    }


    /**
     * 强势个股   ->   IN 主线板块                  // 通用方法
     *
     * @param topBlockCodeSet        主线板块
     * @param buy__topStock__codeSet 强势个股
     * @param data
     * @param tradeDate
     * @return
     */
    public Set<String> inTopBlock__stockCodeSet(Set<String> topBlockCodeSet,
                                                Collection<String> buy__topStock__codeSet,

                                                BacktestCache data,
                                                LocalDate tradeDate) {


        // 强势个股   ->   IN 主线板块
        Set<String> inTopBlock__stockCodeSet = buy__topStock__codeSet
                .stream()
                .filter(stockCode -> {


                    // 个股   -对应->   板块列表
                    Set<String> stock__blockCodeSet = data.stockCode_blockCodeSet_Map.getOrDefault(stockCode, Sets.newHashSet());


                    // 交集（个股板块 - 主线板块）
                    Collection<String> stock__blockCodeSet__inTopBlock = CollectionUtils.intersection(topBlockCodeSet, stock__blockCodeSet);

                    // 非空（个股所属 主线板块）
                    if (CollectionUtils.isNotEmpty(stock__blockCodeSet__inTopBlock)) {


                        // 个股   ->   主线板块（IN 主线板块）      code-name列表
                        Set<String> stock__blockCodeNameSet__inTopBlock = stock__blockCodeSet__inTopBlock.stream()
                                                                                                         // 板块code-板块name
                                                                                                         .map(blockCode -> blockCode + "-" + data.block__codeNameMap.get(blockCode))
                                                                                                         .collect(Collectors.toSet());

                        // Cache（code-name 列表）
                        data.stock__inTopBlockCache.get(tradeDate, k -> Maps.newConcurrentMap())
                                                   .put(stockCode, stock__blockCodeNameSet__inTopBlock);


                        if (log.isDebugEnabled()) {
                            log.debug("个股 -> IN 主线板块     >>>     {} , [{}-{}] , [{}]",
                                      tradeDate,
                                      stockCode, data.stock__codeNameMap.get(stockCode),
                                      stock__blockCodeNameSet__inTopBlock);
                        }

                        return true;
                    }


//                    for (String stock__blockCode : stock__blockCodeSet) {
//
//
//                        // 个股   ->   IN 主线板块
//                        boolean inTopBlock = topBlockCodeSet.contains(stock__blockCode);
//
//                        if (inTopBlock) {
//
//
//                            // 个股   ->   B-signal   主线板块
//                            // String key = tradeDate + "|" + stockCode;
//                            // data.stockCode_topBlockCache.get(stockCode, k -> Sets.newConcurrentHashSet()).add(stock__blockCode);
//
//
//                            log.debug("个股 -> IN 主线板块     >>>     {} , [{}-{}] , [{}-{}]", tradeDate,
//                                      stockCode, data.stock__codeNameMap.get(stockCode),
//                                      stock__blockCode, data.block__codeNameMap.get(stock__blockCode));
//
//
//                            return true;
//                        }
//                    }


                    return false;
                }).collect(Collectors.toSet());


        return inTopBlock__stockCodeSet;
    }


    /**
     * B策略   ->   强势个股
     *
     * @param buyConSet   B策略
     * @param data
     * @param tradeDate
     * @param buy_infoMap
     * @param ztFlag      个股是否涨停： true-是；false-否（默认）；null-不过滤；
     * @return
     */
    private Set<String> buy__topETF__codeSet(Set<String> buyConSet,
                                             BacktestCache data,
                                             LocalDate tradeDate,
                                             Map<String, String> buy_infoMap,
                                             Boolean ztFlag) {


        Set<String> buy__topStock__codeSet = Sets.newHashSet();
        data.ETF_stockDOList.forEach(stockDO -> {

            String stockCode = stockDO.getCode();
            StockFun fun = data.getOrCreateStockFun(stockDO);


            KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // -------------------------------------------


            // 当日 - 停牌（003005  ->  2022-10-27）
            Integer idx = dateIndexMap.get(tradeDate);

            // 过滤 停牌/新股       // TODO 个股行情指标 异常数据bug   688692（达梦数据）     kline 301条   extData 300条（首日 2024-06-12 扩展数据 缺失）
            if (idx == null || Double.isNaN(extDataArrDTO.rps50[idx]) || fun.getKlineDTOList().size() != fun.getExtDataDTOList().size()) {
                return;
            }


//            // ----------------------------------------- ztFlag 过滤策略 ------------------------------------------------
//
//
//            // ztFlag 策略   ->   是否过滤 涨停（true/false/不过滤）
//            boolean today_涨停 = extDataArrDTO.涨停[idx];
//            if (ztFlag != null && !Objects.equals(ztFlag, today_涨停)) {
//                return;
//            }
//
//
//            // ---------------------------------------------------------------------------------------------------------
//
//
//            ExtDataDTO extDataDTO = fun.getExtDataDTOList().get(idx);
//
//
            // ---------------------------------------------------------------------------------------------------------


//            double rps50 = extDataArrDTO.rps50[idx];
//            double rps120 = extDataArrDTO.rps120[idx];
//            double rps250 = extDataArrDTO.rps250[idx];


            boolean 月多 = extDataArrDTO.月多[idx];
            boolean 均线预萌出 = extDataArrDTO.均线预萌出[idx];
            boolean RPS红 = extDataArrDTO.RPS红[idx] && extDataArrDTO.rps50[idx] >= 90;
            boolean SSF多 = extDataArrDTO.SSF多[idx];


            boolean signal_B = (月多 || 均线预萌出) && RPS红 && SSF多;
            if (signal_B) {
                buy__topStock__codeSet.add(stockCode);
                buy_infoMap.put(stockCode, "主线ETF（月多2）");
            }


            // ---------------------------------------------------------------------------------------------------------


//            Map<String, Boolean> conMap = Maps.newHashMap();
//            try {
//                conMap = conMap(klineArrDTO, extDataArrDTO, extDataDTO, idx);
//            } catch (Exception ex) {
//                log.error("conMap - err     >>>     stockCode : {} , tradeDate : {} , errMsg : {}", stockCode, tradeDate, ex.getMessage(), ex);
//            }
//
//
//            // ---------------------------------------------------------------------------------------------------------
//
//
//            // 是否买入       =>       conList   ->   全为 true
//            boolean signal_B = BuyStrategy__ConCombiner.calcCon(buyConSet, conMap);
//
//
//            // ---------------------------------------------------------------------------------------------------------
//
//
//            // B   +   anyMatch__buyStrategy          // ❌❌❌ 年收益率 从100%  ->  10% ❌❌❌（废弃！）
//            // signal_B = signal_B && buyStrategy__conCombiner__topStock.anyMatch__buyStrategy(extDataDTO);
//
//
//            // ---------------------------------------------------------------------------------------------------------
//
//
//            // B + 未涨停  ->  可买入（今日[close]  ->  直接买入）
//            if (signal_B && !today_涨停) {
//                buy__topStock__codeSet.add(stockCode);
//                buySignalInfo(buy_infoMap, stockCode, data, idx, conMap);
//            }
//
//
//            // B + 涨停  ->  无法买入（最简化处理：[next_close] = [next_open]，次日开盘 直接买入）
//            if (signal_B && today_涨停) {
//
//                if (idx < fun.getMaxIdx()) {
//
//                    LocalDate next_date = klineArrDTO.date[idx + 1];
//
//
//                    // 次日S  ->  不能B（提前预知 -> 次日收盘价？？？❌❌❌）
//
//                    // 今日B + 涨停     =>     今日S  ->  次日不能B
//                    Set<String> nextDate__sellStockCodeSet = backtestSellStrategy.rule(btCompareDTO.get().getTopBlockStrategyEnum(), data, tradeDate, Sets.newHashSet(stockCode), Maps.newHashMap(), btCompareDTO.get());
//                    boolean next_date_S = nextDate__sellStockCodeSet.contains(stockCode);
//                    if (next_date_S /*|| nextDate__大盘仓位限制->等比减仓*/) {
//                        return;
//                    }
//
//
//                    // -------------------------------------------------------------------------------------------------
//
//
//                    // 今日B + 涨停     =>     次日 -> 开盘B
//                    btOpenBSDTO.get().today_date = tradeDate;
//                    btOpenBSDTO.get().next_date = next_date;
//                    btOpenBSDTO.get().open_B___stockCodeSet.add(stockCode);
//                    btOpenBSDTO.get().open_B___buy_infoMap.put(stockCode, 涨停_SSF多_月多.getDesc());
//                }
//            }
        });


        return buy__topStock__codeSet;
    }


    /**
     * 指定   主线策略 + 日期     ->     主线板块 列表
     *
     * @param topBlockStrategyEnum 主线策略
     * @param data
     * @param tradeDate            日期
     * @return
     */
    public Set<String> topBlock(TopBlockStrategyEnum topBlockStrategyEnum, BacktestCache data, LocalDate tradeDate) {
        // return data.topBlockCache.get(tradeDate + "|" + topBlockStrategyEnum.getDesc(), k -> topBlockStrategy(topBlockStrategyEnum, data, tradeDate));

        return data.topBlockCache.get(tradeDate, k -> Maps.newConcurrentMap())
                                 .computeIfAbsent(topBlockStrategyEnum, kk -> topBlockStrategy(topBlockStrategyEnum, data, tradeDate));
    }


    /**
     * 主线板块（LV2 -> 普通行业   ）    ->     百日新高 - 天数占比Top1       ->      唯一主线
     * 主线板块（LV3 -> 概念 + 细分）    ->     月多 + RPS红(87) + SSF多     ->     N个 “主线”
     *
     * @param topBlockStrategyEnum
     * @param data
     * @param tradeDate
     * @return
     */
    private Set<String> topBlockStrategy(TopBlockStrategyEnum topBlockStrategyEnum,
                                         BacktestCache data,
                                         LocalDate tradeDate) {


        // -------------------------------------------------------------------------------------------------------------
        //                                      1、主线板块（LV1 -> 研究行业）
        // -------------------------------------------------------------------------------------------------------------


        // TODO   暂不考虑
        //
        //


        // -------------------------------------------------------------------------------------------------------------
        //                                      2、主线板块（LV2 -> 普通行业）
        // -------------------------------------------------------------------------------------------------------------


        // 主线板块（唯一）    ->     LV2（普通行业） /  LV1（研究行业）
        String lv2_topBlockCode = lv2_topBlockCode(tradeDate);


        // -------------------------------------------------------------------------------------------------------------
        //                                      3、主线板块（LV3 -> 概念 + 细分）
        // -------------------------------------------------------------------------------------------------------------


        // 主线板块（N个）    ->     LV3（概念 + 细分）
        Set<String> lv3_topBlockCodeSet = lv3_topBlockCodeSet(data, tradeDate);


        // -------------------------------------------------------------------------------------------------------------
        //                                      4、主线板块（LV2普通行业   <- 升级/降级 ->   LV3 概念 + 细分）
        // -------------------------------------------------------------------------------------------------------------


        // LV2_LV3
        Set<String> lv2_lv3__topBlockCodeSet = lv2_lv3__topBlockCodeSet(tradeDate, lv3_topBlockCodeSet);


        // -------------------------------------------------------------------------------------------------------------
        //                                      topBlockStrategyEnum
        // -------------------------------------------------------------------------------------------------------------


        // 中期  = 短期     ->     100% LV2 （   唯一主线）
        // 中期 != 短期     ->     主线 切换期（无 唯一主线）    =>     LV2（唯一主线）    -降级->     LV3（N个 “主线”）
        Set<String> topBlockCodeSet = topBlockStrategy(topBlockStrategyEnum, lv2_topBlockCode, lv3_topBlockCodeSet, lv2_lv3__topBlockCodeSet);


        // -------------------------------------------------------------------------------------------------------------


        topBlockCodeSet.removeIf(StringUtils::isBlank);
        return topBlockCodeSet;
    }


    /**
     * 主线板块（LV2 -> 普通行业   ）    ->     百日新高 - 天数占比Top1       ->      唯一主线
     * 主线板块（LV3 -> 概念 + 细分）    ->     月多 + RPS红(87) + SSF多     ->     N个 “主线”
     *
     * @param topBlockStrategyEnum
     * @param data
     * @param tradeDate
     * @return
     */
    private Set<String> topBlockStrategy2(TopBlockStrategyEnum topBlockStrategyEnum,
                                          BacktestCache data,
                                          LocalDate tradeDate) {


        if (Objects.equals(topBlockStrategyEnum, TopBlockStrategyEnum.LV1)) {


            // ---------------------------------------------------------------------------------------------------------
            //                                      1、主线板块（LV1 -> 研究行业）
            // ---------------------------------------------------------------------------------------------------------


            // TODO   暂不考虑
            //
            //


        } else if (Objects.equals(topBlockStrategyEnum, TopBlockStrategyEnum.LV2)) {


            // ---------------------------------------------------------------------------------------------------------
            //                                      2、主线板块（LV2 -> 普通行业）
            // ---------------------------------------------------------------------------------------------------------


            // 主线板块（唯一）    ->     LV2（普通行业） /  LV1（研究行业）
            String lv2_topBlockCode = lv2_topBlockCode__unique(tradeDate);

            return Sets.newHashSet(lv2_topBlockCode);


        } else if (Objects.equals(topBlockStrategyEnum, TopBlockStrategyEnum.LV3)) {

            // ---------------------------------------------------------------------------------------------------------
            //                                      3、主线板块（LV3 -> 概念 + 细分）
            // ---------------------------------------------------------------------------------------------------------


            // 主线板块（N个）    ->     LV3（概念 + 细分）
            Set<String> lv3_topBlockCodeSet = lv3_topBlockCodeSet(data, tradeDate);

            return lv3_topBlockCodeSet;


        } else if (Objects.equals(topBlockStrategyEnum, TopBlockStrategyEnum.LV2_LV3)) {


            // ---------------------------------------------------------------------------------------------------------
            //                                      4、主线板块（LV2普通行业   <- 升级/降级 ->   LV3 概念 + 细分）
            // ---------------------------------------------------------------------------------------------------------


            // LV2_LV3

            // 中期  = 短期     ->     100% LV2 （   唯一主线）
            // 中期 != 短期     ->     主线 切换期（无 唯一主线）    =>     LV2（唯一主线）    -降级->     LV3（N个 “主线”）

            Set<String> lv2_lv3__topBlockCodeSet = lv2_lv3__topBlockCodeSet(tradeDate, lv3_topBlockCodeSet(data, tradeDate));


            return lv2_lv3__topBlockCodeSet;
        }


        throw new BizException("topBlockStrategyEnum=[" + topBlockStrategyEnum + "]有误！");
    }


    /**
     * 中期  = 短期     ->     100% LV2 （   唯一主线）
     * 中期 != 短期     ->     主线 切换期（无 唯一主线）    =>     LV2（唯一主线）    -降级->     LV3（N个 “主线”）
     *
     * @param topBlockStrategyEnum
     * @param lv2_topBlockCode         LV2（唯一主线）
     * @param lv3_topBlockCodeSet      LV3（N个 “主线”）
     * @param lv2_lv3__topBlockCodeSet
     * @return
     */
    private Set<String> topBlockStrategy(TopBlockStrategyEnum topBlockStrategyEnum,
                                         String lv2_topBlockCode,
                                         Set<String> lv3_topBlockCodeSet,
                                         Set<String> lv2_lv3__topBlockCodeSet) {


        if (Objects.equals(topBlockStrategyEnum, TopBlockStrategyEnum.LV2)) {

            return Sets.newHashSet(lv2_topBlockCode);

        } else if (Objects.equals(topBlockStrategyEnum, TopBlockStrategyEnum.LV3)) {

            return lv3_topBlockCodeSet;

        } else if (Objects.equals(topBlockStrategyEnum, TopBlockStrategyEnum.LV2_LV3)) {

            return lv2_lv3__topBlockCodeSet;
        }


        // 默认值：LV3
        return lv3_topBlockCodeSet;
    }

    private Set<String> lv2_lv3__topBlockCodeSet(LocalDate tradeDate, Set<String> lv3_topBlockCodeSet) {

        // 唯一主线  !=  null     ->     LV2（唯一主线）
        // 唯一主线  ==  null     ->     LV3（N个 “主线”）
        String lv2_topBlockCode = lv2_topBlockCode__unique(tradeDate);

        return StringUtils.isNotBlank(lv2_topBlockCode) ? Sets.newHashSet(lv2_topBlockCode) : lv3_topBlockCodeSet;
    }


    /**
     * 主线板块（LV2 -> 普通行业   ）    ->     百日新高 - 天数占比Top1       ->      唯一主线（中期 = 短期）
     *
     *
     * 中期 = 短期     ->     100% LV2（唯一主线）
     *
     * @param tradeDate
     * @return
     */
    private String lv2_topBlockCode(LocalDate tradeDate) {


        // 主线板块
        Map<String, Integer> blockCode_count_Map = topBlockService.topBlockRate(BlockNewIdEnum.百日新高.getBlockNewId(), tradeDate, 2, 10);


        // 主线板块   ->   仅取 TOP1 板块
        return MapUtils.isEmpty(blockCode_count_Map) ? null :
                blockCode_count_Map.keySet().iterator().next().split("-")[0];
    }

    /**
     * 主线板块（LV2 -> 普通行业   ）    ->     百日新高 - 天数占比Top1       ->      唯一主线（中期 = 短期）
     *
     *
     * 中期 = 短期     ->     100% LV2（唯一主线）
     *
     * @param tradeDate
     * @return
     */
    private String lv2_topBlockCode__unique(LocalDate tradeDate) {


        // 主线板块
        Map<String, Integer> blockCode_count_Map__N15 = topBlockService.topBlockRate(BlockNewIdEnum.百日新高.getBlockNewId(), tradeDate, 2, 15);
        Map<String, Integer> blockCode_count_Map__N7 = topBlockService.topBlockRate(BlockNewIdEnum.百日新高.getBlockNewId(), tradeDate, 2, 7);


        // 主线板块   ->   仅取 TOP1 板块
        String topBlockCodeSet__db__N15 = MapUtils.isEmpty(blockCode_count_Map__N15) ? null :
                blockCode_count_Map__N15.keySet().iterator().next().split("-")[0];

        String topBlockCodeSet__db__N7 = MapUtils.isEmpty(blockCode_count_Map__N7) ? null :
                blockCode_count_Map__N7.keySet().iterator().next().split("-")[0];


        // 中期 = 短期     ->     100% LV2（唯一主线）
        if (Objects.equals(topBlockCodeSet__db__N15, topBlockCodeSet__db__N7)) {
            return topBlockCodeSet__db__N15;
        }


        // 中期 != 短期     ->     主线 切换期（无 唯一主线）    =>     LV2（唯一主线）    -降级->     LV3（N个 “主线”）
        return null;
    }

    /**
     * 主线板块（LV3 -> 概念 + 细分）    ->     月多 + RPS红(87) + SSF多     ->     N个 “主线”
     *
     * @param data
     * @param tradeDate
     * @return
     */
    private Set<String> lv3_topBlockCodeSet(BacktestCache data, LocalDate tradeDate) {


        Set<String> lv3_topBlockCodeSet = Sets.newHashSet();

        // 主线板块 - 个股涨停数
        Map<String, Integer> topBlock__涨停数_Map = Maps.newHashMap();
        // 主线板块 - 百日新高数
        Map<String, Integer> topBlock__百日新高_Map = Maps.newHashMap();


        data.blockDOList.forEach(blockDO -> {


            String blockCode = blockDO.getCode();


            BlockFun fun = data.getOrCreateBlockFun(blockDO);


            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // ---------------------


            Integer idx = dateIndexMap.get(tradeDate);

            // 过滤当日  ->  未上市/新板块、非LV3
            if (blockDO.getEndLevel() != 1 || extDataArrDTO.date.length == 0 || idx == null || Double.isNaN(extDataArrDTO.rps50[idx])) {
                return;
            }


            // ---------------------


            // --------------------- 主线板块（LV3 -> 概念 + 细分）    ->     月多 + RPS红(87) + SSF多


            boolean 月多 = extDataArrDTO.月多[idx];
            boolean 均线预萌出 = extDataArrDTO.均线预萌出[idx];
            boolean RPS红 = extDataArrDTO.RPS红[idx];
            boolean SSF多 = extDataArrDTO.SSF多[idx];


            if ((月多 || 均线预萌出) && RPS红 && SSF多) {
                lv3_topBlockCodeSet.add(blockCode);


//                // 主线板块 -> 个股列表
//                Set<String> block__stockCodeSet = data.blockCode_stockCodeSet_Map.get(blockCode);
//                block__stockCodeSet.forEach(stockCode -> {
//
//                    StockFun stockFun = data.getFun(stockCode);
//                    Integer stock_idx = stockFun.getDateIndexMap().get(tradeDate);
//
//                    // 板块内个股  ->  当日 涨停
//                    if (null != stock_idx && stockFun.getExtDataArrDTO().涨停[stock_idx]) {
//                        topBlock__涨停数_Map.merge(blockCode, 1, Integer::sum);
//                    }
//                    // 板块内个股  ->  当日 百日新高
//                    if (null != stock_idx && stockFun.getExtDataArrDTO().百日新高[stock_idx]) {
//                        topBlock__百日新高_Map.merge(blockCode, 1, Integer::sum);
//                    }
//                });
            }
        });


        List<String> _topBlock__codeNameSet = lv3_topBlockCodeSet.stream().map(code -> code + "-" + data.block__codeNameMap.get(code)).collect(Collectors.toList());
        log.debug("topBlockCodeSet - 板块-月多2     >>>     [{}] , {} , {}", tradeDate, lv3_topBlockCodeSet.size(), JSON.toJSONString(_topBlock__codeNameSet));


        // -------------------------------------------------------------------------------------------------------------


//        // TODO   排序（上榜天数TOP1 + 主线个股数量TOP1）
//
//
//        // 排序（涨停榜TOP1 + 百日新高榜TOP1）  ->   真 主线板块（1~2个）
//
//
//        // 1、板块  涨停榜TOP1
//        topBlock__涨停数_Map.entrySet().stream()
//                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
//                            .limit(1)
//                            .forEach(entry -> {
//                                String topBlockCode = entry.getKey();
//                                if (entry.getValue() > 10) {
//                                    lv3_topBlockCodeSet.add(topBlockCode);
//                                }
//                            });
//
//        // 2、板块  百日新高榜TOP1
//        topBlock__百日新高_Map.entrySet().stream()
//                              .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
//                              .limit(1)
//                              .forEach(entry -> {
//                                  String topBlockCode = entry.getKey();
//                                  if (entry.getValue() > 20) {
//                                      lv3_topBlockCodeSet.add(topBlockCode);
//                                  }
//                              });
//
//
//        List<String> topBlock__codeNameSet = lv3_topBlockCodeSet.stream().map(code -> code + "-" + data.block__codeNameMap.get(code)).collect(Collectors.toList());
//        log.info("topBlockCodeSet - 真-主线板块（涨停榜TOP1 + 百日新高榜TOP1）    >>>     [{}] , {} , {}", tradeDate, lv3_topBlockCodeSet.size(), JSON.toJSONString(topBlock__codeNameSet));


        // -------------------------------------------------------------------------------------------------------------


        return lv3_topBlockCodeSet;
    }


    public Set<String> top1__topBlockCodeSet__Cache(TopBlockStrategyEnum topBlockStrategyEnum,
                                                    BacktestCache data,
                                                    Set<String> topBlockCodeSet,
                                                    LocalDate tradeDate) {

        Set<String> top1__topBlockCodeSet = data.TOP1__topBlockCache.get(tradeDate, k -> Maps.newConcurrentMap())
                                                                    .computeIfAbsent(topBlockStrategyEnum, kk -> top1__topBlockCodeSet(data, topBlockCodeSet, tradeDate));
        return top1__topBlockCodeSet;
    }

    public Set<String> top1__topBlockCodeSet(BacktestCache data,
                                             Set<String> bkyd2_topBlockCodeSet,
                                             LocalDate tradeDate) {


        if (CollectionUtils.isEmpty(bkyd2_topBlockCodeSet)) {
            return Sets.newHashSet();
        }


        Set<String> top1__lv3_topBlockCodeSet = Sets.newHashSet();

        // 主线板块 - 个股涨停数
        Map<String, Integer> topBlock__涨停数_Map = Maps.newHashMap();
        // 主线板块 - 百日新高数
        Map<String, Integer> topBlock__百日新高_Map = Maps.newHashMap();


        data.blockDOList.forEach(blockDO -> {
            String blockCode = blockDO.getCode();
            if (!bkyd2_topBlockCodeSet.contains(blockCode)) {
                return;
            }


            BlockFun fun = data.getOrCreateBlockFun(blockDO);


            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // ---------------------


            Integer idx = dateIndexMap.get(tradeDate);

            // 过滤当日  ->  未上市/新板块、非LV3
            if (blockDO.getEndLevel() != 1 || extDataArrDTO.date.length == 0 || idx == null || Double.isNaN(extDataArrDTO.rps50[idx])) {
                return;
            }


            // 主线板块 -> 个股列表
            Set<String> block__stockCodeSet = data.blockCode_stockCodeSet_Map.get(blockCode);
            block__stockCodeSet.forEach(stockCode -> {

                // 当前个股 未上市（当前OOM 截取date范围内  ->  当前日期段 未上市）
                if (data.codeStockMap.get(stockCode) == null) {
                    return;
                }


                StockFun stockFun = data.getFun(stockCode);
                Integer stock_idx = stockFun.getDateIndexMap().get(tradeDate);

                // 板块内个股  ->  当日 涨停
                if (null != stock_idx && stockFun.getExtDataArrDTO().涨停[stock_idx]) {
                    topBlock__涨停数_Map.merge(blockCode, 1, Integer::sum);
                }
                // 板块内个股  ->  当日 百日新高
                if (null != stock_idx && stockFun.getExtDataArrDTO().百日新高[stock_idx]) {
                    topBlock__百日新高_Map.merge(blockCode, 1, Integer::sum);
                }
            });
        });


        // -------------------------------------------------------------------------------------------------------------


        // TODO   排序（上榜天数TOP1 + 主线个股数量TOP1）


        // 排序（涨停榜TOP1 + 百日新高榜TOP1）  ->   真 主线板块（1~2个）


        // 1、板块  涨停榜TOP1
        topBlock__涨停数_Map.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .limit(1)
                            .forEach(entry -> {
                                String topBlockCode = entry.getKey();
                                if (entry.getValue() > 10) {
                                    top1__lv3_topBlockCodeSet.add(topBlockCode);
                                }
                            });

        // 2、板块  百日新高榜TOP1
        topBlock__百日新高_Map.entrySet().stream()
                              .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                              .limit(1)
                              .forEach(entry -> {
                                  String topBlockCode = entry.getKey();
                                  if (entry.getValue() > 20) {
                                      top1__lv3_topBlockCodeSet.add(topBlockCode);
                                  }
                              });


        List<String> topBlock__codeNameSet = top1__lv3_topBlockCodeSet.stream().map(code -> code + "-" + data.block__codeNameMap.get(code)).collect(Collectors.toList());
        log.info("topBlockCodeSet - 真-主线板块（涨停榜TOP1 + 百日新高榜TOP1）    >>>     [{}] , {} , {}", tradeDate, top1__lv3_topBlockCodeSet.size(), JSON.toJSONString(topBlock__codeNameSet));


        // -------------------------------------------------------------------------------------------------------------


        return top1__lv3_topBlockCodeSet;
    }


    private Map<String, Boolean> conMap(KlineArrDTO klineArrDTO,
                                        ExtDataArrDTO extDataArrDTO,
                                        ExtDataDTO extDataDTO,
                                        Integer idx) {


        // -------------------------------------------------------------------------------------------------------------


        // double 中期涨幅 = extDataArrDTO.中期涨幅N20[idx];
        // int 趋势支撑线 = extDataArrDTO.趋势支撑线[idx];


        // boolean 高位爆量上影大阴 = extDataArrDTO.高位爆量上影大阴[idx];


        // -------------------------------------------------------------------------------------------------------------


        Map<String, Boolean> conMap = ConvertStockExtData.toBooleanMap(extDataDTO);


        // -------------------------------------------------------------------------------------------------------------


//        boolean XZZB = extDataArrDTO.XZZB[idx];
//
//
//        boolean SSF多 = extDataArrDTO.SSF多[idx];
//        boolean MA20多 = extDataArrDTO.MA20多[idx];
//
//
//        boolean N60日新高 = extDataArrDTO.N60日新高[idx];
//        boolean N100日新高 = extDataArrDTO.N100日新高[idx];
//        boolean 历史新高 = extDataArrDTO.历史新高[idx];
//
//
//        boolean 百日新高 = extDataArrDTO.百日新高[idx];
//
//
//        boolean 月多 = extDataArrDTO.月多[idx];
//        boolean 均线预萌出 = extDataArrDTO.均线预萌出[idx];
//        boolean 均线萌出 = extDataArrDTO.均线萌出[idx];
//        boolean 小均线多头 = extDataArrDTO.小均线多头[idx];
//        boolean 大均线多头 = extDataArrDTO.大均线多头[idx];
//        boolean 均线大多头 = extDataArrDTO.均线大多头[idx];
//        boolean 均线极多头 = extDataArrDTO.均线极多头[idx];
//
//
//        boolean RPS红 = extDataArrDTO.RPS红[idx];
//        boolean RPS一线红 = extDataArrDTO.RPS一线红[idx];
//        boolean RPS双线红 = extDataArrDTO.RPS双线红[idx];
//        boolean RPS三线红 = extDataArrDTO.RPS三线红[idx];


        // -------------------------------------------------------------------------------------------------------------


//        boolean 百日新高 = false;
//        for (int i = 0; i < 10; i++) {
//            int n_Idx = idx - i;
//            if (n_Idx < 0) {
//                break;
//            }
//
//            boolean _N100日新高 = extDataArrDTO.N100日新高[n_Idx];   // 近10日内 N100日新高
//            if (_N100日新高 && (SSF多 || MA20多)) {
//                百日新高 = true;
//                break;
//            }
//        }


        // -------------------------------------------------------------------------------------------------------------


//        Map<String, Boolean> conMap = Maps.newHashMap();
//
//
//        conMap.put("XZZB", XZZB);
//
//
//        conMap.put("SSF多", SSF多);
//        conMap.put("MA20多", MA20多);
//
//
//        conMap.put("N60日新高", N60日新高);
//        conMap.put("N100日新高", N100日新高);
//        conMap.put("历史新高", 历史新高);
//
//
//        conMap.put("百日新高", 百日新高);
//
//
//        conMap.put("月多", 月多);
//        conMap.put("均线预萌出", 均线预萌出);
//        conMap.put("均线萌出", 均线萌出);
//        conMap.put("小均线多头", 小均线多头);
//        conMap.put("大均线多头", 大均线多头);
//        conMap.put("均线大多头", 均线大多头);
//        conMap.put("均线极多头", 均线极多头);
//
//
//        conMap.put("RPS红", RPS红);
//        conMap.put("RPS一线红", RPS一线红);
//        conMap.put("RPS双线红", RPS双线红);
//        conMap.put("RPS三线红", RPS三线红);


        // -------------------------------------------------------------------------------------------------------------


        // MA200多  =  C>MA200  &&  MA200>prev_MA200
        boolean MA200多 = klineArrDTO.close[idx] >= extDataArrDTO.MA200[idx]
                && extDataArrDTO.MA200[idx] >= extDataArrDTO.MA200[idx - 1];

        boolean MA250多 = klineArrDTO.close[idx] >= extDataArrDTO.MA250[idx]
                && extDataArrDTO.MA250[idx] >= extDataArrDTO.MA250[idx - 1];


        boolean 上MA200 = klineArrDTO.close[idx] >= extDataArrDTO.MA200[idx];
        boolean 上MA250 = klineArrDTO.close[idx] >= extDataArrDTO.MA250[idx];


        conMap.put("MA200多", MA200多);
        conMap.put("MA250多", MA250多);
        conMap.put("上MA200", 上MA200);
        conMap.put("上MA250", 上MA250);


        // -------------------------------------------------------------------------------------------------------------


        // ------------------------------------------- 低吸 -------------------------------------------------------------


        double C_SSF_偏离率 = extDataArrDTO.C_SSF_偏离率[idx];
        conMap.put("C_SSF_偏离率<5", C_SSF_偏离率 < 5);


        // ------------------------------------------- 低吸 -------------------------------------------------------------


//        double C_MA_偏离率 = extDataDTO.getC_MA5_偏离率();
        double C_MA_短期偏离率 = extDataDTO.getC_短期MA_偏离率();
//        double C_MA_中期偏离率 = extDataDTO.getC_中期MA_偏离率();
//        double C_MA_长期偏离率 = extDataDTO.getC_长期MA_偏离率();
        conMap.put("C_MA_偏离率<3", C_MA_短期偏离率 < 3);
        conMap.put("C_MA_偏离率<5", C_MA_短期偏离率 < 5);
        conMap.put("C_MA_偏离率<7", C_MA_短期偏离率 < 7);


//        int 短期趋势支撑线 = extDataDTO.get短期支撑线();
//        int 中期趋势支撑线 = extDataDTO.get中期支撑线();
//
//        conMap.put("短期趋势支撑线", 短期趋势支撑线);
//        conMap.put("中期趋势支撑线", 中期趋势支撑线);


        // ------------------------------------------- 低吸 -------------------------------------------------------------


        double C_MA5_偏离率 = extDataDTO.getC_MA5_偏离率();
        double C_MA10_偏离率 = extDataDTO.getC_MA10_偏离率();
        double C_MA20_偏离率 = extDataDTO.getC_MA20_偏离率();
        double C_MA30_偏离率 = extDataDTO.getC_MA30_偏离率();
        double C_MA50_偏离率 = extDataDTO.getC_MA50_偏离率();
        double C_MA60_偏离率 = extDataDTO.getC_MA60_偏离率();
        double C_MA100_偏离率 = extDataDTO.getC_MA100_偏离率();


        conMap.put("C_MA5_偏离率<5", C_MA5_偏离率 < 5);
        conMap.put("C_MA10_偏离率<5", C_MA10_偏离率 < 5);
        conMap.put("C_MA20_偏离率<5", C_MA20_偏离率 < 5);
        conMap.put("C_MA30_偏离率<5", C_MA30_偏离率 < 5);
        conMap.put("C_MA50_偏离率<5", C_MA50_偏离率 < 5);
        conMap.put("C_MA60_偏离率<5", C_MA60_偏离率 < 5);
        conMap.put("C_MA100_偏离率<5", C_MA100_偏离率 < 5);


        // ------------------------------------------- 限高 -------------------------------------------------------------


        double 中期涨幅 = extDataArrDTO.中期涨幅N20[idx];


        conMap.put("中期涨幅<35", 中期涨幅 < 35);
        conMap.put("中期涨幅<50", 中期涨幅 < 50);
        conMap.put("中期涨幅<100", 中期涨幅 < 100);


        // -------------------------------------------------------------------------------------------------------------


        return conMap;
    }


    /**
     * buySignalInfo
     *
     * @param buy_infoMap
     * @param stockCode
     * @param data
     * @param idx
     * @param conMap
     */
    private void buySignalInfo(Map<String, String> buy_infoMap,
                               String stockCode,
                               BacktestCache data,
                               Integer idx,
                               Map<String, Boolean> conMap) {


        // 动态收集所有为 true 的信号名称，按固定顺序拼接
        List<String> signalInfoList = Lists.newArrayList();


        // ---------------------------------------------------------------------------


        // 行业板块
        String pthyLv2 = data.getPthyLv2(stockCode);
        String getYjhyLv1 = data.getYjhyLv1(stockCode);
        signalInfoList.add(pthyLv2);
        signalInfoList.add(getYjhyLv1 + "|");


        // ---------------------------------------------------------------------------

        // conList
        conMap.forEach((k, v) -> {

            // "N60日新高" - true/false

            if (v) {
                signalInfoList.add(k);
            }
        });


        // ---------------------------------------------------------------------------


        signalInfoList.add("|idx-" + idx);


        // ---------------------------------------------------------------------------


        // stockCode - infoList
        buy_infoMap.put(stockCode, String.join(",", signalInfoList));
    }


}