package ar.com.anepanet.planificator.web;

import ar.com.anepanet.planificator.domain.HoursByUserWeek;
import ar.com.anepanet.planificator.domain.HoursReport;
import ar.com.anepanet.planificator.domain.Shift;
import ar.com.anepanet.planificator.domain.ShiftWeekView;
import ar.com.anepanet.planificator.domain.VacationReport;
import ar.com.anepanet.planificator.service.ShiftService;
import ar.com.anepanet.planificator.web.dto.CreateShiftRequest;
import ar.com.anepanet.planificator.web.dto.CreateVacationsRequest;
import ar.com.anepanet.planificator.web.dto.UpdateShiftRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/shifts")
public class ShiftController {

    private final ShiftService service;

    public ShiftController(ShiftService service) {
        this.service = service;
    }

    @GetMapping
    public List<ShiftWeekView> byWeek(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return service.byWeek(weekStart);
    }

    @GetMapping("/vacation-report")
    public VacationReport vacationReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.vacationReport(from, to);
    }

    @GetMapping(value = "/vacation-report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> vacationReportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        byte[] pdf = service.vacationReportPdf(from, to);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"vacaciones-" + from + "-" + to + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/hours-report")
    public HoursReport hoursReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID person,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal lunch) {
        return service.hoursReport(from, to, person, location, lunch);
    }

    @GetMapping(value = "/hours-report.pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> hoursReportPdf(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID person,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) BigDecimal lunch) {
        byte[] pdf = service.hoursReportPdf(from, to, person, location, lunch);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"horas-" + from + "-" + to + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/summary")
    public List<HoursByUserWeek> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return service.hoursByWeek(weekStart);
    }

    @PostMapping("/vacations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void createVacations(@Valid @RequestBody CreateVacationsRequest request) {
        service.createVacations(request);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Shift create(@Valid @RequestBody CreateShiftRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public Shift update(@PathVariable UUID id, @Valid @RequestBody UpdateShiftRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }
}
