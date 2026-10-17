package ar.com.anepanet.planificator.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record AppUser(
        UUID id,
        String username,
        String passwordHash,
        String displayName,
        String color,
        String telegramChatId,
        boolean active,
        boolean canLogin,
        boolean mustChangePassword,
        LocalDate hireDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<String> roleIds,
        List<String> individualPermissions,
        List<String> permissions
) {}
