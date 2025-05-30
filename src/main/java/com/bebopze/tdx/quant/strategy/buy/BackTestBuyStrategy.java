package com.bebopze.tdx.quant.strategy.buy;

import com.bebopze.tdx.quant.common.convert.ConvertDate;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.strategy.backtest.BackTestStrategy;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;


/**
 * 回测 - B策略
 *
 * @author: bebopze
 * @date: 2025/5/30
 */
@Slf4j
@Component
public class BackTestBuyStrategy extends BuyStrategy {


    public void initData(BackTestStrategy backTestStrategy) {

        this.dateIndexMap = backTestStrategy.getDateIndexMap();


        this.blockDOList = backTestStrategy.getBlockDOList();
        this.block__dateCloseMap = backTestStrategy.getBlock__dateCloseMap();


        this.stockDOList = backTestStrategy.getStockDOList();
        this.stock__dateCloseMap = backTestStrategy.getStock__dateCloseMap();
    }


    public List<String> rule(BackTestStrategy backTestStrategy, LocalDate tradeDate) {


        initData(backTestStrategy);


        // ------------------------- 板块


        List<String> filter__blockCodeList = Lists.newArrayList();
        for (BaseBlockDO baseBlockDO : blockDOList) {
            String blockCode = baseBlockDO.getCode();


            // 1、in__板块-月多


            // 2、in__板块-60日新高

            // 3、in__板块-RPS三线红


            // 4、in__板块占比-TOP1


            // 5、xxx


            BlockFun blockFun = new BlockFun(blockCode, baseBlockDO);


            boolean[] 月多_arr = blockFun.月多();

            boolean[] _60日新高_arr = blockFun.N日新高(60);
            boolean[] RPS三线红_arr = blockFun.RPS三线红(80);

            boolean[] 大均线多头_arr = blockFun.大均线多头();

            boolean[] SSF多_arr = blockFun.SSF多();


            boolean 月多 = getByDate(月多_arr, tradeDate);
            boolean _60日新高 = getByDate(_60日新高_arr, tradeDate);
            boolean RPS三线红 = getByDate(RPS三线红_arr, tradeDate);
            boolean 大均线多头 = getByDate(大均线多头_arr, tradeDate);
            boolean SSF多 = getByDate(SSF多_arr, tradeDate);


            boolean flag = 月多 && (_60日新高 || RPS三线红 || 大均线多头) && SSF多;
            if (flag) {
                filter__blockCodeList.add(blockCode);
            }
        }


        // ------------------------- 个股

        // 1、in__60日新高

        // 2、in__月多

        // 3、in__RPS三线红


        // 4、SSF多

        // 5、xxx


        return Collections.emptyList();
    }


    private boolean getByDate(boolean[] arr, LocalDate date) {

        boolean result = ConvertDate.getByDate(arr, date);


        int idx = dateIndexMap.get(DateTimeUtil.format_yyyy_MM_dd(date));
        boolean result2 = arr[idx];


        log.debug("getByDate     >>>     {}", result == result2);
        return result2;
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
