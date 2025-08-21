package com.bebopze.tdx.quant.common.domain.dto.backtest;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * BS策略 InfoDTO
 *
 * @author: bebopze
 * @date: 2025/8/13
 */
@Data
public class BSStrategyInfoDTO {


    private LocalDate date;
    private List<String> buyConList;
    private List<String> sellConList;
    private String topBlockCon;


    private Set<String> sell__stockCodeSet;
    private Map<String, String> sell_infoMap;


    private Set<String> buy__stockCodeSet;
    private Map<String, String> buy_infoMap;


}