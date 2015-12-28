package controllers;

import logics.calculation.TotalCalculation;
import logics.calendar.CalendarMapper;
import models.RequestForm;
import models.ZonedEvent;
import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.Play;
import play.data.Form;
import play.data.validation.ValidationError;
import play.i18n.Messages;
import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unused") //used by routes
public class Application extends Controller {

    private final TotalCalculation calculation;
    private final CalendarMapper calendarMapper;

    @Inject
    public Application(TotalCalculation calculation, CalendarMapper calendarMapper) {
        this.calculation = calculation;
        this.calendarMapper = calendarMapper;
    }

    public F.Promise<Result> query() {
        return handleQueryRequest(this::renderError, (result, goodForm) -> ok(Json.toJson(result)));
    }

    public F.Promise<Result> queryAsICalendar(LangQueryStringBindable lang) {
        if (lang != null && lang.get() != null) {
            ctx().setTransientLang(lang.get());
        }
        return handleQueryRequest(badForm -> badRequest(badForm.errorsAsJson()), (result, goodForm) -> {
            response().setContentType("text/calendar");
            final long updateFrequency = goodForm.getFrom().until(goodForm.getTo(), ChronoUnit.DAYS) / 20;
            return ok(calendarMapper.map(result, updateFrequency));
        });
    }

    private F.Promise<Result> handleQueryRequest(Function<Form<RequestForm>, Result> badRequest, BiFunction<Collection<ZonedEvent>, RequestForm, Result> goodRequest) {
        return handleQueryForm(badRequest, requestForm -> handleETag(requestForm, F.Promise.promise(() -> {
            //noinspection CodeBlock2Expr
            return goodRequest.apply(calculation.calculate(requestForm), requestForm);
        })));
    }

    private F.Promise<Result> handleETag(RequestForm requestForm, F.Promise<Result> request) {
        final String calculatedETag = requestForm.calculateETag();
        final boolean isNotModified = calculatedETag.equals(request().getHeader(IF_NONE_MATCH));
        Logger.info("Request: " + request().uri() + (isNotModified ? " NOT_MODIFIED" : ""));
        if (isNotModified && Play.isProd()) {
            return F.Promise.pure(status(NOT_MODIFIED));
        }
        response().setHeader(CACHE_CONTROL, "max-age=21600"); //=6h
        response().setHeader(ETAG, calculatedETag);
        return request;
    }

    private F.Promise<Result> handleQueryForm(Function<Form<RequestForm>, Result> badRequest, Function<RequestForm, F.Promise<Result>> goodRequest) {
        Form<RequestForm> form = Form.form(RequestForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return F.Promise.pure(badRequest.apply(form));
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
