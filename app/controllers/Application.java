package controllers;

import logics.calculation.TotalCalculation;
import logics.calendar.CalendarMapper;
import models.EventInstance;
import models.RequestForm;
import org.jetbrains.annotations.NotNull;
import play.Environment;
import play.Logger;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.Lang;
import play.i18n.Langs;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
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
    private final Logger.ALogger logger = Logger.of(getClass());

    @Inject
    public Application(TotalCalculation calculation, CalendarMapper calendarMapper, Environment environment, FormFactory formFactory, MessagesApi messagesApi, Langs langs) {
        this.calculation = calculation;
        this.calendarMapper = calendarMapper;
        this.environment = environment;
        this.formFactory = formFactory;
        this.messagesApi = messagesApi;
        this.langs = langs;
    }

    public CompletionStage<Result> query(String queryLang, Http.Request request) {
        return handleQueryRequest(form -> renderError(form, request), (result, goodForm) -> ok(Json.toJson(result)), queryLang, request);
    }

    public CompletionStage<Result> queryAsICalendar(String queryLang, Http.Request request) {
        return handleQueryRequest(badForm -> badRequest(badForm.errorsAsJson()), (result, goodForm) -> {
            logger.info("Responding iCalender file for query: " + goodForm.getForLog(queryLang));
            final long updateFrequency = goodForm.getFrom().until(goodForm.getTo(), ChronoUnit.DAYS) / 20;
            return ok(calendarMapper.map(result, updateFrequency)).as("text/calendar");
        }, queryLang, request);
    }

    private CompletionStage<Result> handleQueryRequest(Function<Form<RequestForm>, Result> badRequest, BiFunction<Collection<EventInstance>, RequestForm, Result> goodRequest, String queryLang, Http.Request request) {
        Lang lang = getLang(queryLang, request);
        return handleQueryForm(badRequest, requestForm -> handleETag(requestForm, lang, request, CompletableFuture.supplyAsync(() -> {
            //noinspection CodeBlock2Expr
            return goodRequest.apply(calculation.calculate(requestForm, lang), requestForm);
        })), request);
    }

    private Lang getLang(String queryLang, Http.Request request) {
        if (queryLang != null) {
            return Lang.forCode(queryLang);
        }
        return messagesApi.preferred(request).lang();
    }

    private CompletionStage<Result> handleETag(RequestForm requestForm, @NotNull Lang lang, Http.Request request, CompletionStage<Result> requestResult) {
        final String calculatedETag = requestForm.calculateETag(messagesApi.get(lang, "lang.current"));
        final boolean isNotModified = request.header(IF_NONE_MATCH).map(currentETag -> currentETag.equals(calculatedETag)).orElse(false);
        logger.debug("Request: " + request.uri() + (isNotModified ? " NOT_MODIFIED" : ""));
        if (isNotModified && environment.isProd()) {
            return CompletableFuture.completedFuture(status(NOT_MODIFIED));
        }
        return requestResult.thenApply(result -> result
                .withHeader(CACHE_CONTROL, "max-age=21600") //=6h
                .withHeader(ETAG, calculatedETag)
        );
    }

    private CompletionStage<Result> handleQueryForm(Function<Form<RequestForm>, Result> badRequest, Function<RequestForm, CompletionStage<Result>> goodRequest, Http.Request actualRequest) {
        Form<RequestForm> form = formFactory.form(RequestForm.class).bindFromRequest(actualRequest);
        if (form.hasErrors()) {
            return CompletableFuture.completedFuture(badRequest.apply(form));
        }
        RequestForm requestForm = form.get();
        return goodRequest.apply(requestForm);
    }

    private Result renderError(Form<RequestForm> form, Http.Request request) {
        return badRequest(
                form.errors().stream()
                        .map(error -> messagesApi.preferred(request).at(error.message(), error.key()))
                        .collect(Collectors.joining(", "))
        );
    }

    public Result setLanguage(String lang) {
        Result result = seeOther(routes.Application.index());
        return messagesApi.setLang(result, Lang.forCode(lang));
    }

    public Result index(Http.Request request) {
        return ok(views.html.index.render(messagesApi.preferred(request), langs, environment));
    }

    public Result about(Http.Request request) {
        return ok(views.html.templates.about.render(messagesApi.preferred(request)));
    }

    public Result main(Http.Request request) {
        return ok(views.html.templates.main.render(messagesApi.preferred(request), request));
    }

}
