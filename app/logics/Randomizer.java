package logics;

import org.jetbrains.annotations.TestOnly;

import java.util.Random;

public class Randomizer {

	private static final Random random = new Random();

	private Randomizer() {
	}

	@SafeVarargs
	public static <T> T chooseRandom(T... list) {
		return list[random.nextInt(list.length)];
	}

	@TestOnly
	public static void reseed() {
		random.setSeed(1337);
	}
}
