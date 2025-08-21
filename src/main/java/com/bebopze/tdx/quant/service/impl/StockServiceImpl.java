package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.domain.dto.base.BaseStockDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.StockBlockInfoDTO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.service.*;
import com.bebopze.tdx.quant.service.StockService;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;


/**
 * @author: bebopze
 * @date: 2025/5/18
 */
@Service
public class StockServiceImpl implements StockService {


    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;

    @Autowired
    private IBaseBlockNewRelaStockService baseBlockNewRelaStockService;


    @Override
    public BaseStockDTO info(String stockCode) {
        BaseStockDO entity = baseStockService.getByCode(stockCode);


        BaseStockDTO dto = new BaseStockDTO();
        if (entity != null) {

            dto.setKlineHis(entity.getKlineHis());
            BeanUtils.copyProperties(entity, dto);


//            List<KlineDTO> klineDTOList = ConvertStockKline.str2DTOList(entity.getKlineHis(), 100);
//
//            Map<String, Object> klineMap = new HashMap<>();
//            klineMap.put("date", ConvertStockKline.dateFieldValArr(klineDTOList, "date"));
//            klineMap.put("close", ConvertStockKline.fieldValArr(klineDTOList, "close"));
//
//            dto.setKlineMap(klineMap);
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
            blockDTO.setBlockCodePath(StringUtils.defaultString(e.getCodePath(), e.getCode()));
            blockDTO.setBlockNamePath(StringUtils.defaultString(e.getNamePath(), e.getName()));


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