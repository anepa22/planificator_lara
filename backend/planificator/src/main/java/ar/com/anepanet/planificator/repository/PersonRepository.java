package ar.com.anepanet.planificator.repository;

import ar.com.anepanet.planificator.domain.Person;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PersonRepository {

    private final JdbcClient jdbc;

    public PersonRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public List<Person> findAllActive() {
        return jdbc.sql("""
                SELECT id, name, color, is_active, created_at, updated_at
                FROM people
                WHERE is_active = TRUE
                ORDER BY name
                """)
                .query(this::map)
                .list();
    }

    public Optional<Person> findById(UUID id) {
        return jdbc.sql("""
                SELECT id, name, color, is_active, created_at, updated_at
                FROM people
                WHERE id = :id
                """)
                .param("id", id)
                .query(this::map)
                .optional();
    }

    public Person insert(String name, String color) {
        return jdbc.sql("""
                INSERT INTO people (name, color)
                VALUES (:name, :color)
                RETURNING id, name, color, is_active, created_at, updated_at
                """)
                .param("name", name)
                .param("color", color)
                .query(this::map)
                .single();
    }

    public boolean softDelete(UUID id) {
        int updated = jdbc.sql("""
                UPDATE people
                SET is_active = FALSE, updated_at = NOW()
                WHERE id = :id AND is_active = TRUE
                """)
                .param("id", id)
                .update();
        return updated > 0;
    }

    private Person map(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new Person(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("color"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", java.time.OffsetDateTime.class),
                rs.getObject("updated_at", java.time.OffsetDateTime.class)
        );
    }
}
