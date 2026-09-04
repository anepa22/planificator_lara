package ar.com.anepanet.planificator.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskTelegramNotificationServiceTest {

    @Test
    void formatsStatusChangeInSpanishWithRelevantDetails() {
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(
                "123456789",
                "Armar vidriera",
                "IN_PROGRESS",
                "BLOCKED",
                "Gisela",
                "Lara 3",
                "Falta mercadería",
                "Supervisor"
        );

        assertEquals("""
                Tarea actualizada
                Armar vidriera
                En proceso → Bloqueada
                Responsable: Gisela
                Local: Lara 3
                Motivo: Falta mercadería
                Movida por: Supervisor""", TaskTelegramNotificationService.format(event));
    }

    @Test
    void omitsEmptyOptionalDetails() {
        TaskStatusChangedEvent event = new TaskStatusChangedEvent(
                "123456789",
                "Controlar stock",
                "PENDING",
                "IN_PROGRESS",
                "Ana",
                null,
                null,
                "Ana"
        );

        assertEquals("""
                Tarea actualizada
                Controlar stock
                Pendiente → En proceso
                Responsable: Ana
                Movida por: Ana""", TaskTelegramNotificationService.format(event));
    }
}
