/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.logging.LogManager;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.XmlRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import org.jaxen.dom.DOMXPath;
import org.jaxen.JaxenException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerRequestXpathAssertion implements ServerAssertion {

    public ServerRequestXpathAssertion( RequestXpathAssertion data ) {
        _data = data;
    }

    private DOMXPath getDOMXpath() throws JaxenException {
        if ( _domXpath == null ) {

            String pattern = _data.getPattern();

            _domXpath = new DOMXPath(pattern);

            Map namespaceMap = _data.getNamespaceMap();

            for (Iterator i = namespaceMap.keySet().iterator(); i.hasNext();) {
                String key = (String) i.next();
                String uri = (String)namespaceMap.get(key);
                _domXpath.addNamespace( key, uri );
            }
        }

        return _domXpath;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        if ( request instanceof XmlRequest ) {
            try {
                XmlRequest xreq = (XmlRequest)request;
                Document doc = xreq.getDocument();

                List result = getDOMXpath().selectNodes(doc);

                if ( result != null && result.size() > 0 ) {
                    _logger.fine( "XPath pattern " + _data.getPattern() + " matched request" );
                    return AssertionStatus.NONE;
                } else {
                    _logger.info( "XPath pattern " + _data.getPattern()  + " didn't match request!" );
                    return AssertionStatus.FALSIFIED;
                }
            } catch (SAXException e) {
                _logger.log( Level.WARNING, "Caught SAXException during XPath query", e );
                return AssertionStatus.SERVER_ERROR;
            } catch (IOException e) {
                _logger.log( Level.WARNING, "Caught IOException during XPath query", e );
                return AssertionStatus.SERVER_ERROR;
            } catch (JaxenException e) {
                _logger.log( Level.WARNING, "Caught JaxenException during XPath query", e );
                return AssertionStatus.SERVER_ERROR;
            } /*catch (WSDLException e) {
                _logger.log( Level.SEVERE, "Caught WSDLException during XPath namespace resolution", e );
                return AssertionStatus.SERVER_ERROR;
            } */
        }
        _logger.warning( "RequestXPathAssertion only works on XML requests!" );
        return AssertionStatus.FALSIFIED;
    }

    private final RequestXpathAssertion _data;
    private final Logger _logger = LogManager.getInstance().getSystemLogger();
    private DOMXPath _domXpath;
}
