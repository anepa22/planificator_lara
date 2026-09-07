package ar.com.anepanet.planificator.realtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PlanificatorWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log =
            LoggerFactory.getLogger(PlanificatorWebSocketHandler.class);
    private static final int SEND_TIME_LIMIT_MS = 10_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 64 * 1024;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(
                session.getId(),
                new ConcurrentWebSocketSessionDecorator(
                        session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT_BYTES));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        sessions.remove(session.getId());
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException ex) {
            log.debug("Could not close failed WebSocket session {}", session.getId(), ex);
        }
    }

    public void broadcast(String scope) {
        sendToAll(new TextMessage("{\"type\":\"sync\",\"scope\":\"" + scope + "\"}"));
    }

    @Scheduled(fixedDelay = 25_000)
    public void heartbeat() {
        sendToAll(new PingMessage());
    }

    private void sendToAll(WebSocketMessage<?> message) {
        sessions.forEach((id, session) -> {
            if (!session.isOpen()) {
                sessions.remove(id);
                return;
            }
            try {
                session.sendMessage(message);
            } catch (IOException | IllegalStateException ex) {
                sessions.remove(id);
                try {
                    session.close(CloseStatus.SERVER_ERROR);
                } catch (IOException closeEx) {
                    log.debug("Could not close WebSocket session {}", id, closeEx);
                }
            }
        });
    }
}
