package ar.com.anepanet.planificator.web.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateRolePermissionsRequest(
        @NotNull List<String> permissionCodes
) {}
