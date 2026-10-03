package cl.duoc.andesstay.reservations.messaging.model;

public record EmailCommandPayload(

        Long reservationId,

        String recipient,

        String notificationType

) {
}