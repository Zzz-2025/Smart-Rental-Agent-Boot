package dev.langchain4j.example;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.TokenWindowChatMemory;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiTokenCountEstimator;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

import static dev.langchain4j.data.document.loader.FileSystemDocumentLoader.loadDocument;
import static dev.langchain4j.model.openai.OpenAiChatModelName.GPT_4_O_MINI;

/**
 * Spring 配置类。初始化 AI Agent 所需的基础设施：
 * 聊天记忆窗口（5000 token）、嵌入模型与向量存储、服务条款 RAG 检索器。
 */
@Configuration
public class CustomerSupportAgentConfiguration {

    @Bean
    ChatMemoryProvider chatMemoryProvider(TokenCountEstimator tokenizer) {
        return memoryId -> TokenWindowChatMemory.builder()
                .id(memoryId)
                .maxTokens(5000, tokenizer)
                .build();
    }

    @Bean
    EmbeddingModel embeddingModel() {
        // 虽然不是最好的嵌入模型，但对于这个演示来说足够了
        return new AllMiniLmL6V2EmbeddingModel();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore(EmbeddingModel embeddingModel, ResourceLoader resourceLoader, TokenCountEstimator tokenizer) throws IOException {

        // 通常情况下，你的嵌入存储应该已经填充好了数据。
        // 但是为了演示目的，我们将在这里完成全过程：

        // 1. 创建内存嵌入存储
        EmbeddingStore<TextSegment> embeddingStore = new InMemoryEmbeddingStore<>();

        // 2. 加载示例文档（"Miles of Smiles" 使用条款）
        Resource resource = resourceLoader.getResource("classpath:miles-of-smiles-terms-of-use.txt");
        Document document = loadDocument(resource.getFile().toPath(), new TextDocumentParser());

        // 3. 将文档切分为每段 100 token 的片段
        // 4. 将片段转换为向量嵌入
        // 5. 将嵌入存储到向量存储中
        // 以上步骤都可以手动完成，但这里使用 EmbeddingStoreIngestor 来自动化处理：
        DocumentSplitter documentSplitter = DocumentSplitters.recursive(100, 0, tokenizer);
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                .documentSplitter(documentSplitter)
                .embeddingModel(embeddingModel)
                .embeddingStore(embeddingStore)
                .build();
        ingestor.ingest(document);

        return embeddingStore;
    }

    @Bean
    ContentRetriever contentRetriever(EmbeddingStore<TextSegment> embeddingStore, EmbeddingModel embeddingModel) {

        // 你需要调整这些参数来找到最佳配置，
        // 这取决于多种因素，例如：
        // - 你的数据特性
        // - 你使用的嵌入模型
        int maxResults = 1;
        double minScore = 0.6;

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
}
