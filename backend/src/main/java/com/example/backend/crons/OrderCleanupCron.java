package com.example.backend.crons;

import com.example.backend.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupCron {

    private final OrderService orderService;

    // Run every minute
    @Scheduled(cron = "0 * * * * *")
    public void cancelExpiredOrders() {
        log.info("Running cron job to cancel expired pending orders...");
        orderService.cancelExpiredPendingOrders();
    }
}
