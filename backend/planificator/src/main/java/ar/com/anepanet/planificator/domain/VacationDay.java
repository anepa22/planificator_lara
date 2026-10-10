package ar.com.anepanet.planificator.domain;

import java.time.LocalDate;
import java.util.UUID;

public record VacationDay(
        UUID userId,
        String userName,
        LocalDate workDate
) {}
