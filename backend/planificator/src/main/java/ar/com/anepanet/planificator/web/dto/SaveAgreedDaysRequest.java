package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SaveAgreedDaysRequest(
        @NotNull UUID userId,
        @NotNull @Min(1990) @Max(2100) Integer periodYear,
        @NotNull @Min(0) @Max(366) Integer days
) {}
