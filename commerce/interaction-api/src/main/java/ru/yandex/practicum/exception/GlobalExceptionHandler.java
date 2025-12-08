package ru.yandex.practicum.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(ProductNotFoundException.class)
    public ApiError handleProductNotFound(ProductNotFoundException ex) {
        log.warn("Product not found: {}", ex.getMessage());
        return ApiError.fromException(ex, HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NoSpecifiedProductInWarehouseException.class)
    public ApiError handleProductMissingInWarehouse(NoSpecifiedProductInWarehouseException ex) {
        log.warn("Product missing in warehouse: {}", ex.getMessage());
        return ApiError.fromException(ex, HttpStatus.NOT_FOUND);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(SpecifiedProductAlreadyInWarehouseException.class)
    public ApiError handleProductAlreadyInWarehouse(SpecifiedProductAlreadyInWarehouseException ex) {
        log.warn("Product already exists in warehouse: {}", ex.getMessage());
        return ApiError.fromException(ex, HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ProductInShoppingCartLowQuantityInWarehouse.class)
    public ApiError handleInsufficientQuantity(ProductInShoppingCartLowQuantityInWarehouse ex) {
        log.warn("Insufficient product quantity in warehouse: {}", ex.getMessage());
        return ApiError.fromException(ex, HttpStatus.BAD_REQUEST);
    }

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(Exception.class)
    public ApiError handleGenericException(Exception ex, WebRequest request) {
        log.error("Unhandled exception at {}: {}", request.getDescription(false), ex.getMessage(), ex);
        return ApiError.fromException(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
