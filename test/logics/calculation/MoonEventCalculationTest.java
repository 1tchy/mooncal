package logics.calculation;

import models.EventInstance;
import models.EventType;
import models.RequestForm;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.i18n.Lang;
import play.i18n.Messages;
import play.i18n.MessagesApi;
import play.mvc.Http;
import play.test.WithApplication;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MoonEventCalculationTest extends WithApplication {

    private final MoonEventCalculation cut = new MoonEventCalculation(getMessagesApiMock());

    @NotNull
    private static MessagesApi getMessagesApiMock() {
        final Messages messages = mock(Messages.class);
        final ArgumentCaptor<String> message = ArgumentCaptor.forClass(String.class);
        when(messages.at(message.capture())).then(invocation -> message.getValue());
        MessagesApi messagesApi = mock(MessagesApi.class);
        when(messagesApi.preferred(any(Http.RequestHeader.class))).thenReturn(messages);
        when(messagesApi.preferred(anyCollection())).thenReturn(messages);
        return messagesApi;
    }

    private RequestForm prepareRequestForm(EventType event, LocalDate from, LocalDate to) {
        RequestForm requestForm = new RequestForm();
        requestForm.getEvents().put(event, true);
        requestForm.setFrom(ZonedDateTime.of(from, LocalTime.NOON, ZoneOffset.UTC));
        requestForm.setTo(ZonedDateTime.of(to, LocalTime.NOON, ZoneOffset.UTC));
        requestForm.setLang(Lang.defaultLang());
        return requestForm;
    }

    private Collection<EventInstance> calculate(RequestForm requestForm) {
        final Collection<EventInstance> eventCollection = new TreeSet<>();
        cut.calculate(requestForm, eventCollection);
        return eventCollection;
    }

    @Test
    public void testFindFirstKnownLunarEclipse() {
        RequestForm requestForm = prepareRequestForm(EventType.LUNARECLIPSE, LocalDate.of(-2000, 1, 1), LocalDate.of(-1998, 5, 18));
        Collection<EventInstance> actual = calculate(requestForm);
        assertThat(actual, hasSize(1));
        assertThat(actual.iterator().next().getDateTime(), equalTo(ZonedDateTime.of(-1998, 5, 17, 5, 47, 36, 0, ZoneOffset.UTC)));
    }

    @Test
    public void testFindLastKnownLunarEclipse() {
        RequestForm requestForm = prepareRequestForm(EventType.LUNARECLIPSE, LocalDate.of(2999, 11, 1), LocalDate.of(5000, 1, 1));
        Collection<EventInstance> actual = calculate(requestForm);
        assertThat(actual, hasSize(1));
        assertThat(actual.iterator().next().getDateTime(), equalTo(ZonedDateTime.of(2999, 11, 14, 16, 41, 25, 0, ZoneOffset.UTC)));
    }

    @Test
    public void testFindSeveralLunarEclipse() {
        RequestForm requestForm = prepareRequestForm(EventType.LUNARECLIPSE, LocalDate.of(2014, 1, 1), LocalDate.of(2015, 12, 31));
        Collection<EventInstance> actual = calculate(requestForm);
        assertThat(actual, hasSize(4));
    }

    @Test
    public void testFindFirstKnownSolarEclipse() {
        RequestForm requestForm = prepareRequestForm(EventType.SOLARECLIPSE, LocalDate.of(-2000, 1, 1), LocalDate.of(-1999, 10, 10));
        Collection<EventInstance> actual = calculate(requestForm);
        assertThat(actual, hasSize(1));
        assertThat(actual.iterator().next().getDateTime(), equalTo(ZonedDateTime.of(-1999, 6, 12, 3, 14, 51, 0, ZoneOffset.UTC)));
    }

    @Test
    public void testFindLastKnownSolarEclipse() {
        RequestForm requestForm = prepareRequestForm(EventType.SOLARECLIPSE, LocalDate.of(3000, 6, 1), LocalDate.of(5000, 1, 1));
        Collection<EventInstance> actual = calculate(requestForm);
        assertThat(actual, hasSize(1));
        assertThat(actual.iterator().next().getDateTime(), equalTo(ZonedDateTime.of(3000, 10, 19, 16, 10, 16, 0, ZoneOffset.UTC)));
    }

    @Test
    public void testFindAMoonLanding() {
        RequestForm requestForm = prepareRequestForm(EventType.MOONLANDING, LocalDate.of(1959, 9, 1), LocalDate.of(1959, 9, 30));
        Collection<EventInstance> actual = calculate(requestForm);
        assertThat(actual, hasSize(1));
    }


    @Test
    public void testFindSeveralMoonLandings() {
        //Arrange
        RequestForm requestForm = prepareRequestForm(EventType.MOONLANDING, LocalDate.of(1950, 1, 1), LocalDate.of(2029, 12, 31));
        cut.removeLatestMoonLanding();
        cut.removeLatestMoonLanding();
        cut.removeLatestMoonLanding();
        //Act
        Collection<EventInstance> actual1 = calculate(requestForm);
        cut.updateMoonLandings("lldev.thespacedevs.com");
        List<EventInstance> actual2 = new ArrayList<>(calculate(requestForm));
        //Assert
        actual1.forEach(event1 -> actual2.removeIf(event2 -> event1.getDateTime().equals(event2.getDateTime())));
        assertFalse(actual2.isEmpty());
    }
}
