package cl.duoc.andesstay.reservations.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topología RabbitMQ del sistema AndesStay.
 *
 * Este microservicio es el PRODUCTOR. Declara toda la topología para que
 * los mensajes queden guardados en sus colas aunque algún consumidor
 * todavía no esté levantado. Cada consumidor declara además su propia
 * cola con los mismos argumentos (RabbitMQ acepta declaraciones idénticas).
 *
 * Rutas de mensajería:
 *
 *  andesstay.commands (direct)
 *    ├── email.send          -> notify.email.q          (DLQ notify.email.dlq)
 *    ├── housekeeping.ticket -> catalog.housekeeping.q  (DLQ catalog.housekeeping.dlq)
 *    └── voucher.gen         -> report.voucher.q        (DLQ report.voucher.dlq)
 *
 *  andesstay.events (topic)
 *    └── reservation.#       -> audit.reservation.q     (DLQ audit.reservation.dlq)
 *
 *  andesstay.dlx (direct): recibe los mensajes rechazados y los envía a cada DLQ.
 *
 * Los nombres reales se leen desde application.yaml (MessagingProperties).
 */
@Configuration
@EnableConfigurationProperties(MessagingProperties.class)
public class RabbitMQConfig {

    private final MessagingProperties properties;

    public RabbitMQConfig(MessagingProperties properties) {
        this.properties = properties;
    }

    /* ======================= EXCHANGES ======================= */

    @Bean
    public DirectExchange commandsExchange() {
        return ExchangeBuilder
                .directExchange(properties.exchanges().commands())
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange eventsExchange() {
        return ExchangeBuilder
                .topicExchange(properties.exchanges().events())
                .durable(true)
                .build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return ExchangeBuilder
                .directExchange(properties.exchanges().deadLetter())
                .durable(true)
                .build();
    }

    /* ============ CASO DE USO 1: correo (notify) ============ */

    @Bean
    public Queue emailQueue() {
        return mainQueue(properties.routes().email());
    }

    @Bean
    public Queue emailDeadLetterQueue() {
        return deadLetterQueue(properties.routes().email());
    }

    @Bean
    public Binding emailBinding() {
        return BindingBuilder
                .bind(emailQueue())
                .to(commandsExchange())
                .with(properties.routes().email().routingKey());
    }

    @Bean
    public Binding emailDeadLetterBinding() {
        return BindingBuilder
                .bind(emailDeadLetterQueue())
                .to(deadLetterExchange())
                .with(properties.routes().email().deadLetterQueue());
    }

    /* ====== CASO DE USO 2: ticket de limpieza (catalog) ====== */

    @Bean
    public Queue housekeepingQueue() {
        return mainQueue(properties.routes().housekeeping());
    }

    @Bean
    public Queue housekeepingDeadLetterQueue() {
        return deadLetterQueue(properties.routes().housekeeping());
    }

    @Bean
    public Binding housekeepingBinding() {
        return BindingBuilder
                .bind(housekeepingQueue())
                .to(commandsExchange())
                .with(properties.routes().housekeeping().routingKey());
    }

    @Bean
    public Binding housekeepingDeadLetterBinding() {
        return BindingBuilder
                .bind(housekeepingDeadLetterQueue())
                .to(deadLetterExchange())
                .with(properties.routes().housekeeping().deadLetterQueue());
    }

    /* ======== CASO DE USO 3: voucher / documento (report) ======== */

    @Bean
    public Queue voucherQueue() {
        return mainQueue(properties.routes().voucher());
    }

    @Bean
    public Queue voucherDeadLetterQueue() {
        return deadLetterQueue(properties.routes().voucher());
    }

    @Bean
    public Binding voucherBinding() {
        return BindingBuilder
                .bind(voucherQueue())
                .to(commandsExchange())
                .with(properties.routes().voucher().routingKey());
    }

    @Bean
    public Binding voucherDeadLetterBinding() {
        return BindingBuilder
                .bind(voucherDeadLetterQueue())
                .to(deadLetterExchange())
                .with(properties.routes().voucher().deadLetterQueue());
    }

    /* ======= CASO DE USO 4: auditoría de eventos (audit) ======= */

    @Bean
    public Queue auditQueue() {
        return mainQueue(properties.routes().audit());
    }

    @Bean
    public Queue auditDeadLetterQueue() {
        return deadLetterQueue(properties.routes().audit());
    }

    @Bean
    public Binding auditBinding() {
        return BindingBuilder
                .bind(auditQueue())
                .to(eventsExchange())
                .with(properties.routes().audit().routingKey());
    }

    @Bean
    public Binding auditDeadLetterBinding() {
        return BindingBuilder
                .bind(auditDeadLetterQueue())
                .to(deadLetterExchange())
                .with(properties.routes().audit().deadLetterQueue());
    }

    /* ======================= CONVERSOR ======================= */

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /* ================= MÉTODOS DE APOYO ================= */

    /**
     * Cola durable que envía sus mensajes rechazados al DLX.
     * Los argumentos deben ser idénticos a los del consumidor.
     */
    private Queue mainQueue(MessagingProperties.Route route) {
        return QueueBuilder
                .durable(route.queue())
                .deadLetterExchange(properties.exchanges().deadLetter())
                .deadLetterRoutingKey(route.deadLetterQueue())
                .build();
    }

    private Queue deadLetterQueue(MessagingProperties.Route route) {
        return QueueBuilder
                .durable(route.deadLetterQueue())
                .build();
    }
}
