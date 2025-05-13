package com.bebopze.tdx.quant.common.domain.kline;


import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 * 个股 - 历史行情          周K
 *
 * @author: bebopze
 * @date: 2025/5/14
 */
@Data
public class StockKlineWeekResp implements Serializable {


    //   code: "300059",
    //   market: 0,
    //
    //   records: [
    //      {
    //         date: "2010-07-09",
    //         type: 1,
    //         pxbl: 0.2,
    //         sgbl: 0,
    //         cxbl: 0,
    //         pgbl: 0,
    //         pgjg: 0,
    //         pghg: 0,
    //         zfbl: 0,
    //         zfgs: 0,
    //         zfjg: 0,
    //         ggflag: 0,
    //         zzbl: 0
    //      }
    //   ]


    // 证券代码
    private String code;
    private String market;


    // ----------------------------------------------------


    private List<Record> records;


    @Data
    public static class Record implements Serializable {
        private String date;
        private String type;
        private String pxbl;
        private String sgbl;
        private String cxbl;
        private String pgbl;
        private String pgjg;
        private String pghg;
        private String zfbl;
        private String zfgs;
        private String zfjg;
        private String ggflag;
        private String zzbl;
    }

}
