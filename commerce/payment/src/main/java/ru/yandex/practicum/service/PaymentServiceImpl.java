package ru.yandex.practicum.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.OrderFeignClient;
import ru.yandex.practicum.client.ShoppingStoreFeignClient;
import ru.yandex.practicum.dto.OrderDto;
import ru.yandex.practicum.dto.PaymentDto;
import ru.yandex.practicum.dto.ProductDto;
import ru.yandex.practicum.enums.PaymentState;
import ru.yandex.practicum.exception.NoOrderFoundException;
import ru.yandex.practicum.exception.NotEnoughInfoInOrderToCalculateException;
import ru.yandex.practicum.mapper.PaymentMapper;
import ru.yandex.practicum.model.Payment;
import ru.yandex.practicum.repository.PaymentRepository;
import ru.yandex.practicum.util.PaymentConstants;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderFeignClient orderFeignClient;
    private final ShoppingStoreFeignClient shoppingStoreFeignClient;

    @Override
    @Transactional
    public PaymentDto payment(OrderDto order) {
        log.info("Формирование платежа для заказа: {}", order);

        checkOrder(order);

        OrderDto calculatedOrder = totalCost(order);

        BigDecimal vat = calculatedOrder.getProductPrice()
                .multiply(PaymentConstants.VAT_RATE);

        Payment payment = Payment.builder()
                .productsTotal(calculatedOrder.getProductPrice())
                .deliveryTotal(calculatedOrder.getDeliveryPrice())
                .feeTotal(vat)
                .totalPayment(calculatedOrder.getTotalPrice())
                .paymentState(PaymentState.PENDING)
                .orderId(calculatedOrder.getOrderId())
                .build();

        return paymentMapper.toDto(paymentRepository.save(payment));
    }

    @Override
    public OrderDto totalCost(OrderDto orderDto) {
        log.info("Расчёт полной стоимости заказа: {}", orderDto);

        checkOrder(orderDto);

        BigDecimal productCost = productCost(orderDto);
        BigDecimal deliveryCost =
                orderDto.getDeliveryPrice() != null ? orderDto.getDeliveryPrice() : calculateDeliveryPrice(orderDto);

        BigDecimal vat = productCost.multiply(PaymentConstants.VAT_RATE);
        BigDecimal total = productCost.add(deliveryCost).add(vat);

        orderDto.setProductPrice(productCost);
        orderDto.setDeliveryPrice(deliveryCost);
        orderDto.setTotalPrice(total);

        return orderDto;
    }

    @Override
    public BigDecimal productCost(OrderDto order) {
        log.info("Расчёт стоимости товаров: {}", order);

        Map<UUID, Long> products = order.getProducts();

        if (products == null || products.isEmpty()) {
            throw new NotEnoughInfoInOrderToCalculateException("В заказе нет товаров");
        }

        BigDecimal totalCost = BigDecimal.ZERO;

        for (Map.Entry<UUID, Long> entry : products.entrySet()) {
            UUID productId = entry.getKey();
            Long quantity = entry.getValue();

            ProductDto product;
            try {
                product = shoppingStoreFeignClient.getProduct(productId);
            } catch (FeignException.NotFound e) {
                throw new NotEnoughInfoInOrderToCalculateException(
                        "Товар с id %s не найден".formatted(productId)
                );
            }

            if (product.getPrice() == null) {
                throw new NotEnoughInfoInOrderToCalculateException(
                        "У товара %s не указана цена".formatted(productId)
                );
            }

            totalCost = totalCost.add(
                    product.getPrice().multiply(BigDecimal.valueOf(quantity))
            );
        }

        return totalCost;
    }

    @Override
    @Transactional
    public void refund(UUID paymentId) {
        log.info("Подтверждение успешной оплаты: {}", paymentId);

        Payment payment = paymentRepository.findPaymentByPaymentId(paymentId)
                .orElseThrow(() -> new NoOrderFoundException("Платёж не найден"));

        payment.setPaymentState(PaymentState.SUCCESS);
        orderFeignClient.payment(payment.getOrderId());
    }

    @Override
    @Transactional
    public void failed(UUID paymentId) {
        log.info("Отказ при оплате: {}", paymentId);

        Payment payment = paymentRepository.findPaymentByPaymentId(paymentId)
                .orElseThrow(() -> new NoOrderFoundException("Платёж не найден"));

        payment.setPaymentState(PaymentState.FAILED);
        orderFeignClient.paymentFailed(payment.getOrderId());
    }

    private BigDecimal calculateDeliveryPrice(OrderDto orderDto) {
        BigDecimal price = PaymentConstants.BASE_DELIVERY_PRICE;

        if (orderDto.getDeliveryWeight() != null &&
                orderDto.getDeliveryWeight()
                        .compareTo(PaymentConstants.HEAVY_WEIGHT_LIMIT) > 0) {
            price = price.add(PaymentConstants.HEAVY_DELIVERY_EXTRA);
        }

        if (Boolean.TRUE.equals(orderDto.getFragile())) {
            price = price.add(PaymentConstants.FRAGILE_DELIVERY_EXTRA);
        }

        return price;
    }

    private void checkOrder(OrderDto orderDto) {
        if (orderDto == null ||
                orderDto.getOrderId() == null ||
                orderDto.getProducts() == null ||
                orderDto.getProducts().isEmpty()) {
            throw new NotEnoughInfoInOrderToCalculateException(
                    "Недостаточно данных для расчёта заказа"
            );
        }
    }
}