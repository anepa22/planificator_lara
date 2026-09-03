package ar.com.anepanet.planificator.web.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        String token,
        UUID id,
        String username,
        String displayName,
        UUID personId,
        List<String> roles,
        List<String> permissions
) {}
