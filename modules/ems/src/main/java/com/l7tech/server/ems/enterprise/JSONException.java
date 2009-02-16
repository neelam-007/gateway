package com.l7tech.server.ems.enterprise;

import java.io.Serializable;

/**
 * A JSONException is used to transmit an error message from server to browser client.
 * Two constructors are provided.
 * <p>
 * {@link #JSONException(String)} should be used when a user message can be
 * composed to pinpoint the cause of the error unambiguously.
 * For usability, this is preferred.
 * <p>
 * {@link #JSONException(Throwable)} should be used when the cause of the error
 * cannot be pinpointed (such as catching a general exception) or when it is
 * a possible internal bug. The entire exception chain and stack trace will
 * be transmitted to aid in diagnostic/debugging.
 * <p>
 * This class is not thread safe.
 *
 * @since Enteprise Manager 1.0
 * @author rmak
 */
public class JSONException extends JSONSupport implements Serializable {

    private String message;
    private Throwable t;

    /**
     * Use this constructor when a user message can be composed to pinpoint the cause
     * of the error unambiguously.
     * For usability, this is preferred.
     * @param message   the error message to display; possibly localized
     */
    public JSONException(final String message) {
        assert(message != null);
        this.message = message;
    }

    /**
     * Use this constructor when the cause of the error cannot be pinpointed (such as
     * catching a general exception) or when it is a possible internal bug.
     * The entire exception chain and stack trace will be transmitted to aid
     * diagnostics/debugging.
     * @param t     the exception chain
     */
    public JSONException(final Throwable t) {
        assert(t != null);
        this.t = t;
    }

    @Override
    protected void writeJson() {
        if (this.message != null) {
            add(JSONConstants.Exception.EXCEPTION, "");
            add(JSONConstants.Exception.MESSAGE, this.message);
        } else if (this.t != null) {
            add(JSONConstants.Exception.EXCEPTION, t.getClass().getName());
            if (t.getMessage() != null) add(JSONConstants.Exception.MESSAGE, t.getMessage());
            if (t.getLocalizedMessage() != null && !t.getLocalizedMessage().equals(t.getMessage())) add(JSONConstants.Exception.LOCALIZED_MESSAGE, t.getLocalizedMessage());
            if (t.getStackTrace() != null) add(JSONConstants.Exception.STACK_TRACE, t.getStackTrace());
            if (t.getCause() != null) add(JSONConstants.Exception.CAUSE, new JSONException(t.getCause()));  // Recurse through the chain.
        }
    }
}
