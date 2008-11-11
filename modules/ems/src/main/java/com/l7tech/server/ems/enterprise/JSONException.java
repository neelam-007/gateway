package com.l7tech.server.ems.enterprise;

import org.mortbay.util.ajax.JSON;

import java.util.Map;

/**
 * Provides for conversion of a Java Throwable to JSON notation.
 *
 * @since Enteprise Manager 1.0
 * @author rmak
 */
public class JSONException implements JSON.Convertible {

    private Throwable t;

    public JSONException(final Throwable t) {
        this.t = t;
    }

    // Implements JSON.Convertible
    public void toJSON(JSON.Output output) {
        output.add(JSONConstants.Exception.EXCEPTION, t.getClass().getName());
        if (t.getMessage() != null) output.add(JSONConstants.Exception.MESSAGE, t.getMessage());
        if (t.getLocalizedMessage() != null && !t.getLocalizedMessage().equals(t.getMessage())) output.add(JSONConstants.Exception.LOCALIZED_MESSAGE, t.getLocalizedMessage());
        if (t.getStackTrace() != null) output.add(JSONConstants.Exception.STACK_TRACE, t.getStackTrace());
        if (t.getCause() != null) output.add(JSONConstants.Exception.CAUSE, new JSONException(t.getCause()));
    }

    // Implements JSON.Convertible
    public void fromJSON(Map map) {
        throw new UnsupportedOperationException("Mapping from JSON not supported.");
    }
}
