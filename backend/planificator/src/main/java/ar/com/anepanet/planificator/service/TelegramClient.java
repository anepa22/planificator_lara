package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.config.TelegramProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramClient {

    private static final Pattern BOT_TOKEN_IN_TEXT =
            Pattern.compile("bot\\d+:[A-Za-z0-9_-]+");

    private final TelegramProperties properties;
    private final RestClient restClient;

    public TelegramClient(TelegramProperties properties) {
        this.properties = properties;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public boolean isConfigured() {
        return properties.isEnabled()
                && properties.getBotToken() != null
                && !properties.getBotToken().isBlank();
    }

    public void sendMessage(String chatId, String text) {
        if (!isConfigured() || chatId == null || chatId.isBlank()) {
            return;
        }
        String token = properties.getBotToken().trim();
        try {
            restClient.post()
                    .uri("https://api.telegram.org/bot{token}/sendMessage", token)
                    .body(Map.of("chat_id", chatId, "text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            throw new IllegalStateException(
                    "Telegram HTTP " + ex.getStatusCode().value(), null);
        } catch (RuntimeException ex) {
            throw new IllegalStateException(sanitize(ex.getMessage(), token), null);
        }
    }

    static String sanitize(String message, String token) {
        if (message == null || message.isBlank()) {
            return "error de Telegram";
        }
        String safe = message;
        if (token != null && !token.isBlank()) {
            safe = safe.replace(token.trim(), "***");
        }
        return BOT_TOKEN_IN_TEXT.matcher(safe).replaceAll("bot***");
    }
}
