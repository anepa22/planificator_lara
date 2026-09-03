package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignTaskRequest(
        @NotNull UUID userId
) {}
