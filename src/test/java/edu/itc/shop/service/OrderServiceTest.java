package edu.itc.shop.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import jakarta.persistence.EntityManagerFactory;
import edu.itc.shop.domain.*;
import edu.itc.shop.repository.*;
import edu.itc.shop.web.dto.OrderDto;

@SpringBootTest
@ActiveProfiles("test")
@Execution(ExecutionMode.SAME_THREAD)
class OrderServiceTest {

    @Autowired OrderService service;
    @Autowired OrderRepository orders;
    @Autowired ProductRepository products;
    @Autowired EntityManagerFactory emf;
    @Autowired TestData data;

    private Statistics stats() {
        return emf.unwrap(SessionFactory.class).getStatistics();
    }

    @Test
    void managedChangeIsFlushedWithoutSave() {
        Long id = data.newOrder();
        stats().clear();
        service.markPaid(id);
        assertThat(stats().getEntityUpdateCount()).isEqualTo(1);
        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void detachedChangeIsLostUntilSaveMerges() {
        Long id = data.newProduct();
        Product p = products.findById(id).orElseThrow();      // no transaction: detached at once
        p.setStock(99);
        assertThat(products.findById(id).orElseThrow().getStock()).isZero();   // nothing was written
        stats().clear();
        products.save(p);                                      // merge: 1 SELECT + 1 UPDATE
        assertThat(stats().getEntityLoadCount()).isEqualTo(1);
        assertThat(stats().getEntityUpdateCount()).isEqualTo(1);
        assertThat(products.findById(id).orElseThrow().getStock()).isEqualTo(99);
    }

    @Test
    void readOnlyTransactionNeverFlushesChanges() {
        Long id = data.newOrder();
        service.wronglyModifyInReadOnly(id);
        assertThat(orders.findById(id).orElseThrow().getStatus()).isEqualTo(OrderStatus.NEW);
    }

    @Test
    void viewReturnsDtoFromReadOnlyTransaction() {
        Long id = data.newOrder();
        OrderDto dto = service.view(id);
        assertThat(dto.lineCount()).isEqualTo(1);
        assertThat(dto.total().amount()).isEqualByComparingTo("20.00");
    }
}