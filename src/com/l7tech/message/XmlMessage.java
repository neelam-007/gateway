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
     * Replace the DOM document for this message.
     * @param doc the new DOM tree.  Must not be null.
     */
    void setDocument(Document doc);

    /**
     * Returns a String containing the XML document of the <code>Request</code>.
     *
     * This method consumes any underlying <code>InputStream</code> if necessary
     * the first time it's called.  Subsequent calls will return the cached XML
     * document, leaving the InputStream unchanged.
     *
     * Note that SocketInputStreams are not resettable!
     *
     * @return a String containing the XML document of the Request.  Might be empty, but never null.
     * @throws java.io.IOException if there was a problem reading from the message stream
     */
    String getXml() throws IOException;

    /**
     * Sets the String containing the XML document for this <code>Request</code>.
     *
     * Implementors <b>MUST</b> clear any cached data structures that are dependent on the
     * XML content of the request (e.g. DOM Documents) when this method is called.
     *
     * @param xml the new XML String.  Must not be null.
     */
    void setXml( String xml );
}
