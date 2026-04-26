package logics.calculation;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TranslatorTest {

    private final Translator cut = new Translator();

    @Test
    void testTranslate() throws IOException {
        String text = "Hello world!";
        String actual = cut.translate("en", "es", text);
        assertEquals("¡Hola Mundo!", actual);
    }
}
