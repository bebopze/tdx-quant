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
     * 收益率 汇总统计（普通账户）
     */
    TopPoolSumReturnDTO sumReturnDTO;
    /**
     * 收益率 汇总统计（融资账户）
     */
    TopPoolSumReturnDTO marginSumReturnDTO;


    /**
     * 每日收益率（普通账户/融资账户）
     */
    List<TopPoolDailyReturnDTO> dailyReturnDTOList;
    /**
     * 平均每日收益率（普通账户/融资账户）
     */
    TopPoolDailyReturnDTO avgDailyReturnDTO;


    /**
     * 上榜 次数/涨幅 统计
     */
    List<TopCountDTO> countDTOList;

}