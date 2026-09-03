package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record UpdateUserRequest(
        @NotBlank @Size(max = 120) String displayName,
        @NotNull Boolean active,
        String password,
        UUID personId,
        @NotNull List<String> roleIds
) {}
