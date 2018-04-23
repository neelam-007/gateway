package com.l7tech.external.assertions.email;

/**
 * Expected to be thrown for invalid attachments.
 */
public class EmailAttachmentException extends EmailException {

    public static final String UNDEFINED_ATTACHMENT_SOURCE_VARIABLE = "Source variable for the attachment is not defined";
    public static final String INVALID_ATTACHMENT_SOURCE_VARIABLE = "Source variable for the attachment is invalid";
    public static final String INVALID_ATTACHMENT_NAME = "Invalid attachment name";
    public static final String DUPLICATE_ATTACHMENT_NAME = "Attachment name is duplicate";
    public static final String ATTACHMENT_SIZE_EXCEEDS = "Size of the attachment exceeds the limit";
    public static final String MISSING_ATTACHMENT_NAME_FROM_MIMEPART = "Unable to extract attachment name from the MimePart";

    /**
     * @param message message
     * exception handling for attachments sent through Send Email Assertion.
     * This exception is thrown when any one of the validation fails:
     *                attachments with invalid names
     *                attachments with invalid source context variable
     *                attachments with size exceeding max size limit
     */
    public EmailAttachmentException(final String message) {
        super(message);
    }

     /** @param message message
     * exception handling for attachments sent through Send Email Assertion.
            * This exception is thrown when any one of the validation fails:
            *                attachments with invalid names
     *                attachments with invalid source context variable
     *                attachments with size exceeding max size limit
     */
    public EmailAttachmentException(final String message, final Throwable exception) {
        super(message, exception);
    }

}
