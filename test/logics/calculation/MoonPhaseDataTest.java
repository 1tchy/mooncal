package logics.calculation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import play.test.WithApplication;

import static org.junit.jupiter.api.Assertions.*;

class MoonPhaseDataTest extends WithApplication {

    private MoonPhaseData data;

    @BeforeEach
    void setup() {
        startPlay();
        data = new MoonPhaseData();
    }

    @AfterEach
    void tearDown() {
        stopPlay();
    }

    @Test
    void loadsPhasesFromCsv() {
        assertFalse(data.phases().isEmpty());
        // CSV covers 1700..2100
        assertTrue(data.phases().firstKey().getYear() <= 1700);
        assertTrue(data.phases().lastKey().getYear() >= 2100);
    }
}
