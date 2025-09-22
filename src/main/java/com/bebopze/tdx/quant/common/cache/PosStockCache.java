package com.bebopze.tdx.quant.common.cache;

import com.bebopze.tdx.quant.common.domain.dto.base.BaseStockDTO;
import com.bebopze.tdx.quant.common.domain.dto.base.StockBlockInfoDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.dal.service.IBaseStockService;
import com.bebopze.tdx.quant.service.StockService;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;


/**
 * 持仓个股 - Cache
 *
 * @author: bebopze
 * @date: 2025/9/22
 */
@Slf4j
@Component
public class PosStockCache {


    @Autowired
    private StockService stockService;

    @Autowired
    private IBaseStockService baseStockService;


    // -----------------------------------------------------------------------------------------------------------------


    @Getter
    @Setter
    BaseStockDTO baseStockDTO;

    @Getter
    @Setter
    StockBlockInfoDTO stockBlockInfoDTO;


    // -----------------------------------------------------------------------------------------------------------------


    private static final Cache<String, PosStockCache> POS_STOCK_CACHE = Caffeine.newBuilder()
                                                                                .maximumSize(2_000)
                                                                                // .expireAfterWrite(10, TimeUnit.MINUTES)
                                                                                .expireAfterAccess(1, TimeUnit.HOURS)
                                                                                .recordStats()
                                                                                // .removalListener(createStatsRemovalListener("marketCache", () -> BacktestCache.marketCache))
                                                                                .scheduler(Scheduler.systemScheduler())
                                                                                .build();


    public PosStockCache get(String stockCode) {
        return POS_STOCK_CACHE.get(stockCode, this::refreshStock);
    }


    private PosStockCache refreshStock(String stockCode) {
        PosStockCache dto = new PosStockCache();
        dto.setBaseStockDTO(stockService.info(stockCode));
        dto.setStockBlockInfoDTO(stockService.blockInfo(stockCode));
        return dto;
    }


    public static Double getPreClose(String stockCode) {
        PosStockCache dto = POS_STOCK_CACHE.getIfPresent(stockCode);
        if (dto != null) {

            BaseStockDTO baseStockDTO = dto.getBaseStockDTO();
            if (baseStockDTO.getId() == null) {
                return null;
            }


            BigDecimal preClose = baseStockDTO.getPreClose();
            if (preClose != null) {
                return preClose.doubleValue();
            }


            LocalDate tradeDate = baseStockDTO.getTradeDate();
            if (tradeDate.isBefore(LocalDate.now())) {
                return baseStockDTO.getClose().doubleValue();
            }


            KlineDTO klineDTO = baseStockDTO.getKlineDTOList().get(baseStockDTO.getKlineDTOList().size() - 2);
            return klineDTO.getClose();
        }


        return null;
    }


}
