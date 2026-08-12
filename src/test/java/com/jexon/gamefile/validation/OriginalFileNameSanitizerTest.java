package com.jexon.gamefile.validation;

import com.jexon.gamefile.exception.InvalidGameFileException;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OriginalFileNameSanitizerTest {
    private final OriginalFileNameSanitizer sanitizer = new OriginalFileNameSanitizer();

    @Test
    void sanitizesNormalFileNameAndLowercasesExtension() {
        SanitizedFileName result = sanitizer.sanitize("game.ZIP");

        assertThat(result.sanitizedFileName()).isEqualTo("game.ZIP");
        assertThat(result.extension()).isEqualTo("zip");
    }

    @Test
    void extractsBaseNameFromWindowsAndUnixPaths() {
        assertThat(sanitizer.sanitize("C:\\fakepath\\game.zip").sanitizedFileName())
                .isEqualTo("game.zip");
        assertThat(sanitizer.sanitize("/tmp/game.zip").sanitizedFileName())
                .isEqualTo("game.zip");
    }

    @Test
    void rejectsNullAndBlankFileNames() {
        assertInvalid(null);
        assertInvalid("");
        assertInvalid("   ");
    }

    @Test
    void rejectsDotAndDotDotBaseNames() {
        assertInvalid(".");
        assertInvalid("..");
        assertInvalid("/tmp/.");
        assertInvalid("C:\\fakepath\\..");
    }

    @Test
    void rejectsNullByteAndC0OrC1ControlCharacters() {
        assertInvalid("game\u0000.zip");
        assertInvalid("game\u001F.zip");
        assertInvalid("game\u007F.zip");
        assertInvalid("game\u009F.zip");
    }

    @Test
    void rejectsLeadingAndTrailingWhitespaceInBaseName() {
        assertInvalid(" game.zip");
        assertInvalid("game.zip ");
        assertInvalid("game.zip\t");
    }

    @Test
    void rejectsExtensionOnlyAndMissingExtension() {
        assertInvalid(".zip");
        assertInvalid("game");
        assertInvalid("game.");
    }

    @Test
    void enforces255CharacterBoundary() {
        String valid = "a".repeat(251) + ".zip";
        String tooLong = "a".repeat(252) + ".zip";

        assertThat(sanitizer.sanitize(valid).sanitizedFileName()).hasSize(255);
        assertInvalid(tooLong);
    }

    @Test
    void allowsKoreanAndNormalizesUnicodeToNfc() {
        String decomposed = Normalizer.normalize("게임.zip", Normalizer.Form.NFD);

        SanitizedFileName result = sanitizer.sanitize(decomposed);

        assertThat(result.sanitizedFileName()).isEqualTo("게임.zip");
        assertThat(Normalizer.isNormalized(result.sanitizedFileName(), Normalizer.Form.NFC)).isTrue();
        assertThat(result.extension()).isEqualTo("zip");
    }

    private void assertInvalid(String fileName) {
        assertThatThrownBy(() -> sanitizer.sanitize(fileName))
                .isInstanceOf(InvalidGameFileException.class);
    }
}
