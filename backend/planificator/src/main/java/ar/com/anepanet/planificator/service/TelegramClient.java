package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.config.TelegramProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

@Service
@EnableConfigurationProperties(TelegramProperties.class)
public class TelegramClient {

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
        restClient.post()
                .uri("https://api.telegram.org/bot{token}/sendMessage",
                        properties.getBotToken().trim())
                .body(Map.of("chat_id", chatId, "text", text))
                .retrieve()
                .toBodilessEntity();
    }
}
