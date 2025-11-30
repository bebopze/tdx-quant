package com.bebopze.tdx.quant.indicator;

import com.bebopze.tdx.quant.common.convert.ConvertStock;
import com.bebopze.tdx.quant.common.util.DateTimeUtil;
import com.bebopze.tdx.quant.dal.entity.BaseBlockDO;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.Assert;


/**
 * 基础指标   -   序列化（返回 数组）
 *
 * @author: bebopze
 * @date: 2025/5/25
 */
@Slf4j
public class BlockFun extends StockFun {


    private BaseBlockDO blockDO;


    // -----------------------------------------------------------------------------------------------------------------
    //                                            个股/板块 - 行情数据 init
    // -----------------------------------------------------------------------------------------------------------------


    public BlockFun(BaseBlockDO blockDO) {
        this(blockDO == null ? null : blockDO.getCode(), blockDO);
    }


    public BlockFun(String code, BaseBlockDO blockDO) {
        Assert.notNull(blockDO, String.format("blockDO:[%s] is null  ->  请检查 dataCache 是否为null", code));
        long start = System.currentTimeMillis();


        // super(null);


        this.blockDO = blockDO;


        String blockName = blockDO.getName();


        // K线数据
        klineDTOList = blockDO.getKlineDTOList();
        // 扩展数据（预计算 指标）
        extDataDTOList = blockDO.getExtDataDTOList();


        // -----------------------------------------------


        klineArrDTO = ConvertStock.kline__dtoList2Arr(klineDTOList);
        extDataArrDTO = ConvertStock.extData__dtoList2Arr(extDataDTOList);


        // -----------------------------------------------

        date = klineArrDTO.date;

        open = klineArrDTO.open;
        high = klineArrDTO.high;
        low = klineArrDTO.low;
        close = klineArrDTO.close;

        vol = klineArrDTO.vol;
        amo = klineArrDTO.amo;


        // -----------------------------------------------


        rps10 = extDataArrDTO.rps10;
        rps20 = extDataArrDTO.rps20;
        rps50 = extDataArrDTO.rps50;
        rps120 = extDataArrDTO.rps120;
        rps250 = extDataArrDTO.rps250;


        // -----------------------------------------------


        dateIndexMap = Maps.newHashMap();
        for (int i = 0; i < date.length; i++) {
            dateIndexMap.put(date[i], i);
        }

        maxIdx = Math.max(0, date.length - 1);


        // --------------------------- init data


        this.code = code;
        this.name = blockName;


        this.shszQuoteSnapshotResp = null;


        ssf = extDataArrDTO.SSF;


        // -------------------------------------------------------------------------------------------------------------
        log.info("BlockFun - init     >>>     [{}-{}] , time : {}", code, name, DateTimeUtil.formatNow2Hms(start));
    }


}