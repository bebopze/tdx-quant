package com.bebopze.tdx.quant.common.domain.tq;

import com.alibaba.fastjson2.annotation.JSONField;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;


/**
 * 股本数据       -       get_gb_info / get_gb_info_by_date
 *
 * @author: bebopze
 * @date: 2026/10/1
 */
@Slf4j
@Data
public class GetGbInfoDTO {

    /**
     * 日期
     */
    // @JSONField(format = "yyyyMMdd")   // 仅对 String 有效     ->     解析成 int（会被识别为 时间戳） 自动转换 失败
    // @JsonFormat(pattern = "yyyyMMdd")
    private String Date;

    /**
     * 总股本
     */
    private Long Zgb;

    /**
     * 流通股本
     */
    private Long Ltgb;


    // -----------------------------------------------------------------------------------------------------------------


    public LocalDate getDate() {
        try {
            return DateTimeUtil.parseDate_yyyyMMdd(Date);
        } catch (Exception ex) {
            log.error("parseDate_yyyyMMdd - err     >>>     Date : {}", Date);
            return null;
        }
    }

}