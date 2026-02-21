package com.upkar.uaip.model;

import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public record Email(String to, String cc, String bcc, String subject, String body, String prompt) {

    private static final String EMAIL_REGEX =
            "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile(EMAIL_REGEX, Pattern.CASE_INSENSITIVE); // Case-insensitive matching

    public static boolean isValidEmail(String to) {
        if (to == null) {
            return true;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(to);
        return !matcher.matches();
    }

    public Email {
        if (to == null || to.isBlank() || isValidEmail(to)) {
            log.info("Invalid email address: {}", to);
            throw new IllegalArgumentException("\"to\" should contain valid email address");
        }
        if (cc != null && !cc.isBlank() && isValidEmail(cc)) {
            throw new IllegalArgumentException("\"cc\" should contain valid email address");
        }
        if (bcc != null && !bcc.isBlank() && isValidEmail(bcc)) {
            throw new IllegalArgumentException("\"bcc\" should contain valid email address");
        }
    }
}
