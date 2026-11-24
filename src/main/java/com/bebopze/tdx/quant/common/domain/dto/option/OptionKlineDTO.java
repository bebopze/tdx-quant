package com.bebopze.tdx.quant.common.domain.dto.option;

import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


/**
 * 期权 - 单合约 K线（合约详情 / 合约K线弹窗）
 *
 * @author: bebopze
 * @date: 2026/11/24
 */
@Data
public class OptionKlineDTO implements Serializable {

    private static final long serialVersionUID = 1L;


    @Schema(description = "期权合约编码", example = "10012853")
    private String code;

    @Schema(description = "期权合约名称")
    private String name;

    @Schema(description = "合约类型：C-认购；P-认沽")
    private String callPutType;

    @Schema(description = "行权价格")
    private BigDecimal strikePrice;

    @Schema(description = "合约月份（yyyyMM）")
    private Integer contractMonth;

    @Schema(description = "合约开始日期")
    private LocalDate startDate;

    @Schema(description = "合约结束日期")
    private LocalDate endDate;

    @Schema(description = "合约乘数")
    private Integer multiplier;

    @Schema(description = "标的证券code")
    private String underlyingCode;

    @Schema(description = "标的证券name")
    private String underlyingName;

    @Schema(description = "K线数量")
    private Integer barCount;

    @Schema(description = "日K线列表（升序）")
    private List<KlineDTO> klineList;

}
