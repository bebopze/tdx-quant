package com.bebopze.tdx.quant.common.domain.dto.kline;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;


/**
 * Data Info
 *
 * @author: bebopze
 * @date: 2025/8/15
 */
@Data
public class DataInfoDTO {


    // ------------------------------------ stock


    private LocalDate stock_tradeDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime stock_updateTime;

    private KlineDTO stock_klineDTO;

    private ExtDataDTO stock_extDataDTO;


    // ------------------------------------ block


    private LocalDate block_tradeDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime block_updateTime;

    private KlineDTO block_klineDTO;

    private ExtDataDTO block_extDataDTO;


    // ------------------------------------ topBlock


    private LocalDate topBlock_tradeDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime topBlock_updateTime;


    // ------------------------------------ market


    private LocalDate market_tradeDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime market_updateTime;


}