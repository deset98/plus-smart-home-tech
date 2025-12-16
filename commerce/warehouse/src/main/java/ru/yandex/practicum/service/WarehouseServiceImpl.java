package ru.yandex.practicum.service;

import jakarta.ws.rs.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.dto.AddressDto;
import ru.yandex.practicum.dto.BookedProductsDto;
import ru.yandex.practicum.dto.ShoppingCartDto;
import ru.yandex.practicum.exception.NoSpecifiedProductInWarehouseException;
import ru.yandex.practicum.exception.ProductInShoppingCartLowQuantityInWarehouse;
import ru.yandex.practicum.exception.ProductNotFoundException;
import ru.yandex.practicum.exception.SpecifiedProductAlreadyInWarehouseException;
import ru.yandex.practicum.mapper.WarehouseMapper;
import ru.yandex.practicum.model.Address;
import ru.yandex.practicum.model.OrderBooking;
import ru.yandex.practicum.model.WarehouseProduct;
import ru.yandex.practicum.repository.BookingRepository;
import ru.yandex.practicum.repository.WarehouseRepository;
import ru.yandex.practicum.request.AddProductToWarehouseRequest;
import ru.yandex.practicum.request.AssemblyProductsForOrderRequest;
import ru.yandex.practicum.request.NewProductInWarehouseRequest;
import ru.yandex.practicum.request.ShippedToDeliveryRequest;

import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseMapper warehouseMapper;
    private final BookingRepository bookingRepository;

    @Override
    @Transactional
    public void newProductInWarehouse(NewProductInWarehouseRequest request) {
        if (warehouseRepository.existsById(request.getProductId())) {
            throw new SpecifiedProductAlreadyInWarehouseException("На складе уже есть Product с id",
                    request.getProductId());
        }
        warehouseRepository.save(warehouseMapper.toEntity(request));
    }

    @Override
    public BookedProductsDto checkProductQuantityEnoughForShoppingCart(ShoppingCartDto cart) {
        validateProductQuantities(cart.getProducts());

        double weight = 0;
        double volume = 0;
        boolean fragile = false;

        Map<UUID, Long> cartProducts = cart.getProducts();
        Map<UUID, WarehouseProduct> warehouseProducts = warehouseRepository.findAllById(cartProducts.keySet())
                .stream()
                .collect(Collectors.toMap(WarehouseProduct::getProductId, Function.identity()));

        for (Map.Entry<UUID, Long> cartProduct : cartProducts.entrySet()) {
            WarehouseProduct warehouseProduct = warehouseProducts.get(cartProduct.getKey());
            double productVolume = warehouseProduct.getDimension().getHeight() *
                    warehouseProduct.getDimension().getDepth() *
                    warehouseProduct.getDimension().getWidth();

            volume += productVolume * cartProduct.getValue();
            weight += warehouseProduct.getWeight() * cartProduct.getValue();

            if (warehouseProduct.getFragile()) {
                fragile = true;
            }
        }

        return BookedProductsDto.builder()
                .deliveryVolume(volume)
                .deliveryWeight(weight)
                .fragile(fragile)
                .build();
    }

    @Override
    @Transactional
    public void addProductToWarehouse(AddProductToWarehouseRequest request) {
        WarehouseProduct warehouseProduct = warehouseRepository.findById(request.getProductId())
                .orElseThrow(() ->
                        new NoSpecifiedProductInWarehouseException("Нет информации о Product, id:",
                                request.getProductId()));
        Long quantity = warehouseProduct.getQuantity();
        if (quantity == null) {
            quantity = 0L;
        }

        warehouseProduct.setQuantity(quantity + request.getQuantity());
        warehouseRepository.save(warehouseProduct);
        log.info("Количество Product на складе: {}", warehouseProduct.getQuantity());
    }

    @Override
    public AddressDto getWarehouseAddress() {
        String address = new Address().getAddress();
        return AddressDto.builder()
                .country(address)
                .city(address)
                .street(address)
                .house(address)
                .flat(address)
                .build();
    }

    private void validateProductQuantities(Map<UUID, Long> cartProducts) {
        Map<UUID, WarehouseProduct> products = warehouseRepository.findAllById(cartProducts.keySet())
                .stream()
                .collect(Collectors.toMap(WarehouseProduct::getProductId, Function.identity()));

        for (Map.Entry<UUID, Long> cartProduct : cartProducts.entrySet()) {
            WarehouseProduct warehouseProduct = products.get(cartProduct.getKey());
            if (warehouseProduct == null) {
                throw new ProductNotFoundException("Отсутствует Product с id: ", cartProduct.getKey());
            }

            if (cartProduct.getValue() > warehouseProduct.getQuantity()) {
                throw new ProductInShoppingCartLowQuantityInWarehouse("На складе не хватает Product с id: ",
                        warehouseProduct.getProductId());
            }
        }
    }

    @Override
    public void shippedToDelivery(ShippedToDeliveryRequest request) {
        log.info("Передача товаров в доставку: {}", request);

        UUID orderId = request.getOrderId();
        OrderBooking booking = bookingRepository.findBookingByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("Бронь не найдена."));

        booking.setDeliveryId(request.getDeliveryId());
        bookingRepository.save(booking);
    }


    @Override
    public void acceptReturn(Map<UUID, Long> products) {
        log.info("Возврат товаров на склад, список: {}", products);

        products.forEach((id, quantity) -> {
            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Некорректное количество товара " + id + ": " + quantity);
            }

            addProductToWarehouse(
                    AddProductToWarehouseRequest.builder()
                            .productId(id)
                            .quantity(quantity)
                            .build()
            );
        });
    }

    @Override
    @Transactional
    public BookedProductsDto assemblyProductsForOrder(AssemblyProductsForOrderRequest request) {
        log.info("сбор товаров к заказу, request: {}", request);

        UUID orderId = request.getOrderId();
        Map<UUID, Long> productsForBooking = request.getProducts();

        double totalWeight = 0.0;
        double totalVolume = 0.0;
        boolean fragile = false;

        Map<UUID, WarehouseProduct> products = warehouseRepository
                .findAllById(productsForBooking.keySet())
                .stream()
                .collect(Collectors.toMap(WarehouseProduct::getProductId, Function.identity()));

        for (Map.Entry<UUID, Long> entry : productsForBooking.entrySet()) {
            UUID id = entry.getKey();
            long quantity = entry.getValue();

            WarehouseProduct product = products.get(id);
            if (product == null) {
                throw new NoSpecifiedProductInWarehouseException("Товар не найден на складе", id);
            }

            if (quantity <= 0) {
                throw new IllegalArgumentException("Некорректное количество товара " + id + ": " + quantity);
            }

            if (quantity > product.getQuantity()) {
                throw new ProductInShoppingCartLowQuantityInWarehouse("Недостаточно товара %s на складе", id);
            }

            product.setQuantity(product.getQuantity() - quantity);
            var dim = product.getDimension();
            double productVolume = dim.getHeight() * dim.getDepth() * dim.getWidth();

            totalVolume += productVolume * quantity;
            totalWeight += product.getWeight() * quantity;

            if (Boolean.TRUE.equals(product.getFragile())) {
                fragile = true;
            }
        }

        warehouseRepository.saveAll(products.values());
        bookingRepository.save(OrderBooking.builder()
                .orderId(orderId)
                .products(productsForBooking)
                .build());

        return BookedProductsDto.builder()
                .deliveryVolume(totalVolume)
                .deliveryWeight(totalWeight)
                .fragile(fragile)
                .build();
    }
}