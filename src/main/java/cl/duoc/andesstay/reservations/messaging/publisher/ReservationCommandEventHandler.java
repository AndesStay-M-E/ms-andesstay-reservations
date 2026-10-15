package cl.duoc.andesstay.reservations.messaging.publisher;

import cl.duoc.andesstay.reservations.entity.ReservationStatus;
import cl.duoc.andesstay.reservations.messaging.config.MessagingProperties;
import cl.duoc.andesstay.reservations.messaging.event.ReservationStatusChangedEvent;
import cl.duoc.andesstay.reservations.messaging.model.CommandEnvelope;
import cl.duoc.andesstay.reservations.messaging.model.EmailCommandPayload;
import cl.duoc.andesstay.reservations.messaging.model.HousekeepingCommandPayload;
import cl.duoc.andesstay.reservations.messaging.model.VoucherCommandPayload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Locale;
import java.util.UUID;

/**
 * Traduce los cambios de estado de una reserva en mensajes RabbitMQ.
 *
 * Se ejecuta DESPUÉS del commit: si la transacción falla, no se envía nada.
 *
 *  - Siempre: evento al exchange topic (auditoría).
 *  - CONFIRMADA: correo + ticket de preparación + voucher.
 *  - CHECKIN_PENDIENTE: correo recordatorio.
 *  - CHECKOUT: correo + ticket de limpieza.
 */
@Component
public class ReservationCommandEventHandler {

    private static final String RESERVATION_EVENT_TYPE =
            "reservation.status-changed";

    private final ReservationCommandPublisher publisher;
    private final MessagingProperties properties;

    public ReservationCommandEventHandler(
            ReservationCommandPublisher publisher,
            MessagingProperties properties
    ) {
        this.publisher = publisher;
        this.properties = properties;
    }

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void handle(
            ReservationStatusChangedEvent event
    ) {

        String traceId =
                UUID.randomUUID().toString();

        String correlationId =
                "reservation-" + event.reservationId();

        publishAuditEvent(event, traceId, correlationId);

        if (event.newStatus() == ReservationStatus.CONFIRMADA) {
            publishConfirmationCommands(event, traceId, correlationId);
        }

        if (event.newStatus() == ReservationStatus.CHECKIN_PENDIENTE) {
            publishCheckInReminder(event, traceId, correlationId);
        }

        if (event.newStatus() == ReservationStatus.CHECKOUT) {
            publishCheckoutCommands(event, traceId, correlationId);
        }
    }

    /**
     * Routing key del evento: prefijo + estado. Ej: reservation.confirmada
     */
    private void publishAuditEvent(
            ReservationStatusChangedEvent event,
            String traceId,
            String correlationId
    ) {
        String routingKey =
                properties.eventRoutingKeyPrefix()
                        + event.newStatus().name().toLowerCase(Locale.ROOT);

        publisher.publishEvent(
                routingKey,
                CommandEnvelope.create(
                        RESERVATION_EVENT_TYPE,
                        traceId,
                        correlationId,
                        event
                )
        );
    }

    private void publishConfirmationCommands(
            ReservationStatusChangedEvent event,
            String traceId,
            String correlationId
    ) {
        sendEmail(event, traceId, correlationId, "RESERVATION_CONFIRMED");

        sendHousekeeping(event, traceId, correlationId, "PREPARE_ROOM");

        String voucherKey = properties.routes().voucher().routingKey();

        publisher.publishCommand(
                voucherKey,
                CommandEnvelope.create(
                        voucherKey,
                        traceId,
                        correlationId,
                        new VoucherCommandPayload(
                                event.reservationId(),
                                event.userEmail(),
                                "RESERVATION_VOUCHER"
                        )
                )
        );
    }

    private void publishCheckInReminder(
            ReservationStatusChangedEvent event,
            String traceId,
            String correlationId
    ) {
        sendEmail(event, traceId, correlationId, "CHECKIN_REMINDER");
    }

    private void publishCheckoutCommands(
            ReservationStatusChangedEvent event,
            String traceId,
            String correlationId
    ) {
        sendEmail(event, traceId, correlationId, "CHECKOUT_COMPLETED");

        sendHousekeeping(event, traceId, correlationId, "CLEAN_ROOM");
    }

    private void sendEmail(
            ReservationStatusChangedEvent event,
            String traceId,
            String correlationId,
            String notificationType
    ) {
        String emailKey = properties.routes().email().routingKey();

        publisher.publishCommand(
                emailKey,
                CommandEnvelope.create(
                        emailKey,
                        traceId,
                        correlationId,
                        new EmailCommandPayload(
                                event.reservationId(),
                                event.userEmail(),
                                notificationType
                        )
                )
        );
    }

    private void sendHousekeeping(
            ReservationStatusChangedEvent event,
            String traceId,
            String correlationId,
            String action
    ) {
        String housekeepingKey =
                properties.routes().housekeeping().routingKey();

        publisher.publishCommand(
                housekeepingKey,
                CommandEnvelope.create(
                        housekeepingKey,
                        traceId,
                        correlationId,
                        new HousekeepingCommandPayload(
                                event.reservationId(),
                                event.accommodationId(),
                                event.checkInDate(),
                                event.checkOutDate(),
                                action
                        )
                )
        );
    }
}
