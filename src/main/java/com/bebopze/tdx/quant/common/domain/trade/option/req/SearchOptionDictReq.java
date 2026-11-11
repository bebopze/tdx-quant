package com.bebopze.tdx.quant.common.domain.trade.option.req;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * ETF期权 - 合约检索
 *
 * @author: bebopze
 * @date: 2026/11/11
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchOptionDictReq {


    // 标的证券code（588000-科创50ETF；159915-创业板ETF；510500-中证500ETF；）
    private String targetCode;


    // 合约类型：C-认购；P-认沽；
    private String callOrPut;


    // 合约编码（支持 [合约编码/合约名称] 关键词 模糊搜索：10011565/科创50购12月1900）
    private String id;


}