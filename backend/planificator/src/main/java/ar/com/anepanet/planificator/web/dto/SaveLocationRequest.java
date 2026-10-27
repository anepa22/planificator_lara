package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveLocationRequest(
        @NotBlank String name,
        @NotBlank String color,
        Boolean supportsVidriera
) {}
