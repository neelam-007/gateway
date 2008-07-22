package com.l7tech.message;

import com.l7tech.xml.SoftwareFallbackException;
import com.l7tech.xml.tarari.TarariMessageContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Represents something that knows how to produce a {@link com.l7tech.xml.tarari.TarariMessageContext} from a message body InputStream.
 */
public interface TarariMessageContextFactory {
    /**
     * Parse the specified InputStream and produce a RaxDocument, wrapped in a TarariMessageContext.
     * The message must be XML, but does not
     * need to be SOAP.
     * <p>
     * The caller must close the TarariMessageContext when they are finished with it to avoid leaking
     * whatever special memory or whatever it is that RaxDocuments use up (and start failing weirdly when it runs out).
     *
     * @param messageBody an InputStream that will produce well-formed XML.  Must not be null.
     * @return the TarariMessageContext.  Never null.
     *         Caller is responsible for closing this context when they are finished with it.
     * @throws SAXException if the XML is not well-formed.
     * @throws SoftwareFallbackException if this document could not be processed in hardware.
     *                                   the operation should be retried using software.
     * @throws IOException if there was a problem reading from the InputStream.
     */
    TarariMessageContext makeMessageContext(InputStream messageBody)
            throws IOException, SAXException, SoftwareFallbackException;
}
