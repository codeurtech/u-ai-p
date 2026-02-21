package com.upkar.uaip.service;

import com.upkar.uaip.utils.OpenGmail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@Slf4j
public class EmailService {

    private final OpenGmail openGmail;

    public EmailService(OpenGmail openGmail) {
        this.openGmail = openGmail;
    }

    public Mono<Boolean> sendPlainText(String to, String cc, String bcc, String subject, String body) {
        log.info("Opening Gmail compose window for recipients and prefilled content");
        // Run the blocking call on boundedElastic to avoid blocking event-loop threads
        return Mono.fromSupplier(() -> openGmail.openGmailCompose(to, cc, bcc, subject, body))
                .subscribeOn(Schedulers.boundedElastic());
    }
}
