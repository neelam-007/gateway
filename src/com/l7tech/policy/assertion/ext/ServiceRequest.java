package com.l7tech.policy.assertion.ext;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.help.Map;
import java.security.Principal;
import java.io.IOException;
/**
 * Defines an interface to provide request information to custom assertions.
 *
 * @see ServiceInvocation
 */
public interface ServiceRequest {
    /**
     * @return the DOM <code>Document</code> attached to this service request.
     * @throws org.xml.sax.SAXException
     * @throws java.io.IOException
     */
    Document getDocument() throws SAXException, IOException;

    /**
     * Context is a <code>Map</code> of key/value properties that is associated
     * with the service invocation.
     * When running within the servlet engine the contaxt will contain elements
     * "httpRequest" and "httpResponse" that correspond to the HttpServletRequest
     * and HttpServletResponse.
     *
     * @return the map of key/value pairs represnting the invocation context.
     */
    Map getContext();
}