package com.l7tech.server.message;

import com.l7tech.message.MessageKnob;

import java.nio.charset.Charset;

/**
 * Knob that is present on Messages that are actually backed by context variables.
 */
public class ContextVariableKnob implements MessageKnob {
    private String variableName;
    private Charset overrideEncoding = null;

    public ContextVariableKnob(String variableName) {
        this.variableName = variableName;
    }

    /**
     * @return the name of the context variable that is backing this Message.
     */
    public String getVariableName() {
        return variableName;
    }

    /**
     * Override the encoding that will be used when turning modified message bytes into a String to store
     * back into the context variable.
     *
     * @param overrideEncoding a Java encoding name to use instead of the encoding from this Message content type,
     *                         or null to just use the content type's encoding.
     */
    public void setOverrideEncoding(Charset overrideEncoding) {
        this.overrideEncoding = overrideEncoding;
    }

    /**
     * Get the override encoding that will be used when writing back bytes into a String context variable.
     * 
     * @return the current override encoding, or null if we plan to use the encoding from the content type.
     */
    public Charset getOverrideEncoding() {
        return overrideEncoding;
    }
}
