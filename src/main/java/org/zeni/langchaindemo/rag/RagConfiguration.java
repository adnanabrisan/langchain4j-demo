package org.zeni.langchaindemo.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * RAG infrastructure: an Ollama embedding model + an in-memory vector store.
 *
 * <p>For production swap {@link InMemoryEmbeddingStore} for PGVector / Qdrant /
 * Chroma / Redis by replacing this single bean.</p>
 */
@Configuration
public class RagConfiguration {

    @Bean
    EmbeddingModel embeddingModel(
            @Value("${ollama.embedding.base-url}") String baseUrl,
            @Value("${ollama.embedding.model-name}") String modelName) {
        return OllamaEmbeddingModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .timeout(Duration.ofMinutes(2))
                .logRequests(true)
                .logResponses(false)
                .build();
    }

    @Bean
    EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}

