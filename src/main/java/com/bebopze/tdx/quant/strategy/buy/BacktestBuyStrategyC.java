package com.bebopze.tdx.quant.strategy.buy;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.BlockNewIdEnum;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.service.TopBlockService;
import com.bebopze.tdx.quant.strategy.QuickOption;
import com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.util.BoolUtil.bool2Int;


/**
 * 回测 - B策略
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestBuyStrategyC implements BuyStrategy {


    @Autowired
    private MarketService marketService;

    @Autowired
    private TopBlockService topBlockService;

    @Autowired
    private BacktestBuyStrategyA backtestBuyStrategyA;

    @Lazy
    @Autowired
    private BacktestStrategy backtestStrategy;


    @Override
    public String key() {
        return "C";
    }


    @Override
    public List<String> rule(BacktestCache data, LocalDate tradeDate, Map<String, String> buy_infoMap, double posRate) {
        return Lists.newArrayList();
    }


    /**
     * 买入策略   =   大盘（70%） +  主线板块（25%） +  个股买点（5%）
     *
     * @param topBlockStrategyEnum 主线策略
     * @param buyConList           B策略
     * @param data                 全量行情
     * @param tradeDate            交易日期
     * @param buy_infoMap          买入个股-交易信号
     * @param posRate              当前 总仓位
     * @return
     */
    @TotalTime
    public List<String> rule2(TopBlockStrategyEnum topBlockStrategyEnum,
                              List<String> buyConList,
                              BacktestCache data,
                              LocalDate tradeDate,
                              Map<String, String> buy_infoMap,
                              double posRate) {


        // -------------------------------------------------------------------------------------------------------------
        //                                                1、大盘牛熊
        // -------------------------------------------------------------------------------------------------------------


        QaMarketMidCycleDO marketInfo = marketService.marketInfo(tradeDate);

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
        Set<String> topBlockCodeSet = topBlock(topBlockStrategyEnum, data, tradeDate);
        log.info("BacktestBuyStrategyC - topBlock     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_1));


        // -------------------------------------------------------------------------------------------------------------
        //                                                3、（强势）个股
        // -------------------------------------------------------------------------------------------------------------


        // B策略   ->   强势个股
        long start_2 = System.currentTimeMillis();
        List<String> buy__topStock__codeList = buy__topStock__codeList(buyConList, data, tradeDate, buy_infoMap);
        log.info("BacktestBuyStrategyC - buy__topStock__codeList     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_2));


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股 -> IN 主线板块
        // -------------------------------------------------------------------------------------------------------------


        // 强势个股   ->   IN 主线板块
        long start_3 = System.currentTimeMillis();
        List<String> inTopBlock__stockCodeList = inTopBlock__stockCodeList(topBlockCodeSet, buy__topStock__codeList, data, tradeDate);
        log.info("BacktestBuyStrategyC - inTopBlock__stockCodeList     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_3));


        // -------------------------------------------------------------------------------------------------------------


        // 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）

        long start_4 = System.currentTimeMillis();
        backtestBuyStrategyA.buyStrategy_ETF(inTopBlock__stockCodeList, data, tradeDate, buy_infoMap, posRate);
        log.info("BacktestBuyStrategyC - buyStrategy_ETF     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_4));


        // -------------------------------------------------------------------------------------------------------------


        // B策略 - S策略   相互冲突bug       =>       以 S策略 为准       ->       出现 S信号 个股不能买入（buyList -> 剔除）
        // backtestStrategy.buy_sell__signalConflict(data, tradeDate, inTopBlock__stockCodeList);


        // -------------------------------------------------------------------------------------------------------------


        // TODO     按照 规则打分 -> sort
        long start_5 = System.currentTimeMillis();
        List<String> sort__stockCodeList = scoreSort(inTopBlock__stockCodeList, data, tradeDate, 100);
        log.info("BacktestBuyStrategyC - scoreSort     >>>     totalTime : {}", DateTimeUtil.formatNow2Hms(start_5));


        return sort__stockCodeList;
    }


    /**
     * 强势个股   ->   IN 主线板块
     *
     * @param topBlockCodeSet         主线板块
     * @param buy__topStock__codeList 强势个股
     * @param data
     * @param tradeDate
     * @return
     */
    public List<String> inTopBlock__stockCodeList(Set<String> topBlockCodeSet,
                                                  List<String> buy__topStock__codeList,
                                                  BacktestCache data,
                                                  LocalDate tradeDate) {


        // 强势个股   ->   IN 主线板块
        List<String> inTopBlock__stockCodeList = buy__topStock__codeList
                .stream()
                .filter(stockCode -> {


                    // 个股   -对应->   板块列表
                    Set<String> stock__blockCodeSet = data.stockCode_blockCodeSet_Map.getOrDefault(stockCode, Sets.newHashSet());

                    for (String stock__blockCode : stock__blockCodeSet) {


                        // 个股   ->   IN 主线板块
                        boolean inTopBlock = topBlockCodeSet.contains(stock__blockCode);

                        if (inTopBlock) {


                            // 个股   ->   B-single   主线板块
                            data.stockCode_topBlockCache.get(stockCode, k -> Sets.newConcurrentHashSet()).add(stock__blockCode);


                            log.debug("个股 -> IN 主线板块     >>>     {} , [{}-{}] , [{}-{}]", tradeDate,
                                      stockCode, data.stock__codeNameMap.get(stockCode),
                                      stock__blockCode, data.block__codeNameMap.get(stock__blockCode));


                            return true;
                        }
                    }


                    return false;
                }).collect(Collectors.toList());


        return inTopBlock__stockCodeList;
    }


    /**
     * B策略   ->   强势个股
     *
     * @param buyConList  B策略
     * @param data
     * @param tradeDate
     * @param buy_infoMap
     * @return
     */
    private List<String> buy__topStock__codeList(List<String> buyConList,
                                                 BacktestCache data,
                                                 LocalDate tradeDate,
                                                 Map<String, String> buy_infoMap) {


//        List<String> buy__topStock__CodeList = Collections.synchronizedList(Lists.newArrayList());
//
//        ParallelCalcUtil.forEach(data.stockDOList,
//                                 stockDO -> {


        List<String> buy__topStock__codeList = Lists.newArrayList();
        data.stockDOList.forEach(stockDO -> {


            String stockCode = stockDO.getCode();


            StockFun fun = data.stockFunCache.get(stockCode, k -> new StockFun(stockDO));

            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // -------------------------------------------


            // 当日 - 停牌（003005  ->  2022-10-27）
            Integer idx = dateIndexMap.get(tradeDate);

            // 过滤 停牌/新股
            if (idx == null || idx < 50) {
                return;
            }


            // -----------------------------------------------------------------------------------------


            Map<String, Boolean> conMap = conMap(extDataArrDTO, idx);


            // -----------------------------------------------------------------------------------------


            // 是否买入       =>       conList   ->   全为 true
            boolean signal_B = BuyStrategy__ConCombiner.calcCon(buyConList, conMap);


            if (signal_B) {

                buy__topStock__codeList.add(stockCode);


                // ----------------------------------------------------- buySingleInfo


                buySingleInfo(buy_infoMap, stockCode, data, idx, conMap);
            }
        });


//                                 },
//                                 ThreadPoolType.CPU_INTENSIVE);


        return buy__topStock__codeList;
    }


    /**
     * 主线板块
     *
     * @param topBlockStrategyEnum
     * @param data
     * @param tradeDate
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


        // TdxBlockNewWriter.write("ZXBK-LV2", Sets.newHashSet(lv2_topBlockCode));


        // -------------------------------------------------------------------------------------------------------------
        //                                      3、主线板块（LV3 -> 概念 + 细分）
        // -------------------------------------------------------------------------------------------------------------


        // 主线板块（N个）    ->     LV3（概念 + 细分）
        Set<String> lv3_topBlockCodeSet = lv3_topBlockCodeSet(data, tradeDate);


        // TdxBlockNewWriter.write("ZXBK-LV3", lv3_topBlockCodeSet);


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


        data.blockDOList.forEach(blockDO -> {


            String blockCode = blockDO.getCode();


            BlockFun fun = data.blockFunCache.get(blockCode, x -> new BlockFun(x, blockDO));


            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // ---------------------


            Integer idx = dateIndexMap.get(tradeDate);

            // 过滤当日  ->  未上市/新板块、非LV3
            if (blockDO.getEndLevel() != 1 || extDataArrDTO.date.length == 0 || idx == null || idx < 50) {
                return;
            }


            // ---------------------


            // --------------------- 主线板块（LV3 -> 概念 + 细分）    ->     月多 + RPS红(87) + SSF多


            boolean 月多 = extDataArrDTO.月多[idx];
            boolean RPS红 = extDataArrDTO.RPS红[idx];
            boolean SSF多 = extDataArrDTO.SSF多[idx];


            if (月多 && RPS红 && SSF多) {
                lv3_topBlockCodeSet.add(blockCode);
            }
        });


        List<String> topBlock__codeNameSet = lv3_topBlockCodeSet.stream().map(code -> code + "-" + data.block__codeNameMap.get(code)).collect(Collectors.toList());
        log.info("topBlockCodeSet - 板块-月多2     >>>     [{}] , {} , {}", tradeDate, lv3_topBlockCodeSet.size(), JSON.toJSONString(topBlock__codeNameSet));


        return lv3_topBlockCodeSet;
    }


    private Map<String, Boolean> conMap(ExtDataArrDTO extDataArrDTO, Integer idx) {


        // -------------------------------------------------------------------------------------------------------------


        // double 中期涨幅 = extDataArrDTO.中期涨幅[idx];
        // int 趋势支撑线 = extDataArrDTO.趋势支撑线[idx];


        // boolean 高位爆量上影大阴 = extDataArrDTO.高位爆量上影大阴[idx];


        // -------------------------------------------------------------------------------------------------------------


        boolean SSF多 = extDataArrDTO.SSF多[idx];
        boolean MA20多 = extDataArrDTO.MA20多[idx];


        boolean N60日新高 = extDataArrDTO.N60日新高[idx];
        boolean N100日新高 = extDataArrDTO.N100日新高[idx];
        boolean 历史新高 = extDataArrDTO.历史新高[idx];


        boolean 月多 = extDataArrDTO.月多[idx];
        boolean 均线预萌出 = extDataArrDTO.均线预萌出[idx];
        boolean 均线萌出 = extDataArrDTO.均线萌出[idx];
        boolean 大均线多头 = extDataArrDTO.大均线多头[idx];


        boolean RPS红 = extDataArrDTO.RPS红[idx];
        boolean RPS一线红 = extDataArrDTO.RPS一线红[idx];
        boolean RPS双线红 = extDataArrDTO.RPS双线红[idx];
        boolean RPS三线红 = extDataArrDTO.RPS三线红[idx];


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        Map<String, Boolean> conMap = Maps.newHashMap();


        conMap.put("SSF多", SSF多);
        conMap.put("MA20多", MA20多);


        conMap.put("N60日新高", N60日新高);
        conMap.put("N100日新高", N100日新高);
        conMap.put("历史新高", 历史新高);


        conMap.put("月多", 月多);
        conMap.put("均线预萌出", 均线预萌出);
        conMap.put("均线萌出", 均线萌出);
        conMap.put("大均线多头", 大均线多头);


        conMap.put("RPS红", RPS红);
        conMap.put("RPS一线红", RPS一线红);
        conMap.put("RPS双线红", RPS双线红);
        conMap.put("RPS三线红", RPS三线红);


        return conMap;
    }


    /**
     * buySingleInfo
     *
     * @param buy_infoMap
     * @param stockCode
     * @param data
     * @param idx
     * @param conMap
     */
    private void buySingleInfo(Map<String, String> buy_infoMap,
                               String stockCode,
                               BacktestCache data,
                               Integer idx,
                               Map<String, Boolean> conMap) {


        // 动态收集所有为 true 的信号名称，按固定顺序拼接
        List<String> singleInfoList = Lists.newArrayList();


        // ---------------------------------------------------------------------------


        // 行业板块
        String pthyLv2 = data.getPthyLv2(stockCode);
        String getYjhyLv1 = data.getYjhyLv1(stockCode);
        singleInfoList.add(pthyLv2);
        singleInfoList.add(getYjhyLv1 + "     ");


        // ---------------------------------------------------------------------------

        // conList
        conMap.forEach((k, v) -> {

            // "N60日新高" - true/false

            if (v) {
                singleInfoList.add(k);
            }
        });


        // ---------------------------------------------------------------------------


        singleInfoList.add("idx-" + idx);


        // ---------------------------------------------------------------------------


        // stockCode - infoList
        buy_infoMap.put(stockCode, String.join(",", singleInfoList));
    }


    /**
     * 权重规则   排序
     *
     * @param stockCodeList
     * @param data
     * @param tradeDate
     * @param N
     * @return
     */
    public static List<String> scoreSort(Collection<String> stockCodeList,
                                         BacktestCache data,
                                         LocalDate tradeDate,
                                         int N) {


        if (stockCodeList.size() <= N) {
            return Lists.newArrayList(stockCodeList);
        }


        Map<String, String> codeNameMap = data.stock__codeNameMap;


        // ----------------- 规则排名

        // 金额 -> 涨幅榜（近10日） -> ...

        // TODO     RPS（50） -> 大均线多头（20） -> 60日新高（10） -> 涨幅榜（10） -> 成交额-近10日（10） -> ...


        // Step 2: 计算各项指标 & 打分
        List<QuickOption.StockScore> scoredStocks = Lists.newArrayList();


        // 用于归一化处理
        double maxRPS和 = 0;
        double maxAmount = 0;
        double maxMidReturn = 0;
        double max大均线多头 = 0;
        double maxN日新高 = 0;


        // Step 2.1: 遍历所有股票，计算原始值
        for (String stockCode : stockCodeList) {
            String stockName = codeNameMap.get(stockCode);


            // -------------------------------------------------------------------------------------------


            // BUY策略   ->   已完成init
            // StockFun fun = data.stockFunCache.getIfPresent(stockCode);
            // StockFun fun = data.stockFunCache.get(stockCode, k -> new StockFun(data.codeStockMap.get(stockCode)));
            StockFun fun = data.getOrCreateStockFun(stockCode);


            KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();


            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();
            Integer idx = dateIndexMap.get(tradeDate);


            double[] amoArr = klineArrDTO.amo;


            double[] rps10_arr = extDataArrDTO.rps10;
            double[] rps20_arr = extDataArrDTO.rps20;
            double[] rps50_arr = extDataArrDTO.rps50;
            double[] rps120_arr = extDataArrDTO.rps120;
            double[] rps250_arr = extDataArrDTO.rps250;


            double[] 中期涨幅_arr = extDataArrDTO.中期涨幅;


            boolean[] 大均线多头_arr = extDataArrDTO.大均线多头;
            boolean[] N60日新高_arr = extDataArrDTO.N60日新高;


            double rps10 = rps10_arr[idx];
            double rps20 = rps20_arr[idx];
            double rps50 = rps50_arr[idx];
            double rps120 = rps120_arr[idx];
            double rps250 = rps250_arr[idx];


            // RPS和
            double RPS和1 = rps10 + rps20 + rps50;
            double RPS和2 = rps50 + rps120 + NumUtil.NaN_0(rps250);

            double RPS和 = Math.max(RPS和1, RPS和2);


            // AMO
            double amount = amoArr[idx];


            // 中期涨幅
            double 中期涨幅 = 中期涨幅_arr[idx];

            // 新高天数
            // int 新高天数 = 新高天数_arr[idx];


            // 大均线多头
            int 大均线多头 = bool2Int(大均线多头_arr[idx]);
            // 60日新高
            int N日新高 = bool2Int(N60日新高_arr[idx]);


            // -------------------------------------------------------------------------------------------


            // 更新最大值用于归一化
            maxRPS和 = Math.max(maxRPS和, RPS和);
            maxMidReturn = Math.max(maxMidReturn, 中期涨幅);
            max大均线多头 = Math.max(max大均线多头, 大均线多头);
            maxN日新高 = Math.max(maxN日新高, N日新高);
            maxAmount = Math.max(maxAmount, amount);


            scoredStocks.add(new QuickOption.StockScore(stockCode, stockName, RPS和, 中期涨幅, 大均线多头, N日新高, amount, 0));
        }


        // Step 3: 归一化 & 加权打分
        for (QuickOption.StockScore s : scoredStocks) {


            // RPS（50） ->  成交额-近10日（10） ->  大均线多头（10） ->  60日新高（10） ->  涨幅榜（10）  ->   ...


            double rpsScore = NaNor0(maxRPS和) ? 0 : s.RPS和 / maxRPS和 * 50;                         // 权重50%

            // double 新高天数Score = 新高天数 == 0 ? 0 : s.RPS和 / maxRPS和 * 50;                      // 权重30%（新高天数）

            double amountScore = NaNor0(maxAmount) ? 0 : s.amount / maxAmount * 20;                  // 权重20%
            double 大均线Score = NaNor0(max大均线多头) ? 0 : s.大均线多头 / max大均线多头 * 10;            // 权重10%
            double 新高Score = NaNor0(maxN日新高) ? 0 : s.N日新高 / maxN日新高 * 10;                     // 权重10%
            double midScore = NaNor0(maxMidReturn) ? 0 : s.midTermChangePct / maxMidReturn * 10;     // 权重10%


            s.score = NumUtil.of(rpsScore + amountScore + 大均线Score + 新高Score + midScore);


//            if (Double.isNaN(s.score)) {
//                log.debug("scoreSort - NaN     >>>     rpsScore : {} , amountScore : {} , 大均线Score : {} , 新高Score : {} , midScore : {} , score : {}",
//                          rpsScore, amountScore, 大均线Score, 新高Score, midScore, s.score);
//            }

        }


        // Step 4: 按得分排序，取前N名
        List<QuickOption.StockScore> topNStocks = scoredStocks.stream()
                                                              .sorted(Comparator.comparing((QuickOption.StockScore::getScore)).reversed())
                                                              .limit(N)
                                                              .collect(Collectors.toList());


        // 输出结果或进一步操作
        if (topNStocks.size() < scoredStocks.size() /*|| scoredStocks.size() > N / 2*/) {
            log.debug("scoreSort     >>>     前->后 : [{}->{}] , topNStocks : {}",
                      scoredStocks.size(), topNStocks.size(), JSON.toJSONString(topNStocks));
        }


        return topNStocks.stream().map(QuickOption.StockScore::getStockCode).collect(Collectors.toList());
    }


    private static boolean NaNor0(double val) {
        return Double.isNaN(val) || val == 0;
    }


}