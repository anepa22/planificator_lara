package ar.com.anepanet.planificator.web.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String displayName,
        String color,
        String telegramChatId,
        boolean active,
        boolean canLogin,
        List<String> roles,
        List<String> individualPermissions,
        List<String> permissions,
        LocalDate hireDate
) {}
