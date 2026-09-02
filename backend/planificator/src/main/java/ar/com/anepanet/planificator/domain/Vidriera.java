package ar.com.anepanet.planificator.domain;

import java.time.LocalDate;

public record Vidriera(
        String locationId,
        String locationName,
        LocalDate workDate
) {}
