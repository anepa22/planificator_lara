package ar.com.anepanet.planificator.security;

public final class Permissions {

    public static final String SHIFTS_WRITE = "shifts:write";
    public static final String VACATIONS_WRITE = "vacations:write";
    public static final String STAFF_WRITE = "staff:write";
    public static final String LUNCH_MANAGE = "lunch:manage";
    public static final String USERS_MANAGE = "users:manage";
    public static final String ROLES_MANAGE = "roles:manage";
    public static final String AUDIT_READ = "audit:read";
    public static final String TASKS_WRITE = "tasks:write";
    public static final String TASKS_MANAGE = "tasks:manage";
    public static final String TASKS_HISTORY = "tasks:history";

    public static final String VACATION_LOCATION_ID = "vacaciones";
    public static final String FRANCO_LOCATION_ID = "franco";

    private Permissions() {}

    public static boolean isAbsenceLocation(String locationId) {
        return VACATION_LOCATION_ID.equals(locationId) || FRANCO_LOCATION_ID.equals(locationId);
    }

    public static String writePermissionForLocation(String locationId) {
        return isAbsenceLocation(locationId) ? VACATIONS_WRITE : SHIFTS_WRITE;
    }
}
