package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTaskRequest(
        @NotBlank @Size(max = 160) String title,
        @Size(max = 4000) String description,
        @Size(max = 80) String locationId
) {}
