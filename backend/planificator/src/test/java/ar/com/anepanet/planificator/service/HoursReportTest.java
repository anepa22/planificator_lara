package ar.com.anepanet.planificator.service;

import ar.com.anepanet.planificator.domain.AppUser;
import ar.com.anepanet.planificator.domain.HoursReport;
import ar.com.anepanet.planificator.domain.HoursReportPerson;
import ar.com.anepanet.planificator.domain.HoursShiftRow;
import ar.com.anepanet.planificator.domain.Location;
import ar.com.anepanet.planificator.domain.StaffMember;
import ar.com.anepanet.planificator.repository.AuthRepository;
import ar.com.anepanet.planificator.repository.LocationRepository;
import ar.com.anepanet.planificator.repository.ShiftRepository;
import ar.com.anepanet.planificator.security.AuthUser;
import ar.com.anepanet.planificator.security.Permissions;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HoursReportTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 14);

    @Mock ShiftRepository shifts;
    @Mock AuthRepository users;
    @Mock LocationRepository locations;
    @Mock AuditService audit;
    @Mock ApplicationEventPublisher events;
    @Mock VacationQuotaService quotas;

    private ShiftService service;

    @BeforeEach
    void setUp() {
        service = new ShiftService(shifts, users, locations, audit, events, quotas);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void morningToEveningWithOneHourLunchIsNineAndAHalf() {
        UUID ana = UUID.randomUUID();
        List<HoursReportPerson> people = HoursReportMath.people(
                List.of(shift(ana, "Ana", "1", DAY, "09:30", "20:00")),
                null,
                null,
                null,
                new BigDecimal("1"));

        assertEquals(1, people.size());
        assertEquals(new BigDecimal("9.50"), people.get(0).hours());
    }

    @Test
    void twoShiftsOnTheSameDaySubtractLunchOnce() {
        UUID ana = UUID.randomUUID();
        List<HoursReportPerson> people = HoursReportMath.people(
                List.of(
                        shift(ana, "Ana", "1", DAY, "08:00", "12:00"),
                        shift(ana, "Ana", "2", DAY, "13:00", "18:00")),
                null,
                null,
                null,
                new BigDecimal("1"));

        assertEquals(new BigDecimal("8.00"), people.get(0).hours());
    }

    @Test
    void francoIsOmittedUnlessThatPersonIsSelected() {
        UUID ana = UUID.randomUUID();
        List<HoursShiftRow> rows = List.of(shift(ana, "Ana", "franco", DAY, "08:00", "22:00"));

        assertTrue(HoursReportMath.people(rows, null, null, null, new BigDecimal("1")).isEmpty());

        List<HoursReportPerson> selected = HoursReportMath.people(
                rows, ana, "Ana", null, new BigDecimal("1"));
        assertEquals(1, selected.size());
        assertEquals(new BigDecimal("0.00"), selected.get(0).hours());
    }

    @Test
    void rejectsARangeThatEndsBeforeItStartsOrExceeds800Days() {
        ResponseStatusException backwards = assertThrows(
                ResponseStatusException.class,
                () -> HoursReportMath.requireRange(LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 1)));
        assertEquals(HttpStatus.BAD_REQUEST, backwards.getStatusCode());

        ResponseStatusException tooLong = assertThrows(
                ResponseStatusException.class,
                () -> HoursReportMath.requireRange(LocalDate.of(2024, 1, 1), LocalDate.of(2026, 3, 11)));
        assertEquals(HttpStatus.BAD_REQUEST, tooLong.getStatusCode());
    }

    @Test
    void missingLunchDefaultsToOneHourAndValuesOutsideZeroToFourAreRejected() {
        assertEquals(new BigDecimal("1.00"), HoursReportMath.requireLunch(null));
        assertEquals(HttpStatus.BAD_REQUEST, assertThrows(
                ResponseStatusException.class,
                () -> HoursReportMath.requireLunch(new BigDecimal("5"))).getStatusCode());
    }

    @Test
    void visibleRowsSumToTheTotal() {
        UUID ana = UUID.randomUUID();
        UUID beto = UUID.randomUUID();
        List<HoursReportPerson> people = HoursReportMath.people(
                List.of(
                        shift(ana, "Ana", "1", DAY, "08:00", "18:00"),
                        shift(beto, "Beto", "1", DAY, "08:00", "13:00")),
                null,
                null,
                null,
                BigDecimal.ZERO);
        assertEquals(new BigDecimal("15.00"), HoursReportMath.total(people));
    }

    @Test
    void locationOptionsSkipFrancoAndVacationsAndKeepInactiveLocationsWithShifts() {
        List<HoursReportMath.LocationChoice> catalog = List.of(
                new HoursReportMath.LocationChoice("1", "Lara 1", true),
                new HoursReportMath.LocationChoice("3", "Lara 3", false),
                new HoursReportMath.LocationChoice("4", "Lara 4", false),
                new HoursReportMath.LocationChoice("franco", "Franco", true),
                new HoursReportMath.LocationChoice("vacaciones", "Vacaciones", true));

        List<String> ids = HoursReportMath.locationOptions(catalog, java.util.Set.of("3")).stream()
                .map(option -> option.id())
                .toList();

        assertEquals(List.of("1", "3"), ids);
    }

    @Test
    void reportAndPdfRequireTheHoursPermission() {
        LocalDate from = LocalDate.of(2026, 10, 1);
        LocalDate to = LocalDate.of(2026, 10, 31);
        ResponseStatusException anonymous = assertThrows(
                ResponseStatusException.class,
                () -> service.hoursReport(from, to, null, null, null));
        assertEquals(HttpStatus.FORBIDDEN, anonymous.getStatusCode());

        login(Permissions.SHIFTS_WRITE);
        ResponseStatusException writer = assertThrows(
                ResponseStatusException.class,
                () -> service.hoursReportPdf(from, to, null, null, null));
        assertEquals(HttpStatus.FORBIDDEN, writer.getStatusCode());
    }

    @Test
    void francoLocationFilterIsRejected() {
        login(Permissions.HOURS_REPORT);
        ResponseStatusException rejected = assertThrows(
                ResponseStatusException.class,
                () -> service.hoursReport(
                        LocalDate.of(2026, 10, 1),
                        LocalDate.of(2026, 10, 31),
                        null,
                        "vacaciones",
                        null));
        assertEquals(HttpStatus.BAD_REQUEST, rejected.getStatusCode());
    }

    @Test
    void pdfRepeatsTheNetHoursAndTheTotal() throws Exception {
        UUID ana = UUID.randomUUID();
        login(Permissions.HOURS_REPORT);
        when(shifts.findHoursShifts(DAY, DAY)).thenReturn(List.of(
                shift(ana, "Ana", "1", DAY, "09:30", "20:00")));
        when(users.findActiveStaff()).thenReturn(List.of(new StaffMember(ana, "Ana", "#112233")));
        when(locations.findAll()).thenReturn(List.of(location("1", "Lara 1", true)));

        HoursReport report = service.hoursReport(DAY, DAY, null, null, new BigDecimal("1"));
        assertEquals(new BigDecimal("9.50"), report.totalHours());

        byte[] pdf = service.hoursReportPdf(DAY, DAY, null, null, new BigDecimal("1"));
        PdfReader reader = new PdfReader(pdf);
        try {
            String text = new PdfTextExtractor(reader).getTextFromPage(1).replaceAll("\\s+", " ");
            assertTrue(text.contains("Ana"), text);
            assertTrue(text.contains("9,5"), text);
            assertTrue(text.contains("Total"), text);
            assertTrue(text.contains("Almuerzo: 1 h"), text);
        } finally {
            reader.close();
        }
    }

    @Test
    void selectedPersonWithOnlyFrancoShowsZero() {
        UUID ana = UUID.randomUUID();
        login(Permissions.HOURS_REPORT);
        when(users.findById(ana)).thenReturn(Optional.of(user(ana, "Ana")));
        when(shifts.findHoursShifts(DAY, DAY)).thenReturn(List.of(
                shift(ana, "Ana", "franco", DAY, "08:00", "22:00")));
        when(users.findActiveStaff()).thenReturn(List.of(new StaffMember(ana, "Ana", "#112233")));
        when(locations.findAll()).thenReturn(List.of());

        HoursReport report = service.hoursReport(DAY, DAY, ana, null, BigDecimal.ZERO);

        assertEquals(1, report.people().size());
        assertEquals(new BigDecimal("0.00"), report.people().get(0).hours());
        assertEquals(new BigDecimal("0.00"), report.totalHours());
    }

    private static HoursShiftRow shift(
            UUID userId, String name, String locationId, LocalDate day, String start, String end) {
        return new HoursShiftRow(
                userId, name, locationId, day, LocalTime.parse(start), LocalTime.parse(end));
    }

    private static Location location(String id, String name, boolean active) {
        OffsetDateTime now = OffsetDateTime.parse("2026-10-27T12:00:00Z");
        return new Location(id, name, "#112233", "#D0D5DB", (short) 1, active, false, now, now);
    }

    private static AppUser user(UUID id, String name) {
        OffsetDateTime now = OffsetDateTime.parse("2026-10-27T12:00:00Z");
        return new AppUser(
                id, "ana", "hash", name, "#112233", null,
                true, true, false, null, now, now, List.of(), List.of(), List.of());
    }

    private void login(String permission) {
        AuthUser principal = new AuthUser(
                UUID.randomUUID(), "admin", List.of(new SimpleGrantedAuthority(permission)));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }
}
