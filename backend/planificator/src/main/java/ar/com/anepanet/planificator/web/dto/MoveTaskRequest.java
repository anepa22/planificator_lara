package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MoveTaskRequest(
        @NotBlank String status,
        @Size(max = 2000) String blockReason
) {}
