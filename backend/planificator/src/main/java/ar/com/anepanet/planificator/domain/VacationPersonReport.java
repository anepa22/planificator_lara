package ar.com.anepanet.planificator.domain;

import java.util.List;
import java.util.UUID;

public record VacationPersonReport(
        UUID userId,
        String userName,
        int days,
        List<VacationRange> ranges
) {}
