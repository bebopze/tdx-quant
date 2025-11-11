package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.dto.base.BlockNewBlockDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.BlockNewStockDTO;

import java.util.List;
import java.util.Set;

/**
 * @author: bebopze
 * @date: 2025/5/22
 */
public interface BlockNewService {

    List<BlockNewStockDTO> stockList(String blockNewCode);

    List<BlockNewBlockDTO> blockList(String blockNewCode);


    /**
     * DB -> 通达信
     *
     * @param blockNewCode
     * @param codeSet
     */
    void importTdx(String blockNewCode, Set<String> codeSet);
}
