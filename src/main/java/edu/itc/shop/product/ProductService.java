package edu.itc.shop.product;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class ProductService {

    private final ProductRepository repo;

    public ProductService(ProductRepository repo) {
        this.repo = repo;
    }

    // Existing team method kept intact
    @Transactional
    public void restock(long fromId, long toId, int qty) {
        if (repo.updateStock(fromId, -qty) == 0) {
            throw new IllegalStateException("Cannot take " + qty + " from product " + fromId);
        }
        if (repo.updateStock(toId, qty) == 0) {
            throw new IllegalStateException("Target product " + toId + " not found");
        }
    }

    // --- NEW METHODS FOR TASK 4 REST API ---

    public List<ProductDto> search(String q) {
        Iterable<Product> products = (q == null || q.isBlank()) 
            ? repo.findAll() 
            : repo.findByNameContaining(q);
            
        List<ProductDto> result = new ArrayList<>();
        products.forEach(p -> result.add(ProductDto.from(p)));
        return result;
    }

    public ProductDto findById(long id) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
        return ProductDto.from(p);
    }

    @Transactional
    public ProductDto create(CreateProduct cmd) {
        Product p = new Product(null, cmd.name(), cmd.price(), cmd.stock());
        
        // Capture the returned long ID
        long generatedId = repo.insert(p);
        
        // Reconstruct the saved product with the new ID
        Product saved = new Product(generatedId, cmd.name(), cmd.price(), cmd.stock());
        return ProductDto.from(saved);
    }

    @Transactional
    public ProductDto updateStock(long id, int delta) {
        Product p = repo.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        // Check if removing stock would cause it to go below zero
        if (p.stock() + delta < 0) {
            throw new InsufficientStockException(id, p.stock(), delta);
        }

        // Reuse the updateStock method from the repository
        repo.updateStock(id, delta);
        
        // Fetch the newly updated product to return
        Product updated = repo.findById(id).orElseThrow();
        return ProductDto.from(updated);
    }

    @Transactional
    public void delete(long id) {
        // Fetch first to ensure it throws a 404 (ProductNotFoundException) if missing.
        // We don't need to assign it to a variable, which fixes the "never read" warning.
        repo.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
        
        // Change from deleteById to delete
        repo.delete(id); 
    }
}