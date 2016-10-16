package controllers;

import logics.calculation.TotalCalculation;
import logics.calendar.CalendarMapper;
import models.EventInstance;
import models.RequestForm;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import play.Environment;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.data.validation.ValidationError;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unused") //used by routes
public class Application extends Controller {

    private final TotalCalculation calculation;
    private final CalendarMapper calendarMapper;
    private final Environment environment;
    private final FormFactory formFactory;
    private final MessagesApi messagesApi;
    private final Langs langs;

    @Inject
    public Application(TotalCalculation calculation, CalendarMapper calendarMapper, Environment environment, FormFactory formFactory, MessagesApi messagesApi, Langs langs) {
        this.calculation = calculation;
        this.calendarMapper = calendarMapper;
        this.environment = environment;
        this.formFactory = formFactory;
        this.messagesApi = messagesApi;
        this.langs = langs;
    }

    public CompletionStage<Result> query(LangQueryStringBindable queryLang) {
        return handleQueryRequest(this::renderError, (result, goodForm) -> ok(Json.toJson(result)), getLang(queryLang));
    }

    public CompletionStage<Result> queryAsICalendar(LangQueryStringBindable queryLang) {
        return handleQueryRequest(badForm -> badRequest(badForm.errorsAsJson()), (result, goodForm) -> {
            final long updateFrequency = goodForm.getFrom().until(goodForm.getTo(), ChronoUnit.DAYS) / 20;
            return ok(calendarMapper.map(result, updateFrequency)).as("text/calendar");
        }, getLang(queryLang));
    }

    private static Lang getLang(LangQueryStringBindable queryLang) {
        if (queryLang != null && queryLang.isDefined()) {
            return queryLang.get();
        }
        final Http.Context context = Http.Context.current.get();
        if (context != null) {
            return context.lang();
        }
        return new Lang(Lang.defaultLang());
    }

    private CompletionStage<Result> handleQueryRequest(Function<Form<RequestForm>, Result> badRequest, BiFunction<Collection<EventInstance>, RequestForm, Result> goodRequest, @NotNull Lang lang) {
        return handleQueryForm(badRequest, requestForm -> handleETag(requestForm, lang, CompletableFuture.supplyAsync(() -> {
            //noinspection CodeBlock2Expr
            return goodRequest.apply(calculation.calculate(requestForm, lang), requestForm);
        })));
    }

    private CompletionStage<Result> handleETag(RequestForm requestForm, @NotNull Lang lang, CompletionStage<Result> request) {
        final String calculatedETag = requestForm.calculateETag(messagesApi.get(lang, "lang.current"));
        final boolean isNotModified = calculatedETag.equals(request().getHeader(IF_NONE_MATCH));
        Logger.info("Request: " + request().uri() + (isNotModified ? " NOT_MODIFIED" : ""));
        if (isNotModified && environment.isProd()) {
            return CompletableFuture.completedFuture(status(NOT_MODIFIED));
        }
        response().setHeader(CACHE_CONTROL, "max-age=21600"); //=6h
        response().setHeader(ETAG, calculatedETag);
        return request;
    }

    private CompletionStage<Result> handleQueryForm(Function<Form<RequestForm>, Result> badRequest, Function<RequestForm, CompletionStage<Result>> goodRequest) {
        Form<RequestForm> form = formFactory.form(RequestForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return CompletableFuture.completedFuture(badRequest.apply(form));
        }
        RequestForm requestForm = form.get();
        return goodRequest.apply(requestForm);
    }

    private Result renderError(Form<RequestForm> form) {
        final Collection<List<ValidationError>> errorList = form.errors().values();
        Collection<String> errors = errorList.stream().flatMap(Collection::stream).map(error -> messagesApi.preferred(request()).at(error.message(), error.key())).collect(Collectors.toList());
        return badRequest(StringUtils.join(errors, ", "));
    }

    public Result setLanguage(String lang) {
        ctx().changeLang(lang);
        return seeOther(routes.Application.index());
    }

    public Result index() {
        return ok(views.html.index.render(langs, environment));
    }

    public Result read(String scalaHtmlFile) {
        try {
            final Class<?> template = this.getClass().getClassLoader().loadClass("views.html.templates." + scalaHtmlFile);
            return ok((Html) template.getMethod("render").invoke(template));
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            return notFound(views.html.notFound.render(NOT_FOUND));
        }
    }

}
