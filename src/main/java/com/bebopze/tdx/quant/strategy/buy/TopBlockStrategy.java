package com.bebopze.tdx.quant.strategy.buy;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.constant.BlockNewIdEnum;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.service.TopBlockService;
import com.bebopze.tdx.quant.service.impl.TopBlockServiceImpl;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 主线策略
 *
 * @author: bebopze
 * @date: 2026/1/30
 */
@Slf4j
@Component
public class TopBlockStrategy {


    @Autowired
    private MarketService marketService;

    @Autowired
    private TopBlockService topBlockService;


    /**
     * 指定   主线策略 + 日期     ->     主线板块 列表
     *
     * @param topBlockStrategyEnum 主线策略
     * @param data
     * @param tradeDate            日期
     * @param top1TopBlockFlag     2次过滤（1~5个）：true-取TOP1（1~2个）；false-取TOP5（1~5个）
     * @return
     */
    public Set<String> topBlock(TopBlockStrategyEnum topBlockStrategyEnum, BacktestCache data, LocalDate tradeDate,
                                boolean top1TopBlockFlag) {


        // --------------------------------------------- 主线板块（板块-月多2）列表（10~20个）    =>     内部增加了 1次 top1__topBlockCodeSet(false)   ->   初次过滤（false：1~5个）
        Set<String> topBlockCodeSet = topN__topBlockCodeSet__Cache(topBlockStrategyEnum, data, tradeDate);


        // --------------------------------------------- 2次过滤（true：1~2个）
        if (top1TopBlockFlag) {
            // 板块-月多2     +     涨停TOP1 + 百日新高TOP1
            topBlockCodeSet = top1__topBlockCodeSet__Cache(topBlockStrategyEnum, data, topBlockCodeSet, tradeDate, top1TopBlockFlag);
        }


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
        //                                      topN 初次过滤（涨停TOP + 百日新高TOP）  ->   1~5个
        // -------------------------------------------------------------------------------------------------------------


        // 内部增加 1次 top1__topBlockCodeSet(false)   ->   初次过滤（false：1~5个）


        // topN（涨停TOP + 百日新高TOP）  ->   false-取TOP5（1~5个）
        Set<String> topN__topBlockCodeSet = top1__topBlockCodeSet(data, topBlockCodeSet, tradeDate, false);


        // -------------------------------------------------------------------------------------------------------------


        topN__topBlockCodeSet.removeIf(StringUtils::isBlank);
        return topN__topBlockCodeSet;
    }


//    /**
//     * 主线板块（LV2 -> 普通行业   ）    ->     百日新高 - 天数占比Top1       ->      唯一主线
//     * 主线板块（LV3 -> 概念 + 细分）    ->     月多 + RPS红(87) + SSF多     ->     N个 “主线”
//     *
//     * @param topBlockStrategyEnum
//     * @param data
//     * @param tradeDate
//     * @return
//     */
//    private Set<String> topBlockStrategy2(TopBlockStrategyEnum topBlockStrategyEnum,
//                                          BacktestCache data,
//                                          LocalDate tradeDate) {
//
//
//        if (Objects.equals(topBlockStrategyEnum, TopBlockStrategyEnum.LV1)) {
//
//
//            // ---------------------------------------------------------------------------------------------------------
//            //                                      1、主线板块（LV1 -> 研究行业）
//            // ---------------------------------------------------------------------------------------------------------
//
//
//            // TODO   暂不考虑
//            //
//            //
//
//
//        } else if (Objects.equals(topBlockStrategyEnum, TopBlockStrategyEnum.LV2)) {
//
//
//            // ---------------------------------------------------------------------------------------------------------
//            //                                      2、主线板块（LV2 -> 普通行业）
//            // ---------------------------------------------------------------------------------------------------------
//
//
//            // 主线板块（唯一）    ->     LV2（普通行业） /  LV1（研究行业）
//            String lv2_topBlockCode = lv2_topBlockCode__unique(tradeDate);
//
//            return Sets.newHashSet(lv2_topBlockCode);
//
//
//        } else if (Objects.equals(topBlockStrategyEnum, TopBlockStrategyEnum.LV3)) {
//
//            // ---------------------------------------------------------------------------------------------------------
//            //                                      3、主线板块（LV3 -> 概念 + 细分）
//            // ---------------------------------------------------------------------------------------------------------
//
//
//            // 主线板块（N个）    ->     LV3（概念 + 细分）
//            Set<String> lv3_topBlockCodeSet = lv3_topBlockCodeSet(data, tradeDate);
//
//            return lv3_topBlockCodeSet;
//
//
//        } else if (Objects.equals(topBlockStrategyEnum, TopBlockStrategyEnum.LV2_LV3)) {
//
//
//            // ---------------------------------------------------------------------------------------------------------
//            //                                      4、主线板块（LV2普通行业   <- 升级/降级 ->   LV3 概念 + 细分）
//            // ---------------------------------------------------------------------------------------------------------
//
//
//            // LV2_LV3
//
//            // 中期  = 短期     ->     100% LV2 （   唯一主线）
//            // 中期 != 短期     ->     主线 切换期（无 唯一主线）    =>     LV2（唯一主线）    -降级->     LV3（N个 “主线”）
//
//            Set<String> lv2_lv3__topBlockCodeSet = lv2_lv3__topBlockCodeSet(tradeDate, lv3_topBlockCodeSet(data, tradeDate));
//
//
//            return lv2_lv3__topBlockCodeSet;
//        }
//
//
//        throw new BizException("topBlockStrategyEnum=[" + topBlockStrategyEnum + "]有误！");
//    }


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
            }
        });


        List<String> topBlock__codeNameSet = lv3_topBlockCodeSet.stream().map(code -> code + "-" + data.block__codeNameMap.get(code)).collect(Collectors.toList());
        log.info("topBlockCodeSet - 板块-月多2     >>>     [{}] , {} , {}", tradeDate, lv3_topBlockCodeSet.size(), JSON.toJSONString(topBlock__codeNameSet));


        return lv3_topBlockCodeSet;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * topN（涨停TOP + 百日新高TOP）  ->   topN 主线板块（1~5个）      // Cache
     *
     * @param topBlockStrategyEnum
     * @param data
     * @param tradeDate
     * @return
     */
    private Set<String> topN__topBlockCodeSet__Cache(TopBlockStrategyEnum topBlockStrategyEnum,
                                                     BacktestCache data,
                                                     LocalDate tradeDate) {

        Set<String> topN__topBlockCodeSet = data.topBlockCache.get(tradeDate, k -> Maps.newConcurrentMap())
                                                              .computeIfAbsent(topBlockStrategyEnum, kk -> topBlockStrategy(topBlockStrategyEnum, data, tradeDate));


        return topN__topBlockCodeSet;
    }


    /**
     * top1（涨停TOP + 百日新高TOP）  ->   真 主线板块（1~2个）      // Cache
     *
     * @param topBlockStrategyEnum
     * @param data
     * @param topBlockCodeSet
     * @param tradeDate
     * @param top1TopBlockFlag     2次过滤（1~5个）：true-取TOP1（1~2个）；false-取TOP5（1~5个）
     * @return
     */
    private Set<String> top1__topBlockCodeSet__Cache(TopBlockStrategyEnum topBlockStrategyEnum,
                                                     BacktestCache data,
                                                     Set<String> topBlockCodeSet,
                                                     LocalDate tradeDate,
                                                     boolean top1TopBlockFlag) {

        Set<String> top1__topBlockCodeSet = data.TOP1__topBlockCache.get(tradeDate, k -> Maps.newConcurrentMap())
                                                                    .computeIfAbsent(topBlockStrategyEnum, kk -> top1__topBlockCodeSet(data, topBlockCodeSet, tradeDate, top1TopBlockFlag));
        return top1__topBlockCodeSet;
    }


    /**
     * topN（涨停TOP + 百日新高TOP）  ->   真 主线板块（1~2个）
     *
     * @param data
     * @param bkyd2_topBlockCodeSet
     * @param tradeDate
     * @param top1TopBlockFlag      2次过滤（1~5个）：true-取TOP1（1~2个）；false-取TOP5（1~5个）
     * @return
     */
    private Set<String> top1__topBlockCodeSet(BacktestCache data,
                                              Set<String> bkyd2_topBlockCodeSet,
                                              LocalDate tradeDate,
                                              boolean top1TopBlockFlag) {


        if (CollectionUtils.isEmpty(bkyd2_topBlockCodeSet)) {
            return Sets.newHashSet();
        }


        Set<String> top1__lv3_topBlockCodeSet = Sets.newHashSet();

        // 主线板块 - 个股涨停数
        Map<String, Integer> topBlock__涨停数_Map = Maps.newHashMap();
        // 主线板块 - 百日新高数
        Map<String, Integer> topBlock__百日新高_Map = Maps.newHashMap();


        // ------------------------------------- 方式1：topBlock 预计算 --------------------------------------------------


        List<TopBlockServiceImpl.BlockTopInfoDTO> zt_blockTopInfoDTOS = topBlockService.topBlockInfo(BlockNewIdEnum.涨停.getBlockNewId(), tradeDate);
        List<TopBlockServiceImpl.BlockTopInfoDTO> dt_blockTopInfoDTOS = topBlockService.topBlockInfo(BlockNewIdEnum.跌停.getBlockNewId(), tradeDate);
        List<TopBlockServiceImpl.BlockTopInfoDTO> 百日新高_blockTopInfoDTOS = topBlockService.topBlockInfo(BlockNewIdEnum.百日新高.getBlockNewId(), tradeDate);


        zt_blockTopInfoDTOS.forEach(dto -> {
            String blockCode = dto.getBlockCode();
            if (bkyd2_topBlockCodeSet.contains(blockCode)) {
                topBlock__涨停数_Map.put(blockCode, dto.getSize());
            }
        });


        dt_blockTopInfoDTOS.forEach(dto -> {
            String blockCode = dto.getBlockCode();
            if (bkyd2_topBlockCodeSet.contains(blockCode)) {
                topBlock__涨停数_Map.merge(blockCode, -dto.getSize(), Integer::sum);
            }
        });


        百日新高_blockTopInfoDTOS.forEach(dto -> {
            String blockCode = dto.getBlockCode();
            if (bkyd2_topBlockCodeSet.contains(blockCode)) {
                topBlock__百日新高_Map.put(blockCode, dto.getSize());
            }
        });


//        // ------------------------------------- 方式2：extData 实时计算 -----------------------------------------------
//
//
//        data.blockDOList.forEach(blockDO -> {
//            String blockCode = blockDO.getCode();
//            if (!bkyd2_topBlockCodeSet.contains(blockCode)) {
//                return;
//            }
//
//
//            BlockFun fun = data.getOrCreateBlockFun(blockDO);
//
//
//            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
//            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();
//
//
//            // ---------------------
//
//
//            Integer idx = dateIndexMap.get(tradeDate);
//
//            // 过滤当日  ->  未上市/新板块、非LV3
//            if (blockDO.getEndLevel() != 1 || extDataArrDTO.date.length == 0 || idx == null || Double.isNaN(extDataArrDTO.rps50[idx])) {
//                return;
//            }
//
//
//            // 主线板块 -> 个股列表
//            Set<String> block__stockCodeSet = data.blockCode_stockCodeSet_Map.get(blockCode);
//            block__stockCodeSet.forEach(stockCode -> {
//
//                // 当前个股 未上市（当前OOM 截取date范围内  ->  当前日期段 未上市）
//                if (data.codeStockMap.get(stockCode) == null) {
//                    return;
//                }
//
//
//                StockFun stockFun = data.getFun(stockCode);
//                Integer stock_idx = stockFun.getDateIndexMap().get(tradeDate);
//
//                // 板块内个股  ->  当日 涨停
//                if (null != stock_idx && stockFun.getExtDataArrDTO().涨停[stock_idx]) {
//                    topBlock__涨停数_Map.merge(blockCode, 1, Integer::sum);
//                }
//                if (null != stock_idx && stockFun.getExtDataArrDTO().跌停[stock_idx]) {
//                    topBlock__涨停数_Map.merge(blockCode, -1, Integer::sum);
//                }
//                // 板块内个股  ->  当日 百日新高
//                if (null != stock_idx && stockFun.getExtDataArrDTO().百日新高[stock_idx]) {
//                    topBlock__百日新高_Map.merge(blockCode, 1, Integer::sum);
//                }
//            });
//        });
//
//
//        Assert.notEmpty(topBlock__涨停数_Map, "topBlock__涨停数_Map 为空：检查Cache是否 未加载全量stockDOList（block -> stockDO -> extData -> 涨停/跌停）");
//        Assert.notEmpty(topBlock__涨停数_Map, "topBlock__百日新高_Map 为空：检查Cache是否 未加载全量stockDOList（block -> stockDO -> extData -> 百日新高）");


        // -------------------------------------------------------------------------------------------------------------


        // TODO   排序（上榜天数TOP1 + 主线个股数量TOP1）


        // 排序（涨停榜TOP1 + 百日新高榜TOP1）  ->   真 主线板块（1~2个）


        int topN = top1TopBlockFlag ? 1 : 5;
        int zt_minN = top1TopBlockFlag ? 10 : 5;


        // 1、板块  涨停榜TOP1
        topBlock__涨停数_Map.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .limit(topN)
                            .forEach(entry -> {
                                String topBlockCode = entry.getKey();
                                if (entry.getValue() > zt_minN) {
                                    top1__lv3_topBlockCodeSet.add(topBlockCode);
                                }
                            });

//        // 2、板块  百日新高榜TOP1
//        topBlock__百日新高_Map.entrySet().stream()
//                              .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
//                              .limit(1)
//                              .forEach(entry -> {
//                                  String topBlockCode = entry.getKey();
//                                  if (entry.getValue() > 20) {
//                                      top1__lv3_topBlockCodeSet.add(topBlockCode);
//                                  }
//                              });


        List<String> topBlock__codeNameSet = top1__lv3_topBlockCodeSet.stream().map(code -> code + "-" + data.block__codeNameMap.get(code)).collect(Collectors.toList());
        log.info("topBlockCodeSet - 真-主线板块（涨停榜TOP1 + 百日新高榜TOP1）    >>>     [{}] , {} , {}", tradeDate, top1__lv3_topBlockCodeSet.size(), JSON.toJSONString(topBlock__codeNameSet));


        // -------------------------------------------------------------------------------------------------------------


        return top1__lv3_topBlockCodeSet;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 强势个股   ->   IN 主线板块                  // 通用方法
     *
     * @param topBlockStrategyEnum   主线策略
     * @param buy__topStock__codeSet 强势个股
     * @param data
     * @param tradeDate
     * @return
     */
    public Set<String> inBaseTopBlock__stockCodeSet(TopBlockStrategyEnum topBlockStrategyEnum,
                                                    Collection<String> buy__topStock__codeSet,

                                                    BacktestCache data,
                                                    LocalDate tradeDate) {


        // read Cache   ->   topN 主线板块（1~5个）
        Set<String> topN__topBlockCodeSet = topN__topBlockCodeSet__Cache(topBlockStrategyEnum, data, tradeDate);


        return inTopBlock__stockCodeSet(topN__topBlockCodeSet, buy__topStock__codeSet, data, tradeDate);
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


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）
     *
     * @param inTopBlock__stockCodeSet
     * @param data
     * @param tradeDate
     * @param buy_infoMap
     * @param posRate
     */
    public void buyStrategy_ETF(Set<String> inTopBlock__stockCodeSet,
                                BacktestCache data,
                                LocalDate tradeDate,
                                Map<String, String> buy_infoMap,
                                double posRate) {


        // 无股可买（inTopBlock__stockCodeList 为空     ||     总仓位 < 50%）


        // 有股可买   ->   取反（无股可买）
        // if (CollectionUtils.isNotEmpty(inTopBlock__stockCodeList) && posRate > 0.5) {
        if (!(CollectionUtils.isEmpty(inTopBlock__stockCodeSet) || posRate < 0.5)) {
            return;
        }


        // 大盘量化
        QaMarketMidCycleDO marketInfo = data.marketCache.get(tradeDate, k -> marketService.marketInfo(tradeDate));
        Assert.notNull(marketInfo, "[大盘量化]数据为空：" + tradeDate);


        // 大盘-牛熊：1-牛市；2-熊市；
        Integer marketBullBearStatus = marketInfo.getMarketBullBearStatus();
        // 大盘-中期顶底：1-底部；2- 底->顶；3-顶部；4- 顶->底；
        Integer marketMidStatus = marketInfo.getMarketMidStatus();


        // 大盘底
        if (marketMidStatus == 1) {


            // ETF
            data.ETF_stockDOList.forEach(stockDO -> {

//                // ETF   ->   最小交易日（上市日期）
//                List<KlineDTO> klineDTOList = stockDO.getKlineDTOList();
//                LocalDate date = CollectionUtils.isEmpty(klineDTOList) ? null : klineDTOList.get(0).getDate();
//
//
//                // 当前日期   ->   已上市
//                if (date != null && date.isBefore(tradeDate)) {
//                    String stockCode = stockDO.getCode();
//
//                    inTopBlock__stockCodeSet.add(stockCode);
//                    buy_infoMap.put(stockCode, "大盘极限底->ETF策略");
//                }


                StockFun fun = data.getOrCreateStockFun(stockDO);
                Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                Integer idx = dateIndexMap.get(tradeDate);
                // 当前日期   ->   未停牌
                if (idx != null) {
                    String stockCode = stockDO.getCode();

                    inTopBlock__stockCodeSet.add(stockCode);
                    buy_infoMap.put(stockCode, "大盘极限底->ETF策略");
                }
            });


            System.out.println(inTopBlock__stockCodeSet);
        }
    }


}