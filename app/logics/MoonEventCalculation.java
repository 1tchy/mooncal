package logics;

import com.google.common.io.LineReader;
import models.Event;
import models.RequestForm;

import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.TreeMap;
import java.util.function.Consumer;

public class MoonEventCalculation implements Calculation {

    public static final DateTimeFormatter DATE_TIME_PATTERN = DateTimeFormatter.ofPattern("d.M.u'T'H:m:s");
    TreeMap<ZonedDateTime, Event> lunarEclipses = new TreeMap<>();

    TreeMap<ZonedDateTime, Event> moonLandings = new TreeMap<>();

    public MoonEventCalculation() {
        initializeLunarEclipses();
        initializeMoonLandings();
    }

    private void initializeLunarEclipses() {
        initializeByCVS("lunar-eclipses.csv", rows -> {
            final ZonedDateTime date = LocalDateTime.parse(rows[0], DATE_TIME_PATTERN).atZone(ZoneOffset.UTC);
            lunarEclipses.put(date, new Event(date, getEclipseName(rows[1]), null));
        });
    }

    private void initializeMoonLandings() {
        initializeByCVS("moon-landings.csv", rows -> {
            final ZonedDateTime date = LocalDateTime.parse(rows[0], DATE_TIME_PATTERN).atZone(ZoneOffset.UTC);
            moonLandings.put(date, new Event(date, rows[1], rows[2]));
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
                return "Totale Mondfinsternis";
            case "P":
                return "Partielle Mondfinsternis";
            default:
                throw new RuntimeException(shortcut + " is no known eclipse type");
        }
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<Event> eventCollection) {
        if (requestForm.includeEvent("Mondfinsternis")) {
            findEventsInMap(requestForm, eventCollection, this.lunarEclipses);
        }
        if (requestForm.includeEvent("Mondlandung")) {
            findEventsInMap(requestForm, eventCollection, moonLandings);
        }
    }

    private static void findEventsInMap(RequestForm requestForm, Collection<Event> eventCollection, TreeMap<ZonedDateTime, Event> map) {
        for (Event lunarEclipseEvent : map.tailMap(requestForm.getFrom()).values()) {
            if (lunarEclipseEvent.getDateTime().isBefore(requestForm.getTo())) {
                eventCollection.add(lunarEclipseEvent);
            } else {
                break;
            }
        }
    }

}
