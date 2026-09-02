package ar.com.anepanet.planificator.web;

import ar.com.anepanet.planificator.domain.Vidriera;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.service.VidrieraService;
import ar.com.anepanet.planificator.web.dto.UpsertVidrieraRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/vidrieras")
public class VidrieraController {

    private final VidrieraService service;

    public VidrieraController(VidrieraService service) {
        this.service = service;
    }

    @GetMapping
    public List<Vidriera> list(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.byRange(from, to);
    }

    @PutMapping
    @PreAuthorize("hasAuthority('" + Permissions.SHIFTS_WRITE + "')")
    public Vidriera upsert(@Valid @RequestBody UpsertVidrieraRequest request) {
        return service.upsert(request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('" + Permissions.SHIFTS_WRITE + "')")
    public void delete(
            @RequestParam String locationId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate workDate) {
        service.delete(locationId, workDate);
    }
}
