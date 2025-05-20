package com.bebopze.tdx.quant.common.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * 通达信 - 基础 板块池（选股公式）
 *
 * @author: bebopze
 * @date: 2025/5/20
 */
@AllArgsConstructor
public enum BlockPoolEnum {


    BK_YD("BK-YD(ZD)", "板块-月多(自动)"),
    BK_YD2("BK-YD2(ZD)", "板块-月多2(自动)"),
    BK_YD3("BK-YD3(ZD)", "板块-月多3(自动)"),


    BK_T0("BK-T0(ZD)", "板块-T0(自动)"),


    BK_EJD("BK-EJD(ZD)", "板块-二阶段(自动)"),
    BK_SXH("BK-SXH(ZD)", "板块-三线红(自动)"),

    BK_N("BK-N(ZD)", "板块-牛(自动)"),


    BK_60RXG("BK-60RXG(ZD)", "板块-60日新高(自动)"),
    BK_KDZD("BK-KDZD(ZD)", "板块-口袋支点(自动)"),


    BK_ZCM("BK-ZCM(ZD)", "板块-左侧买(自动)"),
    BK_YCM("BK-YCM(ZD)", "板块-右侧买(自动)"),
    BK_QSMC("BK-QSMC(ZD)", "板块-强势卖出(自动)"),
    BK_ZCS("BK-ZCS(ZD)", "板块-左侧S(自动)"),
    BK_YCS("BK-YCS(ZD)", "板块-右侧S(自动)"),


    BK_ZX("BK-ZX(ZD)", "板块-主线(自动)"),
    BK_ZXN("BK-ZXN(ZD)", "板块-主线牛(自动)"),


    BK_XLT("BK-XLT(ZD)", "板块-新龙头(自动)"),
    BK_HL("BK-HL(ZD)", "板块-活龙(自动)"),


    ;


    @Getter
    private String blockNewCode;

    @Getter
    private String desc;
}