package com.bebopze.tdx.quant.common.domain.trade.option.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * ETF期权 - 当日委托单
 *
 * @author: bebopze
 * @date: 2026/11/7
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OptionGetTodayOrdersReq {


    /**
     * 页面大小
     */
    private int qqhs = 500;

    /**
     * 当前页码
     */
    private int dwc = 1;

}