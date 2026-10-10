package ar.com.anepanet.planificator.domain;

import java.time.LocalDate;

public record VacationRange(
        LocalDate dateFrom,
        LocalDate dateTo,
        int days
) {}
