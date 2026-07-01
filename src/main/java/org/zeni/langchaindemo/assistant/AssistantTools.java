package org.zeni.langchaindemo.assistant;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Component;
import org.zeni.langchaindemo.image_generation.ImageGenerationService;
import org.zeni.langchaindemo.rag.DocumentRetrievalService;

import java.nio.file.Path;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class AssistantTools {

    private final ContentRetriever contentRetriever;
    private final ImageGenerationService imageGenerationService;
    private final DocumentRetrievalService documentRetrievalService;

    public AssistantTools(ContentRetriever contentRetriever,
                          ImageGenerationService imageGenerationService,
                          DocumentRetrievalService documentRetrievalService) {
        this.contentRetriever = contentRetriever;
        this.imageGenerationService = imageGenerationService;
        this.documentRetrievalService = documentRetrievalService;
    }

    /**
     * Returns the current local system time.
     * Use this only for time/date questions.
     */
    @Tool("Get the current local system time. Use only for time-related questions.")
    @Observed
    public String currentTime(String userMessage) {
        if (userMessage == null || !userMessage.toLowerCase().matches(".*\\b(time|clock)\\b.*")) {
            return "This tool is only for time questions.";
        }
        return LocalTime.now().toString();
    }

    /**
     * Searches the web for external information.
     * Never use for time/date requests.
     */
    @Tool("Search the web for external information")
    @Observed
    public String webSearch(String query) {
        String lower = query == null ? "" : query.toLowerCase();
        if (lower.matches(".*\\b(time|clock)\\b.*")) {
            return "Use the currentTime tool for time questions.";
        }
        return contentRetriever.retrieve(new Query(query)).stream()
                .map(content -> content.textSegment().text())
                .collect(Collectors.joining("\n\n"));
    }

    /**
     * Generates an image from a natural-language prompt via Ollama and
     * returns a URL the user can open to view it.
     */
    @Tool("Generate an image from a text prompt. Use ONLY when the user explicitly asks to create, draw, render, or generate a picture/image." +
            "Return the URL of the generated image in the user-facing response so the user can view it.")
    @Observed
    public String generateImage(String prompt) {
        Path file = imageGenerationService.generate(prompt);
        return "Image generated. View it at: /assistant/image/" + file.getFileName().toString();
    }

    /**
     * Searches the user's previously uploaded documents (PDF, DOCX, MD, HTML,
     * source code, ...) ingested via {@code POST /assistant/ingest}.
     */
    @Tool("Search the user's uploaded documents (PDF/DOCX/Markdown/HTML/code). " +
            "Use when the user asks about their own files, uploads, knowledge base, or 'the docs I sent'.")
    @Observed
    public String searchDocuments(String query) {
        List<Content> results = documentRetrievalService.retrieve(query);
        if (results.isEmpty()) {
            return "No relevant content found in the uploaded documents.";
        }
        return results.stream()
                .map(c -> {
                    var seg = c.textSegment();
                    String source = seg.metadata().getString("file_name");
                    String prefix = source != null ? "[" + source + "]\n" : "";
                    return prefix + seg.text();
                })
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
