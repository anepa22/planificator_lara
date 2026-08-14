package ar.com.anepanet.planificator.repository;

import ar.com.anepanet.planificator.domain.HoursByPersonWeek;
import ar.com.anepanet.planificator.domain.Shift;
import ar.com.anepanet.planificator.domain.ShiftWeekView;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ShiftRepository {

    private final JdbcClient jdbc;

    public ShiftRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<ShiftWeekView> findByWeekStart(LocalDate weekStart) {
        return jdbc.sql("""
                SELECT id, person_id, person_name, person_color,
                       location_id, location_name, location_color, location_color_soft,
                       work_date, week_start, day_index, start_time, end_time, hours, notes
                FROM v_shifts_week
                WHERE week_start = :weekStart
                ORDER BY location_id, start_time, person_name
                """)
                .param("weekStart", weekStart)
                .query(this::mapWeekView)
                .list();
    }

    public List<HoursByPersonWeek> hoursByWeek(LocalDate weekStart) {
        return jdbc.sql("""
                SELECT week_start, person_id, person_name, total_hours, shift_count
                FROM v_hours_by_person_week
                WHERE week_start = :weekStart
                ORDER BY person_name
                """)
                .param("weekStart", weekStart)
                .query((rs, n) -> new HoursByPersonWeek(
                        rs.getObject("week_start", LocalDate.class),
                        rs.getObject("person_id", UUID.class),
                        rs.getString("person_name"),
                        rs.getBigDecimal("total_hours"),
                        rs.getInt("shift_count")
                ))
                .list();
    }

    public Optional<Shift> findById(UUID id) {
        return jdbc.sql("""
                SELECT id, person_id, location_id, work_date, start_time, end_time, notes, created_at, updated_at
                FROM shifts
                WHERE id = :id
                """)
                .param("id", id)
                .query(this::mapShift)
                .optional();
    }

    public Shift insert(UUID personId, String locationId, LocalDate workDate,
                        LocalTime startTime, LocalTime endTime, String notes) {
        return jdbc.sql("""
                INSERT INTO shifts (person_id, location_id, work_date, start_time, end_time, notes)
                VALUES (:personId, :locationId, :workDate, :startTime, :endTime, :notes)
                RETURNING id, person_id, location_id, work_date, start_time, end_time, notes, created_at, updated_at
                """)
                .param("personId", personId)
                .param("locationId", locationId)
                .param("workDate", workDate)
                .param("startTime", startTime)
                .param("endTime", endTime)
                .param("notes", notes)
                .query(this::mapShift)
                .single();
    }

    public Optional<Shift> update(UUID id, String locationId, LocalDate workDate,
                                  LocalTime startTime, LocalTime endTime, String notes) {
        return jdbc.sql("""
                UPDATE shifts
                SET location_id = :locationId,
                    work_date = :workDate,
                    start_time = :startTime,
                    end_time = :endTime,
                    notes = :notes,
                    updated_at = NOW()
                WHERE id = :id
                RETURNING id, person_id, location_id, work_date, start_time, end_time, notes, created_at, updated_at
                """)
                .param("id", id)
                .param("locationId", locationId)
                .param("workDate", workDate)
                .param("startTime", startTime)
                .param("endTime", endTime)
                .param("notes", notes)
                .query(this::mapShift)
                .optional();
    }

    public boolean delete(UUID id) {
        int deleted = jdbc.sql("DELETE FROM shifts WHERE id = :id")
                .param("id", id)
                .update();
        return deleted > 0;
    }

    private Shift mapShift(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Shift(
                rs.getObject("id", UUID.class),
                rs.getObject("person_id", UUID.class),
                rs.getString("location_id"),
                rs.getObject("work_date", LocalDate.class),
                rs.getObject("start_time", LocalTime.class),
                rs.getObject("end_time", LocalTime.class),
                rs.getString("notes"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }

    private ShiftWeekView mapWeekView(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ShiftWeekView(
                rs.getObject("id", UUID.class),
                rs.getObject("person_id", UUID.class),
                rs.getString("person_name"),
                rs.getString("person_color"),
                rs.getString("location_id"),
                rs.getString("location_name"),
                rs.getString("location_color"),
                rs.getString("location_color_soft"),
                rs.getObject("work_date", LocalDate.class),
                rs.getObject("week_start", LocalDate.class),
                rs.getShort("day_index"),
                rs.getObject("start_time", LocalTime.class),
                rs.getObject("end_time", LocalTime.class),
                rs.getBigDecimal("hours"),
                rs.getString("notes")
        );
    }
}
