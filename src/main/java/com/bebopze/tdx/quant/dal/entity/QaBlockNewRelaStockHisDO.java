package com.bebopze.tdx.quant.dal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 量化分析 - 每日
 * </p>
 *
 * @author bebopze
 * @since 2025-07-14
 */
@Getter
@Setter
@ToString
@TableName("qa_block_new_rela_stock_his")
@Schema(name = "QaBlockNewRelaStockHisDO", description = "量化分析 - 每日")
public class QaBlockNewRelaStockHisDO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 自定义板块ID
     */
    @TableField("block_new_id")
    @Schema(description = "自定义板块ID")
    private Long blockNewId;

    /**
     * 日期
     */
    @TableField("date")
    @Schema(description = "日期")
    private LocalDate date;

    /**
     * 关联ID列表：股票ID/板块ID/指数ID（逗号分隔）
     */
    @TableField("stock_id_list")
    @Schema(description = "关联ID列表：股票ID/板块ID/指数ID（逗号分隔）")
    private String stockIdList;

    /**
     * 关联ID类型：1-个股；2-板块；3-指数；
     */
    @TableField("type")
    @Schema(description = "关联ID类型：1-个股；2-板块；3-指数；")
    private Integer type;

    /**
     * 概念板块-分析结果JSON
     */
    @TableField("gn_result")
    @Schema(description = "概念板块-分析结果JSON")
    private String gnResult;

    /**
     * 一级研究行业-分析结果JSON
     */
    @TableField("yjhy_lv1_result")
    @Schema(description = "一级研究行业-分析结果JSON")
    private String yjhyLv1Result;

    /**
     * 二级研究行业-分析结果JSON
     */
    @TableField("yjhy_lv2_result")
    @Schema(description = "二级研究行业-分析结果JSON")
    private String yjhyLv2Result;

    /**
     * 三级研究行业-分析结果JSON
     */
    @TableField("yjhy_lv3_result")
    @Schema(description = "三级研究行业-分析结果JSON")
    private String yjhyLv3Result;

    /**
     * 一级普通行业-分析结果JSON
     */
    @TableField("pthy_lv1_result")
    @Schema(description = "一级普通行业-分析结果JSON")
    private String pthyLv1Result;

    /**
     * 二级普通行业-分析结果JSON
     */
    @TableField("pthy_lv2_result")
    @Schema(description = "二级普通行业-分析结果JSON")
    private String pthyLv2Result;

    /**
     * 三级普通行业-分析结果JSON
     */
    @TableField("pthy_lv3_result")
    @Schema(description = "三级普通行业-分析结果JSON")
    private String pthyLv3Result;

    /**
     * 汇总-分析结果JSON（一级研究行业 + 概念板块）
     */
    @TableField("result")
    @Schema(description = "汇总-分析结果JSON（一级研究行业 + 概念板块）")
    private String result;

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
}
