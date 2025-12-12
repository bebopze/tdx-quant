package com.bebopze.tdx.quant.common.domain.dto.backtest;

import com.bebopze.tdx.quant.common.constant.SellStrategyEnum;
import com.google.common.collect.Maps;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;


/**
 * 开盘B/开盘S
 *
 * @author: bebopze
 * @date: 2025/12/10
 */
@Data
public class BacktestOpenBSDTO {


    public LocalDate today_date;

    public LocalDate next_date;  // 99% 的个股  ->  next_date 相同  （1% 不同 -> 停牌 【暂时先忽略】）


    // 开盘B
    public Map<String, String> open_B___stockCode_name_Map = Maps.newHashMap();
    public Map<String, Double> open_B___stockCode_open_Map = Maps.newHashMap();
    public Map<String, Double> open_B___stockCode_close_Map = Maps.newHashMap();
    public Map<String, String> open_B___buy_infoMap = Maps.newHashMap();


    // 开盘S
    public Map<String, String> open_S___stockCode_name_Map = Maps.newHashMap();
    public Map<String, Double> open_S___stockCode_open_Map = Maps.newHashMap();
    public Map<String, Double> open_S___stockCode_close_Map = Maps.newHashMap();
    public Map<String, SellStrategyEnum> open_S___sell_infoMap = Maps.newHashMap();

}