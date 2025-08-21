package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.domain.dto.base.BlockDTO;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockRelaStockService;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.service.BlockService;
import com.bebopze.tdx.quant.service.InitDataService;
import com.bebopze.tdx.quant.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 板块
 *
 * @author: bebopze
 * @date: 2025/6/8
 */
@Slf4j
@Service
public class BlockServiceImpl implements BlockService {


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


}