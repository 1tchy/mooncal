package models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EventTypeTest {
    @Test
    void gardenTypesAreReadableByKey() {
        assertEquals(EventType.GARDEN_BIODYNAMIC, EventType.read("garden-biodynamic"));
    }
}
