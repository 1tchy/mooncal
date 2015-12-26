package controllers;

import logics.TotalCalculation;
import logics.calendar.CalendarMapper;
import models.RequestForm;
import org.apache.commons.lang3.StringUtils;
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
        Form<RequestForm> form = Form.form(RequestForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return renderError(form);
        }
        return ok(Json.toJson(calculation.calculate(form.get())));
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

    public Result queryAsICalendar() {
        Form<RequestForm> form = Form.form(RequestForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }
        final RequestForm requestForm = form.get();
        if (requestForm.getLang() != null) {
            ctx().setTransientLang(requestForm.getLang());
        }
        response().setContentType("text/calendar");
        return ok(calendarMapper.map(calculation.calculate(requestForm)));
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
