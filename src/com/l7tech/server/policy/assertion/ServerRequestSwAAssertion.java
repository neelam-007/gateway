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
import com.l7tech.server.attachments.ServerMultipartMessageReader;

import java.io.IOException;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.*;
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
                    String key = (String)i.next();
                    String uri = (String)namespaceMap.get(key);
                    domXpath.addNamespace(key, uri);
                }
            }
        }

        return domXpath;
    }

    public AssertionStatus checkRequest(Request request, Response response) throws IOException, PolicyAssertionException {

        List result = null;
        XmlRequest xreq = (XmlRequest)request;
        boolean assertionStatusOK = true;
        boolean operationElementFound = false;
        ServerMultipartMessageReader multipartReader = null;

        if (!isSoap(request)) {
            logger.finest("The operation specified in the request is invalid.");
            return AssertionStatus.FAILED;
        }

        if (!xreq.isMultipart()) {
            logger.info("The request does not contain attachment or is not a mulitipart message");
            return AssertionStatus.FALSIFIED;
        }


        try {
            Document doc = xreq.getDocument();

            Iterator bindingItr = _data.getBindings().keySet().iterator();

            // while next binding found in assertion
            while (bindingItr.hasNext()) {
                String bindingName = (String)bindingItr.next();
                BindingInfo binding = (BindingInfo)_data.getBindings().get(bindingName);

                Iterator boItr = binding.getBindingOperations().keySet().iterator();

                // while next operation of the binding found in assertion
                while (boItr.hasNext()) {
                    String boName = (String)boItr.next();
                    BindingOperationInfo bo = (BindingOperationInfo)binding.getBindingOperations().get(boName);

                    DOMXPath operationXPath = getDOMXpath(bo.getXpath());
                    result = operationXPath.selectNodes(doc);

                    if (result == null || result.size() == 0) {
                        logger.finest("Element not found in the request. Xpath expression is: " + bo.getXpath());
                        continue;
                    }

                    operationElementFound = true;
                    if (result.size() > 1) {
                        logger.info("Element appears more than once in the request. Xpath expression is: " + bo.getXpath());
                        return AssertionStatus.FALSIFIED;
                    }

                    // operation element found in the request
                    Object o = result.get(0);
                    if (!(o instanceof Node)) throw new RuntimeException("The values of the map is not org.w3c.dom.Node");

                    Node operationNodeRequest = (Node)o;
                    int type = operationNodeRequest.getNodeType();

                    if (type != Node.ELEMENT_NODE) {
                        logger.info("XPath pattern " + bo.getXpath() + " found some other node '" + operationNodeRequest.toString() + "'");
                        return AssertionStatus.FAILED;
                    }

                    logger.fine("The operation " + bo.getName() + " is found in the request");

                    Iterator parameterItr = bo.getMultipart().keySet().iterator();

                    // for each input parameter of the operation of the binding in WSDL
                    while (assertionStatusOK && parameterItr.hasNext()) {
                        String parameterName = (String)parameterItr.next();
                        MimePartInfo part = (MimePartInfo)bo.getMultipart().get(parameterName);

                        DOMXPath parameterXPath = getDOMXpath(bo.getXpath() + "/" + part.getName());
                        result = parameterXPath.selectNodes(doc);

                        if (result == null || result.size() == 0) {
                            logger.info("Element not found in the request. Xpath expression is: " + bo.getXpath() + "/" + part.getName());
                            return AssertionStatus.FALSIFIED;
                        }

                        if (result.size() > 1) {
                            logger.info("Element appears more than once in the request. Xpath expression is: " + bo.getXpath() + "/" + part.getName());
                            return AssertionStatus.FALSIFIED;
                        }

                        // parameter element found in the request
                        Object obj = result.get(0);
                        if (!(obj instanceof Node)) throw new RuntimeException("The values of the map is not org.w3c.dom.Node");

                        Node parameterNodeRequest = (Node)obj;
                        type = parameterNodeRequest.getNodeType();

                        if (type != Node.ELEMENT_NODE) {
                            logger.info("XPath pattern " + bo.getXpath() + "/" + part.getName() + " found some other node '" + parameterNodeRequest.toString() + "'");
                            return AssertionStatus.FAILED;
                        }

                        logger.fine("The parameter " + part.getName() + " is found in the request");
                        Element parameterElementRequest = (Element)parameterNodeRequest;
                        Attr href = parameterElementRequest.getAttributeNode("href");

                        Vector hrefs = new Vector();
                        if (href == null) {
                            // maybe it is an array
                            Node currentNode = parameterElementRequest.getFirstChild();

                            do {
                                if (currentNode instanceof Element) {
                                    href = ((Element)currentNode).getAttributeNode("href");
                                    if (href != null) {
                                        hrefs.add(href);
                                    }
                                }
                            } while ((currentNode = currentNode.getNextSibling()) != null);

                        } else {
                            hrefs.add(href);
                        }

                        // for each attachment (href)
                        if (hrefs.size() == 0) {
                            logger.info("The reference (href) of the " + part.getName() + " is found in the request");
                            return AssertionStatus.FALSIFIED;
                        }

                        int totalLen = 0;

                        // each attachment must fulfill the requirement of the input parameter specified in the SwA Request Assertion
                        for (int i = 0; i < hrefs.size(); i++) {
                            href = (Attr)hrefs.elementAt(i);

                            String mimePartCID = href.getValue();
                            logger.fine("The href of the parameter " + part.getName() + " is found in the request, value=" + mimePartCID);


                            multipartReader = xreq.getMultipartReader();

                            if (multipartReader == null) throw new IllegalStateException("ServerMultipartMessageReader must be created first before use");
                            MultipartUtil.Part mimepartRequest = multipartReader.getMessagePart(mimePartCID);

                            if (mimepartRequest != null) {
                                // validate the content type
                                if (!part.validateContentType(mimepartRequest.getHeader(XmlUtil.CONTENT_TYPE).getValue())) {
                                    if (part.getContentTypes().length > 1) {
                                        logger.info("The content type of the attachment " + mimePartCID + " must be one of the types: " + part.retrieveAllContentTypes());
                                    } else {
                                        logger.info("The content type of the attachment " + mimePartCID + " must be: " + part.retrieveAllContentTypes());
                                    }
                                    return AssertionStatus.FALSIFIED;
                                }

                                totalLen += mimepartRequest.getContentLength();

                                // check the max. length allowed
                                if (totalLen > part.getMaxLength() * 1000) {
                                    if (hrefs.size() > 1) {
                                        logger.info("The parameter [" + part.getName() + "] has " + hrefs.size() + " attachments. The total length exceeds the limit: " + part.getMaxLength() + "K bytes");
                                    } else {
                                        logger.info("The length of the attachment " + mimePartCID + " exceeds the limit: " + part.getMaxLength() + "K bytes");
                                    }
                                    return AssertionStatus.FALSIFIED;
                                }

                                // the attachment is validated OK
                                // set the validated flag of the attachment to true
                                mimepartRequest.setValidated(true);
                            } else {
                                logger.info("The required attachment " + mimePartCID + " is not found in the request");
                                return AssertionStatus.FALSIFIED;
                            }
                        } // for each attachment
                    } // for each input parameter

                    // also check if there is any unexpected attachments in the request
                    if (xreq.getMultipartReader().hasNextMessagePart()) {
                        logger.info("Unexpected attachment(s) found in the request.");
                        return AssertionStatus.FALSIFIED;
                    } else {

                        // check if all parsed attachments in the request are validated
                        Map attachments = xreq.getMultipartReader().getMessageAttachments();
                        Iterator attItr = attachments.keySet().iterator();
                        while (attItr.hasNext()) {
                            String attachmentName = (String)attItr.next();
                            MultipartUtil.Part attachment = (MultipartUtil.Part)attachments.get(attachmentName);
                            if (!attachment.isValidated()) {
                                logger.info("Unexpected attachment " + attachmentName + " found in the request.");
                                return AssertionStatus.FALSIFIED;
                            }
                        }

                        // all attachments are satisfied
                        return AssertionStatus.NONE;
                    }
                }  // while next operation of the binding found in assertion
            }   // while next binding found in assertion
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

        if (!operationElementFound) {
            logger.info("The operation specified in the request is invalid.");
        }
        return AssertionStatus.FALSIFIED;
    }

    private boolean isSoap(Request request) {
        return request instanceof SoapRequest && ((SoapRequest)request).isSoap();
    }
}
