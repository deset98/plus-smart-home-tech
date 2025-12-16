package ru.yandex.practicum.exception;

public class NoDeliveryFoundException extends RuntimeException {

    public NoDeliveryFoundException() {
        super();
    }

    public NoDeliveryFoundException(String message) {
        super(message);
    }

    public NoDeliveryFoundException(String message, Object... args) {
        super(String.format(message.replace("{}", "%s"), args));
    }
}