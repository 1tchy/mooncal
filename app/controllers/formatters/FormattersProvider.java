package controllers.formatters;

import models.EventType;
import models.MoonPhaseType;
import play.data.format.Formatters;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.time.Period;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Singleton
public class FormattersProvider implements Provider<Formatters> {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssVV");
    private final MessagesApi messagesApi;

    @Inject
    public FormattersProvider(MessagesApi messagesApi) {
        this.messagesApi = messagesApi;
    }

    @Override
    public Formatters get() {
        return new Formatters(messagesApi)
                .register(ZonedDateTime.class, new Formatters.SimpleFormatter<>() {
                    @Override
                    public ZonedDateTime parse(String input, Locale l) {
                        return ZonedDateTime.parse(input.replaceAll(" ", "+"), DATE_TIME_FORMATTER);
                    }

                    @Override
                    public String print(ZonedDateTime input, Locale l) {
                        return DATE_TIME_FORMATTER.format(input);
                    }

                })
                .register(MoonPhaseType.class, new Formatters.SimpleFormatter<>() {
                    @Override
                    public MoonPhaseType parse(String input, Locale l) {
                        return MoonPhaseType.read(input);
                    }

                    @Override
                    public String print(MoonPhaseType input, Locale l) {
                        return input.getKey();
                    }
                })
                .register(EventType.class, new Formatters.SimpleFormatter<>() {
                    @Override
                    public EventType parse(String input, Locale l) {
                        return EventType.read(input);
                    }

                    @Override
                    public String print(EventType input, Locale l) {
                        return input.getKey();
                    }
                }).register(Period.class, new Formatters.SimpleFormatter<>() {
                    @Override
                    public Period parse(String input, Locale l) {
                        return Period.parse(input);
                    }

                    @Override
                    public String print(Period input, Locale l) {
                        return input.toString();
                    }
                }).register(Lang.class, new Formatters.SimpleFormatter<>() {
                    @Override
                    public Lang parse(String input, Locale l) {
                        return Lang.forCode(input);
                    }

                    @Override
                    public String print(Lang input, Locale l) {
                        return input.code();
                    }
                });
    }
}