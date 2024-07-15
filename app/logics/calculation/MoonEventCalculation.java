package logics.calculation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.LineReader;
import models.EventInstance;
import models.EventTemplate;
import models.EventType;
import models.RequestForm;
import org.jetbrains.annotations.TestOnly;
import play.Logger;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import javax.inject.Inject;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MoonEventCalculation extends Calculation {

    private static final Logger.ALogger logger = Logger.of(MethodHandles.lookup().lookupClass());
    private final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("d.M.u'T'H:m:s");
    private final Translator translator = new Translator();
    private final TreeMap<ZonedDateTime, EventTemplate> lunarEclipses = new TreeMap<>();
    private final TreeMap<ZonedDateTime, EventTemplate> solarEclipses = new TreeMap<>();
    private final TreeMap<ZonedDateTime, EventTemplate> moonLandings = new TreeMap<>();

    @Inject
    public MoonEventCalculation(MessagesApi messagesApi) {
        super(messagesApi);
        initializeLunarEclipses();
        initializeSolarEclipses();
        initializeMoonLandings();
        //noinspection resource
        Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, MethodHandles.lookup().lookupClass().getSimpleName());
            thread.setDaemon(true);
            return thread;
        }).scheduleAtFixedRate(() -> updateMoonLandings("ll.thespacedevs.com"), 1, 4 * 60, TimeUnit.MINUTES);
    }

    private void initializeLunarEclipses() {
        initializeByCVS(Objects.requireNonNull(getClass().getResource("lunar-eclipses/lunar-eclipses.csv")).getFile(), rows -> {
            final ZonedDateTime date = LocalDateTime.parse(rows[0], DATE_TIME_PATTERN).atZone(ZoneOffset.UTC);
            lunarEclipses.put(date, new EventTemplate.WithZoneId(date, (zoneId, lang) -> getLunarEclipseName(rows[1], lang), (zoneId, lang) -> eventAt(date, getLunarEclipseName(rows[1], lang), zoneId, lang), "lunar-eclipse"));
        });
    }

    private void initializeSolarEclipses() {
        initializeByCVS(Objects.requireNonNull(getClass().getResource("solar-eclipses/solar-eclipses.csv")).getFile(), rows -> {
            final ZonedDateTime date = LocalDateTime.parse(rows[0], DATE_TIME_PATTERN).atZone(ZoneOffset.UTC);
            solarEclipses.put(date, new EventTemplate.WithZoneId(date, (zoneId, lang) -> getSolarEclipseName(rows[1], lang), (zoneId, lang) -> eventAt(date, getSolarEclipseName(rows[1], lang), zoneId, lang), "solar-eclipse"));
        });
    }

    private void initializeMoonLandings() {
        Optional<String> updatedFile = Optional.ofNullable(System.getProperty("updated-moon-landings.csv"));
        initializeByCVS(updatedFile.orElseGet(() -> Objects.requireNonNull(getClass().getResource("moon-landings/moon-landings.csv")).getFile()), rows -> {
            final ZonedDateTime date = LocalDateTime.parse(rows[0], DATE_TIME_PATTERN).atZone(ZoneOffset.UTC);
            moonLandings.put(date, new EventTemplate.WithoutZoneId(date,
                    getByLang("🚀 " + rows[1], "🚀 " + rows[3], "🚀 " + rows[5], "🚀 " + rows[7], "🚀 " + rows[9], "🚀 " + rows[11]),
                    getByLang(rows[2], rows[4], rows[6], rows[8], rows[10], rows[12]),
                    "moon-landing"));
        });
    }

    private static Function<Lang, String> getByLang(String en, String de, String nl, String es, String fr, String ro) {
        return lang -> switch (lang.code()) {
            case "de" -> de;
            case "nl" -> nl;
            case "es" -> es;
            case "fr" -> fr;
            case "ro" -> ro;
            default -> en;
        };
    }

    @VisibleForTesting
    void updateMoonLandings(String baseUrl) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode jsonNode = mapper.readTree(URI.create("https://" + baseUrl + "/2.2.0/event/?type=7").toURL());
            ArrayNode results = (ArrayNode) jsonNode.get("results");
            while (!jsonNode.get("next").isNull()) {
                jsonNode = mapper.readTree(URI.create(jsonNode.get("next").asText()).toURL());
                results.addAll((ArrayNode) jsonNode.get("results"));
            }
            Set<LocalDate> eventDatesAlreadyKnown = moonLandings.keySet().stream().map(d -> d.toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC).toLocalDate()).collect(Collectors.toSet());
            for (JsonNode result : results) {
                if (result.hasNonNull("date_precision")) {
                    int precision = result.get("date_precision").get("id").asInt();
                    if (precision > 2) { // 2 == Hour, see https://ll.thespacedevs.com/2.2.0/config/netprecision/
                        continue;
                    }
                }
                ZonedDateTime date = OffsetDateTime.parse(result.get("date").asText()).atZoneSameInstant(ZoneOffset.UTC);
                if (date.getYear() < 2024 || eventDatesAlreadyKnown.contains(date.toLocalDate())) {
                    continue;
                }
                String name = result.get("name").asText();
                String descriptionEN = result.get("description").asText().replaceAll("\r", "").replaceAll("\n", " ");
                String descriptionDE = translator.translate("en", "de", descriptionEN);
                String descriptionNL = translator.translate("en", "nl", descriptionEN);
                String descriptionES = translator.translate("en", "es", descriptionEN);
                String descriptionFR = translator.translate("en", "fr", descriptionEN);
                String descriptionRO = translator.translate("en", "ro", descriptionEN);
                moonLandings.put(date, new EventTemplate.WithoutZoneId(date, lang -> "🚀 " + name, lang -> getByLang(descriptionEN, descriptionDE, descriptionNL, descriptionES, descriptionFR, descriptionRO).apply(lang), "moon-landing"));
            }
        } catch (IOException e) {
            logger.error("Could not update moon landings", e);
        }
    }

    @TestOnly
    void removeLatestMoonLanding() {
        moonLandings.remove(moonLandings.lastKey());
    }

    private void initializeByCVS(String file, Consumer<String[]> lineHandler) {
        try (final FileReader fileReader = new FileReader(file)) {
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
        return switch (shortcut) {
            case "T" -> messagesApi.get(lang, "events.lunareclipse.total");
            case "P" -> messagesApi.get(lang, "events.lunareclipse.partial");
            default -> throw new RuntimeException(shortcut + " is no known eclipse type");
        };
    }

    private String getSolarEclipseName(String shortcut, Lang lang) {
        return switch (shortcut) {
            case "T" -> messagesApi.get(lang, "events.solareclipse.total");
            case "P" -> messagesApi.get(lang, "events.solareclipse.partial");
            case "A" -> messagesApi.get(lang, "events.solareclipse.annular");
            case "H" -> messagesApi.get(lang, "events.solareclipse.hybrid");
            default -> throw new RuntimeException(shortcut + " is no known eclipse type");
        };
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<EventInstance> eventCollection) {
        if (requestForm.includeEvent(EventType.LUNARECLIPSE)) {
            findEventsInMap(requestForm, eventCollection, this.lunarEclipses, requestForm.getLang());
        }
        if (requestForm.includeEvent(EventType.SOLARECLIPSE)) {
            findEventsInMap(requestForm, eventCollection, this.solarEclipses, requestForm.getLang());
        }
        if (requestForm.includeEvent(EventType.MOONLANDING)) {
            findEventsInMap(requestForm, eventCollection, moonLandings, requestForm.getLang());
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
