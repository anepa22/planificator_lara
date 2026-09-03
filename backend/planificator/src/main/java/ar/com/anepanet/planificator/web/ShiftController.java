package ar.com.anepanet.planificator.web;

import ar.com.anepanet.planificator.domain.HoursByUserWeek;
import ar.com.anepanet.planificator.domain.Shift;
import ar.com.anepanet.planificator.domain.ShiftWeekView;
import ar.com.anepanet.planificator.service.ShiftService;
import ar.com.anepanet.planificator.web.dto.CreateShiftRequest;
import ar.com.anepanet.planificator.web.dto.UpdateShiftRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/summary")
    public List<HoursByUserWeek> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekStart) {
        return service.hoursByWeek(weekStart);
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
