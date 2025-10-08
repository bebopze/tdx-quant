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
     * 主线板块（板块-月多2：月多 + RPS红 + SSF多）     [机选]
     */
    @TableField("top_block_code_set")
    @Schema(description = "主线板块（板块-月多2：月多 + RPS红 + SSF多）     [机选]")
    private String topBlockCodeSet;

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
     * 股票池-平均涨跌幅（%）     [机选]
     */
    @TableField("stock_avg_pct")
    @Schema(description = "股票池-平均涨跌幅（%）     [机选]")
    private String stockAvgPct;

    /**
     * 主线板块     [人选]
     */
    @TableField("top_block_code_set_man")
    @Schema(description = "主线板块     [人选]")
    private String topBlockCodeSetMan;

    /**
     * 主线个股     [人选]
     */
    @TableField("top_stock_code_set_man")
    @Schema(description = "主线个股     [人选]")
    private String topStockCodeSetMan;

    /**
     * 板块池-平均涨跌幅（%）     [人选]
     */
    @TableField("block_avg_pct_man")
    @Schema(description = "板块池-平均涨跌幅（%）     [人选]")
    private String blockAvgPctMan;

    /**
     * 股票池-平均涨跌幅（%）     [人选]
     */
    @TableField("stock_avg_pct_man")
    @Schema(description = "股票池-平均涨跌幅（%）     [人选]")
    private String stockAvgPctMan;

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
        String topBlockCodeSet_type = type == 1 ? topBlockCodeSet : topBlockCodeSetMan;
        return JSON.parseArray(topBlockCodeSet_type, TopChangePctDTO.class);
    }

    public List<TopChangePctDTO> getTopStockList(int type) {
        String topStockCodeSet_type = type == 1 ? topStockCodeSet : topStockCodeSetMan;
        return JSON.parseArray(topStockCodeSet_type, TopChangePctDTO.class);
    }


    public TopPoolAvgPctDTO getTopBlockAvgPct(int type) {
        String blockAvgPct_type = type == 1 ? blockAvgPct : blockAvgPctMan;
        return JSON.to(TopPoolAvgPctDTO.class, blockAvgPct_type);
    }

    public TopPoolAvgPctDTO getTopStockAvgPct(int type) {
        String stockAvgPct_type = type == 1 ? stockAvgPct : stockAvgPctMan;
        return JSON.to(TopPoolAvgPctDTO.class, stockAvgPct_type);
    }


    // -----------------------------------------------------------------------------------------------------------------


//    public List<TopChangePctDTO> getManTopBlockList() {
//        return JSON.parseArray(topBlockCodeSetMan, TopChangePctDTO.class);
//    }
//
//    public List<TopChangePctDTO> getManTopStockList() {
//        return JSON.parseArray(topStockCodeSetMan, TopChangePctDTO.class);
//    }
//
//
//    public TopPoolAvgPctDTO getManTopBlockAvgPct() {
//        return JSON.to(TopPoolAvgPctDTO.class, blockAvgPctMan);
//    }
//
//    public TopPoolAvgPctDTO getManTopStockAvgPct() {
//        return JSON.to(TopPoolAvgPctDTO.class, stockAvgPctMan);
//    }


    // -----------------------------------------------------------------------------------------------------------------


    public Set<String> getTopBlockCodeJsonSet(int type) {
        String topBlockCodeSet_type = type == 1 ? topBlockCodeSet : topBlockCodeSetMan;
        return JSON.parseArray(topBlockCodeSet_type, TopChangePctDTO.class)
                   .stream()
                   .map(TopChangePctDTO::getCode)
                   .collect(Collectors.toSet());
    }


    public Set<String> getTopStockCodeJsonSet(int type) {
        String topStockCodeSet_type = type == 1 ? topStockCodeSet : topStockCodeSetMan;
        return JSON.parseArray(topStockCodeSet_type, TopChangePctDTO.class)
                   .stream()
                   .map(TopChangePctDTO::getCode)
                   .collect(Collectors.toSet());
    }


    // -----------------------------------------------------------------------------------------------------------------


//    public Set<String> getManTopBlockCodeJsonSet() {
//        return JSON.parseArray(topBlockCodeSetMan, TopChangePctDTO.class)
//                   .stream()
//                   .map(TopChangePctDTO::getCode)
//                   .collect(Collectors.toSet());
//    }
//
//
//    public Set<String> getManTopStockCodeJsonSet() {
//        return JSON.parseArray(topStockCodeSetMan, TopChangePctDTO.class)
//                   .stream()
//                   .map(TopChangePctDTO::getCode)
//                   .collect(Collectors.toSet());
//    }


}