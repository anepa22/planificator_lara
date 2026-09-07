package ar.com.anepanet.planificator.realtime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlanificatorWebSocketHandlerTest {

    @Mock WebSocketSession session;

    @Test
    void broadcastsSyncScopeToConnectedSessions() throws Exception {
        when(session.getId()).thenReturn("session-1");
        when(session.isOpen()).thenReturn(true);
        PlanificatorWebSocketHandler handler = new PlanificatorWebSocketHandler();
        handler.afterConnectionEstablished(session);

        handler.broadcast(PlanificatorChangedEvent.TASKS);

        ArgumentCaptor<TextMessage> message = ArgumentCaptor.forClass(TextMessage.class);
        verify(session).sendMessage(message.capture());
        assertEquals(
                "{\"type\":\"sync\",\"scope\":\"tasks\"}",
                message.getValue().getPayload());
    }
}
