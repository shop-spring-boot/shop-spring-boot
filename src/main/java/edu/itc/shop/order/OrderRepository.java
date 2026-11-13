// order/OrderRepository.java
package edu.itc.shop.order;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {
    // List<Order> findByCustomerEmailAndStatusOrderByPlacedAtDesc(String email, OrderStatus status);
    // List<Order> findByCustomerEmailAndStatusOrderByPlacedAtDesc(String email, OrderStatus status);
    List<Order> findByCustomerEmaliAndStatusOrderByPlacedAtDesc(String email, OrderStatus status);
    
    long countByStatus(OrderStatus status);
    Optional<Order> findFirstByCustomerIdOrderByPlacedAtDesc(Long customerId);
}