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
import com.l7tech.service.PublishedService;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.utils.PrefixResolver;
import org.apache.xpath.CachedXPathAPI;
import org.apache.xpath.XPathContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

import javax.wsdl.WSDLException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerRequestXpathAssertion implements ServerAssertion {
    private class L7PrefixResolver implements PrefixResolver {
        public L7PrefixResolver( Map namespaceMap ) {
            _map = namespaceMap;
        }

        public String getNamespaceForPrefix(String prefix) {
            return (String)_map.get(prefix);
        }

        public String getNamespaceForPrefix(String prefix, Node context) {
            return getNamespaceForPrefix(prefix);
        }

        public String getBaseIdentifier() {
            return null;
        }

        public boolean handlesNullPrefixes() {
            return false;
        }

        private Map _map;
    }

    public ServerRequestXpathAssertion( RequestXpathAssertion data ) {
        _data = data;
        _cachedXpathAPI = new CachedXPathAPI();

        /*
        Map namespaceMap = _data.getNamespaceMap();
        if ( namespaceMap != null && !namespaceMap.isEmpty() ) {
            _namespaceNode = getNamespaceNode( namespaceMap );
            XPathContext context = _cachedXpathAPI.getXPathContext();
            context.pushNamespaceContext( new L7PrefixResolver( namespaceMap ) );
            _havePrefixResolver = true;
        }
        */
    }

    private Node getNamespaceNode( Map namespaceMap ) {
        DocumentImpl doc = new DocumentImpl();
        Element el = doc.createElement("namespaces");
        doc.appendChild(el);
        for (Iterator i = namespaceMap.keySet().iterator(); i.hasNext();) {
            String prefix = (String) i.next();
            String uri = (String)namespaceMap.get(prefix);
            el.setAttribute( "xmlns:" + prefix, uri );
        }

        return doc;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        if ( request instanceof XmlRequest ) {
            try {
                XmlRequest xreq = (XmlRequest)request;
                Document doc = xreq.getDocument();

                String pattern = _data.getPattern();

                /*
                if ( !_havePrefixResolver ) {
                    PublishedService service = (PublishedService)request.getParameter( Request.PARAM_SERVICE );
                    synchronized( _cachedXpathAPI ) {
                        XPathContext context = _cachedXpathAPI.getXPathContext();
                        Map namespaceMap = service.parsedWsdl().getNamespaces();
                        context.pushNamespaceContext( new L7PrefixResolver( namespaceMap ) );
                        _havePrefixResolver = true;
                        _namespaceNode = getNamespaceNode( namespaceMap );
                    }
                }
                */

                NodeIterator i = _cachedXpathAPI.selectNodeIterator( doc, pattern );
                if ( i.nextNode() != null ) {
                    _logger.fine( "XPath pattern " + pattern + " matched request" );
                    return AssertionStatus.NONE;
                } else {
                    _logger.info( "XPath pattern " + pattern + " didn't match request!" );
                    return AssertionStatus.FALSIFIED;
                }
            } catch ( TransformerException txe ) {
                _logger.log( Level.WARNING, "Caught TransformerException during XPath query", txe );
                return AssertionStatus.SERVER_ERROR;
            } catch (SAXException e) {
                _logger.log( Level.WARNING, "Caught SAXException during XPath query", e );
                return AssertionStatus.SERVER_ERROR;
            } catch (IOException e) {
                _logger.log( Level.WARNING, "Caught IOException during XPath query", e );
                return AssertionStatus.SERVER_ERROR;
            } /*catch (WSDLException e) {
                _logger.log( Level.SEVERE, "Caught WSDLException during XPath namespace resolution", e );
                return AssertionStatus.SERVER_ERROR;
            } */
        }
        _logger.warning( "RequestXPathAssertion only works on XML requests!" );
        return AssertionStatus.FALSIFIED;
    }

    private Logger _logger = LogManager.getInstance().getSystemLogger();
    private RequestXpathAssertion _data;
    private CachedXPathAPI _cachedXpathAPI = new CachedXPathAPI();
    private boolean _havePrefixResolver = false;
    private Node _namespaceNode;
}
