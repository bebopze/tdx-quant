package com.bebopze.tdx.quant.ai;

import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Path;
import java.util.Objects;


/**
 * 使用 qwen 视觉模型识别验证码图片。
 *
 * <p>模型、接口地址和 API Key 均由 {@code llm.providers.qwen} 配置提供，
 * 本类不保存任何凭据。</p>
 *
 * @author: bebopze
 * @date: 2026/8/23
 */
@Service
public class CaptchaRecognizer {


    public static final String PROVIDER_NAME = "qwen";


    private final OpenAiCompatibleLlmClient llmClient;

    public CaptchaRecognizer(OpenAiCompatibleLlmClient llmClient) {
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient 不能为空");
    }


    /**
     * 识别本地验证码图片，只返回模型识别出的验证码文本。
     */
    public String recognize(Path imagePath) {
        return llmClient.recognizeCaptcha(PROVIDER_NAME, imagePath);
    }


    /**
     * 识别本地验证码图片，只返回模型识别出的验证码文本。
     */
    public String recognize(File imageFile) {
        Objects.requireNonNull(imageFile, "imageFile 不能为空");
        return recognize(imageFile.toPath());
    }


}