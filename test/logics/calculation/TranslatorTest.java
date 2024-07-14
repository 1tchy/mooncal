package logics.calculation;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TranslatorTest {

    private final Translator cut = new Translator();

    @Test
    public void testTranslate() throws IOException {
        String text = "Hello world!";
        String actual = cut.translate("en", "es", text);
        assertEquals("¡Hola Mundo!", actual);
    }
}
