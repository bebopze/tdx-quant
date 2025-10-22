package com.bebopze.tdx.quant.common.domain.dto.topblock;

import lombok.Data;

import java.util.List;


/**
 * 主线个股 列表   ->   收益率 详情分析（指定时间段）
 *
 * @author: bebopze
 * @date: 2025/10/21
 */
@Data
public class TopStockPoolAnalysisDTO {


    /**
     * 收益率 汇总统计
     */
    TopStockPoolSumReturnDTO sumReturnDTO;


    /**
     * 每日收益率
     */
    List<TopStockPoolDailyReturnDTO> dailyReturnDTOList;


}