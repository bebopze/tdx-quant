package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.domain.dto.base.OptionDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.OptionEtfInfoDTO;
import com.bebopze.tdx.quant.dal.entity.BaseOptionDO;
import com.bebopze.tdx.quant.dal.service.IBaseOptionService;
import com.bebopze.tdx.quant.service.OptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


/**
 * 期权
 *
 * @author: bebopze
 * @date: 2026/11/16
 */
@Slf4j
@Service
public class OptionServiceImpl implements OptionService {


    @Autowired
    private IBaseOptionService baseOptionService;


    @Override
    public OptionDTO info(String optionCode) {

        BaseOptionDO entity = baseOptionService.getByCode(optionCode);

        OptionDTO dto = new OptionDTO();
        if (entity != null) {
            BeanUtils.copyProperties(entity, dto);
        }

        return dto;
    }


    @Override
    public OptionEtfInfoDTO optionEtfInfo(String optionCode) {
        return null;
    }


}