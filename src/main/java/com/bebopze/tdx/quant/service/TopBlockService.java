package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.service.impl.TopBlockServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * @author: bebopze
 * @date: 2025/7/13
 */
public interface TopBlockService {


    void refreshAll();


    /**
     * @param N
     */
    void nDayHighTask(int N);

    /**
     * 涨幅榜（N日涨幅>25%） - 占比分布
     *
     * @param N
     */
    void changePctTopTask(int N);


    /**
     * TOP榜（主线板块） - 近N日 占比分布
     *
     * @param blockNewId 1-百日新高；2-涨幅榜；3-RPS三线红（一线95/双线90/三线85）；4-二阶段；5-均线大多头；     - @See BlockNewIdEnum
     * @param date
     * @param resultType result类型：2-普通行业（LV2）；4-概念板块（LV3）；12-研究行业（LV1）
     * @param N
     * @return
     */
    Map<String, Integer> topBlockRate(int blockNewId, LocalDate date, int resultType, int N);

    List<TopBlockServiceImpl.TopBlockDTO> topBlockRateInfo(int blockNewId, LocalDate date, int resultType, int N);
}
