package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.domain.dto.base.OptionDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.OptionEtfInfoDTO;

/**
 * 期权
 *
 * @author: bebopze
 * @date: 2026/11/16
 */
public interface OptionService {


    OptionDTO info(String optionCode);

    OptionEtfInfoDTO optionEtfInfo(String optionCode);
}