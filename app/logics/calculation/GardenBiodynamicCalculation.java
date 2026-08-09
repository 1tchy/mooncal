package logics.calculation;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import logics.astronomy.Element;
import logics.astronomy.Ephemeris;
import models.*;
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

    // Appended after the plant emoji to mark the Moon's declination phase: 🔼 ascending, 🔽 descending.
    // Public so the PDF renderer can recognise the phase of an event from its title.
    public static final String ASCENDING_MOON_MARKER = "🔼";
    public static final String DESCENDING_MOON_MARKER = "🔽";

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

        Instant from = requestForm.getFrom().toInstant();
        Instant to = requestForm.getTo().toInstant();

        // "Bad garden days": whole local days that contain a lunar-node crossing or an eclipse.
        // In the Maria Thun tradition these override the plant-part classification entirely.
        SortedSet<LocalDate> badDays = collectBadDays(from, to, zone);

        // Split the timeline both at sidereal sign boundaries (which change the plant part) and at the
        // Moon's declination turning points (which flip ascending<->descending), so that within each
        // segment both the plant part and the ascending/descending phase are constant.
        TreeSet<Instant> splits = new TreeSet<>();
        for (Instant b = ephemeris.nextSignBoundaryCrossing(from, to); b != null;
             b = ephemeris.nextSignBoundaryCrossing(b.plusSeconds(120), to)) {
            splits.add(b);
        }
        for (Instant e = ephemeris.nextDeclinationExtreme(from, to); e != null;
             e = ephemeris.nextDeclinationExtreme(e.plusSeconds(120), to)) {
            splits.add(e);
        }

        Instant segmentStart = from;
        for (Instant boundary : splits) {
            emitSegment(segmentStart, boundary, badDays, zone, lang, eventCollection);
            segmentStart = boundary;
        }
        emitSegment(segmentStart, to, badDays, zone, lang, eventCollection);

        // One all-day "unfavourable garden day" event per bad garden day.
        for (LocalDate bad : badDays) {
            eventCollection.add(buildBadDayEvent(bad, zone, lang));
        }
    }

    /** Classify a constant-part, constant-phase segment and emit its plant-part events (carving bad days). */
    private void emitSegment(Instant start, Instant end, SortedSet<LocalDate> badDays, ZoneId zone, Lang lang,
                             Collection<EventInstance> out) {
        if (!start.isBefore(end)) {
            return;
        }
        // Sample just inside the segment so we are clear of the boundary instant.
        Instant probe = start.plusSeconds(120).isBefore(end) ? start.plusSeconds(120) : start;
        Element element = Element.forSiderealLongitude(ephemeris.moonSiderealLongitude(probe));
        boolean ascending = isDeclinationRising(probe);
        emitPlantPartPeriod(start, end, element, ascending, badDays, zone, lang, out);
    }

    /** Ascending Moon: its declination is rising (moving toward its northernmost point). Same worldwide. */
    private boolean isDeclinationRising(Instant t) {
        return ephemeris.moonDeclination(t.plusSeconds(3600)) - ephemeris.moonDeclination(t) > 0;
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
    private void emitPlantPartPeriod(Instant start, Instant periodEnd, Element element, boolean ascending,
                                     SortedSet<LocalDate> badDays, ZoneId zone, Lang lang, Collection<EventInstance> out) {
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
                out.add(buildEvent(segStart, badStart.minusSeconds(1), element, ascending, zone, lang));
            }
            if (segStart.isBefore(badEnd)) {
                segStart = badEnd;
            }
        }
        if (segStart.isBefore(periodEnd)) {
            out.add(buildEvent(segStart, periodEnd, element, ascending, zone, lang));
        }
    }

    private static Instant latest(Instant a, Instant b) {
        return a.isAfter(b) ? a : b;
    }

    private static Instant earliest(Instant a, Instant b) {
        return a.isBefore(b) ? a : b;
    }

    private EventInstance buildEvent(Instant start, Instant periodEnd, Element element, boolean ascending,
                                     ZoneId zone, Lang lang) {
        ZonedDateTime startZ = start.atZone(zone);
        ZonedDateTime endZ = periodEnd.atZone(zone);
        String part = messagesApi.get(lang, element.plantPartKey());
        // Mark the declination phase with a second emoji after the plant emoji.
        String title = element.emoji() + (ascending ? ASCENDING_MOON_MARKER : DESCENDING_MOON_MARKER) + " " + part;

        StringBuilder d = new StringBuilder();
        d.append(messagesApi.get(lang, "garden.biodynamic." + element.name().toLowerCase() + ".guidance"));
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
}
