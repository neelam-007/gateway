package com.l7tech.message;

import java.io.IOException;

/**
 * @since SecureSpan 4.0
 */
public interface HasSoapAction {
    /**
     * @return SOAPAction value; <code>null</code> if not available
     * @throws IOException if multivalued
     */
    String getSoapAction() throws IOException;
}
