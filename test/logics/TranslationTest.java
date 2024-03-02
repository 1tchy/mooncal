package logics;

import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.test.WithApplication;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.*;

public class TranslationTest extends WithApplication {

    private static final Lang defaultLang = Lang.forCode("de");
    private MessagesApi messagesApi;

    @Before
    public void setUp() {
        messagesApi = app.injector().instanceOf(MessagesApi.class);
    }

    @Test
    public void testGetAllKeysHelperMethod() throws IOException {
        final Set<String> allKeys = getAllKeys();
        assertThat(allKeys.size(), greaterThan(20));
        assertTrue(allKeys.contains("events.title"));
    }

    private Set<String> getAllKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        try (Scanner scanner = new Scanner(new File(Objects.requireNonNull(Messages.class.getResource("/messages")).getFile()))) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                keys.add(line.replaceAll("=.*", ""));
            }
        }
        keys.remove("");
        return keys;
    }

    @Test
    public void testGetAllLangsHelperMethod() {
        final List<Lang> allKeys = getAllLangs();
        assertThat(allKeys.size(), greaterThan(2));
    }


    private List<Lang> getAllLangs() {
        return Lang.availables(app);
    }

    @Test
    public void noMessageIsJustItsKey() throws IOException {
        for (Lang lang : getAllLangs()) {
            for (String key : getAllKeys()) {
                assertNotEquals(key, messagesApi.get(lang, key));
            }
        }
    }

    @Test
    public void allMessagesInAllLanguages() throws IOException {
        for (Lang lang : getAllLangs()) {
            if (!lang.equals(defaultLang)) {
                for (String key : getAllKeys()) {
                    assertSpecificTranslation(lang, key);
                }
            }
        }
    }

    private void assertSpecificTranslation(Lang lang, String key) {
        if (isTranslationRequired(lang, key)) {
            assertNotEquals(key + " for '" + lang.code() + "' and '" + defaultLang.code() + "' are equal but probably shouldn't",
                    messagesApi.get(lang, key),
                    messagesApi.get(defaultLang, key));
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    private static boolean isTranslationRequired(Lang lang, String key) {
        Map<String, String> noTranslationRequired = new HashMap<>() {{
            put("lang.en", ".*");
            put("lang.de", ".*");
            put("lang.nl", ".*");
            put("time.fromTo.in", ".*");
            put("navigation.home", ".*");
            put("events.title", "en");
            put("calendar.title", "nl");
        }};
        final String noTranslationsRequiredForLanguage = noTranslationRequired.get(key);
        if (noTranslationsRequiredForLanguage == null) return true; //translation required
        if (lang.code().matches(noTranslationsRequiredForLanguage)) return false; //translation NOT required
        return true; //translation required
    }

    @Test
    public void langField_lang_current() {
        for (Lang lang : getAllLangs()) {
            assertEquals(lang.code(), messagesApi.get(lang, "lang.current"));
        }
    }

    @Test
    public void langField_lang_XX() {
        for (Lang lang : getAllLangs()) {
            assertTrue("lang." + lang.code() + " is missing", messagesApi.isDefinedAt(defaultLang, "lang." + lang.code()));
        }
    }
}
