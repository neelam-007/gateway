package com.l7tech.message;

import java.io.IOException;

/**
 * @since SecureSpan 4.0
 */
public interface HasSoapAction {

    /**
     * Get the soap action value.
     *
     * <p>Note that the returned value may be null or quoted.</p>
     *
     * @return SOAPAction value; <code>null</code> if not available
     * @throws IOException if multivalued
     * @see com.l7tech.xml.soap.SoapUtil#stripQuotes(String)
     */
    String getSoapAction() throws IOException;
}
