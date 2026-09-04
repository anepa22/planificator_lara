package ar.com.anepanet.planificator.web.dto;

import ar.com.anepanet.planificator.domain.TaskHistory;

import java.util.List;

public record TaskHistoryResponse(
        TaskHistory pending,
        TaskHistory assigned,
        TaskHistory blocked,
        TaskHistory done,
        TaskHistory verified,
        List<TaskHistory> movements
) {}
