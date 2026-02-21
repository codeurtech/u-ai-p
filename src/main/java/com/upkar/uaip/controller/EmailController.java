package com.upkar.uaip.controller;

import com.upkar.uaip.model.Email;
import com.upkar.uaip.service.ChatService;
import com.upkar.uaip.service.EmailService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@Slf4j
@AllArgsConstructor
public class EmailController {

    private final EmailService emailService;

    private final ChatService chatService;

    @PostMapping(value = "/email", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> email(@RequestBody Email email) {
        log.info("Received user input for email: {}", email);
        Flux<String> result = chatService.chat(email.prompt());

        // Collect the full streaming response into a single string before parsing
        Mono<String> fullResponse = result.collectList().map(list -> String.join("", list));

        // Build a stream: emit a loading message, then emit success/failure after attempting to open Gmail compose
        return fullResponse.flatMapMany(response -> {
            log.info("Full chat response: {}", response);
            String subject;
            String body;

            try {
                if (response.isBlank()) {
                    subject = "(no subject)";
                    body = "";
                } else {
                    int subjIndex = response.indexOf("Subject: ");

                    if (subjIndex != -1) {
                        // Found 'Subject: '. Find newline AFTER the subject start so we don't pick an earlier newline
                        int start = subjIndex + "Subject: ".length();
                        int newlineAfter = response.indexOf("\n", start);

                        if (newlineAfter != -1) {
                            subject = response.substring(start, newlineAfter).trim();
                            body = response.substring(newlineAfter + 1).trim();
                        } else {
                            // No newline after subject — try common-salutation heuristics (case-insensitive)
                            String lower = response.toLowerCase();

                            int salutationIdx = -1;
                            int dearIdx = lower.indexOf("dear", start);
                            int hiIdx = lower.indexOf("hi", start);
                            int helloIdx = lower.indexOf("hello", start);
                            if (dearIdx >= start) salutationIdx = dearIdx;
                            if (hiIdx >= start && (salutationIdx == -1 || hiIdx < salutationIdx)) salutationIdx = hiIdx;
                            if (helloIdx >= start && (salutationIdx == -1 || helloIdx < salutationIdx)) salutationIdx = helloIdx;

                            if (salutationIdx != -1) {
                                subject = response.substring(start, salutationIdx).trim();
                                body = response.substring(salutationIdx).trim();
                            } else {
                                // Fallback: no clear separator found — treat remainder as subject and leave body empty
                                subject = response.substring(start).trim();
                                body = "";
                            }
                        }
                    } else {
                        int newlineIndex = response.indexOf("\n");
                        if (newlineIndex != -1) {
                            // No explicit 'Subject:' label; use first line as subject
                            subject = response.substring(0, newlineIndex).trim();
                            body = response.substring(newlineIndex + 1).trim();
                        } else {
                            // Single-line response; try to detect common salutations that may be glued to the subject
                            String lower = response.toLowerCase();

                            int salutationIdx = -1;
                            int dearIdx = lower.indexOf("dear");
                            int hiIdx = lower.indexOf("hi");
                            int helloIdx = lower.indexOf("hello");
                            if (dearIdx > 0) salutationIdx = dearIdx;
                            if (hiIdx > 0 && (salutationIdx == -1 || hiIdx < salutationIdx)) salutationIdx = hiIdx;
                            if (helloIdx > 0 && (salutationIdx == -1 || helloIdx < salutationIdx)) salutationIdx = helloIdx;

                            if (salutationIdx != -1 && salutationIdx <= 200) {
                                // Treat the prefix up to the salutation as subject and the rest as body
                                subject = response.substring(0, salutationIdx).trim();
                                body = response.substring(salutationIdx).trim();
                                if (subject.isBlank()) {
                                    // If nothing meaningful before salutation, fallback to default subject
                                    subject = "(no subject)";
                                }
                            } else {
                                // No salutation detected: treat entire single-line as body
                                subject = "(no subject)";
                                body = response.trim();
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // Defensive: if parsing fails for any unexpected reason, log and continue with defaults
                log.warn("Failed to parse chat response into subject/body. Using defaults. Response='{}'", response, e);
                subject = "(no subject)";
                body = response.trim();
            }

            // Ensure body is not empty because EmailService requires a non-empty body
            if (body.isBlank()) {
                log.warn("Parsed email body is empty; using fallback body text");
                body = "(no body provided)";
            }

            // stream: first emit 'loading', then call sendPlainText and emit result
            Mono<String> loading = Mono.just("loading");
            Mono<String> resultMono = emailService.sendPlainText(email.to(), email.cc(), email.bcc(), subject, body)
                    .map(ok -> ok ? "opened" : "failed to open")
                    .onErrorReturn("failed to open");

            return Flux.concat(loading, resultMono);
        });
    }
}