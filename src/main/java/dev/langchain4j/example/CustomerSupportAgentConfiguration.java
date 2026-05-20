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
 * Spring 配置类。初始化 AI Agent 所需的基础设施：
 * 聊天记忆窗口（5000 token）、嵌入模型与向量存储、RAG 知识库检索器，
 * 以及绑定 BookingTools / VehicleTools / ContentRetriever 的 Agent 实例。
 */
@Configuration
public class CustomerSupportAgentConfiguration {

    private static final Logger log = LoggerFactory.getLogger(CustomerSupportAgentConfiguration.class);

    @Bean
    ChatMemoryProvider chatMemoryProvider(TokenCountEstimator tokenizer) {
        return memoryId -> TokenWindowChatMemory.builder()
                .id(memoryId)
                .maxTokens(5000, tokenizer)
                .build();
    }

    @Bean
    EmbeddingModel embeddingModel() {
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel, TokenCountEstimator tokenizer) throws IOException {
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        DocumentSplitter documentSplitter = DocumentSplitters.recursive(100, 0, tokenizer);
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentSplitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();

        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        // 1. 加载原有的服务条款文档（取消政策等）
        Resource termsResource = resolver.getResource("classpath:miles-of-smiles-terms-of-use.txt");
        if (termsResource.exists()) {
            log.info("正在将服务条款文档导入向量存储: {}", termsResource.getFilename());
            Document termsDoc = loadDocument(termsResource.getFile().toPath(), new TextDocumentParser());
            ingestor.ingest(termsDoc);
        }

        // 2. 扫描 resources/docs/ 目录下所有 .txt 文件，批量导入向量库
        Resource[] docResources = resolver.getResources("classpath:docs/*.txt");

        for (Resource resource : docResources) {
            log.info("正在将知识库文档导入向量存储: {}", resource.getFilename());
            Document document = loadDocument(resource.getFile().toPath(), new TextDocumentParser());
            ingestor.ingest(document);
        }

        log.info("知识库文档导入完成，共导入 {} 个文档", docResources.length + 1);
        return embeddingStore;
    }

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {
        int maxResults = 3;
        double minScore = 0.5;

        return EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    @Bean
    TokenCountEstimator tokenCountEstimator() {
        return new OpenAiTokenCountEstimator(GPT_4_O_MINI);
    }

    /**
     * 手动构建 CustomerSupportAgent，将 ContentRetriever (RAG) 与
     * BookingTools / VehicleTools (MySQL) 双核联动注入 Agent。
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
