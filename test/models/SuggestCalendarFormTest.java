package models;

import org.junit.jupiter.api.Test;
import play.data.validation.ValidationError;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class SuggestCalendarFormTest {

    private SuggestCalendarForm valid() {
        SuggestCalendarForm f = new SuggestCalendarForm();
        f.setTitle("Tide planting");
        f.setMethodology("Follows the lunar day and tidal extremes.");
        f.setSource("https://example.org/method");
        return f;
    }

    @Test
    void validWhenRequiredPresentAndNoEmail() {
        assertNull(valid().validate());
    }

    @Test
    void invalidEmailRejected() {
        SuggestCalendarForm f = valid();
        f.setContactEmail("not-an-email");
        List<ValidationError> errors = f.validate();
        assertNotNull(errors);
        assertFalse(errors.isEmpty());
    }

    @Test
    void validEmailAccepted() {
        SuggestCalendarForm f = valid();
        f.setContactEmail("a@b.com");
        assertNull(f.validate());
    }

    @Test
    void toLogLineDoesNotProduceMultipleLinesWhenFieldContainsNewline() {
        SuggestCalendarForm f = valid();
        f.setNotes("line1\nINJECTED");
        f.setOriginCulture("culture\rwith-cr");
        String logLine = f.toLogLine();
        assertNotNull(logLine);
        assertEquals(1, logLine.lines().count(), "toLogLine() must produce a single line even when user input contains newlines");
        assertFalse(logLine.contains("\n"), "Log line must not contain newline characters");
        assertFalse(logLine.contains("\r"), "Log line must not contain carriage return characters");
    }
}
