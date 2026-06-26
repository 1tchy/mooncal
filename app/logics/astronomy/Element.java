package logics.astronomy;

public enum Element {
    ROOT("garden.part.root", "🥕"),
    LEAF("garden.part.leaf", "🥬"),
    FLOWER("garden.part.flower", "🌸"),
    FRUIT("garden.part.fruit", "🍎");

    private final String plantPartKey;
    private final String emoji;

    Element(String plantPartKey, String emoji) {
        this.plantPartKey = plantPartKey;
        this.emoji = emoji;
    }

    public String plantPartKey() { return plantPartKey; }
    public String emoji() { return emoji; }

    /** Maps a sidereal longitude (any real degrees) to its element via equal-house signs.
     *  Classical assignment: Aries=Fire/Fruit, Taurus=Earth/Root, Gemini=Air/Flower, Cancer=Water/Leaf, repeating. */
    public static Element forSiderealLongitude(double siderealLongitudeDeg) {
        double n = ((siderealLongitudeDeg % 360) + 360) % 360;
        int signIndex = (int) Math.floor(n / 30.0);
        // classical: Aries=Fire/FRUIT, Taurus=Earth/ROOT, Gemini=Air/FLOWER, Cancer=Water/LEAF, repeating
        Element[] bySign = {FRUIT, ROOT, FLOWER, LEAF, FRUIT, ROOT, FLOWER, LEAF, FRUIT, ROOT, FLOWER, LEAF};
        return bySign[signIndex];
    }
}
