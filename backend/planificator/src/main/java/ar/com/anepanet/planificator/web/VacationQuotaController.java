package ar.com.anepanet.planificator.web;

import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.service.ShiftService;
import ar.com.anepanet.planificator.service.VacationQuota;
import ar.com.anepanet.planificator.service.VacationQuotaService;
import ar.com.anepanet.planificator.web.dto.AssignVacationsRequest;
import ar.com.anepanet.planificator.web.dto.AssignVacationsResponse;
import ar.com.anepanet.planificator.web.dto.SaveAgreedDaysRequest;
import ar.com.anepanet.planificator.web.dto.VacationBalanceResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vacations")
public class VacationQuotaController {

    private final VacationQuotaService quotas;
    private final ShiftService shifts;

    public VacationQuotaController(VacationQuotaService quotas, ShiftService shifts) {
        this.quotas = quotas;
        this.shifts = shifts;
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAuthority('" + Permissions.VACATIONS_WRITE + "')")
    public List<VacationBalanceResponse> balance(
            @RequestParam List<UUID> userId,
            @RequestParam(required = false) List<Integer> periodYear,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<Integer> years = periodYear;
        if ((years == null || years.isEmpty()) && from != null && to != null) {
            years = VacationQuota.eachDate(from, to).stream()
                    .map(VacationQuota::periodYear)
                    .distinct()
                    .sorted()
                    .toList();
        }
        return quotas.preview(userId, years);
    }

    @PutMapping("/agreed-days")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permissions.VACATIONS_WRITE + "')")
    public void saveAgreedDays(@Valid @RequestBody SaveAgreedDaysRequest request) {
        quotas.saveAgreedDays(request.userId(), request.periodYear(), request.days());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('" + Permissions.VACATIONS_WRITE + "')")
    public AssignVacationsResponse assign(@Valid @RequestBody AssignVacationsRequest request) {
        int created = shifts.assignVacations(request);
        return new AssignVacationsResponse(created);
    }
}
