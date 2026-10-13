package com.bebopze.tdx.quant.strategy.buy;

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
public class BacktestBuyStrategyB extends AbstractBuyStrategy implements BuyStrategy {


    @Override
    public String key() {
        return "B";
    }


    @Override
    public Set<String> buy__topStock__codeSet(Set<String> buyConSet,
                                              BacktestCache data,
                                              LocalDate tradeDate,
                                              Map<String, String> buy_infoMap,
                                              Boolean ztFlag) {


        Set<String> filter__stockCodeSet = Sets.newHashSet();
        data.getAllType_stockDOList(btCompareDTO.get().getBtStockType()).forEach(stockDO -> {


            String stockCode = stockDO.getCode();


            StockFun fun = data.getOrCreateStockFun(stockDO);

            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // -------------------------------------------


            // 当日 - 停牌（003005  ->  2022-10-27）
            Integer idx = dateIndexMap.get(tradeDate);
            if (idx == null) {
                return;
            }


            // -------------------------------------------


            // 是否买入
            // boolean signal_B = false;


            // --------------------------------------------------------------------------------------


            double 中期涨幅 = extDataArrDTO.中期涨幅N20[idx];


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


            // 必须 创新高
            boolean con_1 = N60日新高;


            // RPS一线红95/RPS双线红90/RPS三线红85
            boolean con_2 = RPS红;


            // 低位（中期涨幅<50）
            boolean con_3 = fun.is20CM() ? 中期涨幅 < 70 : 中期涨幅 < 50;

            // SSF多 + MA20多
            boolean con_4 = SSF多 && MA20多;


            // 月多/均线预萌出/大均线多头
            boolean con_5 = 月多 || 均线预萌出 || 大均线多头;

            // RPS三线红/口袋支点/60日新高
            // boolean con_6 = RPS三线红 || _60日新高  /*|| 口袋支点*/;


            // boolean signal_B = 月多 && _60日新高 && (RPS三线红 || 大均线多头) && SSF多;

            boolean signal_B = con_1 && con_2 && con_3 && con_4 && con_5;


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


                if (RPS红) info.add("RPS红");
                if (RPS三线红) info.add("RPS三线红");

                if (con_2) info.add("低位");

                if (SSF多) info.add("SSF多");
                if (MA20多) info.add("MA20多");

                if (月多) info.add("月多");
                if (均线预萌出) info.add("均线预萌出");
                if (大均线多头) info.add("大均线多头");

                if (RPS三线红) info.add("RPS三线红");
                if (N60日新高) info.add("60日新高");
                // if (口袋支点) info.add("口袋支点");
                info.add("idx-" + idx);


                buy_infoMap.put(stockCode, String.join(",", info));
            }
        });


        return filter__stockCodeSet;
    }


}