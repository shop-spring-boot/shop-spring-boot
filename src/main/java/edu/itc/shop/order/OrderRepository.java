// order/OrderRepository.java
package edu.itc.shop.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.OrderStatus;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerEmailAndStatusOrderByPlacedAtDesc(String email, OrderStatus status);
    // List<Order> findByCustomerEmaliAndStatusOrderByPlacedAtDesc(String email, OrderStatus status);
    
    long countByStatus(OrderStatus status);
    Optional<Order> findFirstByCustomerIdOrderByPlacedAtDesc(Long customerId);

    @Query("""
        select distinct o from Order o
        join fetch o.lines l
        join fetch l.product
        where o.customer.email = :email
        order by o.placedAt desc
        """)
    List<Order> findWithLinesByCustomerEmail(@Param("email") String email);

    @Query("select count(l) from OrderLine l where l.order.status = :status")
    long countLinesByStatus(@Param("status") OrderStatus status);

    Page<OrderSummary> findByStatus(OrderStatus status, Pageable pageable);
}