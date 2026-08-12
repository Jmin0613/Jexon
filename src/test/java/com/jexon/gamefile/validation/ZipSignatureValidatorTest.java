package com.jexon.gamefile.validation;

import com.jexon.gamefile.exception.InvalidGameFileException;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ZipSignatureValidatorTest {
    private final ZipSignatureValidator validator = new ZipSignatureValidator();

    @Test
    void acceptsRegularZipAndRestoresEntireStream() throws Exception {
        byte[] original = {0x50, 0x4B, 0x03, 0x04, 1, 2, 3, 4};

        InputStream restored = validator.validateAndRestore(new ByteArrayInputStream(original));

        assertThat(restored.readAllBytes()).isEqualTo(original);
    }

    @Test
    void acceptsEmptyZipSignatureAndRestoresEntireStream() throws Exception {
        byte[] original = {0x50, 0x4B, 0x05, 0x06};

        InputStream restored = validator.validateAndRestore(new ByteArrayInputStream(original));

        assertThat(restored.readAllBytes()).isEqualTo(original);
    }

    @Test
    void rejectsSplitZipExeAndArbitrarySignatures() {
        assertInvalid(new byte[]{0x50, 0x4B, 0x07, 0x08});
        assertInvalid(new byte[]{0x4D, 0x5A, 0, 0});
        assertInvalid(new byte[]{1, 2, 3, 4});
    }

    @Test
    void rejectsInputShorterThanFourBytes() {
        assertInvalid(new byte[]{0x50, 0x4B, 0x03});
    }

    @Test
    void rejectsNullInputStream() {
        assertThatThrownBy(() -> validator.validateAndRestore(null))
                .isInstanceOf(InvalidGameFileException.class);
    }

    @Test
    void wrapsSignatureReadFailureAsInvalidGameFile() {
        InputStream failing = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("simulated read failure");
            }
        };

        assertThatThrownBy(() -> validator.validateAndRestore(failing))
                .isInstanceOf(InvalidGameFileException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    private void assertInvalid(byte[] content) {
        assertThatThrownBy(() -> validator.validateAndRestore(new ByteArrayInputStream(content)))
                .isInstanceOf(InvalidGameFileException.class);
    }
}
