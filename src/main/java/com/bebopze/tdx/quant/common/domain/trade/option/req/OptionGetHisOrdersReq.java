package com.bebopze.tdx.quant.common.domain.trade.option.req;

import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import lombok.Data;

import java.time.LocalDate;


/**
 * ETF期权 - 历史委托
 *
 * @author: bebopze
 * @date: 2026/11/7
 */
@Data
public class OptionGetHisOrdersReq {


    // st=20261105
    // et=20261106
    // qqhs=20
    // dwc=20


    /**
     * 开始日期
     */
    private LocalDate st;

    /**
     * 结束日期
     */
    private LocalDate et;

    /**
     * 页面大小
     */
    private int qqhs = 1000;


    /**
     * 页码
     */
    private transient int page = 1;


    /**
     * 最大条数 = 页面大小 x 当前页码
     */
    private int dwc = qqhs * page;


    // -----------------------------------------------------------------------------------------------------------------


    public String getSt() {
        return DateTimeUtil.format_yyyyMMdd(st);
    }

    public String getEt() {
        return DateTimeUtil.format_yyyyMMdd(et);
    }


    public int getDwc() {
        return qqhs * page;
    }


}