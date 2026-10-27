package ar.com.anepanet.planificator.config;

public final class LocationCatalogSchema {

    public static final String UNIQUE_NAME_INDEX = """
            CREATE UNIQUE INDEX IF NOT EXISTS ux_locations_name_lower
            ON locations (lower(btrim(name)))
            """;

    public static final String SHIFT_WEEK_VIEW = """
            CREATE OR REPLACE VIEW v_shifts_week AS
            SELECT s.id,
                   s.user_id,
                   u.display_name                                                     AS user_name,
                   u.color                                                            AS user_color,
                   s.location_id,
                   l.name                                                             AS location_name,
                   l.color                                                            AS location_color,
                   l.color_soft                                                       AS location_color_soft,
                   s.work_date,
                   (date_trunc('week', s.work_date::timestamp))::date                 AS week_start,
                   ((EXTRACT(isodow FROM s.work_date))::smallint - 1)                 AS day_index,
                   s.start_time,
                   s.end_time,
                   round(EXTRACT(epoch FROM (s.end_time - s.start_time)) / 3600.0, 2) AS hours,
                   s.notes
            FROM shifts s
                     JOIN app_users u ON u.id = s.user_id
                     JOIN locations l ON l.id = s.location_id
            WHERE u.is_active
            """;

    private LocationCatalogSchema() {}

    public static boolean shouldCreateNameIndex(int duplicateNameGroups) {
        return duplicateNameGroups == 0;
    }
}
