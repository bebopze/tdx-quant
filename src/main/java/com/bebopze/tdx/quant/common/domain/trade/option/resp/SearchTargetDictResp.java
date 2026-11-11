package com.bebopze.tdx.quant.common.domain.trade.option.resp;

import lombok.Data;


/**
 * ETF期权 - ETF标的证券 列表
 *
 * @author: bebopze
 * @date: 2026/11/12
 */
@Data
public class SearchTargetDictResp {


    // [
    //  {
    //    "um": "1",
    //    "us": "588000",
    //    "un": "科创50ETF华夏",
    //    "ut": "EBS"
    //  }
    // ]


    // ETF标的证券 市场类型：0-深交所；1-上交所；
    private String um;

    // 标的证券code（588000）
    private String us;

    // 标的证券name（科创50ETF华夏）
    private String un;

    // EBS
    private String ut;


}