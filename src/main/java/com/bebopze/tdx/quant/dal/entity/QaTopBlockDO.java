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
import java.util.Map;
import java.util.Objects;
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


    public List<TopChangePctDTO> getTopStockList(int type, int stockType) {
        if (stockType == 1) {
            return getTopStockList(type);
        } else if (stockType == 2) {
            return getTopEtfList(type);
        }
        return getTopStockList(type);
    }


    private List<TopChangePctDTO> getTopList(String topCodeSet, int type) {
        return JSON.parseArray(Objects.toString(topCodeSet, "[]"), TopChangePctDTO.class)
                   .stream()
                   .filter(e -> e.getTopTypeSet().contains(type))
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


    public TopPoolAvgPctDTO getTopStockAvgPct(int type, int stockType) {
        if (stockType == 1) {
            return getTopStockAvgPct(type);
        } else if (stockType == 2) {
            return getTopEtfAvgPct(type);
        }
        return getTopStockAvgPct(type);
    }


    private TopPoolAvgPctDTO getTopAvgPct(String avgPct, int type) {
        return JSON.parseArray(Objects.toString(avgPct, "[]"), TopPoolAvgPctDTO.class)
                   .stream()
                   .filter(e -> e.getType() == type)
                   .findFirst()
                   .map(e -> {
                       e.setDate(date);
                       return e;
                   })
                   .orElse(new TopPoolAvgPctDTO(type));
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


    public Set<String> getTopStockCodeJsonSet(int type, int stockType) {
        if (stockType == 1) {
            return getTopStockCodeJsonSet(type);
        } else if (stockType == 2) {
            return getTopEtfCodeJsonSet(type);
        }
        return getTopStockCodeJsonSet(type);
    }


    private Set<String> getTopCodeJsonSet(String topCodeSet, int type) {
        return JSON.parseArray(Objects.toString(topCodeSet, "[]"), TopChangePctDTO.class)
                   .stream()
                   .filter(e -> e.getTopTypeSet().contains(type))
                   .map(TopChangePctDTO::getCode)
                   .collect(Collectors.toSet());
    }


    // -----------------------------------------------------------------------------------------------------------------


    public Map<String, String> getTopBlockCodeNameMap(int type) {
        return getTopCodeNameMap(topBlockCodeSet, type);
    }

    public Map<String, String> getTopEtfCodeNameMap(int type) {
        return getTopCodeNameMap(topEtfCodeSet, type);
    }

    public Map<String, String> getTopStockCodeNameMap(int type) {
        return getTopCodeNameMap(topStockCodeSet, type);
    }


    private Map<String, String> getTopCodeNameMap(String topCodeSet, int type) {
        return JSON.parseArray(Objects.toString(topCodeSet, "[]"), TopChangePctDTO.class)
                   .stream()
                   .filter(e -> e.getTopTypeSet().contains(type))
                   .collect(Collectors.toMap(TopChangePctDTO::getCode,
                                             e -> e.getName() == null ? "" : e.getName()
                   ));
    }


}