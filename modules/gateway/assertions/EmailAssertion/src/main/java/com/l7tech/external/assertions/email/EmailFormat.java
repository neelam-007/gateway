package com.l7tech.external.assertions.email;

/**
 * This enum represents the Email format selected by the Admin. The default charset being used is UTF-8.
 */
public enum EmailFormat {

    PLAIN_TEXT ("Plain Text", "text/plain; charset=utf-8"),
    HTML ("HTML", "text/html; charset=utf-8");

    private final String description;
    private final String contentType;

    private EmailFormat(final String description, final String contentType) {
        this.description = description;
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public String getDescription() {
        return description;
    }
}
