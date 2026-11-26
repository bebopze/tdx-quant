package com.bebopze.tdx.quant.optionbs;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 回测输入数据（内存）
 */
public class OptionBsData {

    public List<LocalDate> calendar = new ArrayList<>();
    public Map<String, OptionBsModels.Underlying> underlyings = new LinkedHashMap<>();
    public Map<String, List<OptionBsModels.Contract>> contracts = new LinkedHashMap<>();
    public TreeMap<LocalDate, Integer> statusByDate = new TreeMap<>();
    /**
     * 大盘牛熊：1=牛市；2=熊市
     */
    public TreeMap<LocalDate, Integer> bullBearByDate = new TreeMap<>();
    public TreeMap<LocalDate, OptionBsModels.Signal> signalMeta = new TreeMap<>();
    public OptionBsModels.Underlying benchmark;
    public LocalDate firstOptionDate;
    public LocalDate lastOptionDate;
}
