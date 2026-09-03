package ar.com.anepanet.planificator.web.dto;

import java.util.UUID;

public record TaskAssignee(
        UUID id,
        String username,
        String displayName
) {}
