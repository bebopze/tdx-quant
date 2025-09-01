package com.bebopze.tdx.quant.common.util;

import com.bebopze.tdx.quant.common.cache.BacktestCache;
import com.bebopze.tdx.quant.dal.entity.BaseStockDO;
import com.bebopze.tdx.quant.service.impl.InitDataServiceImpl;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.openjdk.jol.info.GraphLayout;

import java.util.List;


/**
 * Memory Size
 *
 * @author: bebopze
 * @date: 2025/9/1
 */
@Slf4j
public class MemorySize {


    public static String format(Object obj) {
        long sizeByte = sizeByte(obj);


        if (sizeByte > 1024 * 1024 * 1024) {
            return sizeMB(obj) + "GB";
        } else if (sizeByte > 1024 * 1024) {
            return sizeMB(obj) + "MB";
        } else if (sizeByte > 1024) {
            return sizeKb(obj) + "KB";
        }

        return sizeByte + "b";
    }


    public static double sizeGB(Object obj) {
        long sizeInBytes = sizeByte(obj);
        return NumUtil.of(sizeInBytes / (1024 * 1024 * 1024.0));
    }

    public static double sizeMB(Object obj) {
        long sizeInBytes = sizeByte(obj);
        return NumUtil.of(sizeInBytes / (1024 * 1024.0));
    }


    public static double sizeKb(Object obj) {
        long sizeInBytes = sizeByte(obj);
        return NumUtil.of(sizeInBytes / 1024.0);
    }


    public static long sizeByte(Object obj) {
        return GraphLayout.parseInstance(obj).totalSize();
    }


    public static String printable(Object obj) {
        return GraphLayout.parseInstance(obj).toPrintable();
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {


        BaseStockDO stockDO = MybatisPlusUtil.getBaseStockService().getByCode("300059");

        List<BaseStockDO> stockDOList = Lists.newArrayList();
        for (int i = 0; i < 5000; i++) {
            stockDOList.add(stockDO);
        }


        long b = sizeByte(stockDOList);
        double kb = sizeKb(stockDOList);
        double mb = sizeMB(stockDOList);

        System.out.println(b);
        System.out.println(kb);
        System.out.println(mb);


        double sizeMB = MemorySize.sizeMB(InitDataServiceImpl.data);
        double sizeMB2 = MemorySize.sizeMB(BacktestCache.stockFunCache);

        System.out.println(sizeMB);
        System.out.println(sizeMB2);


        // ------------------------ printable


        // String printable = GraphLayout.parseInstance(stockDOList).toPrintable();
        // System.out.println(printable);
        //
        // long printable_sizeByte = sizeByte(printable);
        // System.out.println(printable_sizeByte);


    }


}