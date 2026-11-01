package se.cloudneutral.orders.util;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

public abstract class MetadataUtils {
    private MetadataUtils() {
    }

    public static String databaseIsolation(DataSource dataSource) {
        try {
            return new JdbcTemplate(dataSource)
                    .queryForObject("SHOW transaction_isolation", String.class);
        } catch (DataAccessException e) {
            return "unknown";
        }
    }
}
