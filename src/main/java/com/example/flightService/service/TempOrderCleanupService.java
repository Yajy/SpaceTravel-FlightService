package com.example.flightService.service;

import com.example.flightService.model.ScheduleEntity;
import com.example.flightService.model.TempOrders;
import com.example.flightService.repository.ScheduleRepository;
import com.example.flightService.repository.TempOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class TempOrderCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TempOrderCleanupService.class);

    @Autowired
    private TempOrderRepository tempOrderRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Scheduled(fixedRate = 300000)
    public void cleanupExpiredOrders() {
        log.info("Starting cleanup of expired temp orders");

        try {
            LocalDateTime currentTime = LocalDateTime.now();
            List<TempOrders> expiredOrders = tempOrderRepository.findExpiredOrders(currentTime, TempOrders.OrderStatus.PENDING);

            if (expiredOrders.isEmpty()) {
                log.debug("No expired orders found");
                return;
            }

            log.info("Found {} expired orders to cleanup", expiredOrders.size());

            for (TempOrders expiredOrder : expiredOrders) {
                try {
                    Optional<ScheduleEntity> scheduleOpt = scheduleRepository.findById(expiredOrder.getScheduleId());
                    if (scheduleOpt.isPresent()) {
                        ScheduleEntity schedule = scheduleOpt.get();
                        schedule.setAvailableSeats(schedule.getAvailableSeats() + expiredOrder.getSeatsLocked());
                        scheduleRepository.save(schedule);

                        log.info("Restored {} seats to schedule {} for expired order {}",
                                expiredOrder.getSeatsLocked(),
                                expiredOrder.getScheduleId(),
                                expiredOrder.getOrderId());
                    } else {
                        log.warn("Schedule {} not found for expired order {}",
                                expiredOrder.getScheduleId(),
                                expiredOrder.getOrderId());
                    }
                    expiredOrder.setStatus(TempOrders.OrderStatus.EXPIRED);
                    tempOrderRepository.save(expiredOrder);

                } catch (Exception e) {
                    log.error("Failed to cleanup expired order {}: {}",
                             expiredOrder.getOrderId(), e.getMessage(), e);
                }
            }

            log.info("Completed cleanup of {} expired orders", expiredOrders.size());

        } catch (Exception e) {
            log.error("Failed to run expired order cleanup: {}", e.getMessage(), e);
        }
    }
}
