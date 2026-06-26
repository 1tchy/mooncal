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
}
