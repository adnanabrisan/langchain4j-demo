package org.zeni.langchaindemo.assistant;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.spring.AiService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.zeni.langchaindemo.rag.DocumentIngestionService;
import org.zeni.langchaindemo.rag.IngestionReport;
import org.zeni.langchaindemo.utils.CommonMethods;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.springframework.http.MediaType.TEXT_EVENT_STREAM_VALUE;

/**
 * This is an example of using an {@link AiService}, a high-level LangChain4j API.
 * <p>
 * The assistant itself decides whether a user message needs an image generated
 * (via the {@code generateImage} tool) or whether to look something up in the
 * user's uploaded documents (via the {@code searchDocuments} tool, backed by
 * files uploaded through {@link #ingest(List)}). When an image is produced the
 * tool returns a relative URL pointing at {@link #assistantImage(String)} below
 * so the user can fetch the resulting PNG over the same {@code /assistant}
 * namespace.
 */
@RestController
public class AssistantController {

    private final Assistant assistant;
    private final StreamingAssistant streamingAssistant;
    private final DocumentIngestionService documentIngestionService;
    private final Path imageOutputDir;
    private final ChatModel visionChatModel;

    public AssistantController(Assistant assistant,
                               StreamingAssistant streamingAssistant,
                               DocumentIngestionService documentIngestionService,
                               @Qualifier("visionChatModel") ChatModel visionChatModel,
                               @Value("${ollama.image.output-dir}") String imageOutputDir) {
        this.assistant = assistant;
        this.streamingAssistant = streamingAssistant;
        this.documentIngestionService = documentIngestionService;
        this.imageOutputDir = Paths.get(imageOutputDir);
        this.visionChatModel = visionChatModel;
    }

    @PostMapping(path = "/assistant", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String assistant(@RequestPart("message") String message,
                            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
        try {
            if (image == null || image.isEmpty()) {
                return assistant.chat(UserMessage.from(message));
            }
            UserMessage userMessage = CommonMethods.extractUserMessage(message, image);
            return visionChatModel.chat(userMessage).aiMessage().text();
        } catch (InputGuardrailException ex) {
            return "Sorry, I cannot process this request: " + ex.getMessage();
        } catch (Exception e) {
            // Any other unexpected error
            return "I ran into some problems. Please try again.";
        }
    }


    @PostMapping(path = "/streamingAssistant", produces = TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamingAssistant(@RequestParam(value = "message") String message) throws IOException {
        try {
            return streamingAssistant.chat(UserMessage.from(message));
        } catch (InputGuardrailException ex) {
            return Flux.just("Sorry, I cannot process this request: " + ex.getMessage());
        } catch (Exception e) {
            // Any other unexpected error
            return Flux.just("I ran into some problems. Please try again.");
        }
    }

    /**
     * Ingest one or more files (PDF, DOCX, Markdown, HTML, plain text, source
     * code, ...) into the RAG knowledge base. The assistant can subsequently
     * answer questions about them via its {@code searchDocuments} tool.
     *
     * <p>Example:
     * <pre>
     * curl -F "files=@/path/to/doc.pdf" -F "files=@/path/to/notes.md" \
     *      http://localhost:8082/assistant/ingest
     * </pre>
     */
    @PostMapping(path = "/assistant/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public IngestionReport ingest(@RequestParam("files") List<MultipartFile> files) {
        try {
            return documentIngestionService.ingest(files);
        } catch (Exception ex) {
            return new IngestionReport(Collections.emptyList(), 0);
        }
    }

    /**
     * Serves a PNG previously created by the assistant's {@code generateImage} tool.
     * The tool returns a URL of the form {@code /assistant/image/<filename>} so the
     * client can fetch the image without leaving the assistant's namespace.
     */
    @GetMapping(value = "/assistant/image/{name}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<Resource> assistantImage(@PathVariable("name") String name) {
        Path file = imageOutputDir.resolve(name);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(new FileSystemResource(file));
    }


}
