package com.bebopze.tdx.quant.common.domain.kline;

import com.bebopze.tdx.quant.common.convert.ConvertStockKline;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;


/**
 * 个股 - 历史行情          日/周/月/季/半年/年
 *
 * @author: bebopze
 * @date: 2025/5/14
 */
@Data
public class StockKlineHisResp implements Serializable {


    //   code: "300059",
    //   market: 0,
    //   name: "东方财富",
    //   decimal: 2,
    //   dktotal: 3613,
    //   preKPrice: 0.2,
    //   prePrice: 21.72,
    //   qtMiscType: 7,
    //   version: 0,
    //
    // klines: [
    //     "2010-03-19,0.46,0.43,0.48,0.42,197373,1182393994.00,30.00,115.00,0.23,70.49",
    //     "2010-03-22,0.46,0.50,0.50,0.45,110104,693595698.00,11.63,16.28,0.07,39.32",
    //     "2010-03-23,0.49,0.51,0.52,0.48,85522,547135876.00,8.00,2.00,0.01,30.54"
    // ]


    // 证券代码
    private String code;
    private String market;
    // 证券名称
    private String name;
    // 小数位数
    private String decimal;
    // klines - 总数           （有bug，不可用       日/周/月 -> 全部返回的 日K 总数）
    private String dktotal;
    //
    private String preKPrice;
    // 昨日-收盘价
    private String prePrice;
    // xx类型（7-日线 ？？？）
    private String qtMiscType;
    //
    private String version;


    // 2025-08-01,23.20,23.17,23.47,23.06,2666164,6196831583.57,1.76,-0.26,-0.06,1.99

    // 日期,O,C,H,L,VOL,AMO,振幅,涨跌幅,涨跌额,换手率
    private List<String> klines;


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * 东方财富 klines（O,C,H,L）      ->       base_stock - kline_his（O,H,L,C）
     *
     * @return
     */
    public List<String> getKlines() {

        return klines.stream().map(k -> {

                         String[] arr = k.split("\\,");


                         // O,C,H,L
                         String O = arr[1];
                         String C = arr[2];
                         String H = arr[3];
                         String L = arr[4];


                         // 2025-05-13,21.06,21.97,20.89,21.45,8455131,18181107751.03,5.18,2.98,0.62,6.33
                         // 日期,O,H,L,C,VOL,AMO,振幅,涨跌幅,涨跌额,换手率


                         // O,H,L,C
                         arr[1] = O;
                         arr[2] = H;
                         arr[3] = L;
                         arr[4] = C;

                         return String.join(",", arr);
                     })
                     .collect(Collectors.toList());
    }


    /**
     * klines   ->   dtoList
     *
     * @return
     */
    public List<KlineDTO> klineDTOList() {
        List<String> klines = getKlines();
        return ConvertStockKline.klines2DTOList(klines);
    }


}