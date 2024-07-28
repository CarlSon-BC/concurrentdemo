package com.example.demo.controller;

import com.example.demo.domain.Inventory;
import com.example.demo.repository.InventoryRepository;
import com.example.demo.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryRepository repository;
    private final NotificationService notificationService;
    private final SimpMessagingTemplate template;

    @GetMapping("/inventories")
    public String getInventories(Model model) {
        List<Inventory> inventories = repository.findAll();
        int notificationCount = notificationService.getNotificationCount();
        model.addAttribute("inventories", inventories);
        model.addAttribute("notificationCount", notificationCount);
        return "inventories";
    }
}
