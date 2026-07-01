package org.zeni.langchaindemo.analyzer;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.guardrail.InputGuardrailException;
import dev.langchain4j.guardrail.InputGuardrailResult;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.zeni.langchaindemo.guardrail.PromptInjectionGuardrail;
import org.zeni.langchaindemo.utils.CommonMethods;

import java.io.IOException;

@RestController
public class AnalysisController {

    private final ChatModel visionChatModel;
    private AnalysisAgent analysisAgent;
    private ActionAgent actionAgent;
    private PromptInjectionGuardrail promptInjectionGuardrail;

    public AnalysisController(AnalysisAgent analysisAgent, ActionAgent actionAgent, @Qualifier("visionChatModel") ChatModel visionChatModel,
                              @Qualifier("chatModel") ChatModel chatModel) {
        this.analysisAgent = analysisAgent;
        this.actionAgent = actionAgent;
        this.visionChatModel = visionChatModel;
        this.promptInjectionGuardrail = new PromptInjectionGuardrail(chatModel);
    }

    @PostMapping(path = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String analyze(@RequestPart("message") String message,
                          @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {
        try {
            UserMessage userMessage = CommonMethods.extractUserMessage(message, image);
            InputGuardrailResult validationResult = promptInjectionGuardrail.validate(userMessage);
            if (validationResult.equals(InputGuardrailResult.success())) {
                String analyzedResultString = visionChatModel.chat(userMessage).aiMessage().text();
                VisionAnalysis visionAnalysis = analysisAgent.analyze(analyzedResultString);

                String actions = actionAgent.decideAndAct(visionAnalysis);
                return """
                        Vision:
                        %s
                        
                        Actions:
                        %s
                        """.formatted(visionAnalysis, actions);
            } else throw new InputGuardrailException(validationResult.asString());
        } catch (InputGuardrailException ex) {

            return "Sorry, I cannot process this request: " + ex.getMessage();
        } catch (Exception e) {
            // Any other unexpected error
            return "I ran into some problems. Please try again.";

        }
    }
}

