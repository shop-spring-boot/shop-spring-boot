package edu.itc.shop.common;

import java.net.URI;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import edu.itc.shop.product.InsufficientStockException;
import edu.itc.shop.product.ProductNotFoundException;
import edu.itc.shop.stock.OutOfStockException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Validation failed"
        );
        problem.setTitle("Bad Request");

        List<String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .toList();
        problem.setProperty("errors", fieldErrors);
        return problem;
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ProblemDetail handleNotFound(ProductNotFoundException ex, WebRequest req) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Not Found");
        if (req instanceof ServletWebRequest swr) {
            problem.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }
        return problem;
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail handleInsufficientStock(InsufficientStockException ex, WebRequest req) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        if (req instanceof ServletWebRequest swr) {
            problem.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }
        return problem;
    }

    @ExceptionHandler(OutOfStockException.class)
    public ProblemDetail handleOutOfStock(OutOfStockException ex, WebRequest req) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        if (req instanceof ServletWebRequest swr) {
            problem.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }
        return problem;
    }

    @ExceptionHandler(DuplicateKeyException.class)
    public ProblemDetail handleDuplicateKey(DuplicateKeyException ex, WebRequest req) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflict");
        if (req instanceof ServletWebRequest swr) {
            problem.setInstance(URI.create(swr.getRequest().getRequestURI()));
        }
        return problem;
    }

    // ---- Order feature (challenge) ----

    @ExceptionHandler({edu.itc.shop.order.InsufficientStockException.class,
                       edu.itc.shop.order.OrderNotCancellableException.class})
    public ProblemDetail handleOrderConflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(edu.itc.shop.order.OrderNotFoundException.class)
    public ProblemDetail handleOrderNotFound(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadArgument(IllegalArgumentException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Invalid reference: unknown customer or product");
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleParamValidation(HandlerMethodValidationException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request parameters");
    }

    // ---- catch-all: must stay last ----

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericFallback(Exception ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "An unexpected error occurred."
        );
        problem.setTitle("Internal Server Error");
        return problem;
    }
}