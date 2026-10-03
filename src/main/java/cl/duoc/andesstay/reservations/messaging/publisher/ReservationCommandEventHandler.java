package cl.duoc.andesstay.reservations.messaging.publisher;

import cl.duoc.andesstay.reservations.entity.ReservationStatus;
import cl.duoc.andesstay.reservations.messaging.config.RabbitMQConfig;
import cl.duoc.andesstay.reservations.messaging.event.ReservationStatusChangedEvent;
import cl.duoc.andesstay.reservations.messaging.model.CommandEnvelope;
import cl.duoc.andesstay.reservations.messaging.model.EmailCommandPayload;
import cl.duoc.andesstay.reservations.messaging.model.HousekeepingCommandPayload;
import cl.duoc.andesstay.reservations.messaging.model.VoucherCommandPayload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Component
public class ReservationCommandEventHandler {

    private final ReservationCommandPublisher publisher;
    

    public ReservationCommandEventHandler(
            ReservationCommandPublisher publisher
    ) {
        this.publisher = publisher;
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

        if (event.newStatus() == ReservationStatus.CONFIRMADA) {

            publishConfirmationCommands(
                    event,
                    traceId,
                    correlationId
            );
        }

        if (event.newStatus()
                == ReservationStatus.CHECKIN_PENDIENTE) {

            publishCheckInReminder(
                    event,
                    traceId,
                    correlationId
            );
        }

        if (event.newStatus()
                == ReservationStatus.CHECKOUT) {

            publishCheckoutCommands(
                    event,
                    traceId,
                    correlationId
            );
        }
    }

    private void publishConfirmationCommands(
        ReservationStatusChangedEvent event,
        String traceId,
        String correlationId
    ) {

        var emailPayload =
                new EmailCommandPayload(
                        event.reservationId(),
                        event.userEmail(),
                        "RESERVATION_CONFIRMED"
                );

        publisher.publish(
                RabbitMQConfig.EMAIL_SEND,
                CommandEnvelope.create(
                        RabbitMQConfig.EMAIL_SEND,
                        traceId,
                        correlationId,
                        emailPayload
                )
        );

        var housekeepingPayload =
                new HousekeepingCommandPayload(
                        event.reservationId(),
                        event.accommodationId(),
                        event.checkInDate(),
                        event.checkOutDate(),
                        "PREPARE_ROOM"
                );

        publisher.publish(
                RabbitMQConfig.HOUSEKEEPING_TICKET,
                CommandEnvelope.create(
                        RabbitMQConfig.HOUSEKEEPING_TICKET,
                        traceId,
                        correlationId,
                        housekeepingPayload
                )
        );

        var voucherPayload =
                new VoucherCommandPayload(
                        event.reservationId(),
                        event.userEmail(),
                        "RESERVATION_VOUCHER"
                );

        publisher.publish(
                RabbitMQConfig.VOUCHER_GENERATE,
                CommandEnvelope.create(
                        RabbitMQConfig.VOUCHER_GENERATE,
                        traceId,
                        correlationId,
                        voucherPayload
                )
        );
    }

    private void publishCheckInReminder(
        ReservationStatusChangedEvent event,
        String traceId,
        String correlationId
    ) {

        var payload =
                new EmailCommandPayload(
                        event.reservationId(),
                        event.userEmail(),
                        "CHECKIN_REMINDER"
                );

        publisher.publish(
                RabbitMQConfig.EMAIL_SEND,
                CommandEnvelope.create(
                        RabbitMQConfig.EMAIL_SEND,
                        traceId,
                        correlationId,
                        payload
                )
        );
    }

    private void publishCheckoutCommands(
        ReservationStatusChangedEvent event,
        String traceId,
        String correlationId
    ) {

        var emailPayload =
                new EmailCommandPayload(
                        event.reservationId(),
                        event.userEmail(),
                        "CHECKOUT_COMPLETED"
                );

        publisher.publish(
                RabbitMQConfig.EMAIL_SEND,
                CommandEnvelope.create(
                        RabbitMQConfig.EMAIL_SEND,
                        traceId,
                        correlationId,
                        emailPayload
                )
        );

        var housekeepingPayload =
                new HousekeepingCommandPayload(
                        event.reservationId(),
                        event.accommodationId(),
                        event.checkInDate(),
                        event.checkOutDate(),
                        "CLEAN_ROOM"
                );

        publisher.publish(
                RabbitMQConfig.HOUSEKEEPING_TICKET,
                CommandEnvelope.create(
                        RabbitMQConfig.HOUSEKEEPING_TICKET,
                        traceId,
                        correlationId,
                        housekeepingPayload
                )
        );
    }
}