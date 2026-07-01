package org.zeni.langchaindemo;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Duration;

@SpringBootApplication
public class LangchainDemoApplication {

    public static void main(String[] args) {

        SpringApplication.run(LangchainDemoApplication.class, args);
    }
}
