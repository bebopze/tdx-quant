package com.bebopze.tdx.quant.common.domain.trade.option.resp;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.google.common.collect.Lists;
import lombok.Data;

import java.util.List;


/**
 * ETF期权 - 撤单结果
 *
 * @author: bebopze
 * @date: 2026/11/11
 */
@Data
public class OptionRevokeOrderResultDTO {


    // 合同编号
    private String Htxh;
    // 交易所（05-深交所；15-上交所；）
    private String Jybk;
    // 委托价格
    private String Wtjg;
    // 委托ph
    private String Wtph;
    // 委托数量
    private int Wtsl;
    // 交易类型（400-买入开仓；401-卖出平仓；402-卖出开仓；403-买入平仓；）
    private String Zqyw;
    // 报价方式
    private String Zqywxw;


    // --------------------------------


    // 0：成功；-1：失败；
    private int Errcode;
    // 失败原因
    private String Errmsg;
    private String Id;
    // 成功撤单数量
    private int Recsum;
    private int Type;


    // -------------------------------- 自定义字段


    /**
     * 撤单结果：true-成功；false-失败；
     */
    private boolean suc;


    // -----------------------------------------------------------------------------------------------------------------


    // [
    //  {
    //    "Data": [
    //      {
    //        "Htxh": "9000020953",
    //        "Jybk": "15",
    //        "Wtjg": "0.0001",
    //        "Wtph": "20",
    //        "Wtsl": "1",
    //        "Zqyw": "400",
    //        "Zqywxw": "130"
    //      }
    //    ],
    //    "Errcode": 0,
    //    "Errmsg":"CODE:309164,MSG:重复撤单",
    //    "Id": -1,
    //    "Recsum": 1,
    //    "Type": 30012
    //  }
    // ]


    /**
     * result_Data_arr   ->   平铺
     *
     * @param data
     * @return
     */
    public static List<OptionRevokeOrderResultDTO> convert(JSONArray data) {


        List<OptionRevokeOrderResultDTO> dtoList = Lists.newArrayList();


        data.forEach(e -> {

            JSONObject json = (JSONObject) e;
            JSONObject e_data = json.getJSONArray("Data").getJSONObject(0);


            // result_Data_arr   ->   平铺
            OptionRevokeOrderResultDTO dto = new OptionRevokeOrderResultDTO();


            dto.setErrcode(json.getInteger("Errcode"));
            dto.setErrmsg(json.getString("Errmsg"));
            dto.setId(json.getString("Id"));
            dto.setRecsum(json.getInteger("Recsum"));
            dto.setType(json.getInteger("Type"));


            dto.setHtxh(e_data.getString("Htxh"));
            dto.setJybk(e_data.getString("Jybk"));
            dto.setWtjg(e_data.getString("Wtjg"));
            dto.setWtph(e_data.getString("Wtph"));
            dto.setWtsl(e_data.getInteger("Wtsl"));
            dto.setZqyw(e_data.getString("Zqyw"));
            dto.setZqywxw(e_data.getString("Zqywxw"));


            // 撤单结果：true-成功；false-失败；
            dto.setSuc(dto.getErrcode() == 0);


            dtoList.add(dto);
        });


        return dtoList;
    }

}