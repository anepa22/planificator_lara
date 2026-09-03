package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record CreateUserRequest(
        @NotBlank @Size(max = 80) String username,
        @NotBlank @Size(min = 6, max = 100) String password,
        @NotBlank @Size(max = 120) String displayName,
        UUID personId,
        @NotNull List<String> roleIds
) {}
