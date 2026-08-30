package com.siren.sirenpaymentapi.elasticsearch;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;

/**
 * MailService.sendMail이 재시도(RetryTemplate)까지 다 소진하고도 최종 실패한 기록.
 */
@Document(indexName = "4iren-payment-mail-failures")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MailFailureDocument {

    @Id
    private String id;

    @Field(type = FieldType.Long)
    private Long userId;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Text)
    private String reason;

    @Field(type = FieldType.Date)
    private Instant occurredAt;
}
