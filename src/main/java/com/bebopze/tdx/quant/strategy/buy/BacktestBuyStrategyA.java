package com.bebopze.tdx.quant.strategy.buy;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.constant.BlockNewIdEnum;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.KlineDTO;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.service.TopBlockService;
import com.bebopze.tdx.quant.strategy.QuickOption;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.tdxfun.Tools.*;
import static com.bebopze.tdx.quant.common.util.BoolUtil.bool2Int;


/**
 * 回测 - B策略
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestBuyStrategyA implements BuyStrategy {


    @Autowired
    private MarketService marketService;

    @Autowired
    private TopBlockService topBlockService;


    @Override
    public String key() {
        return "A";
    }


    /**
     * 买入策略   =   大盘（70%） +  主线板块（25%） +  个股买点（5%）
     *
     * @param data
     * @param tradeDate
     * @param buy_infoMap
     * @return
     */
    @Override
    public List<String> rule(BacktestCache data, LocalDate tradeDate, Map<String, String> buy_infoMap) {


        // -------------------------------------------------------------------------------------------------------------
        //                                                1、大盘 -> 仓位
        // -------------------------------------------------------------------------------------------------------------

        // QaMarketMidCycleDO qaMarketMidCycleDO = marketService.marketInfo(tradeDate);
        // Assert.notNull(qaMarketMidCycleDO, "[大盘量化]数据为空：" + tradeDate);


        // 总仓位-上限
        // BigDecimal positionPct = qaMarketMidCycleDO.getPositionPct();


        // -------------------------------------------------------------------------------------------------------------
        //                                                2、主线板块
        // -------------------------------------------------------------------------------------------------------------


        // 主线板块
        Map<String, Integer> blockCode_count_Map = topBlockService.topBlockRate(BlockNewIdEnum.百日新高.getBlockNewId(), tradeDate, 2, 10);
        // Set<String> filter__blockCodeSet = blockCode_count_Map.keySet().stream().map(e -> e.split("-")[0]).collect(Collectors.toSet());

        // 仅取 TOP1 板块
        Set<String> filter__blockCodeSet = MapUtils.isEmpty(blockCode_count_Map) ? Sets.newHashSet() :
                Sets.newHashSet(blockCode_count_Map.keySet().iterator().next().split("-")[0]);


        // -------------------------------------------------------------------------------------------------------------
        //                                                3、（强势）个股
        // -------------------------------------------------------------------------------------------------------------


        List<String> filter__stockCodeList = Collections.synchronizedList(Lists.newArrayList());
        data.stockDOList.parallelStream().forEach(stockDO -> {


            try {


                String stockCode = stockDO.getCode();


                StockFun fun = data.stockFunCache.get(stockCode, k -> new StockFun(k, stockDO));

                ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
                Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                // -------------------------------------------


                // 当日 - 停牌（003005  ->  2022-10-27）
                Integer idx = dateIndexMap.get(tradeDate);
                if (idx == null) {
                    return;
                }


                // --------------------------------------------------------------------------------------

                double[] rps50_arr = extDataArrDTO.rps50;
                double[] rps120_arr = extDataArrDTO.rps120;
                double[] rps250_arr = extDataArrDTO.rps250;


                double rps50 = rps50_arr[idx];
                double rps120 = rps120_arr[idx];
                double rps250 = rps250_arr[idx];


                // RPS
                boolean RPS一线红 = RPS一线红(rps50, rps120, rps250, 95);
                boolean RPS双线红 = RPS双线红(rps50, rps120, rps250, 90);
                boolean RPS三线红 = RPS三线红(rps50, rps120, rps250, 85);


                // --------------------------------------------------------------------------------------


                // double 中期涨幅 = extDataArrDTO.中期涨幅[idx];


                boolean SSF多 = extDataArrDTO.SSF多[idx];
                boolean MA20多 = extDataArrDTO.MA20多[idx];


                boolean 月多 = extDataArrDTO.月多[idx];
                boolean RPS红 = extDataArrDTO.RPS红[idx];


                boolean N60日新高 = extDataArrDTO.N60日新高[idx];
                boolean 均线预萌出 = extDataArrDTO.均线预萌出[idx];
                boolean 大均线多头 = extDataArrDTO.大均线多头[idx];


                boolean 高位爆量上影大阴 = extDataArrDTO.高位爆量上影大阴[idx];


                // -------------------------------------------

                // B -> 持 -> S


                // B  =>  RPS一线红95/RPS双线红90/RPS三线红85   +   低位（中期涨幅<50）   +   SSF多 + MA20多   +   均线预萌出/大均线多头   +   RPS三线红/口袋支点/60日新高


                // 持  =>  RPS一线红95   +   MA20多/SSF多


                // S  =>  高位 -> 月空/高位爆量上影大阴   /   MA空200   /   MA20空/SSF空   /   RPS三线和<210


                // -------------------------------------------


                // B  =>  RPS一线红95/RPS双线红90/RPS三线红85   +   低位（中期涨幅<50）   +   SSF多 + MA20多   +   月多/均线预萌出/大均线多头   +   RPS三线红/口袋支点/60日新高


                // RPS一线红95/RPS双线红90/RPS三线红85
                boolean con_1 = RPS一线红 || RPS双线红 || RPS三线红;


                // TODO DEL     check
                if (con_1 != RPS红) {
                    String debugMsg = String.format("con_1 != RPS红     >>>     RPS一线红 : {} , RPS双线红 : {} , RPS三线红 : {} , RPS红 : {}", RPS一线红, RPS双线红, RPS三线红, RPS红);
                    log.debug(debugMsg);
                }
//            Assert.isTrue(con_1 == RPS红,
//                          String.format("con_1 != RPS红     >>>     RPS一线红 : {} , RPS双线红 : {} , RPS三线红 : {} , RPS红 : {}",
//                                        RPS一线红, RPS双线红, RPS三线红, RPS红));


                // 低位（中期涨幅<50）
                boolean con_2 = true; // fun.is20CM() ? 中期涨幅 < 70 : 中期涨幅 < 50;

                // SSF多 + MA20多
                boolean con_3 = SSF多 && MA20多;


                // 月多/均线预萌出/大均线多头
                boolean con_4 = 月多 || 均线预萌出 || 大均线多头;

                // RPS三线红/口袋支点/60日新高
                boolean con_5 = /*RPS三线红 ||*/ N60日新高 /*|| 口袋支点*/;


                // 偏离率 < 10%
                double C_SSF_偏离率 = fun.C_SSF_偏离率(idx);
                boolean con_6 = C_SSF_偏离率 < 10;


                // 非卖点
                boolean con_7 = !高位爆量上影大阴;


                // -------------------------------------------


                // 是否买入
                boolean signal_B = con_1 && con_2 && con_3 && con_4 && con_5 && con_6 && con_7;
                if (signal_B) {

                    filter__stockCodeList.add(stockCode);


                    // ----------------------------------------------------- info


                    // 动态收集所有为 true 的信号名称，按固定顺序拼接
                    List<String> info = Lists.newArrayList();


                    // 行业板块
                    String pthyLv2 = data.getPthyLv2(stockCode);
                    String getYjhyLv1 = data.getYjhyLv1(stockCode);
                    info.add(pthyLv2);
                    info.add(getYjhyLv1 + "     ");


                    if (RPS一线红) info.add("RPS一线红");
                    if (RPS双线红) info.add("RPS双线红");
                    if (RPS三线红) info.add("RPS三线红");

                    // if (con_2) info.add("低位");

                    if (SSF多) info.add("SSF多");
                    if (MA20多) info.add("MA20多");

                    if (月多) info.add("月多");
                    if (均线预萌出) info.add("均线预萌出");
                    if (大均线多头) info.add("大均线多头");

                    if (RPS三线红) info.add("RPS三线红");
                    if (N60日新高) info.add("60日新高");
                    // if (口袋支点) info.add("口袋支点");
                    info.add("idx-" + dateIndexMap.get(tradeDate));


                    buy_infoMap.put(stockCode, String.join(",", info));
                }


            } catch (Exception e) {

                log.error("filter强势个股 - err     >>>     stockCode : {} , stockDO : {} , errMsg : {}",
                          stockDO.getCode(), JSON.toJSONString(stockDO), e.getMessage(), e);
            }
        });


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股 -> IN 主线板块
        // -------------------------------------------------------------------------------------------------------------


        // 个股   ->   IN 主线板块
        List<String> filter__stockCodeList2 = filter__stockCodeList/*.parallelStream()*/.stream().filter(stockCode -> {
            Set<String> blockCodeSet = data.stockCode_blockCodeSet_Map.getOrDefault(stockCode, Sets.newHashSet());


            // B（主线板块）
            boolean block_B = false;
            for (String blockCode : blockCodeSet) {

                block_B = filter__blockCodeSet.contains(blockCode);
                if (block_B) {
                    log.debug("个股 -> IN 主线板块     >>>     {} , [{}-{}] , [{}-{}]", tradeDate,
                              stockCode, data.stock__codeNameMap.get(stockCode),
                              blockCode, data.block__codeNameMap.get(blockCode));
                    break;
                }
            }


            return block_B;
        }).collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        // 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）

        buyStrategy_ETF(filter__stockCodeList2, data, tradeDate, buy_infoMap);


        // -------------------------------------------------------------------------------------------------------------


        // TODO     按照 规则打分 -> sort
        List<String> filterSort__stockCodeList = scoreSort(filter__stockCodeList2, data, tradeDate, 20);
        // List<String> filterSort__stockCodeList = filter__stockCodeList2.stream().limit(20).collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        return filterSort__stockCodeList;


    }


    /**
     * 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）
     *
     * @param filter__stockCodeList2
     * @param data
     * @param tradeDate
     * @param buy_infoMap
     */
    private void buyStrategy_ETF(List<String> filter__stockCodeList2,
                                 BacktestCache data,
                                 LocalDate tradeDate,
                                 Map<String, String> buy_infoMap) {

        if (!CollectionUtils.isEmpty(filter__stockCodeList2)) {
            return;
        }


        // 大盘量化
        QaMarketMidCycleDO qaMarketMidCycleDO = marketService.marketInfo(tradeDate);
        Assert.notNull(qaMarketMidCycleDO, "[大盘量化]数据为空：" + tradeDate);


        // 大盘-牛熊：1-牛市；2-熊市；
        Integer marketBullBearStatus = qaMarketMidCycleDO.getMarketBullBearStatus();
        // 大盘-中期顶底：1-底部；2- 底->顶；3-顶部；4- 顶->底；
        Integer marketMidStatus = qaMarketMidCycleDO.getMarketMidStatus();


        // 大盘底
        if (marketMidStatus == 1) {


            // ETF
            data.ETF_stockDOList.forEach(e -> {

                // ETF   ->   最小交易日（上市日期）
                List<KlineDTO> klineDTOList = e.getKlineDTOList();
                LocalDate date = klineDTOList.get(0).getDate();

                // 当前日期   ->   已上市
                if (date.isBefore(tradeDate)) {
                    String stockCode = e.getCode();

                    filter__stockCodeList2.add(stockCode);
                    buy_infoMap.put(stockCode, "大盘极限底->ETF策略");
                }
            });


            System.out.println(filter__stockCodeList2);
        }
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
        for (String code : stockCodeList) {
            String stockName = codeNameMap.get(code);


            // -------------------------------------------------------------------------------------------


            // BUY策略   ->   已完成init
            StockFun fun = data.stockFunCache.getIfPresent(code);


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


            scoredStocks.add(new QuickOption.StockScore(code, stockName, RPS和, 中期涨幅, 大均线多头, N日新高, amount, 0));
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