package com.bebopze.tdx.quant.service;

import java.util.List;
import java.util.Map;

/**
 * @author: bebopze
 * @date: 2025/5/7
 */
public interface TdxDataParserService {


    void tdxData();

    Map<String, List<String>> marketRelaStockCodePrefixList();
}
