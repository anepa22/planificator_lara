package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePersonRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 20) String color
) {}
