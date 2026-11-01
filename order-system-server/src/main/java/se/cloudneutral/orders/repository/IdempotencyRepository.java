package se.cloudneutral.orders.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import se.cloudneutral.orders.domain.IdempotencyRecord;

/**
 * Idempotency records keyed by (scope, idempotency_key). Written in portable SQL that runs on
 * PostgreSQL, which is the reference database.
 */
@Repository
public class IdempotencyRepository {
    private static final RowMapper<IdempotencyRecord> ROW_MAPPER = IdempotencyRepository::mapRow;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    public Optional<IdempotencyRecord> findByKey(String scope, String key) {
        List<IdempotencyRecord> records = jdbcTemplate.query("""
                        select scope,
                               idempotency_key,
                               fingerprint,
                               resource_id,
                               created_at,
                               expires_at
                        from idempotency_record
                        where scope = :scope
                          and idempotency_key = :key
                        """,
                new MapSqlParameterSource()
                        .addValue("scope", scope)
                        .addValue("key", key),
                ROW_MAPPER);
        return records.stream().findFirst();
    }

    /**
     * Attempts to claim the key for the given resource. A record that has expired but not yet
     * been evicted is treated as absent and recycled in place.
     *
     * @return the resource id if the key was claimed (new or recycled), or empty if a live record
     * already exists
     */
    public Optional<UUID> acquire(String scope,
                                  String key,
                                  byte[] fingerprint,
                                  UUID resourceId,
                                  Instant expiresAt) {
        List<UUID> ids = jdbcTemplate.queryForList("""
                        insert into idempotency_record (
                            scope,
                            idempotency_key,
                            fingerprint,
                            resource_id,
                            expires_at
                        )
                        values (
                            :scope,
                            :key,
                            :fingerprint,
                            :resourceId,
                            :expiresAt
                        )
                        on conflict (scope, idempotency_key) do update
                        set fingerprint = excluded.fingerprint,
                            resource_id = excluded.resource_id,
                            created_at  = now(),
                            expires_at  = excluded.expires_at
                        where idempotency_record.expires_at < now()
                        returning resource_id
                        """,
                new MapSqlParameterSource()
                        .addValue("scope", scope)
                        .addValue("key", key)
                        .addValue("fingerprint", fingerprint)
                        .addValue("resourceId", resourceId)
                        .addValue("expiresAt", OffsetDateTime.ofInstant(expiresAt, ZoneOffset.UTC)),
                UUID.class);
        return ids.stream().findFirst();
    }

    /**
     * Deletes at most {@code batchSize} expired records in a transaction of their own. Rows are
     * locked with {@code skip locked}, so concurrent evictors on other instances take disjoint
     * batches without any coordination and never block on rows held by live requests. Runs at
     * read committed since it has no need for serializable semantics.
     *
     * @return the number of records deleted
     */
    public int deleteExpired(int batchSize) {
        return jdbcTemplate.update("""
                        delete from idempotency_record
                        where (scope, idempotency_key) in (
                            select scope, idempotency_key
                            from idempotency_record
                            where expires_at < now()
                            order by expires_at
                            limit :batchSize
                            for update skip locked
                        )
                        """,
                new MapSqlParameterSource("batchSize", batchSize));
    }

    private static IdempotencyRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
        OffsetDateTime createdAt = rs.getObject("created_at", OffsetDateTime.class);
        OffsetDateTime expiresAt = rs.getObject("expires_at", OffsetDateTime.class);
        return new IdempotencyRecord(
                rs.getString("scope"),
                rs.getString("idempotency_key"),
                rs.getBytes("fingerprint"),
                rs.getObject("resource_id", UUID.class),
                createdAt != null ? createdAt.toInstant() : null,
                expiresAt != null ? expiresAt.toInstant() : null
        );
    }
}
