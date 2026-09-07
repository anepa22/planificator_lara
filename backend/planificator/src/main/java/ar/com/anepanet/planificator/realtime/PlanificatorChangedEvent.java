package ar.com.anepanet.planificator.realtime;

public record PlanificatorChangedEvent(String scope) {

    public static final String TASKS = "tasks";
    public static final String SCHEDULE = "schedule";
    public static final String VIDRIERAS = "vidrieras";
}
