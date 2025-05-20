package com.bebopze.tdx.quant.common.domain.kline;

import com.alibaba.fastjson2.JSONObject;
import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 * 个股行情     -     分时
 *
 * @author: bebopze
 * @date: 2025/5/14
 */
@Data
public class StockKlineTrendResp implements Serializable {


    //   {
    //        "code": "300059",
    //        "market": 0,
    //        "type": 80,
    //        "status": 0,
    //        "name": "东方财富",
    //        "decimal": 2,
    //        "preSettlement": 0,
    //        "preClose": 21.72,
    //        "beticks": "33300|34200|54000|34200|41400|46800|54000",
    //        "trendsTotal": 241,
    //        "time": 1747121667,
    //        "kind": 1,
    //        "prePrice": 21.72,
    //
    //
    //         "tradePeriods": {
    //            "pre": {
    //                "b": 202505130915,
    //                "e": 202505130930
    //            },
    //            "after": {
    //                "b": 202505131500,
    //                "e": 202505131530
    //            },
    //            "periods": [
    //                {
    //                    "b": 202505130930,
    //                    "e": 202505131130
    //                },
    //                {
    //                    "b": 202505131300,
    //                    "e": 202505131500
    //                }
    //            ]
    //        },
    //
    //
    //        "hisPrePrices": : [
    //            {
    //                "date": 20250513,
    //                "prePrice": 21.72
    //            }
    //        ],
    //
    //
    //        "trends": [
    //              "2025-05-13 09:30,21.90,21.90,21.90,21.90,99778,218513820.00,21.900",
    //
    //              "2025-05-13 15:00,21.45,21.45,21.45,21.45,52079,111709884.00,21.622"
    //        ]
    //
    //
    //    }


    private String code;
    private String market;
    private String type;
    private String status;
    private String name;
    private String decimal;
    private String preSettlement;
    private String preClose;
    private String beticks;
    private String trendsTotal;
    private String time;
    private String kind;
    private String prePrice;


    // private JSONObject tradePeriods;


    // private JSONObject hisPrePrices;

    private List<String> trends;


}