package cl.duoc.andesstay.reservations.messaging.model;

import java.time.LocalDate;

public record HousekeepingCommandPayload(

        Long reservationId,

        Long accommodationId,

        LocalDate checkInDate,

        LocalDate checkOutDate,

        String action

) {
}