package com.l7tech.server.policy.assertion;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.SoapRequest;
import com.l7tech.message.XmlRequest;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.util.MultipartUtil;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.xml.sax.SAXException;
import org.jaxen.dom.DOMXPath;
import org.jaxen.JaxenException;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class ServerRequestSwAAssertion implements ServerAssertion {
    private final RequestSwAAssertion _data;
    private final Logger logger = Logger.getLogger(getClass().getName());

    public ServerRequestSwAAssertion(RequestSwAAssertion data) {
        if (data == null) throw new IllegalArgumentException("must provide assertion");
        _data = data;
    }

    private synchronized DOMXPath getDOMXpath(String pattern) throws JaxenException {
        DOMXPath domXpath = null;

        Map namespaceMap = _data.getNamespaceMap();

        if (pattern != null) {
            domXpath = new DOMXPath(pattern);

            if (namespaceMap != null) {
                for (Iterator i = namespaceMap.keySet().iterator(); i.hasNext();) {
                    String key = (String) i.next();
                    String uri = (String) namespaceMap.get(key);
                    domXpath.addNamespace(key, uri);
                }
            }
        }

        return domXpath;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {

        List result = null;
        boolean operationFound = false;
        XmlRequest xreq = (XmlRequest) request;

        if (request instanceof SoapRequest && ((SoapRequest) request).isSoap()) {

            try {
                Document doc = xreq.getDocument();

                // look for operation whose xpath expression can find the operation in the request
                Iterator bindingItr = _data.getBindings().keySet().iterator();
                while(!operationFound && bindingItr.hasNext()) {
                    String bindingName = (String) bindingItr.next();
                    BindingInfo binding = (BindingInfo) _data.getBindings().get(bindingName);

                    Iterator boItr = binding.getBindingOperations().keySet().iterator();

                    while(boItr.hasNext()) {
                        String boName= (String) boItr.next();
                        BindingOperationInfo bo = (BindingOperationInfo) binding.getBindingOperations().get(boName);

                        DOMXPath operationXPath = getDOMXpath(bo.getXpath());
                        result = operationXPath.selectNodes(doc);

                        if(result != null) {
                            Object o = result.get(0);
                            if ( o instanceof Node ) {
                                Node n = (Node)o;
                                int type = n.getNodeType();

                                if(type == Node.ELEMENT_NODE) {
                                    Element operationElement = XmlUtil.findFirstChildElement((Element) n);
                                    logger.fine("The operation " + bo.getName() + " is found in the request");

                                } else {
                                    logger.info( "XPath pattern " + bo.getXpath() + " found some other node '" + n.toString() + "'" );
                                    return AssertionStatus.FALSIFIED;
                                }

                                Iterator parameterItr = bo.getMultipart().keySet().iterator();
                                // get parameter
                                while(parameterItr.hasNext()) {
                                    String parameterName= (String) parameterItr.next();
                                    MimePartInfo part = (MimePartInfo) bo.getMultipart().get(parameterName);

                                    DOMXPath parameterXPath = getDOMXpath(bo.getXpath() + "/" + part.getName());
                                    result = parameterXPath.selectNodes(doc);

                                    if(result != null) {
                                        o = result.get(0);
                                        if ( o instanceof Node ) {
                                            n = (Node)o;
                                            type = n.getNodeType();
                                            if(type == Node.ELEMENT_NODE) {

                                                logger.fine("The parameter " + part.getName() + " is found in the request");
                                                Element parameterElement = (Element) n;
                                                Attr href = parameterElement.getAttributeNode("href");
                                                String mimePardCID = href.getValue();
                                                logger.fine("The href of the parameter " + part.getName() + " is found in the request, value=" + mimePardCID);

                                                MultipartUtil.Part mimepart = xreq.getMultipartReader().getMessagePart(mimePardCID);

                                                if(mimepart != null) {
                                                    // validate the content type
                                                    if(mimepart.getHeader(XmlUtil.CONTENT_TYPE).equals(part.getContentType())) {
                                                        return AssertionStatus.NONE;
                                                    }

                                                    // check the max. length allowed
                                                    if(mimepart.getContent().length() <= part.getMaxLength()) {
                                                        return AssertionStatus.NONE;
                                                    }
                                                }

                                                return AssertionStatus.FALSIFIED;
                                            } else {
                                                logger.info( "XPath pattern " + bo.getXpath() + "/" + part.getName() + " found some other node '" + n.toString() + "'" );
                                                return AssertionStatus.FALSIFIED;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        operationFound = true;
                        break;
                    }
                }
            } catch (SAXException e) {
                logger.log(Level.WARNING, "Caught SAXException when retrieving xml document from request", e);
                return AssertionStatus.SERVER_ERROR;
            } catch (IOException e) {
                logger.log(Level.WARNING, "Caught IOException when retrieving xml document from request", e);
                return AssertionStatus.SERVER_ERROR;
            } catch (JaxenException e) {
                logger.log(Level.WARNING, "Caught JaxenException when retrieving xml document from request", e);
                return AssertionStatus.SERVER_ERROR;
            }
        } else {

        }

        return AssertionStatus.NONE;
    }
}
