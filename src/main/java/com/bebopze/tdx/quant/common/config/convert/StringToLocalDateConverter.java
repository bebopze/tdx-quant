package com.bebopze.tdx.quant.common.config.convert;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


/**
 * web层 - 日期参数   转换器
 *
 * @author: bebopze
 * @date: 2025/6/27
 */
public class StringToLocalDateConverter implements Converter<String, LocalDate> {


    private final DateTimeFormatter formatter;


    public StringToLocalDateConverter(String dateFormat) {
        this.formatter = DateTimeFormatter.ofPattern(dateFormat);
    }


    @Override
    public LocalDate convert(String source) {
        return StringUtils.isBlank(source) ? null : LocalDate.parse(source, formatter);
    }


}
