package controllers;

import play.i18n.Lang;
import play.i18n.Messages;
import play.libs.F;
import play.mvc.QueryStringBindable;

import java.util.Map;

import static play.libs.F.None;
import static play.libs.F.Option.Some;
import static play.mvc.Controller.ctx;

public class LangQueryStringBindable implements QueryStringBindable<LangQueryStringBindable> {

    Lang lang;

    public LangQueryStringBindable() {
    }

    public LangQueryStringBindable(String lang) {
        this.lang = Lang.forCode(lang);
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
    public F.Option<LangQueryStringBindable> bind(String key, Map<String, String[]> data) {
        final String[] param = data.get(key);
        if (param != null && param.length > 0) {
            this.lang = Lang.forCode(param[0]);
            return Some(this);
        }
        return new None<>();
    }

    /**
     * Unbind a query string parameter.  This should return a query string fragment, in the form
     * <code>key=value[&amp;key2=value2...]</code>.
     *
     * @param key Parameter key
     */
    @Override
    public String unbind(String key) {
        return key + "=" + Messages.get(lang, "lang.current");
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
