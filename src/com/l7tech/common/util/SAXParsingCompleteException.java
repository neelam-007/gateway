package com.l7tech.common.util;

import org.xml.sax.SAXException;

/**
 * User: flascell
 * Date: Aug 12, 2003
 * Time: 8:52:25 AM
 *
 * This exception is thrown by a SAX content handler to express the fact that all information was extracted
 * out of an xml document and that it is not necessary to continue parsing until the end of the document. See
 * XMLReader.parse documentation for more info.
 * 
 */
public class SAXParsingCompleteException extends SAXException {
    public SAXParsingCompleteException() {
        super("Parsing complete");
    }
}
