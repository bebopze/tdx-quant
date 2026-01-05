package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.dto.base.BlockDTO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;

import java.util.List;


/**
 * @author: bebopze
 * @date: 2025/6/8
 */
public interface BlockService {

    BlockDTO info(String blockCode);

    List<BaseStockDO> listStock(String blockCode);

}