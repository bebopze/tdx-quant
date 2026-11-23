package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.base.BaseStockDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.StockBlockInfoDTO;
import com.bebopze.tdx.quant.common.domain.tq.SimpleStockDTO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseOptionDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.service.StockService;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;


/**
 * 股票 info
 *
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
    private IBaseOptionService baseOptionService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;

    @Autowired
    private IBaseBlockNewRelaStockService baseBlockNewRelaStockService;


    @Override
    public List<SimpleStockDTO> search(String keyword, int limit) {
        keyword = StringUtils.isBlank(keyword) ? "" : keyword.trim();
        // limit ∈ [1,50]
        limit = Math.max(1, Math.min(limit, 50));


        List<BaseStockDO> stockDOList = baseStockService.listSimpleByKeyword(keyword);
        List<BaseBlockDO> blockDOList = baseBlockService.listSimpleByKeyword(keyword);
        List<BaseOptionDO> optionDOList = baseOptionService.listSimpleByKeyword(keyword);


        List<SimpleStockDTO> dtoList = Lists.newArrayList();


        stockDOList.forEach(e -> {
            SimpleStockDTO dto = new SimpleStockDTO();
            BeanUtils.copyProperties(e, dto);
            dtoList.add(dto);
        });

        blockDOList.forEach(e -> {
            SimpleStockDTO dto = new SimpleStockDTO();
            BeanUtils.copyProperties(e, dto);
            dtoList.add(dto);
        });

        optionDOList.forEach(e -> {
            SimpleStockDTO dto = new SimpleStockDTO();
            BeanUtils.copyProperties(e, dto);
            dtoList.add(dto);
        });


        // limit
        return dtoList.stream().limit(limit).toList();
    }


    @Override
    public BaseStockDTO info(String stockCode, Integer updateType) {


        // 默认使用 ALL_RANGE（读取全部 nd + his）
        updateType = updateType == null ? UpdateTypeEnum.ALL_RANGE.type : updateType;


        // ---------------------------------


        BaseStockDO entity = new BaseStockDO();


        if (StockTypeEnum.isStock(stockCode)) {

            entity = baseStockService.getByCodeAndUpdateType(stockCode, updateType);


        } else if (StockTypeEnum.isBlock(stockCode)) {


            BaseBlockDO blockDO = baseBlockService.getByCodeAndUpdateType(stockCode, updateType);
            if (blockDO != null) {
                BeanUtils.copyProperties(blockDO, entity);
            }


        } else if (StockTypeEnum.isOption(stockCode)) {


            BaseOptionDO optionDO = baseOptionService.getByCode(stockCode);
            if (optionDO != null) {
                BeanUtils.copyProperties(optionDO, entity);
            }
        }


        BaseStockDTO dto = new BaseStockDTO();
        if (entity != null) {

            // copy
            BeanUtils.copyProperties(entity, dto);

            // 股票类型
            dto.setStockType(StockTypeEnum.getTypeByStockCode(stockCode));
        }


        return dto;
    }


    @Override
    public StockBlockInfoDTO blockInfo(String stockCode) {


        StockBlockInfoDTO dto = new StockBlockInfoDTO();
        dto.setStockCode(stockCode);
        dto.setStockName(Optional.ofNullable(baseStockService.getSimpleByCode(stockCode)).map(BaseStockDO::getName).orElse(""));


        // ------------------------------------------------------------------- 系统板块（行业、概念）


        List<BaseBlockDO> baseBlockDOList = baseBlockRelaStockService.listBlockByStockCode(stockCode);


        List<StockBlockInfoDTO.BlockDTO> hyBlockDTOList = Lists.newArrayList();
        List<StockBlockInfoDTO.BlockDTO> gnBlockDTOList = Lists.newArrayList();


        baseBlockDOList.forEach(e -> {


            // ---------------------------------- block type


            Integer type = e.getType();


            // ---------------------------------- block  ->  codePath / namePath


            StockBlockInfoDTO.BlockDTO blockDTO = new StockBlockInfoDTO.BlockDTO();
            blockDTO.setBlockType(type);
            blockDTO.setLevel(e.getLevel());
            blockDTO.setEndLevel(e.getEndLevel());
            blockDTO.setBlockCodePath(Objects.toString(e.getCodePath(), e.getCode()));
            blockDTO.setBlockNamePath(Objects.toString(e.getNamePath(), e.getName()));


            // 个股-概念     1->多
            if (type.equals(4)) {
                gnBlockDTOList.add(blockDTO);
            } else {
                // 个股-行业     1->1
                hyBlockDTOList.add(blockDTO);
            }

        });


        // 个股-行业     1->1
        dto.setHyBlockDTOList(hyBlockDTOList);

        // 个股-概念     1->多
        dto.setGnBlockDTOList(gnBlockDTOList);


        // ------------------------------------------------------------------- 自定义板块


        // List<BaseBlockNewDO> baseBlockNewDOList = baseBlockNewRelaStockService.listByStockCode(stockCode, BlockNewTypeEnum.STOCK.getType());
        // dto.setBlockNewDOList(baseBlockNewDOList);


        return dto;
    }


}