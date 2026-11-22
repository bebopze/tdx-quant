package com.bebopze.tdx.quant.common.domain.dto.base;

import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


/**
 * StockDTO
 *
 * @author: bebopze
 * @date: 2025/5/31
 */
@Data
public class BaseStockDTO extends BaseStockDO implements Serializable {


    /**
     * 股票类型：1-A股；2-ETF；3-板块；11-港股；12-美股；21-期权；
     *
     * @see StockTypeEnum
     */
    @Schema(description = "股票类型：1-A股；2-ETF；3-板块；11-港股；12-美股；21-期权；", implementation = StockTypeEnum.class)
    private Integer stockType;


}