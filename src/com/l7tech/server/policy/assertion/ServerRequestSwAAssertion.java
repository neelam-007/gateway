package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import org.jaxen.JaxenException;
import org.jaxen.dom.DOMXPath;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class ServerRequestSwAAssertion extends AbstractServerAssertion implements ServerAssertion {

    private static final long KB_TO_B_MULT = 1024;

    private final RequestSwAAssertion _data;
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final Auditor auditor;

    public ServerRequestSwAAssertion(RequestSwAAssertion data, ApplicationContext springContext) {
       super(data);
        if (data == null) throw new IllegalArgumentException("must provide assertion");
        _data = data;
        auditor = new Auditor(this, springContext, logger);
    }

    private DOMXPath getDOMXpath(String pattern) throws JaxenException {
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

        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.SWA_NOT_SOAP);
                return AssertionStatus.BAD_REQUEST;
            }
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.SWA_INVALID_XML, new String[]{ExceptionUtils.getMessage(e)});
            return AssertionStatus.BAD_REQUEST;
        }

        if (!context.getRequest().getMimeKnob().isMultipart()) {
            auditor.logAndAudit(AssertionMessages.SWA_NOT_MULTIPART);
            return AssertionStatus.FALSIFIED;
        }

        try {
            Document doc = context.getRequest().getXmlKnob().getDocumentReadOnly();

            int extraAttachmentPolicy = _data.getUnboundAttachmentPolicy();
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
                        auditor.logAndAudit(AssertionMessages.SWA_OPERATION_NOT_FOUND, new String[] {bo.getXpath()});
                        continue;
                    }

                    operationElementFound = true;
                    if (result.size() > 1) {
                        auditor.logAndAudit(AssertionMessages.SWA_REPEATED_OPERATION, new String[] {bo.getXpath()});
                        return AssertionStatus.FALSIFIED;
                    }

                    // operation element found in the request
                    Object o = result.get(0);
                    if (!(o instanceof Node)) throw new RuntimeException("The values of the map is not org.w3c.dom.Node");

                    Node operationNodeRequest = (Node)o;
                    int type = operationNodeRequest.getNodeType();

                    if (type != Node.ELEMENT_NODE) {
                        auditor.logAndAudit(AssertionMessages.SWA_OPERATION_NOT_ELEMENT_NODE, new String[] {bo.getXpath(), operationNodeRequest.toString()});
                        return AssertionStatus.FAILED;
                    }

                    auditor.logAndAudit(AssertionMessages.SWA_OPERATION_FOUND, new String[] {bo.getName()});

                    Map permittedExtras = new HashMap();
                    Map xtraMPI = bo.getExtraMultipart();
                    for(Iterator iterator = xtraMPI.entrySet().iterator(); iterator.hasNext(); ) {
                        Map.Entry entry = (Map.Entry) iterator.next();
                        String contentTypePattern = (String) entry.getKey();
                        MimePartInfo mpi = (MimePartInfo) entry.getValue();
                        long maxLength = mpi.getMaxLength() * KB_TO_B_MULT;
                        // constraints are long (maxLength)
                        // repeated since we change the 1st value
                        permittedExtras.put(contentTypePattern, new long[]{maxLength, maxLength});
                    }

                    // for each input parameter of the operation of the binding in WSDL
                    Iterator parameterItr = bo.getMultipart().keySet().iterator();
                    while (assertionStatusOK && parameterItr.hasNext()) {
                        String parameterName = (String)parameterItr.next();
                        MimePartInfo part = (MimePartInfo)bo.getMultipart().get(parameterName);

                        DOMXPath parameterXPath = getDOMXpath(bo.getXpath() + "/" + part.getName());
                        result = parameterXPath.selectNodes(doc);

                        if (result == null || result.size() == 0) {
                            auditor.logAndAudit(AssertionMessages.SWA_PART_NOT_FOUND, new String[] {bo.getXpath(), part.getName()});
                            return AssertionStatus.FALSIFIED;
                        }

                        if (result.size() > 1) {
                            auditor.logAndAudit(AssertionMessages.SWA_REPEATED_MIME_PART, new String[] {bo.getXpath(), part.getName()});
                            return AssertionStatus.FALSIFIED;
                        }

                        // parameter element found in the request
                        Object obj = result.get(0);
                        if (!(obj instanceof Node)) throw new RuntimeException("The values of the map is not org.w3c.dom.Node");

                        Node parameterNodeRequest = (Node)obj;
                        type = parameterNodeRequest.getNodeType();

                        if (type != Node.ELEMENT_NODE) {
                            auditor.logAndAudit(AssertionMessages.SWA_PARAMETER_NOT_ELEMENT_NODE, new String[] {bo.getXpath(), part.getName(), parameterNodeRequest.toString()});
                            return AssertionStatus.FAILED;
                        }

                        auditor.logAndAudit(AssertionMessages.SWA_PARAMETER_FOUND, new String[] {part.getName()});
                        Element parameterElementRequest = (Element)parameterNodeRequest;
                        List hrefs = getAttachmentHrefs(parameterElementRequest);

                        // for each attachment (href)
                        if (hrefs.size() == 0) {
                            auditor.logAndAudit(AssertionMessages.SWA_REFERENCE_NOT_FOUND, new String[] {part.getName()});
                            return AssertionStatus.FALSIFIED;
                        }

                        int totalLen = 0;

                        // each attachment must fulfill the requirement of the input parameter specified in the SwA Request Assertion
                        for (int i = 0; i < hrefs.size(); i++) {
                            Attr href = (Attr)hrefs.get(i);

                            String mimePartCIDUrl = href.getValue();
                            auditor.logAndAudit(AssertionMessages.SWA_REFERENCE_FOUND, new String[] {part.getName(), mimePartCIDUrl});
                            int cpos = mimePartCIDUrl.indexOf(":");
                            if (cpos < 0) {
                                auditor.logAndAudit(AssertionMessages.SWA_INVALID_CONTENT_ID_URL, new String[] {mimePartCIDUrl});
                                return AssertionStatus.FALSIFIED;
                            }

                            String scheme = mimePartCIDUrl.substring(0,cpos);
                            String id = mimePartCIDUrl.substring(cpos+1);
                            if (!"cid".equals(scheme)) {
                                auditor.logAndAudit(AssertionMessages.SWA_INVALID_CONTENT_ID_URL, new String[] {mimePartCIDUrl});
                                return AssertionStatus.FALSIFIED;
                            }

                            PartInfo mimepartRequest = context.getRequest().getMimeKnob().getPartByContentId(id);

                            if (mimepartRequest != null) {
                                // validate the content type
                                if (!part.validateContentType(mimepartRequest.getContentType())) {
                                    if (part.getContentTypes().length > 1) {
                                        auditor.logAndAudit(AssertionMessages.SWA_NOT_IN_CONTENT_TYPES, new String[] {mimePartCIDUrl, part.retrieveAllContentTypes()});
                                    } else {
                                        auditor.logAndAudit(AssertionMessages.SWA_BAD_CONTENT_TYPE, new String[] {mimePartCIDUrl, part.retrieveAllContentTypes()});
                                    }
                                    return AssertionStatus.FALSIFIED;
                                }

                                totalLen += mimepartRequest.getActualContentLength();

                                // check the max. length allowed
                                if (totalLen > part.getMaxLength() * KB_TO_B_MULT) {
                                    if (hrefs.size() > 1) {
                                        auditor.logAndAudit(AssertionMessages.SWA_TOTAL_LENGTH_LIMIT_EXCEEDED, new String[] {part.getName(), String.valueOf(hrefs.size()), String.valueOf(part.getMaxLength())});
                                    } else {
                                        auditor.logAndAudit(AssertionMessages.SWA_PART_LENGTH_LIMIT_EXCEEDED, new String[] {mimePartCIDUrl, String.valueOf(part.getMaxLength())});
                                    }
                                    return AssertionStatus.FALSIFIED;
                                }

                                // the attachment is validated OK
                                // set the validated flag of the attachment to true
                                mimepartRequest.setValidated(true);
                            } else {
                                auditor.logAndAudit(AssertionMessages.SWA_NO_ATTACHMENT, new String[] {mimePartCIDUrl});
                                return AssertionStatus.FALSIFIED;
                            }
                        } // for each attachment
                    } // for each input parameter

                    // also check if there is any unexpected attachments in the request
                    Set dropAttachments = new HashSet();
                    PartIterator pi = context.getRequest().getMimeKnob().getParts();
                    while (pi.hasNext()) {
                        PartInfo attachment =  pi.next();
                        if (attachment.getPosition() == 0)
                            continue; // skip over SOAP part
                        String attachmentName = attachment.getContentId(true);

                        if (attachmentName == null || attachmentName.length() < 1)
                            attachmentName = "in position #" + attachment.getPosition();

                        if (!attachment.isValidated()) {
                            long[] constraints = getExtraAttachmentConstraints(attachment, permittedExtras);

                            if (constraints != null) {
                                if (validExtraAttachment(attachment, constraints)) {
                                    auditor.logAndAudit(AssertionMessages.SWA_EXTRA_ATTACHMENT, new String[]{attachmentName});
                                    attachment.setValidated(true);
                                }
                                else {
                                    // reason already audited, just exit
                                    return AssertionStatus.FALSIFIED;
                                }
                            }
                            else if (extraAttachmentPolicy == RequestSwAAssertion.UNBOUND_ATTACHMENT_POLICY_PASS){
                                auditor.logAndAudit(AssertionMessages.SWA_EXTRA_ATTACHMENT, new String[]{attachmentName});
                                attachment.setValidated(true);
                            }
                            else if (extraAttachmentPolicy == RequestSwAAssertion.UNBOUND_ATTACHMENT_POLICY_DROP){
                                auditor.logAndAudit(AssertionMessages.SWA_EXTRA_ATTACHMENT_DROPPED, new String[]{attachmentName});
                                dropAttachments.add(attachment);
                            }
                            else {
                                auditor.logAndAudit(AssertionMessages.SWA_UNEXPECTED_ATTACHMENT, new String[] {attachmentName});
                                return AssertionStatus.FALSIFIED;
                            }
                        }
                    }

                    if (!dropAttachments.isEmpty()) {
                        // flag to return only validated mime parts
                        context.getRequest().getMimeKnob().setStreamValidatedPartsOnly();

                        // clean up any dangling hrefs
                        Document reqDoc = context.getRequest().getXmlKnob().getDocumentWritable();
                        try {
                            Element body = SoapUtil.getBodyElement(reqDoc);
                            if (body == null) throw new SAXException("No SOAP body");

                            for (Iterator iterator = dropAttachments.iterator(); iterator.hasNext();) {
                                PartInfo partInfo = (PartInfo) iterator.next();
                                String cid = partInfo.getContentId(true);
                                if (cid != null) {
                                    removeAttachmentHrefs(body, "cid:" + cid);
                                }
                            }
                        }
                        catch(InvalidDocumentFormatException idfe) {
                            throw (SAXException) new SAXException("Invalid request").initCause(idfe);
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
            auditor.logAndAudit(AssertionMessages.SWA_INVALID_OPERATION);
        }
        return AssertionStatus.FALSIFIED;
    }

    /**
     * Remove any descendant elements that reference the given id.
     */
    private void removeAttachmentHrefs(Element element, String id) {
        // find hrefs to remove
        NodeList children = element.getChildNodes();
        List removeList = new ArrayList();
        for (int n=0; n<children.getLength(); n++) {
            Node child = children.item(n);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element childEle = (Element) child;
                if (id.equals(childEle.getAttribute("href"))) {
                    removeList.add(childEle);
                }
            }
        }

        // remove elements
        if (!removeList.isEmpty()) {
            for (Iterator eleIter=removeList.iterator(); eleIter.hasNext(); ) {
                Element toRemove = (Element) eleIter.next();
                Element parent = (Element) toRemove.getParentNode();
                parent.removeChild(toRemove);
            }
        }

        // process remaining children
        children = element.getChildNodes();
        for (int n=0; n<children.getLength(); n++) {
            Node child = children.item(n);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                removeAttachmentHrefs((Element)child, id);
            }
        }
    }

    /**
     * Find all the hrefs, return a List of Attrs
     */
    private List getAttachmentHrefs(Element parameterElementRequest) {
        List hrefs = new ArrayList();

        Attr href = parameterElementRequest.getAttributeNode("href");
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

        return hrefs;
    }

    /**
     * Get the constraints that appy to the given attachment (if any)
     */
    private long[] getExtraAttachmentConstraints(PartInfo attachment, Map permittedExtras) {
        long[] constraints = null; // constraints are long (count), long (maxLength - for all X)
        ContentTypeHeader partType = attachment.getContentType();

        logger.info("Checking for permitted extras with type "+partType.getType()+"/"+partType.getSubtype()+" " + permittedExtras);

        // look for an exact match
        for(Iterator iterator = permittedExtras.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry entry = (Map.Entry) iterator.next();
            String contentType = (String)entry.getKey();
            try {
                ContentTypeHeader cth = ContentTypeHeader.parseValue(contentType);
                if (cth.matches(partType)) { // "backwards" match to see if there is an exact match
                    constraints = (long[]) entry.getValue();
                }
            }
            catch(IOException ioe) {
                logger.warning("Ignoring invalid content type header (extra), value is '"+contentType+"'.");
            }
        }

        // look for a pattern match (e.g. text/*)
        if (constraints == null) {
            for(Iterator iterator = permittedExtras.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String contentType = (String)entry.getKey();
                try {
                    ContentTypeHeader cth = ContentTypeHeader.parseValue(contentType);
                    if (partType.matches(cth)) {
                        constraints = (long[]) entry.getValue();
                    }
                }
                catch(IOException ioe) {
                    logger.warning("Ignoring invalid content type header (extra), value is '"+contentType+"'.");
                }
            }
        }

        logger.info("Found?" + (constraints!=null));

        return constraints;
    }

    /**
     * Check if the given attachment is permitted according to given constraints.
     */
    private boolean validExtraAttachment(PartInfo attachment, long[] constraints) throws NoSuchPartException, IOException {
        boolean valid = false;

        long configuredMaxLn = constraints[1];

        constraints[0] -= attachment.getActualContentLength();
        if (constraints[0] >= 0) {
            valid = true;
        }
        else {
           auditor.logAndAudit(AssertionMessages.SWA_EXTRA_LENGTH_EXCEEDED, new String[]{Long.toString(configuredMaxLn)});
        }

        return valid;
    }
}
