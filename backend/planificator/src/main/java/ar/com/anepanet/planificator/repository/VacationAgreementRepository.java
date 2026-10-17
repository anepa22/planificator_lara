package ar.com.anepanet.planificator.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Repository
public class VacationAgreementRepository {

    private final JdbcClient jdbc;

    public VacationAgreementRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Map<Integer, Integer> findByUser(UUID userId) {
        Map<Integer, Integer> days = new HashMap<>();
        jdbc.sql("""
                SELECT period_year, days
                FROM vacation_agreed_days
                WHERE user_id = :userId
                """)
                .param("userId", userId)
                .query((rs, n) -> Map.entry(rs.getInt("period_year"), rs.getInt("days")))
                .list()
                .forEach(row -> days.put(row.getKey(), row.getValue()));
        return days;
    }

    public void upsert(UUID userId, int periodYear, int days) {
        jdbc.sql("""
                INSERT INTO vacation_agreed_days (user_id, period_year, days)
                VALUES (:userId, :periodYear, :days)
                ON CONFLICT (user_id, period_year)
                DO UPDATE SET days = EXCLUDED.days
                """)
                .param("userId", userId)
                .param("periodYear", periodYear)
                .param("days", days)
                .update();
    }
}
