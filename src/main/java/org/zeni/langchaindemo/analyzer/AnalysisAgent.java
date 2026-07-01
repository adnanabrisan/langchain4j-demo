package org.zeni.langchaindemo.analyzer;

import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.service.SystemMessage;

public interface AnalysisAgent {
    @SystemMessage("""
        You are a vision analysis system.

        Extract:
        - sceneDescription
        - objects

        Return ONLY valid JSON.
    """)
    VisionAnalysis analyze(String message);
}
