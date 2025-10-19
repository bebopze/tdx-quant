package com.bebopze.tdx.quant.dal.entity;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bebopze.tdx.quant.common.constant.TopTypeEnum;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopChangePctDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopPoolAvgPctDTO;
import com.google.common.collect.Sets;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


/**
 * <p>
 * 量化分析 - LV3主线板块（板块-月多2）               // 主线板块  ->  主线个股
 * </p>
 *
 * @author bebopze
 * @since 2025-09-24
 */
@Getter
@Setter
@ToString
@TableName("qa_top_block")
@Schema(name = "QaTopBlockDO", description = "量化分析 - LV3主线板块（板块-月多2）")
public class QaTopBlockDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 日期
     */
    @TableField("date")
    @Schema(description = "日期")
    private LocalDate date;

    /**
     * 主线板块（板块-月多2：月多 + RPS红 + SSF多）     [机选]
     */
    @TableField("top_block_code_set")
    @Schema(description = "主线板块（板块-月多2：月多 + RPS红 + SSF多）     [机选]")
    private String topBlockCodeSet;

    /**
     * 主线ETF（板块-月多2：月多 + RPS红 + SSF多）     [机选]
     */
    @TableField("top_etf_code_set")
    @Schema(description = "主线ETF（板块-月多2：月多 + RPS红 + SSF多）     [机选]")
    private String topEtfCodeSet;

    /**
     * 主线个股（N100日新高 + 月多 + IN主线）     [机选]
     */
    @TableField("top_stock_code_set")
    @Schema(description = "主线个股（N100日新高 + 月多 + IN主线）     [机选]")
    private String topStockCodeSet;

    /**
     * 板块池-平均涨跌幅（%）     [机选]
     */
    @TableField("block_avg_pct")
    @Schema(description = "板块池-平均涨跌幅（%）     [机选]")
    private String blockAvgPct;

    /**
     * ETF池-平均涨跌幅（%）     [机选]
     */
    @TableField("etf_avg_pct")
    @Schema(description = "ETF池-平均涨跌幅（%）     [机选]")
    private String etfAvgPct;

    /**
     * 股票池-平均涨跌幅（%）     [机选]
     */
    @TableField("stock_avg_pct")
    @Schema(description = "股票池-平均涨跌幅（%）     [机选]")
    private String stockAvgPct;

    /**
     * 创建时间
     */
    @TableField("gmt_create")
    @Schema(description = "创建时间")
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @TableField("gmt_modify")
    @Schema(description = "更新时间")
    private LocalDateTime gmtModify;


    // -----------------------------------------------------------------------------------------------------------------


    public List<TopChangePctDTO> getTopBlockList(int type) {
        return getTopList(topBlockCodeSet, type);
    }

    public List<TopChangePctDTO> getTopEtfList(int type) {
        return getTopList(topEtfCodeSet, type);
    }

    public List<TopChangePctDTO> getTopStockList(int type) {
        return getTopList(topStockCodeSet, type);
    }


    private List<TopChangePctDTO> getTopList(String topCodeSet, int type) {
        Set<Integer> typeSet = typeSet(type);

        return JSON.parseArray(topCodeSet, TopChangePctDTO.class)
                   .stream()
                   .filter(e -> typeSet.contains(e.getType()) && e.getIsDel() == 0)
                   .collect(Collectors.toList());
    }


    // -----------------------------------------------------------------------------------------------------------------


    public TopPoolAvgPctDTO getTopBlockAvgPct(int type) {
        return getTopAvgPct(blockAvgPct, type);
    }

    public TopPoolAvgPctDTO getTopEtfAvgPct(int type) {
        return getTopAvgPct(etfAvgPct, type);
    }

    public TopPoolAvgPctDTO getTopStockAvgPct(int type) {
        return getTopAvgPct(stockAvgPct, type);
    }


    private TopPoolAvgPctDTO getTopAvgPct(String avgPct, int type) {
        return JSON.parseArray(StringUtils.defaultString(avgPct, "[]"), TopPoolAvgPctDTO.class)
                   .stream()
                   .filter(e -> e.getType() == type)
                   .findFirst()
                   .map(e -> {
                       e.setDate(date);
                       return e;
                   })
                   .orElse(null);
    }


    // -----------------------------------------------------------------------------------------------------------------


    public Set<String> getTopBlockCodeJsonSet(int type) {
        return getTopCodeJsonSet(topBlockCodeSet, type);
    }

    public Set<String> getTopEtfCodeJsonSet(int type) {
        return getTopCodeJsonSet(topEtfCodeSet, type);
    }

    public Set<String> getTopStockCodeJsonSet(int type) {
        return getTopCodeJsonSet(topStockCodeSet, type);
    }


    private Set<String> getTopCodeJsonSet(String topCodeSet, int type) {
        Set<Integer> typeSet = typeSet(type);

        return JSON.parseArray(topCodeSet, TopChangePctDTO.class)
                   .stream()
                   .filter(e -> typeSet.contains(e.getType()) && e.getIsDel() == 0)
                   .map(TopChangePctDTO::getCode)
                   .collect(Collectors.toSet());
    }


    // -----------------------------------------------------------------------------------------------------------------


    private Set<Integer> typeSet(int type) {
        Set<Integer> typeSet = Sets.newHashSet(type);

        if (type == TopTypeEnum.AUTO.type) {
            typeSet.add(TopTypeEnum.AUTO.type);
        } else if (type == TopTypeEnum.MANUAL.type) {
            typeSet.add(TopTypeEnum.AUTO.type);
            typeSet.add(TopTypeEnum.MANUAL.type);
        }

        return typeSet;
    }


}