package com.bebopze.tdx.quant.strategy.sell;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.domain.dto.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.strategy.buy.BacktestBuyStrategy.getByDate;


/**
 * 回测 - S策略
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestSellStrategy extends SellStrategy {


    public static final Map<String, StockFun> stockFunMap = Maps.newConcurrentMap();

    public static final Map<String, BlockFun> blockFunMap = Maps.newConcurrentMap();


    // -----------------------------------------------------------------------------------------------------------------


    public List<String> rule(BacktestCache data,
                             LocalDate tradeDate,
                             List<String> positionStockCodeList,
                             Map<String, String> sell_infoMap) {


        // -------------------------------------------------------------------------------------------------------------


        // 2.1、当日 S策略（破位 -> S淘汰） -> stockCodeList（对昨日 持股 -> S淘汰）


        List<String> sell__stockCodeList = positionStockCodeList/*.parallelStream()*/.stream().filter(stockCode -> {
            BaseStockDO stockDO = data.codeStockMap.get(stockCode);


            StockFun fun = stockFunMap.computeIfAbsent(stockCode, k -> new StockFun(k, stockDO));


            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // -------------------------------------------


            // 是否淘汰
            // boolean flag_S = false;


            // -------------------------------------------


            // 1、月空
            boolean[] 月多_arr = extDataArrDTO.月多;
            boolean 月多 = getByDate(月多_arr, dateIndexMap, tradeDate);
            if (!月多) {
                sell_infoMap.put(stockCode, "月空" + ",idx-" + dateIndexMap.get(tradeDate));
                return true;
            }


            // 2、SSF空
            boolean[] SSF空_arr = extDataArrDTO.SSF空;
            boolean SSF空 = getByDate(SSF空_arr, dateIndexMap, tradeDate);
            if (SSF空) {
                sell_infoMap.put(stockCode, "SSF空" + ",idx-" + dateIndexMap.get(tradeDate));
                return true;
            }


            // 3、高位（中期涨幅_MA20 > 100）   ->   爆天量/长上影/大阴线
            // double[] 中期涨幅 = extDataArrDTO.中期涨幅;
            boolean[] 高位爆量上影大阴_arr = extDataArrDTO.高位爆量上影大阴;
            boolean 高位爆量上影大阴 = getByDate(高位爆量上影大阴_arr, dateIndexMap, tradeDate);
            if (高位爆量上影大阴) {
                sell_infoMap.put(stockCode, "高位爆量上影大阴" + ",idx-" + dateIndexMap.get(tradeDate));
                return true;
            }


            return false;

        }).collect(Collectors.toList());


        return sell__stockCodeList;
    }


    /**
     * 个股   指定日期 -> 收盘价
     *
     * @param blockCode
     * @param tradeDate
     * @return
     */
    private double getBlockClosePrice(String blockCode, LocalDate tradeDate) {
        Double closePrice = stock__dateCloseMap.get(blockCode).get(DateTimeUtil.format_yyyy_MM_dd(tradeDate));
        return closePrice == null ? 0.0 : closePrice;
    }

    /**
     * 个股   指定日期 -> 收盘价
     *
     * @param stockCode
     * @param tradeDate
     * @return
     */
    private double getStockClosePrice(String stockCode, LocalDate tradeDate) {
        Double closePrice = stock__dateCloseMap.get(stockCode).get(DateTimeUtil.format_yyyy_MM_dd(tradeDate));
        return closePrice == null ? 0.0 : closePrice;
    }

}
