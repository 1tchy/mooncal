package models;

import logics.calculation.Translator;
import play.i18n.Lang;

import java.io.IOException;

public record TranslatedString(String en, String de, String nl, String es, String fr, String ro) {

    public static TranslatedString translate(String en, Translator translator) throws IOException {
        String de = translator.translate("en", "de", en);
        String nl = translator.translate("en", "nl", en);
        String es = translator.translate("en", "es", en);
        String fr = translator.translate("en", "fr", en);
        String ro = translator.translate("en", "ro", en);
        return new TranslatedString(en, de, nl, es, fr, ro);
    }

    public TranslatedString prefix(String prefix) {
        return new TranslatedString(
                prefix + en,
                prefix + de,
                prefix + nl,
                prefix + es,
                prefix + fr,
                prefix + ro
        );
    }

    public String getByLang(Lang lang) {
        return switch (lang.code()) {
            case "de" -> de;
            case "nl" -> nl;
            case "es" -> es;
            case "fr" -> fr;
            case "ro" -> ro;
            default -> en;
        };
    }
}
