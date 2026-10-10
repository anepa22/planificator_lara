package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.VacationDay;
import ar.com.anepanet.planificator.domain.VacationPersonReport;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ShiftServiceVacationReportTest {

    @Test
    void groupsContiguousDaysAndSplitsGaps() {
        UUID ana = UUID.randomUUID();
        UUID beto = UUID.randomUUID();
        List<VacationPersonReport> people = ShiftService.groupVacationDays(List.of(
                new VacationDay(ana, "Ana", LocalDate.of(2026, 1, 2)),
                new VacationDay(ana, "Ana", LocalDate.of(2026, 1, 3)),
                new VacationDay(ana, "Ana", LocalDate.of(2026, 1, 5)),
                new VacationDay(beto, "Beto", LocalDate.of(2026, 2, 1))
        ));

        assertEquals(2, people.size());
        assertEquals("Ana", people.get(0).userName());
        assertEquals(3, people.get(0).days());
        assertEquals(2, people.get(0).ranges().size());
        assertEquals(2, people.get(0).ranges().get(0).days());
        assertEquals(LocalDate.of(2026, 1, 5), people.get(0).ranges().get(1).dateFrom());
        assertEquals(1, people.get(1).days());
    }
}
