package controllers;

import logics.TotalCalculation;
import models.RequestForm;
import play.data.Form;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;

public class Application extends Controller {

    private final TotalCalculation calculation;

    @Inject
    public Application(TotalCalculation calculation) {
        this.calculation = calculation;
    }

    public Result query() {
        Form<RequestForm> form = Form.form(RequestForm.class).bindFromRequest();
        if (form.hasErrors()) {
            return badRequest(form.errorsAsJson());
        }
        return ok(Json.toJson(calculation.calculate(form.get())));
    }

}
