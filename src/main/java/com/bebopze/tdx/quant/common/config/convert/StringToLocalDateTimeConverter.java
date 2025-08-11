package com.bebopze.tdx.quant.common.config.convert;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.convert.converter.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


/**
 * web层 - 时间参数   转换器
 *
 * @author: bebopze
 * @date: 2025/6/27
 */
public class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {


    private final DateTimeFormatter formatter;


    public StringToLocalDateTimeConverter(String dateFormat) {
        this.formatter = DateTimeFormatter.ofPattern(dateFormat);
    }


    @Override
    public LocalDateTime convert(String source) {
         return StringUtils.isBlank(source) ? null : LocalDateTime.parse(source, formatter);
    }


}
