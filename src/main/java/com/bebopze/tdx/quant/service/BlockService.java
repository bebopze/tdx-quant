package com.bebopze.tdx.quant.service;


import com.bebopze.tdx.quant.common.domain.dto.BlockDTO;

/**
 * @author: bebopze
 * @date: 2025/6/8
 */
public interface BlockService {

    BlockDTO info(String blockCode);

    Object listStock(String blockCode);
}
