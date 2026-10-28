package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.HoursReport;
import ar.com.anepanet.planificator.domain.HoursReportPerson;
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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

final class HoursReportPdf {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font TEXT = FontFactory.getFont(FontFactory.HELVETICA, 10);
    private static final Font TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
    private static final Font HEADER = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Color HEADER_BACKGROUND = new Color(47, 84, 120);
    private static final Color TOTAL_BACKGROUND = new Color(232, 238, 244);
    private static final float[] COLUMN_WIDTHS = {3.2f, 1f};

    private HoursReportPdf() {}

    static byte[] render(HoursReport report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Document document = new Document(PageSize.A4, 40, 40, 40, 40);
        try {
            PdfWriter.getInstance(document, out);
            document.open();
            document.add(new Paragraph("Reporte de horas", TITLE));
            document.add(new Paragraph(
                    "Del " + DATE.format(report.from()) + " al " + DATE.format(report.to()),
                    TEXT));
            document.add(new Paragraph("Persona: " + label(report.personName(), "Todas"), TEXT));
            document.add(new Paragraph("Local: " + label(report.locationName(), "Todos"), TEXT));
            document.add(new Paragraph(
                    "Almuerzo: " + formatHours(report.lunchHours()) + " h por día trabajado",
                    TEXT));
            document.add(table(report));
        } finally {
            if (document.isOpen()) {
                document.close();
            }
        }
        return out.toByteArray();
    }

    static String formatHours(BigDecimal value) {
        String plain = value.setScale(2, RoundingMode.HALF_UP).toPlainString();
        if (plain.contains(".")) {
            plain = plain.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return plain.replace('.', ',');
    }

    private static String label(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static PdfPTable table(HoursReport report) {
        PdfPTable table = new PdfPTable(COLUMN_WIDTHS);
        table.setWidthPercentage(100);
        table.setSpacingBefore(12);
        header(table, "Persona");
        header(table, "Horas");
        if (report.people().isEmpty()) {
            PdfPCell empty = cell("Sin horas en el período", TEXT);
            empty.setColspan(COLUMN_WIDTHS.length);
            table.addCell(empty);
            return table;
        }
        for (HoursReportPerson person : report.people()) {
            table.addCell(cell(person.userName(), TEXT));
            table.addCell(cell(formatHours(person.hours()), TEXT));
        }
        PdfPCell label = cell("Total", TOTAL);
        label.setBackgroundColor(TOTAL_BACKGROUND);
        table.addCell(label);
        PdfPCell hours = cell(formatHours(report.totalHours()), TOTAL);
        hours.setBackgroundColor(TOTAL_BACKGROUND);
        table.addCell(hours);
        return table;
    }

    private static void header(PdfPTable table, String text) {
        PdfPCell cell = cell(text, HEADER);
        cell.setBackgroundColor(HEADER_BACKGROUND);
        table.addCell(cell);
    }

    private static PdfPCell cell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }
}
