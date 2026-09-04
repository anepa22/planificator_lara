package ar.com.anepanet.planificator.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Service
public class TaskTelegramNotificationService {

    private static final Logger log =
            LoggerFactory.getLogger(TaskTelegramNotificationService.class);
    private static final Map<String, String> STATUS_LABELS = Map.of(
            "PENDING", "Pendiente",
            "IN_PROGRESS", "En proceso",
            "BLOCKED", "Bloqueada",
            "DONE", "Terminada",
            "VERIFIED", "Verificada"
    );

    private final TelegramClient telegram;

    public TaskTelegramNotificationService(TelegramClient telegram) {
        this.telegram = telegram;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        if (!telegram.isConfigured()) {
            return;
        }
        String message = format(event);
        for (String chatId : event.chatIds()) {
            try {
                telegram.sendMessage(chatId, message);
            } catch (Exception ex) {
                log.warn("No se pudo notificar por Telegram el cambio de estado de '{}': {}",
                        event.title(), ex.getMessage());
            }
        }
    }

    static String format(TaskStatusChangedEvent event) {
        StringBuilder message = new StringBuilder()
                .append("Tarea actualizada\n")
                .append(event.title()).append("\n")
                .append(label(event.fromStatus()))
                .append(" → ")
                .append(label(event.toStatus()));
        append(message, "Responsable", event.assigneeName());
        append(message, "Local", event.locationName());
        if ("BLOCKED".equals(event.toStatus())) {
            append(message, "Motivo", event.blockReason());
        }
        append(message, "Movida por", event.changedBy());
        return message.toString();
    }

    private static String label(String status) {
        return STATUS_LABELS.getOrDefault(status, status);
    }

    private static void append(StringBuilder message, String label, String value) {
        if (value != null && !value.isBlank()) {
            message.append("\n").append(label).append(": ").append(value.trim());
        }
    }
}
