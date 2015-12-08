package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import logics.MoonPhasesCalculation;
import models.Event;
import models.RequestForm;
import play.libs.Json;
import play.mvc.BodyParser;
import play.mvc.Controller;
import play.mvc.Result;

import java.util.Collection;

public class Application extends Controller {

	@BodyParser.Of(BodyParser.Json.class)
	public Result query() {
		JsonNode json = request().body().asJson();
		final RequestForm requestForm = Json.fromJson(json, RequestForm.class);
		final Collection<Event> result = new MoonPhasesCalculation().calculate(requestForm.from.toLocalDate(), requestForm.to.toLocalDate(), requestForm.from.getZone());
		return ok(Json.toJson(result));
	}

}
