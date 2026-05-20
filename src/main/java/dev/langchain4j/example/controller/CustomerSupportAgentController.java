package dev.langchain4j.example.controller;

import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.example.CustomerSupportAgent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
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
 * HTTP 控制器。暴露 /customerSupportAgent 端点接收前端聊天请求，
 * 支持纯文本和带图片上传（多模态识别）两种交互方式。
 */
@RestController
public class CustomerSupportAgentController {

    private final CustomerSupportAgent customerSupportAgent;
    private final ChatModel visionModel;

    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/png", "image/jpeg", "image/gif", "image/webp", "image/bmp"
    );

    private static final String VISION_SYSTEM_PROMPT = """
            You are a text extraction assistant. Carefully examine the image and extract ALL visible text, paying special attention to:
            1. Booking numbers (format like MS-XXX)
            2. Customer names (first name and last name)
            3. Dates (in any format)
            4. Any other booking-related information

            Return ONLY the extracted text exactly as it appears. Do not add any commentary.
            """;

    public CustomerSupportAgentController(
            CustomerSupportAgent customerSupportAgent,
            @Value("${langchain4j.open-ai.chat-model.api-key}") String apiKey,
            @Value("${langchain4j.open-ai.chat-model.base-url}") String baseUrl) {
        this.customerSupportAgent = customerSupportAgent;
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

    @GetMapping("/customerSupportAgent")
    public String customerSupportAgent(@RequestParam String sessionId, @RequestParam String userMessage) {
        Result<String> result = customerSupportAgent.answer(sessionId, userMessage);
        return result.content();
    }

    @PostMapping("/customerSupportAgent")
    public String customerSupportAgentPost(@RequestBody Map<String, String> body) {
        String sessionId = body.get("sessionId");
        String userMessage = body.get("userMessage");
        Result<String> result = customerSupportAgent.answer(sessionId, userMessage);
        return result.content();
    }

    @PostMapping(value = "/customerSupportAgent/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String customerSupportAgentUpload(
            @RequestParam("sessionId") String sessionId,
            @RequestParam("userMessage") String userMessage,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        String combinedMessage;

        if (file != null && !file.isEmpty()) {
            String contentType = file.getContentType();
            if (contentType == null || !SUPPORTED_IMAGE_TYPES.contains(contentType)) {
                throw new ResponseStatusException(
                        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                        "Unsupported image format: " + contentType + ". Supported: " + SUPPORTED_IMAGE_TYPES);
            }

            String extractedText = extractTextFromImage(file);
            combinedMessage = String.format("""
                    [Image content from uploaded file:
                    %s]

                    %s""", extractedText.trim(), userMessage.trim());
        } else {
            combinedMessage = userMessage;
        }

        Result<String> result = customerSupportAgent.answer(sessionId, combinedMessage);
        return result.content();
    }

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
}
