package edu.itc.shop.product;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ProductService service;

    @Test
    void listProductsReturns200() throws Exception {
        when(service.search(null)).thenReturn(List.of(
            new ProductDto(1L, "USB-C cable", new BigDecimal("7.90"), 25)
        ));

        mvc.perform(get("/api/products"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].name").value("USB-C cable"));
    }

    @Test
    void getByIdNotFoundReturns404() throws Exception {
        when(service.findById(999L)).thenThrow(new ProductNotFoundException(999L));

        mvc.perform(get("/api/products/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value("Not Found"));
    }

    @Test
    void createProductReturns201WithLocation() throws Exception {
        ProductDto created = new ProductDto(6L, "USB-C cable", new BigDecimal("7.90"), 25);
        when(service.create(any(CreateProduct.class))).thenReturn(created);

        mvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"USB-C cable","price":7.90,"stock":25}
                """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/api/products/6"))
            .andExpect(jsonPath("$.id").value(6));
    }

    @Test
    void createWithInvalidBodyReturns400WithFieldErrors() throws Exception {
        mvc.perform(post("/api/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name": "", "price": 0, "stock": -1}
                """))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.errors.length()").value(3));
    }
}