package com.bebopze.tdx.quant.common.domain.dto;

import lombok.Data;


/**
 * 个股 - 行情快照（批量拉取 全A 实时行情）
 *
 * @author: bebopze
 * @date: 2025/8/3
 */
@Data
public class StockSnapshotKlineDTO extends KlineDTO {


    private String stockCode;

    private String stockName;


}