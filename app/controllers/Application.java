package controllers;

import logics.MoonEventCalculation;
import logics.MoonPhasesCalculation;
import models.Event;
import models.RequestForm;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import java.util.Collection;
import java.util.TreeSet;

public class Application extends Controller {

	private final MoonPhasesCalculation moonPhasesCalculation;
	private final MoonEventCalculation moonEventCalculation;

	@Inject
	public Application(MoonPhasesCalculation moonPhasesCalculation, MoonEventCalculation moonEventCalculation) {
		this.moonPhasesCalculation = moonPhasesCalculation;
		this.moonEventCalculation = moonEventCalculation;
	}

	@BodyParser.Of(BodyParser.Json.class)
	public Result query() {
		final RequestForm requestForm = Json.fromJson(request().body().asJson(), RequestForm.class);
		final Collection<Event> resultSet = new TreeSet<>();
		moonPhasesCalculation.calculate(requestForm, resultSet);
		moonEventCalculation.calculate(requestForm, resultSet);
		return ok(Json.toJson(resultSet));
	}

}
