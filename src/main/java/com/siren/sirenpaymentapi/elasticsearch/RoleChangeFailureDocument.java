package com.siren.sirenpaymentapi.elasticsearch;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

/**
 * Payment -> Account "role 변경 요청" 발행이 실패한 기록.
 */
@Document(indexName = "4iren-payment-role-change-failures")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RoleChangeFailureDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Keyword)
    private String targetRole;

    @Field(type = FieldType.Keyword)
    private String tokenId;

    // ROUTING_FAILED(exchange까진 도착했는데 바인딩된 큐가 없음)
    // BROKER_NACK(브로커가 저장 실패)
    // PUBLISH_FAILED(RetryTemplate 3회 재시도까지 다 실패)
    @Field(type = FieldType.Keyword)
    private String failureStage;

    @Field(type = FieldType.Text)
    private String reason;

    @Field(type = FieldType.Date)
    private Instant occurredAt;
}
