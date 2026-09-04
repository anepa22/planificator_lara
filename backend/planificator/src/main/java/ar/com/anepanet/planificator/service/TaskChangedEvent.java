package ar.com.anepanet.planificator.service;

import java.util.List;

/**
 * Movimiento de tarea que se avisa por Telegram: cambio de estado, de
 * responsable, o ambos a la vez (por ejemplo al desasignar).
 */
public record TaskChangedEvent(
        List<String> chatIds,
        String title,
        String fromStatus,
        String toStatus,
        String assigneeName,
        String previousAssigneeName,
        boolean assigneeChanged,
        String locationName,
        String blockReason,
        String changedBy
) {

    public boolean statusChanged() {
        return fromStatus != null && !fromStatus.equals(toStatus);
    }
}
