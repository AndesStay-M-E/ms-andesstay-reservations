package cl.duoc.andesstay.reservations.messaging.publisher;

import cl.duoc.andesstay.reservations.messaging.config.MessagingProperties;
import cl.duoc.andesstay.reservations.messaging.model.CommandEnvelope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * Productor de mensajes. Los servicios de negocio no conocen RabbitMQ:
 * solo llaman a publishCommand / publishEvent.
 *
 * Si el broker no está disponible, el error se registra en el log y
 * NO se propaga, para no afectar la lógica de reservas (la reserva ya
 * fue guardada en la base de datos).
 */
@Component
public class ReservationCommandPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(ReservationCommandPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final MessagingProperties properties;

    public ReservationCommandPublisher(
            RabbitTemplate rabbitTemplate,
            MessagingProperties properties
    ) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    /**
     * Envía un comando al exchange direct (un solo destinatario).
     */
    public void publishCommand(
            String routingKey,
            CommandEnvelope<?> command
    ) {
        send(properties.exchanges().commands(), routingKey, command);
    }

    /**
     * Envía un evento al exchange topic (cualquier interesado lo recibe).
     */
    public void publishEvent(
            String routingKey,
            CommandEnvelope<?> event
    ) {
        send(properties.exchanges().events(), routingKey, event);
    }

    private void send(
            String exchange,
            String routingKey,
            CommandEnvelope<?> envelope
    ) {
        // El id del mensaje viaja como correlación para que los callbacks
        // de confirmación puedan identificarlo en el log.
        CorrelationData correlationData =
                new CorrelationData(envelope.eventId().toString());

        try {
            rabbitTemplate.convertAndSend(
                    exchange,
                    routingKey,
                    envelope,
                    correlationData
            );

            log.info(
                    "Mensaje publicado exchange={} routingKey={} type={} eventId={} correlationId={}",
                    exchange,
                    routingKey,
                    envelope.type(),
                    envelope.eventId(),
                    envelope.correlationId()
            );

        } catch (AmqpException exception) {
            log.error(
                    "No se pudo publicar el mensaje exchange={} routingKey={} eventId={}: {}",
                    exchange,
                    routingKey,
                    envelope.eventId(),
                    exception.getMessage()
            );
        }
    }
}
