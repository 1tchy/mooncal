package controllers;

import logics.calculation.MoonPhasesCalculation;
import play.api.mvc.Action;
import play.api.mvc.AnyContent;
import play.mvc.Controller;

import javax.inject.Inject;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("unused") //used by routes
public class Favicon extends Controller {

    private static final List<Integer> MOON_VISIBILITIES_WITH_ICON = List.of(6, 8, 11, 14, 18, 22, 26, 31, 35, 40, 45, 51, 56, 61, 67, 72, 77, 82, 86, 90, 93, 96, 98, 99, 100, 101, 102, 104, 107, 110, 114, 118, 123, 128, 133, 139, 144, 150, 155, 161, 166, 171, 175, 179, 183, 187, 190, 192, 195);
    private static final List<String> MOON_ICONS = List.of("android-chrome-192x192.png", "android-chrome-512x512.png", "apple-touch-icon.png", "favicon.ico", "favicon-16x16.png", "favicon-32x32.png", "mstile-150x150.png", "safari-pinned-tab.svg");
    private final ConcurrentHashMap<String, Action<AnyContent>> currentIcons = new ConcurrentHashMap<>();

    @Inject
    public Favicon(MoonPhasesCalculation calculation, Assets assets) {
        @SuppressWarnings("resource") //must never terminate
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> updateCurrentIcons(calculation, assets), 0, 2, TimeUnit.HOURS);
    }

    private void updateCurrentIcons(MoonPhasesCalculation calculation, Assets assets) {
        int iconNumber = getNextMoonVisibilityWithIcon(calculation.getCurrentMoonPhase().getPhaseVisibilityPercentage());
        MOON_ICONS.forEach(moonIconFileName -> currentIcons.put(moonIconFileName, assets.at("/public/favicon/" + iconNumber, moonIconFileName, false)));
    }

    public Action<AnyContent> getIcon(String path) {
        return currentIcons.get(path);
    }

    private static int getNextMoonVisibilityWithIcon(int actual) {
        if (actual <= MOON_VISIBILITIES_WITH_ICON.getFirst()) {
            return MOON_VISIBILITIES_WITH_ICON.getFirst();
        }
        for (int i = 0; i < MOON_VISIBILITIES_WITH_ICON.size() - 1; i++) {
            float middleToNext = (MOON_VISIBILITIES_WITH_ICON.get(i) + MOON_VISIBILITIES_WITH_ICON.get(i + 1)) / 2f;
            if (actual < middleToNext) {
                return MOON_VISIBILITIES_WITH_ICON.get(i);
            }
        }
        return MOON_VISIBILITIES_WITH_ICON.getLast();
    }
}
