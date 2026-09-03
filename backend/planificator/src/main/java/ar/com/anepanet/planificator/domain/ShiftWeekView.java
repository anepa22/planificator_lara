package ar.com.anepanet.planificator.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/** Fila de la vista v_shifts_week */
public record ShiftWeekView(
        UUID id,
        UUID userId,
        String userName,
        String userColor,
        String locationId,
        String locationName,
        String locationColor,
        String locationColorSoft,
        LocalDate workDate,
        LocalDate weekStart,
        short dayIndex,
        LocalTime startTime,
        LocalTime endTime,
        BigDecimal hours,
        String notes
) {}
