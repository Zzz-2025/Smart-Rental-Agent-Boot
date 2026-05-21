package dev.langchain4j.example;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.example.tools.BookingTools;
import dev.langchain4j.example.tools.VehicleTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * =========================== Spring 配置中心 ===========================
 *
 * 这个类负责"组装"整个 AI 客服系统的各个零件。可以把它理解为一个"工厂流水线"：
 *
 *   步骤1: 聊天记忆（chatMemoryProvider）
 *     → 让 AI 记住前后对话，最多保留 5000 个 token（约 3000 个汉字）的上下文
 *
 *   步骤2: 文本嵌入模型（embeddingModel）
 *     → 把文字转换成数学向量，用于后续的"语义搜索"
 *
 *   步骤3: 向量知识库（embeddingStore）
 *     → 把公司政策文档（服务条款、保险政策、还车规则）切成小段，
 *       逐段转换成向量，存入内存向量库
 *
 *   步骤4: 知识检索器（contentRetriever）
 *     → 当用户提问时，从向量库中搜索最相关的 3 条文档片段（最低相似度 50%）
 *
 *   步骤5: 组装 Agent（customerSupportAgent）
 *     → 把大语言模型 + 聊天记忆 + 知识库 + 业务工具 组装成一个完整的 AI 客服
 *
 * 这些组件都是 Spring Bean，会被自动注入到需要它们的地方。
 */
@Configuration
public class CustomerSupportAgentConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CustomerSupportAgentConfiguration.class);

    // ==================== 聊天记忆：让 AI 记住上下文 ====================

    @Bean
    ChatMemoryProvider chatMemoryProvider(TokenCountEstimator tokenizer) {
        return memoryId -> TokenWindowChatMemory.builder()
                .id(memoryId)                     // 用 sessionId 区分不同用户
                .maxTokens(5000, tokenizer)       // 最多保留 5000 token
                .build();
    }

    // ==================== 嵌入模型：把文字变成数学向量 ====================

    @Bean
    EmbeddingModel embeddingModel() {
        // AllMiniLmL6V2 是一个轻量级嵌入模型，本地运行，无需联网
        return new AllMiniLmL6V2EmbeddingModel();
    }

    // ==================== 向量知识库：存储公司政策文档 ====================

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel, TokenCountEstimator tokenizer) throws IOException {
        // 内存向量库（重启后需重新导入，适合演示；生产环境应换 Redis/Postgres）
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 文档切分器：每 100 字切一段，段间不重叠
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(100, 0, tokenizer);

        // 文档导入器：切分 → 嵌入 → 存库
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentSplitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        // 导入服务条款文档
        Resource termsResource = resolver.getResource("classpath:miles-of-smiles-terms-of-use.txt");
        if (termsResource.exists()) {
            log.info("正在将服务条款文档导入向量存储: {}", termsResource.getFilename());
            Document termsDoc = loadDocument(termsResource.getFile().toPath(), new TextDocumentParser());
            ingestor.ingest(termsDoc);
        }

        // 批量导入 resources/docs/ 下所有 .txt 知识库文件（保险政策、还车规则等）
        Resource[] docResources = resolver.getResources("classpath:docs/*.txt");
        for (Resource resource : docResources) {
            log.info("正在将知识库文档导入向量存储: {}", resource.getFilename());
            Document document = loadDocument(resource.getFile().toPath(), new TextDocumentParser());
            ingestor.ingest(document);
        }

        log.info("知识库文档导入完成，共导入 {} 个文档", docResources.length + 1);
        return embeddingStore;
    }

    // ==================== 知识检索器：语义搜索相关文档 ====================

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        int maxResults = 3;       // 最多返回 3 条最相关的文档片段
        double minScore = 0.5;    // 最低相似度 50%，低于此分数的结果丢弃

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    // ==================== Token 计数器：估算文本消耗的 Token 数 ====================

    @Bean
    TokenCountEstimator tokenCountEstimator() {
        // 使用 OpenAI GPT-4o-mini 的 token 计算规则
        return new OpenAiTokenCountEstimator(GPT_4_O_MINI);
    }

    // ==================== 组装 AI Agent ====================

    /**
     * 把所有零件拼装成一个完整的 AI 客服：
     *   - chatModel:         大语言模型（"大脑"）
     *   - chatMemoryProvider: 聊天记忆（"记性"）
     *   - contentRetriever:  知识库检索（"翻手册"）
     *   - bookingTools:      订单操作能力（"办业务的手"）
     *   - vehicleTools:      车辆查询能力（"查库存的眼"）
     */
    @Bean
    CustomerSupportAgent customerSupportAgent(ChatModel chatModel,
                                              ChatMemoryProvider chatMemoryProvider,
                                              ContentRetriever contentRetriever,
                                              BookingTools bookingTools,
                                              VehicleTools vehicleTools) {
        return AiServices.builder(CustomerSupportAgent.class)
                .chatModel(chatModel)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .tools(bookingTools, vehicleTools)
                .build();
    }
}
