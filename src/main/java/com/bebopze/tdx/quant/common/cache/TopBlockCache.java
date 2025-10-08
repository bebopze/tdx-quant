package com.bebopze.tdx.quant.common.cache;

import com.bebopze.tdx.quant.common.config.anno.TotalTime;
import com.bebopze.tdx.quant.common.constant.StockTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopBlockDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopChangePctDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopStockDTO;
import com.bebopze.tdx.quant.common.tdxfun.TdxExtFun;
import com.bebopze.tdx.quant.common.tdxfun.TdxFun;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.common.util.StockUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.dal.entity.QaTopBlockDO;
import com.bebopze.tdx.quant.dal.service.IBaseBlockRelaStockService;
import com.bebopze.tdx.quant.dal.service.IBaseBlockService;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.dal.service.IQaTopBlockService;
import com.bebopze.tdx.quant.indicator.StockFun;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


/**
 * 主线板块/主线个股（板块-月多2）  -   Cache
 *
 * @author: bebopze
 * @date: 2025/9/25
 */
@Slf4j
@Component
public class TopBlockCache {


    public static final BacktestCache data = new BacktestCache();


    // -----------------------------------------------------------------------------------------------------------------


    @Autowired
    private IBaseBlockService baseBlockService;

    @Autowired
    private IBaseStockService baseStockService;

    @Autowired
    private IBaseBlockRelaStockService baseBlockRelaStockService;


    @Autowired
    private IQaTopBlockService qaTopBlockService;


    public BaseBlockDO getBlock(String blockCode) {
        return data.codeBlockMap.computeIfAbsent(blockCode, k -> baseBlockService.getSimpleByCode(k));
    }


    public String getBlockName(String blockCode) {
        BaseBlockDO blockDO = getBlock(blockCode);
        return blockDO == null ? null : blockDO.getName();
    }


    public List<TopBlockDTO.TopStock> getTopStockList(String blockCode,
                                                      Set<String> allTopStockCodeSet,
                                                      Map<String, Integer> topStock__codeCountMap) {


        // 板块 - 个股列表
        Set<String> block_stockCodeSet = data.blockCode_stockCodeSet_Map.computeIfAbsent(blockCode, k -> {

            List<BaseStockDO> block__stockDOList = baseBlockRelaStockService.listStockByBlockCodeList(Lists.newArrayList(blockCode));


            block__stockDOList.forEach(simpleStockDO -> {
                data.codeStockMap.computeIfAbsent(simpleStockDO.getCode(), x -> simpleStockDO);
            });


            return block__stockDOList.stream().map(BaseStockDO::getCode).collect(Collectors.toSet());
        });


        // -------------------------------------------------------------------------------------------------------------


        List<TopBlockDTO.TopStock> topStockList = Lists.newArrayList();


        // 主线个股
        allTopStockCodeSet.forEach(topStockCode -> {

            // 板块个股  ->  IN 主线个股
            if (block_stockCodeSet.contains(topStockCode)) {


                TopBlockDTO.TopStock topStock = new TopBlockDTO.TopStock();
                topStock.setStockCode(topStockCode);
                topStock.setStockName(data.codeStockMap.getOrDefault(topStockCode, new BaseStockDO()).getName());
                topStock.setTopDays(topStock__codeCountMap.get(topStockCode));


                topStockList.add(topStock);
            }
        });


        return topStockList.stream()
                           .sorted(Comparator.comparing(TopBlockDTO.TopStock::getTopDays).reversed())
                           .collect(Collectors.toList());
    }


    // -----------------------------------------------------------------------------------------------------------------


    public BaseStockDO getStock(String stockCode) {
        return data.codeStockMap.computeIfAbsent(stockCode, k -> baseStockService.getSimpleByCode(k));
    }


    public String getStockName(String stockCode) {
        BaseStockDO stockDO = getStock(stockCode);
        return stockDO == null ? null : stockDO.getName();
    }


    public List<TopStockDTO.TopBlock> getTopBlockList(String stockCode,
                                                      Set<String> allTopBlockCodeSet,
                                                      Map<String, Integer> topBlock__codeCountMap) {


        // 个股 - 板块列表
        Set<String> stock_blockCodeSet = data.stockCode_blockCodeSet_Map.computeIfAbsent(stockCode, k -> {

            List<BaseBlockDO> stock__blockDOList = baseBlockRelaStockService.listBlockByStockCodeList(Lists.newArrayList(stockCode));


            stock__blockDOList.forEach(simpleBlockDO -> {
                data.codeBlockMap.computeIfAbsent(simpleBlockDO.getCode(), x -> simpleBlockDO);
            });


            return stock__blockDOList.stream().map(BaseBlockDO::getCode).collect(Collectors.toSet());
        });


        // -------------------------------------------------------------------------------------------------------------


        List<TopStockDTO.TopBlock> topBlockList = Lists.newArrayList();


        // 主线板块
        allTopBlockCodeSet.forEach(topBlockCode -> {

            // 个股板块  ->  IN 主线板块
            if (stock_blockCodeSet.contains(topBlockCode)) {


                TopStockDTO.TopBlock topBlock = new TopStockDTO.TopBlock();
                topBlock.setBlockCode(topBlockCode);
                topBlock.setBlockName(data.codeBlockMap.get(topBlockCode).getName());
                topBlock.setTopDays(topBlock__codeCountMap.get(topBlockCode));


                topBlockList.add(topBlock);
            }
        });


        return topBlockList.stream()
                           .sorted(Comparator.comparing(TopStockDTO.TopBlock::getTopDays).reversed())
                           .collect(Collectors.toList());
    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    @TotalTime
    public TopChangePctDTO changePctInfo(String code,
                                         LocalDate date,
                                         Map<String, TopChangePctDTO> stock_topDateInfo_map) {


        StockFun fun = getFun(code);


        KlineArrDTO klineArrDTO = fun.getKlineArrDTO();
        Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


        // -------------------------------------------------------------------------------------------------------------


        // 首次上榜日期、跌出榜单日期
        TopChangePctDTO topChangePctDTO = stock_topDateInfo_map.get(code);
        if (null == topChangePctDTO) {
            return null;
        }


        // -------------------------------------------------------------------------------------------------------------


        // 上榜涨幅


        Integer date_idx = dateIndexMap.get(date);
        Integer nextDate_idx = Math.min(date_idx + 1, dateIndexMap.size() - 1);
        Integer startTopDate_idx = dateIndexMap.get(topChangePctDTO.topStartDate);
        Integer endTopDate_idx = dateIndexMap.get(topChangePctDTO.topEndDate);


        if (null == date_idx || null == startTopDate_idx || null == endTopDate_idx) {
            log.error("idx = null     >>>     code : {} , date : {} , startTopDate : {} , endTopDate : {}",
                      code, date, topChangePctDTO.topStartDate, topChangePctDTO.topEndDate);
        }


        // 停牌
        endTopDate_idx = endTopDate_idx == null ? date_idx : endTopDate_idx;


        double date_close = klineArrDTO.close[date_idx];
        double nextDate_idx_close = klineArrDTO.close[nextDate_idx];
        double startTopDate_idx_close = klineArrDTO.close[startTopDate_idx];
        double endTopDate_idx_close = klineArrDTO.close[endTopDate_idx];


        double start2Today_changePct = date_close / startTopDate_idx_close * 100 - 100;
        double start2End_changePct = endTopDate_idx_close / startTopDate_idx_close * 100 - 100;
        double today2Next_changePct = nextDate_idx_close / date_close * 100 - 100;
        double today2End_changePct = endTopDate_idx_close / date_close * 100 - 100;


        topChangePctDTO.setStart2Today_changePct(NumUtil.of(start2Today_changePct));
        topChangePctDTO.setStart2End_changePct(NumUtil.of(start2End_changePct));
        topChangePctDTO.setToday2Next_changePct(NumUtil.of(today2Next_changePct));
        topChangePctDTO.setToday2End_changePct(NumUtil.of(today2End_changePct));


        // -------------------------------------------------------------------------------------------------------------


        return topChangePctDTO;
    }


    // -----------------------------------------------------------------------------------------------------------------


    private StockFun getFun(String code) {
        return StockTypeEnum.isBlock(code) ? data.getOrCreateBlockFun(getBlock(code)) : data.getOrCreateStockFun(getStock(code));
    }


    public static StockFun getFun2(String code) {
        return StockTypeEnum.isBlock(code) ? data.getOrCreateBlockFun(code) : data.getOrCreateStockFun(code);
    }

    public static Set<String> getTopCodeSet(QaTopBlockDO topBlockDO, String code, int type) {
        return StockTypeEnum.isBlock(code) ? topBlockDO.getTopBlockCodeJsonSet(type) : topBlockDO.getTopStockCodeJsonSet(type);
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 获取 指定日期（基准日）   当日上榜 主线板块/个股     的     首次上榜日期（往前倒推）、跌出榜单日期（往后倒推）
     *
     * @param date          交易日（基准日）
     * @param stockTypeEnum
     * @return
     */
    @TotalTime
    public Map<String, TopChangePctDTO> stock_topDateInfo_map(LocalDate date, StockTypeEnum stockTypeEnum, int type) {
        // 今日date   ->   前后 各50条
        int days = StockUtil.tradeDays2NatureDays(50); // 交易日天数 -> 自然日天数
        List<QaTopBlockDO> lastEntity__before50_after50 = qaTopBlockService.lastN(date.plusDays(days), 100);


        List<QaTopBlockDO> lastEntity__1 = qaTopBlockService.lastN(date, 1);
        if (lastEntity__1.isEmpty()) {
            return Collections.emptyMap();
        }


        LocalDate baseDate = lastEntity__1.get(0).getDate();


        return stock_topDateInfo_map(baseDate, lastEntity__before50_after50, stockTypeEnum, type);
    }

    /**
     * @param date                         交易日（基准日）
     * @param lastEntity__before50_after50 交易日（基准日）  ->   前后 各50条
     * @return
     */
    public Map<String, TopChangePctDTO> stock_topDateInfo_map(LocalDate date,
                                                              List<QaTopBlockDO> lastEntity__before50_after50,
                                                              StockTypeEnum stockTypeEnum,
                                                              int type) {


        Map<String, TopChangePctDTO> stock_topDateInfo_map = Maps.newHashMap();


        // 按日期排序（升序）
        List<QaTopBlockDO> sortedList = lastEntity__before50_after50.stream()
                                                                    .sorted(Comparator.comparing(QaTopBlockDO::getDate))
                                                                    .collect(Collectors.toList());


        // 找到基准日期的索引
        int topBaseIdx = -1;
        for (int i = 0; i < sortedList.size(); i++) {
            if (sortedList.get(i).getDate().isEqual(date)) {
                topBaseIdx = i;
                break;
            }
        }


        // 基准日期不存在
        if (topBaseIdx == -1) {
            return stock_topDateInfo_map;
        }


        // -------------------------------------------------------------------------------------------------------------


        // 获取 基准日期 当天上榜的   板块/个股 code列表
        QaTopBlockDO baseEntity = sortedList.get(topBaseIdx);
        Set<String> baseDate__topCodeSet = Objects.equals(StockTypeEnum.TDX_BLOCK, stockTypeEnum) ? baseEntity.getTopBlockCodeJsonSet(type) : baseEntity.getTopStockCodeJsonSet(type);


        for (String code : baseDate__topCodeSet) {
            TopChangePctDTO dto = new TopChangePctDTO();


            // 计算首次上榜日期（往前倒推）
            dto.setTopStartDate(calculateFirstTopDate(sortedList, date, topBaseIdx, code, type));

            // 计算跌出榜单日期（往后倒推）
            dto.setTopEndDate(calculateEndTopDate(sortedList, date, topBaseIdx, code, type));


            stock_topDateInfo_map.put(code, dto);
        }


        return stock_topDateInfo_map;
    }


    /**
     * 计算首次上榜日期（往前倒推）
     */
    private LocalDate calculateFirstTopDate(List<QaTopBlockDO> sortedList,
                                            LocalDate topBaseDate,
                                            int topBaseIdx,
                                            String code,
                                            int type) {


        StockFun fun = getFun(code);

        ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
        Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


        // -------------------------------------------------------------------------------------------------------------


        boolean[] SSF空 = extDataArrDTO.SSF空;
        boolean[] MA20空 = extDataArrDTO.MA20空;

        boolean[] SSF_MA20空 = TdxExtFun.con_or(SSF空, MA20空);


        int[] BARSLAST__SSF_MA20空 = TdxFun.BARSLAST(SSF_MA20空);


        // -------------------------------------------------------------------------------------------------------------


        Integer kline_idx = dateIndexMap.get(topBaseDate);
        if (kline_idx == null) {
            return topBaseDate;
        }


        // -------------------------------------------------------------


        // 计算 首次上榜日期（往前倒推）

        // 1、SSF空 || MA20空   ->   当日   区间（date 列表）

        // 2、区间内 首次上榜日期


        // -------------------------------------------------------------


        // top区间起始日期   ->   距今天数
        int last__SSF_MA20空__days = BARSLAST__SSF_MA20空[kline_idx];

        // top区间起始日期   ->   idx
        int topStartIdx = Math.max(topBaseIdx - last__SSF_MA20空__days, 0);


        // 遍历区间
        for (int i = topStartIdx; i <= topBaseIdx; i++) {
            QaTopBlockDO topBlockDO = sortedList.get(i);

            LocalDate date = topBlockDO.getDate();
            // Set<String> topBlockCodeSet = topBlockDO.getTopBlockCodeJsonSet();
            Set<String> topCodeSet = getTopCodeSet(topBlockDO, code, type);

            if (topCodeSet.contains(code)) {
                return date;
            }
        }


        return topBaseDate;
    }


    /**
     * 计算 首次跌出榜单日期（往后倒推）
     */
    private LocalDate calculateEndTopDate(List<QaTopBlockDO> sortedList,
                                          LocalDate topBaseDate,
                                          int topBaseIdx,
                                          String code, int type) {


        StockFun fun = getFun(code);

        ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();
        Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


        // -------------------------------------------------------------------------------------------------------------


        boolean[] 下SSF = extDataArrDTO.下SSF;
        boolean[] 下MA20 = extDataArrDTO.下MA20;

        boolean[] 下SSF_MA20 = TdxExtFun.con_or(下SSF, 下MA20);


        int[] BARSNEXT__下SSF_MA20 = TdxFun.BARSNEXT(下SSF_MA20);


        // -------------------------------------------------------------------------------------------------------------


        Integer kline_idx = dateIndexMap.get(topBaseDate);
        if (kline_idx == null) {
            return topBaseDate;
        }

        // -------------------------------------------------------------


        // 计算 首次 跌出榜单日期（往后倒推）


        // 1、当日   ->   下SSF || 下MA20   区间（date 列表）

        // 2、区间内 首次 跌出榜单日期


        // -------------------------------------------------------------


        // 今日   ->   top区间end日期
        int next__下SSF_MA20__days = BARSNEXT__下SSF_MA20[kline_idx];


        // top区间end日期   ->   idx
        int topEndIdx;
        if (next__下SSF_MA20__days == -1) {
            topEndIdx = sortedList.size() - 1;
        } else {
            topEndIdx = Math.min(topBaseIdx + next__下SSF_MA20__days, sortedList.size() - 1);
        }


        LocalDate topEndDate = sortedList.get(topEndIdx).getDate();
        if (topEndDate == null) {
            log.error("————————————————————————————————       topEndDate == null");
        }
        return topEndDate;
    }


}