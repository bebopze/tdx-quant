package com.bebopze.tdx.quant.strategy.buy;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.constant.BlockNewTypeEnum;
import com.bebopze.tdx.quant.common.constant.BlockPoolEnum;
import com.bebopze.tdx.quant.common.constant.StockPoolEnum;
import com.bebopze.tdx.quant.common.domain.dto.backtest.BuyStockStrategyResultDTO;
import com.bebopze.tdx.quant.common.util.SleepUtils;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockNewRelaStockService;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseBlockRelaStockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.indicator.BlockFun;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.bebopze.tdx.quant.indicator.StockFunLast;
import com.bebopze.tdx.quant.strategy.QuickOption;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.BlockPoolEnum.*;
import static com.bebopze.tdx.quant.common.constant.BlockPoolEnum.BK_ZX;
import static com.bebopze.tdx.quant.common.constant.StockPoolEnum.*;
import static com.bebopze.tdx.quant.common.constant.StockPoolEnum.中期池子;


/**
 * 买入策略     ->     主：   60日新高 + RPS三线红 + SSF多（主）
 *
 * -           ->     辅：   月多 + 大均线多头 + 成交额
 *
 * @author: bebopze
 * @date: 2025/5/21
 */
@Slf4j
@Component
public class BuyStockStrategy {


    /**
     * 买入策略 - result记录
     */
    private BuyStockStrategyResultDTO dto = new BuyStockStrategyResultDTO();


    // -----------------------------------------------------------------------------------------------------------------


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockService baseBlockService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;

    @Autowired
    private IBaseBlockNewRelaStockService baseBlockNewRelaStockService;


    public BuyStockStrategyResultDTO buyStockRule() {


        // 策略1
        holdingStockRule();


        // 策略2
        // ...


        // 策略3
        // ...


        return dto;
    }


    /**
     * 持股 策略   ->   自动买入
     *
     * @param
     */
    public void holdingStockRule() {


        // -------------------------------------------------------------------------------------------------------------
        //                                              板块池
        // -------------------------------------------------------------------------------------------------------------


        // 板块池子   ->   板块code 列表
        List<String> filterBlockCodeList = filterBlockPool();


        // -------------------------------------------------------------------------------------------------------------


        // 板块池子 -> 个股Entity 列表
        List<BaseStockDO> blockPool__stockDOList = baseBlockRelaStockService.listStockByBlockCodeList(filterBlockCodeList);


        // 板块池子 -> 个股code 列表
        List<String> blockPool__stockCodeList = blockPool__stockDOList.stream().map(BaseStockDO::getCode).collect(Collectors.toList());
        // 板块池子 -> 个股 - code_name map
        Map<String, String> blockPool__stock_codeNameMap = blockPool__stockDOList.stream().collect(Collectors.toMap(BaseStockDO::getCode, BaseStockDO::getName));


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股池
        // -------------------------------------------------------------------------------------------------------------


        // 股票池子 - 个股列表
        Map<StockPoolEnum, List<BaseStockDO>> stockPool__stockDOList__map = Maps.newHashMap();
        Map<StockPoolEnum, List<String>> stockPool__stockCodeList__map = Maps.newHashMap();


        List<BaseStockDO> stockPool__stockDOList = loadStockPool(stockPool__stockDOList__map, stockPool__stockCodeList__map);


        // 个股池子 -> 个股code 列表
        List<String> stockPool__stockCodeList = stockPool__stockDOList.stream().map(BaseStockDO::getCode).collect(Collectors.toList());
        // 个股池子 -> 个股 - code_name map
        Map<String, String> stockPool__stock_codeNameMap = stockPool__stockDOList.stream().collect(Collectors.toMap(BaseStockDO::getCode, BaseStockDO::getName));


        // -------------------------------------------------------------------------------------------------------------


        // 个股池子 -> 板块Entity 列表
        // List<BaseBlockDO> stockPool__blockDOList = baseBlockRelaStockService.listBlockByStockCodeList(stockPool__stockCodeList);

        // 个股 - 所属板块 -> 板块code 列表
        // List<String> stockPool__blockCodeList = stockPool__blockDOList.stream().map(BaseBlockDO::getCode).collect(Collectors.toList());
        // 板块池子 -> 个股 - code_name map
        // Map<String, String> stockPool__block_codeNameMap = stockPool__blockDOList.stream().collect(Collectors.toMap(BaseBlockDO::getCode, BaseBlockDO::getName));

        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------
        //                                              个股筛选
        // -------------------------------------------------------------------------------------------------------------


        // 买入个股 条件（门槛）：
        //
        // 1、个股           ->   in_个股池子
        // 2、个股-所属板块   ->   in_板块池子


        // Collection<String> same__blockCodeList = CollectionUtils.intersection(stockPool__blockCodeList, blockPool__blockCodeList);


        Collection<String> same__stockCodeList = CollectionUtils.intersection(blockPool__stockCodeList, stockPool__stockCodeList);


        Map<String, String> sameStockPool__codeNameMap = Maps.newTreeMap();
        stockPool__stock_codeNameMap.forEach((stockCode, stockName) -> {

            if (same__stockCodeList.contains(stockCode)) {
                sameStockPool__codeNameMap.put(stockCode, stockName);
            }
        });

        log.info("初次筛选     >>>     size : {} , same__stockList : {}",
                 same__stockCodeList.size(), JSON.toJSONString(sameStockPool__codeNameMap));


        // -------------------------------------------------------------------------------------------------------------


        // 二次筛选：


        // 1、RPS
        // 2、均线
        // 3、月多
        // 4、SSF
        // 5、新高
        // 6、成交额
        // 7、


        List<String> filterStockCodeList = Lists.newArrayList();
        Map<String, String> filterStockPool__codeNameMap = Maps.newTreeMap();


        same__stockCodeList.forEach(stockCode -> {


            // boolean[] RPS红 = stockFun.RPS红();

            // boolean[] 大均线多头 = stockFun.大均线多头();
            // boolean[] 月多 = stockFun.月多();


            // boolean[] N日新高 = stockFun.N日新高(60);


            boolean in_月多 = stockPool__stockCodeList__map.get(月多).contains(stockCode);
            boolean in_60日新高 = stockPool__stockCodeList__map.get(_60日新高).contains(stockCode);

            boolean in_大均线多头 = stockPool__stockCodeList__map.get(大均线多头).contains(stockCode);
            boolean in_RPS三线翻红 = stockPool__stockCodeList__map.get(RPS三线翻红).contains(stockCode);


            boolean flag = in_月多 && in_60日新高 && (in_大均线多头 || in_RPS三线翻红) /*&& SSF多_last*/;


            if (flag) {
                StockFun stockFun = new StockFun(stockCode);

                boolean[] SSF多 = stockFun.SSF多();
                boolean SSF多_last = stockFun.last(SSF多);


                if (flag && SSF多_last) {
                    filterStockCodeList.add(stockCode);
                    filterStockPool__codeNameMap.put(stockCode, sameStockPool__codeNameMap.get(stockCode));
                }
            }


            SleepUtils.sleep(50);
        });


        log.info("二次筛选     >>>     size : {} , filter__stockList : {}",
                 filterStockCodeList.size(), JSON.toJSONString(filterStockPool__codeNameMap));


        // -------------------------------------------------------------------------------------------------------------


        // 三次筛选（妖股）：

        // 1、市场行情（舆情分析）
        // 2、热股抓取
        // 3、热股抓取


        // -------------------------------------------------------------------------------------------------------------


        // 等比 买入前50只
        buyStocks(filterStockCodeList, filterStockPool__codeNameMap);
    }

    private List<BaseStockDO> loadStockPool(Map<StockPoolEnum, List<BaseStockDO>> stockPool__stockDOList__map,
                                            Map<StockPoolEnum, List<String>> stockPool__stockCodeList__map) {


        // 基础 - 板块池子（自定义板块）
        List<StockPoolEnum> stockPoolEnums = Lists.newArrayList(
                // 60日新高   /   RPS三线翻红   /   月多
                _60日新高, RPS三线翻红, 月多,

                // 口袋支点
                口袋支点,

                // 大均线多头
                大均线多头,

                // 中期池子
                中期池子);


        for (StockPoolEnum stockPoolEnum : stockPoolEnums) {
            List<BaseStockDO> stockPool__stockDOList = baseBlockNewRelaStockService.listStockByBlockNewCodeList(Lists.newArrayList(stockPoolEnum.getBlockNewCode()));
            List<String> stockPoolEnum__stockCodeList = stockPool__stockDOList.stream().map(BaseStockDO::getCode).collect(Collectors.toList());

            stockPool__stockDOList__map.put(stockPoolEnum, stockPool__stockDOList);
            stockPool__stockCodeList__map.put(stockPoolEnum, stockPoolEnum__stockCodeList);
        }


        // 个股池子 -> 个股Entity 列表
        List<BaseStockDO> stockPool__stockDOList = stockPool__stockDOList__map.values().stream().flatMap(List::stream)
                .collect(Collectors.toMap(
                        BaseStockDO::getCode,
                        stock -> stock,
                        (existing, replacement) -> existing
                )).
                values().stream().collect(Collectors.toList());


        return stockPool__stockDOList;

    }


    /**
     * 板块池子  -  筛选
     *
     * @return
     */
    private List<String> filterBlockPool() {

        // 板块池子 - 板块列表

        // 基础 - 板块池子（自定义板块）
        List<BlockPoolEnum> blockPoolEnums = Lists.newArrayList(

                // ----------------- 买B（买进区域）


                // 板块-月多   /   板块-T0
                BK_YD, BK_T0,

                // 板块-二阶段   /   板块-三线红
                BK_EJD, BK_SXH,

                // 板块-60日新高   /   板块-口袋支点
                BK_60RXG, BK_KDZD,

                // 板块-主线
                BK_ZX,


                // ----------------- 卖S（淘汰区域）


                // 板块-右侧卖
                BK_YCS);


        // -------------------------------------------------------------------------------------------------------------

        Map<BlockPoolEnum, List<BaseBlockDO>> blockPool__blockDOList__map = Maps.newHashMap();
        Map<BlockPoolEnum, List<String>> blockPool__blockCodeList__map = Maps.newHashMap();


        for (BlockPoolEnum blockPoolEnum : blockPoolEnums) {
            List<BaseBlockDO> blockPool__blockDOList = baseBlockNewRelaStockService.listBlockByBlockNewCodeList(Lists.newArrayList(blockPoolEnum.getBlockNewCode()));
            List<String> blockPool__blockCodeList = blockPool__blockDOList.stream().map(BaseBlockDO::getCode).collect(Collectors.toList());

            blockPool__blockDOList__map.put(blockPoolEnum, blockPool__blockDOList);
            blockPool__blockCodeList__map.put(blockPoolEnum, blockPool__blockCodeList);
        }


        // 板块池子 -> 板块Entity 列表
        List<BaseBlockDO> blockPool__blockDOList = blockPool__blockDOList__map.values().stream().flatMap(List::stream)
                .collect(Collectors.toMap(
                        BaseBlockDO::getCode,
                        stock -> stock,
                        (existing, replacement) -> existing
                )).
                values().stream().collect(Collectors.toList());


        // 板块池子 -> 板块code 列表
        List<String> blockPool__blockCodeList = blockPool__blockDOList.stream().map(BaseBlockDO::getCode).collect(Collectors.toList());
        // 板块池子 -> 板块 - code_name map
        Map<String, String> blockPool__block_codeNameMap = blockPool__blockDOList.stream().collect(Collectors.toMap(BaseBlockDO::getCode, BaseBlockDO::getName));


        // -------------------------------------------------------------------------------------------------------------


        // 板块筛选
        List<String> filterBlockCodeList = Lists.newArrayList();
        Map<String, String> filterBlockPool__codeNameMap = Maps.newTreeMap();


        // 1、月多
        // 2、SSF多
        // 3、NOT(右侧卖)   AND   NOT(左侧卖)


        blockPool__blockCodeList.forEach(blockCode -> {


            // boolean[] RPS红 = stockFun.RPS红();

            // boolean[] 大均线多头 = stockFun.大均线多头();
            // boolean[] 月多 = stockFun.月多();


            // boolean[] N日新高 = stockFun.N日新高(60);


            boolean in_板块_月多 = blockPool__blockCodeList__map.get(BK_YD).contains(blockCode);
            boolean in_板块_T0 = blockPool__blockCodeList__map.get(BK_T0).contains(blockCode);

            boolean in_板块_二阶段 = blockPool__blockCodeList__map.get(BK_EJD).contains(blockCode);
            boolean in_板块_三线红 = blockPool__blockCodeList__map.get(BK_SXH).contains(blockCode);

            boolean in_板块_60日新高 = blockPool__blockCodeList__map.get(BK_60RXG).contains(blockCode);
            boolean in_板块_口袋支点 = blockPool__blockCodeList__map.get(BK_KDZD).contains(blockCode);
            boolean in_板块_主线 = blockPool__blockCodeList__map.get(BK_ZX).contains(blockCode);


            boolean not_in_板块_右侧卖 = !blockPool__blockCodeList__map.get(BK_YCS).contains(blockCode);


            boolean flag = in_板块_月多 && (in_板块_T0 || in_板块_二阶段 || in_板块_三线红 || in_板块_60日新高 || in_板块_口袋支点 || in_板块_主线)
                    && not_in_板块_右侧卖 /*&& SSF多_last*/;


            if (flag) {


                BaseBlockDO baseBlockDO = baseBlockService.getByCode(blockCode);


                BlockFun blockFun = new BlockFun(blockCode, baseBlockDO);


                boolean[] SSF多 = blockFun.SSF多();
                boolean SSF多_last = blockFun.last(SSF多);


                if (flag && SSF多_last) {
                    filterBlockCodeList.add(blockCode);
                    filterBlockPool__codeNameMap.put(blockCode, blockPool__block_codeNameMap.get(blockCode));
                }
            }


        });


        // 板块淘汰


        // 1、下SSF
        // 2、右侧卖


        log.info("板块筛选     >>>     size : {} , filterBlockCodeList : {}",
                 filterBlockCodeList.size(), JSON.toJSONString(filterBlockPool__codeNameMap));


        return filterBlockCodeList;
    }

    private void buyStocks(List<String> filterStockCodeList, Map<String, String> filterStockPool__codeNameMap) {
        // 等份 买入前50只
        QuickOption.等比买入(filterStockCodeList, 50);
    }


    private boolean rule_stockPool(String stockCode) {

        // 个股 - 自定义板块（选股池子）
        List<BaseBlockNewDO> baseBlockNewDOList = baseBlockNewRelaStockService.listByStockCode(stockCode, BlockNewTypeEnum.STOCK.getType());
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


        return in_60日新高 || in_RPS三线翻红 || in_口袋支点 || in_月多 || in_大均线多头 || in_中期池子;
    }


    public static void main(String[] args) {


        // String stockCode = "300059";

        // 纳指ETF
        String stockCode = "159941";


        StockFunLast fun = new StockFunLast(stockCode);


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
