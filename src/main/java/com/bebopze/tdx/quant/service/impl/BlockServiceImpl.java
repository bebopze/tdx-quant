package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.common.domain.dto.BlockDTO;
import com.bebopze.tdx.quant.common.domain.dto.StockBlockInfoDTO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockRelaStockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockRelaStockService;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.service.BlockService;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.service.StockService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;


/**
 * @author: bebopze
 * @date: 2025/6/8
 */
@Slf4j
@Service
public class BlockServiceImpl implements BlockService {


    BacktestCache data = new BacktestCache();


    @Autowired
    private IBaseBlockService baseBlockService;

    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;

    @Autowired
    private StockService stockService;


    @Autowired
    private InitDataService initDataService;


    @Override
    public BlockDTO info(String blockCode) {
        BaseBlockDO entity = baseBlockService.getByCode(blockCode);


        BlockDTO dto = new BlockDTO();
        if (entity != null) {
            dto.setKlineHis(entity.getKlineHis());
            BeanUtils.copyProperties(entity, dto);


            // List<KlineDTO> klineDTOList = ConvertStockKline.str2DTOList(entity.getKlineHis(), 100);
            //
            // Map<String, Object> klineMap = new HashMap<>();
            // klineMap.put("date", ConvertStockKline.strFieldValArr(klineDTOList, "date"));
            // klineMap.put("close", ConvertStockKline.fieldValArr(klineDTOList, "close"));
            //
            // dto.setKlineMap(klineMap);
        }


        return dto;
    }


    @Override
    public Object listStock(String blockCode) {
        return null;
    }


    @Override
    public Object _100DayHigh(LocalDate date) {


        // 数据初始化   ->   加载 全量行情数据
        data = initDataService.initData(null, null, false);


        // 全量  =>  百日新高-个股
        List<BaseStockDO> stockList = Lists.newArrayList();


        // 个股 - 板块
        for (BaseStockDO stockDO : stockList) {

            String code = stockDO.getCode();

            //
            StockBlockInfoDTO stockBlockInfoDTO = stockService.blockInfo(code);
        }


        return null;
    }


    public StockBlockInfoDTO blockInfo() {

        StockBlockInfoDTO dto = new StockBlockInfoDTO();


        // ------------------------------------------------------------------- 系统板块（行业、概念）


        // 个股 - 研究行业
        Map<String, Set<String>> stockCode_yjhyBlockCodeSet_map = Maps.newHashMap();
        // 个股 - 细分行业
        Map<String, Set<String>> stockCode_xfhyBlockCodeSet_map = Maps.newHashMap();

        // 个股 - 概念
        Map<String, Set<String>> stockCode_gnBlockCodeSet_map = Maps.newHashMap();


        // 板块 - 个股
        Map<String, Set<String>> blockCode_stockCodeSet_Map = Maps.newHashMap();
        // 个股 - 板块
        Map<String, Set<String>> stockCode_blockCodeSet_map = Maps.newHashMap();


        List<BaseBlockRelaStockDO> relaStockDOList = baseBlockRelaStockService.listAll();
        relaStockDOList.forEach(relaEntity -> {

            Long blockId = relaEntity.getBlockId();
            Long stockId = relaEntity.getStockId();


            String blockCode = data.block__idCodeMap.get(blockId);
            String stockCode = data.stock__idCodeMap.get(stockId);


            BaseBlockDO blockDO = data.codeBlockMap.get(blockCode);


            // tdx板块类型：1-暂无（保留）；2-普通行业-二级分类/细分行业；3-地区板块；4-概念板块；5-风格板块；12-研究行业-一级/二级/三级分类；
            Integer type = blockDO.getType();

            if (Objects.equals(type, 4)) {

                stockCode_gnBlockCodeSet_map.computeIfAbsent(stockCode, k -> Sets.newHashSet()).add(blockCode);

            } else if (Objects.equals(type, 2)) {

                stockCode_xfhyBlockCodeSet_map.computeIfAbsent(stockCode, k -> Sets.newHashSet()).add(blockCode);

            } else if (Objects.equals(type, 12)) {

                stockCode_yjhyBlockCodeSet_map.computeIfAbsent(stockCode, k -> Sets.newHashSet()).add(blockCode);
            }


        });


        // ------------------------------------------------------------------- 系统板块（行业、概念）


        Map<Integer, StockBlockInfoDTO.BlockTypeDTO> type_dto_map = Maps.newHashMap();
        data.blockDOList.forEach(e -> {


            // ---------------------------------- block type
            Integer type = e.getType();

            StockBlockInfoDTO.BlockTypeDTO blockTypeDTO = new StockBlockInfoDTO.BlockTypeDTO();
            blockTypeDTO.setBlockType(type);


            // ---------------------------------- block
            Integer level = e.getLevel();

            String blockCode = e.getCode();
            String blockName = e.getName();


            if (level == 1) {

            } else if (level == 2) {

                BaseBlockDO level_1 = baseBlockService.getById(e.getParentId());
                blockCode = level_1.getCode() + "-" + e.getCode();
                blockName = level_1.getName() + "-" + e.getName();

            } else if (level == 3) {

                BaseBlockDO level_2 = baseBlockService.getById(e.getParentId());
                BaseBlockDO level_1 = baseBlockService.getById(level_2.getParentId());

                blockCode = level_1.getCode() + "-" + level_2.getCode() + "-" + e.getCode();
                blockName = level_1.getName() + "-" + level_2.getName() + "-" + e.getName();
            }

            StockBlockInfoDTO.BlockDTO blockDTO = new StockBlockInfoDTO.BlockDTO();
            blockDTO.setLevel(level);
            blockDTO.setBlockCode(blockCode);
            blockDTO.setBlockName(blockName);


            // ---------------------------------- map

            if (type_dto_map.containsKey(type)) {
                type_dto_map.get(type).getBlockDTOList().add(blockDTO);
            } else {
                blockTypeDTO.setBlockDTOList(Lists.newArrayList(blockDTO));
                type_dto_map.put(type, blockTypeDTO);
            }
        });

        dto.setBlockDTOList(Lists.newArrayList(type_dto_map.values()));


        // ------------------------------------------------------------------- 自定义板块


        // List<BaseBlockNewDO> baseBlockNewDOList = baseBlockNewRelaStockService.listByStockCode(stockCode, BlockNewTypeEnum.STOCK.getType());
        // dto.setBaseBlockNewDOList(baseBlockNewDOList);

        return dto;
    }


}