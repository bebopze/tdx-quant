package com.bebopze.tdx.quant.service.impl;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.*;
import com.bebopze.tdx.quant.common.domain.dto.analysis.*;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.*;
import com.bebopze.tdx.quant.common.tdxfun.PerformanceMetrics;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.JsonUtil;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.service.IBaseBlockRelaStockService;
import com.bebopze.tdx.quant.dal.service.IQaTopBlockService;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.DataAnalysisService;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.strategy.backtest.TradePairStat;
import com.bebopze.tdx.quant.strategy.buy.BacktestBuyStrategyD;
import com.bebopze.tdx.quant.strategy.sell.SellStrategyFactory;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.convert.ConvertDate.tradeDateIncr;
import static com.bebopze.tdx.quant.common.util.NumUtil.double2Decimal;
import static com.bebopze.tdx.quant.common.util.NumUtil.of;


/**
 * 数据分析
 *
 * @author: bebopze
 * @date: 2025/10/28
 */
@Slf4j
@Service
public class DataAnalysisServiceImpl implements DataAnalysisService {


    private static final BacktestCache data = InitDataServiceImpl.data;


    @Autowired
    private InitDataService initDataService;

    @Autowired
    private IQaTopBlockService qaTopBlockService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;

    @Autowired
    private BacktestBuyStrategyD backtestBuyStrategyD;
    @Autowired
    private SellStrategyFactory sellStrategyFactory;


    @TotalTime
    @Override
    public TopPoolAnalysisDTO topListAnalysis(LocalDate startDate,
                                              LocalDate endDate,
                                              Integer topPoolType,
                                              Integer topStrategyType,
                                              Integer bsStrategyType,
                                              Boolean ztFlag) {
        Assert.isTrue(!startDate.isAfter(endDate), "开始日期不能晚于结束日期");


        TopPoolAnalysisDTO dto = new TopPoolAnalysisDTO();


        // -------------------------------------------------------------------------------------------------------------


        // 持仓列表、收益率列表
        List<QaTopBlockDO> list = qaTopBlockService.listByDate(startDate, endDate);


        // -------------------------------------------------------------------------------------------------------------


        // --------------------------------------------------


        if (Objects.equals(BSStrategyTypeEnum.BS_MA_偏离率.type, bsStrategyType)) {
            bs__zt_reCalc(list, topPoolType, topStrategyType, bsStrategyType, ztFlag);
        } else if (Objects.equals(BSStrategyTypeEnum.BS_MA_偏离率2.type, bsStrategyType)) {
            bs__zt_reCalc_2(list, topPoolType, topStrategyType, bsStrategyType, ztFlag);
            // list = bs__zt_reCalc_3(topPoolType, topStrategyType, bsStrategyType, ztFlag, startDate, endDate);
        } else {
            zt_reCalc(list, topPoolType, topStrategyType, bsStrategyType, ztFlag);
        }


        // -------------------------------------------------------------------------------------------------------------


        // ---------------------------------------- count 指标

        Map<String, TopCountDTO> code_countMap = Maps.newHashMap();

        Map<String, Integer> topBlock__codeCountMap = Maps.newHashMap();
        Map<String, Integer> topStock__codeCountMap = Maps.newHashMap();


        // -------------------------------------------------------------------------------------------------------------


        // 每日收益率列表
        List<TopPoolDailyReturnDTO> dailyReturnDTOList = dailyReturnDTOList(list, topPoolType, topStrategyType, code_countMap, topBlock__codeCountMap, topStock__codeCountMap);


        // -------------------------------------------------------------------------------------------------------------


        // 汇总统计
        TopPoolSumReturnDTO sumReturnDTO = sumReturn_topPool(dailyReturnDTOList, null);
        TopPoolSumReturnDTO marginSumReturnDTO = sumReturn_topPool_margin(dailyReturnDTOList, null);


        // -------------------------------------------------------------------------------------------------------------


        dto.setSumReturnDTO(sumReturnDTO);
        dto.setMarginSumReturnDTO(marginSumReturnDTO);
        dto.setDailyReturnDTOList(dailyReturnDTOList);
        dto.setAvgDailyReturnDTO(avgDailyReturn(dailyReturnDTOList));
        dto.setCountDTOList(countDTOList(code_countMap, topBlock__codeCountMap, topStock__codeCountMap));


        // -------------------------------------------------------------------------------------------------------------


        return dto;
    }


    /**
     * 涨停（打板）  ->   重计算（涨停 -> 买不进去       =>       daily_return = close / prev_close 规则失效）
     *
     * @param list
     * @param topPoolType
     * @param type
     * @param bsStrategyType
     * @param ztFlag
     */
    private void zt_reCalc(List<QaTopBlockDO> list,
                           Integer topPoolType,
                           Integer type,
                           Integer bsStrategyType,
                           Boolean ztFlag) {
//        if (ztFlag == null && !(Objects.equals(TopTypeEnum.涨停.type, type) && topPoolType == 3)) {
//            return;
//        }


        // -------------------------------------------------------------------------------------------------------------


        // 逐日遍历 -> 计算
        for (QaTopBlockDO entity : list) {
            LocalDate date = entity.getDate();


            TopPoolAvgPctDTO avgPct;
            Set<String> todayCodeSet;
            Map<String, String> codeNameMap;
            List<TopChangePctDTO> topList;
            if (topPoolType == 1) {
                avgPct = entity.getTopBlockAvgPct(type);
                codeNameMap = entity.getTopBlockCodeNameMap(type);
                todayCodeSet = codeNameMap.keySet();
                topList = entity.getTopBlockList(type);
            } else if (topPoolType == 2) {
                avgPct = entity.getTopEtfAvgPct(type);
                codeNameMap = entity.getTopEtfCodeNameMap(type);
                todayCodeSet = codeNameMap.keySet();
                topList = entity.getTopEtfList(type);
            } else if (topPoolType == 3) {
                avgPct = entity.getTopStockAvgPct(type);
                codeNameMap = entity.getTopStockCodeNameMap(type);
                todayCodeSet = codeNameMap.keySet();
                topList = entity.getTopStockList(type);
            } else {
                throw new BizException("主线列表类型异常：" + topPoolType);
            }


            // ---------------------------------------------------------------------------------------------------------


            // 重计算   ->   topList
            topList = topList.stream()
                             // 是否涨停
                             .filter(e -> ztFlag == null || Objects.equals(ztFlag, e.isZtFlag()))
                             .map(e -> {


                                 // ---------------------------------- bsStrategyType ----------------------------------


                                 // BS策略
                                 // boolean isMatch = bsStrategyType(e, bsStrategyType);
                                 // boolean isMatch = bsStrategyType_2(e, bsStrategyType);
                                 boolean isMatch = bsStrategyType_3(e, bsStrategyType);
                                 // boolean isMatch = bsStrategyType_4(e, bsStrategyType);
                                 if (!isMatch) {
                                     return null;
                                 }


                                 // ------------------------------------------------------------------------------------


//                                 // 涨停（打板）  ->   重计算（涨停 -> 买不进去       =>       daily_return = close / prev_close 规则失效）
//                                 if (Objects.equals(TopTypeEnum.涨停.type, type) && topPoolType == 3) {
//                                     // 非涨停 -> 忽略
//                                     if (!e.isZtFlag()) {
//                                         return null;
//                                     }
//
//
//                                     double today_closePct = e.getToday_changePct();   // 当日涨停
//                                     double ztPct = today_closePct;
//
//
//                                     // 次日
//                                     double today2Next_openPct = e.getToday2Next_openPct();
//                                     double today2Next_closePct = e.getToday2Next_changePct();
//                                     // 次2日
//                                     double today2N2_closePct = e.getToday2N2_closePct();
//                                     double today2N2_openPct = e.getToday2N2_openPct();
//                                     double today2N2_highPct = e.getToday2N2_highPct();
//
//
//                                     // 假设买不进去（次1日 开盘价买入   ->   次2日 开盘价卖出）
//                                     // double today2Next_changePct = (100 + today2N2_closePct) / (100 + today2Next_openPct) * 100 - 100;     // 1500%
//
//                                     // double today2Next_changePct = (100 + today2N2_openPct) / (100 + today2Next_openPct) * 100 - 100;     // -99%
//
//
//                                     // 次2日 盘中价格（H） >  涨停幅度x0.7     ->     直接S
//                                     // 次2日 盘中价格（H） <  涨停幅度x0.7     ->     收盘价 S
//                                     double zt_70_percent = (100 + today2Next_closePct) * (1 + 0.01 * ztPct * 0.7) - 100;
//                                     double s_pct = today2N2_highPct > zt_70_percent ? zt_70_percent : today2N2_closePct;
//
//
//                                     double today2Next_changePct = (100 + s_pct) / (100 + today2Next_openPct) * 100 - 100;     // 30000%
//
//
//                                     // 收益率（%）
//                                     e.setToday2Next_changePct(today2Next_changePct);
//                                 }


                                 // 涨停（打板）  ->   重计算（涨停 -> 买不进去       =>       daily_return = close / prev_close 规则失效）
                                 if (Objects.equals(TopTypeEnum.涨停.type, type) && topPoolType == 3) {
                                     // 非涨停 -> 忽略
                                     if (!e.isZtFlag()) {
                                         return null;
                                     }


                                     double today_closePct = e.getToday_changePct();   // 当日涨停
                                     double ztPct = today_closePct;


                                     // 次日
                                     double today2N1_open = e.getToday2Next_open();
                                     double today2N1_close = e.getToday2Next_close();
                                     // 次2日
                                     double today2N2_open = e.getToday2N2_open();
                                     double today2N2_high = e.getToday2N2_high();
                                     double today2N2_close = e.getToday2N2_close();


                                     // 假设买不进去（次1日 开盘价买入   ->   次2日 收盘价卖出）
                                     double today2Next_changePct = today2N2_close / today2N1_open * 100 - 100;     // 1500%

                                     // 假设买不进去（次1日 开盘价买入   ->   次2日 开盘价卖出）
                                     // double today2Next_changePct = today2N2_open / today2N1_open * 100 - 100;      // -99%


//                                     // 次2日 盘中价格（H） >  涨停幅度x0.7     ->     直接S
//                                     // 次2日 盘中价格（H） <  涨停幅度x0.7     ->     收盘价 S
//                                     double zt_70_percent = today2N1_close * (1 + 0.01 * ztPct * 0.7);
//                                     double N2_s_price = today2N2_high > zt_70_percent ? zt_70_percent : today2N2_close;
//
//                                     // 假设买不进去（次1日 开盘价买入   ->   次2日  涨停价*0.7/收盘价  卖出）
//                                     double today2Next_changePct = N2_s_price / today2N1_open * 100 - 100;     // 30000%


                                     // 收益率（%）
                                     e.setToday2Next_changePct(today2Next_changePct);
                                 }


//                                 // 涨停（打板）  ->   重计算（涨停 -> 买不进去       =>       daily_return = close / prev_close 规则失效）
//                                 if (Objects.equals(TopTypeEnum.涨停_次2日.type, type) && topPoolType == 3) {
//                                     // 非涨停 -> 忽略
//                                     if (!e.isZtFlag()) {
//                                         return null;
//                                     }
//
//
//                                     // 次日
//                                     double today2Next_openPct = e.getToday2Next_openPct();
//                                     double today2Next_closePct = e.getToday2Next_changePct();
//                                     // 次2日
//                                     double today2N2_closePct = e.getToday2N2_closePct();
//                                     double today2N2_openPct = e.getToday2N2_openPct();
//
//
//                                     // 假设买不进去（次1日 开盘价买入   ->   次2日 收盘价卖出）
//                                     double today2Next_changePct = (100 + today2N2_closePct) / (100 + today2Next_openPct) * 100 - 100;   // 简化（实际应该   次2日 收盘价 / 次1日 开盘价）
//
//
//                                     // 收益率（%）
//                                     e.setToday2Next_changePct(today2Next_changePct);
//                                 }


                                 // ------------------------------------------------------------------------------------

                                 return e;
                             })
                             .filter(Objects::nonNull)
                             .collect(Collectors.toList());


            // ---------------------------------------------------------------------------------------------------------


            // 不足10只股票  ->  空列表（空仓）
            if (topList.size() < 20 && !(Objects.equals(TopTypeEnum.涨停.type, type) && topPoolType == 3) && (ztFlag == null || !ztFlag)) {
                topList = Collections.emptyList();
            }


            // ---------------------------------------------------------------------------------------------------------


//            // 每只个股 最大仓位 ->   1%
//            double pos_pct = topList.size() < 100 ? 1 : 100 / topList.size();
//
//
//            if (topList.size() > 100) {
//
//                topList.forEach(e -> {
//                    double pct = e.getToday2Next_changePct();
//                    e.setToday2Next_changePct(pct);
//                });
//            }


            // ---------------------------------------------------------------------------------------------------------


            // 重计算   ->   avgPct
            avgPct = new TopBlockServiceImpl().avgPct(topList, TopTypeEnum.getByType(type));


            entity.setTopStockCodeSet(JsonUtil.toJSONString(topList));
            entity.setStockAvgPct(JsonUtil.toJSONString(Lists.newArrayList(avgPct)));
        }
    }


    /**
     * 涨停（打板）  ->   重计算（涨停 -> 买不进去       =>       daily_return = close / prev_close 规则失效）
     *
     * @param list
     * @param topPoolType
     * @param type
     * @param bsStrategyType
     * @param ztFlag
     */
    private void bs__zt_reCalc(List<QaTopBlockDO> list,
                               Integer topPoolType,
                               Integer type,
                               Integer bsStrategyType,
                               Boolean ztFlag) {


        initDataService.initData();


        // -------------------------------------------------------------------------------------------------------------


        LocalDate prevDate = null;
        List<TopChangePctDTO> prev_topList = Lists.newArrayList();


        // -------------------------------------------------------------------------------------------------------------


        // 逐日遍历 -> 计算
        for (QaTopBlockDO entity : list) {
            LocalDate date = entity.getDate();


            TopPoolAvgPctDTO avgPct;
            Set<String> todayCodeSet;
            Map<String, String> codeNameMap;
            List<TopChangePctDTO> topList;
            if (topPoolType == 1) {
                avgPct = entity.getTopBlockAvgPct(type);
                codeNameMap = entity.getTopBlockCodeNameMap(type);
                todayCodeSet = codeNameMap.keySet();
                topList = entity.getTopBlockList(type);
            } else if (topPoolType == 2) {
                avgPct = entity.getTopEtfAvgPct(type);
                codeNameMap = entity.getTopEtfCodeNameMap(type);
                todayCodeSet = codeNameMap.keySet();
                topList = entity.getTopEtfList(type);
            } else if (topPoolType == 3) {
                avgPct = entity.getTopStockAvgPct(type);
                codeNameMap = entity.getTopStockCodeNameMap(type);
                todayCodeSet = codeNameMap.keySet();
                topList = entity.getTopStockList(type);
            } else {
                throw new BizException("主线列表类型异常：" + topPoolType);
            }


            // ---------------------------------------------------------------------------------------------------------


            Set<String> today_buyCodeSet = Sets.newHashSet();
            Set<String> today_sellCodeSet = Sets.newHashSet();

            List<TopChangePctDTO> today_posTopList = Lists.newArrayList();


            // ---------------------------------------------------------------------------------------------------------


            // 重计算   ->   topList
            topList.stream()
                   // 是否涨停
                   .filter(e -> ztFlag == null || Objects.equals(ztFlag, e.isZtFlag()))
                   .forEach(e -> {


                       String stockCode = e.getCode();


                       // ---------------------------------- bsStrategyType --------------------------------------------


                       // BS策略
                       int bsStatus = bsStrategyType__bsStatus(e.getCode(), bsStrategyType, date);


                       // B
                       if (bsStatus == 1) {
                           today_buyCodeSet.add(stockCode);
                           today_posTopList.add(e);
                       }
                       // S
                       else if (bsStatus == 2) {
                           today_sellCodeSet.add(stockCode);
                       }
                   });


            // ---------------------------------------------------------------------------------------------------------


            if (prevDate != null) {
                // 昨日持仓列表
                prev_topList.stream()
                            // 昨日持仓中 -> 今日未S     =>     今日继续持仓中
                            .filter(e -> !today_sellCodeSet.contains(e.getCode()) && !today_buyCodeSet.contains(e.getCode()))
                            .forEach(e -> {
                                TopChangePctDTO pos_dto = convertPrevPos2TodayPos(e.getCode(), date);
                                if (null != pos_dto) {
                                    today_posTopList.add(pos_dto);
                                }
                            });
            }


            topList = today_posTopList;


            // ---------------------------------------------------------------------------------------------------------


//            // 不足10只股票  ->  空列表（空仓）
//            if (topList.size() < 20 && !(Objects.equals(TopTypeEnum.涨停.type, type) && topPoolType == 3) && (ztFlag == null || !ztFlag)) {
//                topList = Collections.emptyList();
//            }


            // ---------------------------------------------------------------------------------------------------------


            // ---------------------------------------------------------------------------------------------------------


            // 重计算   ->   avgPct
            avgPct = new TopBlockServiceImpl().avgPct(topList, TopTypeEnum.getByType(type));


            entity.setTopStockCodeSet(JsonUtil.toJSONString(topList));
            entity.setStockAvgPct(JsonUtil.toJSONString(Lists.newArrayList(avgPct)));


            // ---------------------------------------------------------------------------------------------------------
            prevDate = date;
            prev_topList = today_posTopList;
            // ---------------------------------------------------------------------------------------------------------
        }
    }


    /**
     * 涨停（打板）  ->   重计算（涨停 -> 买不进去       =>       daily_return = close / prev_close 规则失效）
     *
     * @param list
     * @param topPoolType
     * @param type
     * @param bsStrategyType
     * @param ztFlag
     */
    @Deprecated
    private void bs__zt_reCalc_2(List<QaTopBlockDO> list,
                                 Integer topPoolType,
                                 Integer type,
                                 Integer bsStrategyType,
                                 Boolean ztFlag) {


        initDataService.initData();


        // -------------------------------------------------------------------------------------------------------------


        LocalDate prevDate = null;
        List<TopChangePctDTO> prev_topList = Lists.newArrayList();


        // -------------------------------------------------------------------------------------------------------------


        // 逐日遍历 -> 计算
        for (int i = 0; i < list.size(); i++) {
            QaTopBlockDO entity = list.get(i);
            LocalDate date = entity.getDate();


            // 前后10日内 主线列表（QaTopBlockDO -> 板块/ETF/股票）
            // 使用 subList 获取指定范围的视图，然后复制到新列表（subList 返回的是 原列表的视图，修改它会影响原列表，如果不需要修改，可以考虑直接使用 subList 的视图）
            List<QaTopBlockDO> today__bf10_af10__topEntityList = list.subList(Math.max(0, i - 10), Math.min(list.size(), i + 11));


            // 前后10日内 主线个股列表（TopChangePctDTO -> 股票）
            List<TopChangePctDTO> bf10_topList = Lists.newArrayList();
            for (QaTopBlockDO bf10_entity : today__bf10_af10__topEntityList) {

                if (topPoolType == 1) {
                    bf10_topList.addAll(bf10_entity.getTopBlockList(type));
                } else if (topPoolType == 2) {
                    bf10_topList.addAll(bf10_entity.getTopEtfList(type));
                } else if (topPoolType == 3) {
                    bf10_topList.addAll(bf10_entity.getTopStockList(type));
                } else {
                    throw new BizException("主线列表类型异常：" + topPoolType);
                }
            }
            Set<String> topCodeSet = bf10_topList.stream().map(TopChangePctDTO::getCode).collect(Collectors.toSet());


            // ---------------------------------------------------------------------------------------------------------


            Set<String> today_buyCodeSet = Sets.newHashSet();
            Set<String> today_sellCodeSet = Sets.newHashSet();

            List<TopChangePctDTO> today_posTopList = Lists.newArrayList();


            // ---------------------------------------------------------------------------------------------------------


            // 重计算   ->   topList
            topCodeSet.stream()
                      // 是否涨停
//                   .filter(e -> ztFlag == null || Objects.equals(ztFlag, e.isZtFlag()))
                      .forEach(stockCode -> {


                          // ---------------------------------- bsStrategyType --------------------------------------------


                          // BS策略
                          int bsStatus = bsStrategyType__bsStatus(stockCode, bsStrategyType, date);


                          // B
                          if (bsStatus == 1) {
                              today_buyCodeSet.add(stockCode);

                              TopChangePctDTO pos_dto = convertPrevPos2TodayPos(stockCode, date);
                              if (null != pos_dto) {
                                  today_posTopList.add(pos_dto);
                              }
                          }
                          // S
                          else if (bsStatus == 2) {
                              today_sellCodeSet.add(stockCode);
                          }
                      });


            // ---------------------------------------------------------------------------------------------------------


            if (prevDate != null) {
                // 昨日持仓列表
                prev_topList.stream()
                            // 昨日持仓中 -> 今日未S     =>     今日继续持仓中
                            .filter(e -> !today_sellCodeSet.contains(e.getCode()) && !today_buyCodeSet.contains(e.getCode()))
                            .forEach(e -> {
                                TopChangePctDTO pos_dto = convertPrevPos2TodayPos(e.getCode(), date);
                                if (null != pos_dto) {
                                    today_posTopList.add(pos_dto);
                                }
                            });
            }


            // ---------------------------------------------------------------------------------------------------------


//            // 不足10只股票  ->  空列表（空仓）
//            if (topList.size() < 20 && !(Objects.equals(TopTypeEnum.涨停.type, type) && topPoolType == 3) && (ztFlag == null || !ztFlag)) {
//                topList = Collections.emptyList();
//            }


            // ---------------------------------------------------------------------------------------------------------


            // ---------------------------------------------------------------------------------------------------------


            // 重计算   ->   avgPct
            TopPoolAvgPctDTO avgPct = new TopBlockServiceImpl().avgPct(today_posTopList, TopTypeEnum.getByType(type));


            entity.setTopStockCodeSet(JsonUtil.toJSONString(today_posTopList));
            entity.setStockAvgPct(JsonUtil.toJSONString(Lists.newArrayList(avgPct)));


            // ---------------------------------------------------------------------------------------------------------
            prevDate = date;
            prev_topList = today_posTopList;
            // ---------------------------------------------------------------------------------------------------------
        }
    }


    /**
     * 涨停（打板）  ->   重计算（涨停 -> 买不进去       =>       daily_return = close / prev_close 规则失效）
     *
     * @param topPoolType
     * @param type
     * @param bsStrategyType
     * @param ztFlag
     */
    @Deprecated   // 和 跑回测 时间一样长   ->  30min 起步
    private List<QaTopBlockDO> bs__zt_reCalc_3(Integer topPoolType,
                                               Integer type,
                                               Integer bsStrategyType,
                                               Boolean ztFlag,
                                               LocalDate startDate,
                                               LocalDate endDate) {


        initDataService.initData();


        // -------------------------------------------------------------------------------------------------------------


        List<QaTopBlockDO> list = Lists.newArrayList();


        // -------------------------------------------------------------------------------------------------------------


        LocalDate prevDate = null;
        List<String> prev_posList = Lists.newArrayList();


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------
        //                                            回测-task   按日 循环执行
        // -------------------------------------------------------------------------------------------------------------


        TopBlockStrategyEnum topBlockStrategyEnum = TopBlockStrategyEnum.LV3;

        List<String> buyConList = Lists.newArrayList("月多", "SSF多");


        // -------------------------------------------------------------------------------------------------------------


        LocalDate tradeDate = startDate;
        endDate = DateTimeUtil.min(endDate, data.endDate());


        while (tradeDate.isBefore(endDate)) {
            tradeDate = tradeDateIncr(tradeDate);


            Map<String, String> buy_infoMap = Maps.newConcurrentMap();
            // 当前仓位
            double posRate = 0;


            // ---------------------------------------------------------------------------------------------------------


            List<TopChangePctDTO> today_posTopList = Lists.newArrayList();


            // ---------------------------------------------------------------------------------------------------------


            // ---------------------------------------------------------------------------------------------------------
            //                                            S策略
            // ---------------------------------------------------------------------------------------------------------


            Map<String, SellStrategyEnum> sell_infoMap = Maps.newHashMap();


            // 卖出策略
            Set<String> sell__stockCodeSet = sellStrategyFactory.get("A").rule(topBlockStrategyEnum, data, tradeDate, prev_posList, sell_infoMap);

            log.info("S策略     >>>     [{}] , topBlockStrategyEnum : {} , size : {} , sell__stockCodeSet : {} , sell_infoMap : {}",
                     tradeDate, topBlockStrategyEnum, sell__stockCodeSet.size(), JSON.toJSONString(sell__stockCodeSet), JSON.toJSONString(sell_infoMap));


            // ---------------------------------------------------------------------------------------------------------
            //                                            B策略
            // ---------------------------------------------------------------------------------------------------------


            // BS策略
            List<String> today__buy__stockCodeList = backtestBuyStrategyD.rule2(topBlockStrategyEnum, buyConList, data, tradeDate, buy_infoMap, posRate, ztFlag);

            log.info("B策略     >>>     [{}] , topBlockStrategyEnum : {} , size : {} , buy__stockCodeList : {} , buy_infoMap : {}",
                     tradeDate, topBlockStrategyEnum, today__buy__stockCodeList.size(), JSON.toJSONString(today__buy__stockCodeList), JSON.toJSONString(buy_infoMap));


            // ---------------------------------------------------------------------------------------------------------


            if (prevDate != null) {
                LocalDate finalTradeDate = tradeDate;

                // 昨日持仓列表
                prev_posList.stream()
                            // 昨日持仓中 -> 今日未S     =>     今日继续持仓中
                            .filter(stockCode -> !sell__stockCodeSet.contains(stockCode) && !today__buy__stockCodeList.contains(stockCode))
                            .forEach(stockCode -> {
                                TopChangePctDTO pos_dto = convertPrevPos2TodayPos(stockCode, finalTradeDate);
                                if (null != pos_dto) {
                                    today_posTopList.add(pos_dto);
                                }
                            });
            }


            // ---------------------------------------------------------------------------------------------------------


            // 重计算   ->   avgPct
            TopPoolAvgPctDTO avgPct = new TopBlockServiceImpl().avgPct(today_posTopList, TopTypeEnum.getByType(type));


            QaTopBlockDO entity = new QaTopBlockDO();
            entity.setDate(tradeDate);
            entity.setTopStockCodeSet(JsonUtil.toJSONString(today_posTopList));
            entity.setStockAvgPct(JsonUtil.toJSONString(Lists.newArrayList(avgPct)));
            list.add(entity);


            // ---------------------------------------------------------------------------------------------------------
            prevDate = tradeDate;
            prev_posList = today_posTopList.stream().map(TopChangePctDTO::getCode).collect(Collectors.toList());
            // ---------------------------------------------------------------------------------------------------------
        }


        return list;
    }


    private TopChangePctDTO convertPrevPos2TodayPos(String stockCode, LocalDate today) {
        TopChangePctDTO dto = new TopChangePctDTO(stockCode, null);


        StockFun fun = data.getFun(stockCode);
        Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();

        Integer idx = dateIndexMap.get(today);
        // 停牌  /  当前date -> 非交易日
        if (idx == null) {
            return null;
        }


        List<KlineDTO> klineDTOList = fun.getKlineDTOList();
        int maxIdx = klineDTOList.size() - 1;


        KlineDTO today_klineDTO = klineDTOList.get(idx);
        KlineDTO next_klineDTO = klineDTOList.get(Math.min(idx + 1, maxIdx));
        ExtDataDTO today_extDataDTO = fun.getExtDataDTOList().get(idx);


        dto.setName(fun.getName());
        dto.setToday2Next_changePct(next_klineDTO.getChange_pct());
        dto.setBuySignalExtDataDTO(today_extDataDTO);


        dto.setAmo(today_klineDTO.getAmo());


        return dto;
    }


    /**
     * BS策略
     *
     * @param e
     * @param bsStrategyType
     * @return
     */
    private boolean bsStrategyType(TopChangePctDTO e, Integer bsStrategyType) {
        // 全部
        if (bsStrategyType == null) {
            return true;
        }


        ExtDataDTO buySignal = e.getBuySignalExtDataDTO();

        Integer changePctLimit = StockLimitEnum.getChangePctLimit(e.getCode(), e.getName());
        boolean _5CM = changePctLimit == 5;
        boolean _10CM = changePctLimit == 10;
        boolean _20CM = changePctLimit == 20;
        boolean _30CM = changePctLimit == 30;


        if (bsStrategyType == 1) {

            double C_SSF_偏离率 = buySignal.getC_SSF_偏离率();
            double H_SSF_偏离率 = buySignal.getH_SSF_偏离率();
            double C_MA5_偏离率 = buySignal.getC_MA5_偏离率();
            double H_MA5_偏离率 = buySignal.getH_MA5_偏离率();
            boolean 高位爆量上影大阴 = buySignal.get高位爆量上影大阴();


            // B   ->   C_SSF_偏离率   ∈   [-0.3%, 3%]
            if (NumUtil.between(C_SSF_偏离率, -0.3, 3)) {
                return true;
            }

            // S   ->   C_SSF_偏离率   >=   20
            if (C_SSF_偏离率 >= 20) {
                return false;
            }


            // 1-B；
            boolean B_1_1 = _10CM && NumUtil.between(C_SSF_偏离率, 0, 7);    // 天赐材料（2025-09 ~ 2025-11）
            boolean B_1_2 = _20CM && NumUtil.between(C_SSF_偏离率, 0, 10);   // 天华新能（2025-09 ~ 2025-11）
            boolean B_1_3 = _30CM && NumUtil.between(C_SSF_偏离率, 0, 13);


            // 2-持有；


            // 3-S；
            boolean S_1 = 高位爆量上影大阴;


            boolean S_2_1 = _10CM && C_SSF_偏离率 > 20 && C_MA5_偏离率 > 16;   // 天赐材料（2025-09 ~ 2025-11）
            boolean S_2_2 = _20CM && C_SSF_偏离率 > 20 && C_MA5_偏离率 > 21;   // 天华新能（2025-09 ~ 2025-11）

            boolean S_3_1 = _10CM && H_SSF_偏离率 > 23 && H_MA5_偏离率 > 23;   // 天赐材料（2025-09 ~ 2025-11）
            boolean S_3_2 = _20CM && H_SSF_偏离率 > 23 && H_MA5_偏离率 > 23;   // 天华新能（2025-09 ~ 2025-11）


        } else if (bsStrategyType == 2) {
            double C_MA10_偏离率 = buySignal.getC_MA10_偏离率();

            // B   ->   C_MA10_偏离率   ∈   [-0.3%, 3%]
            if (NumUtil.between(C_MA10_偏离率, -0.3, 3)) {
                return true;
            }

            // S   ->   C_MA10_偏离率   >=   20
            if (C_MA10_偏离率 >= 20) {
                return false;
            }
        }


        return false;
    }


    /**
     * BS策略
     *
     * @param e
     * @param bsStrategyType
     * @return
     */
    private boolean bsStrategyType_2(TopChangePctDTO e, Integer bsStrategyType) {
        // 全部
        if (bsStrategyType == null) {
            return true;
        }


        ExtDataDTO buySignal = e.getBuySignalExtDataDTO();

        Integer changePctLimit = StockLimitEnum.getChangePctLimit(e.getCode(), e.getName());
        boolean _5CM = changePctLimit == 5;
        boolean _10CM = changePctLimit == 10;
        boolean _20CM = changePctLimit == 20;
        boolean _30CM = changePctLimit == 30;


        LocalDate date = buySignal.getDate();
        double today_close = e.getToday_close();


        if (Objects.equals(BSStrategyTypeEnum.BS_MA20多.type, bsStrategyType)) {

            // B/持仓
            return buySignal.getMA20多();

        } else if (Objects.equals(BSStrategyTypeEnum.BS_上MA20.type, bsStrategyType)) {

            // B/持仓
            return buySignal.get上MA20();

        } else if (Objects.equals(BSStrategyTypeEnum.BS_SSF多.type, bsStrategyType)) {

            // B/持仓
            return buySignal.getSSF多();

        } else if (Objects.equals(BSStrategyTypeEnum.BS_上SSF.type, bsStrategyType)) {

            // B/持仓
            return buySignal.get上SSF();


        } else if (Objects.equals(BSStrategyTypeEnum.BS_上SAR.type, bsStrategyType)) {

            double SAR = buySignal.getSAR();

            // B/持有（上SAR     =>     今日收盘价 > SAR）
            return today_close > SAR;


        } else if (Objects.equals(BSStrategyTypeEnum.BS_XZZB.type, bsStrategyType)) {

            // B/持仓
            return buySignal.getXZZB();

        } else if (Objects.equals(BSStrategyTypeEnum.BS_BS区间.type, bsStrategyType)) {

            // B/持仓
            return buySignal.getBSQJ();

        } else if (Objects.equals(BSStrategyTypeEnum.BS_MA_偏离率.type, bsStrategyType)) {


        }


        return false;
    }

    /**
     * BS策略（持仓策略）
     *
     * @param e
     * @param bsStrategyType
     * @return
     */
    private boolean bsStrategyType_3(TopChangePctDTO e, Integer bsStrategyType) {
        // 全部
        if (bsStrategyType == null) {
            return true;
        }


        ExtDataDTO buySignal = e.getBuySignalExtDataDTO();

        Integer changePctLimit = StockLimitEnum.getChangePctLimit(e.getCode(), e.getName());
        boolean _5CM = changePctLimit == 5;
        boolean _10CM = changePctLimit == 10;
        boolean _20CM = changePctLimit == 20;
        boolean _30CM = changePctLimit == 30;


        LocalDate date = buySignal.getDate();
        double today_close = e.getToday_close();


        if (Objects.equals(BSStrategyTypeEnum.BS_MA20多.type, bsStrategyType)) {

            // B/持仓
            return buySignal.getMA20多();

        } else if (Objects.equals(BSStrategyTypeEnum.BS_上MA20.type, bsStrategyType)) {

            // B/持仓
            return buySignal.get上MA20();

        } else if (Objects.equals(BSStrategyTypeEnum.BS_SSF多.type, bsStrategyType)) {

            // B/持仓
            return buySignal.getSSF多();

        } else if (Objects.equals(BSStrategyTypeEnum.BS_上SSF.type, bsStrategyType)) {

            // B/持仓
            return buySignal.get上SSF();


        } else if (Objects.equals(BSStrategyTypeEnum.BS_上SAR.type, bsStrategyType)) {

            double SAR = buySignal.getSAR();

            // B/持有（上SAR     =>     今日收盘价 > SAR）
            return today_close > SAR;


        } else if (Objects.equals(BSStrategyTypeEnum.BS_XZZB.type, bsStrategyType)) {

            // B/持仓
            return buySignal.getXZZB();

        } else if (Objects.equals(BSStrategyTypeEnum.BS_BS区间.type, bsStrategyType)) {

            // B/持仓
            return buySignal.getBSQJ();

        } else if (Objects.equals(BSStrategyTypeEnum.BS_MA_偏离率.type, bsStrategyType)) {

        }


        return false;
    }


    /**
     * BS策略（持仓策略）
     *
     * @param code
     * @param bsStrategyType
     * @param date
     * @return
     */
    private int bsStrategyType__bsStatus(String code, Integer bsStrategyType, LocalDate date) {
        int bsStatus = 0;


        StockFun fun = data.getFun(code);

//        ExtDataDTO buySignal = e.getBuySignalExtDataDTO();

        Integer changePctLimit = fun.changePctLimit();
//        Integer changePctLimit = StockLimitEnum.getChangePctLimit(code, name);
        boolean _5CM = changePctLimit == 5;
        boolean _10CM = changePctLimit == 10;
        boolean _20CM = changePctLimit == 20;
        boolean _30CM = changePctLimit == 30;


//        LocalDate date = buySignal.getDate();


        Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();

        Integer idx = dateIndexMap.get(date);
        if (idx == null) {
            return bsStatus;
        }


        ExtDataDTO buySignal = fun.getExtDataDTOList().get(idx);


        if (Objects.equals(BSStrategyTypeEnum.BS_MA_偏离率.type, bsStrategyType)) {


            Integer 短期趋势支撑线 = buySignal.get短期趋势支撑线();
            Integer 中期趋势支撑线 = buySignal.get中期趋势支撑线();

            double min = 短期趋势支撑线 == 5 ? 0.25
                    : 短期趋势支撑线 == 10 ? -0.5
                    : 短期趋势支撑线 == 20 ? -0.5 : -0.5;

            double max = 短期趋势支撑线 == 5 ? 0.5
                    : 短期趋势支撑线 == 10 ? 1.0
                    : 短期趋势支撑线 == 20 ? 3.5 : 3.0;


            double C_短期MA_偏离率 = buySignal.getC_短期MA_偏离率();
            double C_中期MA_偏离率 = buySignal.getC_中期MA_偏离率();


            // B
            if (NumUtil.between(C_短期MA_偏离率, min, max)) {
                bsStatus = 1;

//                e.setPosStatus(1);
//
//                // 已持仓
//                if (e.isPrev_posFlag()) {
//
//                } else {
//                    // 首次买入
//                    e.setFirstBuyDate(date);
//                }
//                e.setPosFlag(true);
            }


            // S
            if (C_中期MA_偏离率 > 50) {
                bsStatus = 2;
            }
        }


        return bsStatus;
    }


    /**
     * 每日收益率列表
     *
     * @param list                   时间段 -> 主线列表
     * @param topPoolType            主线类型：1-板块；2-ETF；3-个股；
     * @param type                   主线策略：1-机选；2-精选（TOP50）；3-历史新高；4-极多头；5-RPS三线红；6-10亿；7-首次三线红；8-口袋支点；9-T0；10-涨停（打板）；     // TopTypeEnum
     * @param code_countMap
     * @param topBlock__codeCountMap
     * @param topStock__codeCountMap
     * @return
     */
    private List<TopPoolDailyReturnDTO> dailyReturnDTOList(List<QaTopBlockDO> list,
                                                           Integer topPoolType,
                                                           Integer type,
                                                           Map<String, TopCountDTO> code_countMap,
                                                           Map<String, Integer> topBlock__codeCountMap,
                                                           Map<String, Integer> topStock__codeCountMap) {


        List<TopPoolDailyReturnDTO> dailyReturnDTOList = Lists.newArrayList();
        Set<String> prevCodeSet = Sets.newHashSet();


        // ---------------------------------------- 汇总指标

        double nav = 1.0;            // 初始净值（普通账户）
        double capital = 100_0000;   // 初始资金（普通账户）

        double marginNav = 1.0;            // 初始净值（融资账户）
        double marginCapital = 100_0000;   // 初始资金（融资账户）


        LocalDate actualStartDate = null; // 实际开始日期


        // -------------------------------------------------------------------------------------------------------------


        // 逐日遍历 -> 计算
        for (QaTopBlockDO entity : list) {
            LocalDate date = entity.getDate();


            TopPoolAvgPctDTO avgPct;
            Set<String> todayCodeSet;
            Map<String, String> codeNameMap;
            List<TopChangePctDTO> topList;
            if (topPoolType == 1) {
                avgPct = entity.getTopBlockAvgPct(type);
                codeNameMap = entity.getTopBlockCodeNameMap(type);
                todayCodeSet = codeNameMap.keySet();
                topList = entity.getTopBlockList(type);
            } else if (topPoolType == 2) {
                avgPct = entity.getTopEtfAvgPct(type);
                codeNameMap = entity.getTopEtfCodeNameMap(type);
                todayCodeSet = codeNameMap.keySet();
                topList = entity.getTopEtfList(type);
            } else if (topPoolType == 3) {
                avgPct = entity.getTopStockAvgPct(type);
                codeNameMap = entity.getTopStockCodeNameMap(type);
                todayCodeSet = codeNameMap.keySet();
                topList = entity.getTopStockList(type);
            } else {
                throw new BizException("主线列表类型异常：" + topPoolType);
            }


            if (null == avgPct) {
                log.error("date : {} , avgPct is null", date);
                continue;
            }


            double daily_return = avgPct.getToday2Next_changePct();


            if (actualStartDate == null && daily_return != 0) {
                actualStartDate = date;
            }


            // --------------------------------------------------


            // 主线板块
            entity.getTopBlockCodeNameMap(type).forEach((code, name) -> topBlock__codeCountMap.merge(code, 1, Integer::sum));
            // 主线个股
            entity.getTopStockCodeNameMap(type).forEach((code, name) -> topStock__codeCountMap.merge(code, 1, Integer::sum));


            Map<String, TopChangePctDTO> topMap = topList.stream().collect(Collectors.toMap(TopChangePctDTO::getCode, Function.identity()));


            todayCodeSet.forEach(code -> {

                String name = codeNameMap.get(code);
                double today2Next_changePct = topMap.get(code).getToday2Next_changePct() * 0.01;


                code_countMap.merge(code, new TopCountDTO(code, name, 1, today2Next_changePct, date), (old, newVal) -> {
                    old.setCount(old.getCount() + newVal.getCount());
                    old.setPct((1 + old.getPct()) * (1 + newVal.getPct()) - 1);
                    old.getPctList().add(of(today2Next_changePct * 100));
                    old.getDateList().add(date);

                    return old;
                });
            });


            // --------------------------------------------------


            if (actualStartDate == null) {
                continue;
            }


            // --------------------------------------- 涨停个股（特殊处理） -----------------------------------------------


            // --------------------------------------- 每日 收益/净值（普通账户） ------------------------------------------


            // 交易手续费（0.15%     万15 ~ 7万【万一免五】）
            double treadFeeFactor = 0.15 * 0.01;


            double rate = 1 + (daily_return - treadFeeFactor) * 0.01;

            nav *= rate;
            capital *= rate;


            // --------------------------------------- 每日 收益/净值（融资账户） ------------------------------------------


            // 融资保证金比例系数（折算率：0.85 ~ 1.15）
            double marginFactor = 0.95;   // 0.85   ->   9成仓

            // 融资费率（年化6%  ->  6%/365 = 0.017%）
            double rzFeeFactor = 6.0 / 365 * 0.01;


            double marginDailyReturn = (daily_return - treadFeeFactor - rzFeeFactor) * (1 + marginFactor);   // 融资账户收益率 = 普通账户收益率 * 2（普通账户 涨/跌 10%  ->  融资账户 涨/跌 20%）
            double marginRate = 1 + marginDailyReturn * 0.01;

            marginNav *= marginRate;
            marginCapital *= marginRate;


            // ---------------------------------------------------------------------------------------------------------


            TopPoolDailyReturnDTO dr = new TopPoolDailyReturnDTO();
            dr.setDate(date);

            dr.setDailyReturn(of(daily_return));
            dr.setNav(of(nav));
            dr.setCapital(of(capital));

            dr.setMarginDailyReturn(of(marginDailyReturn));
            dr.setMarginNav(of(marginNav));
            dr.setMarginCapital(of(marginCapital));


            // ------------------------------------------ 每日 调仓换股 比例 ----------------------------------------------


            // 当日调仓换股比例
            posReplaceRatio(dr, prevCodeSet, todayCodeSet);


            dailyReturnDTOList.add(dr);


            // -------------------------
            prevCodeSet = todayCodeSet;
        }


        // ------------------------- next_daily_return -------------------------

        // 当前计算 次日 收益/净值   ->   被存储到了 当日 entity中     =>     需要移位（全部 daily_return/nav/capital 往后一位）
        next_daily_return(dailyReturnDTOList);


        return dailyReturnDTOList;
    }

    private TopPoolDailyReturnDTO avgDailyReturn(List<TopPoolDailyReturnDTO> dailyReturnDTOList) {
        TopPoolDailyReturnDTO avgDTO = new TopPoolDailyReturnDTO();


        // avg
        double dailyReturn = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getDailyReturn).average().orElse(0.0);
        double marginDailyReturn = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getMarginDailyReturn).average().orElse(0.0);
        double nav = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getNav).average().orElse(0.0);
        double marginNav = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getMarginNav).average().orElse(0.0);
        double capital = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getCapital).average().orElse(0.0);
        double marginCapital = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getMarginCapital).average().orElse(0.0);
        double prevCount = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getPrevCount).average().orElse(0.0);
        double todayCount = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getTodayCount).average().orElse(0.0);
        double oldPosCount = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getOldPosCount).average().orElse(0.0);
        double oldSellCount = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getOldSellCount).average().orElse(0.0);
        double newBuyCount = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getNewBuyCount).average().orElse(0.0);
        double posReplaceRatio = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getPosReplaceRatio).average().orElse(0.0);


        avgDTO.setDailyReturn(of(dailyReturn));
        avgDTO.setMarginDailyReturn(of(marginDailyReturn));
        avgDTO.setNav(of(nav));
        avgDTO.setMarginNav(of(marginNav));
        avgDTO.setCapital(of(capital));
        avgDTO.setMarginCapital(of(marginCapital));
        avgDTO.setPrevCount((int) prevCount);
        avgDTO.setTodayCount((int) todayCount);
        avgDTO.setOldPosCount((int) oldPosCount);
        avgDTO.setOldSellCount((int) oldSellCount);
        avgDTO.setNewBuyCount((int) newBuyCount);
        avgDTO.setPosReplaceRatio(of(posReplaceRatio));


        return avgDTO;
    }


    @Override
    public TopNAnalysisDTO top100(LocalDate startDate,
                                  LocalDate endDate,
                                  Integer topPoolType,
                                  Integer topStrategyType) {

        TopPoolAnalysisDTO dto = topListAnalysis(startDate, endDate, topPoolType, topStrategyType, null, null);

        List<TopCountDTO> top100List = dto.getCountDTOList().stream()
                                          .sorted(Comparator.comparing(TopCountDTO::getCount).reversed())
                                          .sorted(Comparator.comparing(TopCountDTO::getPct).reversed())
                                          .limit(100)
                                          .collect(Collectors.toList());


        top100List.forEach(e -> {

            String code = e.getCode();
            String name = e.getName();

            List<LocalDate> dateList = e.getDateList();


            StockFun fun = InitDataServiceImpl.data.getFun(code);


            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();
            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            List<ExtDataDTO> extDataDTOList = fun.getExtDataDTOList();


            int idx = 0;


            ExtDataDTO extDataDTO = extDataDTOList.get(idx);

            boolean SSF多 = extDataArrDTO.SSF多[idx];


            // 均线形态


            // 支撑线


            // 买点


            // 成交额


            // C_MA偏离率


            // C_SSF偏离率

        });


        return null;
    }


    @Override
    public List<BtDailyReturnDO> calcDailyReturn(List<BtDailyReturnDO> dailyReturnDOList) {
        double nav = 1.0;
        double capital = 100_0000;
        double dailyReturn = 0.0;

        double profitLossAmount = 0.0;
        double marketValue = 0.0;
        double avlCapital = 0.0;
        double buyCapital = 0.0;
        double sellCapital = 0.0;


        for (int i = 0; i < dailyReturnDOList.size(); i++) {
            BtDailyReturnDO e = dailyReturnDOList.get(i);


            if (i > 0) {
                dailyReturn = e.getDailyReturn().doubleValue();

                nav *= (1 + dailyReturn);
                capital *= (1 + dailyReturn);
            }


            double _capital = e.getCapital().doubleValue();
            profitLossAmount = capital * e.getProfitLossAmount().doubleValue() / _capital;
            marketValue = capital * e.getMarketValue().doubleValue() / _capital;
            avlCapital = capital * e.getAvlCapital().doubleValue() / _capital;
            buyCapital = capital * e.getBuyCapital().doubleValue() / _capital;
            sellCapital = capital * e.getSellCapital().doubleValue() / _capital;


            e.setDailyReturn(double2Decimal(dailyReturn));
            e.setNav(double2Decimal(nav));
            e.setCapital(double2Decimal(capital));
            e.setProfitLossAmount(double2Decimal(profitLossAmount));
            e.setMarketValue(double2Decimal(marketValue));
            e.setAvlCapital(double2Decimal(avlCapital));
            e.setBuyCapital(double2Decimal(buyCapital));
            e.setSellCapital(double2Decimal(sellCapital));
        }


        return dailyReturnDOList;
    }


    @TotalTime
    @Override
    public TopPoolSumReturnDTO sumReturn(List<BtDailyReturnDO> dailyReturnDOList,
                                         List<BtTradeRecordDO> tradeRecordList,
                                         List<BtPositionRecordDO> positionRecordList) {


        TopPoolSumReturnDTO sumReturnDTO = sumReturn_backtest(dailyReturnDOList, tradeRecordList, positionRecordList);


        return sumReturnDTO;
    }


    // -----------------------------------------------------------------------------------------------------------------


    private TopPoolSumReturnDTO sumReturn_topPool(List<TopPoolDailyReturnDTO> dailyReturnDTOList,
                                                  List<BtTradeRecordDO> tradeRecordList) {


        // 每日收益率列表（%）
        List<Double> dailyReturnPctList = dailyReturnDTOList.stream().map(e -> e.getDailyReturn()).collect(Collectors.toList());

        // 日期 - 当日收益率（小数）
        Map<LocalDate, Double> date_dailyReturn_Map = dailyReturnDTOList.stream().collect(Collectors.toMap(TopPoolDailyReturnDTO::getDate,
                                                                                                           e -> e.getDailyReturn() * 0.01,
                                                                                                           (v1, v2) -> v1,
                                                                                                           LinkedHashMap::new)); // 有序


        // 日均调仓换股比例
        double avgPosReplaceRatio = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getPosReplaceRatio).average().orElse(0);


        // -------------------------------------------------------------------------------------------------------------


        TopPoolSumReturnDTO sumReturnDTO = sumReturn(dailyReturnPctList, date_dailyReturn_Map, tradeRecordList, avgPosReplaceRatio);


        // -------------------------------------------------------------------------------------------------------------


        TopPoolDailyReturnDTO last = ListUtil.last(dailyReturnDTOList);
        if (null == last) {
            return sumReturnDTO;
        }


        return sumReturnDTO;
    }


    private TopPoolSumReturnDTO sumReturn_topPool_margin(List<TopPoolDailyReturnDTO> dailyReturnDTOList,
                                                         List<BtTradeRecordDO> tradeRecordList) {


        // 每日收益率列表（%）
        List<Double> dailyReturnPctList = dailyReturnDTOList.stream().map(e -> e.getMarginDailyReturn()).collect(Collectors.toList());

        // 日期 - 当日收益率（小数）
        Map<LocalDate, Double> date_dailyReturn_Map = dailyReturnDTOList.stream().collect(Collectors.toMap(TopPoolDailyReturnDTO::getDate,
                                                                                                           e -> e.getMarginDailyReturn() * 0.01,
                                                                                                           (v1, v2) -> v1,
                                                                                                           LinkedHashMap::new)); // 有序


        // 日均调仓换股比例
        double avgPosReplaceRatio = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getPosReplaceRatio).average().orElse(0);


        // -------------------------------------------------------------------------------------------------------------


        TopPoolSumReturnDTO sumReturnDTO = sumReturn(dailyReturnPctList, date_dailyReturn_Map, tradeRecordList, avgPosReplaceRatio);


        // -------------------------------------------------------------------------------------------------------------


        TopPoolDailyReturnDTO last = ListUtil.last(dailyReturnDTOList);
        if (null == last) {
            return sumReturnDTO;
        }


        return sumReturnDTO;
    }

    private TopPoolSumReturnDTO sumReturn_backtest(List<BtDailyReturnDO> dailyReturnDOList,
                                                   List<BtTradeRecordDO> tradeRecordList,
                                                   List<BtPositionRecordDO> positionRecordList) {


        List<Double> dailyReturnPctList = dailyReturnDOList.stream().map(e -> e.getDailyReturn().doubleValue() * 100).collect(Collectors.toList());

        Map<LocalDate, Double> date_dailyReturn_Map = dailyReturnDOList.stream().collect(Collectors.toMap(BtDailyReturnDO::getTradeDate,
                                                                                                          e -> e.getDailyReturn().doubleValue(),
                                                                                                          (v1, v2) -> v1,
                                                                                                          LinkedHashMap::new)); // 有序


        // 日均调仓换股比例
        double avgPosReplaceRatio = avgPosReplaceRatio(positionRecordList);


        // -------------------------------------------------------------------------------------------------------------


        TopPoolSumReturnDTO sumReturnDTO = sumReturn(dailyReturnPctList, date_dailyReturn_Map, tradeRecordList, avgPosReplaceRatio);


        // -------------------------------------------------------------------------------------------------------------


        BtDailyReturnDO last = ListUtil.last(dailyReturnDOList);
        if (null == last) {
            return sumReturnDTO;
        }


        return sumReturnDTO;
    }


    /**
     * 收益汇总结果（胜率/盈亏比、最大回撤、夏普比率、年化收益率、...）
     *
     * @param dailyReturnPctList   日收益率列表（%）
     * @param date_dailyReturn_Map 日期 - 日收益率映射
     * @param tradeRecordList      交易记录列表
     * @param avgPosReplaceRatio   日均调仓换股比例（小数）
     * @return
     */
    private TopPoolSumReturnDTO sumReturn(List<Double> dailyReturnPctList,
                                          Map<LocalDate, Double> date_dailyReturn_Map,
                                          List<BtTradeRecordDO> tradeRecordList,
                                          double avgPosReplaceRatio) {


        TopPoolSumReturnDTO sumReturnDTO = new TopPoolSumReturnDTO();


        // ---------------------------- 波峰/波谷/最大回撤


        TradePairStat.MaxDrawdownDTO maxDrawdownResult = TradePairStat.calcMaxDrawdown(date_dailyReturn_Map);

        sumReturnDTO.setMaxDrawdownPct(maxDrawdownResult.maxDrawdownPct);
        sumReturnDTO.setPeakNav(maxDrawdownResult.peakNav);
        sumReturnDTO.setPeakDate(maxDrawdownResult.peakDate);
        sumReturnDTO.setTroughNav(maxDrawdownResult.troughNav);
        sumReturnDTO.setTroughDate(maxDrawdownResult.troughDate);

        sumReturnDTO.setMaxNav(maxDrawdownResult.maxNav);
        sumReturnDTO.setMaxNavDate(maxDrawdownResult.maxNavDate);
        sumReturnDTO.setMinNav(maxDrawdownResult.minNav);
        sumReturnDTO.setMinNavDate(maxDrawdownResult.minNavDate);


        sumReturnDTO.setStartDate(maxDrawdownResult.startDate);
        sumReturnDTO.setEndDate(maxDrawdownResult.endDate);
        sumReturnDTO.setFinalNav(maxDrawdownResult.finalNav);
        sumReturnDTO.setFinalCapital(maxDrawdownResult.finalCapital);
        sumReturnDTO.setTotalReturnPct(maxDrawdownResult.totalReturnPct);


        // ---------------------------- 胜率/盈亏比（笔级）


        TradePairStat.TradeStatResult tradeStatResult = TradePairStat.calcTradeWinPct(tradeRecordList);   // 笔级交易明细（BS交易明细列表）

        sumReturnDTO.setTotalTrades(tradeStatResult.total);
        sumReturnDTO.setWinTrades(tradeStatResult.winTotal);
        sumReturnDTO.setWinTradesPct(tradeStatResult.winPct);
        sumReturnDTO.setLossTrades(tradeStatResult.lossTotal);
        sumReturnDTO.setLossTradesPct(tradeStatResult.lossPct);
        sumReturnDTO.setAvgWinTradesPct(tradeStatResult.avgWinPct);
        sumReturnDTO.setAvgLossTradesPct(tradeStatResult.avgLossPct);
        sumReturnDTO.setTradeLevelProfitFactor(tradeStatResult.profitFactor);

        sumReturnDTO.setTotalTradeAmount(tradeStatResult.totalTradeAmount);


        // ---------------------------- 胜率/盈亏比（日级）


        TradePairStat.TradeStatResult daysStatResult = TradePairStat.calcDayWinPct(dailyReturnPctList);   // 日级交易明细（日收益列表）

        sumReturnDTO.setTotalDays(daysStatResult.total);
        sumReturnDTO.setWinDays(daysStatResult.winTotal);
        sumReturnDTO.setWinDaysPct(daysStatResult.winPct);
        sumReturnDTO.setLossDays(daysStatResult.lossTotal);
        sumReturnDTO.setLossDaysPct(daysStatResult.lossPct);
        sumReturnDTO.setAvgWinDailyPct(daysStatResult.avgWinPct);
        sumReturnDTO.setAvgLossDailyPct(daysStatResult.avgLossPct);
        sumReturnDTO.setDailyLevelProfitFactor(daysStatResult.profitFactor);


        // ---------------------------- 期望/日均调仓比例/日均交易费率


        // 日均收益期望 = (胜率×日均盈利) - (败率×日均亏损)               // 日级
        double expectedDailyReturnRate = sumReturnDTO.getWinDaysPct() * 0.01 * sumReturnDTO.getAvgWinDailyPct() * 0.01 - sumReturnDTO.getLossDaysPct() * 0.01 * sumReturnDTO.getAvgLossDailyPct() * 0.01;
        sumReturnDTO.setExpectedDailyReturnPct(of(expectedDailyReturnRate * 100));


        // 净值期望 = (1 + 日均盈利)^盈利天数 × (1 - 日均亏损)^亏损天数
        double expectedNav = Math.pow(1 + sumReturnDTO.getAvgWinDailyPct() * 0.01, sumReturnDTO.getWinDays()) * Math.pow(1 - sumReturnDTO.getAvgLossDailyPct() * 0.01, sumReturnDTO.getLossDays());
        // 净值期望 = 初始净值 × (1 + 日均收益期望) ^ 期数
        double expectedNav2 = Math.pow(1 + expectedDailyReturnRate, sumReturnDTO.getWinDays() + sumReturnDTO.getLossDays());


        // 日均交易费率（1‰ * N%）  ->   假设：每日 调仓换股率 = 30%（即：每天 S淘汰 30%的 昨日持仓，B买入 30%的 今日新上榜个股）
        double avgFee = 0.001 * avgPosReplaceRatio;

        double expectedNav1_1 = Math.pow(1 + sumReturnDTO.getAvgWinDailyPct() * 0.01 - avgFee, sumReturnDTO.getWinDays()) * Math.pow(1 - sumReturnDTO.getAvgLossDailyPct() * 0.01 - avgFee, sumReturnDTO.getLossDays());
        double expectedNav2_2 = Math.pow(1 + expectedDailyReturnRate - avgFee, sumReturnDTO.getWinDays() + sumReturnDTO.getLossDays());


        sumReturnDTO.setExpectedNav(of(expectedNav));
        sumReturnDTO.setExpectedNav2(of(expectedNav2));

        sumReturnDTO.setAvgPosReplacePct(of(avgPosReplaceRatio * 100));
        sumReturnDTO.setAvgFeePct(of(avgFee * 100));
        sumReturnDTO.setExpectedNav1_1(of(expectedNav1_1));
        sumReturnDTO.setExpectedNav2_2(of(expectedNav2_2));


        // ---------------------------- 卡玛/夏普/sortino/年化/最大回撤


        // 假设 年化无风险利率 为2%
        double riskFreeRate = 0.02;


        List<Double> dailyReturnRateList = dailyReturnPctList.stream().map(e -> e * 0.01).collect(Collectors.toList());

        PerformanceMetrics.Result result = PerformanceMetrics.computeAll(dailyReturnRateList, riskFreeRate);

        sumReturnDTO.setCalmarRatio(of(result.calmar));
        sumReturnDTO.setRiskFreeRate(of(riskFreeRate));
        sumReturnDTO.setSharpeRatio(of(result.sharpe));
        sumReturnDTO.setSortinoRatio(of(result.sortino));
        sumReturnDTO.setAnnualReturnPct(of(result.annualizedReturn * 100));


        return sumReturnDTO;
    }


    private List<TopCountDTO> countDTOList(Map<String, TopCountDTO> code_countMap,
                                           Map<String, Integer> topBlock__codeCountMap,
                                           Map<String, Integer> topStock__codeCountMap) {


        // blockCodeList  ->  relaList（多对多）      =>       blockCode - relaList（stockList）    Map
        Map<String, Set<BaseBlockRelaStockDO>> blockCode_stockDOList_Map = baseBlockRelaStockService.listByBlockCodeList(topBlock__codeCountMap.keySet()) // 1. 获取数据
                                                                                                    .stream()                                             // 2. 转换为 Stream     // 3. 使用 groupingBy 收集
                                                                                                    .collect(Collectors.groupingBy(
                                                                                                            BaseBlockRelaStockDO::getBlockCode, // 作为Map的key
                                                                                                            Collectors.toSet()                  // 作为Map的value，将相同key的元素收集到Set中
                                                                                                    ));


        // stockCodeList  ->  relaList（多对多）      =>       stockCode - relaList（blockList）    Map
        Map<String, Set<BaseBlockRelaStockDO>> stockCode_blockDOList_Map = baseBlockRelaStockService.listByStockCodeList(topStock__codeCountMap.keySet())
                                                                                                    .stream()
                                                                                                    .collect(Collectors.toMap(
                                                                                                            BaseBlockRelaStockDO::getStockCode, // 作为Map的key
                                                                                                            Sets::newHashSet,                   // 作为Map的value（为每个元素创建一个包含该元素的Set）
                                                                                                            (v1, v2) -> {                       // 合并函数：当key重复时，将v2的元素添加到v1中
                                                                                                                v1.addAll(v2);
                                                                                                                return v1;
                                                                                                            }));


        // -------------------------------------------------------------------------------------------------------------


        code_countMap.values().parallelStream().forEach(e -> {
            // 平均涨跌幅
            double avgPct = Math.pow(1 + e.getPct(), 1.0 / e.getCount()) - 1;
            e.setAvgPct(of(avgPct * 100));
            e.setPct(of(e.getPct() * 100));


            String code = e.getCode();
            List<TopStockDTO.TopBlock> topBlockList = stockCode_blockDOList_Map.getOrDefault(code, Sets.newHashSet()).stream()
                                                                               .map(r -> {
                                                                                   TopStockDTO.TopBlock topBlock = new TopStockDTO.TopBlock();
                                                                                   topBlock.setBlockCode(r.getBlockCode());
                                                                                   topBlock.setBlockName(r.getBlockName());
                                                                                   topBlock.setTopDays(topBlock__codeCountMap.getOrDefault(topBlock.getBlockCode(), 0));
                                                                                   return topBlock;
                                                                               })
                                                                               .collect(Collectors.toList());


            List<TopBlockDTO.TopStock> topStockList = blockCode_stockDOList_Map.getOrDefault(code, Sets.newHashSet()).stream()
                                                                               .map(r -> {
                                                                                   TopBlockDTO.TopStock topStock = new TopBlockDTO.TopStock();
                                                                                   topStock.setStockCode(r.getStockCode());
                                                                                   topStock.setStockName(r.getStockName());
                                                                                   topStock.setTopDays(topStock__codeCountMap.getOrDefault(topStock.getStockCode(), 0));
                                                                                   return topStock;
                                                                               })
                                                                               .collect(Collectors.toList());


            // ---------------------------------------------------------------------------------------------------------


            // 主线板块 列表
            e.setTopBlockList(topBlockList);
            e.setTopStockList(topStockList);


            // ---------------------------------------------------------------------------------------------------------

            e.setBuySignalList(null);
            e.setSellSignalList(null);
        });


        // sort
        return code_countMap.values().stream()
                            .sorted(Comparator.comparing(TopCountDTO::getCount).reversed())
                            .sorted(Comparator.comparing(TopCountDTO::getPct).reversed())
                            .collect(Collectors.toList());
    }


    /**
     * 日均调仓换股比例
     *
     * @param positionRecordList 持仓记录列表
     * @return
     */
    private double avgPosReplaceRatio(List<BtPositionRecordDO> positionRecordList) {


        // 日期 -> 持仓代码Set
        Map<LocalDate, Set<String>> date_positionCodeMap = positionRecordList.stream().collect(Collectors.toMap(BtPositionRecordDO::getTradeDate,
                                                                                                                e -> Sets.newHashSet(e.getStockCode()),
                                                                                                                (v1, v2) -> {
                                                                                                                    v1.addAll(v2);
                                                                                                                    return v1;
                                                                                                                }));


        // -------------------------------------------------------------------------------------------------------------


        List<TopPoolDailyReturnDTO> dailyReturnDTOList = Lists.newArrayList();
        Set<String> preCodeSet = Sets.newHashSet();


        for (Map.Entry<LocalDate, Set<String>> entry : date_positionCodeMap.entrySet()) {
            LocalDate date = entry.getKey();
            Set<String> todayCodeSet = entry.getValue();


            // ------------------------------------------ 每日 收益/净值 ---------------------------------------------


//            double rate = 1 + daily_return * 0.01;

//            nav *= rate;
//            capital *= rate;


            TopPoolDailyReturnDTO dr = new TopPoolDailyReturnDTO();
            dr.setDate(date);
//            dr.setDaily_return(of(daily_return));
//            dr.setNav(of(nav));
//            dr.setCapital(of(capital));


            // ------------------------------------------ 每日 调仓换股 比例 ------------------------------------------


            // 当日调仓换股比例
            posReplaceRatio(dr, preCodeSet, todayCodeSet);


            dailyReturnDTOList.add(dr);


            // -------------------------
            preCodeSet = todayCodeSet;
        }


        // -------------------------------------------------------------------------------------------------------------


        // 日均调仓换股比例
        double avgPosReplaceRatio = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getPosReplaceRatio).average().orElse(0);


        return avgPosReplaceRatio;
    }


    /**
     * 当日调仓换股比例
     *
     * @param dr
     * @param preCodeSet
     * @param todayCodeSet
     */
    private void posReplaceRatio(TopPoolDailyReturnDTO dr, Set<String> preCodeSet, Set<String> todayCodeSet) {

        int preCount = preCodeSet.size();
        int todayCount = todayCodeSet.size();


        // 交集（继续 持有中）
        Collection<String> intersection = CollectionUtils.intersection(preCodeSet, todayCodeSet);
        // 今日S  =  pre - 交集
        preCodeSet.removeAll(intersection);
        // 今日B  =  today - 交集
        Set<String> todayCodeSet_copy = Sets.newHashSet(todayCodeSet);
        todayCodeSet_copy.removeAll(intersection);

        int oldPosCount = intersection.size();
        int oldSellCount = preCodeSet.size();
        int newBuyCount = todayCodeSet_copy.size();

        // 简化算法  =>  调仓比例 = 今日S / 昨日持有数
        double posReplaceRatio = preCount == 0 ? 0 : (double) oldSellCount / preCount;


        dr.setPrevCount(preCount);
        dr.setTodayCount(todayCount);
        dr.setOldPosCount(oldPosCount);
        dr.setOldSellCount(oldSellCount);
        dr.setNewBuyCount(newBuyCount);
        dr.setPosReplaceRatio(NumUtil.of(posReplaceRatio, 5));
    }


    /**
     * 当前计算 次日 收益/净值   ->   被存储到了 当日 entity中     =>     需要移位（全部 daily_return/nav/capital 往后一位）
     *
     * @param dailyReturnDTOList
     */
    private void next_daily_return(List<TopPoolDailyReturnDTO> dailyReturnDTOList) {
        // 1. 如果列表为空或只有一个元素，则无需移位
        if (dailyReturnDTOList == null || dailyReturnDTOList.size() <= 1) {
            return;
        }


        // 2. 从后往前遍历列表，将指定字段的值往后移一位
        // 从倒数第二个元素开始，直到第一个元素 (索引 0)
        for (int i = dailyReturnDTOList.size() - 2; i >= 0; i--) {

            TopPoolDailyReturnDTO curr = dailyReturnDTOList.get(i);
            TopPoolDailyReturnDTO next = dailyReturnDTOList.get(i + 1); // 下一个元素，即移位后的接收者


            // 将当前元素的值复制到下一个元素
            next.setDailyReturn(curr.getDailyReturn());
            next.setMarginDailyReturn(curr.getMarginDailyReturn());
            next.setNav(curr.getNav());
            next.setMarginNav(curr.getMarginNav());
            next.setCapital(curr.getCapital());
            next.setMarginCapital(curr.getMarginCapital());
        }


        // 3. 处理第一个元素（索引 0）的值
        TopPoolDailyReturnDTO first = dailyReturnDTOList.get(0);
        first.setDailyReturn(0.0);
        first.setMarginDailyReturn(0.0);
        first.setNav(1.0);
        first.setMarginNav(1.0);
        first.setCapital(100_0000.0);
        first.setMarginCapital(100_0000.0);
    }


}