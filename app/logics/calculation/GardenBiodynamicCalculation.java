package logics.calculation;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import logics.astronomy.Element;
import logics.astronomy.Ephemeris;
import models.*;
import org.jetbrains.annotations.NotNull;
import play.i18n.Lang;
import play.i18n.MessagesApi;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

// Singleton so the eclipse CSVs are parsed once rather than on every calendar request
// (the class holds no per-request state after construction).
@Singleton
public class GardenBiodynamicCalculation extends Calculation {

    private final Ephemeris ephemeris;
    private final TreeSet<ZonedDateTime> eclipses = new TreeSet<>();

    @Inject
    public GardenBiodynamicCalculation(Ephemeris ephemeris, MessagesApi messagesApi) {
        super(messagesApi);
        this.ephemeris = ephemeris;
        loadEclipses();
    }

    private void loadEclipses() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d.M.u'T'H:m:s");
        for (String name : new String[]{"lunar-eclipses/lunar-eclipses.csv", "solar-eclipses/solar-eclipses.csv"}) {
            CSVUtil.load(Objects.requireNonNull(getClass().getResource(name)).getFile(), rows ->
                    eclipses.add(LocalDateTime.parse(rows[0], fmt).atZone(ZoneOffset.UTC)));
        }
    }

    @Override
    public void calculate(RequestForm requestForm, Collection<EventInstance> eventCollection) {
        if (!requestForm.includeEvent(EventType.GARDEN_BIODYNAMIC)) {
            return;
        }
        ZoneId zone = requestForm.getFrom().getZone();
        Lang lang = requestForm.getLang();
        Hemisphere hemisphere = requestForm.getHemisphere();

        Instant from = requestForm.getFrom().toInstant();
        Instant to = requestForm.getTo().toInstant();

        // "Bad garden days": whole local days that contain a lunar-node crossing or an eclipse.
        // In the Maria Thun tradition these override the plant-part classification entirely.
        SortedSet<LocalDate> badDays = collectBadDays(from, to, zone);

        // Plant-part periods, split so that none of them covers a bad garden day.
        Instant firstBoundary = ephemeris.nextSignBoundaryCrossing(from, to);
        Element startElement = Element.forSiderealLongitude(ephemeris.moonSiderealLongitude(from));
        emitPlantPartPeriod(from, firstBoundary != null ? firstBoundary : to, startElement, badDays, zone, lang, hemisphere, eventCollection);
        Instant boundary = firstBoundary;
        while (boundary != null) {
            Element entered = Element.forSiderealLongitude(
                    ephemeris.moonSiderealLongitude(boundary.plusSeconds(120)));
            Instant nextBoundary = ephemeris.nextSignBoundaryCrossing(boundary.plusSeconds(120), to);
            emitPlantPartPeriod(boundary, nextBoundary != null ? nextBoundary : to, entered, badDays, zone, lang, hemisphere, eventCollection);
            boundary = nextBoundary;
        }

        // One all-day "unfavourable garden day" event per bad garden day.
        for (LocalDate bad : badDays) {
            eventCollection.add(buildBadDayEvent(bad, zone, lang));
        }
    }

    /** Local calendar days (in {@code zone}) blacked out by a lunar node crossing or an eclipse within [from, to). */
    private SortedSet<LocalDate> collectBadDays(Instant from, Instant to, ZoneId zone) {
        SortedSet<LocalDate> days = new TreeSet<>();
        Instant cursor = from;
        while (cursor.isBefore(to)) {
            Instant node = ephemeris.nextNodeCrossing(cursor, to);
            if (node == null) {
                break;
            }
            days.add(node.atZone(zone).toLocalDate());
            cursor = node.plusSeconds(120);
        }
        for (ZonedDateTime eclipse : eclipses.subSet(from.atZone(ZoneOffset.UTC), to.atZone(ZoneOffset.UTC))) {
            days.add(eclipse.withZoneSameInstant(zone).toLocalDate());
        }
        return days;
    }

    /**
     * Emits plant-part events for the interval [start, periodEnd), carving out every bad garden day so that
     * no plant-part event covers one. A node day inside a sign period therefore splits it into the segments
     * before and after that day.
     */
    private void emitPlantPartPeriod(Instant start, Instant periodEnd, Element element, SortedSet<LocalDate> badDays,
                                     ZoneId zone, Lang lang, Hemisphere hemisphere, Collection<EventInstance> out) {
        LocalDate firstDay = start.atZone(zone).toLocalDate();
        LocalDate lastDay = periodEnd.atZone(zone).toLocalDate();
        Instant segStart = start;
        for (LocalDate bad : badDays.subSet(firstDay, lastDay.plusDays(1))) {
            Instant badStart = latest(bad.atStartOfDay(zone).toInstant(), start);
            Instant badEnd = earliest(bad.plusDays(1).atStartOfDay(zone).toInstant(), periodEnd);
            if (!badStart.isBefore(badEnd)) {
                continue; // bad day does not actually overlap this period
            }
            if (segStart.isBefore(badStart)) {
                // End one second before midnight so the segment's last local day is the day before
                // the bad day (an end at exactly 00:00 would otherwise render on the bad day itself).
                out.add(buildEvent(segStart, badStart.minusSeconds(1), element, zone, lang, hemisphere));
            }
            if (segStart.isBefore(badEnd)) {
                segStart = badEnd;
            }
        }
        if (segStart.isBefore(periodEnd)) {
            out.add(buildEvent(segStart, periodEnd, element, zone, lang, hemisphere));
        }
    }

    private static Instant latest(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private static Instant earliest(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    private EventInstance buildEvent(Instant start, Instant periodEnd, Element element, ZoneId zone,
                                     Lang lang, Hemisphere hemisphere) {
        ZonedDateTime startZ = start.atZone(zone);
        ZonedDateTime endZ = periodEnd.atZone(zone);
        String part = messagesApi.get(lang, element.plantPartKey());
        String title = element.emoji() + " " + part;

        StringBuilder d = new StringBuilder();
        d.append(messagesApi.get(lang, "garden.biodynamic." + element.name().toLowerCase() + ".guidance"));
        boolean ascending = isAscending(start, hemisphere);
        d.append("\n").append(messagesApi.get(lang,
                ascending ? "garden.biodynamic.ascending" : "garden.biodynamic.descending"));
        // perigee/apogee rest-period notes (node and eclipse days are handled as bad garden days instead)
        Ephemeris.Apsis apsis = ephemeris.nextApsis(start, periodEnd);
        if (apsis != null) {
            d.append("\n").append(messagesApi.get(lang,
                    apsis.perigee() ? "garden.biodynamic.buffer.perigee" : "garden.biodynamic.buffer.apogee"));
        }
        return new EventInstance(startZ, endZ, title, part, d.toString(), zone, "garden-biodynamic-" + element.name().toLowerCase());
    }

    private EventInstance buildBadDayEvent(LocalDate day, ZoneId zone, Lang lang) {
        ZonedDateTime startZ = day.atStartOfDay(zone);
        String title = "⚠️ " + messagesApi.get(lang, "garden.biodynamic.badday.title");
        String pdfTitle = messagesApi.get(lang, "garden.biodynamic.badday.pdf");
        String description = messagesApi.get(lang, "garden.biodynamic.badday.description");
        return new EventInstance(startZ, title, pdfTitle, description, zone, "garden-biodynamic-badday");
    }

    private boolean isAscending(@NotNull Instant t, Hemisphere hemisphere) {
        double slope = ephemeris.moonDeclination(t.plusSeconds(3600)) - ephemeris.moonDeclination(t);
        boolean ascending = slope > 0;
        if (hemisphere == Hemisphere.SOUTHERN) {
            ascending = !ascending;
        }
        return ascending;
    }
}
