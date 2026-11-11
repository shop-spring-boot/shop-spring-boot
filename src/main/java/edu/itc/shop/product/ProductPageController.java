package edu.itc.shop.product;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import edu.itc.shop.config.ShopProperties;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/products")
public class ProductPageController {

    private static final Logger log = LoggerFactory.getLogger(ProductPageController.class);

    private final ProductService service;
    private final ShopProperties props;

    public ProductPageController(ProductService service, ShopProperties props) {
        this.service = service;
        this.props = props;
    }

    // GET /products  (list + search)
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q, Model model) {
        List<ProductRow> rows = service.search(q.isBlank() ? null : q).stream()
                .map(d -> ProductRow.of(d, props.taxRate(), props.lowStockThreshold()))
                .toList();
        model.addAttribute("products", rows);
        model.addAttribute("q", q);
        model.addAttribute("currency", props.currency());
        model.addAttribute("threshold", props.lowStockThreshold());
        return "products/list";
    }

    // GET /products/new  (empty form)
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("productForm", new ProductForm());
        return "products/form";
    }

    // POST /products  (save)
    @PostMapping
    public String create(@Valid @ModelAttribute("productForm") ProductForm form,
                         BindingResult result,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            log.warn("validation failed for new product: {} error(s)", result.getErrorCount());
            return "products/form";            // show the form again with errors
        }
        ProductDto created = service.create(
                new CreateProduct(form.getName(), form.getPrice(), form.getStock()));
        log.info("created product {} '{}'", created.id(), created.name());
        redirect.addFlashAttribute("message", "Product saved: " + form.getName());
        return "redirect:/products";           // POST-redirect-GET
    }
}