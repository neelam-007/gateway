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
     * @return
     * @throws SAXException
     * @throws IOException
     */
    Document getDocument() throws SAXException, IOException;

    // TODO: setDocument()?  Include DOM serialization functionality in XmlMessageAdapter?
}
