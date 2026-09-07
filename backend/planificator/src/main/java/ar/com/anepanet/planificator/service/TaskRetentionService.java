package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.repository.TaskRepository;
import ar.com.anepanet.planificator.realtime.PlanificatorChangedEvent;
import ar.com.anepanet.planificator.security.Permissions;
import ar.com.anepanet.planificator.security.SecurityUtils;
import ar.com.anepanet.planificator.web.dto.TaskRetentionSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskRetentionService {

    private static final Logger log = LoggerFactory.getLogger(TaskRetentionService.class);

    private final TaskRepository tasks;
    private final ApplicationEventPublisher events;

    public TaskRetentionService(TaskRepository tasks, ApplicationEventPublisher events) {
        this.tasks = tasks;
        this.events = events;
    }

    public TaskRetentionSettings settings() {
        SecurityUtils.requireAuthority(Permissions.TASKS_RETENTION);
        return new TaskRetentionSettings(tasks.verifiedRetentionDays());
    }

    @Transactional
    public TaskRetentionSettings update(TaskRetentionSettings request) {
        SecurityUtils.requireAuthority(Permissions.TASKS_RETENTION);
        tasks.updateVerifiedRetentionDays(request.verifiedRetentionDays());
        retireExpiredAndPublish();
        return new TaskRetentionSettings(request.verifiedRetentionDays());
    }

    @Scheduled(initialDelay = 60_000, fixedDelay = 3_600_000)
    @Transactional
    public void scheduledRetirement() {
        try {
            int retired = retireExpiredAndPublish();
            if (retired > 0) {
                log.info("Retired {} verified tasks after retention period", retired);
            }
        } catch (Exception ex) {
            log.warn("Verified task retirement skipped: {}", ex.getMessage());
        }
    }

    private int retireExpired() {
        return tasks.retireExpiredVerified(tasks.verifiedRetentionDays());
    }

    private int retireExpiredAndPublish() {
        int retired = retireExpired();
        if (retired > 0) {
            events.publishEvent(new PlanificatorChangedEvent(PlanificatorChangedEvent.TASKS));
        }
        return retired;
    }
}
