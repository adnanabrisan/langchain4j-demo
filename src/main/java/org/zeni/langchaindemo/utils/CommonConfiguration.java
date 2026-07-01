package org.zeni.langchaindemo.utils;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaImageModel;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.WebSearchContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.zeni.langchaindemo.assistant.Assistant;
import org.zeni.langchaindemo.assistant.AssistantTools;
import org.zeni.langchaindemo.assistant.MyChatModelListener;
import org.zeni.langchaindemo.assistant.StreamingAssistant;
import org.zeni.langchaindemo.guardrail.PromptInjectionGuardrail;
import org.zeni.langchaindemo.lowlevel.ChatModelController;

import java.time.Duration;
import java.util.Collections;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;

@Configuration
public class CommonConfiguration {

    /**
     * This chat memory will be used by {@link Assistant} and {@link StreamingAssistant}
     */
    @Bean
    @Scope(SCOPE_PROTOTYPE)
    ChatMemory chatMemory() {
        return MessageWindowChatMemory.withMaxMessages(10);
    }

    /**
     * This listener will be injected into every {@link ChatModel} and {@link StreamingChatModel}
     * bean   found in the application context.
     * It will listen for {@link ChatModel} in the {@link ChatModelController} as well as
     * {@link Assistant} and {@link StreamingAssistant}.
     */
    @Bean
    ChatModelListener chatModelListener() {
        return new MyChatModelListener();
    }

    @Bean
    ContentRetriever webSearchRetriever() {
        WebSearchEngine webSearchEngine = TavilyWebSearchEngine.builder()
                .apiKey("REPLACE_WITH_YOUR_KEY") // get a free key: https://app.tavily.com/sign-in
                .build();
        return WebSearchContentRetriever.builder()
                .webSearchEngine(webSearchEngine)
                .maxResults(5)
                .build();
    }

    @Bean
    ChatModel chatModel(
            @Value("${langchain4j.ollama.chat-model.base-url}") String baseUrl,
            @Value("${langchain4j.ollama.chat-model.model-name}") String modelName,
            @Value("${langchain4j.ollama.chat-model.temperature}") Double temperature
    ) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(250))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean
    @Qualifier("streamingChatModel")
    StreamingChatModel streamingChatModel(
            @Value("${langchain4j.ollama.chat-model.base-url}") String baseUrl,
            @Value("${langchain4j.ollama.chat-model.model-name}") String modelName,
            @Value("${langchain4j.ollama.chat-model.temperature}") Double temperature
    ) {
        return OllamaStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(250))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * Image generation model served by Ollama (e.g. {@code x/z-image-turbo}).
     * Pull the model once with:  ollama pull x/z-image-turbo
     */
    @Bean
    ImageModel imageModel(
            @Value("${ollama.image.base-url}") String baseUrl,
            @Value("${ollama.image.model-name}") String modelName,
            @Value("${ollama.image.width}") int width,
            @Value("${ollama.image.height}") int height,
            @Value("${ollama.image.steps}") int steps) {
        return OllamaImageModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .width(width)
                .height(height)
                .steps(steps)
                .timeout(Duration.ofMinutes(5))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean("visionChatModel")
    ChatModel visionChatModel(
            @Value("${langchain4j.ollama.chat-model.base-url}") String baseUrl,
            @Value("${ollama.image-processor.model-name}") String modelName
    ) {
        return OllamaChatModel.builder()
                .baseUrl(baseUrl)
                .modelName(modelName)
                .temperature(0.8)
                .numPredict(1024)        // allow longer descriptions
                .timeout(Duration.ofSeconds(250))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    @Bean("assistant")
    Assistant assistant(@Qualifier("chatModel") ChatModel chatModel,
                        ChatMemory chatMemory,
                        AssistantTools assistantTools) {
        return AiServices.builder(Assistant.class)
                .chatModel(chatModel)
                .chatMemory(chatMemory)
                .inputGuardrails(Collections.singletonList(new PromptInjectionGuardrail(chatModel)))
                .tools(assistantTools)
                .build();
    }

    @Bean("streamingAssistant")
    StreamingAssistant streamingAssistant(@Qualifier("streamingChatModel") StreamingChatModel streamingChatModel,
                                          @Qualifier("chatModel") ChatModel chatModel,
                                          ChatMemory chatMemory,
                                          AssistantTools assistantTools) {
        return AiServices.builder(StreamingAssistant.class)
                .streamingChatModel(streamingChatModel)
                .chatMemory(chatMemory)
                .inputGuardrails(Collections.singletonList(new PromptInjectionGuardrail(chatModel)))
                .tools(assistantTools)
                .build();
    }
}
