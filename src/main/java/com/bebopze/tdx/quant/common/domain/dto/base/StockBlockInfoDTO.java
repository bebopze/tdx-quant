package com.bebopze.tdx.quant.common.domain.dto.base;

import com.bebopze.tdx.quant.common.constant.BlockTypeEnum;
import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 * 个股 - 板块
 *
 * @author: bebopze
 * @date: 2025/5/18
 */
@Data
public class StockBlockInfoDTO implements Serializable {


    private String stockCode;
    private String stockName;


    // --------------------------------------------


    private List<BlockDTO> hyBlockDTOList;


    private List<BlockDTO> gnBlockDTOList;


    private List<BaseBlockNewDO> blockNewDOList;


    // ----------------------------------------------------------------------------------------


    @Data
    public static class BlockDTO implements Serializable {


        /**
         * tdx板块类型：1-暂无（保留）；2-普通行业-二级分类/细分行业；3-地区板块；4-概念板块；5-风格板块；12-研究行业-一级/二级/三级分类；
         */
        private Integer blockType;
        private String blockTypeDesc;


        /**
         * 板块code   -   级联
         */
        private String blockCodePath;

        /**
         * 板块name   -   级联
         */
        private String blockNamePath;


        /**
         * 行业级别：1-1级行业；2-2级行业；3-3级行业（细分行业）；
         */
        private Integer level;

        private Integer endLevel;


        // --------------------------------------------


        public String getBlockTypeDesc() {
            return BlockTypeEnum.getDescByType(blockType);
        }
    }

}
