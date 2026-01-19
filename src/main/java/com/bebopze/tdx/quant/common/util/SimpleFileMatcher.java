package com.bebopze.tdx.quant.common.util;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.CollectionUtils;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * 文件匹配器
 *
 * @author: bebopze
 * @date: 2026/1/7
 */
public class SimpleFileMatcher {


    /**
     * 获取指定目录下匹配 "ETF基金yyyyMMdd.txt" 的文件列表
     *
     * @return 文件列表（按照文件名 倒序）
     */
    public static List<File> getETFFiles(String basePath, String fileName) {
        File directory = new File(basePath);

        if (!directory.exists() || !directory.isDirectory()) {
            return Lists.newArrayList();
        }


        return Arrays.stream(directory.listFiles())
                     .filter(file -> file.isFile() && file.getName().startsWith(fileName) && file.getName().endsWith(".txt"))
                     .sorted((f1, f2) -> f2.getName().compareTo(f1.getName())) // 按文件名 降序
                     // .sorted((f1, f2) -> Long.compare(f2.lastModified(), f1.lastModified())) // 按文件修改日期 降序
                     .collect(Collectors.toList());
    }


    /**
     * 获取 最新日期的 文件
     *
     * @param basePath
     * @param fileName
     * @return
     */
    public static File getLastETFFile(String basePath, String fileName) {
        List<File> etfFiles = getETFFiles(basePath, fileName);

        if (CollectionUtils.isEmpty(etfFiles)) {
            return null;
        }
        return etfFiles.get(0);
    }


    // -----------------------------------------------------------------------------------------------------------------


    public static void main(String[] args) {


        String filePath = TDX_PATH + "/T0002/export";
        String fileName = "ETF基金";


        // -------------------------------


        List<File> etfFiles = getETFFiles(filePath, fileName);

        System.out.println("找到 " + etfFiles.size() + " 个ETF基金文件:");
        etfFiles.forEach(file -> System.out.println("- " + file.getName()));


        // -------------------------------


        // 获取 最新日期的 文件
        File lastETFFile = getLastETFFile(filePath, fileName);

        System.out.println("\n找到[" + fileName + "]最新日期的文件: " + lastETFFile.getName());
    }


}