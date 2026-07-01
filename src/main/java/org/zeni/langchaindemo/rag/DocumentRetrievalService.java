package org.zeni.langchaindemo.rag;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Thin wrapper around an {@link EmbeddingStoreContentRetriever} so the
 * assistant can query the ingested-documents knowledge base from a tool.
 *
 * <p>Exposed as a regular service (not a {@link ContentRetriever} bean) to
 * avoid colliding with the existing web-search {@code ContentRetriever} bean
 * - the LLM decides between the two via {@code @Tool} methods.</p>
 */
@Service
public class DocumentRetrievalService {

    private final ContentRetriever retriever;

    public DocumentRetrievalService(EmbeddingStore<TextSegment> embeddingStore,
                                    EmbeddingModel embeddingModel,
                                    @Value("${rag.max-results}") int maxResults,
                                    @Value("${rag.min-score}") double minScore) {
        this.retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .maxResults(maxResults)
                .minScore(minScore)
                .build();
    }

    /**
     * Retrieve relevant text segments from the ingested documents.
     */
    public List<Content> retrieve(String query) {
        return retriever.retrieve(Query.from(query));
    }
}

