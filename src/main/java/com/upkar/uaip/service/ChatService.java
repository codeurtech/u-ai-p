package com.upkar.uaip.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;

@RestController
@Slf4j
public class ChatService {

    private final ChatClient chatClient;

    private final ToolService toolService;

    public ChatService(ChatClient.Builder chatClientBuilder, ToolService toolService) {
        this.chatClient = chatClientBuilder.build();
        this.toolService = toolService;
    }

    public Flux<String> chat(String userInput) {
        SystemMessage systemMessage = new SystemMessage("""
                 You are an assistant for a user. You can look for the 
                 information that the user asks and respond to his question."""
        );
        UserMessage userMessage = new UserMessage(userInput);
        final Prompt prompt = new Prompt(List.of(systemMessage, userMessage));
        return chatClient
                .prompt(prompt)
                .tools(toolService)
                .stream()
                .content()
                .bufferTimeout(12, Duration.ofMillis(80))
                .map(list -> String.join("", list));
    }
}