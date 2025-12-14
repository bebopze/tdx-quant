package com.bebopze.tdx.quant.common.domain.dto.backtest;

import com.bebopze.tdx.quant.common.constant.SellStrategyEnum;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import lombok.Data;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;


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
    public Set<String> open_B___stockCodeSet = Sets.newHashSet();
    public Map<String, String> open_B___buy_infoMap = Maps.newHashMap();


    // 开盘S
    public Set<String> open_S___stockCodeSet = Sets.newHashSet();
    public Map<String, SellStrategyEnum> open_S___sell_infoMap = Maps.newHashMap();

}