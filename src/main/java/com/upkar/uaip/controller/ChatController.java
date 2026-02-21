package com.upkar.uaip.controller;

import com.upkar.uaip.service.ChatService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;


@RestController
@Slf4j
@AllArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping(value = "/chat", consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = MediaType.TEXT_PLAIN_VALUE)
    public Flux<String> chat(@RequestBody String userInput) {
        log.info("Received user input: {}", userInput);
        return chatService.chat(userInput);
    }
}