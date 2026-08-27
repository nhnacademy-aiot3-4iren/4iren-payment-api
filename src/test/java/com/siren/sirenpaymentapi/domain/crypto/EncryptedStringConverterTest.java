package com.siren.sirenpaymentapi.domain.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EncryptedStringConverterTest {

    private EncryptedStringConverter encryptedStringConverter;

    @BeforeEach
    void setUp() {
        encryptedStringConverter = new EncryptedStringConverter("test-password", "abcdef0123456789");
    }

    @Test
    void convertToDatabaseColumnEncrypts() {
        String encrypted = encryptedStringConverter.convertToDatabaseColumn("billing-key-1");

        assertNotNull(encrypted);
        assertNotEquals("billing-key-1", encrypted);
    }

    @Test
    void convertToDatabaseColumnReturnsNullWhenInputNull() {
        assertNull(encryptedStringConverter.convertToDatabaseColumn(null));
    }

    @Test
    void convertToEntityAttributeDecryptsBackToOriginal() {
        String encrypted = encryptedStringConverter.convertToDatabaseColumn("billing-key-1");

        String decrypted = encryptedStringConverter.convertToEntityAttribute(encrypted);

        assertEquals("billing-key-1", decrypted);
    }

    @Test
    void convertToEntityAttributeReturnsNullWhenInputNull() {
        assertNull(encryptedStringConverter.convertToEntityAttribute(null));
    }
}
