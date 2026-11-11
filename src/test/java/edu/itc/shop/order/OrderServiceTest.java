package edu.itc.shop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import edu.itc.shop.config.ShopProperties;
import edu.itc.shop.product.Product;
import edu.itc.shop.product.ProductRepository;
import edu.itc.shop.notification.Notifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

    private final OrderRepository orders = mock(OrderRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final Notifier notifier = mock(Notifier.class);

    private OrderService serviceWithTax(String taxRate) {
        return new OrderService(orders, products,
                new ShopProperties("USD", new BigDecimal(taxRate), 20), notifier);
    }

    private PlaceOrder.Line line(long productId, int qty) {
        return new PlaceOrder.Line(productId, qty);
    }

    private Optional<Product> product(long id, String price) {
        return Optional.of(new Product(id, "p" + id, new BigDecimal(price), 100));
    }

    @Test
    void placeComputesTotalWithTax() {
        when(products.updateStock(anyLong(), anyInt())).thenReturn(1);
        when(products.findById(1L)).thenReturn(product(1, "49.99"));
        when(products.findById(3L)).thenReturn(product(3, "10.00"));
        when(orders.insert(any(), any())).thenReturn(7L);

        OrderDto dto = serviceWithTax("0.10")
                .place(new PlaceOrder(1L, List.of(line(1, 2), line(3, 1))));

        assertThat(dto.id()).isEqualTo(7);
        assertThat(dto.status()).isEqualTo(OrderStatus.NEW);
        assertThat(dto.total()).isEqualByComparingTo("120.98");
        assertThat(dto.lines()).hasSize(2);
    }

    @Test
    void placeRoundsHalfUp() {
        when(products.updateStock(anyLong(), anyInt())).thenReturn(1);
        when(products.findById(1L)).thenReturn(product(1, "0.03"));
        when(orders.insert(any(), any())).thenReturn(1L);

        OrderDto dto = serviceWithTax("0.50")
                .place(new PlaceOrder(1L, List.of(line(1, 1))));

        assertThat(dto.total()).isEqualByComparingTo("0.05");
    }

    @Test
    void placeRejectsDuplicateProducts() {
        assertThatThrownBy(() -> serviceWithTax("0.10")
                .place(new PlaceOrder(1L, List.of(line(1, 1), line(1, 2)))))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(products, orders);
    }

    @Test
    void placeFailsWhenStockIsInsufficient() {
        when(products.updateStock(1L, -1)).thenReturn(1);
        when(products.updateStock(2L, -999)).thenReturn(0);
        when(products.findById(1L)).thenReturn(product(1, "49.99"));

        assertThatThrownBy(() -> serviceWithTax("0.10")
                .place(new PlaceOrder(1L, List.of(line(1, 1), line(2, 999)))))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessage("Insufficient stock for product 2");

        verify(orders, never()).insert(any(), any());
    }

    @Test
    void cancelRestoresStockAndNotifies() {
        Order order = new Order(5L, 1, OrderStatus.NEW, LocalDateTime.now(), new BigDecimal("120.98"));
        when(orders.findById(5L)).thenReturn(Optional.of(order));
        when(orders.findLines(5L)).thenReturn(List.of(
                new OrderLine(1, 2, new BigDecimal("49.99")),
                new OrderLine(3, 1, new BigDecimal("10.00"))));

        OrderDto dto = serviceWithTax("0.10").cancel(5L);

        assertThat(dto.status()).isEqualTo(OrderStatus.CANCELLED);
        verify(products).updateStock(1L, 2);
        verify(products).updateStock(3L, 1);
        verify(orders).updateStatus(5L, OrderStatus.CANCELLED);
        verify(notifier).send(anyString(), anyString());
    }

    @Test
    void cancelRefusesOrdersThatAreNotNew() {
        Order order = new Order(5L, 1, OrderStatus.PAID, LocalDateTime.now(), new BigDecimal("10.00"));
        when(orders.findById(5L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> serviceWithTax("0.10").cancel(5L))
                .isInstanceOf(OrderNotCancellableException.class);

        verifyNoInteractions(products, notifier);
        verify(orders, never()).updateStatus(anyLong(), any());
    }
}