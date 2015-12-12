package controllers;

import logics.TotalCalculation;
import logics.calendar.CalendarMapper;
import models.RequestForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.twirl.api.Html;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;

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
            return badRequest(form.errorsAsJson());
        }
        return ok(Json.toJson(calculation.calculate(form.get())));
    }

    public Result queryAsICalendar() {
        Form<RequestForm> form = Form.form(RequestForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }
        response().setContentType("text/calendar");
        return ok(calendarMapper.map(calculation.calculate(form.get())));
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
