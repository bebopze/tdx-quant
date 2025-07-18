package com.bebopze.tdx.quant.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * @author: bebopze
 * @date: 2025/7/13
 */
public interface IndexService {


    void nDayHighTask(int N);

    /**
     * @param date
     * @param resultType result类型：2-普通行业（LV2）；4-概念板块（LV3）；12-研究行业（LV1）
     * @param N
     * @return
     */
    Map<String, Integer> nDayHighRate(LocalDate date, int resultType, int N);
}
