package com.bebopze.tdx.quant.common.domain.dto.analysis;

import com.bebopze.tdx.quant.common.domain.dto.topblock.TopBlockDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopStockDTO;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.glassfish.jersey.internal.guava.Sets;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;


/**
 * 上榜次数 统计
 *
 * @author: bebopze
 * @date: 2025/10/27
 */
@Data
@AllArgsConstructor
public class TopCountDTO {


    /**
     * 股票代码
     */
    private String code;
    /**
     * 股票名称
     */
    private String name;

    /**
     * 上榜次数
     */
    private int count;

    /**
     * 上榜涨幅（%）
     */
    private double pct;
    private double avgPct;


    private List<LocalDate> dateList = Lists.newArrayList();
    private List<Double> pctList = Lists.newArrayList();
    private Set<String> blockCodeSet = Sets.newHashSet();
    private List<TopStockDTO.TopBlock> topBlockList = Lists.newArrayList();
    private List<TopBlockDTO.TopStock> topStockList = Lists.newArrayList();


    // -----------------------------------------------------------------------------------------------------------------


    public TopCountDTO(String code, String name, int count, double pct, LocalDate date) {
        this.code = code;
        this.name = name;
        this.count = count;
        this.pct = pct;
        this.pctList.add(NumUtil.of(pct * 100));
        this.dateList.add(date);
    }


}