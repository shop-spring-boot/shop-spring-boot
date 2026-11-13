package edu.itc.shop.order;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import edu.itc.shop.support.QueryCounter;
import edu.itc.shop.support.TestData;

@SpringBootTest
@Execution(ExecutionMode.SAME_THREAD)
class NPlusOneTest {

    @Autowired ReportService report;
    @Autowired QueryCounter counter;
    @Autowired TestData data;

    @BeforeEach
    void seed() { data.paidOrders(50, 3); }

    @Test @Tag("demo")
    void naiveImplementationRunsNPlusOneQueries() {
        counter.reset();
        report.linesPerPaidOrder();
        // 1 query to find the 50 orders + 50 individual queries to find their lines = 51 queries
        assertThat(counter.count()).isEqualTo(51); 
    }
    @Test 
    void fastImplementationRunsOneQuery() {
        counter.reset();
        report.linesPerPaidOrderFast();
        // 1 single query fetches all 50 orders and all their lines at the same time
        assertThat(counter.count()).isEqualTo(1); 
}
}
