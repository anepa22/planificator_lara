package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStaffRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 20) String color
) {}
