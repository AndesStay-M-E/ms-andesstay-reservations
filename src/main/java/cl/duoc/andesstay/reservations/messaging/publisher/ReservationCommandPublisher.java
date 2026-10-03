package cl.duoc.andesstay.reservations.messaging.publisher;

import cl.duoc.andesstay.reservations.messaging.config.RabbitMQConfig;
import cl.duoc.andesstay.reservations.messaging.model.CommandEnvelope;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
public class ReservationCommandPublisher {

    private final RabbitTemplate rabbitTemplate;

    public ReservationCommandPublisher(
            RabbitTemplate rabbitTemplate
    ) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(
            String routingKey,
            CommandEnvelope<?> command
    ) {

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.DIRECT_EXCHANGE,
                routingKey,
                command
        );
    }
}