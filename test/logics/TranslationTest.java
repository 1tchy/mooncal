package logics;

import org.junit.Test;
import play.i18n.Lang;
import play.i18n.Messages;
import play.test.WithApplication;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TranslationTest extends WithApplication {

    @Test
    public void testGetAllKeysHelperMethod() throws IOException {
        final Set<String> allKeys = getAllKeys();
        assertThat(allKeys.size(), greaterThan(20));
        assertTrue(allKeys.contains("events.title"));
    }

    private Set<String> getAllKeys() throws IOException {
        Set<String> keys = new HashSet<>();
        try (Scanner scanner = new Scanner(new File(Messages.class.getResource("/messages").getFile()))) {
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
        return Lang.availables();
    }

    @Test
    public void allMessagesInAllLanguages() throws IOException {
        for (Lang lang : getAllLangs()) {
            for (String key : getAllKeys()) {
                assertTrue(key + " in " + lang.code() + " is missing", Messages.isDefined(lang, key));
            }
        }
    }

    @Test
    public void langField_lang_current() {
        for (Lang lang : getAllLangs()) {
            assertEquals(lang.code(), Messages.get(lang, "lang.current"));
        }
    }

    @Test
    public void langField_lang_XX() {
        for (Lang lang : getAllLangs()) {
            assertTrue("lang." + lang.code() + " is missing", Messages.isDefined("lang." + lang.code()));
        }
    }
}
