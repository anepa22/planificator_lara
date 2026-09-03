package ar.com.anepanet.planificator.domain;

import java.util.UUID;

/** Vista pública del personal del planificador: un usuario visto desde la grilla. */
public record StaffMember(
        UUID id,
        String name,
        String color
) {}
