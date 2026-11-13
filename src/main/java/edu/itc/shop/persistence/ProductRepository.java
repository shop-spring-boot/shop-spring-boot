package edu.itc.shop.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;

import edu.itc.shop.domain.Product;
import edu.itc.shop.reporting.ProductReportRepository;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long>, ProductReportRepository {

    Optional<Product> findBySku(String sku);

    long countBySkuStartingWith(String skuPrefix);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findForUpdate(@Param("id") Long id);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Product p set p.stock = p.stock - :qty where p.id = :id and p.stock >= :qty")
    int reserveIfAvailable(@Param("id") Long id, @Param("qty") int qty);
}
