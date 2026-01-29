package com.bebopze.tdx.quant.strategy.buy;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.constant.BlockNewIdEnum;
import com.bebopze.tdx.quant.common.constant.TopBlockStrategyEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.service.MarketService;
import com.bebopze.tdx.quant.service.TopBlockService;
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

import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.btCompareDTO;


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

    @Autowired
    private TopBlockStrategy topBlockStrategy;


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
     * @param posRate
     * @return
     */
    @Override
    public List<String> rule(TopBlockStrategyEnum topBlockStrategyEnum,
                             Set<String> buyConSet,
                             BacktestCache data,
                             LocalDate tradeDate,
                             Map<String, String> buy_infoMap,
                             double posRate,
                             Boolean ztFlag) {


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
        data.getStockDOList(btCompareDTO.get().getStockType()).parallelStream().forEach(stockDO -> {


            try {


                String stockCode = stockDO.getCode();


                StockFun fun = data.getOrCreateStockFun(stockDO);

                ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
                Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


                // -------------------------------------------


                // 当日 - 停牌（003005  ->  2022-10-27）
                Integer idx = dateIndexMap.get(tradeDate);

                // 过滤 停牌/新股
                if (idx == null || Double.isNaN(extDataArrDTO.rps50[idx])) {
                    return;
                }


                // --------------------------------------------------------------------------------------


                // double 中期涨幅 = extDataArrDTO.中期涨幅[idx];


                double C_SSF_偏离率 = extDataArrDTO.C_SSF_偏离率[idx];
                boolean 高位爆量上影大阴 = extDataArrDTO.高位爆量上影大阴[idx];


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
        Set<String> filter__stockCodeSet2 = filter__stockCodeList/*.parallelStream()*/.stream().filter(stockCode -> {
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
        }).collect(Collectors.toSet());


        // -------------------------------------------------------------------------------------------------------------


        // 大盘极限底（按照正常策略  ->  将无股可买）      =>       指数ETF 策略（分批买入 50% -> 100%）
        topBlockStrategy.buyStrategy_ETF(filter__stockCodeSet2, data, tradeDate, buy_infoMap, posRate);


        // -------------------------------------------------------------------------------------------------------------


        // TODO     按照 规则打分 -> sort
        List<String> filterSort__stockCodeList = ScoreSort.scoreSort(filter__stockCodeSet2, data, tradeDate, 20);
        // List<String> filterSort__stockCodeList = filter__stockCodeSet2.stream().limit(20).collect(Collectors.toList());


        // -------------------------------------------------------------------------------------------------------------


        return filterSort__stockCodeList;
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
            data.ETF_stockDOList.forEach(e -> {

                // ETF   ->   最小交易日（上市日期）
                List<KlineDTO> klineDTOList = e.getKlineDTOList();
                LocalDate date = CollectionUtils.isEmpty(klineDTOList) ? null : klineDTOList.get(0).getDate();

                // 当前日期   ->   已上市
                if (date != null && date.isBefore(tradeDate)) {
                    String stockCode = e.getCode();

                    inTopBlock__stockCodeSet.add(stockCode);
                    buy_infoMap.put(stockCode, "大盘极限底->ETF策略");
                }
            });


            System.out.println(inTopBlock__stockCodeSet);
        }
    }


}