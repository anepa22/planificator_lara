package ar.com.anepanet.planificator.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class VacationOpeningCarryRepository {

    private final JdbcClient jdbc;

    public VacationOpeningCarryRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Una fila ausente equivale a cero. Solo aplica al período que abre el 1 oct 2026. */
    public int findDays(UUID userId) {
        Integer days = jdbc.sql("""
                SELECT days
                FROM vacation_opening_carry
                WHERE user_id = :userId
                """)
                .param("userId", userId)
                .query(Integer.class)
                .optional()
                .orElse(null);
        return days == null ? 0 : days;
    }

    public void save(UUID userId, int days) {
        jdbc.sql("""
                INSERT INTO vacation_opening_carry (user_id, days)
                VALUES (:userId, :days)
                ON CONFLICT (user_id) DO UPDATE SET days = EXCLUDED.days
                """)
                .param("userId", userId)
                .param("days", days)
                .update();
    }
}
