package ar.com.anepanet.planificator.repository;

import ar.com.anepanet.planificator.domain.Location;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class LocationRepository {

    private static final String COLUMNS = """
            id, name, color, color_soft, sort_order, is_active, supports_vidriera, created_at, updated_at
            """;

    private final JdbcClient jdbc;

    public LocationRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Location> findAllActive() {
        return jdbc.sql("""
                SELECT %s
                FROM locations
                WHERE is_active = TRUE
                ORDER BY sort_order, name
                """.formatted(COLUMNS))
                .query(this::map)
                .list();
    }

    public List<Location> findAll() {
        return jdbc.sql("""
                SELECT %s
                FROM locations
                ORDER BY sort_order, name
                """.formatted(COLUMNS))
                .query(this::map)
                .list();
    }

    public Optional<Location> findById(String id) {
        return jdbc.sql("""
                SELECT %s
                FROM locations
                WHERE id = :id
                """.formatted(COLUMNS))
                .param("id", id)
                .query(this::map)
                .optional();
    }

    public boolean idExists(String id) {
        Integer count = jdbc.sql("""
                SELECT count(*)::int FROM locations WHERE id = :id
                """)
                .param("id", id)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    public boolean nameTaken(String nameKey, String exceptId) {
        Integer count = jdbc.sql("""
                SELECT count(*)::int
                FROM locations
                WHERE lower(btrim(name)) = :name
                  AND id <> :exceptId
                """)
                .param("name", nameKey)
                .param("exceptId", exceptId == null ? "" : exceptId)
                .query(Integer.class)
                .single();
        return count != null && count > 0;
    }

    public short nextSortOrder() {
        Integer max = jdbc.sql("""
                SELECT COALESCE(MAX(sort_order), 0)::int FROM locations
                """)
                .query(Integer.class)
                .single();
        int next = (max == null ? 0 : max) + 1;
        if (next > Short.MAX_VALUE) {
            return Short.MAX_VALUE;
        }
        return (short) next;
    }

    public void insert(
            String id,
            String name,
            String color,
            String colorSoft,
            short sortOrder,
            boolean supportsVidriera) {
        jdbc.sql("""
                INSERT INTO locations (
                    id, name, color, color_soft, sort_order, is_active, supports_vidriera, created_at, updated_at
                ) VALUES (
                    :id, :name, :color, :colorSoft, :sortOrder, TRUE, :supportsVidriera, NOW(), NOW()
                )
                """)
                .param("id", id)
                .param("name", name)
                .param("color", color)
                .param("colorSoft", colorSoft)
                .param("sortOrder", sortOrder)
                .param("supportsVidriera", supportsVidriera)
                .update();
    }

    public int update(
            String id,
            String name,
            String color,
            String colorSoft,
            short sortOrder,
            boolean active,
            boolean supportsVidriera) {
        return jdbc.sql("""
                UPDATE locations
                SET name = :name,
                    color = :color,
                    color_soft = :colorSoft,
                    sort_order = :sortOrder,
                    is_active = :active,
                    supports_vidriera = :supportsVidriera,
                    updated_at = NOW()
                WHERE id = :id
                """)
                .param("id", id)
                .param("name", name)
                .param("color", color)
                .param("colorSoft", colorSoft)
                .param("sortOrder", sortOrder)
                .param("active", active)
                .param("supportsVidriera", supportsVidriera)
                .update();
    }

    private Location map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Location(
                rs.getString("id"),
                rs.getString("name"),
                rs.getString("color"),
                rs.getString("color_soft"),
                rs.getShort("sort_order"),
                rs.getBoolean("is_active"),
                rs.getBoolean("supports_vidriera"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }
}
