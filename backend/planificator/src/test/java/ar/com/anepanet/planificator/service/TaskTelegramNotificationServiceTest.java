package ar.com.anepanet.planificator.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskTelegramNotificationServiceTest {

    @Test
    void formatsStatusChangeInSpanishWithRelevantDetails() {
        TaskChangedEvent event = new TaskChangedEvent(
                List.of("123456789"),
                "Armar vidriera",
                "IN_PROGRESS",
                "BLOCKED",
                "Gisela",
                "Gisela",
                false,
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
        TaskChangedEvent event = new TaskChangedEvent(
                List.of("123456789"),
                "Controlar stock",
                "PENDING",
                "IN_PROGRESS",
                "Ana",
                "Ana",
                false,
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

    @Test
    void formatsAssignmentWithoutStatusLine() {
        TaskChangedEvent event = new TaskChangedEvent(
                List.of("123456789"),
                "Limpiar estantes",
                "PENDING",
                "PENDING",
                "Brenda",
                null,
                true,
                "Lara 1",
                null,
                "Supervisor"
        );

        assertEquals("""
                Tarea asignada
                Limpiar estantes
                Responsable: Brenda
                Local: Lara 1
                Asignada por: Supervisor""", TaskTelegramNotificationService.format(event));
    }

    @Test
    void formatsReassignmentShowingPreviousAssignee() {
        TaskChangedEvent event = new TaskChangedEvent(
                List.of("123456789"),
                "Arqueo de caja",
                "PENDING",
                "PENDING",
                "Carolina",
                "Brenda",
                true,
                null,
                null,
                "Supervisor"
        );

        assertEquals("""
                Tarea asignada
                Arqueo de caja
                Responsable: Carolina
                Antes: Brenda
                Asignada por: Supervisor""", TaskTelegramNotificationService.format(event));
    }

    @Test
    void formatsUnassignment() {
        TaskChangedEvent event = new TaskChangedEvent(
                List.of("123456789"),
                "Ordenar cajones",
                "PENDING",
                "PENDING",
                null,
                "Brenda",
                true,
                null,
                null,
                "Supervisor"
        );

        assertEquals("""
                Tarea sin asignar
                Ordenar cajones
                Se quitó a: Brenda
                Por: Supervisor""", TaskTelegramNotificationService.format(event));
    }
}
