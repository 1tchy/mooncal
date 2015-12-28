package controllers;

import play.Play;
import play.http.DefaultHttpErrorHandler;
import play.http.HttpErrorHandler;
import play.libs.F;
import play.mvc.Http;
import play.mvc.Result;

import javax.inject.Inject;

import static play.mvc.Results.internalServerError;
import static play.mvc.Results.status;

public class ErrorHandler implements HttpErrorHandler {

    private final DefaultHttpErrorHandler defaultErrorHandler;

    @Inject
    public ErrorHandler(DefaultHttpErrorHandler defaultErrorHandler) {
        this.defaultErrorHandler = defaultErrorHandler;
    }

    /**
     * Invoked when a client error occurs, that is, an error in the 4xx series.
     *
     * @param request    The request that caused the client error.
     * @param statusCode The error status code.  Must be greater or equal to 400, and less than 500.
     * @param message    The error message.
     */
    @Override
    public F.Promise<Result> onClientError(Http.RequestHeader request, int statusCode, String message) {
        return F.Promise.pure(status(statusCode, views.html.notFound.render(statusCode)));
    }

    /**
     * Invoked when a server error occurs.
     *
     * @param request   The request that triggered the server error.
     * @param exception The server error.
     */
    @Override
    public F.Promise<Result> onServerError(Http.RequestHeader request, Throwable exception) {
        return Play.isProd() ? F.Promise.pure(internalServerError("Server Error")) : defaultErrorHandler.onServerError(request, exception);
    }
}
