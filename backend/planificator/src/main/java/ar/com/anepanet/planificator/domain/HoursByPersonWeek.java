package ar.com.anepanet.planificator.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Fila de la vista v_hours_by_person_week */
public record HoursByPersonWeek(
        LocalDate weekStart,
        UUID personId,
        String personName,
        BigDecimal totalHours,
        int shiftCount
) {}
