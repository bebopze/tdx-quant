package com.bebopze.tdx.quant.llm;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * OpenAI Chat Completions 兼容供应商配置
 *
 * <p> API Key 应通过环境变量或密钥管理服务注入，禁止写死在源码或提交到 Git </p>
 *
 * @author: bebopze
 * @date: 2026/8/23
 */
@Data
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProviderProperties {


    /**
     * 未显式指定供应商时使用的配置名。
     */
    private String defaultProvider;
    /**
     * 单张图片允许的最大字节数，默认 10 MiB。
     */
    private long maxImageBytes = 10 * 1024 * 1024L;
    /**
     * key 为业务使用的供应商名称，例如 qwen、doubao、openai。
     */
    private Map<String, Provider> providers = new LinkedHashMap<>();


    @Data
    public static class Provider {

        /**
         * 服务根地址，例如 https://api.openai.com
         */
        private String baseUrl;

        /**
         * Chat Completions 路径，例如 /v1/chat/completions。
         */
        private String chatCompletionsPath = "/v1/chat/completions";

        private String apiKey;
        private String model;

        /**
         * 当前 model 是否接受图片输入；用于在发请求前快速失败。
         */
        private boolean vision;

        /**
         * 留空时不发送，避免某些推理模型拒绝 temperature 参数。
         */
        private Double temperature;

        /**
         * 传统 OpenAI-compatible 输出上限参数。
         */
        private Integer maxTokens;

        /**
         * OpenAI 新模型使用的输出上限参数。不要和 maxTokens 同时配置。
         */
        private Integer maxCompletionTokens;

        /**
         * 透传供应商特有参数，例如 enable_thinking。
         */
        private Map<String, Object> extraBody = new LinkedHashMap<>();
    }


}