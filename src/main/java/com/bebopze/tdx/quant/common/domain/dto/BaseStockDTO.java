package com.bebopze.tdx.quant.common.domain.dto;

import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import lombok.Data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * @author: bebopze
 * @date: 2025/5/31
 */
@Data
public class BaseStockDTO extends BaseStockDO implements Serializable {


    private Map<String, Object> klineMap = new HashMap<>();
}
