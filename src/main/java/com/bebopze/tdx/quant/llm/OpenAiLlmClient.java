package com.bebopze.tdx.quant.llm;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * 基于 Spring AI 的 OpenAI Chat Completions 兼容客户端
 *
 *
 * DeepSeek、Qwen、豆包、MiMo、OpenAI 或其他兼容服务的差异全部由配置描述，业务代码只依赖本类。
 * ChatModel 按供应商延迟创建并缓存，不会因为未配置非默认供应商的API Key 而影响应用启动。
 *
 * @author: bebopze
 * @date: 2026/8/23
 */
@Component
public class OpenAiLlmClient {


    /**
     * 验证码图片 默认提示词
     */
    public static final String DEFAULT_CAPTCHA_OCR_PROMPT = "4位数字验证码原文";


    private final LlmProviderProperties properties;
    private final ChatModelFactory chatModelFactory;
    private final ConcurrentMap<String, ChatModel> chatModels = new ConcurrentHashMap<>();


    @Autowired
    public OpenAiLlmClient(LlmProviderProperties properties) {
        this(properties, OpenAiLlmClient::createChatModel);
    }


    /**
     * 供单元测试替换真实网络模型。
     */
    OpenAiLlmClient(LlmProviderProperties properties, ChatModelFactory chatModelFactory) {
        this.properties = Objects.requireNonNull(properties, "properties 不能为空");
        this.chatModelFactory = Objects.requireNonNull(chatModelFactory, "chatModelFactory 不能为空");
    }

    /**
     * 使用 {@code llm.default-provider} 完成纯文本对话。
     */
    public String chat(String prompt) {
        return chat(requiredDefaultProviderName(), prompt);
    }

    /**
     * 使用指定供应商完成纯文本对话。
     */
    public String chat(String providerName, String prompt) {
        requireText(prompt, "prompt");
        return responseText(modelFor(providerName).call(new Prompt(prompt)));
    }

    /**
     * 使用默认供应商识别验证码文件。
     */
    public String recognizeCaptcha(File imageFile) {
        return recognizeCaptcha(requiredDefaultProviderName(), requireFile(imageFile));
    }

    /**
     * 使用指定供应商识别验证码文件。
     */
    public String recognizeCaptcha(String providerName, File imageFile) {
        return recognizeCaptcha(providerName, requireFile(imageFile));
    }

    /**
     * 使用默认供应商识别验证码文件。
     */
    public String recognizeCaptcha(Path imagePath) {
        return recognizeCaptcha(requiredDefaultProviderName(), imagePath);
    }

    /**
     * 使用指定供应商识别验证码文件。
     */
    public String recognizeCaptcha(String providerName, Path imagePath) {
        return analyzeImage(providerName, imagePath, DEFAULT_CAPTCHA_OCR_PROMPT);
    }

    /**
     * 使用默认供应商按自定义提示词分析图片。
     */
    public String analyzeImage(Path imagePath, String prompt) {
        return analyzeImage(requiredDefaultProviderName(), imagePath, prompt);
    }

    /**
     * 使用指定供应商按自定义提示词分析本地图片。
     *
     * <p>Spring AI 会将本地图片转换为兼容 Chat Completions 的 Base64 data URL。
     * 图片会发送给配置的第三方模型服务，请只处理允许上传的数据。</p>
     */
    public String analyzeImage(String providerName, Path imagePath, String prompt) {
        requireText(prompt, "prompt");
        LlmProviderProperties.Provider provider = providerFor(providerName);
        if (!provider.isVision()) {
            throw new IllegalArgumentException("供应商 " + providerName + " 的模型 " + provider.getModel()
                                                       + " 未配置图片输入能力，请切换视觉模型或将 vision 配置为 true");
        }

        Path checkedImage = validateImage(imagePath);
        MediaType mediaType = MediaTypeFactory.getMediaType(checkedImage.getFileName().toString())
                                              .filter(type -> "image".equals(type.getType()))
                                              .orElseThrow(() -> new IllegalArgumentException("不支持或无法识别图片类型: " + checkedImage));

        UserMessage message = UserMessage.builder()
                                         .text(prompt.trim())
                                         .media(new Media(mediaType, new FileSystemResource(checkedImage)))
                                         .build();
        return responseText(modelFor(providerName).call(new Prompt(message)));
    }

    private Path validateImage(Path imagePath) {
        if (imagePath == null) {
            throw new IllegalArgumentException("imagePath 不能为空");
        }
        Path normalized = imagePath.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalized) || !Files.isReadable(normalized)) {
            throw new IllegalArgumentException("图片文件不存在或不可读: " + normalized);
        }

        long maxImageBytes = properties.getMaxImageBytes();
        if (maxImageBytes <= 0) {
            throw new IllegalArgumentException("llm.max-image-bytes 必须大于 0");
        }
        try {
            long size = Files.size(normalized);
            if (size == 0) {
                throw new IllegalArgumentException("图片文件不能为空: " + normalized);
            }
            if (size > maxImageBytes) {
                throw new IllegalArgumentException("图片文件过大: " + size + " bytes，最大允许 "
                                                           + maxImageBytes + " bytes");
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("读取图片文件失败: " + normalized, e);
        }
        return normalized;
    }

    private ChatModel modelFor(String providerName) {
        String checkedName = requireText(providerName, "providerName");
        LlmProviderProperties.Provider provider = providerFor(checkedName);
        return chatModels.computeIfAbsent(checkedName, ignored -> chatModelFactory.create(checkedName, provider));
    }

    private LlmProviderProperties.Provider providerFor(String providerName) {
        String checkedName = requireText(providerName, "providerName");
        LlmProviderProperties.Provider provider = properties.getProviders().get(checkedName);
        if (provider == null) {
            throw new IllegalArgumentException("未配置大模型供应商: " + checkedName
                                                       + "，可用配置: " + properties.getProviders().keySet());
        }
        return provider;
    }

    private String requiredDefaultProviderName() {
        return requireText(properties.getDefaultProvider(), "llm.default-provider");
    }

    static ChatModel createChatModel(String providerName, LlmProviderProperties.Provider provider) {
        String baseUrl = requireHttpUrl(provider.getBaseUrl(), providerName + ".base-url");
        String completionsPath = requirePath(provider.getChatCompletionsPath(),
                                             providerName + ".chat-completions-path");

        OpenAiChatOptions.Builder options = OpenAiChatOptions.builder()
                                                             .baseUrl(chatApiBaseUrl(baseUrl, completionsPath, providerName))
                                                             .apiKey(requireText(provider.getApiKey(), providerName + ".api-key"))
                                                             .model(requireText(provider.getModel(), providerName + ".model"));
        if (provider.getTemperature() != null) {
            options.temperature(provider.getTemperature());
        }
        if (provider.getMaxTokens() != null) {
            if (provider.getMaxTokens() <= 0) {
                throw new IllegalArgumentException(providerName + ".max-tokens 必须大于 0");
            }
            options.maxTokens(provider.getMaxTokens());
        }
        if (provider.getMaxCompletionTokens() != null) {
            if (provider.getMaxCompletionTokens() <= 0) {
                throw new IllegalArgumentException(providerName + ".max-completion-tokens 必须大于 0");
            }
            if (provider.getMaxTokens() != null) {
                throw new IllegalArgumentException(providerName
                                                           + ".max-tokens 和 max-completion-tokens 不能同时配置");
            }
            options.maxCompletionTokens(provider.getMaxCompletionTokens());
        }
        if (!provider.getExtraBody().isEmpty()) {
            options.extraBody(provider.getExtraBody());
        }

        return OpenAiChatModel.builder()
                              .options(options.build())
                              .build();
    }

    /**
     * Spring AI 2 使用 OpenAI 官方 Java SDK，SDK 会在 base URL 后固定追加
     * {@code /chat/completions}，因此把旧配置中的完整路径转换为 API base URL。
     */
    private static String chatApiBaseUrl(String baseUrl, String completionsPath, String providerName) {
        final String endpointSuffix = "/chat/completions";
        if (!completionsPath.endsWith(endpointSuffix)) {
            throw new IllegalArgumentException(providerName + ".chat-completions-path 必须以 "
                                                       + endpointSuffix + " 结尾，当前值: " + completionsPath);
        }
        String apiPrefix = completionsPath.substring(0, completionsPath.length() - endpointSuffix.length());
        return baseUrl + apiPrefix;
    }

    private static String responseText(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            throw new IllegalStateException("模型未返回可用结果");
        }
        String text = response.getResult().getOutput().getText();
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("模型返回内容为空");
        }
        return text.trim();
    }

    private static Path requireFile(File imageFile) {
        if (imageFile == null) {
            throw new IllegalArgumentException("imageFile 不能为空");
        }
        return imageFile.toPath();
    }

    private static String requireHttpUrl(String value, String field) {
        String checked = requireText(value, field);
        URI uri;
        try {
            uri = URI.create(checked);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(field + " 不是合法 URL: " + checked, e);
        }
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) {
            throw new IllegalArgumentException(field + " 必须是 http/https URL: " + checked);
        }
        return checked.replaceAll("/+$", "");
    }

    private static String requirePath(String value, String field) {
        String checked = requireText(value, field);
        return checked.startsWith("/") ? checked : "/" + checked;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " 不能为空");
        }
        return value.trim();
    }

    @FunctionalInterface
    interface ChatModelFactory {
        ChatModel create(String providerName, LlmProviderProperties.Provider provider);
    }


}