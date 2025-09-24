package com.bebopze.tdx.quant.common.cache;

import com.bebopze.tdx.quant.common.domain.dto.topblock.TopBlockDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopStockDTO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockRelaStockService;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * 主线板块/主线个股（板块-月多2）  -   Cache
 *
 * @author: bebopze
 * @date: 2025/9/25
 */
@Slf4j
@Component
public class TopBlockCache {


    public static final BacktestCache data = new BacktestCache();


    // -----------------------------------------------------------------------------------------------------------------


    @Autowired
    private IBaseBlockService baseBlockService;

    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;


    public BaseBlockDO getBlock(String blockCode) {
        return data.codeBlockMap.computeIfAbsent(blockCode, k -> baseBlockService.getByCode(k));
    }


    public String getBlockName(String blockCode) {
        BaseBlockDO blockDO = getBlock(blockCode);
        return blockDO == null ? null : blockDO.getName();
    }


    public List<TopBlockDTO.TopStock> getTopStockList(String blockCode,
                                                      Set<String> allTopStockCodeSet,
                                                      Map<String, Integer> topStock__codeCountMap) {


        // 板块 - 个股列表
        Set<String> block_stockCodeSet = data.blockCode_stockCodeSet_Map.computeIfAbsent(blockCode, k -> {

            List<BaseStockDO> block__stockDOList = baseBlockRelaStockService.listStockByBlockCodeList(Lists.newArrayList(blockCode));


            block__stockDOList.forEach(simpleStockDO -> {
                data.codeStockMap.computeIfAbsent(simpleStockDO.getCode(), x -> simpleStockDO);
            });


            return block__stockDOList.stream().map(BaseStockDO::getCode).collect(Collectors.toSet());
        });


        // -------------------------------------------------------------------------------------------------------------


        List<TopBlockDTO.TopStock> topStockList = Lists.newArrayList();


        // 主线个股
        allTopStockCodeSet.forEach(topStockCode -> {

            // 板块个股  ->  IN 主线个股
            if (block_stockCodeSet.contains(topStockCode)) {


                TopBlockDTO.TopStock topStock = new TopBlockDTO.TopStock();
                topStock.setStockCode(topStockCode);
                topStock.setStockName(data.codeStockMap.get(topStockCode).getName());
                topStock.setTopDays(topStock__codeCountMap.get(topStockCode));


                topStockList.add(topStock);
            }
        });


        return topStockList.stream()
                           .sorted(Comparator.comparing(TopBlockDTO.TopStock::getTopDays).reversed())
                           .collect(Collectors.toList());
    }


    // -----------------------------------------------------------------------------------------------------------------


    public BaseStockDO getStock(String stockCode) {
        return data.codeStockMap.computeIfAbsent(stockCode, k -> baseStockService.getSimpleByCode(k));
    }


    public String getStockName(String stockCode) {
        BaseStockDO stockDO = getStock(stockCode);
        return stockDO == null ? null : stockDO.getName();
    }


    public List<TopStockDTO.TopBlock> getTopBlockList(String stockCode,
                                                      Set<String> allTopBlockCodeSet,
                                                      Map<String, Integer> topBlock__codeCountMap) {


        // 个股 - 板块列表
        Set<String> stock_blockCodeSet = data.stockCode_blockCodeSet_Map.computeIfAbsent(stockCode, k -> {

            List<BaseBlockDO> stock__blockDOList = baseBlockRelaStockService.listBlockByStockCodeList(Lists.newArrayList(stockCode));


            stock__blockDOList.forEach(simpleBlockDO -> {
                data.codeBlockMap.computeIfAbsent(simpleBlockDO.getCode(), x -> simpleBlockDO);
            });


            return stock__blockDOList.stream().map(BaseBlockDO::getCode).collect(Collectors.toSet());
        });


        // -------------------------------------------------------------------------------------------------------------


        List<TopStockDTO.TopBlock> topBlockList = Lists.newArrayList();


        // 主线板块
        allTopBlockCodeSet.forEach(topBlockCode -> {

            // 个股板块  ->  IN 主线板块
            if (stock_blockCodeSet.contains(topBlockCode)) {


                TopStockDTO.TopBlock topBlock = new TopStockDTO.TopBlock();
                topBlock.setBlockCode(topBlockCode);
                topBlock.setBlockName(data.codeBlockMap.get(topBlockCode).getName());
                topBlock.setTopDays(topBlock__codeCountMap.get(topBlockCode));


                topBlockList.add(topBlock);
            }
        });


        return topBlockList.stream()
                           .sorted(Comparator.comparing(TopStockDTO.TopBlock::getTopDays).reversed())
                           .collect(Collectors.toList());
    }


}