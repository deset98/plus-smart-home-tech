package ru.yandex.practicum.service;

import ru.yandex.practicum.dto.DeliveryDto;
import ru.yandex.practicum.dto.OrderDto;

import java.math.BigDecimal;
import java.util.UUID;

public interface DeliveryService {
    DeliveryDto delivery(DeliveryDto delivery);

    void successful(UUID deliveryId);

    void picked(UUID deliveryId);

    void failed(UUID deliveryId);

    BigDecimal cost(OrderDto order);
}