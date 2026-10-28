package ar.com.anepanet.planificator.web;

import ar.com.anepanet.planificator.service.VacationQuotaService;
import ar.com.anepanet.planificator.web.dto.SaveVacationAdjustmentRequest;
import ar.com.anepanet.planificator.web.dto.VacationQuotaResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/vacation-quota")
public class VacationQuotaController {

    private final VacationQuotaService quotas;

    public VacationQuotaController(VacationQuotaService quotas) {
        this.quotas = quotas;
    }

    @GetMapping
    public VacationQuotaResponse quota(
            @RequestParam UUID userId,
            @RequestParam int periodYear) {
        return quotas.quota(userId, periodYear);
    }

    @PutMapping
    public VacationQuotaResponse save(@Valid @RequestBody SaveVacationAdjustmentRequest request) {
        return quotas.saveAdjustment(request.userId(), request.periodYear(), request.days(), request.carried());
    }
}
