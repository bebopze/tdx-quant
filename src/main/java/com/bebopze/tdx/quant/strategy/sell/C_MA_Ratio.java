package com.bebopze.tdx.quant.strategy.sell;

import lombok.Data;


/**
 * C_MA_偏离率     - B/S（低吸/高抛 区间极值）
 *
 * @author: bebopze
 * @date: 2025/11/24
 */
@Data
public class C_MA_Ratio {


    public double short_MA_Ratio;

    public double medium_MA_Ratio;

    public double long_MA_Ratio;
}