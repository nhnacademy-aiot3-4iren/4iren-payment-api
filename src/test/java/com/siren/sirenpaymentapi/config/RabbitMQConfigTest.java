package com.siren.sirenpaymentapi.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.retry.support.RetryTemplate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RabbitMQConfigTest {

    @Mock
    private ConnectionFactory connectionFactory;

    private final RabbitMQConfig rabbitMQConfig = new RabbitMQConfig();

    @Test
    void paymentEventsExchangeUsesGivenName() {
        TopicExchange exchange = rabbitMQConfig.paymentEventsExchange("payment.events");

        assertEquals("payment.events", exchange.getName());
    }

    @Test
    void jackson2JsonMessageConverterIsNotNull() {
        assertNotNull(rabbitMQConfig.jackson2JsonMessageConverter());
    }

    @Test
    void retryTemplateIsNotNull() {
        assertNotNull(rabbitMQConfig.retryTemplate());
    }

    @Test
    void rabbitTemplateUsesGivenMessageConverter() {
        Jackson2JsonMessageConverter messageConverter = rabbitMQConfig.jackson2JsonMessageConverter();
        RetryTemplate retryTemplate = rabbitMQConfig.retryTemplate();

        RabbitTemplate rabbitTemplate = rabbitMQConfig.rabbitTemplate(connectionFactory, messageConverter, retryTemplate);

        assertEquals(messageConverter, rabbitTemplate.getMessageConverter());
    }
}
