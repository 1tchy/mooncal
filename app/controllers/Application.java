package controllers;

import logics.calculation.TotalCalculation;
import logics.calendar.CalendarMapper;
import logics.calendar.PDFMapper;
import models.BetterTranslationForm;
import models.EventInstance;
import models.RequestForm;
import play.Environment;
import play.Logger;
import play.api.mvc.Action;
import play.api.mvc.AnyContent;
import play.data.Form;
import play.data.FormFactory;
import play.i18n.MessagesApi;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@SuppressWarnings("unused") //used by routes
public class Application extends Controller {

    private final TotalCalculation calculation;
    private final CalendarMapper calendarMapper;
    private final PDFMapper pdfMapper;
    private final Environment environment;
    private final FormFactory formFactory;
    private final Action<AnyContent> indexHtml;
    private final MessagesApi messagesApi;
    private final Logger.ALogger logger = Logger.of(getClass());

    @Inject
    public Application(TotalCalculation calculation, CalendarMapper calendarMapper, PDFMapper pdfMapper, Environment environment, FormFactory formFactory, Assets assets, MessagesApi messagesApi) {
        this.calculation = calculation;
        this.calendarMapper = calendarMapper;
        this.pdfMapper = pdfMapper;
        this.environment = environment;
        this.formFactory = formFactory;
        this.messagesApi = messagesApi;
        this.indexHtml = assets.at("/public", "index.html", false);
    }

    public CompletionStage<Result> query(Http.Request request) {
        return handleQueryRequest(form -> renderError(form, request), (result, goodForm) -> ok(Json.toJson(result)), request);
    }

    public CompletionStage<Result> queryAsICalendar(Http.Request request) {
        if (request.queryString("created").isEmpty() && request.queryString("before").isPresent()) {
            return CompletableFuture.completedFuture(permanentRedirect(request.uri().replaceFirst("\\?", "?created=" + ((long) (Math.random() * 100000000) + 400000000) + "&")));
        }
        if (request.queryString("style").isEmpty()) {
            return CompletableFuture.completedFuture(permanentRedirect(request.uri().replaceFirst("^(.*?)&(events|before|after|zone)", "$1&style=withDescription&$2")));
        }
        return handleQueryRequest(badForm -> badRequest(badForm.errorsAsJson()), (result, goodForm) -> {
            logger.info("Responding iCalender file for query: " + goodForm.getForLog() + " to " + request.remoteAddress());
            final long updateFrequency = goodForm.getFrom().until(goodForm.getTo(), ChronoUnit.DAYS) / 20;
            return ok(calendarMapper.map(result, updateFrequency, goodForm.getLang())).as("text/calendar");
        }, request);
    }

    public CompletionStage<Result> queryAsPdf(Http.Request request) {
        return handleQueryRequest(
                badForm -> badRequest(badForm.errorsAsJson()),
                (result, goodForm) -> ok(pdfMapper.map(result, goodForm.getLang())).as("application/pdf"),
                request);
    }

    private CompletionStage<Result> handleQueryRequest(Function<Form<RequestForm>, Result> badRequest, BiFunction<Collection<EventInstance>, RequestForm, Result> goodRequest, Http.Request request) {
        return handleQueryForm(badRequest, requestForm -> handleETag(requestForm, request, CompletableFuture.supplyAsync(() -> {
            //noinspection CodeBlock2Expr
            return goodRequest.apply(calculation.calculate(requestForm), requestForm);
        })), request);
    }

    private CompletionStage<Result> handleETag(RequestForm requestForm, Http.Request request, CompletionStage<Result> requestResult) {
        final String calculatedETag = requestForm.calculateETag();
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

    public Result suggestBetterTranslation(Http.Request request) {
        formFactory.form(BetterTranslationForm.class).bindFromRequest(request).value().ifPresent(requestForm -> {
            String file = Optional.ofNullable(System.getProperty("translation-improvements.txt")).orElse("translation-improvements.txt");
            synchronized (this) {
                try {
                    Files.writeString(Paths.get(file), requestForm.getLanguage() + ": \"" + requestForm.getOldText() + "\" should better be \"" + requestForm.getBetterText() + "\"\n", StandardOpenOption.APPEND);
                } catch (IOException e) {
                    logger.error("Could not write to " + file, e);
                }
            }
        });
        return noContent();
    }

    public Result status() {
        return ok("up and running");
    }

    public Action<AnyContent> indexHtml(String path) {
        return indexHtml;
    }
}
