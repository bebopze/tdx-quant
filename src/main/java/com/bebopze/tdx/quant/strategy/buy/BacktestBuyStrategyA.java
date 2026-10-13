package com.bebopze.tdx.quant.strategy.buy;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.btCompareDTO;


/**
 * 回测 - B策略
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestBuyStrategyA extends AbstractBuyStrategy implements BuyStrategy {


    @Override
    public String key() {
        return "A";
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
    @Override
    public Set<String> buy__topStock__codeSet(Set<String> buyConSet,
                                              BacktestCache data,
                                              LocalDate tradeDate,
                                              Map<String, String> buy_infoMap,
                                              Boolean ztFlag) {


        Set<String> filter__stockCodeSet = Sets.newHashSet();
        data.getAllType_stockDOList(btCompareDTO.get().getBtStockType()).forEach(stockDO -> {


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

                    filter__stockCodeSet.add(stockCode);
                    // buySignalInfo(buy_infoMap, stockCode, data, idx, conMap);


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


        return filter__stockCodeSet;
    }


}