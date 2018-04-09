package com.l7tech.external.assertions.email;

import javax.activation.DataSource;

/**
 * Interface for various types of attachment source like Message, PartInfo and Multipart.
 */
public interface EmailAttachmentDataSource extends DataSource {

    /**
     * Gets the Content Length to verify with the email.attachment.maxSize cluster property
     * @return Content length
     */
    long getContentLength();
}
