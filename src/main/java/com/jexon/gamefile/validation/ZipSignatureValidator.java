package com.jexon.gamefile.validation;

import com.jexon.gamefile.exception.InvalidGameFileException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;

@Component
public class ZipSignatureValidator {
    // stream 앞 4바이트로 ZIP 형식을 확인하고, 소비한 prefix를 복원

    private static final int SIGNATURE_LENGTH = 4; // 맨 앞 4바이트
    private static final byte[] REGULAR_ZIP_SIGNATURE = {0x50, 0x4B, 0x03, 0x04}; // 일반 zip
    private static final byte[] EMPTY_ZIP_SIGNATURE = {0x50, 0x4B, 0x05, 0x06}; // 빈 zip

    public InputStream validateAndRestore(InputStream inputStream) {
        if (inputStream == null) {
            throw new InvalidGameFileException("검증할 파일 스트림이 필요합니다.");
        }

        // 검사할 바이트 받아오기
        byte[] prefix = readPrefix(inputStream);

        // zip인지 검사
        if (!isAllowedSignature(prefix)) {
            throw new InvalidGameFileException("허용된 ZIP 파일 형식이 아닙니다.");
        }

        // 잘라낸 key 복원하여 반환
        return new SequenceInputStream(new ByteArrayInputStream(prefix), inputStream);
    }

    // 스트림에서 4바이트 읽어오기
    private byte[] readPrefix(InputStream inputStream) {
        byte[] prefix = new byte[SIGNATURE_LENGTH]; // 4byte
        int offset = 0;

        try {
            while (offset < SIGNATURE_LENGTH) { // stream → 4byte 채울때까지 반복
                // 스트림에서 데이터 읽어오기
                int bytesRead = inputStream.read(prefix, offset, SIGNATURE_LENGTH - offset);

                if (bytesRead == -1) {
                    throw new InvalidGameFileException("ZIP signature를 확인하기에는 파일이 너무 짧습니다.");
                }
                if (bytesRead == 0) {
                    continue;
                }

                // 읽은 만큼 offset 증가
                offset += bytesRead;
            }
        } catch (IOException exception) {
            throw new InvalidGameFileException("ZIP signature를 읽을 수 없습니다.", exception);
        }

        return prefix;
    }

    // zip 형식인지 검사
    private boolean isAllowedSignature(byte[] prefix) {
        return Arrays.equals(prefix, REGULAR_ZIP_SIGNATURE)
                || Arrays.equals(prefix, EMPTY_ZIP_SIGNATURE);
    }
}
