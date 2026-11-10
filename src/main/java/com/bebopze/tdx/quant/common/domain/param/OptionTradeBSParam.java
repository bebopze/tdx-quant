package com.bebopze.tdx.quant.common.domain.param;

import com.bebopze.tdx.quant.common.constant.option.OptionPriceTypeEnum;
import com.bebopze.tdx.quant.common.constant.option.OptionTradeTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;


/**
 * ETF期权 - BS
 *
 * @author: bebopze
 * @date: 2026/11/11
 */
@Data
public class OptionTradeBSParam {


    // Hybh     10011565
    // Amount   1
    // Price    0.0033
    //
    // Zqyw     400
    // Ywxw     130
    // Jybk     15


    @Schema(description = "标的证券code（ETF代码）", example = "588000", requiredMode = Schema.RequiredMode.REQUIRED)
    private String etfCode;

    // 非必须
    @Schema(description = "标的证券name（ETF名称）", example = "科创50ETF华夏", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String etfName;


    @Schema(description = "合约编码", example = "10011565", requiredMode = Schema.RequiredMode.REQUIRED)
    private String hybh;

    @Schema(description = "价格", example = "0.0001", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal price;

    @Schema(description = "数量", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer amount;


    // -----------------------------------------------------------------------------------------------------------------


    // 交易类型 枚举（自动映射   ->   Zqyw）
    @Schema(description = "交易类型", example = "BUY_OPEN", requiredMode = Schema.RequiredMode.REQUIRED)
    private OptionTradeTypeEnum optionTradeTypeEnum;

    // 报价方式 枚举（自动映射   ->   Ywxw）
    @Schema(description = "报价方式", example = "LMT", requiredMode = Schema.RequiredMode.REQUIRED)
    private OptionPriceTypeEnum optionPriceTypeEnum;


//    // 交易所 枚举（自动映射   ->   Jybk）     // 可自动 根据 etfCode -> 获取 交易所 -> 获取 optionMarketTypeEnum
//    @Schema(description = "交易所", example = "SZ", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
//    private transient OptionMarketTypeEnum optionMarketTypeEnum;


}