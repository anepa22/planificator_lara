package ar.com.anepanet.planificator.web.dto;

import java.time.LocalDate;
import java.util.UUID;

public record VacationQuotaResponse(
        UUID userId,
        String userName,
        LocalDate hireDate,
        int periodYear,
        String periodLabel,
        Integer thisPeriod,
        Integer carried,
        Integer extra,
        Integer enjoyed,
        Integer remaining
) {}
