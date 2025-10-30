package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.config.BizException;
import com.bebopze.tdx.quant.common.domain.dto.analysis.*;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.*;
import com.bebopze.tdx.quant.common.tdxfun.PerformanceMetrics;
import com.bebopze.tdx.quant.common.util.ListUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.*;
import com.bebopze.tdx.quant.dal.service.IBaseBlockRelaStockService;
import com.bebopze.tdx.quant.dal.service.IQaTopBlockService;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.DataAnalysisService;
import com.bebopze.tdx.quant.strategy.backtest.TradePairStat;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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


    @Autowired
    private IQaTopBlockService qaTopBlockService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;


    @Override
    public TopPoolAnalysisDTO topListAnalysis(LocalDate startDate,
                                              LocalDate endDate,
                                              Integer topPoolType,
                                              Integer type) {

        TopPoolAnalysisDTO dto = new TopPoolAnalysisDTO();


        // -------------------------------------------------------------------------------------------------------------


        // 持仓列表、收益率列表
        List<QaTopBlockDO> list = qaTopBlockService.listByDate(startDate, endDate);


        // -------------------------------------------------------------------------------------------------------------


        // ---------------------------------------- count 指标

        Map<String, TopCountDTO> code_countMap = Maps.newHashMap();

        Map<String, Integer> topStock__codeCountMap = Maps.newHashMap();
        Map<String, Integer> topBlock__codeCountMap = Maps.newHashMap();


        // -------------------------------------------------------------------------------------------------------------


        // 每日收益率列表
        List<TopPoolDailyReturnDTO> dailyReturnDTOList = dailyReturnDTOList(list, topPoolType, type, code_countMap, topStock__codeCountMap, topBlock__codeCountMap);


        // -------------------------------------------------------------------------------------------------------------


        TopPoolSumReturnDTO sumReturnDTO = sumReturn_topPool(dailyReturnDTOList, null);


        // -------------------------------------------------------------------------------------------------------------


        dto.setSumReturnDTO(sumReturnDTO);
        dto.setDailyReturnDTOList(dailyReturnDTOList);
        dto.setCountDTOList(countDTOList(code_countMap, topBlock__codeCountMap, topStock__codeCountMap));


        // -------------------------------------------------------------------------------------------------------------


        return dto;
    }


    /**
     * 每日收益率列表
     *
     * @param list
     * @param topPoolType
     * @param type
     * @param code_countMap
     * @param topStock__codeCountMap
     * @param topBlock__codeCountMap
     * @return
     */
    private List<TopPoolDailyReturnDTO> dailyReturnDTOList(List<QaTopBlockDO> list, Integer topPoolType, Integer type,
                                                           Map<String, TopCountDTO> code_countMap,
                                                           Map<String, Integer> topStock__codeCountMap,
                                                           Map<String, Integer> topBlock__codeCountMap) {


        List<TopPoolDailyReturnDTO> dailyReturnDTOList = Lists.newArrayList();
        Set<String> preCodeSet = Sets.newHashSet();


        // ---------------------------------------- 汇总指标

        double nav = 1.0;            // 初始净值
        double capital = 100_0000;   // 初始资金


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
                double today2NextChangePct = topMap.get(code).getToday2Next_changePct() * 0.01;


                code_countMap.merge(code, new TopCountDTO(code, name, 1, today2NextChangePct, date), (old, newVal) -> {
                    old.setCount(old.getCount() + newVal.getCount());
                    old.setPct((1 + old.getPct()) * (1 + newVal.getPct()) - 1);
                    old.getPctList().add(of(today2NextChangePct * 100));
                    old.getDateList().add(date);

                    return old;
                });
            });


            // --------------------------------------------------


            if (actualStartDate == null) {
                continue;
            }


            // ------------------------------------------ 每日 收益/净值 ---------------------------------------------


            double rate = 1 + daily_return * 0.01;

            nav *= rate;
            capital *= rate;


            TopPoolDailyReturnDTO dr = new TopPoolDailyReturnDTO();
            dr.setDate(date);
            dr.setDaily_return(of(daily_return));
            dr.setNav(of(nav));
            dr.setCapital(of(capital));


            // ------------------------------------------ 每日 调仓换股 比例 ------------------------------------------


            // 当日调仓换股比例
            posReplaceRatio(dr, preCodeSet, todayCodeSet);


            dailyReturnDTOList.add(dr);


            // -------------------------
            preCodeSet = todayCodeSet;
        }


        return dailyReturnDTOList;
    }


    @Override
    public TopNAnalysisDTO top100(LocalDate startDate, LocalDate endDate, Integer topPoolType, Integer type) {

        TopPoolAnalysisDTO dto = topListAnalysis(startDate, endDate, topPoolType, type);

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
        List<Double> dailyReturnPctList = dailyReturnDTOList.stream().map(e -> e.getDaily_return()).collect(Collectors.toList());

        // 日期 - 当日收益率（小数）
        Map<LocalDate, Double> date_dailyReturn_Map = dailyReturnDTOList.stream().collect(Collectors.toMap(TopPoolDailyReturnDTO::getDate,
                                                                                                           e -> e.getDaily_return() * 0.01,
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


        // ---------------------------- 起始日期

        sumReturnDTO.setStartDate(dailyReturnDTOList.get(0).getDate());
        sumReturnDTO.setEndDate(last.getDate());


        // ---------------------------- 总收益

        sumReturnDTO.setFinalNav(last.getNav());
        sumReturnDTO.setFinalCapital(last.getCapital());
        sumReturnDTO.setTotalReturnPct(of((last.getNav() - 1) * 100.0));


//        // ---------------------------- 年化
//
//        // 年化收益率（%） = （期末净值 / 初始净值）^(252 / 总天数) - 1          x 100%
//        double annualReturn = Math.pow(sumReturnDTO.getFinalNav(), 252.0 / sumReturnDTO.getTotalDays()) - 1;
//        sumReturnDTO.setAnnualReturnPct(of(annualReturn * 100));


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


        // ---------------------------- 起始日期

        sumReturnDTO.setStartDate(dailyReturnDOList.get(0).getTradeDate());
        sumReturnDTO.setEndDate(last.getTradeDate());


        // ---------------------------- 总收益

        sumReturnDTO.setFinalNav(last.getNav().doubleValue());
        sumReturnDTO.setFinalCapital(last.getCapital().doubleValue());
        sumReturnDTO.setTotalReturnPct(of((last.getNav().doubleValue() - 1) * 100.0));


//        // ---------------------------- 年化
//
//        // 年化收益率（%） = （期末净值 / 初始净值）^(252 / 总天数) - 1          x 100%
//        double annualReturn = Math.pow(sumReturnDTO.getFinalNav(), 252.0 / sumReturnDTO.getTotalDays()) - 1;
//        sumReturnDTO.setAnnualReturnPct(of(annualReturn * 100));


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


//        Map<LocalDate, Double> date_dailyReturn_Map = dailyReturnDOList.stream().collect(Collectors.toMap(BtDailyReturnDO::getTradeDate,
//                                                                                                          e -> e.getDailyReturn().doubleValue()))
//                                                                       .sorted(Map.Entry.comparingByKey());


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


//        List<Double> dailyReturnPctList = dailyReturnDOList.stream().map(e -> e.getDailyReturn().doubleValue() * 100).collect(Collectors.toList());


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


        // 日均收益期望 = (胜率×平均盈利) - (败率×平均亏损)               // 日级
        double expectedDailyReturnRate = sumReturnDTO.getWinDaysPct() * 0.01 * sumReturnDTO.getAvgWinDailyPct() * 0.01 - sumReturnDTO.getLossDaysPct() * 0.01 * sumReturnDTO.getAvgLossDailyPct() * 0.01;
        sumReturnDTO.setExpectedDailyReturnPct(of(expectedDailyReturnRate * 100));


        // 净值期望 = (1 + 日均盈利)^盈利天数 × (1 - 日均亏损)^亏损天数
        double expectedNav = Math.pow(1 + sumReturnDTO.getAvgWinDailyPct() * 0.01, sumReturnDTO.getWinDays()) * Math.pow(1 - sumReturnDTO.getAvgLossDailyPct() * 0.01, sumReturnDTO.getLossDays());
        // 净值期望 = 初始净值 × (1 + 期望值) ^ 期数
        double expectedNav2 = Math.pow(1 + expectedDailyReturnRate, sumReturnDTO.getWinDays() + sumReturnDTO.getLossDays());


        // 日均调仓换股比例
//        double avgPosReplaceRatio = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getPosReplaceRatio).average().orElse(0);
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


        // ---------------------------- 波峰/波谷/最大回撤


//        // 净值波峰
//        sumReturnDTO.setPeakNav(of(peakNav));
//        sumReturnDTO.setPeakDate(peakDate);
//        // 净值波谷
//        sumReturnDTO.setTroughNav(of(troughNav));
//        sumReturnDTO.setTroughDate(troughDate);
//        // 最大回撤
//        sumReturnDTO.setMaxDrawdownPct(of(maxDrawdown * -100));
//
//        // max/min
//        sumReturnDTO.setMaxNav(of(maxNav));
//        sumReturnDTO.setMaxNavDate(maxNavDate);
//        sumReturnDTO.setMinNav(of(minNav));
//        sumReturnDTO.setMinNavDate(minNavDate);


//        // ---------------------------- 卡玛比率
//
//
//        // 卡玛比率 = 年化收益 / 最大回撤
//        double calmarRatio = annualReturn / maxDrawdown;
//        sumReturnDTO.setCalmarRatio(of(calmarRatio));
//
//
//        // ---------------------------- 夏普比率 = 超额收益 / 总波动
//
//
//        // 夏普比率 = 日均收益 / 日收益标准差  *  sqrt(252)
//
//
//        // 日均收益
//        double mean = dailyReturnDTOList.stream().mapToDouble(TopPoolDailyReturnDTO::getDaily_return).average().orElse(0);
//        // 日收益标准差  =  sqrt( sum(diff ^2) / size )
//        double sd = Math.sqrt(dailyReturnDTOList.stream()
//                                                .mapToDouble(r -> Math.pow(r.getDaily_return() - mean, 2))
//                                                .sum() / dailyReturnDTOList.size());
//
//
//        // * sqrt(252) 是一个 年化因子，用于将基于 日度数据计算出的风险调整比率 转换成 年度比率，使其与通常以年为单位报告的收益率和风险指标（如年化标准差）在时间尺度上保持一致
//        // 但请注意，这种方式计算出的分子是 总收益 而非超额收益，因此它不是标准定义下的夏普比率
//
//        // 夏普比率 = 日均收益 / 日收益标准差  *  sqrt(252)
//        double sharpeRatio = mean / sd * Math.sqrt(252);   // 年化因子：N * Math.sqrt(252)          日 -> 年
//        sumReturnDTO.setSharpeRatio(of(sharpeRatio));      // 忽略 年化无风险利率（2%年利 -> 基本可忽略）
//
//
//        // ---------------
//
//
//        double[] dailyReturns2 = dailyReturnDTOList.stream().mapToDouble(e -> e.getDaily_return() * 0.01).toArray();
//        // 假设 年化无风险利率 为2%
//        double riskFreeRate = 0.02;
//
//        // 夏普比率 = (年化回报率 - 无风险利率) / 年化标准差
//        double sharpeRatio2 = SharpeFun.calcSharpeRatio(dailyReturns2, riskFreeRate);
//
//        sumReturnDTO.setRiskFreeRate(of(riskFreeRate));
//        sumReturnDTO.setSharpeRatio2(of(sharpeRatio2));


        // ---------------


        // 假设 年化无风险利率 为2%
        double riskFreeRate = 0.02;


        List<Double> dailyReturnRateList = dailyReturnPctList.stream().map(e -> e * 0.01).collect(Collectors.toList());

        PerformanceMetrics.Result result = PerformanceMetrics.computeAll(dailyReturnRateList, riskFreeRate);
        // sumReturnDTO.setR(result);


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


        Map<String, Set<BaseBlockRelaStockDO>> blockCode_stockDOList_Map = baseBlockRelaStockService.listByBlockCodeList(topBlock__codeCountMap.keySet()).stream().
                                                                                                    collect(Collectors.toMap(BaseBlockRelaStockDO::getBlockCode,
                                                                                                                             Sets::newHashSet,
                                                                                                                             (v1, v2) -> {
                                                                                                                                 v1.addAll(v2);
                                                                                                                                 return v1;
                                                                                                                             }));

        Map<String, Set<BaseBlockRelaStockDO>> stockCode_blockDOList_Map = baseBlockRelaStockService.listByStockCodeList(topStock__codeCountMap.keySet()).stream()
                                                                                                    .collect(Collectors.toMap(BaseBlockRelaStockDO::getStockCode,
                                                                                                                              Sets::newHashSet,
                                                                                                                              (v1, v2) -> {
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


        dr.setPreCount(preCount);
        dr.setTodayCount(todayCount);
        dr.setOldPosCount(oldPosCount);
        dr.setOldSellCount(oldSellCount);
        dr.setNewBuyCount(newBuyCount);
        dr.setPosReplaceRatio(NumUtil.of(posReplaceRatio, 5));
    }


}