package com.bebopze.tdx.quant.common.domain.trade.req;

import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import lombok.Data;

import java.time.LocalDate;


/**
 * 历史委托 列表
 *
 * @author: bebopze
 * @date: 2025/9/13
 */
@Data
public class QueryCreditHisOrderV2Req {


    // st=20250911
    // et=20250911
    // qqhs=20
    // dwc=20250911|744750
    // qryFlag=0


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
     * 上一页 最后一条   Dwc字段值（Dwc : "20250911|625958"）
     */
    private String dwc;


    private int qryFlag = 0;


    // -----------------------------------------------------------------------------------------------------------------


    public String getSt() {
        return DateTimeUtil.format_yyyyMMdd(st);
    }

    public String getEt() {
        return DateTimeUtil.format_yyyyMMdd(et);
    }

}