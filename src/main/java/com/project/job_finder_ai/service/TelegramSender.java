package com.project.job_finder_ai.service;

import com.project.job_finder_ai.config.TelegramProperties;
import com.project.job_finder_ai.exception.TelegramException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TelegramSender {

    private final RestClient restClient;
    private final TelegramProperties props;

    public boolean isConfigured() {
        return props.isEnabled()
            && props.getBotToken() != null && !props.getBotToken().isBlank()
            && props.getChatId() != null && !props.getChatId().isBlank();
    }

    public void send(String htmlText) {
        if (!isConfigured()) {
            throw new TelegramException(
                "Telegram is not configured: set telegram.enabled, bot-token and chat-id");
        }

        String url = props.getApiBaseUrl() + "/bot" + props.getBotToken() + "/sendMessage";
        Map<String, Object> body = Map.of(
            "chat_id", props.getChatId(),
            "text", htmlText,
            "parse_mode", "HTML",
            "link_preview_options", Map.of("is_disabled", true));

        try {
            restClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientResponseException e) {
            throw new TelegramException("Telegram rejected the message: HTTP "
                + e.getStatusCode().value() + " " + scrub(e.getResponseBodyAsString()));
        } catch (RestClientException e) {
            throw new TelegramException("Could not reach Telegram: " + scrub(e.getMessage()));
        }
    }

    // Telegram puts the bot token inside the URL, so error texts can contain it
    private String scrub(String text) {
        if (text == null) return "";
        String token = props.getBotToken();
        return (token == null || token.isBlank()) ? text : text.replace(token, "***");
    }
}