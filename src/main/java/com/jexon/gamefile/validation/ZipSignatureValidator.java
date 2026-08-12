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
    private static final int SIGNATURE_LENGTH = 4;
    private static final byte[] REGULAR_ZIP_SIGNATURE = {0x50, 0x4B, 0x03, 0x04};
    private static final byte[] EMPTY_ZIP_SIGNATURE = {0x50, 0x4B, 0x05, 0x06};

    public InputStream validateAndRestore(InputStream inputStream) {
        if (inputStream == null) {
            throw new InvalidGameFileException("검증할 파일 스트림이 필요합니다.");
        }

        byte[] prefix = readPrefix(inputStream);
        if (!isAllowedSignature(prefix)) {
            throw new InvalidGameFileException("허용된 ZIP 파일 형식이 아닙니다.");
        }

        return new SequenceInputStream(new ByteArrayInputStream(prefix), inputStream);
    }

    private byte[] readPrefix(InputStream inputStream) {
        byte[] prefix = new byte[SIGNATURE_LENGTH];
        int offset = 0;

        try {
            while (offset < SIGNATURE_LENGTH) {
                int bytesRead = inputStream.read(prefix, offset, SIGNATURE_LENGTH - offset);
                if (bytesRead == -1) {
                    throw new InvalidGameFileException("ZIP signature를 확인하기에는 파일이 너무 짧습니다.");
                }
                if (bytesRead == 0) {
                    continue;
                }
                offset += bytesRead;
            }
        } catch (IOException exception) {
            throw new InvalidGameFileException("ZIP signature를 읽을 수 없습니다.", exception);
        }

        return prefix;
    }

    private boolean isAllowedSignature(byte[] prefix) {
        return Arrays.equals(prefix, REGULAR_ZIP_SIGNATURE)
                || Arrays.equals(prefix, EMPTY_ZIP_SIGNATURE);
    }
}
