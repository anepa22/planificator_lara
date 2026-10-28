package ar.com.anepanet.planificator.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record HoursReportPerson(UUID userId, String userName, BigDecimal hours) {}
