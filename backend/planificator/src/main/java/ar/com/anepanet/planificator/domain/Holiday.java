package ar.com.anepanet.planificator.domain;

import java.time.LocalDate;

public record Holiday(
        LocalDate date,
        String name,
        String type
) {}
