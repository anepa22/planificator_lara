package ar.com.anepanet.planificator.repository;

import ar.com.anepanet.planificator.domain.Location;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class LocationRepository {

    private final JdbcClient jdbc;

    public LocationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Location> findAllActive() {
        return jdbc.sql("""
                SELECT id, name, color, color_soft, sort_order, is_active, created_at, updated_at
                FROM locations
                WHERE is_active = TRUE
                ORDER BY sort_order, name
                """)
                .query(this::map)
                .list();
    }

    public Optional<Location> findById(String id) {
        return jdbc.sql("""
                SELECT id, name, color, color_soft, sort_order, is_active, created_at, updated_at
                FROM locations
                WHERE id = :id
                """)
                .param("id", id)
                .query(this::map)
                .optional();
    }

    private Location map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Location(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("color"),
                rs.getString("color_soft"),
                rs.getShort("sort_order"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }
}
