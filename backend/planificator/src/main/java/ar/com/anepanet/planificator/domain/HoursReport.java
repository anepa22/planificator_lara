package ar.com.anepanet.planificator.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HoursReport(
        LocalDate from,
        LocalDate to,
        BigDecimal lunchHours,
        UUID personId,
        String personName,
        String locationId,
        String locationName,
        BigDecimal totalHours,
        List<HoursReportPerson> people,
        List<HoursReportPersonOption> personOptions,
        List<HoursReportLocationOption> locationOptions
) {}
