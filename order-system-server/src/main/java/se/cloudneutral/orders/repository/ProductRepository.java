package se.cloudneutral.orders.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import se.cloudneutral.orders.domain.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    @Query(value = "select id from product order by random() limit :limit", nativeQuery = true)
    List<UUID> findRandomProductIDs(@Param("limit") int limit);
}
