package com.l7tech.message;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * Encapsulates a Message (i.e. a Request or Response) that contains XML.
 *
 * @author alex
 * @version $Revision$
 */
public interface XmlMessage extends Message {

    /**
     * Returns the DOM <code>Document</code> attached to this message.
     *
     * <b>Note</b> that this calling method may result in any underlying InputStream
     * being consumed.
     *
     * @return The parsed Document of the SOAP part of this message. Never null.
     * @throws SAXException if the SOAP part of this message was empty or was not well-formed XML
     * @throws IOException if there was a problem reading from the message InputStream
     */
    Document getDocument() throws SAXException, IOException;

    /**
     * Replace the DOM document for this message.  Use after ie redecorating.
     *
     * @param doc the new DOM tree.  Must not be null.
     */
    void setDocument(Document doc);
}
