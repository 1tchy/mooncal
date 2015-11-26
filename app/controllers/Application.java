package controllers;

import play.*;
import play.mvc.*;

import views.html.*;

public class Application extends Controller {

    public Result index() {
        return ok("Hallo");
//        return ok(index.render());
    }

    public Result index1(String s) {
        return ok(s);
//        return ok(index.render());
    }

}
