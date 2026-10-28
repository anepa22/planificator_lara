package ar.com.anepanet.planificator.domain;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record HoursShiftRow(
        UUID userId,
        String userName,
        String locationId,
        LocalDate workDate,
        LocalTime startTime,
        LocalTime endTime
) {}
