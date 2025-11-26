package com.bebopze.tdx.quant.common.util;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONB;
import com.bebopze.tdx.quant.common.convert.ConvertStockExtData;
import com.bebopze.tdx.quant.common.domain.dto.kline.ExtDataDTO;
import com.github.luben.zstd.Zstd;
import com.google.common.collect.Lists;
import lombok.SneakyThrows;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;
import org.apache.commons.io.FileUtils;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * 压缩工具类（LZ4、ZSTD、GZIP）
 *
 * @author: bebopze
 * @date: 2025/11/26
 */
public final class CompressUtil {


    private static final LZ4Factory LZ4 = LZ4Factory.fastestInstance();


    /**
     * LZ4压缩
     *
     * @param src 待压缩数据
     * @return 压缩后数据
     */
    public static byte[] lz4Compress(byte[] src) {
        if (src == null || src.length == 0) {
            return src;
        }


        LZ4Compressor compressor = LZ4.fastCompressor();
        int max = compressor.maxCompressedLength(src.length);
        byte[] dest = new byte[max];
        int len = compressor.compress(src, 0, src.length, dest, 0, max);
        return Arrays.copyOf(dest, len);
    }

    /**
     * LZ4解压缩
     *
     * @param compressed         压缩数据
     * @param decompressedLength 解压后数据长度
     * @return 解压后数据
     */
    public static byte[] lz4Decompress(byte[] compressed, Integer decompressedLength) {
        if (compressed == null || compressed.length == 0 || decompressedLength == null) {
            return compressed;
        }


        LZ4SafeDecompressor de = LZ4.safeDecompressor();
        byte[] restored = new byte[decompressedLength];
        int len = de.decompress(compressed, 0, compressed.length, restored, 0);
        if (len != decompressedLength) {
            return Arrays.copyOf(restored, len);
        }
        return restored;
    }


    /**
     * ZSTD压缩
     *
     * @param src 待压缩数据
     * @return 压缩后数据
     */
    public static byte[] zstdCompress(byte[] src) {
        return zstdCompress(src, 1);
    }

    /**
     * ZSTD压缩
     *
     * @param src   待压缩数据
     * @param level 压缩级别，1-3 速度快，4-12 速度慢，压缩比高（一般默认：1）
     *              1MB 实测 压缩对比（level：压缩率/耗时）     =>     1：36%/3ms、3：37%/5ms、5：35%/9ms、9：33%/19ms、19：32%/200ms、22：32%/250ms
     * @return 压缩后数据
     */
    public static byte[] zstdCompress(byte[] src, int level) {
        if (src == null || src.length == 0) {
            return src;
        }

        return Zstd.compress(src, level);
    }

    /**
     * ZSTD解压缩
     *
     * @param compressed   压缩数据
     * @param originalSize 解压后数据长度
     * @return 解压后数据
     */
    public static byte[] zstdDecompress(byte[] compressed, Integer originalSize) {
        if (compressed == null || compressed.length == 0 || originalSize == null) {
            return compressed;
        }

        return Zstd.decompress(compressed, originalSize);
    }


    /**
     * GZIP压缩
     *
     * @param src 待压缩数据
     * @return 压缩后数据
     */
    @SneakyThrows
    public static byte[] gzipCompress(byte[] src) {
        if (src == null || src.length == 0) {
            return src;
        }


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(src);
        }
        return baos.toByteArray();
    }

    /**
     * GZIP解压缩
     *
     * @param compressed 压缩数据
     * @return 解压后数据
     */
    @SneakyThrows
    public static byte[] gzipDecompress(byte[] compressed) {
        if (compressed == null || compressed.length == 0) {
            return compressed;
        }


        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
        try (GZIPInputStream gis = new GZIPInputStream(bais);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = gis.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            return baos.toByteArray();
        }
    }


//    // Java 9+
//    public static byte[] gzipDecompress(byte[] compressed) throws IOException {
//        ByteArrayInputStream bais = new ByteArrayInputStream(compressed);
//        try (GZIPInputStream gis = new GZIPInputStream(bais)) {
//            return gis.readAllBytes();
//        }
//    }


    // -----------------------------------------------------------------------------------------------------------------


    /**
     * fastjson压缩            ❌❌❌（不能用，有 JSONB 有重大bug）
     *
     * -                      经测试：JSONB 不支持   含中文字段（JSONB 会直接忽略 中文字段   ->   导致 数据丢失、且字段顺序错乱）
     *
     * @param dtoList 待压缩数据
     * @return 压缩后数据
     */
    @Deprecated
    private static byte[] fastjsonCompress(List<ExtDataDTO> dtoList) {
        if (dtoList == null || dtoList.isEmpty()) {
            return new byte[0];
        }


        // return JSON.toJSONBytes(dtoList);
        return JSONB.toBytes(dtoList);
    }

    /**
     * fastjson解压缩             ❌❌❌（不能用，有 JSONB 有重大bug）
     *
     * -                         经测试：JSONB 不支持   含中文字段（JSONB 会直接忽略 中文字段   ->   导致 数据丢失、且字段顺序错乱）
     *
     * @param compressed 压缩数据
     * @return 解压后数据
     */
    @Deprecated
    private static List<ExtDataDTO> fastjsonDecompress(byte[] compressed) {
        if (compressed == null || compressed.length == 0) {
            return Lists.newArrayList();
        }


        // return JSON.parseArray(compressed, ExtDataDTO.class);
        return JSONB.parseArray(compressed, ExtDataDTO.class);
    }


    // -----------------------------------------------------------------------------------------------------------------


    @SneakyThrows
    public static void main(String[] args) {


        byte[] decompressed = CompressUtil.zstdDecompress(null, null);


//        String filePath = System.getProperty("user.dir") + "/wiki/DB/000001__kline.json";
        String filePath = System.getProperty("user.dir") + "/wiki/DB/000001__extData.json";
        String klineHis = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);


        double kb = klineHis.getBytes(StandardCharsets.UTF_8).length / 1024.0;
        System.out.printf("压缩前大小 = %.1f KB       字符串长度 = %d%n", kb, klineHis.length());
        System.out.printf("--------------------------------------------------------------------%n%n%n");


        List<ExtDataDTO> dtoList = ConvertStockExtData.extDataHis2DTOList(klineHis);


        for (int i = 0; i < 50; i++) {
            test_lz4(klineHis);
            test_zstd(klineHis);
            test_gzip(klineHis);


            test_fastjson(klineHis, dtoList);


            System.out.printf("---------------------------------------------------%n%n");
        }


        //   压缩前大小 = 811.2 KB
        //
        //
        //   lz4压缩耗时: 2ms
        //   lz4解压耗时: 631μs
        //   压缩后大小 = 486.2 KB , 压缩率 = 59.93%
        //
        //   zstd压缩耗时: 2ms
        //   zstd解压耗时: 1ms
        //   压缩后大小 = 292.2 KB , 压缩率 = 36.02%
        //
        //   gzip压缩耗时: 46ms
        //   gzip解压耗时: 2ms
        //   压缩后大小 = 279.2 KB , 压缩率 = 34.41%


        // -------------------------------------------------------------------------------------------------------------


        //   JSONB 压缩（DTO -> JSONB 字节数组）：
        //
        //   fastjson压缩耗时: 5ms
        //   fastjson解压耗时: 4ms
        //   压缩后大小 = 2902.4 KB , 压缩率 = 357.80%
        //
        //
        //   JSON 压缩（JSONB 字节数组 -> DTO）：
        //
        //   fastjson压缩耗时: 12ms
        //   fastjson解压耗时: 10ms
        //   压缩后大小 = 3275.1 KB , 压缩率 = 403.74%


        // 经测试：JSONB 不支持   含中文字段（JSONB 会直接忽略 中文字段   ->   导致 数据丢失、且字段顺序错乱）
        // 经测试：JSON   支持   含中文字段（JSON  不识别NaN           ->   会被压缩为 null）
    }


    private static void test_lz4(String klineHis) {

        long zip_start = System.nanoTime();
        byte[] bytes = klineHis == null ? null : klineHis.getBytes(StandardCharsets.UTF_8);
        byte[] zipBytes = lz4Compress(bytes);
        System.out.println("lz4压缩耗时: " + DateTimeUtil.format2μs(zip_start));


        long unzip_start = System.nanoTime();
        byte[] unzipBytes = lz4Decompress(zipBytes, bytes.length);
        String unzip_klineHis = new String(unzipBytes, StandardCharsets.UTF_8);
        System.out.println("lz4解压耗时: " + DateTimeUtil.format2μs(unzip_start));


        Assert.isTrue(klineHis.equals(unzip_klineHis), "lz4压缩前后 字符串不相等");


        double kb = zipBytes.length / 1024.0;
        System.out.printf("压缩后大小 = %.1f KB , 压缩率 = %.2f%% %n", kb, (double) zipBytes.length / bytes.length * 100);


        // System.out.println(unzip_klineHis);
        System.out.println();
    }

    private static void test_zstd(String klineHis) {

        long zip_start = System.nanoTime();
        byte[] bytes = klineHis == null ? null : klineHis.getBytes(StandardCharsets.UTF_8);
        byte[] zipBytes = zstdCompress(bytes);
        System.out.println("zstd压缩耗时: " + DateTimeUtil.format2μs(zip_start));


        long unzip_start = System.nanoTime();
        byte[] unzipBytes = zstdDecompress(zipBytes, bytes.length);
        String unzip_klineHis = new String(unzipBytes, StandardCharsets.UTF_8);
        System.out.println("zstd解压耗时: " + DateTimeUtil.format2μs(unzip_start));


        Assert.isTrue(klineHis.equals(unzip_klineHis), "zstd压缩前后 字符串不相等");


        double kb = zipBytes.length / 1024.0;
        System.out.printf("压缩后大小 = %.1f KB , 压缩率 = %.2f%% %n", kb, (double) zipBytes.length / bytes.length * 100);


        // System.out.println(unzip_klineHis);
        System.out.println();
    }

    private static void test_gzip(String klineHis) {

        long zip_start = System.nanoTime();
        byte[] bytes = klineHis == null ? null : klineHis.getBytes(StandardCharsets.UTF_8);
        byte[] zipBytes = gzipCompress(bytes);
        System.out.println("gzip压缩耗时: " + DateTimeUtil.format2μs(zip_start));


        long unzip_start = System.nanoTime();
        byte[] unzipBytes = gzipDecompress(zipBytes);
        String unzip_klineHis = new String(unzipBytes, StandardCharsets.UTF_8);
        System.out.println("gzip解压耗时: " + DateTimeUtil.format2μs(unzip_start));


        Assert.isTrue(klineHis.equals(unzip_klineHis), "gzip压缩前后 字符串不相等");


        double kb = zipBytes.length / 1024.0;
        System.out.printf("压缩后大小 = %.1f KB , 压缩率 = %.2f%% %n", kb, (double) zipBytes.length / bytes.length * 100);


        // System.out.println(unzip_klineHis);
        System.out.println();
    }


    private static void test_fastjson(String klineHis, List<ExtDataDTO> dtoList) {

        long zip_start = System.nanoTime();
        byte[] zipBytes = fastjsonCompress(dtoList);
        System.out.println("fastjson压缩耗时: " + DateTimeUtil.format2μs(zip_start));


        long unzip_start = System.nanoTime();
        List<ExtDataDTO> dtoList2 = fastjsonDecompress(zipBytes);
        System.out.println("fastjson解压耗时: " + DateTimeUtil.format2μs(unzip_start));


        // Assert.isTrue(JSON.toJSONString(dtoList).equals(JSON.toJSONString(dtoList2)), "fastjson压缩前后 列表不相等");


        double kb = zipBytes.length / 1024.0;
        // 反向 压缩   ->   压缩后大小 = 2902.4 KB , 压缩率 = 357.80%     ❌❌❌
        int srcLength = klineHis.getBytes(StandardCharsets.UTF_8).length;   // DTOList -> 包含 K-V  （会远大于  arr -> 仅包含值）
        System.out.printf("压缩后大小 = %.1f KB , 压缩率 = %.2f%% %n", kb, (double) zipBytes.length / srcLength * 100);


        // 经测试：JSONB 不支持   含中文字段（JSONB 会直接忽略 中文字段   ->   导致 数据丢失、且字段顺序错乱）    ❌❌❌
        System.out.println("JSONB 压缩前后 字符串长度 是否相等: " + (klineHis.length() == JSON.toJSONString(dtoList).length())); // false     ❌❌❌


        // System.out.println(unzip_klineHis);
        System.out.println();
    }


}