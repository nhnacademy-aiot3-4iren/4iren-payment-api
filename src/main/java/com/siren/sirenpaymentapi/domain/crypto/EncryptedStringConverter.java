package com.siren.sirenpaymentapi.domain.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.stereotype.Component;

/**
 * 엔티티 필드(billing_keys.provider_credential)를 DB에 쓸 때 암호화, 읽을 때 복호화한다.
 * 엔티티/서비스 코드는 평문 String만 다루면 되고, 실제 암복호화는 이 컨버터가 전담한다.
 * AES 키를 내장한 암호화기임
 */
@Component
@Converter
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private final TextEncryptor encryptor;

    public EncryptedStringConverter(
            @Value("${payment.crypto.password}") String password,
            @Value("${payment.crypto.salt}") String salt) {
        this.encryptor = Encryptors.text(password, salt); // AES 키 만들어짐
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return attribute == null ? null : encryptor.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        return dbData == null ? null : encryptor.decrypt(dbData);
    }
}
