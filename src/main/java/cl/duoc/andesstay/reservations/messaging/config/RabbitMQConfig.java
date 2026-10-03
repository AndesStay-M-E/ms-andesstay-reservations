package cl.duoc.andesstay.reservations.messaging.config;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String DIRECT_EXCHANGE =
            "cmd.direct";

    public static final String TOPIC_EXCHANGE =
            "cmd.topic";

    public static final String EMAIL_SEND =
            "email.send";

    public static final String HOUSEKEEPING_TICKET =
            "housekeeping.ticket";

    public static final String VOUCHER_GENERATE =
            "voucher.gen";

    @Bean
    public DirectExchange commandDirectExchange() {

        return ExchangeBuilder
                .directExchange(DIRECT_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public TopicExchange commandTopicExchange() {

        return ExchangeBuilder
                .topicExchange(TOPIC_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public JacksonJsonMessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}