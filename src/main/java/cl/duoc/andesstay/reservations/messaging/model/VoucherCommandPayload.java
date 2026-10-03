package cl.duoc.andesstay.reservations.messaging.model;

public record VoucherCommandPayload(

        Long reservationId,

        String recipient,

        String documentType

) {
}