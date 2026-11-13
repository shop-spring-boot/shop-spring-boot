package edu.itc.shop.order;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Order;
import edu.itc.shop.domain.OrderStatus;

@Service
public class ReportService {

    private final OrderRepository orders;

    public ReportService(OrderRepository orders) {
        this.orders = orders;
    }

    // The naive method (causes N+1)
    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrder() {
        List<Order> paidOrders = orders.findAllByStatus(OrderStatus.PAID); 
        
        Map<Long, Integer> report = new HashMap<>();
        for (Order o : paidOrders) {
            report.put(o.getId(), o.getLines().size()); 
        }
        return report;
    }

    // The optimized method (fixes N+1)
    @Transactional(readOnly = true)
    public Map<Long, Integer> linesPerPaidOrderFast() {
        List<Order> paidOrders = orders.findAllByStatusWithLines(OrderStatus.PAID); 
        
        Map<Long, Integer> report = new HashMap<>();
        for (Order o : paidOrders) {
            report.put(o.getId(), o.getLines().size()); 
        }
        return report;
    }
}