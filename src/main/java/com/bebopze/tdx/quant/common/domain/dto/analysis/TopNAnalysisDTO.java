package com.bebopze.tdx.quant.common.domain.dto.analysis;

import com.bebopze.tdx.quant.common.domain.dto.topblock.TopBlockDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopStockDTO;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.glassfish.jersey.internal.guava.Sets;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;


/**
 * TOP100 买点/形态/...统计
 *
 * @author: bebopze
 * @date: 2025/10/27
 */
@Data
@AllArgsConstructor
public class TopNAnalysisDTO {


    /**
     * 股票代码
     */
    private String code;
    /**
     * 股票名称
     */
    private String name;


    /**
     * 均线形态
     */
    private String 均线形态;

    /**
     * 支撑线
     */
    private Integer 支撑线;

    /**
     * 买点列表
     */
    private List<String> buySignalList = Lists.newArrayList();

    /**
     * 成交额
     */
    private double AMO;


    /**
     * C_MA偏离率
     */
    private double C_MA偏离率;

    /**
     * C_SSF偏离率
     */
    private double C_SSF偏离率;


    private List<LocalDate> dateList = Lists.newArrayList();
    private List<Double> pctList = Lists.newArrayList();
    private Set<String> blockCodeSet = Sets.newHashSet();
    private List<TopStockDTO.TopBlock> topBlockList = Lists.newArrayList();
    private List<TopBlockDTO.TopStock> topStockList = Lists.newArrayList();


    // -----------------------------------------------------------------------------------------------------------------


}