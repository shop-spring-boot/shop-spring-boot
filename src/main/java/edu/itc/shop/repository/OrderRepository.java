package edu.itc.shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.itc.shop.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomer(String customer);
}