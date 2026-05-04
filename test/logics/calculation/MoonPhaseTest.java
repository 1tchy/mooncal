package logics.calculation;

import models.Hemisphere;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoonPhaseTest {

    @Test
    void quarterEmoticonsSwapInSouthernHemisphere() {
        assertEquals("🌗", MoonPhase.FIRST_QUARTER.getEmoticon(Hemisphere.SOUTHERN));
        assertEquals("🌓", MoonPhase.LAST_QUARTER.getEmoticon(Hemisphere.SOUTHERN));
    }

    @Test
    void quarterEmoticonsUnchangedInNorthernHemisphere() {
        assertEquals("🌓", MoonPhase.FIRST_QUARTER.getEmoticon(Hemisphere.NORTHERN));
        assertEquals("🌗", MoonPhase.LAST_QUARTER.getEmoticon(Hemisphere.NORTHERN));
    }
}
