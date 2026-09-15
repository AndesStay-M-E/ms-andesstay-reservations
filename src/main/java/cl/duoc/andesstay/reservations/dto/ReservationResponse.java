package cl.duoc.andesstay.reservations.dto;

import cl.duoc.andesstay.reservations.entity.ReservationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationResponse(
        Long id,
        String userEmail,
        Long accommodationId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer guests,
        BigDecimal totalAmount,
        ReservationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
