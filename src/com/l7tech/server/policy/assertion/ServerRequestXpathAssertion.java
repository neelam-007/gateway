/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.XmlRequest;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server-side processing for an assertion that verifies whether a request
 * matches a specified XPath pattern.
 *
 * @see com.l7tech.policy.assertion.RequestXpathAssertion
 * @see com.l7tech.proxy.policy.assertion.ClientRequestXpathAssertion
 * @author alex
 * @version $Revision$
 */
public class ServerRequestXpathAssertion implements ServerAssertion {

    public ServerRequestXpathAssertion( RequestXpathAssertion data ) {
        _data = data;
    }

    private synchronized DOMXPath getDOMXpath() throws JaxenException {
        if ( _domXpath == null ) {
            String pattern = _data.getPattern();
            Map namespaceMap = _data.getNamespaceMap();

            if ( pattern != null ) {
                _domXpath = new DOMXPath(pattern);

                if ( namespaceMap != null ) {
                    for (Iterator i = namespaceMap.keySet().iterator(); i.hasNext();) {
                        String key = (String) i.next();
                        String uri = (String)namespaceMap.get(key);
                        _domXpath.addNamespace( key, uri );
                    }
                }
            }
        }

        return _domXpath;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        if ( request instanceof XmlRequest ) {
            try {
                String pattern = _data.getPattern();

                if ( pattern == null || pattern.length() == 0 ) {
                    _logger.warning( "XPath pattern cannot be null or empty!" );
                    return AssertionStatus.FALSIFIED;
                }

                XmlRequest xreq = (XmlRequest)request;
                Document doc = xreq.getDocument();

                List result = null;
                DOMXPath xp = getDOMXpath();
                try {
                    result = xp.selectNodes(doc);
                } catch ( RuntimeException rte ) {
                    _logger.log( Level.WARNING, "XPath processor threw Runtime Exception", rte );
                    return AssertionStatus.FALSIFIED;
                }

                if ( result == null || result.size() == 0 ) {
                    _logger.info( "XPath pattern " + pattern  + " didn't match request!" );
                    return AssertionStatus.FALSIFIED;
                } else {
                    Object o = result.get(0);
                    if ( o instanceof Boolean ) {
                        if ( ((Boolean)o).booleanValue() ) {
                            _logger.fine( "XPath pattern " + pattern + " returned true" );
                            return AssertionStatus.NONE;
                        } else {
                            _logger.info( "XPath pattern " + pattern + " returned false" );
                            return AssertionStatus.FALSIFIED;
                        }
                    } else if ( o instanceof Node ) {
                        Node n = (Node)o;
                        int type = n.getNodeType();
                        switch( type ) {
                            case Node.TEXT_NODE:
                                _logger.fine( "XPath pattern " + pattern + " found a text node '" + n.getNodeValue() + "'" );
                                return AssertionStatus.NONE;
                            case Node.ELEMENT_NODE:
                                _logger.fine( "XPath pattern " + pattern + " found an element '" + n.getNodeName() + "'" );
                                return AssertionStatus.NONE;
                            default:
                                _logger.fine( "XPath pattern " + pattern + " found some other node '" + n.toString() + "'" );
                                return AssertionStatus.NONE;
                        }
                    } else {
                        _logger.fine( "XPath pattern " + pattern + " matched request" );
                        return AssertionStatus.NONE;
                    }
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
            }
        }
        _logger.warning( "RequestXPathAssertion only works on XML requests!" );
        return AssertionStatus.FALSIFIED;
    }

    private final RequestXpathAssertion _data;
    private final Logger _logger = Logger.getLogger(getClass().getName());
    private DOMXPath _domXpath;
}
