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
import org.apache.xpath.CachedXPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.traversal.NodeIterator;
import org.xml.sax.SAXException;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author alex
 * @version $Revision$
 */
public class ServerRequestXpathAssertion implements ServerAssertion {
    public ServerRequestXpathAssertion( RequestXpathAssertion data ) {
        _data = data;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {
        if ( request instanceof XmlRequest ) {
            try {
                XmlRequest xreq = (XmlRequest)request;
                Document doc = xreq.getDocument();
                String pattern = _data.getPattern();

                NodeIterator i = _cachedXpathAPI.selectNodeIterator( doc, pattern );
                if ( i.nextNode() != null ) {
                    _logger.fine( "Found XPath pattern in request" );
                    return AssertionStatus.NONE;
                } else {
                    _logger.info( "Failed to find XPath pattern in request!" );
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
            }
        }
        return null;
    }

    private Logger _logger = LogManager.getInstance().getSystemLogger();
    private RequestXpathAssertion _data;
    private CachedXPathAPI _cachedXpathAPI = new CachedXPathAPI();
}
