package com.bebopze.tdx.quant.parser.check;

import com.alibaba.fastjson2.JSON;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataArrDTO;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.bebopze.tdx.quant.common.util.MybatisPlusUtil;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.indicator.StockFun;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;


/**
 * check     ==>     fun_val   -   DB_val（列表/序列）
 *
 * @author: bebopze
 * @date: 2025/8/11
 */
@Slf4j
public class TdxFunParserCheck {


    public static void main(String[] args) {


        String stockCode = "000001";


        BaseStockDO stockDO = MybatisPlusUtil.getBaseStockService().getByCode(stockCode);


        StockFun fun = new StockFun(stockDO);


        checkList(fun);
        checkArr(fun);
    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    public static void checkList(BaseStockDO stockDO) {
        checkList(new StockFun(stockDO));
    }


    /**
     * check     ==>     fun_val   -   DB_val（列表）
     *
     * @param fun
     */
    public static void checkList(StockFun fun) {
        String stockCode = fun.getCode();


        Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


        // -------------------------------------------------------------------------------------------------------------
        //                                      DB_val（列表）     -     fun_val
        // -------------------------------------------------------------------------------------------------------------


        // DB 解析   ->   列表
        List<ExtDataDTO> extDataDTOList = fun.getExtDataDTOList();


        // ------------------------------------------------------

        // 实时计算
        double[] SSF_arr = fun.SSF();


        double[] 中期涨幅_arr = fun.中期涨幅N(20);
        int[] 趋势支撑线_arr = fun.趋势支撑线();


        boolean[] 高位爆量上影大阴_arr = fun.高位爆量上影大阴();


        boolean[] MA20多_arr = fun.MA多(20);
        boolean[] MA20空_arr = fun.MA空(20);
        boolean[] SSF多_arr = fun.SSF多();
        boolean[] SSF空_arr = fun.SSF空();


        boolean[] N60日新高_arr = fun.N日新高(60);
        boolean[] N100日新高_arr = fun.N日新高(100);
        boolean[] 历史新高_arr = fun.历史新高();


        boolean[] 月多_arr = fun.月多();
        boolean[] 均线预萌出_arr = fun.均线预萌出();
        boolean[] 均线萌出_arr = fun.均线萌出();
        boolean[] 大均线多头_arr = fun.大均线多头();
        boolean[] 均线大多头_arr = fun.均线大多头();
        boolean[] 均线极多头_arr = fun.均线极多头();


        boolean[] RPS红_arr = fun.RPS红(85);
        boolean[] RPS一线红_arr = fun.RPS一线红(95);
        boolean[] RPS双线红_arr = fun.RPS双线红(90);
        boolean[] RPS三线红_arr = fun.RPS三线红(85);


        // -------------------------------------------------------------------------------------------------------------
        //                                      check     ==>     fun_val   -   DB_val
        // -------------------------------------------------------------------------------------------------------------


        // 逐日遍历 -> check
        dateIndexMap.forEach((date, idx) -> {


            // 250日以上数据   计算val   ->   为 有效val                       // tips：[历史新高]   ->   需要全部日K（10年+）
            if (idx < 250) {
                return;
            }


            // ------------------------------------------------------ DB   ->   解析（列表）


            ExtDataDTO dto = extDataDTOList.get(idx);


            double SSF = dto.getSSF();


            boolean SSF多 = dto.getSSF多();
            boolean MA20多 = dto.getMA20多();


            boolean N60日新高 = dto.getN60日新高();
            boolean N100日新高 = dto.getN100日新高();
            boolean 历史新高 = dto.get历史新高();


            boolean 月多 = dto.get月多();
            boolean 均线预萌出 = dto.get均线预萌出();
            boolean 均线萌出 = dto.get均线萌出();
            boolean 大均线多头 = dto.get大均线多头();
            boolean 均线大多头 = dto.get均线大多头();
            boolean 均线极多头 = dto.get均线极多头();


            boolean RPS红 = dto.getRPS红();
            boolean RPS一线红 = dto.getRPS一线红();
            boolean RPS双线红 = dto.getRPS双线红();
            boolean RPS三线红 = dto.getRPS三线红();


            // ------------------------------------------------------ fun   ->   实时计算


            double _SSF = SSF_arr[idx];

            boolean _SSF多 = SSF多_arr[idx];
            boolean _MA20多 = MA20多_arr[idx];


            boolean _N60日新高 = N60日新高_arr[idx];
            boolean _N100日新高 = N100日新高_arr[idx];
            boolean _历史新高 = 历史新高_arr[idx];


            boolean _月多 = 月多_arr[idx];
            boolean _均线预萌出 = 均线预萌出_arr[idx];
            boolean _均线萌出 = 均线萌出_arr[idx];
            boolean _大均线多头 = 大均线多头_arr[idx];
            boolean _均线大多头 = 均线大多头_arr[idx];
            boolean _均线极多头 = 均线极多头_arr[idx];


            boolean _RPS红 = RPS红_arr[idx];
            boolean _RPS一线红 = RPS一线红_arr[idx];
            boolean _RPS双线红 = RPS双线红_arr[idx];
            boolean _RPS三线红 = RPS三线红_arr[idx];


            // ------------------------------------------------------ check     ==>     fun_val   -   DB_val


            checkFunVal(stockCode, date, idx, "SSF", _SSF, SSF, dto);


            checkFunVal(stockCode, date, idx, "SSF多", _SSF多, SSF多, dto);
            checkFunVal(stockCode, date, idx, "MA20多", _MA20多, MA20多, dto);


            checkFunVal(stockCode, date, idx, "N60日新高", _N60日新高, N60日新高, dto);
            checkFunVal(stockCode, date, idx, "N100日新高", _N100日新高, N100日新高, dto);


            // 历史新高   ->   需要 全部日K数据（10年+）
            // Assert.isTrue(_历史新高 == 历史新高, errMsg(stockCode, date, idx, "历史新高", _历史新高, 历史新高, dto);


            checkFunVal(stockCode, date, idx, "月多", _月多, 月多, dto);
            checkFunVal(stockCode, date, idx, "均线预萌出", _均线预萌出, 均线预萌出, dto);
            checkFunVal(stockCode, date, idx, "均线萌出", _均线萌出, 均线萌出, dto);
            checkFunVal(stockCode, date, idx, "大均线多头", _大均线多头, 大均线多头, dto);
            checkFunVal(stockCode, date, idx, "均线大多头", _均线大多头, 均线大多头, dto);
            checkFunVal(stockCode, date, idx, "均线极多头", _均线极多头, 均线极多头, dto);


            checkFunVal(stockCode, date, idx, "RPS红", _RPS红, RPS红, dto);
            checkFunVal(stockCode, date, idx, "RPS一线红", _RPS一线红, RPS一线红, dto);
            checkFunVal(stockCode, date, idx, "RPS双线红", _RPS双线红, RPS双线红, dto);
            checkFunVal(stockCode, date, idx, "RPS三线红", _RPS三线红, RPS三线红, dto);
        });
    }


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    // -----------------------------------------------------------------------------------------------------------------


    public static void checkArr(BaseStockDO stockDO) {
        checkArr(new StockFun(stockDO));
    }


    /**
     * check     ==>     fun_val   -   DB_val（序列）
     *
     * @param fun
     */
    public static void checkArr(StockFun fun) {
        String stockCode = fun.getCode();


        Map<LocalDate, Integer> dateIndexMap = fun.getDateIndexMap();


        List<ExtDataDTO> extDataDTOList = fun.getExtDataDTOList();


        // -------------------------------------------------------------------------------------------------------------
        //                                      DB_val（序列）     -     fun_val
        // -------------------------------------------------------------------------------------------------------------


        // DB 解析   ->   序列
        ExtDataArrDTO extDataArrDTO = fun.getExtDataArrDTO();


        // ------------------------------------------------------

        // 实时计算
        double[] SSF_arr = fun.SSF();


        double[] 中期涨幅_arr = fun.中期涨幅N(20);
        int[] 趋势支撑线_arr = fun.趋势支撑线();


        boolean[] 高位爆量上影大阴_arr = fun.高位爆量上影大阴();


        boolean[] MA20多_arr = fun.MA多(20);
        boolean[] MA20空_arr = fun.MA空(20);
        boolean[] SSF多_arr = fun.SSF多();
        boolean[] SSF空_arr = fun.SSF空();


        boolean[] N60日新高_arr = fun.N日新高(60);
        boolean[] N100日新高_arr = fun.N日新高(100);
        boolean[] 历史新高_arr = fun.历史新高();


        boolean[] 月多_arr = fun.月多();
        boolean[] 均线预萌出_arr = fun.均线预萌出();
        boolean[] 均线萌出_arr = fun.均线萌出();
        boolean[] 大均线多头_arr = fun.大均线多头();


        boolean[] RPS红_arr = fun.RPS红(85);
        boolean[] RPS一线红_arr = fun.RPS一线红(95);
        boolean[] RPS双线红_arr = fun.RPS双线红(90);
        boolean[] RPS三线红_arr = fun.RPS三线红(85);


        // ------------------------------------------------------


        int length = extDataArrDTO.SSF.length;
        int _length = fun.SSF().length;


        Assert.isTrue(length == _length, "idx异常     >>>     数据长度不一致");


        // ------------------------------------------------------


        // -------------------------------------------------------------------------------------------------------------
        //                                      check     ==>     fun_val   -   DB_val
        // -------------------------------------------------------------------------------------------------------------


        // 逐日遍历 -> check
        dateIndexMap.forEach((date, idx) -> {


            // 250日以上数据   计算val   ->   为 有效val                       // tips：[历史新高]   ->   需要全部日K（10年+）
            if (idx < 250) {
                return;
            }


            // ------------------------------------------------------


            ExtDataDTO dto = extDataDTOList.get(idx);


            // ------------------------------------------------------ DB   ->   解析（序列）


            double SSF = extDataArrDTO.SSF[idx];


            boolean SSF多 = extDataArrDTO.SSF多[idx];
            boolean MA20多 = extDataArrDTO.MA20多[idx];


            boolean N60日新高 = extDataArrDTO.N60日新高[idx];
            boolean N100日新高 = extDataArrDTO.N100日新高[idx];
            boolean 历史新高 = extDataArrDTO.历史新高[idx];


            boolean 月多 = extDataArrDTO.月多[idx];
            boolean 均线预萌出 = extDataArrDTO.均线预萌出[idx];
            boolean 均线萌出 = extDataArrDTO.均线萌出[idx];
            boolean 大均线多头 = extDataArrDTO.大均线多头[idx];


            boolean RPS红 = extDataArrDTO.RPS红[idx];
            boolean RPS一线红 = extDataArrDTO.RPS一线红[idx];
            boolean RPS双线红 = extDataArrDTO.RPS双线红[idx];
            boolean RPS三线红 = extDataArrDTO.RPS三线红[idx];


            // ------------------------------------------------------ fun   ->   实时计算


            double _SSF = SSF_arr[idx];


            boolean _SSF多 = SSF多_arr[idx];
            boolean _MA20多 = MA20多_arr[idx];


            boolean _N60日新高 = N60日新高_arr[idx];
            boolean _N100日新高 = N100日新高_arr[idx];
            boolean _历史新高 = 历史新高_arr[idx];


            boolean _月多 = 月多_arr[idx];
            boolean _均线预萌出 = 均线预萌出_arr[idx];
            boolean _均线萌出 = 均线萌出_arr[idx];
            boolean _大均线多头 = 大均线多头_arr[idx];


            boolean _RPS红 = RPS红_arr[idx];
            boolean _RPS一线红 = RPS一线红_arr[idx];
            boolean _RPS双线红 = RPS双线红_arr[idx];
            boolean _RPS三线红 = RPS三线红_arr[idx];


            // ------------------------------------------------------ check     ==>     fun_val   -   DB_val


            checkFunVal(stockCode, date, idx, "SSF", _SSF, SSF, dto);


            checkFunVal(stockCode, date, idx, "SSF多", _SSF多, SSF多, dto);
            checkFunVal(stockCode, date, idx, "MA20多", _MA20多, MA20多, dto);


            checkFunVal(stockCode, date, idx, "N60日新高", _N60日新高, N60日新高, dto);
            checkFunVal(stockCode, date, idx, "N100日新高", _N100日新高, N100日新高, dto);


            // 历史新高   ->   需要 全部日K数据（10年+）
            // Assert.isTrue(_历史新高 == 历史新高, errMsg(stockCode, date, idx, "历史新高", _历史新高, 历史新高, dto);


            checkFunVal(stockCode, date, idx, "月多", _月多, 月多, dto);
            checkFunVal(stockCode, date, idx, "均线预萌出", _均线预萌出, 均线预萌出, dto);
            checkFunVal(stockCode, date, idx, "均线萌出", _均线萌出, 均线萌出, dto);
            checkFunVal(stockCode, date, idx, "大均线多头", _大均线多头, 大均线多头, dto);


            checkFunVal(stockCode, date, idx, "RPS红", _RPS红, RPS红, dto);
            checkFunVal(stockCode, date, idx, "RPS一线红", _RPS一线红, RPS一线红, dto);
            checkFunVal(stockCode, date, idx, "RPS双线红", _RPS双线红, RPS双线红, dto);
            checkFunVal(stockCode, date, idx, "RPS三线红", _RPS三线红, RPS三线红, dto);
        });
    }


    private static void checkFunVal(String stockCode,
                                    LocalDate date, int idx, String fieldName, Object fun_val, Object DB_val,
                                    ExtDataDTO dto) {


        boolean check;

        if (fun_val instanceof Number) {
            check = TdxFunCheck.equals((Number) fun_val, (Number) DB_val, 0.05);
        } else if ("月多".equals(fieldName)) {
            check = idx < 500 || fun_val == DB_val;
        } else {
            check = fun_val == DB_val;
        }


        if (!check) {

            String errMsg = String.format("[%s] - err     >>>     [%s] - [%s] [%s] , fun_val : %s , DB_解析val : %s     >>>     dto : %s",
                                          fieldName,
                                          stockCode, date, idx, fun_val, DB_val,
                                          JSON.toJSONString(dto));

            log.error(errMsg);
        }
    }

}