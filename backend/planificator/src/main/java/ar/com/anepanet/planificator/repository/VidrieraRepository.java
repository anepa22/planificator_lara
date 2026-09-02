package ar.com.anepanet.planificator.repository;

import ar.com.anepanet.planificator.domain.Vidriera;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public class VidrieraRepository {

    private final JdbcClient jdbc;

    public VidrieraRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Vidriera> findByDateRange(LocalDate from, LocalDate to) {
        return jdbc.sql("""
                SELECT v.location_id, l.name AS location_name, v.work_date
                FROM location_vidrieras v
                JOIN locations l ON l.id = v.location_id
                WHERE v.work_date BETWEEN :from AND :to
                ORDER BY v.work_date, l.sort_order, l.name
                """)
                .param("from", from)
                .param("to", to)
                .query(this::map)
                .list();
    }

    public Optional<Vidriera> findOne(String locationId, LocalDate workDate) {
        return jdbc.sql("""
                SELECT v.location_id, l.name AS location_name, v.work_date
                FROM location_vidrieras v
                JOIN locations l ON l.id = v.location_id
                WHERE v.location_id = :locationId AND v.work_date = :workDate
                """)
                .param("locationId", locationId)
                .param("workDate", workDate)
                .query(this::map)
                .optional();
    }

    /** @return true si insertó una fila nueva */
    public boolean insert(String locationId, LocalDate workDate) {
        int n = jdbc.sql("""
                INSERT INTO location_vidrieras (location_id, work_date)
                VALUES (:locationId, :workDate)
                ON CONFLICT (location_id, work_date) DO NOTHING
                """)
                .param("locationId", locationId)
                .param("workDate", workDate)
                .update();
        return n > 0;
    }

    public boolean delete(String locationId, LocalDate workDate) {
        int n = jdbc.sql("""
                DELETE FROM location_vidrieras
                WHERE location_id = :locationId AND work_date = :workDate
                """)
                .param("locationId", locationId)
                .param("workDate", workDate)
                .update();
        return n > 0;
    }

    private Vidriera map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Vidriera(
                rs.getString("location_id"),
                rs.getString("location_name"),
                rs.getObject("work_date", LocalDate.class)
        );
    }
}
