package com.siren.sirenpaymentapi.service;

import com.siren.sirenpaymentapi.elasticsearch.RoleChangeFailureLogService;
import com.siren.sirenpaymentapi.event.RoleChangeRequested;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleChangeEventPublisherTest {

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private RabbitTemplate rabbitTemplate;

    @Mock
    private RoleChangeFailureLogService roleChangeFailureLogService;

    @Mock
    private MessageConverter messageConverter;

    private RoleChangeEventPublisher roleChangeEventPublisher;

    @BeforeEach
    void setUp() {
        TopicExchange exchange = new TopicExchange("payment.events");
        roleChangeEventPublisher = new RoleChangeEventPublisher(
                applicationEventPublisher, rabbitTemplate, exchange, roleChangeFailureLogService);
        ReflectionTestUtils.setField(roleChangeEventPublisher, "routingKey", "payment.role-change");
    }

    @Test
    void requestRoleChangePublishesSpringEvent() {
        roleChangeEventPublisher.requestRoleChange(1L, RoleChangeRequested.OWNER, "token-1");

        ArgumentCaptor<RoleChangeRequested> captor = ArgumentCaptor.forClass(RoleChangeRequested.class);
        verify(applicationEventPublisher).publishEvent(captor.capture());
        assertEquals(1L, captor.getValue().userId());
        assertEquals(RoleChangeRequested.OWNER, captor.getValue().role());
    }

    @Test
    void onRoleChangeRequestedSendsToRabbit() {
        RoleChangeRequested event = new RoleChangeRequested(1L, RoleChangeRequested.OWNER, "token-1");

        roleChangeEventPublisher.onRoleChangeRequested(event);

        verify(rabbitTemplate).convertAndSend(eq("payment.events"), eq("payment.role-change"), eq(event), any(CorrelationData.class));
        verifyNoInteractions(roleChangeFailureLogService);
    }

    @Test
    void onRoleChangeRequestedSavesFailureWhenPublishThrows() {
        RoleChangeRequested event = new RoleChangeRequested(1L, RoleChangeRequested.OWNER, "token-1");
        doThrow(new RuntimeException("연결 실패"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), eq(event), any(CorrelationData.class));

        roleChangeEventPublisher.onRoleChangeRequested(event);

        verify(roleChangeFailureLogService).save(1L, RoleChangeRequested.OWNER, "token-1", "PUBLISH_FAILED", "연결 실패");
    }

    @Test
    void returnsCallbackSavesRoutingFailure() {
        roleChangeEventPublisher.registerCallbacks();
        ArgumentCaptor<RabbitTemplate.ReturnsCallback> captor = ArgumentCaptor.forClass(RabbitTemplate.ReturnsCallback.class);
        verify(rabbitTemplate).setReturnsCallback(captor.capture());

        RoleChangeRequested event = new RoleChangeRequested(1L, RoleChangeRequested.OWNER, "token-1");
        when(rabbitTemplate.getMessageConverter()).thenReturn(messageConverter);
        Message message = new Message(new byte[0], new MessageProperties());
        when(messageConverter.fromMessage(message)).thenReturn(event);
        ReturnedMessage returned = new ReturnedMessage(message, 312, "NO_ROUTE", "payment.events", "payment.role-change");

        captor.getValue().returnedMessage(returned);

        verify(roleChangeFailureLogService).save(1L, RoleChangeRequested.OWNER, "token-1", "ROUTING_FAILED", "NO_ROUTE");
    }

    @Test
    void confirmCallbackSavesBrokerNackFailure() {
        roleChangeEventPublisher.registerCallbacks();
        ArgumentCaptor<RabbitTemplate.ConfirmCallback> captor = ArgumentCaptor.forClass(RabbitTemplate.ConfirmCallback.class);
        verify(rabbitTemplate).setConfirmCallback(captor.capture());

        RoleChangeRequested event = new RoleChangeRequested(1L, RoleChangeRequested.OWNER, "token-1");
        String correlationId = "correlation-1";
        seedPendingCorrelation(correlationId, event);

        CorrelationData correlationData = new CorrelationData(correlationId);
        captor.getValue().confirm(correlationData, false, "브로커 오류");

        verify(roleChangeFailureLogService).save(1L, RoleChangeRequested.OWNER, "token-1", "BROKER_NACK", "브로커 오류");
    }

    @SuppressWarnings("unchecked")
    private void seedPendingCorrelation(String correlationId, RoleChangeRequested event) {
        var pendingCorrelations = (java.util.Map<String, RoleChangeRequested>)
                ReflectionTestUtils.getField(roleChangeEventPublisher, "pendingCorrelations");
        pendingCorrelations.put(correlationId, event);
    }

    @Test
    void confirmCallbackDoesNothingWhenAck() {
        roleChangeEventPublisher.registerCallbacks();
        ArgumentCaptor<RabbitTemplate.ConfirmCallback> captor = ArgumentCaptor.forClass(RabbitTemplate.ConfirmCallback.class);
        verify(rabbitTemplate).setConfirmCallback(captor.capture());

        CorrelationData correlationData = new CorrelationData("correlation-1");
        captor.getValue().confirm(correlationData, true, null);

        verifyNoInteractions(roleChangeFailureLogService);
    }
}
