package com.l7tech.server.policy.assertion;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.common.audit.Auditor;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.AssertionMessages;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

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

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        List result = null;
        boolean assertionStatusOK = true;
        boolean operationElementFound = false;

        Auditor auditor = new Auditor(context.getAuditContext(), logger);
        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.REQUEST_NOT_SOAP);
                return AssertionStatus.FAILED;
            }
        } catch (SAXException e) {
            throw new CausedIOException("Request is declared as XML but is not well-formed", e);
        }

        if (!context.getRequest().getMimeKnob().isMultipart()) {
            auditor.logAndAudit(AssertionMessages.NOT_MULTIPART_MESSAGE);
            return AssertionStatus.FALSIFIED;
        }


        try {
            Document doc = context.getRequest().getXmlKnob().getDocumentReadOnly();

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
                        auditor.logAndAudit(AssertionMessages.OPERATION_NOT_FOUND, new String[] {bo.getXpath()});
                        continue;
                    }

                    operationElementFound = true;
                    if (result.size() > 1) {
                        auditor.logAndAudit(AssertionMessages.SAME_OPERATION_APPEARS_MORE_THAN_ONCE, new String[] {bo.getXpath()});
                        return AssertionStatus.FALSIFIED;
                    }

                    // operation element found in the request
                    Object o = result.get(0);
                    if (!(o instanceof Node)) throw new RuntimeException("The values of the map is not org.w3c.dom.Node");

                    Node operationNodeRequest = (Node)o;
                    int type = operationNodeRequest.getNodeType();

                    if (type != Node.ELEMENT_NODE) {
                        auditor.logAndAudit(AssertionMessages.OPERATION_IS_NON_ELEMENT_NODE, new String[] {bo.getXpath(), operationNodeRequest.toString()});
                        return AssertionStatus.FAILED;
                    }

                    auditor.logAndAudit(AssertionMessages.OPERATION_FOUND, new String[] {bo.getName()});

                    Iterator parameterItr = bo.getMultipart().keySet().iterator();

                    // for each input parameter of the operation of the binding in WSDL
                    while (assertionStatusOK && parameterItr.hasNext()) {
                        String parameterName = (String)parameterItr.next();
                        MimePartInfo part = (MimePartInfo)bo.getMultipart().get(parameterName);

                        DOMXPath parameterXPath = getDOMXpath(bo.getXpath() + "/" + part.getName());
                        result = parameterXPath.selectNodes(doc);

                        if (result == null || result.size() == 0) {
                            auditor.logAndAudit(AssertionMessages.MIME_PART_NOT_FOUND, new String[] {bo.getXpath(), part.getName()});
                            return AssertionStatus.FALSIFIED;
                        }

                        if (result.size() > 1) {
                            auditor.logAndAudit(AssertionMessages.SAME_MIME_PART_APPEARS_MORE_THAN_ONCE, new String[] {bo.getXpath(), part.getName()});
                            return AssertionStatus.FALSIFIED;
                        }

                        // parameter element found in the request
                        Object obj = result.get(0);
                        if (!(obj instanceof Node)) throw new RuntimeException("The values of the map is not org.w3c.dom.Node");

                        Node parameterNodeRequest = (Node)obj;
                        type = parameterNodeRequest.getNodeType();

                        if (type != Node.ELEMENT_NODE) {
                            auditor.logAndAudit(AssertionMessages.PARAMETER_IS_NON_ELEMENT_NODE, new String[] {bo.getXpath(), part.getName(), parameterNodeRequest.toString()});
                            return AssertionStatus.FAILED;
                        }

                        auditor.logAndAudit(AssertionMessages.PARAMETER_FOUND, new String[] {part.getName()});
                        Element parameterElementRequest = (Element)parameterNodeRequest;
                        Attr href = parameterElementRequest.getAttributeNode("href");

                        List hrefs = new ArrayList();
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
                            auditor.logAndAudit(AssertionMessages.REFERENCE_NOT_FOUND, new String[] {part.getName()});
                            return AssertionStatus.FALSIFIED;
                        }

                        int totalLen = 0;

                        // each attachment must fulfill the requirement of the input parameter specified in the SwA Request Assertion
                        for (int i = 0; i < hrefs.size(); i++) {
                            href = (Attr)hrefs.get(i);

                            String mimePartCIDUrl = href.getValue();
                            auditor.logAndAudit(AssertionMessages.REFERENCE_FOUND, new String[] {part.getName(), mimePartCIDUrl});
                            int cpos = mimePartCIDUrl.indexOf(":");
                            if (cpos < 0) {
                                auditor.logAndAudit(AssertionMessages.INVALID_CONTENT_ID_URL, new String[] {mimePartCIDUrl});
                                return AssertionStatus.FALSIFIED;
                            }

                            String scheme = mimePartCIDUrl.substring(0,cpos);
                            String id = mimePartCIDUrl.substring(cpos+1);
                            if (!"cid".equals(scheme)) {
                                auditor.logAndAudit(AssertionMessages.INVALID_CONTENT_ID_URL, new String[] {mimePartCIDUrl});
                                return AssertionStatus.FALSIFIED;
                            }

                            PartInfo mimepartRequest = context.getRequest().getMimeKnob().getPartByContentId(id);

                            if (mimepartRequest != null) {
                                // validate the content type
                                if (!part.validateContentType(mimepartRequest.getContentType())) {
                                    if (part.getContentTypes().length > 1) {
                                        auditor.logAndAudit(AssertionMessages.MUST_BE_ONE_OF_CONTENT_TYPES, new String[] {mimePartCIDUrl, part.retrieveAllContentTypes()});
                                    } else {
                                        auditor.logAndAudit(AssertionMessages.INCORRECT_CONTENT_TYPE, new String[] {mimePartCIDUrl, part.retrieveAllContentTypes()});
                                    }
                                    return AssertionStatus.FALSIFIED;
                                }

                                totalLen += mimepartRequest.getActualContentLength();

                                // check the max. length allowed
                                if (totalLen > part.getMaxLength() * 1000) {
                                    if (hrefs.size() > 1) {
                                        auditor.logAndAudit(AssertionMessages.TOTAL_LENGTH_LIMIT_EXCEEDED, new String[] {part.getName(), String.valueOf(hrefs.size()), String.valueOf(part.getMaxLength())});
                                    } else {
                                        auditor.logAndAudit(AssertionMessages.INDIVIDUAL_LENGTH_LIMIT_EXCEEDED, new String[] {mimePartCIDUrl, String.valueOf(part.getMaxLength())});
                                    }
                                    return AssertionStatus.FALSIFIED;
                                }

                                // the attachment is validated OK
                                // set the validated flag of the attachment to true
                                mimepartRequest.setValidated(true);
                            } else {
                                auditor.logAndAudit(AssertionMessages.ATTACHMENT_NOT_FOUND, new String[] {mimePartCIDUrl});
                                return AssertionStatus.FALSIFIED;
                            }
                        } // for each attachment
                    } // for each input parameter

                    // also check if there is any unexpected attachments in the request
                    PartIterator pi = context.getRequest().getMimeKnob().getParts();
                    while (pi.hasNext()) {
                        PartInfo attachment =  pi.next();
                        if (attachment.getPosition() == 0)
                            continue; // skip over SOAP part
                        String attachmentName = attachment.getContentId();
                        if (attachmentName == null || attachmentName.length() < 1)
                            attachmentName = "in position #" + attachment.getPosition();
                        if (!attachment.isValidated()) {
                            auditor.logAndAudit(AssertionMessages.UNEXPECTED_ATTACHMENT_FOUND, new String[] {attachmentName});
                            return AssertionStatus.FALSIFIED;
                        }
                    }

                    // all attachments are satisfied
                    return AssertionStatus.NONE;
                }  // while next operation of the binding found in assertion
            }   // while next binding found in assertion
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Error retrieving xml document from request"}, e);
            return AssertionStatus.SERVER_ERROR;
        } catch (IOException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Error retrieving xml document from request"}, e);
            return AssertionStatus.SERVER_ERROR;
        } catch (JaxenException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[] {"Error retrieving xml document from request"}, e);
            return AssertionStatus.SERVER_ERROR;
        } catch (NoSuchPartException e) {
            auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO_WITH_MORE_INFO, new String[] {"The required attachment " + e.getWhatWasMissing() + "was not found in the request"}, e);
            return AssertionStatus.FALSIFIED;
        }

        if (!operationElementFound) {
            auditor.logAndAudit(AssertionMessages.INVALID_OPERATION);
        }
        return AssertionStatus.FALSIFIED;
    }

}
