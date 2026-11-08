package com.bebopze.tdx.quant.common.constant.option;

import lombok.AllArgsConstructor;
import lombok.Getter;


/**
 * ETF期权 - 报价方式
 *
 * @author: bebopze
 * @date: 2026/11/7
 */
@AllArgsConstructor
public enum OptionPriceTypeEnum {


    // Ywxw - 报价方式（130-LMT限价；131-LOK限价即全成否则撤；132-FAL市价剩转限价；133-FOK市价即全成否则撤；134-FAK市价即成剩撤；）


    LMT(130, "LMT限价"),
    LOK(131, "LOK限价即全成否则撤"),
    FAL(132, "FAL市价剩转限价"),
    FOK(133, "FOK市价即全成否则撤"),
    FAK(134, "FAK市价即成剩撤"),


    ;


    /**
     * 类型
     */
    @Getter
    private final int Ywxw;


    /**
     * 描述
     */
    @Getter
    private final String desc;

}