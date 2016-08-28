package controllers;

import play.api.Play;
import play.i18n.Lang;
import play.i18n.MessagesApi;
import play.mvc.QueryStringBindable;

import java.util.Map;
import java.util.Optional;

public class LangQueryStringBindable implements QueryStringBindable<LangQueryStringBindable> {

    private final MessagesApi messagesApi;
    private Lang lang;

    @SuppressWarnings("unused") //used by Play Framework
    public LangQueryStringBindable() {
        this(null);
    }

    public LangQueryStringBindable(String lang) {
        this(lang, Play.unsafeApplication().injector().instanceOf(MessagesApi.class));
    }

    public LangQueryStringBindable(String lang, MessagesApi messagesApi) {
        this.lang = Lang.forCode(lang);
        this.messagesApi = messagesApi;
    }

    /**
     * Bind a query string parameter.
     *
     * @param key  Parameter key
     * @param data The query string data
     * @return An instance of this class (it could be this class) if the query string data can be bound to this type,
     * or None if it couldn't.
     */
    @Override
    public Optional<LangQueryStringBindable> bind(String key, Map<String, String[]> data) {
        final String[] param = data.get(key);
        if (param != null && param.length > 0) {
            this.lang = Lang.forCode(param[0]);
            return Optional.of(this);
        }
        return Optional.empty();
    }

    /**
     * Unbind a query string parameter.  This should return a query string fragment, in the form
     * <code>key=value[&amp;key2=value2...]</code>.
     *
     * @param key Parameter key
     */
    @Override
    public String unbind(String key) {
        return key + "=" + messagesApi.get(lang, "lang.current");
    }

    /**
     * Javascript function to unbind in the Javascript router.
     * <p>
     * If this bindable just represents a single value, you may return null to let the default implementation handle it.
     */
    @Override
    public String javascriptUnbind() {
        return null;
    }

    public Lang get() {
        return lang;
    }
}
