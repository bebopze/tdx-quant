package com.bebopze.tdx.quant.domain.param;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


/**
 * 撤单
 *
 * @author: bebopze
 * @date: 2025/5/11
 */
@Data
public class TradeRevokeOrdersParam implements Serializable {


    @Schema(description = "日期", example = "20250511", requiredMode = Schema.RequiredMode.REQUIRED)
    private String date;

    @Schema(description = "委托编号", example = "1006", requiredMode = Schema.RequiredMode.REQUIRED)
    private String wtdh;
}
