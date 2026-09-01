package com.bebopze.tdx.quant.llm;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;


/**
 * 使用 qwen 视觉模型识别验证码图片
 *
 * <p> 模型、接口地址和 API Key 均由 {@code llm.providers.qwen} 配置提供，本类不保存任何凭据 </p>
 *
 * @author: bebopze
 * @date: 2026/8/23
 */
@Component
public class CaptchaRecognizer {


    /**
     * 验证码图片 默认提示词
     */
    public static final String DEFAULT_CAPTCHA_OCR_PROMPT = "4位数字验证码原文";


    @Autowired
    private OpenAiLlmClient llmClient;


    /**
     * 识别本地验证码图片，只返回模型识别出的验证码文本。
     */
    public String recognize(File imageFile) {
        Objects.requireNonNull(imageFile, "imageFile 不能为空");
        return recognize(imageFile.toPath());
    }


    /**
     * 识别本地验证码图片，只返回模型识别出的验证码文本。
     */
    public String recognize(Path imagePath) {
        return llmClient.recognizeCaptcha(imagePath);
    }


}