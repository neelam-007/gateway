package com.l7tech.server.ems.enterprise;

import java.io.Serializable;

/**
 * Provides for conversion of a Java Throwable to JSON notation.
 *
 * <p>This class is not thread safe.</p>
 *
 * @since Enteprise Manager 1.0
 * @author rmak
 */
public class JSONException extends JSONSupport implements Serializable {

    private Throwable t;

    public JSONException(final Throwable t) {
        this.t = t;
    }

    @Override
    protected void writeJson() {
        add(JSONConstants.Exception.EXCEPTION, t.getClass().getName());
        if (t.getMessage() != null) add(JSONConstants.Exception.MESSAGE, t.getMessage());
        if (t.getLocalizedMessage() != null && !t.getLocalizedMessage().equals(t.getMessage())) add(JSONConstants.Exception.LOCALIZED_MESSAGE, t.getLocalizedMessage());
        if (t.getStackTrace() != null) add(JSONConstants.Exception.STACK_TRACE, t.getStackTrace());
        if (t.getCause() != null) add(JSONConstants.Exception.CAUSE, new JSONException(t.getCause()));
    }
}
