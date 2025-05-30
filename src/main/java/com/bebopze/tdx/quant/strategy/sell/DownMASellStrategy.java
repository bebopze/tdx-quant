package com.bebopze.tdx.quant.strategy.sell;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.constant.BlockNewTypeEnum;
import com.bebopze.tdx.quant.common.constant.BlockPoolEnum;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.indicator.StockFunLast;
import com.bebopze.tdx.quant.strategy.QuickOption;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.BlockPoolEnum.*;
import static com.bebopze.tdx.quant.common.constant.StockPoolEnum.*;


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
    private IBaseBlockService baseBlockService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;

    @Autowired
    private IBaseBlockNewRelaStockService baseBlockNewRelaStockService;


    /**
     * 选股公式 - 板块池子
     */
    List<String> block_list = Lists.newArrayList("60RXG", "RPSSXFH", "KDZD", "YD", "ZQCZ");


    /**
     * S策略
     *
     * @param stockCode
     */
    public void sellStockRule(String stockCode) {


        // 1、获取 持仓列表


        // 2、持仓个股   =>   逐一扫描 S策略   ->   触发 -> S个股


        // -------------------------------------------------------------------------------------------------------------


        // 个股量化
        boolean rule1 = rule_stockPool(stockCode);

        if (!rule1) {
            try {
                QuickOption.一键卖出(stockCode);
            } catch (Exception e) {
            }
        }


        // 板块量化
        boolean rule2 = rule_blockPool(stockCode);

        if (!rule2) {
            // 减仓
            try {
                QuickOption.等比卖出(stockCode);
            } catch (Exception e) {
            }
        }


        // 大盘量化
        boolean rule3 = true;//rule_market(stockCode);

        if (!rule3) {
            try {
                QuickOption.一键清仓(null);
            } catch (Exception e) {
            }
        }


        System.out.println("-----------------");
    }


    public void holdingStockRule(List<String> stockCodeList) {
        for (String stockCode : stockCodeList) {
            boolean b = holdingStockRule(stockCode);
        }
    }


    /**
     * 持股 策略
     *
     * @param stockCode
     */
    public boolean holdingStockRule(String stockCode) {


        // 个股量化
        boolean rule1 = rule_stockPool(stockCode);

        if (!rule1) {
            try {
                QuickOption.一键卖出(stockCode);
            } catch (Exception e) {
            }
        }


        // 板块量化     ->     控制 个股（IN-主线板块）
        boolean rule2 = rule_blockPool(stockCode);

        if (!rule2) {
            // 减仓
            try {
                QuickOption.等比卖出(stockCode);
            } catch (Exception e) {
            }
        }


        // 大盘量化     ->     控制 总仓位
        boolean rule3 = true;//rule_market(stockCode);

        if (!rule3) {
            try {
                QuickOption.一键清仓(null);
            } catch (Exception e) {
            }
        }


        System.out.println("-----------------");


        return rule1 && rule2 && rule3;
    }

    private boolean rule_blockPool(String stockCode) {


        // 个股 - 系统板块
        List<BaseBlockDO> baseBlockDOList = baseBlockRelaStockService.listBlockByStockCode(stockCode);
        List<String> stock__blockCodeList = baseBlockDOList.stream().map(BaseBlockDO::getCode).collect(Collectors.toList());

        Map<String, String> stock__block_codeNameMap = baseBlockDOList.stream().collect(Collectors.toMap(BaseBlockDO::getCode, BaseBlockDO::getName));


        // -------------------------------------------------------------------------------------------------------------


        // 基础 - 板块池子（自定义板块）
        List<BlockPoolEnum> blockNewPoolEnums = Lists.newArrayList(
                // 板块-月多   /   板块-T0
                BK_YD, BK_T0,
                // 板块-二阶段   /   板块-三线红
                BK_EJD, BK_SXH,
                // 板块-60日新高   /   板块-口袋支点
                BK_60RXG, BK_KDZD,

                // 板块-强势卖出
                BK_QSMC,
                // 板块-主线
                BK_ZX);

        List<String> blockNewPoolCodeList = blockNewPoolEnums.stream().map(BlockPoolEnum::getBlockNewCode).collect(Collectors.toList());


        // 板块池 -> 板块列表
        List<BaseBlockDO> blockNewPool__blockDOList = baseBlockNewRelaStockService.listBlockByBlockNewCodeList(blockNewPoolCodeList);
        List<String> blockNewPool__blockCodeList = blockNewPool__blockDOList.stream().map(BaseBlockDO::getCode).collect(Collectors.toList());

        Map<String, String> blockNewPool__block_codeNameMap = blockNewPool__blockDOList.stream().collect(Collectors.toMap(BaseBlockDO::getCode, BaseBlockDO::getName));


        // 持仓个股 - 所属板块   ->   必须 满足以下任一


        // List<String> BK_YD__blockList = baseBlockNewRelaStockService.listBlockByBlockNewCodeList(Lists.newArrayList(BK_YD.getBlockNewCode())).stream().map(BaseBlockDO::getCode).collect(Collectors.toList());
        // boolean in_板块月多 = Lists.newArrayList(stock__blockCodeList).retainAll(BK_YD__blockList);


        // 交集
        List<String> in_block = Lists.newArrayList(CollectionUtils.intersection(stock__blockCodeList, blockNewPool__blockCodeList));
        System.out.println("stock__block " + JSON.toJSONString(stock__block_codeNameMap));
        System.out.println("blockNewPool__block " + JSON.toJSONString(blockNewPool__block_codeNameMap));

        boolean flag = in_block.size() >= 3;


        return flag;
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
