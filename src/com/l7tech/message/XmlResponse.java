package com.l7tech.message;

import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public interface XmlResponse extends XmlMessage, Response {
    /**
     * Sets the XML string containing the response from the protected service.
     * @param xml A String containing an XML document.  Will not be validated.
     */
    void setResponseXml( String xml );

    /**
     * Gets the XML string containing the response from the protected service, consuming the <code>ProtectedResponseStream</code> if necessary in the process.
     * @return a String containing an XML document returned from the protected service.
     * @throws java.io.IOException
     */
    String getResponseXml() throws IOException;
}
