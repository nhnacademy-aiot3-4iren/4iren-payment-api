package com.siren.sirenpaymentapi.service;

import com.siren.sirenpaymentapi.elasticsearch.RoleChangeFailureLogService;
import com.siren.sirenpaymentapi.event.RoleChangeRequested;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class RoleChangeEventPublisher {
    private final ApplicationEventPublisher applicationEventPublisher;
    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange paymentEventsExchange;
    private final RoleChangeFailureLogService roleChangeFailureLogService;

    // ConfirmCallback은 발행 시점의 메시지 본문을 안 주고 correlationId만 줘서, nack났을 때 어떤 이벤트였는지
    // 알아내려면 발행 시점에 correlationId <-> 이벤트를 직접 기억해뒀다가 조회해야 한다.
    private final Map<String, RoleChangeRequested> pendingCorrelations = new ConcurrentHashMap<>();

    @Value("${payment.role-change.routing-key:payment.role-change}")
    private String routingKey;

    @PostConstruct
    void registerCallbacks() {
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setReturnsCallback(this::handleReturned);
        rabbitTemplate.setConfirmCallback(this::handleConfirm);
    }

    public void requestRoleChange(Long userId, String role, String jti) {
        applicationEventPublisher.publishEvent(new RoleChangeRequested(userId, role, jti));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onRoleChangeRequested(RoleChangeRequested event) {
        String correlationId = UUID.randomUUID().toString();
        pendingCorrelations.put(correlationId, event);
        try {
            rabbitTemplate.convertAndSend(paymentEventsExchange.getName(), routingKey, event,
                    new CorrelationData(correlationId));
            log.info("[ROLE_CHANGE_PUBLISHED] Role Change 이벤트 발행 - userId={}, role={}, jti={}",
                    event.userId(), event.role(), event.jti());
        } catch (Exception e) {
            // RetryTemplate이 내부적으로 3회(지수 백오프로 재시도 간격 늘려가면서) 재시도하고도 실패하면 여기로 옴
            pendingCorrelations.remove(correlationId);
            log.error("[ROLE_CHANGE_PUBLISH_FAILED] Account로 role 변경 요청 발행 최종 실패 - userId={}, role={}, jti={}",
                    event.userId(), event.role(), event.jti(), e);
            roleChangeFailureLogService.save(event.userId(), event.role(), event.jti(),
                    "PUBLISH_FAILED", e.getMessage());
        }
    }

    // exchange까진 도착했는데 바인딩된 큐가 없어서 라우팅 실패한 경우 - 반환된 메시지 본문을 그대로 디코딩하면
    private void handleReturned(ReturnedMessage returned) {
        RoleChangeRequested event = decode(returned.getMessage());
        log.error("[ROLE_CHANGE_ROUTING_FAILED] 라우팅 실패 - exchange={}, routingKey={}, replyCode={}, replyText={}, "
                        + "userId={}, role={}, jti={}",
                returned.getExchange(), returned.getRoutingKey(), returned.getReplyCode(), returned.getReplyText(),
                event == null ? null : event.userId(), event == null ? null : event.role(),
                event == null ? null : event.jti());
        roleChangeFailureLogService.save(event == null ? null : event.userId(),
                event == null ? null : event.role(), event == null ? null : event.jti(),
                "ROUTING_FAILED", returned.getReplyText());
    }

    // 브로커가 메시지를 받았는지(ack)/저장에 실패했는지(nack) 비동기로 확인
    private void handleConfirm(CorrelationData correlationData, boolean ack, String cause) {
        if (correlationData == null) {
            return;
        }
        if (ack) {
            pendingCorrelations.remove(correlationData.getId());
            return;
        }
        RoleChangeRequested event = pendingCorrelations.remove(correlationData.getId());
        log.error("[ROLE_CHANGE_BROKER_NACK] 브로커 nack - correlationId={}, cause={}, userId={}, role={}, jti={}",
                correlationData.getId(), cause, event == null ? null : event.userId(),
                event == null ? null : event.role(), event == null ? null : event.jti());
        roleChangeFailureLogService.save(event == null ? null : event.userId(),
                event == null ? null : event.role(), event == null ? null : event.jti(),
                "BROKER_NACK", cause);
    }

    private RoleChangeRequested decode(Message message) {
        try {
            return (RoleChangeRequested) rabbitTemplate.getMessageConverter().fromMessage(message);
        } catch (Exception e) {
            return null;
        }
    }
}
