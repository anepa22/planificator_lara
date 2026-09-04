package ar.com.anepanet.planificator.domain;

import java.util.List;
import java.util.UUID;

public record UserPrincipal(
        UUID id,
        String username,
        boolean active,
        boolean mustChangePassword,
        List<String> permissions
) {}
