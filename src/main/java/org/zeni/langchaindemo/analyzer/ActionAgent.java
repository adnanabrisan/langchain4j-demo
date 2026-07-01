package org.zeni.langchaindemo.analyzer;

import dev.langchain4j.service.SystemMessage;

public interface ActionAgent {
    @SystemMessage("""
        You are an automation agent.

        Based on vision input, take the following actions:
        - search the objects from the picture in the inventory uploaded by the user;
        - if the inventory is 0 for a certain object, call the tools to create tickets and send alert notifications.
        
        Do the same operations for all objects present in the picture. If an action has to be repeated, do so.
    """)
    String decideAndAct(VisionAnalysis analysis);
}
