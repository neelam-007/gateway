package com.l7tech.common.util;

import org.xml.sax.SAXException;

/**
 * Throws when sax parsing has extracted all necessary information.
 * This exception is thrown by a SAX content handler to express the fact that all information was extracted
 * out of an xml document and that it is not necessary to continue parsing until the end of the document. See
 * XMLReader.parse documentation for more info.
 *
 * <br/><br/>
 * User: flascell<br/>
 * Date: Aug 12, 2003<br/>
 */
public class SAXParsingCompleteException extends SAXException {
    public SAXParsingCompleteException() {
        super("Parsing complete");
    }
}
