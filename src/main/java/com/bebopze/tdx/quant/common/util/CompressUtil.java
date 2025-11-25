package com.bebopze.tdx.quant.common.util;

import com.github.luben.zstd.Zstd;
import lombok.SneakyThrows;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;
import org.apache.commons.io.FileUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
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
     */
    public static byte[] lz4Decompress(byte[] compressed, int decompressedLength) {
        if (compressed == null || compressed.length == 0) {
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
     */
    public static byte[] zstdDecompress(byte[] compressed, int originalSize) {
        if (compressed == null || compressed.length == 0) {
            return compressed;
        }

        return Zstd.decompress(compressed, originalSize);
    }


    /**
     * GZIP压缩
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


    @SneakyThrows
    public static void main(String[] args) {


//        String filePath = System.getProperty("user.dir") + "/wiki/DB/000001__kline.json";
        String filePath = System.getProperty("user.dir") + "/wiki/DB/000001__extData.json";
        String klineHis = FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);


        double kb = klineHis.getBytes(StandardCharsets.UTF_8).length / 1024.0;
        System.out.printf("压缩前大小 = %.1f KB       字符串长度 = %d%n", kb, klineHis.length());
        System.out.printf("--------------------------------------------------------------------%n%n%n");


        for (int i = 0; i < 50; i++) {
            test_lz4(klineHis);
            test_zstd(klineHis);
            test_gzip(klineHis);


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
    }


    private static void test_lz4(String klineHis) {

        long zip_start = System.nanoTime();
        byte[] bytes = klineHis == null ? null : klineHis.getBytes(StandardCharsets.UTF_8);
        byte[] zipBytes = CompressUtil.lz4Compress(bytes);
        System.out.println("lz4压缩耗时: " + DateTimeUtil.format2μs(zip_start));


        long unzip_start = System.nanoTime();
        byte[] unzipBytes = CompressUtil.lz4Decompress(zipBytes, klineHis.length());
        String unzip_klineHis = new String(unzipBytes, StandardCharsets.UTF_8);
        System.out.println("lz4解压耗时: " + DateTimeUtil.format2μs(unzip_start));


        double kb = zipBytes.length / 1024.0;
        System.out.printf("压缩后大小 = %.1f KB , 压缩率 = %.2f%% %n", kb, (double) zipBytes.length / bytes.length * 100);


        // System.out.println(unzip_klineHis);
        System.out.println();
    }

    private static void test_zstd(String klineHis) {

        long zip_start = System.nanoTime();
        byte[] bytes = klineHis == null ? null : klineHis.getBytes(StandardCharsets.UTF_8);
        byte[] zipBytes = CompressUtil.zstdCompress(bytes);
        System.out.println("zstd压缩耗时: " + DateTimeUtil.format2μs(zip_start));


        long unzip_start = System.nanoTime();
        byte[] unzipBytes = CompressUtil.zstdDecompress(zipBytes, klineHis.length());
        String unzip_klineHis = new String(unzipBytes, StandardCharsets.UTF_8);
        System.out.println("zstd解压耗时: " + DateTimeUtil.format2μs(unzip_start));


        double kb = zipBytes.length / 1024.0;
        System.out.printf("压缩后大小 = %.1f KB , 压缩率 = %.2f%% %n", kb, (double) zipBytes.length / bytes.length * 100);


        // System.out.println(unzip_klineHis);
        System.out.println();
    }

    private static void test_gzip(String klineHis) {

        long zip_start = System.nanoTime();
        byte[] bytes = klineHis == null ? null : klineHis.getBytes(StandardCharsets.UTF_8);
        byte[] zipBytes = CompressUtil.gzipCompress(bytes);
        System.out.println("gzip压缩耗时: " + DateTimeUtil.format2μs(zip_start));


        long unzip_start = System.nanoTime();
        byte[] unzipBytes = CompressUtil.gzipDecompress(zipBytes);
        String unzip_klineHis = new String(unzipBytes, StandardCharsets.UTF_8);
        System.out.println("gzip解压耗时: " + DateTimeUtil.format2μs(unzip_start));


        double kb = zipBytes.length / 1024.0;
        System.out.printf("压缩后大小 = %.1f KB , 压缩率 = %.2f%% %n", kb, (double) zipBytes.length / bytes.length * 100);


        // System.out.println(unzip_klineHis);
        System.out.println();
    }


}