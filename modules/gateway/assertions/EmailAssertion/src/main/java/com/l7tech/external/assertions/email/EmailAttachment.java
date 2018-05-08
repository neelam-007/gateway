package com.l7tech.external.assertions.email;

/**
 * This is a model class to map attachment configuration like name of the attachment, the source context variable name
 * and if it a MimePart context variable.
 */
public class EmailAttachment {

    private String name;
    private String sourceVariable;
    private boolean mimePartVariable;

    @SuppressWarnings("unused")
    public EmailAttachment() {}

    public EmailAttachment(final String name, final String sourceVariable, final boolean mimePartVariable) {
        this.name = name;
        this.sourceVariable = sourceVariable;
        this.mimePartVariable = mimePartVariable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSourceVariable() {
        return sourceVariable;
    }

    public void setSourceVariable(String sourceVariable) {
        this.sourceVariable = sourceVariable;
    }

    public boolean isMimePartVariable() {
        return mimePartVariable;
    }

    public void setMimePartVariable(boolean mimePartVariable) {
        this.mimePartVariable = mimePartVariable;
    }
}
