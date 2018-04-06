package com.l7tech.external.assertions.email;

/**
 * Helper class to create exception while processing Send Email assertion.
 */
public final class EmailExceptionHelper {

    private EmailExceptionHelper() {}

    public static EmailAttachmentException newAttachmentException(final String message, final String nameOrSourceVariable) {
        return newAttachmentException(message, nameOrSourceVariable, null);
    }

    public static EmailAttachmentException newAttachmentException(final String message, final String nameOrSourceVariable, Throwable cause) {
        return new EmailAttachmentException(String.format("%s: %s", message, nameOrSourceVariable, cause));
    }
}
