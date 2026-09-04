package ar.com.anepanet.planificator.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AppUser(
        UUID id,
        String username,
        String passwordHash,
        String displayName,
        String color,
        boolean active,
        boolean canLogin,
        boolean mustChangePassword,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<String> roleIds,
        List<String> permissions
) {}
