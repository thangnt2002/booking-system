package com.booking.orderservice.service.impl;

import com.booking.orderservice.dto.CursorDTO;
import com.booking.orderservice.exception.BusinessException;
import com.booking.orderservice.exception.ErrorCode;
import com.booking.orderservice.service.CursorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@Slf4j
public class CursorServiceImpl implements CursorService {

    @Value("${app.security.cursor-secret-key}")
    private String secretKey;

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    @Override
    public String generateCursor(String id, LocalDateTime time) {
        if (id == null || time == null) {
            log.info("Invalid value to generate cursor, id = {}, time = {}", id, time);
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
        try {
            String rawData = time + "|" + id;

            byte[] iv = new byte[IV_LENGTH_BYTE];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey key = getValidAesKey(secretKey);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            byte[] encryptedText = cipher.doFinal(rawData.getBytes(StandardCharsets.UTF_8));

            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedText.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedText);

            return Base64.getUrlEncoder().withoutPadding().encodeToString(byteBuffer.array());

        } catch (Exception e) {
            log.error("Error generating cursor for id = {}, time = {}", id, time, e);
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
    }

    public CursorDTO parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new CursorDTO();
        }
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(cursor);

            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[IV_LENGTH_BYTE];
            byteBuffer.get(iv);

            byte[] encryptedText = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedText);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            SecretKey key = getValidAesKey(secretKey);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BIT, iv));

            String rawData = new String(cipher.doFinal(encryptedText), StandardCharsets.UTF_8);

            String[] parts = rawData.split("\\|", 2);

            LocalDateTime time = LocalDateTime.parse(parts[0]);
            String id = parts[1];

            return new CursorDTO(id, time);

        } catch (Exception e) {
            log.warn("Invalid or tampered cursor token: {}", cursor, e);
            throw new BusinessException(ErrorCode.SERVER_ERROR);
        }
    }

    private static SecretKey getValidAesKey(String rawSecretKey) throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] keyBytes = sha.digest(rawSecretKey.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(keyBytes, "AES");
    }
}
