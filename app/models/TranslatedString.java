package models;

import logics.calculation.Translator;
import play.i18n.Lang;
import play.i18n.Langs;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TranslatedString {

    private final Map<String, String> strings;

    public <E extends Exception> TranslatedString(Langs langs, FunctionWithException<String, String, E> stringByLangCode) throws E {
        List<Lang> languages = langs.availables();
        strings = new HashMap<>(languages.size());
        for (Lang lang : languages) {
            strings.put(lang.code(), stringByLangCode.apply(lang.code()));
        }
    }

    private TranslatedString(Map<String, String> strings) {
        this.strings = strings;
    }

    public static TranslatedString translate(Langs langs, String en, Translator translator) throws IOException {
        return new TranslatedString(langs, lang -> translator.translate("en", lang, en));
    }

    public TranslatedString prefix(String prefix) {
        Map<String, String> prefixedStrings = strings.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> prefix + e.getValue()));
        return new TranslatedString(prefixedStrings);
    }

    public String getByLang(Lang lang) {
        return strings.get(lang.code());
    }

    public interface FunctionWithException<T, R, E extends Exception> {
        R apply(T t) throws E;
    }
}
