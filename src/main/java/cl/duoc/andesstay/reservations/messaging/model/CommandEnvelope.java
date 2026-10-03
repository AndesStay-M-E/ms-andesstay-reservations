package cl.duoc.andesstay.reservations.messaging.model;

import java.time.Instant;
import java.util.UUID;

public record CommandEnvelope<T>(

        String type,

        UUID eventId,

        Instant timestamp,

        String traceId,

        String correlationId,

        T payload

) {

    public static <T> CommandEnvelope<T> create(
            String type,
            String traceId,
            String correlationId,
            T payload
    ) {

        return new CommandEnvelope<>(
                type,
                UUID.randomUUID(),
                Instant.now(),
                traceId,
                correlationId,
                payload
        );
    }
}