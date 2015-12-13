package logics;

import com.google.common.io.LineReader;
import models.EventTemplate;
import models.RequestForm;
import models.ZonedEvent;
import play.i18n.Messages;

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
    private final TreeMap<ZonedDateTime, EventTemplate> moonLandings = new TreeMap<>();

    public MoonEventCalculation() {
        initializeLunarEclipses();
        initializeMoonLandings();
    }

    private void initializeLunarEclipses() {
        initializeByCVS("lunar-eclipses.csv", rows -> {
            final ZonedDateTime date = LocalDateTime.parse(rows[0], DATE_TIME_PATTERN).atZone(ZoneOffset.UTC);
            final String name = getEclipseName(rows[1]);
            lunarEclipses.put(date, new EventTemplate(date, name, zoneId -> eventAt(date, name, zoneId)));
        });
    }

    private void initializeMoonLandings() {
        initializeByCVS("moon-landings.csv", rows -> {
            final ZonedDateTime date = LocalDateTime.parse(rows[0], DATE_TIME_PATTERN).atZone(ZoneOffset.UTC);
            moonLandings.put(date, new EventTemplate(date, rows[1], zoneId -> rows[2]));
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

    private static String getEclipseName(String shortcut) {
        switch (shortcut) {
            case "T":
                return Messages.get("events.lunareclipse.total");
            case "P":
                return Messages.get("events.lunareclipse.partial");
            default:
                throw new RuntimeException(shortcut + " is no known eclipse type");
        }
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<ZonedEvent> eventCollection) {
        if (requestForm.includeEvent("lunareclipse")) {
            findEventsInMap(requestForm, eventCollection, this.lunarEclipses);
        }
        if (requestForm.includeEvent("moonlanding")) {
            findEventsInMap(requestForm, eventCollection, moonLandings);
        }
    }

    private static void findEventsInMap(RequestForm requestForm, Collection<ZonedEvent> eventCollection, TreeMap<ZonedDateTime, EventTemplate> map) {
        for (EventTemplate event : map.tailMap(requestForm.getFrom()).values()) {
            if (event.getDateTime().isBefore(requestForm.getTo())) {
                eventCollection.add(new ZonedEvent(event, requestForm.getFrom().getZone()));
            } else {
                break;
            }
        }
    }

}
