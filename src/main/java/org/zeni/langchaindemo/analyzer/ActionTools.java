package org.zeni.langchaindemo.analyzer;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.rag.content.Content;
import org.springframework.stereotype.Component;
import org.zeni.langchaindemo.rag.DocumentRetrievalService;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class ActionTools {
    private final DocumentRetrievalService documentRetrievalService;

    public ActionTools(DocumentRetrievalService documentRetrievalService) {
        this.documentRetrievalService = documentRetrievalService;
    }

    @Tool("Create a support ticket in the system if the inventory of a product is empty")
    public String createTicket(String title, String description) {
        return "Ticket created: #" + UUID.randomUUID();
    }

    @Tool("Search inventory for a product")
    public String searchInventory(String product) {
        List<Content> results = documentRetrievalService.retrieve(product);
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

    @Tool("Trigger alert notification for tickets created")
    public String sendAlert(String message) {
        return "Alert sent: " + message;
    }
}
