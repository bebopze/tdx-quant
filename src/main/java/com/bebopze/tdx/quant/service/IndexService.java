package com.bebopze.tdx.quant.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * @author: bebopze
 * @date: 2025/7/13
 */
public interface IndexService {


    void nDayHighTask(int N);

    Map<String, Integer> nDayHighRate(LocalDate date, int N);
}
