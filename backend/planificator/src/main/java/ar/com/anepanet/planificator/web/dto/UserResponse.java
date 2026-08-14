package ar.com.anepanet.planificator.web.dto;

import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String displayName,
        boolean active,
        List<String> roles,
        List<String> permissions
) {}
