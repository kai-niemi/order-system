package se.cloudneutral.orders.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import se.cloudneutral.orders.domain.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID>,
        PagingAndSortingRepository<Customer, UUID> {
    @Query(value = "select id from customer order by random() limit :limit", nativeQuery = true)
    List<UUID> findRandom(@Param("limit") int limit);
}
