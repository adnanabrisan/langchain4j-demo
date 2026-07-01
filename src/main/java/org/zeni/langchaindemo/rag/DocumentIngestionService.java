package org.zeni.langchaindemo.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Ingests uploaded files (PDF, DOCX, HTML, Markdown, plain text, source code,
 * ...) into the embedding store using Apache Tika for parsing and a recursive
 * splitter for chunking.
 *
 * <p>Splits documents with {@link DocumentSplitters#recursive(int, int)}, then
 * embeds the resulting {@link TextSegment}s with the configured
 * {@link EmbeddingModel} and stores them in the shared {@link EmbeddingStore}.</p>
 */
@Service
public class DocumentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentSplitter splitter;

    public DocumentIngestionService(EmbeddingModel embeddingModel,
                                    EmbeddingStore<TextSegment> embeddingStore,
                                    @Value("${rag.max-segment-size-in-chars}") int maxSegmentSizeInChars,
                                    @Value("${rag.max-overlap-size-in-chars}") int maxOverlapSizeInChars) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.splitter = DocumentSplitters.recursive(maxSegmentSizeInChars, maxOverlapSizeInChars);
    }

    /**
     * Parse, split, embed and store the given uploads.
     *
     * @return per-file segment counts and the grand total
     */
    public IngestionReport ingest(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files provided for ingestion");
        }

        DocumentParser parser = new ApacheTikaDocumentParser();
        List<IngestionReport.FileReport> perFile = new ArrayList<>();
        int total = 0;

        for (MultipartFile file : files) {
            String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";

            if (file.isEmpty()) {
                log.warn("Skipping empty upload: {}", fileName);
                perFile.add(new IngestionReport.FileReport(fileName, 0));
                continue;
            }

            log.info("Ingesting file: {} ({} bytes)", fileName, file.getSize());

            try (InputStream in = file.getInputStream()) {
                Document document = parser.parse(in);
                document.metadata().put("file_name", fileName);
                if (file.getContentType() != null) {
                    document.metadata().put("content_type", file.getContentType());
                }

                List<TextSegment> segments = splitter.split(document);
                if (segments.isEmpty()) {
                    log.warn("No text segments produced for {}", fileName);
                    perFile.add(new IngestionReport.FileReport(fileName, 0));
                    continue;
                }

                List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
                embeddingStore.addAll(embeddings, segments);

                perFile.add(new IngestionReport.FileReport(fileName, segments.size()));
                total += segments.size();
                log.info("Ingested {} ({} segments)", fileName, segments.size());
            } catch (IOException e) {
                throw new RuntimeException("Failed to read upload " + fileName, e);
            } catch (Exception e) {
                // Tika can throw BlankDocumentException etc. - skip and continue.
                log.warn("Failed to ingest {}: {}", fileName, e.getMessage());
                perFile.add(new IngestionReport.FileReport(fileName, 0));
            }
        }

        return new IngestionReport(perFile, total);
    }
}

