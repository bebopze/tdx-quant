package com.bebopze.tdx.quant.llm;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;


/**
 * 使用  视觉模型（默认：qwen） 识别验证码图片
 *
 * @author: bebopze
 * @date: 2026/8/23
 */
@Component
@AllArgsConstructor
public class CaptchaRecognizer {


    /**
     * 验证码图片 默认提示词
     */
    public static final String DEFAULT_CAPTCHA_OCR_PROMPT = "4位数字验证码原文";


    @Autowired
    private OpenAiLlmClient llmClient;


    /**
     * 使用 默认供应商 识别验证码文件
     *
     * @param imageFile 验证码图片文件
     * @return
     */
    public String recognizeCaptcha(File imageFile) {
        return recognizeCaptcha(requireFile(imageFile));
    }

    /**
     * 使用 默认供应商 识别验证码文件
     *
     * @param imagePath 验证码图片路径
     * @return
     */
    public String recognizeCaptcha(Path imagePath) {
        return recognizeCaptcha(null, imagePath);
    }

    /**
     * 使用 指定供应商 识别验证码文件
     *
     * @param providerName 供应商名称
     * @param imageFile    验证码图片文件
     * @return
     */
    public String recognizeCaptcha(String providerName, File imageFile) {
        return recognizeCaptcha(providerName, requireFile(imageFile));
    }

    /**
     * 使用 指定供应商 识别验证码文件
     *
     * @param providerName 供应商名称
     * @param imagePath    验证码图片路径
     * @return
     */
    public String recognizeCaptcha(String providerName, Path imagePath) {
        return llmClient.analyzeImage(providerName, imagePath, DEFAULT_CAPTCHA_OCR_PROMPT);
    }


    private static Path requireFile(File imageFile) {
        if (imageFile == null) {
            throw new IllegalArgumentException("imageFile 不能为空");
        }
        return imageFile.toPath();
    }


}