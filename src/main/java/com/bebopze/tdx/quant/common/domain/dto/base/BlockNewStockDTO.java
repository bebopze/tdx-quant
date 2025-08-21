package com.bebopze.tdx.quant.common.domain.dto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


/**
 * @author: bebopze
 * @date: 2025/5/22
 */
@Data
public class BlockNewStockDTO implements Serializable {


    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 股票代码
     */
    @Schema(description = "股票代码")
    private String code;

    /**
     * 股票名称
     */
    @Schema(description = "股票名称")
    private String name;

//    /**
//     * 通达信-市场类型：0-深交所；1-上交所；2-北交所；
//     */
//    @Schema(description = "通达信-市场类型：0-深交所；1-上交所；2-北交所；")
//    private Integer tdxMarketType;
}