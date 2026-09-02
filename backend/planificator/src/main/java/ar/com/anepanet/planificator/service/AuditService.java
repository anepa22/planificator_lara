package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AuditEntry;
import ar.com.anepanet.planificator.domain.AuditUserOption;
import ar.com.anepanet.planificator.repository.AuditRepository;
import ar.com.anepanet.planificator.security.AuthUser;
import ar.com.anepanet.planificator.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    public static final String ACTION_CREATE = "CREATE";
    public static final String ACTION_UPDATE = "UPDATE";
    public static final String ACTION_DELETE = "DELETE";
    public static final String ACTION_LOGIN = "LOGIN";
    public static final String ACTION_LOGOUT = "LOGOUT";
    public static final String ACTION_LOGIN_FAIL = "LOGIN_FAIL";

    public static final String TYPE_SHIFT = "shift";
    public static final String TYPE_VACATION = "vacation";
    public static final String TYPE_FRANCO = "franco";
    public static final String TYPE_PERSON = "person";
    public static final String TYPE_USER = "user";
    public static final String TYPE_ROLE = "role";
    public static final String TYPE_SESSION = "session";
    public static final String TYPE_VIDRIERA = "vidriera";

    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final AuditRepository audit;

    public AuditService(AuditRepository audit) {
        this.audit = audit;
    }

    public void record(String action, String entityType, String entityId, String summary) {
        AuthUser user = SecurityUtils.currentUser().orElse(null);
        UUID userId = user != null ? user.getId() : null;
        String username = user != null ? user.getUsername() : "sistema";
        recordAs(userId, username, action, entityType, entityId, summary);
    }

    /** Para eventos sin SecurityContext (login / intento fallido). */
    public void recordAs(
            UUID userId,
            String username,
            String action,
            String entityType,
            String entityId,
            String summary) {
        try {
            String who = (username == null || username.isBlank()) ? "sistema" : username.trim();
            audit.insert(userId, who, action, entityType, entityId, summary);
        } catch (Exception ex) {
            Throwable root = NestedExceptionUtils.getMostSpecificCause(ex);
            log.warn("No se pudo registrar bitácora: {}", root.getMessage());
        }
    }

    public List<AuditEntry> search(
            LocalDate from,
            LocalDate to,
            String username,
            String entityType,
            Integer limit) {
        int lim = limit == null
                ? DEFAULT_LIMIT
                : Math.min(Math.max(limit, 1), MAX_LIMIT);
        return audit.search(from, to, username, entityType, lim);
    }

    public List<AuditUserOption> filterUsers() {
        return audit.listFilterUsers();
    }
}
