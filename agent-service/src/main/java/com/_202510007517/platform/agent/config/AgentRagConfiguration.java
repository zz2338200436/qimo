package com._202510007517.platform.agent.config;

import com._202510007517.platform.agent.rag.InMemoryRagIndex;
import com._202510007517.platform.agent.rag.OllamaRagEmbeddingClient;
import com._202510007517.platform.agent.rag.RagChunk;
import com._202510007517.platform.agent.rag.RagDocument;
import com._202510007517.platform.agent.rag.RagEmbeddingClient;
import com._202510007517.platform.agent.rag.RagKnowledgeService;
import com._202510007517.platform.agent.rag.RagMarkdownChunker;
import com._202510007517.platform.agent.rag.RagMarkdownDocumentLoader;
import dev.langchain4j.model.chat.ChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableConfigurationProperties(AgentRagProperties.class)
public class AgentRagConfiguration {
    private static final Logger log = LoggerFactory.getLogger(AgentRagConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "agent.rag", name = "enabled", havingValue = "true")
    @ConditionalOnMissingBean
    RagEmbeddingClient ragEmbeddingClient(AgentRagProperties properties) {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getEmbeddingBaseUrl())
                .build();
        return new OllamaRagEmbeddingClient(restClient, properties.getEmbeddingModel());
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.rag", name = "enabled", havingValue = "true")
    InMemoryRagIndex inMemoryRagIndex(AgentRagProperties properties, RagEmbeddingClient embeddingClient) {
        RagMarkdownDocumentLoader loader = new RagMarkdownDocumentLoader();
        RagMarkdownChunker chunker = new RagMarkdownChunker();
        List<RagDocument> documents = loader.load(properties.getDocumentPaths());
        List<InMemoryRagIndex.IndexedChunk> indexedChunks = new ArrayList<>();
        for (RagDocument document : documents) {
            for (RagChunk chunk : chunker.chunk(document)) {
                try {
                    indexedChunks.add(new InMemoryRagIndex.IndexedChunk(chunk, embeddingClient.embed(chunk.content())));
                } catch (RuntimeException ex) {
                    log.info("failed to embed rag chunk: chunkId={}, reason={}", chunk.chunkId(), ex.getClass().getSimpleName());
                    indexedChunks.add(new InMemoryRagIndex.IndexedChunk(chunk, List.of()));
                }
            }
        }
        InMemoryRagIndex index = new InMemoryRagIndex();
        index.replaceAll(indexedChunks);
        log.info("rag index initialized: documents={}, chunks={}", documents.size(), indexedChunks.size());
        return index;
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.rag", name = "enabled", havingValue = "true")
    RagKnowledgeService ragKnowledgeService(AgentRagProperties properties,
                                            RagEmbeddingClient embeddingClient,
                                            InMemoryRagIndex index,
                                            ObjectProvider<ChatModel> chatModelProvider) {
        return new RagKnowledgeService(properties, embeddingClient, index, chatModelProvider.getIfAvailable());
    }

    @Bean
    @ConditionalOnMissingBean(RagKnowledgeService.class)
    RagKnowledgeService disabledRagKnowledgeService() {
        AgentRagProperties properties = new AgentRagProperties();
        properties.setEnabled(false);
        return new RagKnowledgeService(properties, text -> List.of(), new InMemoryRagIndex(), null);
    }
}
