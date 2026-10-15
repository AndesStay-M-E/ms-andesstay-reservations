package cl.duoc.andesstay.reservations.messaging.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Nombres de exchanges, colas, routing keys y DLQ leídos desde
 * application.yaml (prefijo andesstay.messaging).
 *
 * Ningún nombre de RabbitMQ se escribe en el código: todos salen de aquí.
 */
@Validated
@ConfigurationProperties(prefix = "andesstay.messaging")
public record MessagingProperties(

        @Valid @NotNull
        Exchanges exchanges,

        @Valid @NotNull
        Routes routes,

        @NotBlank
        String eventRoutingKeyPrefix

) {

    public record Exchanges(
            @NotBlank String commands,
            @NotBlank String events,
            @NotBlank String deadLetter
    ) {
    }

    /**
     * Una ruta por caso de uso.
     */
    public record Routes(
            @Valid @NotNull Route email,
            @Valid @NotNull Route housekeeping,
            @Valid @NotNull Route voucher,
            @Valid @NotNull Route audit
    ) {
    }

    /**
     * queue: cola principal.
     * routingKey: clave con la que se enlaza la cola al exchange.
     * deadLetterQueue: cola DLQ; también se usa como routing key en el DLX.
     */
    public record Route(
            @NotBlank String queue,
            @NotBlank String routingKey,
            @NotBlank String deadLetterQueue
    ) {
    }
}
