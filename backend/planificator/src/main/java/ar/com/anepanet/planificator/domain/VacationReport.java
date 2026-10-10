package ar.com.anepanet.planificator.domain;

import java.time.LocalDate;
import java.util.List;

public record VacationReport(
        LocalDate from,
        LocalDate to,
        int totalDays,
        List<VacationPersonReport> people
) {}
