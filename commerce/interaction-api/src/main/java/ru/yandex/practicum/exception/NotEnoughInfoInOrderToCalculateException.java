package ru.yandex.practicum.exception;

public class NotEnoughInfoInOrderToCalculateException extends RuntimeException {

    public NotEnoughInfoInOrderToCalculateException(String message) {
        super(message);
    }

    public NotEnoughInfoInOrderToCalculateException(String message, Object... args) {
        super(String.format(message.replace("{}", "%s"), args));
    }
}