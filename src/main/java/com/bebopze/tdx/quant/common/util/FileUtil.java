package com.bebopze.tdx.quant.common.util;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;


/**
 * file
 *
 * @author: bebopze
 * @date: 2026/1/28
 */
public class FileUtil {


    /**
     * 尝试使用不同编码读取文件内容
     *
     * @param file        文件
     * @param charsetName 字符集name
     * @return 文件行列表
     * @throws IOException 读取异常
     */
    public static List<String> readLines(File file, String charsetName) throws IOException {
        // 首先尝试使用 GBK 编码（通达信文件通常是 GBK -> GB2312的超集）
        try {
            return FileUtils.readLines(file, charsetName);
        } catch (MalformedInputException e) {
            // 如果 GBK 失败是因为遇到了无法解析的字符，使用容错的 GBK 解码
            return readFileWithFaultTolerantDecoding(file, Charset.forName(charsetName));
        }
    }


    /**
     * 使用容错解码方式读取文件
     *
     * @param file    文件
     * @param charset 字符集
     * @return 文件行列表
     * @throws IOException 读取异常
     */
    private static List<String> readFileWithFaultTolerantDecoding(File file, Charset charset) throws IOException {
        byte[] bytes = Files.readAllBytes(file.toPath());

        // 使用错误替换策略，将无法解析的字符替换为替代字符
        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        CharsetDecoder decoder = charset.newDecoder()
                                        .onMalformedInput(CodingErrorAction.REPLACE)
                                        .onUnmappableCharacter(CodingErrorAction.REPLACE);

        CharBuffer charBuffer = decoder.decode(byteBuffer);
        String content = charBuffer.toString();

        // 按行分割
        return Arrays.asList(content.split("\\r?\\n"));
    }

}