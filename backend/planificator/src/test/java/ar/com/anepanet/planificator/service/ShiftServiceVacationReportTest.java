package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.VacationDay;
import ar.com.anepanet.planificator.domain.VacationPersonReport;
import ar.com.anepanet.planificator.domain.VacationRange;
import ar.com.anepanet.planificator.domain.VacationReport;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShiftServiceVacationReportTest {

    @Test
    void groupsContiguousDaysAndSplitsGaps() {
        UUID ana = UUID.randomUUID();
        UUID beto = UUID.randomUUID();
        List<VacationPersonReport> people = ShiftService.groupVacationDays(List.of(
                new VacationDay(ana, "Ana", LocalDate.of(2026, 1, 2)),
                new VacationDay(beto, "Beto", LocalDate.of(2026, 2, 1)),
                new VacationDay(ana, "Ana", LocalDate.of(2026, 1, 3)),
                new VacationDay(ana, "Ana", LocalDate.of(2026, 1, 5))
        ));

        assertEquals(2, people.size());
        assertEquals("Ana", people.get(0).userName());
        assertEquals(3, people.get(0).days());
        assertEquals(2, people.get(0).ranges().size());
        assertEquals(2, people.get(0).ranges().get(0).days());
        assertEquals(LocalDate.of(2026, 1, 5), people.get(0).ranges().get(1).dateFrom());
        assertEquals(1, people.get(1).days());
    }

    @Test
    void rendersAPdfDocument() {
        byte[] pdf = VacationReportPdf.render(new VacationReport(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                0,
                List.of()));
        assertTrue(pdf.length > 4);
        assertEquals('%', (char) pdf[0]);
        assertEquals('P', (char) pdf[1]);
        assertEquals('D', (char) pdf[2]);
        assertEquals('F', (char) pdf[3]);
    }

    @Test
    void pdfTotalsDaysPerPerson() throws Exception {
        UUID ana = UUID.randomUUID();
        byte[] pdf = VacationReportPdf.render(new VacationReport(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 31),
                3,
                List.of(new VacationPersonReport(ana, "Ana", 3, List.of(
                        new VacationRange(LocalDate.of(2026, 1, 2), LocalDate.of(2026, 1, 3), 2),
                        new VacationRange(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 5), 1)
                )))));
        PdfReader reader = new PdfReader(pdf);
        try {
            String text = new PdfTextExtractor(reader).getTextFromPage(1).replaceAll("\\s+", " ");
            assertTrue(text.contains("Total Ana"), text);
        } finally {
            reader.close();
        }
    }
}
