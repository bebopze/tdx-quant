package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.domain.dto.BlockNewBlockDTO;
import com.bebopze.tdx.quant.common.domain.dto.BlockNewStockDTO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.service.BlockNewService;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author: bebopze
 * @date: 2025/5/22
 */
@Slf4j
@Service
public class BlockNewServiceImpl implements BlockNewService {


    @Autowired
    private IBaseBlockNewRelaStockService baseBlockNewRelaStockService;


    @Override
    public List<BlockNewStockDTO> stockList(String blockNewCode) {

        List<BaseStockDO> baseStockDOList = baseBlockNewRelaStockService.listStockByBlockNewCodeList(Lists.newArrayList(blockNewCode));
        if (CollectionUtils.isEmpty(baseStockDOList)) {
            return Lists.newArrayList();
        }


        List<BlockNewStockDTO> dtoList = baseStockDOList.stream().map(e -> {
                    BlockNewStockDTO dto = new BlockNewStockDTO();
                    BeanUtils.copyProperties(e, dto);
                    return dto;
                })
                .collect(Collectors.toList());

        return dtoList;
    }

    @Override
    public List<BlockNewBlockDTO> blockList(String blockNewCode) {

        List<BaseBlockDO> baseBlockDOList = baseBlockNewRelaStockService.listBlockByBlockNewCodeList(Lists.newArrayList(blockNewCode));
        if (CollectionUtils.isEmpty(baseBlockDOList)) {
            return Lists.newArrayList();
        }


        List<BlockNewBlockDTO> dtoList = baseBlockDOList.stream().map(e -> {
                    BlockNewBlockDTO dto = new BlockNewBlockDTO();
                    BeanUtils.copyProperties(e, dto);
                    return dto;
                })
                .collect(Collectors.toList());

        return dtoList;
    }

}
