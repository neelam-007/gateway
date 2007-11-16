package com.l7tech.server.policy.assertion;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.server.audit.Auditor;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.mime.PartIterator;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.common.xml.InvalidDocumentFormatException;
import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.common.security.token.SignedPart;
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
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Server implementation that processes Requests using SOAP with Attachments.
 *
 * <p>Validates attachment size/type and that any required signatures are
 * present.</p>
 *
 * @author fpang
 */
public class ServerRequestSwAAssertion extends AbstractServerAssertion implements ServerAssertion {

    private static final Logger logger = Logger.getLogger(ServerRequestSwAAssertion.class.getName());
    private static final long KB_TO_B_MULT = 1024;

    private final RequestSwAAssertion _data;
    private final Auditor auditor;

    /**
     *
     */
    public ServerRequestSwAAssertion(final RequestSwAAssertion data,
                                     final ApplicationContext applicationContext) {
       super(data);

        if (data == null)
            throw new IllegalArgumentException("must provide assertion");

        _data = data;
        auditor = new Auditor(this, applicationContext, logger);
    }

    /**
     *
     */
    public AssertionStatus checkRequest(final PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        try {
            if (!context.getRequest().isSoap()) {
                auditor.logAndAudit(AssertionMessages.SWA_NOT_SOAP);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            auditor.logAndAudit(AssertionMessages.SWA_INVALID_XML, ExceptionUtils.getMessage(e));
            return AssertionStatus.BAD_REQUEST;
        }

        if (!context.getRequest().getMimeKnob().isMultipart()) {
            auditor.logAndAudit(AssertionMessages.SWA_NOT_MULTIPART);
            return AssertionStatus.FALSIFIED;
        }

        try {
            Document doc = context.getRequest().getXmlKnob().getDocumentReadOnly();

            processAttachments(context, doc);
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
        } catch (AttachmentProcessingException ape) {
            logger.log(Level.INFO, "Attachment processing terminated due to ''{0}''", ape.getMessage());
            return ape.getAssertionStatus();
        }

        return AssertionStatus.NONE;
    }

    /**
     * Build a DOMXPath for the given xpath string.
     */
    private DOMXPath getDOMXpath(final String pattern) throws JaxenException {
        DOMXPath domXpath = null;

        Map namespaceMap = _data.getNamespaceMap();

        if (pattern != null) {
            domXpath = new DOMXPath(pattern);

            if (namespaceMap != null) {
                for (Map.Entry<String,String> entry : ((Map<String,String>) namespaceMap).entrySet()) {
                    String key = entry.getKey();
                    String uri = entry.getValue();
                    domXpath.addNamespace(key, uri);
                }
            }
        }

        return domXpath;
    }

    /**
     * Determine the operation for the request message and validate the
     * attachments.
     */
    private void processAttachments(final PolicyEnforcementContext context,
                                    final Document doc)
            throws JaxenException, IOException, NoSuchPartException, SAXException, AttachmentProcessingException {
        Map bindings = _data.getBindings();

        // process bindings for assertion
        for (String bindingName : (Set<String>) bindings.keySet()) {
            BindingInfo binding = (BindingInfo)bindings.get(bindingName);

            // for each operation of the binding found in assertion
            for (String boName : (Set<String>) binding.getBindingOperations().keySet()) {
                BindingOperationInfo bo = (BindingOperationInfo)binding.getBindingOperations().get(boName);

                DOMXPath operationXPath = getDOMXpath(bo.getXpath());
                List result = operationXPath.selectNodes(doc);

                if (result == null || result.size() == 0) {
                    auditor.logAndAudit(AssertionMessages.SWA_OPERATION_NOT_FOUND, bo.getXpath());
                    continue;
                }

                if (result.size() > 1) {
                    auditor.logAndAudit(AssertionMessages.SWA_REPEATED_OPERATION, bo.getXpath());
                    throw new AttachmentProcessingException("Unexpected result from operation XPath", AssertionStatus.FALSIFIED);
                }

                // operation element found in the request
                Object o = result.get(0);
                if (!(o instanceof Node)) throw new RuntimeException("The values of the map is not org.w3c.dom.Node");

                Node operationNodeRequest = (Node)o;
                int type = operationNodeRequest.getNodeType();

                if (type != Node.ELEMENT_NODE) {
                    auditor.logAndAudit(AssertionMessages.SWA_OPERATION_NOT_ELEMENT_NODE, bo.getXpath(), operationNodeRequest.toString());
                    throw new AttachmentProcessingException("Unexpected result from operation XPath", AssertionStatus.FAILED);
                }

                processAttachmentsForOperation(context, doc, bo);
                return; // operation processed
            }
        } 

        // if we get here then the operation was not found
        auditor.logAndAudit(AssertionMessages.SWA_INVALID_OPERATION);
        throw new AttachmentProcessingException("Operation not found", AssertionStatus.FALSIFIED);
    }

    /**
     * Process attachments for the given operation
     */
    private void processAttachmentsForOperation(final PolicyEnforcementContext context,
                                                final Document doc,
                                                final BindingOperationInfo bo)
            throws JaxenException, IOException, NoSuchPartException, SAXException, AttachmentProcessingException {
        auditor.logAndAudit(AssertionMessages.SWA_OPERATION_FOUND, bo.getName());

        // check if any part requires a signature
        ProcessorResult wssResults = null;
        for (String parameterName : (Set<String>) bo.getMultipart().keySet()) {
            MimePartInfo part = (MimePartInfo)bo.getMultipart().get(parameterName);
            if ( part.isRequireSignature() ) {
                wssResults = context.getRequest().getSecurityKnob().getProcessorResult();
                break;
            }
        }

        // for each input parameter of the operation of the binding in WSDL
        for (String parameterName : (Set<String>) bo.getMultipart().keySet()) {
            MimePartInfo part = (MimePartInfo)bo.getMultipart().get(parameterName);

            DOMXPath parameterXPath = getDOMXpath(bo.getXpath() + "/" + part.getName());
            List result = parameterXPath.selectNodes(doc);

            if (result == null || result.size() == 0) {
                auditor.logAndAudit(AssertionMessages.SWA_PART_NOT_FOUND, bo.getXpath(), part.getName());
                throw new AttachmentProcessingException("Part not found "+part.getName(), AssertionStatus.FALSIFIED);
            }

            if (result.size() > 1) {
                auditor.logAndAudit(AssertionMessages.SWA_REPEATED_MIME_PART, bo.getXpath(), part.getName());
                throw new AttachmentProcessingException("Repeated part "+part.getName(), AssertionStatus.FALSIFIED);
            }

            // parameter element found in the request
            Object obj = result.get(0);
            if (!(obj instanceof Node)) throw new RuntimeException("The values of the map is not org.w3c.dom.Node");

            Node parameterNodeRequest = (Node)obj;
            int type = parameterNodeRequest.getNodeType();

            if (type != Node.ELEMENT_NODE) {
                auditor.logAndAudit(AssertionMessages.SWA_PARAMETER_NOT_ELEMENT_NODE, bo.getXpath(), part.getName(), parameterNodeRequest.toString());
                throw new AttachmentProcessingException("Unexpected result from Xpath for part " + part.getName(), AssertionStatus.FAILED);
            }

            auditor.logAndAudit(AssertionMessages.SWA_PARAMETER_FOUND, part.getName());
            Element parameterElementRequest = (Element)parameterNodeRequest;
            List hrefs = getAttachmentHrefs(parameterElementRequest);

            // for each attachment (href)
            if (hrefs.isEmpty()) {
                auditor.logAndAudit(AssertionMessages.SWA_REFERENCE_NOT_FOUND, part.getName());
                throw new AttachmentProcessingException("No reference found for part " + part.getName(), AssertionStatus.FALSIFIED);
            }


            processAttachmentsForPart(context, part, hrefs, wssResults);

        } // for each input parameter

        // also check if there is any unexpected attachments in the request
        processExtraAttachments(context, bo);
    }

    /**
     * Process all attachments for the part
     */
    private int processAttachmentsForPart(final PolicyEnforcementContext context,
                                          final MimePartInfo part,
                                          final List<Attr> hrefs,
                                          final ProcessorResult wssResults)
            throws JaxenException, IOException, NoSuchPartException, SAXException, AttachmentProcessingException {
        int totalLen = 0;

        // each attachment must fulfill the requirement of the input parameter specified in the SwA Request Assertion
        for (Attr href : hrefs) {
            String mimePartCIDUrl = href.getValue();
            auditor.logAndAudit(AssertionMessages.SWA_REFERENCE_FOUND, part.getName(), mimePartCIDUrl);
            int cpos = mimePartCIDUrl.indexOf(":");
            if (cpos < 0) {
                auditor.logAndAudit(AssertionMessages.SWA_INVALID_CONTENT_ID_URL, mimePartCIDUrl);
                throw new AttachmentProcessingException("Invalid attachment URL " + mimePartCIDUrl, AssertionStatus.FALSIFIED);
            }

            String scheme = mimePartCIDUrl.substring(0,cpos);
            String id = mimePartCIDUrl.substring(cpos+1);
            if (!"cid".equals(scheme)) {
                auditor.logAndAudit(AssertionMessages.SWA_INVALID_CONTENT_ID_URL, mimePartCIDUrl);
                throw new AttachmentProcessingException("Invalid attachment URL " + mimePartCIDUrl, AssertionStatus.FALSIFIED);
            }

            PartInfo mimepartRequest = context.getRequest().getMimeKnob().getPartByContentId(id);
            if (mimepartRequest != null) {
                // validate signature
                if ( part.isRequireSignature() ) {
                    if ( wssResults == null || !partSigned(mimepartRequest, wssResults.getPartsThatWereSigned())) {
                        auditor.logAndAudit(AssertionMessages.SWA_NOT_SIGNED, part.getName(), mimePartCIDUrl);
                        throw new AttachmentProcessingException("Signature required but not present for part " + part.getName(), AssertionStatus.FALSIFIED);
                    }
                }

                // validate the content type
                if (!part.validateContentType(mimepartRequest.getContentType())) {
                    if (part.getContentTypes().length > 1) {
                        auditor.logAndAudit(AssertionMessages.SWA_NOT_IN_CONTENT_TYPES, mimePartCIDUrl, part.retrieveAllContentTypes());
                    } else {
                        auditor.logAndAudit(AssertionMessages.SWA_BAD_CONTENT_TYPE, mimePartCIDUrl, part.retrieveAllContentTypes());
                    }
                    throw new AttachmentProcessingException("Validation failed for part " + part.getName(), AssertionStatus.FALSIFIED);
                }

                totalLen += mimepartRequest.getActualContentLength();

                // check the max. length allowed
                if (totalLen > part.getMaxLength() * KB_TO_B_MULT) {
                    if (hrefs.size() > 1) {
                        auditor.logAndAudit(AssertionMessages.SWA_TOTAL_LENGTH_LIMIT_EXCEEDED, part.getName(), String.valueOf(hrefs.size()), String.valueOf(part.getMaxLength()));
                    } else {
                        auditor.logAndAudit(AssertionMessages.SWA_PART_LENGTH_LIMIT_EXCEEDED, mimePartCIDUrl, String.valueOf(part.getMaxLength()));
                    }
                    throw new AttachmentProcessingException("Length limit exceeded for part " + part.getName(), AssertionStatus.FALSIFIED);
                }

                // the attachment is validated OK
                // set the validated flag of the attachment to true
                mimepartRequest.setValidated(true);
            } else {
                auditor.logAndAudit(AssertionMessages.SWA_NO_ATTACHMENT, mimePartCIDUrl);
                throw new AttachmentProcessingException("Attachment not found for part " + part.getName(), AssertionStatus.FALSIFIED);
            }
        }

        return totalLen;
    }

    /**
     * Check that the given part is signed
     */
    private boolean partSigned(final PartInfo partInfoToCheck, final SignedPart[] signedParts) {
        boolean signed = false;
        String checkId = partInfoToCheck.getContentId(true);

        for ( SignedPart signedPart : signedParts ) {
            PartInfo partInfo = signedPart.getPartInfo();
            String partId = partInfo.getContentId(true);

            if ( partId != null && partId.equals(checkId)  ) {
                signed = true;
            }
        }

        return signed;
    }

    /**
     * Process any unreferenced / unexpected attachments
     */
    private void processExtraAttachments(final PolicyEnforcementContext context, final BindingOperationInfo bo)
            throws JaxenException, IOException, NoSuchPartException, SAXException, AttachmentProcessingException {
        int extraAttachmentPolicy = _data.getUnboundAttachmentPolicy();
        Map<String,long[]> permittedExtras = getPermittedExtrasForBindingOperation(bo);
        Set<PartInfo> dropAttachments = new HashSet();

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
                        auditor.logAndAudit(AssertionMessages.SWA_EXTRA_ATTACHMENT, attachmentName);
                        attachment.setValidated(true);
                    }
                    else {
                        // reason already audited, just exit
                        throw new AttachmentProcessingException("Invalid extra attachment " + attachmentName, AssertionStatus.FALSIFIED);
                    }
                }
                else if (extraAttachmentPolicy == RequestSwAAssertion.UNBOUND_ATTACHMENT_POLICY_PASS){
                    auditor.logAndAudit(AssertionMessages.SWA_EXTRA_ATTACHMENT, attachmentName);
                    attachment.setValidated(true);
                }
                else if (extraAttachmentPolicy == RequestSwAAssertion.UNBOUND_ATTACHMENT_POLICY_DROP){
                    auditor.logAndAudit(AssertionMessages.SWA_EXTRA_ATTACHMENT_DROPPED, attachmentName);
                    dropAttachments.add(attachment);
                }
                else {
                    auditor.logAndAudit(AssertionMessages.SWA_UNEXPECTED_ATTACHMENT, attachmentName);
                    throw new AttachmentProcessingException("Unexpected attachment " + attachmentName, AssertionStatus.FALSIFIED);
                }
            }
        }

        if (!dropAttachments.isEmpty()) {
            dropAttachments(context, dropAttachments);
        }
    }

    /**
     * Drop the specified attachments
     */
    private void dropAttachments(final PolicyEnforcementContext context,
                                 final Set<PartInfo> dropAttachments) throws IOException, SAXException {
        // flag to return only validated mime parts
        context.getRequest().getMimeKnob().setStreamValidatedPartsOnly();

        // clean up any dangling hrefs
        Document reqDoc = context.getRequest().getXmlKnob().getDocumentWritable();
        try {
            Element body = SoapUtil.getBodyElement(reqDoc);
            if (body == null) throw new SAXException("No SOAP body");

            for (PartInfo partInfo : dropAttachments) {
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

    /**
     * Get information on permitted extras for the given operation
     */
    private Map<String,long[]> getPermittedExtrasForBindingOperation(final BindingOperationInfo bo) {
        Map permittedExtras = new HashMap();
        Map xtraMPI = bo.getExtraMultipart();

        for(Map.Entry<String,MimePartInfo> entry : ((Map<String,MimePartInfo>)xtraMPI).entrySet()) {
            String contentTypePattern = entry.getKey();
            MimePartInfo mpi = entry.getValue();
            long maxLength = mpi.getMaxLength() * KB_TO_B_MULT;
            // constraints are long (maxLength)
            // repeated since we change the 1st value
            permittedExtras.put(contentTypePattern, new long[]{maxLength, maxLength});
        }

        return permittedExtras; 
    }

    /**
     * Remove any descendant elements that reference the given id.
     */
    private void removeAttachmentHrefs(Element element, String id) {
        // find hrefs to remove
        NodeList children = element.getChildNodes();
        List<Element> removeList = new ArrayList();
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
            for (Element toRemove : removeList) {
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
    private List<Attr> getAttachmentHrefs(Element parameterElementRequest) {
        List<Attr> hrefs = new ArrayList();

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
    private long[] getExtraAttachmentConstraints(PartInfo attachment, Map<String,long[]> permittedExtras) {
        long[] constraints = null; // constraints are long (count), long (maxLength - for all X)
        ContentTypeHeader partType = attachment.getContentType();

        logger.info("Checking for permitted extras with type "+partType.getType()+"/"+partType.getSubtype()+" " + permittedExtras);

        // look for an exact match
        for(Map.Entry<String,long[]> entry : permittedExtras.entrySet()) {
            String contentType = entry.getKey();
            try {
                ContentTypeHeader cth = ContentTypeHeader.parseValue(contentType);
                if (cth.matches(partType)) { // "backwards" match to see if there is an exact match
                    constraints = entry.getValue();
                }
            }
            catch(IOException ioe) {
                logger.warning("Ignoring invalid content type header (extra), value is '"+contentType+"'.");
            }
        }

        // look for a pattern match (e.g. text/*)
        if (constraints == null) {
            for(Map.Entry<String,long[]> entry : permittedExtras.entrySet()) {
                String contentType = entry.getKey();
                try {
                    ContentTypeHeader cth = ContentTypeHeader.parseValue(contentType);
                    if (partType.matches(cth)) {
                        constraints = entry.getValue();
                    }
                }
                catch(IOException ioe) {
                    logger.warning("Ignoring invalid content type header (extra), value is '"+contentType+"'.");
                }
            }
        }

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
           auditor.logAndAudit(AssertionMessages.SWA_EXTRA_LENGTH_EXCEEDED, Long.toString(configuredMaxLn));
        }

        return valid;
    }

    /**
     * Exception used to end attachment processing with the given status
     */
    private static final class AttachmentProcessingException extends Exception {
        private AssertionStatus assertionStatus;

        public AttachmentProcessingException(String message, AssertionStatus assertionStatus) {
            super(message);
            this.assertionStatus = assertionStatus;
        }

        public AssertionStatus getAssertionStatus() {
            return assertionStatus;
        }
    }
}
