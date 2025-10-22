package com.bebopze.tdx.quant.service;

import com.bebopze.tdx.quant.common.constant.UpdateTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.topblock.*;
import com.bebopze.tdx.quant.service.impl.TopBlockServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * 主线板块
 *
 * @author: bebopze
 * @date: 2025/7/13
 */
public interface TopBlockService {


    /**
     * refreshAll
     */
    void refreshAll(UpdateTypeEnum updateTypeEnum);


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 1-百日新高
     *
     * @param N
     */
    void nDayHighTask(UpdateTypeEnum updateTypeEnum, int N);

    /**
     * 2-涨幅榜（N日涨幅>25%）
     *
     * @param N
     */
    void changePctTopTask(UpdateTypeEnum updateTypeEnum, int N);

    /**
     * 3-RPS红（一线95/双线90/三线85）
     *
     * @param RPS
     */
    void rpsRedTask(UpdateTypeEnum updateTypeEnum, double RPS);

    /**
     * 4-二阶段
     */
    void stage2Task(UpdateTypeEnum updateTypeEnum);

    /**
     * 5-大均线多头
     */
    void longTermMABullStackTask(UpdateTypeEnum updateTypeEnum);

    /**
     * 6-均线大多头
     */
    void bullMAStackTask(UpdateTypeEnum updateTypeEnum);

    /**
     * 7-均线极多头
     */
    void extremeBullMAStackTask(UpdateTypeEnum updateTypeEnum);

    /**
     * 11-板块AMO - TOP1
     */
    void blockAmoTopTask(UpdateTypeEnum updateTypeEnum);


    void bkyd2Task_v1(UpdateTypeEnum updateTypeEnum);


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 主线板块（板块-月多2）
     */
    void bkyd2Task(UpdateTypeEnum updateTypeEnum);


    /**
     * 主线板块 列表
     *
     * @param date 交易日
     * @param type 类型：1-机选；2-人选；
     * @return
     */
    TopBlockPoolDTO topBlockList(LocalDate date, Integer type);

    /**
     * 主线个股 列表
     *
     * @param date 交易日
     * @param type 类型：1-机选；2-人选；
     * @return
     */
    TopStockPoolDTO topStockList(LocalDate date, Integer type);


    /**
     * 主线个股  -  批量 add
     *
     * @param date
     * @param stockCodeSet
     * @return
     */
    int addTopStockSet(LocalDate date, Set<String> stockCodeSet);

    /**
     * 主线个股  -  批量 DEL
     *
     * @param date
     * @param stockCodeSet
     * @return
     */
    int delTopStockSet(LocalDate date, Set<String> stockCodeSet);


    // double calcChangePct(Set<String> stockCodeSet, LocalDate date, int N);


    TopStockPoolAnalysisDTO topStockListAnalysis(LocalDate startDate,
                                                 LocalDate endDate,
                                                 Integer topPoolType,
                                                 Integer type);


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * TOP榜（主线板块） - 近N日 占比分布
     *
     * @param blockNewId 1-百日新高；2-涨幅榜；3-RPS红（一线95/双线90/三线85）；4-二阶段；5-大均线多头；     - @See BlockNewIdEnum
     * @param date
     * @param resultType result类型：2-普通行业（LV2）；4-概念板块（LV3）；12-研究行业（LV1）
     * @param N
     * @return
     */
    Map<String, Integer> topBlockRate(int blockNewId, LocalDate date, int resultType, int N);

    /**
     * TOP榜（主线板块） - 近N日 占比分布
     *
     * @param blockNewId 1-百日新高；2-涨幅榜；3-RPS红（一线95/双线90/三线85）；4-二阶段；5-大均线多头；     - @See BlockNewIdEnum
     * @param date
     * @param resultType result类型：2-普通行业（LV2）；4-概念板块（LV3）；12-研究行业（LV1）
     * @param hyLevel    行业level：1-一级行业；2-二级行业；3-三级行业；
     * @param N
     * @return
     */
    Map<String, Integer> topBlockRate(int blockNewId, LocalDate date, int resultType, Integer hyLevel, int N);


    List<TopBlockServiceImpl.ResultTypeLevelRateDTO> topBlockRateAll(int blockNewId, LocalDate date, int N);


    List<TopBlock2DTO> topBlockRateInfo(int blockNewId, LocalDate date, int resultType, int N);

}
