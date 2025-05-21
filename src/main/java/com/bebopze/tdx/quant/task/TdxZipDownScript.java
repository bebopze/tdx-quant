package com.bebopze.tdx.quant.task;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.bebopze.tdx.quant.common.constant.TdxConst.TDX_PATH;


/**
 * 下载     ->     盘后数据（A股-日K） / 财务数据               （通达信官网   每日提供 zip包）
 *
 * @author: bebopze
 * @date: 2025/5/22
 */
@Slf4j
public class TdxZipDownScript {


    /**
     * 首页 > 行情数据下载 > 个人行情数据                 https://www.tdx.com.cn/article/vipdata.html
     *
     *
     * 1. 适用范围：适用于通达信个人版PC端软件的盘后数据下载。
     * 2. 文件包含：沪深京日线数据完整包(A股、B股、交易所指数和板块指数、回购、可交易基金、可转债等)。
     * 3. 适用场景：首次安装通达信PC端软件或有较长时间没有下载日线时，可以下载此zip文件。
     * 4. 使用方法：放置在客户端的vipdoc目录下，在vipdoc下解压文件覆盖。
     * 5. 注意事项：如需下载包括当日的，请在更新日期变为当日之后再下载。
     */
    private static final String LDAY_ZIP_URL = "https://data.tdx.com.cn/vipdoc/hsjday.zip";


    /**
     * 首页 > 行情数据下载 > 专业财务数据                 https://www.tdx.com.cn/article/stockfin.html
     *
     *
     * 1. 适用范围：适用于有增强功能集的券商通达信版本和通达信个人收费版本。公式选股预警以及股票池,指标排序时使用
     * -          专业财务数据时需要下载,使用云数据的技术指标不需要下载。
     *
     * 2. 文件包含：财务数据包、股票数据包。
     * 3. 适用场景：首次安装通达信PC端软件或者在专业财务数据界面下载财务数据、股票数据很慢时，可以下载此zip文件。
     * 4. 使用方法：下载后放置到客户端的vipdoc/cw目录下，直接解压到当前文件夹覆盖。
     */
    private static final String CW_ZIP_URL = "https://data.tdx.com.cn/vipdoc/tdxfin.zip";


    public static void main(String[] args) throws Exception {
        refreshTdxLdayTask();
        refreshTdxCwTask();
    }


    /**
     * 更新   通达信 - 盘后数据
     */
    @SneakyThrows
    public static void refreshTdxLdayTask() {
        String projectPath = System.getProperty("user.dir");
        log.info("项目路径：{}", projectPath);


        // 下载目录
        Path downloadDir = Paths.get(projectPath + "/tdx_zip/tdx_download");
        // 解压目录
        Path unzipDir = Paths.get(projectPath + "/tdx_zip/tdx_unzip");
        // 目标目录
        Path finalDir = Paths.get(TDX_PATH + "/vipdoc/test/lday");
        // Path finalDir = Paths.get(projectPath + "/tdx_zip/tdx_final");


        // 删除旧目录
        // FileUtils.deleteDirectory(downloadDir.toFile());
        FileUtils.deleteDirectory(unzipDir.toFile());
        FileUtils.deleteDirectory(finalDir.toFile());

        // 创建目录
        Files.createDirectories(downloadDir);
        Files.createDirectories(unzipDir);
        Files.createDirectories(finalDir);


        // 下载文件
        Path zipFile = downloadDir.resolve("hsjday.zip");
        downloadFile(LDAY_ZIP_URL, zipFile);

        // 解压 ZIP 文件
        unzip(zipFile, unzipDir);

        // 拷贝解压内容到目标目录（Mac会产生   对每个文件[xxx.day]   ->   都会产生1个 对应的 隐藏文件[._xxx.day] ）
        copyDirectory(unzipDir, finalDir);


        // del All   隐藏文件
        deleteHiddenDayFiles(finalDir);


        log.info("全部完成");
    }


    /**
     * 更新   通达信 - 财务数据
     */
    @SneakyThrows
    public static void refreshTdxCwTask() {
        String projectPath = System.getProperty("user.dir");
        log.info("项目路径：{}", projectPath);


        // 下载目录
        Path downloadDir = Paths.get(projectPath + "/tdx_zip/tdx_download/2");
        // 解压目录
        Path unzipDir = Paths.get(projectPath + "/tdx_zip/tdx_unzip/2");
        // 目标目录
        // Path finalDir = Paths.get(TDX_PATH + "/vipdoc/test/cw");
        Path finalDir = Paths.get(projectPath + "/tdx_zip/tdx_final/2");


        // 删除旧目录
        FileUtils.deleteDirectory(downloadDir.toFile());
        FileUtils.deleteDirectory(unzipDir.toFile());
        FileUtils.deleteDirectory(finalDir.toFile());

        // 创建目录
        Files.createDirectories(downloadDir);
        Files.createDirectories(unzipDir);
        Files.createDirectories(finalDir);


        // 下载文件
        Path zipFile = downloadDir.resolve("tdxfin.zip");
        downloadFile(CW_ZIP_URL, zipFile);

        // 解压 ZIP 文件
        unzip(zipFile, unzipDir);

        // 拷贝解压内容到目标目录（Mac会产生   对每个文件[xxx.dat]   ->   都会产生1个 对应的 隐藏文件[.xxx.dat] ）
        copyDirectory(unzipDir, finalDir);


        // del All   隐藏文件（.gpsz302132.dat / .gpcw20250930.zip / ...）
        deleteHiddenDayFiles(finalDir);


        log.info("全部完成");
    }


    public static void downloadFile(String zipUrl, Path destination) throws IOException {
        log.info("开始下载     >>>     zipUrl : {} , destination : {}", zipUrl, destination);


        URL url = new URL(zipUrl);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

        // 设置请求头（模拟浏览器）
        httpConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36");
        httpConn.setRequestProperty("Referer", "https://www.tdx.com.cn/article/vipdata.html");
        // httpConn.setRequestProperty("Referer", "https://www.tdx.com.cn/article/stockfin.html");

        try (InputStream inputStream = httpConn.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(destination.toFile())) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }


        log.info("下载完成     >>>     zipUrl : {} , destination : {}", zipUrl, destination);
    }


    /**
     * 解压 ZIP 文件到指定目录
     *
     * @param zipFilePath ZIP 文件路径
     * @param destDir     解压目标目录
     */
    @SneakyThrows
    public static void unzip(Path zipFilePath, Path destDir) {
        log.info("开始解压     >>>     zipFilePath : {} , destDir : {}", zipFilePath, destDir);


        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath.toFile()));


        ZipEntry entry;
        while ((entry = zis.getNextEntry()) != null) {

            String entryName = entry.getName();

            // 跳过 macOS 特有的隐藏文件和目录
            if (entryName.startsWith("__MACOSX") || entryName.endsWith(".DS_Store")) {
                continue;
            }

            // 标准化路径分隔符
            entryName = entryName.replace("\\", "/");


            File newFile = new File(destDir.toFile(), entryName);
            if (entry.isDirectory()) {

                newFile.mkdirs();

            } else {

                // 创建父目录
                new File(newFile.getParent()).mkdirs();

                // 写入文件内容
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    byte[] buffer = new byte[4096];
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
            }


            zis.closeEntry();
        }


        log.info("解压完成     >>>     zipFilePath : {} , destDir : {}", zipFilePath, destDir);
    }


    /**
     * 拷贝到 目标目录
     *
     * @param sourceDir
     * @param targetDir
     * @throws IOException
     */
    public static void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        log.info("开始拷贝     >>>     sourceDir : {} , targetDir : {}", sourceDir, targetDir);


        Files.walk(sourceDir).forEach(sourcePath -> {
            try {
                Path targetPath = targetDir.resolve(sourceDir.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });


        log.info("拷贝完成     >>>     sourceDir : {} , targetDir : {}", sourceDir, targetDir);
    }


    /**
     * 删除指定目录及其子目录下所有  以.开头的 隐藏文件       ==>     del All     ->     .sh000001sh
     *
     * @param directoryPath 要扫描的目录路径
     */
    public static void deleteHiddenDayFiles(Path directoryPath) {
        File dir = directoryPath.toFile();
        if (!dir.exists() || !dir.isDirectory()) {
            log.info("指定的路径不存在或不是目录: {}", directoryPath);
            return;
        }

        deleteHiddenDayFilesRecursively(dir);
    }

    /**
     * 递归删除    隐藏文件
     *
     * @param file 当前文件或目录
     */
    private static void deleteHiddenDayFilesRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteHiddenDayFilesRecursively(child);
                }
            }
        } else {
            String fileName = file.getName();
            if (fileName.startsWith(".") && fileName.endsWith(".day")) {
                boolean deleted = file.delete();
                if (deleted) {
                    log.debug("已删除 隐藏文件: {}", file.getAbsolutePath());
                } else {
                    log.error("无法删除 隐藏文件: {}", file.getAbsolutePath());
                }
            }
        }
    }


}