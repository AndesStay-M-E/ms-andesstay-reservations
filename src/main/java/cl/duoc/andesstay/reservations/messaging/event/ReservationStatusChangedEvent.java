package cl.duoc.andesstay.reservations.messaging.event;

import cl.duoc.andesstay.reservations.entity.ReservationStatus;

import java.time.LocalDate;

public record ReservationStatusChangedEvent(

        Long reservationId,

        String userEmail,

        Long accommodationId,

        LocalDate checkInDate,

        LocalDate checkOutDate,

        ReservationStatus previousStatus,

        ReservationStatus newStatus

) {
}