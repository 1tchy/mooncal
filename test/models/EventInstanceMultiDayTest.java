package models;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventInstanceMultiDayTest {

    private static final ZoneOffset UTC = ZoneOffset.UTC;

    private EventInstance single() {
        return new EventInstance(
                ZonedDateTime.of(2026, 1, 5, 10, 0, 0, 0, UTC),
                "Title", "PDF", "desc", UTC, "garden-test");
    }

    private EventInstance multiDay() {
        return new EventInstance(
                ZonedDateTime.of(2026, 1, 5, 10, 0, 0, 0, UTC),
                ZonedDateTime.of(2026, 1, 7, 18, 0, 0, 0, UTC),
                "Title", "PDF", "desc", UTC, "garden-test");
    }

    private EventInstance sameDayEnd() {
        return new EventInstance(
                ZonedDateTime.of(2026, 1, 5, 10, 0, 0, 0, UTC),
                ZonedDateTime.of(2026, 1, 5, 22, 0, 0, 0, UTC),
                "Title", "PDF", "desc", UTC, "garden-test");
    }

    private EventInstance nullEnd() {
        return new EventInstance(
                ZonedDateTime.of(2026, 1, 5, 10, 0, 0, 0, UTC),
                null,
                "Title", "PDF", "desc", UTC, "garden-test");
    }

    @Test
    void singleDayIsNotMultiDay() {
        assertFalse(single().isMultiDay());
    }

    @Test
    void nullEndIsNotMultiDay() {
        assertFalse(nullEnd().isMultiDay());
    }

    @Test
    void sameDayEndIsNotMultiDay() {
        assertFalse(sameDayEnd().isMultiDay());
    }

    @Test
    void laterEndDateIsMultiDay() {
        assertTrue(multiDay().isMultiDay());
    }

    @Test
    void getEndLocalDateReturnStartWhenNull() {
        EventInstance e = single();
        assertEquals(e.getLocalDate(), e.getEndLocalDate());
    }

    @Test
    void getEndLocalDateReturnEndWhenSet() {
        assertEquals(LocalDate.of(2026, 1, 7), multiDay().getEndLocalDate());
    }

    @Test
    void getEndDateTimeNullForSingleDay() {
        assertNull(single().getEndDateTime());
    }

    @Test
    void getEndDateTimeNormalizedToTimezone() {
        // endDateTime is normalized to event timezone
        EventInstance e = multiDay();
        assertNotNull(e.getEndDateTime());
        assertEquals(UTC, e.getEndDateTime().getZone());
    }

    @Test
    void getEndDateIsNullForSingleDay() {
        assertNull(single().getEndDate());
        assertNull(nullEnd().getEndDate());
        assertNull(sameDayEnd().getEndDate());
    }

    @Test
    void getEndDateIsFormattedEndForMultiDay() {
        assertEquals("2026-01-07T18:00", multiDay().getEndDate());
    }

    @Test
    void getDateIsLocalWallClockDateTime() {
        assertEquals("2026-01-05T10:00", single().getDate());
    }

    @Test
    void existingConstructorStillProducesSingleDay() {
        EventInstance e = new EventInstance(
                ZonedDateTime.of(2026, 3, 10, 8, 0, 0, 0, UTC),
                "T", "P", null, UTC, "fullmoon");
        assertFalse(e.isMultiDay());
        assertNull(e.getEndDateTime());
    }

    private EventInstance garden(ZonedDateTime start, ZonedDateTime end, String type) {
        return new EventInstance(start, end, "t", "t", "d", UTC, type);
    }

    @Test
    void gardenPeriodStartingLateAtNightDropsItsFirstDisplayDay() {
        // Fruit->Root boundary at 2026-08-06 22:25: the root period only just touches Aug 6.
        EventInstance root = garden(
                ZonedDateTime.of(2026, 8, 6, 22, 25, 0, 0, UTC),
                ZonedDateTime.of(2026, 8, 9, 0, 20, 0, 0, UTC), "garden-biodynamic-root");
        assertEquals(LocalDate.of(2026, 8, 7), root.getDisplayStartLocalDate());
        assertEquals(LocalDate.of(2026, 8, 8), root.getDisplayEndLocalDate()); // ends 00:20 < 06:00 -> Aug 8
    }

    @Test
    void gardenPeriodEndingLateEveningKeepsItsLastDisplayDay() {
        EventInstance fruit = garden(
                ZonedDateTime.of(2026, 8, 4, 18, 25, 0, 0, UTC),
                ZonedDateTime.of(2026, 8, 6, 22, 25, 0, 0, UTC), "garden-biodynamic-fruit");
        assertEquals(LocalDate.of(2026, 8, 4), fruit.getDisplayStartLocalDate());
        assertEquals(LocalDate.of(2026, 8, 6), fruit.getDisplayEndLocalDate()); // ends 22:25 -> keeps Aug 6
    }

    @Test
    void nonGardenMultiDayEventIsNotTrimmed() {
        EventInstance e = garden(
                ZonedDateTime.of(2026, 8, 6, 23, 0, 0, 0, UTC),
                ZonedDateTime.of(2026, 8, 9, 5, 0, 0, 0, UTC), "fullmoon");
        assertEquals(LocalDate.of(2026, 8, 6), e.getDisplayStartLocalDate());
        assertEquals(LocalDate.of(2026, 8, 9), e.getDisplayEndLocalDate());
    }
}
