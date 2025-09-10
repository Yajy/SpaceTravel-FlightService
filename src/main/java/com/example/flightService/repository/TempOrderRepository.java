package com.example.flightService.repository;

import com.example.flightService.model.TempOrders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TempOrderRepository extends JpaRepository<TempOrders, String> {

    Optional<TempOrders> findByOrderIdAndStatus(String orderId, TempOrders.OrderStatus status);

    @Query("SELECT t FROM TempOrders t WHERE t.expiresAt < :currentTime AND t.status = :status")
    List<TempOrders> findExpiredOrders(@Param("currentTime") LocalDateTime currentTime,
                                       @Param("status") TempOrders.OrderStatus status);

    List<TempOrders> findByScheduleIdAndStatus(String scheduleId, TempOrders.OrderStatus status);
}
