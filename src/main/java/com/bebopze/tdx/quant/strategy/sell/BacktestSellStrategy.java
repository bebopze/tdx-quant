package com.bebopze.tdx.quant.strategy.sell;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataArrDTO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.indicator.StockFun;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 回测 - S策略
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestSellStrategy implements SellStrategy {


    // -----------------------------------------------------------------------------------------------------------------


    @Override
    public String key() {
        return "A";
    }


    @Override
    public List<String> rule(BacktestCache data,
                             LocalDate tradeDate,
                             List<String> positionStockCodeList,
                             Map<String, String> sell_infoMap) {


        // -------------------------------------------------------------------------------------------------------------


        // 2.1、当日 S策略（破位 -> S淘汰） -> stockCodeList（对昨日 持股 -> S淘汰）


        List<String> sell__stockCodeList = positionStockCodeList.stream().filter(stockCode -> {
            BaseStockDO stockDO = data.codeStockMap.get(stockCode);


            StockFun fun = data.stockFunMap.computeIfAbsent(stockCode, k -> new StockFun(k, stockDO));


            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // -------------------------------------------


            // 当日 - 停牌（600446  ->  2023-05-10）
            Integer idx = dateIndexMap.get(tradeDate);
            if (idx == null) {
                return false;
            }


            // -------------------------------------------


            // 是否淘汰
            // boolean flag_S = false;


            // -------------------------------------------


            // 月空
            boolean 月多 = extDataArrDTO.月多[idx];
            boolean MA20空 = extDataArrDTO.MA20空[idx];
            if (!月多 && MA20空) {
                sell_infoMap.put(stockCode, "月空" + ",idx-" + idx);
                sell_infoMap.put(stockCode, "MA20空" + ",idx-" + idx);
                return true;
            }


            // SSF空
            boolean SSF空 = extDataArrDTO.SSF空[idx];
            if (SSF空) {
                sell_infoMap.put(stockCode, "SSF空" + ",idx-" + idx);
                return true;
            }


            // 高位（中期涨幅_MA20 > 100）   ->   爆天量/长上影/大阴线
            boolean 高位爆量上影大阴 = extDataArrDTO.高位爆量上影大阴[idx];
            if (高位爆量上影大阴) {
                sell_infoMap.put(stockCode, "高位爆量上影大阴" + ",idx-" + idx);
                return true;
            }


            // 偏离率 > 25%
            double C_SSF_偏离率 = fun.C_SSF_偏离率(idx);
            if (C_SSF_偏离率 > 25) {
                sell_infoMap.put(stockCode, "C_SSF_偏离率>25%" + ",idx-" + idx);
                return true;
            }


            // TODO     最大 亏损线  ->  -7% 止损


            return false;

        }).collect(Collectors.toList());


        return sell__stockCodeList;
    }


}
