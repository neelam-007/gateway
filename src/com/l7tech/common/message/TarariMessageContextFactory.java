package com.l7tech.common.message;

import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

import com.l7tech.common.xml.tarari.TarariMessageContext;
import com.l7tech.common.xml.SoftwareFallbackException;

/**
 * Represents something that knows how to produce a {@link TarariMessageContext} from a message body InputStream.
 */
public interface TarariMessageContextFactory {
    /**
     * Create a TarariMessageContext from this message body InputStream.  The message must be XML, but does not
     * need to be SOAP.
     *
     * @param messageBody an InputStream that will produce well-formed XML.
     * @return a TarariMessageContext implementation.  Never null.
     * @throws IOException  if there is a problem reading the InputStream.
     * @throws SAXException if the XML is not well-formed.
     * @throws SoftwareFallbackException if the operation should be retried without using the hardware.
     */
    TarariMessageContext makeMessageContext(InputStream messageBody)
            throws IOException, SAXException, SoftwareFallbackException;
}
