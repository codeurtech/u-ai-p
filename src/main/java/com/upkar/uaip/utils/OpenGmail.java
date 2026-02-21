package com.upkar.uaip.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.Desktop;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OpenGmail {

    private static final Logger log = LoggerFactory.getLogger(OpenGmail.class);

    public void openGmailInDesktopBrowser() {
        String gmailUrl = "https://mail.google.com";
        boolean opened = openUrl(gmailUrl);
        if (!opened) {
            log.error("Failed to open browser automatically. Please open this URL manually: {}", gmailUrl);
        }
    }

    /**
     * Open Gmail compose window with optional prefilled recipients, subject and body.
     * Empty or null values are ignored.
     * Returns true if the URL opening was triggered successfully (Desktop or OS fallback), false otherwise.
     */
    public boolean openGmailCompose(String to, String cc, String bcc, String subject, String body) {
        StringBuilder url = new StringBuilder("https://mail.google.com/mail/?view=cm");

        if (to != null && !to.isBlank()) {
            url.append("&to=").append(URLEncoder.encode(to, StandardCharsets.UTF_8));
        }
        if (cc != null && !cc.isBlank()) {
            url.append("&cc=").append(URLEncoder.encode(cc, StandardCharsets.UTF_8));
        }
        if (bcc != null && !bcc.isBlank()) {
            url.append("&bcc=").append(URLEncoder.encode(bcc, StandardCharsets.UTF_8));
        }
        if (subject != null && !subject.isBlank()) {
            // Gmail uses 'su' for subject in the compose URL
            url.append("&su=").append(URLEncoder.encode(subject, StandardCharsets.UTF_8));
        }
        if (body != null && !body.isBlank()) {
            url.append("&body=").append(URLEncoder.encode(body, StandardCharsets.UTF_8));
        }

        String finalUrl = url.toString();

        boolean opened = openUrl(finalUrl);
        if (!opened) {
            log.error("Failed to open Gmail compose automatically. Please open this URL manually: {}", finalUrl);
        }

        return opened;
    }

    private boolean openUrl(String urlToOpen) {
        boolean opened = false;

        try {
            // Try Desktop.browse if available
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.BROWSE)) {
                    desktop.browse(new URI(urlToOpen));
                    log.info("Opened URL in the default browser via Desktop.browse(): {}", urlToOpen);
                    opened = true;
                } else {
                    log.info("Desktop is supported but BROWSE action is not; will try OS-specific fallback.");
                }
            } else {
                log.info("Desktop is not supported on this platform; attempting OS-specific fallback.");
            }
        } catch (Exception e) {
            log.warn("Failed to open browser via Desktop.browse(): {}. Will try OS-specific fallback.", e.toString());
        }

        if (opened) {
            return true;
        }

        // OS-specific fallback commands
        String osName = System.getProperty("os.name").toLowerCase();
        try {
            if (osName.contains("mac")) {
                // macOS
                Runtime.getRuntime().exec(new String[]{"open", urlToOpen});
                log.info("Opened URL via 'open': {}", urlToOpen);
                opened = true;
            } else if (osName.contains("win")) {
                // Windows: use cmd /c start "" <url> to avoid treating first arg as title
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", urlToOpen});
                log.info("Opened URL via 'start': {}", urlToOpen);
                opened = true;
            } else if (osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                // Unix/Linux
                Runtime.getRuntime().exec(new String[]{"xdg-open", urlToOpen});
                log.info("Opened URL via 'xdg-open': {}", urlToOpen);
                opened = true;
            } else {
                log.error("Unsupported OS ('{}') and Desktop is not available. Please open this URL manually: {}", osName, urlToOpen);
            }
        } catch (Exception e) {
            log.error("Error while trying OS-specific command to open browser: {}. Please open this URL manually: {}", e.getMessage(), urlToOpen);
        }

        return opened;
    }
}
