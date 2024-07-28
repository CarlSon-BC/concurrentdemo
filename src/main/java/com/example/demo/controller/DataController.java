package com.example.demo.controller;

import com.example.demo.domain.Inventory;
import com.example.demo.domain.Order;
import com.example.demo.repository.InventoryRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.service.NotificationService;
import com.example.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequiredArgsConstructor
public class DataController {

    private final OrderService orderService;
    private final NotificationService notificationService;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate template;

    private static final List<String> PRODUCTS = Arrays.asList(
            "Apples", "Bananas", "Oranges", "Grapes", "Mangoes",
            "Pineapples", "Strawberries", "Blueberries", "Peaches", "Cherries"
    );

    @GetMapping("/spam")
    public String spamOrders(@RequestParam int count) {
        // Генерируем указанное количество случайных заказов
        for (int i = 0; i < count; i++) {
            String randomProduct = PRODUCTS.get(ThreadLocalRandom.current().nextInt(PRODUCTS.size()));
            int randomQuantity = ThreadLocalRandom.current().nextInt(1, 11);
            String email = "user" + i + "@example.com";
            Order order = new Order(email, randomProduct, randomQuantity, "Создан");
            order = orderRepository.save(order);
            template.convertAndSend("/topic/orders", order);
            orderService.processOrder(order);
        }
        return "Generated " + count + " random orders successfully";
    }

    @GetMapping("/reset")
    public String resetData() {
        // Удаляем все заказы и данные инвентаря
        orderRepository.deleteAll();
        inventoryRepository.deleteAll();
        notificationService.resetCounter();

        // Добавляем дефолтные данные инвентаря
        inventoryRepository.save(new Inventory("Apples", 1000));
        inventoryRepository.save(new Inventory("Bananas", 1300));
        inventoryRepository.save(new Inventory("Oranges", 900));
        inventoryRepository.save(new Inventory("Grapes", 1500));
        inventoryRepository.save(new Inventory("Mangoes", 2000));
        inventoryRepository.save(new Inventory("Pineapples", 200));
        inventoryRepository.save(new Inventory("Strawberries", 14));
        inventoryRepository.save(new Inventory("Blueberries", 1100));
        inventoryRepository.save(new Inventory("Peaches", 1600));
        inventoryRepository.save(new Inventory("Cherries", 17000));

        // Отправляем обновления через WebSocket
        List<Inventory> inventories = inventoryRepository.findAll();
        inventories.forEach(inventory -> template.convertAndSend("/topic/inventories", inventory));

        return "System reset to default state";
    }

    @GetMapping("/notification-count")
    public int getNotificationCount() {
        return notificationService.getNotificationCount();
    }
}
