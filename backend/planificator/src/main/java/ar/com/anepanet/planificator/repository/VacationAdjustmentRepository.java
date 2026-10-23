package ar.com.anepanet.planificator.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
public class VacationAdjustmentRepository {

    private final JdbcClient jdbc;

    public VacationAdjustmentRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Una fila ausente equivale a cero. */
    public int findDays(UUID userId, int periodYear) {
        Integer days = jdbc.sql("""
                SELECT days
                FROM vacation_adjustments
                WHERE user_id = :userId AND period_year = :periodYear
                """)
                .param("userId", userId)
                .param("periodYear", periodYear)
                .query(Integer.class)
                .optional()
                .orElse(null);
        return days == null ? 0 : days;
    }

    public Map<Integer, Integer> findByUser(UUID userId) {
        Map<Integer, Integer> days = new HashMap<>();
        jdbc.sql("""
                SELECT period_year, days
                FROM vacation_adjustments
                WHERE user_id = :userId
                """)
                .param("userId", userId)
                .query((rs, n) -> Map.entry(rs.getInt("period_year"), rs.getInt("days")))
                .list()
                .forEach(row -> days.put(row.getKey(), row.getValue()));
        return days;
    }

    public void save(UUID userId, int periodYear, int days) {
        jdbc.sql("""
                INSERT INTO vacation_adjustments (user_id, period_year, days)
                VALUES (:userId, :periodYear, :days)
                ON CONFLICT (user_id, period_year) DO UPDATE SET days = EXCLUDED.days
                """)
                .param("userId", userId)
                .param("periodYear", periodYear)
                .param("days", days)
                .update();
    }
}
