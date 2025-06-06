package logics.calculation;

import models.EventInstance;
import models.EventStyle;
import models.MoonPhaseType;
import models.RequestForm;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.test.WithApplication;

import java.time.*;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MoonPhasesCalculationTest extends WithApplication {

    private static final ZoneId CH_ZONE = ZoneId.of("Europe/Zurich");
    private MoonPhasesCalculation cut;
    private RequestForm requestForm;

    @Before
    public void setup() {
        final MessagesApi messagesApi = mock(MessagesApi.class);
        when(messagesApi.get(any(Lang.class), anyString(), any(Object[].class))).thenAnswer(invocation -> {
            final Object[] arguments = invocation.getArguments();
            return arguments[arguments.length - 1];
        });
        cut = new MoonPhasesCalculation(messagesApi);
        requestForm = new RequestForm();
        requestForm.getPhases().put(MoonPhaseType.FULLMOON, true);
        requestForm.getPhases().put(MoonPhaseType.NEWMOON, true);
        requestForm.setFrom(ZonedDateTime.of(2015, 10, 20, 12, 0, 0, 0, ZoneOffset.UTC));
        requestForm.setTo(ZonedDateTime.of(2015, 11, 30, 12, 0, 0, 0, ZoneOffset.UTC));
        requestForm.setLang(Lang.forCode("en"));
    }

    private Collection<EventInstance> calculate(RequestForm requestForm) {
        final Collection<EventInstance> eventCollection = new TreeSet<>();
        cut.calculate(requestForm, eventCollection);
        return eventCollection;
    }

    @Test
    public void testGetFullmoonWithNoResult() {
        requestForm.setFrom(ZonedDateTime.of(2015, 11, 12, 12, 0, 0, 0, ZoneOffset.UTC));
        requestForm.setTo(ZonedDateTime.of(2015, 11, 20, 12, 0, 0, 0, ZoneOffset.UTC));
        assertThat(calculate(requestForm), is(empty()));
    }

    @Test
    public void testGetFullmoonWithOneResult() {
        requestForm.setFrom(ZonedDateTime.of(2015, 11, 20, 12, 0, 0, 0, ZoneOffset.UTC));
        final Collection<EventInstance> actual = calculate(requestForm);
        assertThat(actual, contains(eventAt(LocalDate.of(2015, 11, 25))));
    }

    @Test
    public void testGetMoonWithMoreResult() {
        final Collection<EventInstance> actual = calculate(requestForm);
        assertThat(actual, containsInAnyOrder(eventAt(LocalDate.of(2015, 10, 27)), eventAt(LocalDate.of(2015, 11, 11)), eventAt(LocalDate.of(2015, 11, 25))));
    }

    @Test
    public void testGetFullmoonNames() {
        requestForm = new RequestForm();
        requestForm.getPhases().put(MoonPhaseType.FULLMOON, true);
        requestForm.setFrom(ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        requestForm.setTo(ZonedDateTime.of(2026, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC));
        requestForm.setLang(Lang.forCode("en"));
        requestForm.setStyle(EventStyle.FULLMOON.getStyle());
        Collection<EventInstance> actual = calculate(requestForm);
        assertThat(actual, hasSize(13));
        List<String> actualTitles = actual.stream().map(EventInstance::getTitle).distinct().toList();
        assertEquals(List.of("🌕 phases.full"), actualTitles);
    }

    @Test
    public void testGetFullmoonNamesWithDescription() {
        requestForm = new RequestForm();
        requestForm.getPhases().put(MoonPhaseType.FULLMOON, true);
        requestForm.setFrom(ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        requestForm.setTo(ZonedDateTime.of(2026, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC));
        requestForm.setLang(Lang.forCode("en"));
        requestForm.setStyle(EventStyle.WITH_DESCRIPTION.getStyle());
        Collection<EventInstance> actual = calculate(requestForm);
        assertEquals(IntStream.of(1, 2, 3, 4, 5, 13, 6, 7, 8, 9, 10, 11, 12).mapToObj(i -> "🌕 phases.full (phases.full." + i + ")").toList(), actual.stream().map(EventInstance::getTitle).toList());
    }

    @Test
    public void testGetIconOnly() {
        requestForm = new RequestForm();
        requestForm.getPhases().put(MoonPhaseType.FULLMOON, true);
        requestForm.getPhases().put(MoonPhaseType.NEWMOON, true);
        requestForm.getPhases().put(MoonPhaseType.QUARTER, true);
        requestForm.setFrom(ZonedDateTime.of(2026, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC));
        requestForm.setTo(ZonedDateTime.of(2026, 12, 31, 23, 59, 59, 0, ZoneOffset.UTC));
        requestForm.setLang(Lang.forCode("en"));
        requestForm.setStyle(EventStyle.ICON_ONLY.getStyle());
        Collection<EventInstance> actual = calculate(requestForm);
        assertEquals(List.of("🌕", "🌗", "🌑", "🌓"), actual.stream().map(EventInstance::getTitle).distinct().toList());
    }

    @Test
    public void testGetMoonWithResultsInTemporalOrder() {
        final Collection<EventInstance> actual = calculate(requestForm);
        assertThat(actual, contains(eventAt(LocalDate.of(2015, 10, 27)), eventAt(LocalDate.of(2015, 11, 11)), eventAt(LocalDate.of(2015, 11, 25))));
    }

    @Test
    public void testGetHalfMoonResults() {
        requestForm.getPhases().clear();
        requestForm.getPhases().put(MoonPhaseType.QUARTER, true);
        requestForm.setTo(ZonedDateTime.of(2015, 10, 31, 12, 0, 0, 0, ZoneOffset.UTC));

        final Collection<EventInstance> actual = calculate(requestForm);

        assertThat(actual, contains(eventAt(LocalDate.of(2015, 10, 20))));
    }

    @Test
    public void testDailyEventsProvidedForAllDays() {
        requestForm.getPhases().clear();
        requestForm.getPhases().put(MoonPhaseType.DAILY, true);

        final Collection<EventInstance> actual = calculate(requestForm);

        assertThat(actual, hasSize(42));
    }

    @Test
    public void testEventOnLapYear() {
        requestForm.getPhases().clear();
        requestForm.getPhases().put(MoonPhaseType.DAILY, true);
        requestForm.setFrom(ZonedDateTime.of(2016, 2, 29, 12, 0, 0, 0, ZoneOffset.UTC));
        requestForm.setTo(ZonedDateTime.of(2016, 3, 1, 12, 0, 0, 0, ZoneOffset.UTC));
        requestForm.setLang(Lang.forCode("en"));

        final Collection<EventInstance> actual = calculate(requestForm);

        assertThat(actual.stream().map(EventInstance::getTitle).collect(Collectors.toList()), containsInAnyOrder("55", "64"));
    }

    @Test
    public void testCalculatedAndCsvFullMoons() {
        requestForm.getPhases().clear();
        requestForm.getPhases().put(MoonPhaseType.FULLMOON, true);
        requestForm.setFrom(ZonedDateTime.of(1699, 11, 1, 12, 0, 0, 0, CH_ZONE));
        requestForm.setTo(ZonedDateTime.of(1700, 3, 1, 12, 0, 0, 0, CH_ZONE));
        requestForm.setLang(Lang.forCode("en"));

        Collection<EventInstance> actual = calculate(requestForm);

        assertEquals(List.of("1699-11-07T12:49", "1699-12-07T00:13", "1700-01-05T11:06", "1700-02-03T21:38"), actual.stream()
                .map(EventInstance::getDateTime)
                .map(ZonedDateTime::toLocalDateTime)
                .map(LocalDateTime::toString)
                .collect(Collectors.toList()));
    }

    @Test
    public void testLoadCurrentFullMoons() {
        requestForm.getPhases().clear();
        requestForm.getPhases().put(MoonPhaseType.FULLMOON, true);
        requestForm.setFrom(ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, CH_ZONE));
        requestForm.setTo(ZonedDateTime.of(2025, 12, 31, 23, 59, 59, 0, CH_ZONE));
        requestForm.setLang(Lang.forCode("en"));

        Collection<EventInstance> actual = calculate(requestForm);

        assertEquals(12, actual.size());
    }

    private Matcher<EventInstance> eventAt(final LocalDate expectedDate) {
        return new FeatureMatcher<>(equalTo(expectedDate), "LocalDate", "Date") {
            @Override
            protected LocalDate featureValueOf(final EventInstance actual) {
                return actual.getDateTime().toLocalDate();
            }
        };
    }

}
