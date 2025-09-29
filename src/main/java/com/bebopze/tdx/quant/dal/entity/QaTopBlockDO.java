package com.bebopze.tdx.quant.dal.entity;

import com.alibaba.fastjson2.JSON;
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


    public Set<String> getTopBlockCodeJsonSet() {

        return JSON.parseArray(topBlockCodeSet, String.class)
                   .stream()
                   .map(code -> {

                       if (code.length() < 6) {
                           // 保证6位补零（反序列化 bug ： 002755   ->   2755）
                           code = String.format("%06d", Integer.parseInt(code));
                       }

                       return code;
                   })
                   .collect(Collectors.toSet());
    }


    public Set<String> getTopStockCodeJsonSet() {

        return JSON.parseArray(topStockCodeSet, String.class)
                   .stream()
                   .map(code -> {

                       if (code.length() < 6) {
                           // 保证6位补零（反序列化 bug ： 002755   ->   2755）
                           code = String.format("%06d", Integer.parseInt(code));
                       }

                       return code;
                   })
                   .collect(Collectors.toSet());
    }


}