package ar.com.anepanet.planificator.web;

import ar.com.anepanet.planificator.domain.AuditEntry;
import ar.com.anepanet.planificator.domain.AuditUserOption;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.service.AuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService audit;

    public AuditController(AuditService audit) {
        this.audit = audit;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('" + Permissions.AUDIT_READ + "')")
    public List<AuditEntry> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Integer limit) {
        return audit.search(from, to, username, entityType, limit);
    }

    @GetMapping("/users")
    @PreAuthorize("hasAuthority('" + Permissions.AUDIT_READ + "')")
    public List<AuditUserOption> users() {
        return audit.filterUsers();
    }
}
