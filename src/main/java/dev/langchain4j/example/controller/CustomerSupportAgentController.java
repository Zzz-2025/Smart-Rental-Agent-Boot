package dev.langchain4j.example.controller;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.example.CustomerSupportAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import jakarta.servlet.http.HttpServletRequest;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

/**
 * =========================== HTTP 接口层（Controller） ===========================
 *
 * 前端页面通过这两个接口与后端通信：
 *
 *   POST /customerSupportAgent          → 纯文本对话
 *     - 前端发送 { sessionId, userMessage }
 *     - 后端调用 AI Agent 的 answer() 方法
 *     - 返回 AI 的完整回复文本
 *
 *   POST /customerSupportAgent/upload   → 上传图片 + 文字
 *     - 用户上传截图（订单号、身份证等），视觉模型先提取图片中的文字
 *     - 然后把提取的文字和用户消息合并发给 AI Agent
 *     - 返回 AI 的完整回复文本
 *
 * 两个接口都受 Redis 限流保护：同一 IP 每分钟最多 10 次请求。
 */
@RestController
public class CustomerSupportAgentController {

    private static final Logger log = LoggerFactory.getLogger(CustomerSupportAgentController.class);

    private final CustomerSupportAgent customerSupportAgent;
    private final ChatModel visionModel;
    private final RedissonClient redissonClient;
    private final Tracer tracer;
    private final boolean tracingEnabled;

    // 支持的图片格式
    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp"
    );

    // 视觉模型的系统提示词：让它只提取文字，不加评论
    private static final String VISION_SYSTEM_PROMPT = """
            You are a text extraction assistant. Carefully examine the image and extract ALL visible text, paying special attention to:
            1. Booking numbers (format like MS-XXX)
            2. Customer names (first name and last name)
            3. Dates (in any format)
            4. Any other booking-related information

            Return ONLY the extracted text exactly as it appears. Do not add any commentary.
            """;

    /**
     * 构造函数：Spring 自动注入三个依赖。
     *
     * @param customerSupportAgent  核心 AI 客服（由 Configuration 类组装）
     * @param redissonClient        Redis 客户端（用于限流）
     * @param apiKey                大模型 API 密钥（从配置文件读取）
     * @param baseUrl               大模型 API 地址（从配置文件读取）
     */
    public CustomerSupportAgentController(
            CustomerSupportAgent customerSupportAgent,
            RedissonClient redissonClient,
            Tracer tracer,
            @Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.base-url}") String baseUrl,
            @Value("${langfuse.public-key:}") String langfusePublicKey) {
        this.customerSupportAgent = customerSupportAgent;
        this.redissonClient = redissonClient;
        this.tracer = tracer;
        this.tracingEnabled = langfusePublicKey != null && !langfusePublicKey.isBlank();

        // 视觉模型：用于识别用户上传图片中的文字（如订单截图）
        // 这里使用通义千问的 qwen-vl-max 多模态模型
        this.visionModel = OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName("qwen-vl-max")
                .temperature(0.0)
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    // ==================== 纯文本对话接口 ====================

    @PostMapping(value = "/customerSupportAgent")
    public String chat(@RequestBody Map<String, String> body,
                       HttpServletRequest request) {
        // 第一步：检查 IP 是否超过限流阈值
        String ip = getClientIp(request);
        if (isRateLimited(ip)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "请求过于频繁，同一IP每分钟最多调用10次，请稍后再试。");
        }

        // 第二步：从请求中取出 sessionId 和用户消息
        String sessionId = body.get("sessionId");
        String userMessage = body.get("userMessage");
        log.info("Request: ip={}, sessionId={}", ip, sessionId);

        // 第三步：把消息交给 AI Agent 处理（包裹在 Langfuse Trace 中）
        if (tracingEnabled) {
            Span traceSpan = tracer.spanBuilder("customer-support-agent")
                    .setAttribute("langfuse.trace.input", userMessage)
                    .setAttribute("session.id", sessionId)
                    .startSpan();
            try (Scope scope = traceSpan.makeCurrent()) {
                String answer = customerSupportAgent.answer(sessionId, userMessage);
                traceSpan.setAttribute("langfuse.trace.output", answer);
                return answer;
            } catch (Exception e) {
                traceSpan.recordException(e);
                traceSpan.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                traceSpan.end();
            }
        }
        return customerSupportAgent.answer(sessionId, userMessage);
    }

    // ==================== 图片上传 + 对话接口 ====================

    @PostMapping(value = "/customerSupportAgent/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String upload(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("userMessage") String userMessage,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpServletRequest request) {

        String ip = getClientIp(request);
        if (isRateLimited(ip)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                    "请求过于频繁，请稍后再试。");
        }

        String combinedMessage;
        if (file != null && !file.isEmpty()) {
            // 校验图片格式
            String contentType = file.getContentType();
            if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType)) {
                throw new ResponseStatusException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Unsupported image format: " + contentType + ". Supported: " + SUPPORTED_IMAGE_TYPES);
            }

            // 先用视觉模型提取图片中的文字
            String extractedText = extractTextFromImage(file);

            // 把提取的文字和用户消息合并，一起发给 AI Agent
            combinedMessage = String.format("""
                    [Image content from uploaded file:
                    %s]

                    %s""", extractedText.trim(), userMessage.trim());
        } else {
            combinedMessage = userMessage;
        }

        if (tracingEnabled) {
            Span traceSpan = tracer.spanBuilder("customer-support-agent")
                    .setAttribute("langfuse.trace.input", combinedMessage)
                    .setAttribute("session.id", sessionId)
                    .startSpan();
            try (Scope scope = traceSpan.makeCurrent()) {
                String answer = customerSupportAgent.answer(sessionId, combinedMessage);
                traceSpan.setAttribute("langfuse.trace.output", answer);
                return answer;
            } catch (Exception e) {
                traceSpan.recordException(e);
                traceSpan.setStatus(StatusCode.ERROR);
                throw e;
            } finally {
                traceSpan.end();
            }
        }
        return customerSupportAgent.answer(sessionId, combinedMessage);
    }

    // ==================== 辅助方法 ====================

    /**
     * 用视觉模型提取图片中的文字。
     * 流程：读取图片字节 → Base64 编码 → 发给 qwen-vl-max → 返回识别出的文字
     */
    private String extractTextFromImage(MultipartFile file) {
        try {
            byte[] imageBytes = file.getBytes();
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);

            SystemMessage systemMsg = SystemMessage.from(VISION_SYSTEM_PROMPT);
            UserMessage userMsg = UserMessage.from(
                    TextContent.from("Extract all visible text from this image."),
                    ImageContent.from(base64Data, file.getContentType())
            );

            ChatResponse response = visionModel.chat(systemMsg, userMsg);
            return response.aiMessage().text();
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Failed to process image: " + e.getMessage());
        }
    }

    /**
     * 获取客户端真实 IP。
     * 优先从 X-Forwarded-For 头取（适用于代理/负载均衡后），取不到再用 RemoteAddr。
     */
    private String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 基于 Redis 的 IP 限流检查。
     * 每个 IP 每分钟最多 10 次请求，超过则返回 true。
     */
    private boolean isRateLimited(String ip) {
        RRateLimiter limiter = redissonClient.getRateLimiter("rate_limit:" + ip);
        limiter.trySetRate(RateType.OVERALL, 10, Duration.ofMinutes(1));
        return !limiter.tryAcquire();
    }
}
