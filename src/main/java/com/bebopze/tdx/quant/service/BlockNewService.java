package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.dto.BlockNewBlockDTO;
import com.bebopze.tdx.quant.common.domain.dto.BlockNewStockDTO;

import java.util.List;

/**
 * @author: bebopze
 * @date: 2025/5/22
 */
public interface BlockNewService {

    List<BlockNewStockDTO> stockList(String blockNewCode);

    List<BlockNewBlockDTO> blockList(String blockNewCode);
}
