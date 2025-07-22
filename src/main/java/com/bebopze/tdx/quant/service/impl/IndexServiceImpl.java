package com.bebopze.tdx.quant.service.impl;

import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.dal.entity.QaMarketMidCycleDO;
import com.bebopze.tdx.quant.dal.service.IQaMarketMidCycleService;
import com.bebopze.tdx.quant.parser.tdxdata.IndexReportParser;
import com.bebopze.tdx.quant.service.IndexService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 大盘量化
 *
 * @author: bebopze
 * @date: 2025/7/21
 */
@Slf4j
@Service
public class IndexServiceImpl implements IndexService {


    @Autowired
    private IQaMarketMidCycleService qaMarketMidCycleService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importMarketMidCycle() {

        List<IndexReportParser.TdxFunResultDTO> dtoList = IndexReportParser.parse();

        // DTO -> DO
        List<QaMarketMidCycleDO> entityList = dtoList.stream().map(this::convert2DO).collect(Collectors.toList());


        // save -> DB
        qaMarketMidCycleService.deleteAll();
        qaMarketMidCycleService.saveBatch(entityList);
    }


    @Override
    public QaMarketMidCycleDO marketInfo(LocalDate date) {
        QaMarketMidCycleDO entity = qaMarketMidCycleService.getByDate(date);
        return entity;
    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * DTO -> DO
     *
     * @param dto
     * @return
     */
    private QaMarketMidCycleDO convert2DO(IndexReportParser.TdxFunResultDTO dto) {

        QaMarketMidCycleDO entity = new QaMarketMidCycleDO();
        entity.setDate(dto.getDate());


        BigDecimal ma50占比 = dto.getMA50占比();
        entity.setMa50Pct(dto.getMA50占比());


        // -------------------------------------- 大盘

        int 大盘牛熊 = dto.get牛市() == 1 ? 1 : 2;


        // 大盘顶底：1-底部；2- 底->顶；3-顶部；4- 顶->底；
        Integer 大盘顶底_status = dto.get大盘顶底_STATUS();
        Integer 底_DAY = dto.get底_DAY();
        Integer 顶_DAY = dto.get顶_DAY();


        // -------------------------------------- 大盘量化


        // 大盘牛熊
        entity.setMarketBullBearStatus(大盘牛熊);


        // 大盘顶底
        entity.setMarketMidStatus(大盘顶底_status);
        entity.setMarketLowDay(底_DAY);
        entity.setMarketHighDay(顶_DAY);


        // MA50占比
        entity.setMa50Pct(dto.getMA50占比());
        // 仓位
        entity.setPositionPct(calcPositionPct(大盘牛熊, 大盘顶底_status, 底_DAY, 顶_DAY, ma50占比.doubleValue()));


        // 月多占比
        entity.setStockMonthBullPct(dto.get个股月多占比());
        entity.setBlockMonthBullPct(dto.get板块月多占比());


        // 新高-新低
        entity.setHighNum(dto.get新高());
        entity.setLowNum(dto.get新低());
        entity.setAllStockNum(dto.get全A());
        entity.setHighLowDiff(dto.get差值());


        // 板块量化
        entity.setBs1Pct(dto.get左侧试仓_占比());
        entity.setBs2Pct(dto.get左侧买_占比());
        entity.setBs3Pct(dto.get右侧买_占比());
        entity.setBs4Pct(dto.get强势卖出_占比());
        entity.setBs5Pct(dto.get左侧卖_占比());
        entity.setBs6Pct(dto.get右侧卖_占比());

        entity.setRightBuyPct(dto.get右侧B_占比());
        entity.setRightSellPct(dto.get右侧S_占比());


        return entity;
    }


    /**
     * 仓位计算
     *
     * @param 大盘牛熊
     * @param 大盘顶底_status
     * @param 底_day
     * @param 顶_day
     * @param ma50占比
     * @return
     */
    private BigDecimal calcPositionPct(int 大盘牛熊,
                                       Integer 大盘顶底_status,
                                       Integer 底_day,
                                       Integer 顶_day,
                                       double ma50占比) {


        // 1-底
        boolean 底 = 大盘顶底_status == 1;
        // 2- 底->顶
        boolean 底_顶 = 大盘顶底_status == 2;
        // 3-顶
        boolean 顶 = 大盘顶底_status == 3;
        // 4- 顶->底
        boolean 顶_底 = 大盘顶底_status == 4;


        // （2- 底->顶；/ 4- 顶->底；）区间   持续天数
        int typeDay = 底_顶 ? 底_day : 顶_day;


        // -------------------------------------- 仓位


        double positionPct = 0.0;

        // 抄底梭哈（底部）   =>   满仓 + 满融
        if (底) {
            positionPct = 100;
        } else if (底_顶) {
            // 底->顶   =>   满仓
            positionPct = 100;
        } else if (顶) {
            // 顶部   =>   轻仓 -> 清仓
            positionPct = 100 - 10 * typeDay / 1;       // 每1天 -> 减仓10%
        } else if (顶_底) {
            // 顶->底   =>   清仓
            positionPct = 5 * typeDay / 20;      // 每20天 -> 加仓5%
        }


        return NumUtil.double2Decimal(positionPct);
    }


}