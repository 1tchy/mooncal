package logics.astronomy;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ElementTest {

    @Test
    void mapsSignsToElements() {
        assertEquals(Element.FRUIT,  Element.forSiderealLongitude(10));   // Aries (fire)
        assertEquals(Element.ROOT,   Element.forSiderealLongitude(40));   // Taurus (earth)
        assertEquals(Element.FLOWER, Element.forSiderealLongitude(70));   // Gemini (air)
        assertEquals(Element.LEAF,   Element.forSiderealLongitude(100));  // Cancer (water)
        assertEquals(Element.ROOT,   Element.forSiderealLongitude(280));  // Capricorn (earth)
    }

    @Test
    void normalizesAndWraps() {
        assertEquals(Element.forSiderealLongitude(10), Element.forSiderealLongitude(370));
        assertEquals(Element.FRUIT, Element.forSiderealLongitude(-350)); // == 10
    }
}
