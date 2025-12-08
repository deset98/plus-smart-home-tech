package ru.yandex.practicum.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(String message) {
        super(message);
    }

    public ProductNotFoundException(String message, Object... args) {
        super(String.format(message.replace("{}", "%s"), args));
    }
}