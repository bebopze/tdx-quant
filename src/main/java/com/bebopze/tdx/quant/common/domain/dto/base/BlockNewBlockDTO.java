package com.bebopze.tdx.quant.common.domain.dto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;


/**
 * @author: bebopze
 * @date: 2025/5/22
 */
@Data
public class BlockNewBlockDTO implements Serializable {


    /**
     * 主键ID
     */
    @Schema(description = "主键ID")
    private Long id;

    /**
     * 父-ID（行业板块）
     */
    @Schema(description = "父-ID（行业板块）")
    private Long parentId;

    /**
     * 行业级别：1-1级行业；2-2级行业；3-3级行业（细分行业）；
     */
    @Schema(description = "行业级别：1-1级行业；2-2级行业；3-3级行业（细分行业）；")
    private Integer level;

    /**
     * 是否最后一级：0-否；1-是；
     */
    @Schema(description = "是否最后一级：0-否；1-是；")
    private Integer endLevel;

    /**
     * tdx板块类型：1-暂无（保留）；2-普通行业-二级分类/细分行业；3-地区板块；4-概念板块；5-风格板块；12-研究行业-一级/二级/三级分类；
     */
    @Schema(description = "tdx板块类型：1-暂无（保留）；2-普通行业-二级分类/细分行业；3-地区板块；4-概念板块；5-风格板块；12-研究行业-一级/二级/三级分类；")
    private Integer type;

    /**
     * 板块代码
     */
    @Schema(description = "板块代码")
    private String code;

    /**
     * 板块名称
     */
    @Schema(description = "板块名称")
    private String name;

}