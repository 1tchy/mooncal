package models;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import play.i18n.Lang;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class EventTemplate {
    @NotNull
    private final ZonedDateTime dateTime;
    @NotNull
    private final String eventTypeId;

    public EventTemplate(@NotNull ZonedDateTime dateTime, @NotNull String eventTypeId) {
        this.dateTime = dateTime;
        this.eventTypeId = eventTypeId;
    }

    @NotNull
    public ZonedDateTime getDateTime() {
        return dateTime;
    }

    @NotNull
    public abstract String getTitle(ZoneId timezone, Lang lang);

    @NotNull
    public abstract String getPdfTitle(ZoneId timezone, Lang lang);

    @Nullable
    public abstract String getDescription(ZoneId timezone, Lang lang);

    @NotNull
    public String getEventTypeId() {
        return eventTypeId;
    }

    public static class WithoutZoneId extends EventTemplate {
        @Nullable
        private final Function<Lang, String> descriptionTemplate;
        @NotNull
        private final Function<Lang, String> titleTemplate;
        @NotNull
        private final Function<Lang, String> pdfTitleTemplate;

        public WithoutZoneId(@NotNull ZonedDateTime dateTime, @NotNull Function<Lang, String> titleTemplate, @NotNull Function<Lang, String> pdfTitleTemplate, @Nullable Function<Lang, String> descriptionTemplate, @NotNull String eventTypeId) {
            super(dateTime, eventTypeId);
            this.titleTemplate = titleTemplate;
            this.descriptionTemplate = descriptionTemplate;
            this.pdfTitleTemplate = pdfTitleTemplate;
        }

        @Override
        @NotNull
        public String getTitle(ZoneId timezone, Lang lang) {
            return titleTemplate.apply(lang);
        }

        @Override
        @NotNull
        public String getPdfTitle(ZoneId timezone, Lang lang) {
            return pdfTitleTemplate.apply(lang);
        }

        @Override
        @Nullable
        public String getDescription(ZoneId timezone, Lang lang) {
            return descriptionTemplate == null ? null : descriptionTemplate.apply(lang);
        }

        @Override
        public String toString() {
            return titleTemplate.apply(Lang.forCode("en")) + "@" + getDateTime();
        }
    }

    public static class WithZoneId extends EventTemplate {
        @Nullable
        private final BiFunction<ZoneId, Lang, String> descriptionTemplate;
        @NotNull
        private final BiFunction<ZoneId, Lang, String> titleTemplate;

        public WithZoneId(@NotNull ZonedDateTime dateTime, @NotNull BiFunction<ZoneId, Lang, String> titleTemplate, @Nullable BiFunction<ZoneId, Lang, String> descriptionTemplate, @NotNull String eventTypeId) {
            super(dateTime, eventTypeId);
            this.titleTemplate = titleTemplate;
            this.descriptionTemplate = descriptionTemplate;
        }

        @Override
        @NotNull
        public String getTitle(ZoneId timezone, Lang lang) {
            return titleTemplate.apply(timezone, lang);
        }

        @Override
        @NotNull
        public String getPdfTitle(ZoneId timezone, Lang lang) {
            return titleTemplate.apply(timezone, lang);
        }

        @Override
        @Nullable
        public String getDescription(ZoneId timezone, Lang lang) {
            return descriptionTemplate == null ? null : descriptionTemplate.apply(timezone, lang);
        }

        @Override
        public String toString() {
            return titleTemplate.apply(ZoneOffset.UTC, Lang.forCode("en")) + "@" + getDateTime();
        }
    }
}
