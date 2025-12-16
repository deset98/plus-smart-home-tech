package ru.yandex.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.client.DeliveryFeignClient;
import ru.yandex.practicum.client.PaymentFeignClient;
import ru.yandex.practicum.client.ShoppingCartFeignClient;
import ru.yandex.practicum.client.WarehouseFeignClient;
import ru.yandex.practicum.dto.*;
import ru.yandex.practicum.enums.DeliveryState;
import ru.yandex.practicum.enums.OrderState;
import ru.yandex.practicum.exception.NoOrderFoundException;
import ru.yandex.practicum.mapper.OrderMapper;
import ru.yandex.practicum.model.Order;
import ru.yandex.practicum.repository.OrderRepository;
import ru.yandex.practicum.request.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.request.CreateNewOrderRequest;
import ru.yandex.practicum.request.ProductReturnRequest;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final ShoppingCartFeignClient shoppingCartFeignClient;
    private final WarehouseFeignClient warehouseFeignClient;
    private final DeliveryFeignClient deliveryFeignClient;
    private final PaymentFeignClient paymentFeignClient;

    @Override
    public Page<OrderDto> getOrdersByUser(String username, Pageable pageable) {
        ShoppingCartDto cart = shoppingCartFeignClient.getShoppingCart(username);
        return orderRepository.getAllOrdersByCartId(cart.getShoppingCartId(), pageable).map(orderMapper::toDto);
    }

    @Override
    @Transactional
    public OrderDto addOrder(CreateNewOrderRequest request) {
        log.info("Создание заказа: {}", request);

        BookedProductsDto bookedProducts =
                warehouseFeignClient.checkProductQuantityEnoughForShoppingCart(request.getShoppingCart());

        Order order = buildNewOrder(request, bookedProducts);
        orderRepository.save(order);

        DeliveryDto delivery = planDelivery(order, request.getDeliveryAddress());
        order.setDeliveryId(delivery.getDeliveryId());

        PaymentDto payment = paymentFeignClient.payment(orderMapper.toDto(order));
        applyPayment(order, payment);

        orderRepository.save(order);
        log.info("Заказ создан: {}", order);

        return orderMapper.toDto(order);
    }

    @Override
    @Transactional
    public OrderDto productReturn(ProductReturnRequest request) {
        Order order = getOrderById(request.getOrderId());

        warehouseFeignClient.acceptReturn(request.getProducts());
        order.setState(OrderState.PRODUCT_RETURNED);

        return orderMapper.toDto(orderRepository.save(order));
    }

    @Override
    @Transactional
    public OrderDto payment(UUID orderId) {
        return changeState(orderId, OrderState.PAID);
    }

    @Override
    @Transactional
    public OrderDto paymentFailed(UUID orderId) {
        return changeState(orderId, OrderState.PAYMENT_FAILED);
    }

    @Override
    @Transactional
    public OrderDto delivery(UUID orderId) {
        return changeState(orderId, OrderState.DELIVERED);
    }

    @Override
    @Transactional
    public OrderDto deliveryFailed(UUID orderId) {
        return changeState(orderId, OrderState.DELIVERY_FAILED);
    }

    @Override
    @Transactional
    public OrderDto assembly(UUID orderId) {
        return changeState(orderId, OrderState.ASSEMBLED);
    }

    @Override
    @Transactional
    public OrderDto assemblyFailed(UUID orderId) {
        return changeState(orderId, OrderState.ASSEMBLY_FAILED);
    }

    @Override
    @Transactional
    public OrderDto complete(UUID orderId) {
        return changeState(orderId, OrderState.COMPLETED);
    }

    @Override
    public OrderDto calculateTotal(UUID orderId) {
        Order order = getOrderById(orderId);
        return paymentFeignClient.totalCost(orderMapper.toDto(order));
    }

    @Override
    @Transactional
    public OrderDto calculateDelivery(UUID orderId) {
        Order order = getOrderById(orderId);

        BigDecimal deliveryPrice =
                deliveryFeignClient.cost(orderMapper.toDto(order));

        order.setDeliveryPrice(deliveryPrice);
        orderRepository.save(order);

        return orderMapper.toDto(order);
    }

    /* ======================= PRIVATE ======================= */

    private Order buildNewOrder(CreateNewOrderRequest request, BookedProductsDto booked) {
        return Order.builder()
                .cartId(request.getShoppingCart().getShoppingCartId())
                .products(request.getShoppingCart().getProducts())
                .state(OrderState.NEW)
                .deliveryWeight(BigDecimal.valueOf(booked.getDeliveryWeight()))
                .deliveryVolume(BigDecimal.valueOf(booked.getDeliveryVolume()))
                .fragile(booked.getFragile())
                .build();
    }

    private void applyPayment(Order order, PaymentDto payment) {
        order.setPaymentId(payment.getPaymentId());
        order.setTotalPrice(payment.getTotalPayment());
        order.setDeliveryPrice(payment.getDeliveryTotal());
    }

    private DeliveryDto planDelivery(Order order, AddressDto deliveryAddress) {
        AddressDto warehouseAddress = warehouseFeignClient.getWarehouseAddress();

        AssemblyProductsForOrderRequest request = AssemblyProductsForOrderRequest.builder()
                .orderId(order.getOrderId())
                .products(order.getProducts())
                .build();

        BookedProductsDto bookedProducts =
                warehouseFeignClient.assemblyProductsForOrder(request);

        DeliveryDto delivery = DeliveryDto.builder()
                .orderId(order.getOrderId())
                .fromAddress(warehouseAddress)
                .toAddress(deliveryAddress)
                .deliveryVolume(bookedProducts.getDeliveryVolume())
                .deliveryWeight(bookedProducts.getDeliveryWeight())
                .fragile(bookedProducts.getFragile())
                .deliveryState(DeliveryState.CREATED)
                .build();

        return deliveryFeignClient.delivery(delivery);
    }

    private OrderDto changeState(UUID orderId, OrderState state) {
        Order order = getOrderById(orderId);
        order.setState(state);
        return orderMapper.toDto(orderRepository.save(order));
    }

    private Order getOrderById(UUID orderId) {
        return orderRepository.findOrderByOrderId(orderId)
                .orElseThrow(() -> new NoOrderFoundException("Не найден заказ: " + orderId));
    }
}