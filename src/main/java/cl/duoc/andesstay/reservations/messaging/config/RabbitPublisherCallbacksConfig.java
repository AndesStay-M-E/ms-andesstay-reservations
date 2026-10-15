package cl.duoc.andesstay.reservations.messaging.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra en el log la respuesta del broker a cada publicación:
 *
 *  - ConfirmCallback: el broker confirma (ACK) o rechaza (NACK) el mensaje.
 *  - ReturnsCallback: el mensaje llegó al exchange pero ninguna cola
 *    lo pudo recibir (routing key sin binding). Requiere
 *    spring.rabbitmq.template.mandatory=true.
 *
 * Usa las propiedades publisher-confirm-type y publisher-returns
 * definidas en application.yaml.
 */
@Configuration
public class RabbitPublisherCallbacksConfig implements InitializingBean {

    private static final Logger log =
            LoggerFactory.getLogger(RabbitPublisherCallbacksConfig.class);

    private final RabbitTemplate rabbitTemplate;

    public RabbitPublisherCallbacksConfig(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Override
    public void afterPropertiesSet() {

        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            String id = correlationData != null
                    ? correlationData.getId()
                    : "sin-id";

            if (ack) {
                log.debug("Broker confirmó el mensaje id={}", id);
            } else {
                log.error("Broker rechazó (NACK) el mensaje id={} causa={}", id, cause);
            }
        });

        rabbitTemplate.setReturnsCallback(returned ->
                log.error(
                        "Mensaje NO ENTREGADO a ninguna cola exchange={} routingKey={} codigo={} motivo={}",
                        returned.getExchange(),
                        returned.getRoutingKey(),
                        returned.getReplyCode(),
                        returned.getReplyText()
                )
        );
    }
}
