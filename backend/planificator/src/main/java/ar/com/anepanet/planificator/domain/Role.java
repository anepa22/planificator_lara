package ar.com.anepanet.planificator.domain;

import java.util.List;

public record Role(
        String id,
        String code,
        String name,
        List<String> permissionCodes
) {}
