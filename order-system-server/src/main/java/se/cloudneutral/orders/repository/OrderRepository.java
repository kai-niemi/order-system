package se.cloudneutral.orders.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import se.cloudneutral.orders.domain.PurchaseOrder;
import se.cloudneutral.orders.domain.ShipmentStatus;

@Repository
public interface OrderRepository extends JpaRepository<PurchaseOrder, UUID> {
    @Query("""
        select o.id
        from PurchaseOrder o
    """)
    Page<UUID> findOrderIds(Pageable pageable);

    @Query("""
            select o
            from PurchaseOrder o
            join fetch o.customer
            left join fetch o.orderItems oi
            left join fetch oi.product
            where o.id in :ids
            """)
    List<PurchaseOrder> findAllWithDetails(@Param("ids") Collection<UUID> ids);

    @Query(value = """
            from PurchaseOrder o
            join fetch o.customer c
            join fetch o.orderItems oi
            join fetch oi.product
            where o.id=:id
            """)
    Optional<PurchaseOrder> findOrderDetailsById(@Param("id") UUID id);

    @Query(value = """
            select sum(po.totalPrice) from PurchaseOrder po where po.status=:status
            """)
    BigDecimal sumOrderTotal(@Param("status") ShipmentStatus status);

    /**
     * Guarded status transition: the {@code and po.status=:preStatus} predicate makes this a
     * compare-and-set, which is the concurrency control for an entity carrying no {@code @Version}.
     *
     * @return the number of rows updated, zero if the order was absent or no longer in
     * {@code preStatus}
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(name = "updateOrderStatusById")
    int updateOrderStatusById(@Param("id") UUID id,
                              @Param("preStatus") ShipmentStatus preStatus,
                              @Param("postStatus") ShipmentStatus postStatus,
                              @Param("now") OffsetDateTime now);

    @Modifying
    @Query(value = "delete from purchase_order_item where 1=1", nativeQuery = true)
    void deleteAllOrderItems();
}
