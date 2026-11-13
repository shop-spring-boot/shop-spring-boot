package edu.itc.shop.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import edu.itc.shop.domain.Tag;
import edu.itc.shop.product.TagRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FlushOrderTest {

    @Autowired TagRepository tags;

    @Test
    void insertRunsBeforeDeleteSoUniqueConstraintFails() {
        Tag sale = tags.saveAndFlush(new Tag("sale"));
        tags.delete(sale);
        tags.save(new Tag("sale"));
        assertThatThrownBy(() -> tags.flush()).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void flushBetweenDeleteAndInsertFixesIt() {
        Tag sale = tags.saveAndFlush(new Tag("sale"));
        tags.delete(sale);
        tags.flush();                                  // DELETE runs now
        tags.save(new Tag("sale"));
        tags.flush();                                  // INSERT runs after
        assertThat(tags.findAll().stream().filter(t -> "sale".equals(t.getName())).count()).isEqualTo(1);
    }
}