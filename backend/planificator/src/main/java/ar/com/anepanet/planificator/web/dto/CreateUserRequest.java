package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateUserRequest(
        @NotBlank @Size(max = 80) String username,
        @Size(max = 100) String password,
        @NotBlank @Size(max = 120) String displayName,
        @Size(max = 20) String color,
        Boolean canLogin,
        @NotNull List<String> roleIds
) {}
