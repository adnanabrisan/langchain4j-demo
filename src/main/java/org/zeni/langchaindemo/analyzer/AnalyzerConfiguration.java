package org.zeni.langchaindemo.analyzer;

import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AnalyzerConfiguration {

    @Bean("analysisAgent")
    AnalysisAgent analysisAgent(@Qualifier("visionChatModel") ChatModel visionChatModel,
                                @Qualifier("chatModel") ChatModel chatModel) {
        return AiServices.builder(AnalysisAgent.class)
                .chatModel(visionChatModel)
                .build();
    }

    @Bean("actionAgent")
    ActionAgent actionAgent(@Qualifier("chatModel") ChatModel chatModel,
                            ActionTools actionTools) {
        return AiServices.builder(ActionAgent.class)
                .chatModel(chatModel)
                .tools(actionTools)
                .build();
    }
}
