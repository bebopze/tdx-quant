package com.bebopze.tdx.quant.dal.entity;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopChangePctDTO;
import com.bebopze.tdx.quant.common.domain.dto.topblock.TopPoolAvgPctDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

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
     * 主线板块（板块-月多2：月多 + RPS红 + SSF多）
     */
    @TableField("top_block_code_set")
    @Schema(description = "主线板块（板块-月多2：月多 + RPS红 + SSF多）")
    private String topBlockCodeSet;

    /**
     * 主线个股（N100日新高 + 月多 + IN主线）
     */
    @TableField("top_stock_code_set")
    @Schema(description = "主线个股（N100日新高 + 月多 + IN主线）")
    private String topStockCodeSet;

    /**
     * 板块池-平均涨跌幅（%）
     */
    @TableField("block_avg_pct")
    @Schema(description = "板块池-平均涨跌幅（%）")
    private String blockAvgPct;

    /**
     * 股票池-平均涨跌幅（%）
     */
    @TableField("stock_avg_pct")
    @Schema(description = "股票池-平均涨跌幅（%）")
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


    public List<TopChangePctDTO> getTopBlockList() {
        return JSON.parseArray(topBlockCodeSet, TopChangePctDTO.class);
    }

    public List<TopChangePctDTO> getTopStockList() {
        return JSON.parseArray(topStockCodeSet, TopChangePctDTO.class);
    }



    public TopPoolAvgPctDTO getTopBlockAvgPct() {
        return JSON.to(TopPoolAvgPctDTO.class, blockAvgPct);
    }

    public TopPoolAvgPctDTO getTopStockAvgPct() {
        return JSON.to(TopPoolAvgPctDTO.class, stockAvgPct);
    }


    // -----------------------------------------------------------------------------------------------------------------


    public Set<String> getTopBlockCodeJsonSet() {
        return JSON.parseArray(topBlockCodeSet, TopChangePctDTO.class)
                   .stream()
                   .map(TopChangePctDTO::getCode)
                   .collect(Collectors.toSet());
    }


    public Set<String> getTopStockCodeJsonSet() {
        return JSON.parseArray(topStockCodeSet, TopChangePctDTO.class)
                   .stream()
                   .map(TopChangePctDTO::getCode)
                   .collect(Collectors.toSet());
    }


}