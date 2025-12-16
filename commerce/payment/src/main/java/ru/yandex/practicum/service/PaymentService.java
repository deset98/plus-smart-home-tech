package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;

import java.math.BigDecimal;
import java.util.UUID;

public interface PaymentService {

    PaymentDto payment(OrderDto order);

    OrderDto totalCost(OrderDto order);

    void refund(UUID paymentId);

    BigDecimal productCost(OrderDto order);

    void failed(UUID paymentId);
}