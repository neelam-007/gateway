package com.l7tech.message;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public interface XmlMessage extends Message {
    Document getDocument() throws SAXException, IOException;
}
