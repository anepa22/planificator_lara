package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TaskRetentionSettings(
        @NotNull @Min(1) @Max(3650) Integer verifiedRetentionDays
) {}
