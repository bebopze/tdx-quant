package com.bebopze.tdx.quant.common.domain.dto.analysis;

import lombok.Data;

import java.util.List;


/**
 * 主线个股 列表   ->   收益率 详情分析（指定时间段）
 *
 * @author: bebopze
 * @date: 2025/10/21
 */
@Data
public class TopPoolAnalysisDTO {


    /**
     * 收益率 汇总统计
     */
    TopPoolSumReturnDTO sumReturnDTO;


    /**
     * 每日收益率
     */
    List<TopPoolDailyReturnDTO> dailyReturnDTOList;


    /**
     * 上榜 次数/涨幅 统计
     */
    List<TopCountDTO> countDTOList;

}