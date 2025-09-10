package com.example.flightService.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "temp_orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TempOrders {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "order_id", nullable = false, length = 100)
    private String orderId;

    @Column(name = "schedule_id", nullable = false, length = 36)
    private String scheduleId;

    @Column(name = "seats_locked", nullable = false)
    private Integer seatsLocked;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @PrePersist
    public void generateId() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.expiresAt == null) {
            this.expiresAt = this.createdAt.plusMinutes(10); // 10 minutes TTL
        }
        if (this.status == null) {
            this.status = OrderStatus.PENDING;
        }
    }

    public enum OrderStatus {
        PENDING, COMPLETED, FAILED, EXPIRED
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(this.expiresAt);
    }
}
