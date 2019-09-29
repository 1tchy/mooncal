package logics.calculation;

import com.google.common.io.LineReader;
import models.EventInstance;
import models.EventTemplate;
import models.EventType;
import models.RequestForm;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import javax.inject.Inject;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.TreeMap;
import java.util.function.Consumer;

public class MoonEventCalculation extends Calculation {

    private final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("d.M.u'T'H:m:s");
    private final TreeMap<ZonedDateTime, EventTemplate> lunarEclipses = new TreeMap<>();
    private final TreeMap<ZonedDateTime, EventTemplate> solarEclipses = new TreeMap<>();
    private final TreeMap<ZonedDateTime, EventTemplate> moonLandings = new TreeMap<>();

    @Inject
    public MoonEventCalculation(MessagesApi messagesApi) {
        super(messagesApi);
        initializeLunarEclipses();
        initializeSolarEclipses();
        initializeMoonLandings();
    }

    private void initializeLunarEclipses() {
        initializeByCVS("lunar-eclipses/lunar-eclipses.csv", rows -> {
            final ZonedDateTime date = LocalDateTime.parse(rows[0], DATE_TIME_PATTERN).atZone(ZoneOffset.UTC);
            lunarEclipses.put(date, new EventTemplate(date, (zoneId, lang) -> getLunarEclipseName(rows[1], lang), (zoneId, lang) -> eventAt(date, getLunarEclipseName(rows[1], lang), zoneId, lang), "lunar-eclipse"));
        });
    }

    private void initializeSolarEclipses() {
        initializeByCVS("solar-eclipses/solar-eclipses.csv", rows -> {
            final ZonedDateTime date = LocalDateTime.parse(rows[0], DATE_TIME_PATTERN).atZone(ZoneOffset.UTC);
            solarEclipses.put(date, new EventTemplate(date, (zoneId, lang) -> getSolarEclipseName(rows[1], lang), (zoneId, lang) -> eventAt(date, getSolarEclipseName(rows[1], lang), zoneId, lang), "solar-eclipse"));
        });
    }

    private void initializeMoonLandings() {
        initializeByCVS("moon-landings/moon-landings.csv", rows -> {
            final ZonedDateTime date = LocalDateTime.parse(rows[0], DATE_TIME_PATTERN).atZone(ZoneOffset.UTC);
            moonLandings.put(date, new EventTemplate(date, (zoneId, lang) -> "🚀 " + rows[1], (zoneId, lang) -> rows[2], "moon-landing"));
        });
    }

    private void initializeByCVS(String fileName, Consumer<String[]> lineHandler) {
        try (final FileReader fileReader = new FileReader(this.getClass().getResource(fileName).getFile())) {
            final LineReader csvFile = new LineReader(fileReader);
            csvFile.readLine(); //skip header
            String line;
            while ((line = csvFile.readLine()) != null) {
                final String[] rows = line.split("\\t");
                lineHandler.accept(rows);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String getLunarEclipseName(String shortcut, Lang lang) {
        switch (shortcut) {
            case "T":
                return messagesApi.get(lang, "events.lunareclipse.total");
            case "P":
                return messagesApi.get(lang, "events.lunareclipse.partial");
            default:
                throw new RuntimeException(shortcut + " is no known eclipse type");
        }
    }

    private String getSolarEclipseName(String shortcut, Lang lang) {
        switch (shortcut) {
            case "T":
                return messagesApi.get(lang, "events.solareclipse.total");
            case "P":
                return messagesApi.get(lang, "events.solareclipse.partial");
            case "A":
                return messagesApi.get(lang, "events.solareclipse.annular");
            case "H":
                return messagesApi.get(lang, "events.solareclipse.hybrid");
            default:
                throw new RuntimeException(shortcut + " is no known eclipse type");
        }
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<EventInstance> eventCollection, Lang lang) {
        if (requestForm.includeEvent(EventType.LUNARECLIPSE)) {
            findEventsInMap(requestForm, eventCollection, this.lunarEclipses, lang);
        }
        if (requestForm.includeEvent(EventType.SOLARECLIPSE)) {
            findEventsInMap(requestForm, eventCollection, this.solarEclipses, lang);
        }
        if (requestForm.includeEvent(EventType.MOONLANDING)) {
            findEventsInMap(requestForm, eventCollection, moonLandings, lang);
        }
    }

    private static void findEventsInMap(RequestForm requestForm, Collection<EventInstance> eventCollection, TreeMap<ZonedDateTime, EventTemplate> map, Lang lang) {
        for (EventTemplate event : map.tailMap(requestForm.getFrom()).values()) {
            if (event.getDateTime().isBefore(requestForm.getTo())) {
                eventCollection.add(new EventInstance(event, requestForm.getFrom().getZone(), lang));
            } else {
                break;
            }
        }
    }

}
