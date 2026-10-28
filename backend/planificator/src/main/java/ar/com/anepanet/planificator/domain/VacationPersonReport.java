package ar.com.anepanet.planificator.domain;

import java.util.List;
import java.util.UUID;

public record VacationPersonReport(
        UUID userId,
        String userName,
        int days,
        List<VacationRange> ranges,
        Integer thisPeriod,
        Integer carried,
        Integer extra,
        Integer enjoyed,
        Integer scheduled,
        Integer remaining
) {
    public VacationPersonReport(UUID userId, String userName, int days, List<VacationRange> ranges) {
        this(userId, userName, days, ranges, null, null, null, null, null, null);
    }
}
