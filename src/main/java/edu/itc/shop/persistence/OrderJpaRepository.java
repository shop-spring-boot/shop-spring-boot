package edu.itc.shop.persistence;

import java.time.Instant;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import edu.itc.shop.domain.Order;
import edu.itc.shop.orderfeed.OrderSummary;

public interface OrderJpaRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    @Query("""
            select new edu.itc.shop.orderfeed.OrderSummary(o.id, o.placedAt, o.status)
            from Order o
            """)
    Slice<OrderSummary> findAllSummaries(Pageable pageable);

    @Query("""
            select new edu.itc.shop.orderfeed.OrderSummary(o.id, o.placedAt, o.status)
            from Order o
            where o.placedAt > :placedAt
               or (o.placedAt = :placedAt and o.id > :id)
            """)
    Slice<OrderSummary> findSummariesAfter(
            @Param("placedAt") Instant placedAt,
            @Param("id") Long id,
            Pageable pageable);
}
