/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion;

import com.l7tech.common.message.XmlKnob;
import com.l7tech.common.audit.Auditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.AssertionMessages;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Server-side processing for an assertion that verifies whether a request
 * matches a specified XPath pattern.
 *
 * @author alex
 * @version $Revision$
 * @see com.l7tech.policy.assertion.RequestXpathAssertion
 * @see com.l7tech.proxy.policy.assertion.ClientRequestXpathAssertion
 */
public class ServerRequestXpathAssertion implements ServerAssertion {

    public ServerRequestXpathAssertion(RequestXpathAssertion data) {
        _data = data;
    }

    private synchronized DOMXPath getDOMXpath() throws JaxenException {
        if (_domXpath == null) {
            String pattern = _data.pattern();
            Map namespaceMap = _data.namespaceMap();

            if (pattern != null) {
                _domXpath = new DOMXPath(pattern);

                if (namespaceMap != null) {
                    for (Iterator i = namespaceMap.keySet().iterator(); i.hasNext();) {
                        String key = (String) i.next();
                        String uri = (String) namespaceMap.get(key);
                        _domXpath.addNamespace(key, uri);
                    }
                }
            }
        }

        return _domXpath;
    }

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        Auditor auditor = new Auditor(context.getAuditContext(), _logger);
        if (context.getRequest().getKnob(XmlKnob.class) == null) {
            auditor.logAndAudit(AssertionMessages.XPATH_REQUEST_NOT_XML);
            return AssertionStatus.BAD_REQUEST;
        }

        try {
            String pattern = _data.pattern();

            if (pattern == null || pattern.length() == 0) {
                auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_INVALID);
                return AssertionStatus.FALSIFIED;
            }

            XmlKnob reqXml = context.getRequest().getXmlKnob();
            Document doc = reqXml.getDocumentReadOnly();

            List result = null;
            DOMXPath xp = getDOMXpath();
            try {
                result = xp.selectNodes(doc);
            } catch (RuntimeException rte) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"XPath processor threw Runtime Exception"}, rte);
                return AssertionStatus.FALSIFIED;
            }

            if (result == null || result.size() == 0) {
                auditor.logAndAudit(AssertionMessages.XPATH_PATTERN_NOT_MATCHED_REQUEST);
                return AssertionStatus.FALSIFIED;
            } else {
                Object o = result.get(0);
                if (o instanceof Boolean) {
                    if (((Boolean) o).booleanValue()) {
                        auditor.logAndAudit(AssertionMessages.XPATH_RESULT_TRUE);
                        return AssertionStatus.NONE;
                    } else {
                        auditor.logAndAudit(AssertionMessages.XPATH_RESULT_FALSE);
                        return AssertionStatus.FALSIFIED;
                    }
                } else if (o instanceof Node) {
                    Node n = (Node) o;
                    int type = n.getNodeType();
                    switch (type) {
                        case Node.TEXT_NODE:
                            auditor.logAndAudit(AssertionMessages.XPATH_TEXT_NODE_FOUND);
                            return AssertionStatus.NONE;
                        case Node.ELEMENT_NODE:
                            auditor.logAndAudit(AssertionMessages.XPATH_ELEMENT_FOUND);
                            return AssertionStatus.NONE;
                        default:
                            auditor.logAndAudit(AssertionMessages.XPATH_OTHER_NODE_FOUND);
                            return AssertionStatus.NONE;
                    }
                } else {
                    auditor.logAndAudit(AssertionMessages.XPATH_SUCCEED_REQUEST);
                    return AssertionStatus.NONE;
                }
            }
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Error in XPath query"}, e);
            return AssertionStatus.SERVER_ERROR;
        } catch (IOException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Error in XPath query"}, e);
            return AssertionStatus.SERVER_ERROR;
        } catch (JaxenException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{"Error in XPath query"}, e);
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private final RequestXpathAssertion _data;
    private final Logger _logger = Logger.getLogger(getClass().getName());
    private DOMXPath _domXpath;
}
