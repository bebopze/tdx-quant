package com.bebopze.tdx.quant.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;


/**
 * OpenAI Chat Completions 兼容供应商配置。
 *
 * <p>API Key 应通过环境变量或密钥管理服务注入，禁止写死在源码或提交到 Git。</p>
 *
 * @author: bebopze
 * @date: 2026/8/23
 */
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

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public long getMaxImageBytes() {
        return maxImageBytes;
    }

    public void setMaxImageBytes(long maxImageBytes) {
        this.maxImageBytes = maxImageBytes;
    }

    public Map<String, Provider> getProviders() {
        return providers;
    }

    public void setProviders(Map<String, Provider> providers) {
        this.providers = providers == null ? new LinkedHashMap<>() : providers;
    }

    public static class Provider {

        /**
         * 服务根地址，例如 https://api.openai.com。
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

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getChatCompletionsPath() {
            return chatCompletionsPath;
        }

        public void setChatCompletionsPath(String chatCompletionsPath) {
            this.chatCompletionsPath = chatCompletionsPath;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public boolean isVision() {
            return vision;
        }

        public void setVision(boolean vision) {
            this.vision = vision;
        }

        public Double getTemperature() {
            return temperature;
        }

        public void setTemperature(Double temperature) {
            this.temperature = temperature;
        }

        public Integer getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(Integer maxTokens) {
            this.maxTokens = maxTokens;
        }

        public Integer getMaxCompletionTokens() {
            return maxCompletionTokens;
        }

        public void setMaxCompletionTokens(Integer maxCompletionTokens) {
            this.maxCompletionTokens = maxCompletionTokens;
        }

        public Map<String, Object> getExtraBody() {
            return extraBody;
        }

        public void setExtraBody(Map<String, Object> extraBody) {
            this.extraBody = extraBody == null ? new LinkedHashMap<>() : extraBody;
        }
    }


}