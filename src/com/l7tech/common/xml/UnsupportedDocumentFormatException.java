/**
 * LAYER 7 TECHNOLOGIES, INC<br/>
 *
 * User: flascell<br/>
 * Date: Mar 14, 2005<br/>
 */
package com.l7tech.common.xml;

/**
 * Relating to an xml document, the format may be valid but is not supported by the secure span product line.
 *
 * @author flascelles@layer7-tech.com
 */
public class UnsupportedDocumentFormatException extends Exception {
    public UnsupportedDocumentFormatException(String message) {
        super(message);
    }
}
