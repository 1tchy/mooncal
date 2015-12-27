package controllers;

import logics.TotalCalculation;
import logics.calendar.CalendarMapper;
import models.RequestForm;
import models.ZonedEvent;
import org.apache.commons.lang3.StringUtils;
import play.Play;
import play.data.Form;
import play.data.validation.ValidationError;
import play.i18n.Messages;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Application extends Controller {

    private final TotalCalculation calculation;
    private final CalendarMapper calendarMapper;

    @Inject
    public Application(TotalCalculation calculation, CalendarMapper calendarMapper) {
        this.calculation = calculation;
        this.calendarMapper = calendarMapper;
    }

    public Result query() {
        return handleQueryRequest(this::renderError, goodForm -> {
        }, result -> ok(Json.toJson(result)));
    }

    public Result queryAsICalendar() {
        return handleQueryRequest(badForm -> badRequest(badForm.errorsAsJson()), goodForm -> {
            if (goodForm.getLang() != null) {
                ctx().setTransientLang(goodForm.getLang());
            }
        }, result -> {
            response().setContentType("text/calendar");
            return ok(calendarMapper.map(result));
        });
    }

    private Result handleQueryRequest(Function<Form<RequestForm>, Result> badRequest, Consumer<RequestForm> preCalculation, Function<Collection<ZonedEvent>, Result> resultHandling) {
        return handleQueryForm(badRequest, requestForm -> {
            final String calculatedETag = requestForm.calculateETag();
            if (calculatedETag.equals(request().getHeader(IF_NONE_MATCH)) && Play.isProd()) {
                return status(NOT_MODIFIED);
            }
            preCalculation.accept(requestForm);
            response().setHeader(ETAG, calculatedETag);
            return resultHandling.apply(calculation.calculate(requestForm));
        });
    }

    private Result handleQueryForm(Function<Form<RequestForm>, Result> badRequest, Function<RequestForm, Result> goodRequest) {
        Form<RequestForm> form = Form.form(RequestForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest.apply(form);
        }
        RequestForm requestForm = form.get();
        return goodRequest.apply(requestForm);
    }

    private Result renderError(Form<RequestForm> form) {
        final Collection<List<ValidationError>> errorList = form.errors().values();
        Collection<String> errors = errorList.stream().flatMap(Collection::stream).map(error -> Messages.get(error.message(), error.key())).collect(Collectors.toList());
        return badRequest(StringUtils.join(errors, ", "));
    }

    public Result setLanguage(String lang) {
        ctx().changeLang(lang);
        return seeOther(routes.Application.index());
    }

    public Result index() {
        return ok(views.html.index.render());
    }

    public Result read(String scalaHtmlFile) {
        try {
            final Class<?> template = this.getClass().getClassLoader().loadClass("views.html.templates." + scalaHtmlFile);
            return ok((Html) template.getMethod("render").invoke(template));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            return ok(views.html.notFound.render());
        }
    }

}
