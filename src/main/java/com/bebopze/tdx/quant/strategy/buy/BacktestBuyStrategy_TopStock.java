package com.bebopze.tdx.quant.strategy.buy;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.strategy.sell.BacktestSellStrategy;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

import static com.bebopze.tdx.quant.common.constant.BuyStrategyEnum.涨停_SSF多_月多;
import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.btCompareDTO;
import static com.bebopze.tdx.quant.strategy.backtest.BacktestStrategy.btOpenBSDTO;
import static com.bebopze.tdx.quant.strategy.buy.BacktestBuyStrategyC.buySignalInfo;


/**
 * 回测 - B策略（高抛低吸  ->  C_MA_偏离率）
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BacktestBuyStrategy_TopStock extends AbstractBuyStrategy implements BuyStrategy {


    @Lazy
    @Autowired
    private BacktestSellStrategy backtestSellStrategy;

    @Autowired
    private BuyStrategy__ConCombiner__TopStock buyStrategy__ConCombiner__TopStock;


    @Override
    public String key() {
        return "TOP_STOCK";
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


        Set<String> buy__topStock__codeSet = Sets.newHashSet();
        data.getAllType_stockDOList(btCompareDTO.get().getBtStockType()).forEach(stockDO -> {

            String stockCode = stockDO.getCode();
            StockFun fun = data.getOrCreateStockFun(stockDO);


            KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
            ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
            Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


            // -------------------------------------------


            // 当日 - 停牌（003005  ->  2022-10-27）
            Integer idx = dateIndexMap.get(tradeDate);

            // 过滤 停牌/新股       // TODO 个股行情指标 异常数据bug   688692（达梦数据）     kline 301条   extData 300条（首日 2024-06-12 扩展数据 缺失）
            if (idx == null || Double.isNaN(extDataArrDTO.rps50[idx]) || fun.getKlineDTOList().size() != fun.getExtDataDTOList().size()) {
                return;
            }


            // ----------------------------------------- ztFlag 过滤策略 ------------------------------------------------


            // ztFlag 策略   ->   是否过滤 涨停（true/false/不过滤）
            boolean today_涨停 = extDataArrDTO.涨停[idx];
            if (ztFlag != null && !Objects.equals(ztFlag, today_涨停)) {
                return;
            }


            // ---------------------------------------------------------------------------------------------------------


            ExtDataDTO extDataDTO = fun.getExtDataDTOList().get(idx);


            // ---------------------------------------------------------------------------------------------------------


            // Map<String, Boolean> conMap = Maps.newHashMap();
            // boolean signal_B = conMap(klineArrDTO, extDataArrDTO, extDataDTO, fun, idx, conMap);


            Map<String, Boolean> conMap = BuyStrategy__ConCombiner__TopStock.convertExtDataDTO(extDataDTO);
            boolean signal_B = buyStrategy__ConCombiner__TopStock.anyMatch__buyStrategy(conMap);


            // ---------------------------------------------------------------------------------------------------------


            // 是否买入       =>       conList   ->   全为 true
            // boolean signal_B = BuyStrategy__ConCombiner.calcCon(buyConList, conMap);


            // ---------------------------------------------------------------------------------------------------------


            // B + 未涨停  ->  可买入（今日[close]  ->  直接买入）
            if (signal_B && !today_涨停) {
                buy__topStock__codeSet.add(stockCode);
                buySignalInfo(buy_infoMap, stockCode, data, idx, conMap);
            }


            // B + 涨停  ->  无法买入（最简化处理：[next_close] = [next_open]，次日开盘 直接买入）
            if (signal_B && today_涨停) {

                if (idx < fun.getMaxIdx()) {

                    LocalDate next_date = klineArrDTO.date[idx + 1];


                    // 次日S  ->  不能B（❌❌❌提前预知 -> 次日收盘价？？？❌❌❌   ->   应该是 今日）

                    // 今日B + 涨停     =>     今日S  ->  次日不能B
                    Set<String> todayDate__sellStockCodeSet = backtestSellStrategy.rule(btCompareDTO.get().getTopBlockStrategyEnum(), data, tradeDate, Sets.newHashSet(stockCode), Maps.newHashMap(), btCompareDTO.get());
                    boolean today_date_S = todayDate__sellStockCodeSet.contains(stockCode);
                    if (today_date_S) {
                        return;
                    }


                    // -------------------------------------------------------------------------------------------------


                    // 今日B + 涨停     =>     次日 -> 开盘B
                    btOpenBSDTO.get().today_date = tradeDate;
                    btOpenBSDTO.get().next_date = next_date;
                    btOpenBSDTO.get().open_B___stockCodeSet.add(stockCode);
                    btOpenBSDTO.get().open_B___buy_infoMap.put(stockCode, 涨停_SSF多_月多.getDesc());
                }
            }
        });


        return buy__topStock__codeSet;
    }


}