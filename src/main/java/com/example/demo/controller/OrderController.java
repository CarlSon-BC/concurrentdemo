package com.example.demo.controller;

import com.example.demo.domain.Order;
import com.example.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrderController {
    private final OrderRepository repository;

    @GetMapping("/orders")
    public String getOrders(Model model) {
        List<Order> orders = repository.findAll();
        model.addAttribute("orders", orders);
        return "orders";
    }
}