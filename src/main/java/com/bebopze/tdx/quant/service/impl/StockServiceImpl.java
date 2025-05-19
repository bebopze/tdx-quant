package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.constant.BlockNewTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.StockBlockInfoDTO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.service.StockService;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


/**
 * @author: bebopze
 * @date: 2025/5/18
 */
@Service
public class StockServiceImpl implements StockService {


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockService baseBlockService;

    @Autowired
    private IBaseBlockNewService baseBlockNewService;

    @Autowired
    private IBaseStockRelaBlockService baseStockRelaBlockService;

    @Autowired
    private IBaseStockRelaBlockNewService baseStockRelaBlockNewService;


    @Override
    public BaseStockDO info(String stockCode) {
        return baseStockService.getByCode(stockCode);
    }

    @Override
    public StockBlockInfoDTO blockInfo(String stockCode) {

        StockBlockInfoDTO dto = new StockBlockInfoDTO();
        dto.setStockCode(stockCode);


        // ------------------------------------------------------------------- 系统板块（行业、概念）


        List<BaseBlockDO> baseBlockDOList = baseStockRelaBlockService.listBlockByStockCode(stockCode);


        Map<Integer, StockBlockInfoDTO.BlockTypeDTO> type_dto_map = Maps.newHashMap();
        baseBlockDOList.forEach(e -> {


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


        List<BaseBlockNewDO> baseBlockNewDOList = baseStockRelaBlockNewService.listBlockByStockCode(stockCode, BlockNewTypeEnum.STOCK.getType());
        dto.setBaseBlockNewDOList(baseBlockNewDOList);

        return dto;
    }

}