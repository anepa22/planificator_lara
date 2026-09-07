package ar.com.anepanet.planificator.config;

import ar.com.anepanet.planificator.realtime.PlanificatorWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import java.util.Arrays;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final PlanificatorWebSocketHandler handler;
    private final String[] allowedOriginPatterns;

    public WebSocketConfig(
            PlanificatorWebSocketHandler handler,
            @Value("${planificator.cors.allowed-origins:*}") String allowedOrigins) {
        this.handler = handler;
        this.allowedOriginPatterns = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/api/ws/sync")
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }
}
