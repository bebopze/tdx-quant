package com.bebopze.tdx.quant.common.domain.dto.topblock;

import lombok.Data;

import java.util.List;


/**
 * 主线板块 列表
 *
 * @author: bebopze
 * @date: 2025/10/7
 */
@Data
public class TopBlockPoolDTO {


    /**
     * 主线板块 池   ->   指数 涨跌幅（汇总 计算平均值）
     */
    private TopPoolAvgPctDTO topBlockAvgPctDTO;


    /**
     * 主线板块 - 列表
     */
    private List<TopBlockDTO> topBlockDTOList;
}