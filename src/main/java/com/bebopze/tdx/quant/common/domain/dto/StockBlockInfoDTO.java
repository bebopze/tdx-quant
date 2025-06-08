package com.bebopze.tdx.quant.common.domain.dto;

import com.bebopze.tdx.quant.common.constant.BlockTypeEnum;
import com.bebopze.tdx.quant.dal.entity.BaseBlockNewDO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 * @author: bebopze
 * @date: 2025/5/18
 */
@Data
public class StockBlockInfoDTO implements Serializable {


    private String stockCode;
    private String stockName;


    private List<BlockTypeDTO> blockDTOList;

    private List<BaseBlockNewDO> baseBlockNewDOList;


    @Data
    public static class BlockTypeDTO implements Serializable {


        private List<BlockDTO> blockDTOList;


        /**
         * tdx板块类型：1-暂无（保留）；2-普通行业-二级分类/细分行业；3-地区板块；4-概念板块；5-风格板块；12-研究行业-一级/二级/三级分类；
         */
        private Integer blockType;
        private String blockTypeDesc;


        public String getBlockType() {
            return BlockTypeEnum.getDescByType(blockType);
        }
    }


    @Data
    public static class BlockDTO implements Serializable {

        /**
         * 板块code   -   级联
         */
        private String blockCode;

        /**
         * 板块name   -   级联
         */
        private String blockName;


        /**
         * 行业级别：1-1级行业；2-2级行业；3-3级行业（细分行业）；
         */
        private Integer level;
    }

}
