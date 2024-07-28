package com.example.demo.service;

import com.example.demo.domain.Order;
import com.example.demo.repository.InventoryRepository;
import com.example.demo.repository.OrderRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {
    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final SimpMessagingTemplate template;

    private ExecutorService consumerExecutor;
    private final int BATCH_SIZE = 10;
    private final CyclicBarrier barrier = new CyclicBarrier(BATCH_SIZE);
    private final AtomicInteger notificationCounter = new AtomicInteger(0);
    private static final LinkedBlockingQueue<Order> notificationQueue = new LinkedBlockingQueue<>();

    @PostConstruct
    public void init() {
        consumerExecutor = Executors.newFixedThreadPool(10); 
        for (int i = 0; i < 10; i++) {
            consumerExecutor.submit(this::processNotifications);
        }
    }

    @PreDestroy
    public void cleanup() {
        consumerExecutor.shutdown();
        try {
            if (!consumerExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                consumerExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            consumerExecutor.shutdownNow();
        }
    }
    
    public void queueOrder(Order order) {
        try {
            notificationQueue.put(order);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public int getNotificationCount() {
        return notificationCounter.get();
    }

    public void resetCounter() {
        notificationCounter.set(0);
    }

    private void processNotifications() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Order order = notificationQueue.take();
                barrier.await();
                sendNotification(order);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (BrokenBarrierException e) {
                log.error("Barrier broken", e);
            }
        }
    }

    private void sendNotification(Order order) {
        try {
            Thread.sleep(1000); // медленная бизнес-логика отправки сбщ
            log.info("[productId = {}] Отправлено уведомление для заказа: {}", order.getProduct(), order);
            notificationCounter.incrementAndGet();
            updateOrderStatus(order, "Отправлен");
            
            template.convertAndSend("/topic/notifications", notificationCounter.get());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void sendBatchNotifications(List<Order> batch) {
        for (Order order : batch) {
            try {
                Thread.sleep(1000);
                updateOrderStatus(order, "Отправлен");
                System.out.println("Notification sent to: " + order.getUserEmail());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void updateOrderStatus(Order order, String status) {
        order.setStatus(status);
        orderRepository.save(order);
        template.convertAndSend("/topic/orders", order); // Отправить обновление через WebSocket
    }
}
