package com.bebopze.tdx.quant.common.domain.dto.fun;


/**
 * 中期调整 Result
 *
 * @author: bebopze
 * @date: 2025/12/16
 */
public class MidAdjustResult {

    // _H
    public double[] H;
    public int[] H_DAY;

    // _L
    public double[] L;
    public int[] L_DAY;


    // 调整幅度
    public double[] adjustPct1;
    // 调整天数
    public int[] adjustDays1;


    // 调整幅度2
    public double[] adjustPct2;
    // 调整天数2
    public int[] adjustDays2;
}