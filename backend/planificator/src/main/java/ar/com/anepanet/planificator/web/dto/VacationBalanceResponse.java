package ar.com.anepanet.planificator.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record VacationBalanceResponse(
        UUID userId,
        String displayName,
        LocalDate hireDate,
        boolean hireDateMissing,
        int periodYear,
        String periodLabel,
        int legalDays,
        int agreedDays,
        int accumulatedDays,
        int takenDays,
        int usableDays,
        int remainingDays
) {}
