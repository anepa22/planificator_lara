package ar.com.anepanet.planificator.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Fila de la vista v_hours_by_user_week */
public record HoursByUserWeek(
        LocalDate weekStart,
        UUID userId,
        String userName,
        BigDecimal totalHours,
        int shiftCount
) {}
