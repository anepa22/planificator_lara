package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.VacationPersonReport;
import ar.com.anepanet.planificator.domain.VacationRange;
import ar.com.anepanet.planificator.domain.VacationReport;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

final class VacationReportPdf {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font TEXT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

    private VacationReportPdf() {}

    static byte[] render(VacationReport report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        PdfWriter.getInstance(document, out);
        document.open();
        document.add(new Paragraph("Reporte de vacaciones", TITLE));
        document.add(new Paragraph(
                "Del " + DATE.format(report.from()) + " al " + DATE.format(report.to()),
                TEXT));
        document.add(new Paragraph(
                report.totalDays() + " días · " + report.people().size() + " personas",
                TEXT));
        document.add(new Paragraph(" "));
        document.add(table(report));
        document.close();
        return out.toByteArray();
    }

    private static PdfPTable table(VacationReport report) {
        PdfPTable table = new PdfPTable(new float[] {2.2f, 1.2f, 1.2f, 0.7f});
        table.setWidthPercentage(100);
        header(table, "Persona");
        header(table, "Desde");
        header(table, "Hasta");
        header(table, "Días");
        if (report.people().isEmpty()) {
            PdfPCell empty = cell("Sin vacaciones en el período", TEXT);
            empty.setColspan(4);
            table.addCell(empty);
            return table;
        }
        for (VacationPersonReport person : report.people()) {
            for (VacationRange range : person.ranges()) {
                table.addCell(cell(person.userName(), TEXT));
                table.addCell(cell(DATE.format(range.dateFrom()), TEXT));
                table.addCell(cell(DATE.format(range.dateTo()), TEXT));
                table.addCell(cell(String.valueOf(range.days()), TEXT));
            }
        }
        return table;
    }

    private static void header(PdfPTable table, String text) {
        PdfPCell cell = cell(text, HEADER);
        cell.setBackgroundColor(new Color(47, 84, 120));
        table.addCell(cell);
    }

    private static PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }
}
