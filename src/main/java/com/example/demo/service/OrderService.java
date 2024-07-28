package com.example.demo.service;

import com.example.demo.domain.Inventory;
import com.example.demo.domain.Order;
import com.example.demo.repository.InventoryRepository;
import com.example.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final InventoryRepository inventoryRepository;
    private final SimpMessagingTemplate template;
    
    public void processOrder(Order order) {
        log.info("Постановка задачи на обработку заказа: {}", order);
        CompletableFuture.runAsync(() -> {
            log.info("[productId = {}] Старт обработки заказа: {}", order.getProduct(), order);
            Inventory inventory = inventoryRepository.findByProduct(order.getProduct()).orElseThrow(() -> {
                log.error("Не нашли продукт на складе {}", order.getProduct());
                updateOrderStatus(order, "Нет на складе");
                return new RuntimeException("Product not found");
            });

            log.info("[productId = {}] На складе продукт: {}", order.getProduct(), inventory);

            AtomicInteger quantity = new AtomicInteger(inventory.getQuantity());

            while (true) {
                int currentQuantity = quantity.get();
                log.info("[productId = {}] Забираем товар {} со склада", order.getProduct(), inventory);
                if (currentQuantity < order.getQuantity()) {
                    log.info("[productId = {}] Недостаточное количество товара на складе: {}", order.getProduct(), currentQuantity);
                    updateOrderStatus(order, "Нет на складе");
                    return;
                }
                if (quantity.compareAndSet(currentQuantity, currentQuantity - order.getQuantity())) {
                    log.info("[productId = {}] Успешно взяли товар со склада: {}",
                            order.getProduct(),
                            quantity.get());
                    inventory.setQuantity(quantity.get());
                    inventoryRepository.save(inventory);
                    template.convertAndSend("/topic/inventories", inventory);
                    break;
                }
            }

            updateOrderStatus(order, "Куплен");

            sendNotification(order);
        }, Executors.newFixedThreadPool(10));
    }

    private void sendNotification(Order order) {
        notificationService.queueOrder(order);
    }

    private void updateOrderStatus(Order order, String status) {
        log.info("[productId = {}] Обновляю статус на {}", order.getProduct(), status);
        order.setStatus(status);
        orderRepository.save(order);
        template.convertAndSend("/topic/orders", order); // Отправить обновление через WebSocket
    }
}
