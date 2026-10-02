package com.bebopze.tdx.quant.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.bebopze.tdx.quant.common.constant.tq.DividendTypeEnum;
import com.bebopze.tdx.quant.common.constant.tq.GetStockListEnum;
import com.bebopze.tdx.quant.common.constant.tq.PeriodEnum;
import com.bebopze.tdx.quant.common.domain.dto.kline.KlineDTO;
import com.bebopze.tdx.quant.common.domain.dto.trade.StockSnapshotKlineDTO;
import com.bebopze.tdx.quant.common.domain.tq.GetGbInfoDTO;
import com.bebopze.tdx.quant.common.domain.tq.SimpleStockDTO;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.common.util.NumUtil;
import com.bebopze.tdx.quant.common.util.TdxFormatCodeUtil;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/**
 * 通达信 TQ API
 *
 * @author: bebopze
 * @date: 2026/10/01
 */
@Slf4j
public class TdxTqAPI {


    public static void main(String[] args) {


//        System.out.println(get_stock_list(GetStockListEnum.A_STOCK));
//        System.out.println(get_sector_list());
//        System.out.println(get_stock_list_in_sector("880952", 1));
//
//
//        System.out.println(get_market_data("000001.SZ", PeriodEnum.DAY, LocalDate.of(2017, 1, 1), null));
//        System.out.println(get_market_snapshot("880003.SH"));
//
//
//        System.out.println(get_gb_info_by_date("000001.SZ", LocalDate.of(2017, 1, 1), null));
//        System.out.println(get_gb_info_by_date("880515.SH", LocalDate.of(2017, 1, 1), null));
//        System.out.println(get_gb_info_by_date("300783.SZ", LocalDate.of(2019, 7, 12), LocalDate.of(2026, 9, 30)));
    }


    // -------------------------------------------- 分类/板块成份股 ------------------------------------------------------


    /**
     * 获取 市场分类 成份股
     *
     * @param marketEnum 指定代码（5-全部A股；10-所有板块指数；35-所有沪深基金；102-港股；103-美股；）
     */
    public static List<SimpleStockDTO> get_stock_list(GetStockListEnum marketEnum) {


        // 获取系统分类成份股 get_stock_list
        //
        // https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhttn72svo/mindoc-1h10qo3uj48fg.html


        // 参数          是否必选     参数类型        参数说明
        // market         Y         str         指定代码（5-全部A股；10-所有板块指数；35-所有沪深基金；102-港股；103-美股；）
        // list_type      Y         int         返回数据类型（0-只返回代码；1-返回代码和名称；）


        JSONObject result = TdxTqHttpClient.call("get_stock_list",

                                                 Map.of("market", marketEnum.market,
                                                        "list_type", 1));


        String data = result.getString("Value");
        return JSON.parseArray(data, SimpleStockDTO.class);
    }


    /**
     * 获取A股 全部板块 代码列表
     */
    public static List<SimpleStockDTO> get_sector_list() {


        // 获取A股板块代码列表 get_sector_list
        //
        // https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhttn72svo/mindoc-1h10r5907noko.html


        // 参数           是否必选    参数类型    参数说明
        // list_type        Y        int      返回数据类型（0-只返回代码；1-返回代码和名称；）


        JSONObject result = TdxTqHttpClient.call("get_sector_list",

                                                 Map.of("list_type", 1));


        String data = result.getString("Value");
        return JSON.parseArray(data, SimpleStockDTO.class);
    }


    /**
     * 获取 板块 成份股
     *
     * @param block_code 板块 code/name
     * @param block_type 板块类型（block_type=0 表示传入 系统板块   code/name【880952/芯片】）
     *                   -      （block_type=1 表示传入 自定义板块 code/name【YD/月多】）
     */
    public static List<SimpleStockDTO> get_stock_list_in_sector(String block_code, int block_type) {


        // 获取板块成份股 get_stock_list_in_sector
        //
        // https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhttn72svo/mindoc-1h10r92mchgug.html


        //     参数     是否必选    参数类型       参数说明
        // block_code    Y        str         板块代码（block_type=0 表示传入 系统板块  code/name【880952/芯片】）
        // block_type    N        str         板块类型（block_type=1 表示传入 自定义板块 code/name【YD/月多】）
        // list_type     Y        int         返回数据类型（0-只返回代码；1-返回代码和名称；）


        JSONObject result = TdxTqHttpClient.call("get_stock_list_in_sector",

                                                 Map.of("block_code", TdxFormatCodeUtil.formatCode(block_code),
                                                        "block_type", block_type,
                                                        "list_type", 1));


        String data = result.getString("Value");
        return JSON.parseArray(data, SimpleStockDTO.class);
    }


    // -------------------------------------------- 行情类信息 -----------------------------------------------------------

    // https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhthaq5qmg/


    /**
     * 根据股票，获取 历史行情
     *
     * @param stockCode  证券代码
     * @param periodEnum 周期（笔/分钟/日/周/月）
     * @param start_time 起始时间
     * @param end_time   结束时间
     * @return
     */
    public static List<KlineDTO> get_market_data(String stockCode,
                                                 PeriodEnum periodEnum,
                                                 LocalDate start_time,
                                                 LocalDate end_time) {


        // 获取 历史行情     get_market_data
        //
        // https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhthaq5qmg/mindoc-1h10g60jt68sc.html


        // 参数          是否必选     参数类型            参数说明
        // stock_list     Y         List[str]     证券代码列表
        // field_list     N         List[str]     字段筛选，传空则返回全部
        // period         Y         str           周期（笔/分钟/日/周/月）
        // start_time     N         str           起始时间（年月日：yyyyMMdd）
        // end_time       N         str           结束时间（年月日：yyyyMMdd）
        // count          N         int           返回数据个数（每只股票）
        // dividend_type  N         str           复权类型：none不复权、front前复权、back后复权
        // fill_data      N         bool          是否向后填充空缺数据，多只股票一起取k线时使用


        JSONObject result = TdxTqHttpClient.call("get_market_data",

                                                 Map.of("stock_list", List.of(TdxFormatCodeUtil.formatCode(stockCode)),
                                                        "field_list", List.of(),
                                                        "period", periodEnum.period,
                                                        "start_time", DateTimeUtil.format_yyyyMMdd(start_time),
                                                        "end_time", DateTimeUtil.format_yyyyMMdd(end_time),
                                                        "count", 0,
                                                        "dividend_type", DividendTypeEnum.FRONT.type,
                                                        "fill_data", false));


        // 每日股本
        // List<GetGbInfoDTO> gbInfoDTOList = get_gb_info_by_date(stockCode, start_time, end_time);


        List<KlineDTO> dtoList = Lists.newArrayList();


        result.getJSONObject("Value")
              .forEach((code, _data) -> {
                  JSONObject data = (JSONObject) _data;


                  JSONArray date_arr = data.getJSONArray("Date");
                  JSONArray time_arr = data.getJSONArray("Time");

                  JSONArray open_arr = data.getJSONArray("Open");
                  JSONArray high_arr = data.getJSONArray("High");
                  JSONArray low_arr = data.getJSONArray("Low");
                  JSONArray close_arr = data.getJSONArray("Close");

                  JSONArray volume_arr = data.getJSONArray("Volume");
                  JSONArray amount_arr = data.getJSONArray("Amount");


                  for (int i = 0; i < close_arr.size(); i++) {
                      KlineDTO dto = new KlineDTO();

                      dto.setDate(DateTimeUtil.parseDate_yyyyMMdd(date_arr.getString(i)));
                      // dto.setTime(DateTimeUtil.parseTime_HHmmss(time_arr.getString(i)));

                      dto.setOpen(open_arr.getDouble(i));
                      dto.setHigh(high_arr.getDouble(i));
                      dto.setLow(low_arr.getDouble(i));
                      dto.setClose(close_arr.getDouble(i));

                      dto.setVol(volume_arr.getLong(i));
                      dto.setAmo(amount_arr.getDouble(i));


//                      // ---------- 股本（换手率、流通市值、总市值）
//                      GetGbInfoDTO gbDTO = gbInfoDTOList.get(i);
//
//                      LocalDate date = gbDTO.getDate();
//                      Long ltgb = gbDTO.getLtgb();
//                      Long zgb = gbDTO.getZgb();
//
//
//                      // 换手率 = (成交量 / 流通股本) * 100
//                      dto.setTurnoverPct(ltgb > 0 ? NumUtil.of(dto.getVol() / ltgb * 100) : Double.NaN);
//
//                      // 流通市值
//                      // double ltMarketValue = NumUtil.of(dto.getClose() * ltgb);
//                      // 总市值
//                      // double totalMarketValue = NumUtil.of(dto.getClose() * zgb);
//
//
//                      Assert.isTrue(date.isEqual(dto.getDate()), String.format("K线.date[%s] != 股本.date[%s]", dto.getDate(), gbDTO.getDate()));


                      dtoList.add(dto);
                  }
              });


        // LdayParser.fill_gbInfo(stockCode, dtoList);


        return dtoList;
    }


    /**
     * 根据股票，获取 实时行情（买5/卖5）
     *
     * @param stock_code 证券代码
     * @return
     */
    public static StockSnapshotKlineDTO get_market_snapshot(String stock_code) {


        // 获取 实时行情     get_market_snapshot
        //
        // https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhthaq5qmg/mindoc-1h10iig4pb6e0.html


        //     参数       是否必选       参数类型                参数说明
        // stock_code       Y            str             证券代码
        // field_list       N          List[str]         字段筛选，传空则返回全部


        JSONObject result = TdxTqHttpClient.call("get_market_snapshot",

                                                 Map.of("stock_code", TdxFormatCodeUtil.formatCode(stock_code),
                                                        "field_list", List.of()));


        // {
        //     'ItemNum': '3342',
        //     'LastClose': '34.21',
        //     'Open': '33.78',
        //     'Max': '36.49',
        //     'Min': '32.50',
        //     'Now': '35.06',
        //     'Volume': '122881',
        //     'NowVol': '1449',
        //     'Amount': '43068.48',
        //     'Inside': '60373',
        //     'Outside': '62509',
        //     'TickDiff': '0.00',
        //     'InOutFlag': '2',
        //     'Jjjz': '0.00',
        //     'Buyp': ['35.05', '35.04', '35.02', '35.01', '35.00'],
        //     'Buyv': ['154', '9', '49', '136', '154'],
        //     'Sellp': ['35.06', '35.07', '35.08', '35.09', '35.10'],
        //     'Sellv': ['4', '31', '139', '4', '4'],
        //     'UpHome': '0',
        //     'DownHome': '0',
        //     'Before5MinNow': '35.15',
        //     'Average': '35.05',
        //     'XsFlag': '2',
        //     'Zangsu': '-0.25',
        //     'ZAFPre3': '-1.83',
        //     'ErrorId': '0'
        // }


        StockSnapshotKlineDTO dto = new StockSnapshotKlineDTO();

        dto.setStockCode(stock_code);
        dto.setStockName(null);

        dto.setDate(LocalDate.now());
        dto.setOpen(NumUtil.of(result.getDouble("Open")));
        dto.setHigh(NumUtil.of(result.getDouble("Max")));
        dto.setLow(NumUtil.of(result.getDouble("Min")));
        dto.setClose(NumUtil.of(result.getDouble("Now")));
        dto.setPrevClose(NumUtil.of(result.getDouble("LastClose")));

        dto.setVol(result.getLongValue("Volume", 0L));
        dto.setAmo(NumUtil.of(result.getDouble("Amount")));


        dto.setRangePct(NumUtil.of(dto.getHigh() / dto.getLow() * 100 - 100));
        dto.setChangePrice(NumUtil.of(dto.getClose() - dto.getPrevClose()));
        dto.setChangePct(NumUtil.of(dto.getClose() / dto.getPrevClose() * 100 - 100));
        dto.setTurnoverPct(Double.NaN);


        return dto;
    }


    //


    /**
     * 根据时间段 获取 股本数据     get_gb_info_by_date（每日 股本   ->   计算每日   换手率、流通市值、总市值）
     *
     * @param stock_code 股票代码
     * @param start_date 开始日期
     * @param end_date   截止日期
     */
    public static List<GetGbInfoDTO> get_gb_info_by_date(String stock_code, LocalDate start_date, LocalDate end_date) {


        // 根据时间段 获取 股本数据     get_gb_info_by_date
        //
        // https://help.tdx.com.cn/quant/docs/markdown/mindoc-1ctuhthaq5qmg/mindoc-1hc4303vsv1fk.html


        //       参数       是否必选     参数类型     参数说明
        //   stock_code       Y          str       股票代码
        //   start_date       Y          str       开始日期
        //   end_date         Y          str       截止日期


        JSONObject result = TdxTqHttpClient.call("get_gb_info_by_date",

                                                 Map.of("stock_code", TdxFormatCodeUtil.formatCode(stock_code),
                                                        "start_date", DateTimeUtil.format_yyyyMMdd(start_date),
                                                        "end_date", DateTimeUtil.format_yyyyMMdd(end_date)));


        String data = result.getString("Value");
        return JSON.parseArray(data, GetGbInfoDTO.class);
    }

}