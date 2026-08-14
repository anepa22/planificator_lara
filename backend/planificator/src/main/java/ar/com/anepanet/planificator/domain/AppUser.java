package ar.com.anepanet.planificator.domain;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AppUser(
        UUID id,
        String username,
        String passwordHash,
        String displayName,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<String> roleIds,
        List<String> permissions
) {}
