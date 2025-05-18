package com.bebopze.tdx.quant.strategy.sell;

import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import com.bebopze.tdx.quant.dal.service.IBaseStockRelaBlockNewService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.indicator.Fun1;
import com.bebopze.tdx.quant.strategy.QuickOption;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.StockPoolBlockEnum.*;


/**
 * 卖出策略 -> 破位          等级：强制          ->          最后 持仓底线 - 破↓
 *
 * @author: bebopze
 * @date: 2025/5/13
 */
@Slf4j
@Component
public class DownMASellStrategy {


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseStockRelaBlockNewService baseStockRelaBlockNewService;


    /**
     * 选股公式 - 板块池子
     */
    List<String> block_list = Lists.newArrayList("60RXG", "RPSSXFH", "KDZD", "YD", "ZQCZ");


    public void chiGuRule() {

        String stockCode = "000559";


        // 个股 - 自定义板块
        List<BaseBlockNewDO> baseBlockNewDOList = baseStockRelaBlockNewService.listBlockByStockCode(stockCode);
        List<String> stockBlockNewCodeList = baseBlockNewDOList.stream().map(BaseBlockNewDO::getCode).collect(Collectors.toList());


        // 持仓   ->   必须 满足以下任一


        // 1、in   60日新高
        boolean in_60日新高 = stockBlockNewCodeList.contains(_60日新高.getBlockNewCode());

        // 2、in   RPS三线翻红
        boolean in_RPS三线翻红 = stockBlockNewCodeList.contains(RPS三线翻红.getBlockNewCode());

        // 3、in   口袋支点
        boolean in_口袋支点 = stockBlockNewCodeList.contains(口袋支点.getBlockNewCode());

        // 4、in   月多
        boolean in_月多 = stockBlockNewCodeList.contains(月多.getBlockNewCode());

        // 5、in   大均线多头
        boolean in_大均线多头 = stockBlockNewCodeList.contains(大均线多头.getBlockNewCode());

        // 6、in   中期池子
        boolean in_中期池子 = stockBlockNewCodeList.contains(中期池子.getBlockNewCode());


        boolean flag = in_60日新高 || in_RPS三线翻红 || in_口袋支点 || in_月多 || in_大均线多头 || in_中期池子;
        if (!flag) {
            QuickOption.一键卖出(stockCode);
        }

    }


    public static void main(String[] args) {


        // String stockCode = "300059";

        // 纳指ETF
        String stockCode = "159941";


        Fun1 fun = new Fun1(stockCode);


        // --------------------------------------- 个股


        // 1、下MA50
        boolean 下MA50 = fun.下MA(50);

        // 2、MA空(20)
        boolean MA20_空 = fun.MA空(20);

        // 3、SSF空
        boolean SSF空 = fun.SSF空();


        // 4、月空


        // 5、高位 - 上影大阴


        // 10、RPS三线 < 85
        // boolean RPS三线红_NOT = true;


        // --------------------------------------- 板块


        // 1、


        // --------------------------------------- 大盘


        // 1、


        boolean sell = 下MA50 || MA20_空 || SSF空/*|| RPS三线红_NOT*/;


        if (sell) {
            QuickOption.一键卖出(fun);
        }
    }


}
