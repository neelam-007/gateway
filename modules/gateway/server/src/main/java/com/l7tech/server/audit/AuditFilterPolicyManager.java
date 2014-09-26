/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * @author darmstrong
 */
package com.l7tech.server.audit;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.util.*;
import com.l7tech.xml.SoapFaultLevel;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Invoke the audit-message-filter or audit-viewer internal policies.
 */
public class AuditFilterPolicyManager {

    // - PUBLIC

    public AuditFilterPolicyManager(PolicyCache policyCache, StashManagerFactory stashManagerFactory) {
        this.policyCache = policyCache;
        this.stashManagerFactory = stashManagerFactory;
    }

    public boolean isAuditViewerPolicyAvailable() {
        final Set<String> policyViewerPolicies =
                policyCache.getPoliciesByTypeAndTag(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_VIEWER);

        return !policyViewerPolicies.isEmpty();
    }

    /**
     * Filter the request and response messages if they are included in the AuditRecord by running them through
     * the {@link PolicyType#TAG_AUDIT_MESSAGE_FILTER} internal policy. The actual request and response messages in the
     * context are not modified, only the requestXml and responseXml properties of the audit record.
     * <p/>
     * The Audit Message Filter policy is ran for the request and response independently.
     * <p/>
     * If the Audit Message Filter policy does not return status {@link AssertionStatus#NONE} for a message, then the
     * appropriate field 'requestXml' or 'responseXml' will be set to null.
     *
     * @param auditRecord the audit record which may contain request or response messages to filter. If the audit record
     *                    is not a message audit then nothing in the AuditRecord is modified. Cannot be null.
     * @param context     PolicyEnforcementContext which contains the original request and response. Cannot be null.
     * @param listener    AuditLogListener to notify if any audit details are added to the audi record in case of AMF failure.
     * @param formatter   AuditLogFormatter to use for formatting any audit / log messages created.
     */
    public void filterAuditRecord(final AuditRecord auditRecord,
                                  final PolicyEnforcementContext context,
                                  final AuditLogListener listener,
                                  final AuditLogFormatter formatter) {
        if (!(auditRecord instanceof MessageSummaryAuditRecord)) {
            return;
        }

        final MessageSummaryAuditRecord messageAudit = (MessageSummaryAuditRecord) auditRecord;
        final String requestXml = messageAudit.getRequestXml();
        final String responseXml = messageAudit.getResponseXml();

        if (requestXml == null && responseXml == null) {
            return;
        }

        final Set<String> guids = policyCache.getPoliciesByTypeAndTag(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_MESSAGE_FILTER);

        if (guids.isEmpty() || guids.iterator().next() == null) {
            return;
        }

        //only support a single policy at the moment
        final String guid = guids.iterator().next();
        ServerPolicyHandle handle = null;
        try {
            handle = policyCache.getServerPolicy(guid);
            if (handle != null) {
                if (requestXml != null) {
                    processMessage(messageAudit, context.getRequest(), true, handle, listener, formatter);
                }

                if (responseXml != null) {
                    final Message responseMsg = context.getResponse();
                    if (responseMsg.getKnob(MimeKnob.class) != null && responseMsg.isInitialized()) {
                        processMessage(messageAudit, responseMsg, false, handle, listener, formatter);
                    } else {
                        //no response, in this case a fault is being returned.
                        processResponseFault(context.getFaultlevel(), messageAudit, responseXml, handle, listener, formatter);
                    }
                }
            }
        } finally {
            ResourceUtils.closeQuietly(handle);
        }
    }

    /**
     * Evaluate the audit viewer policy for the input String xml. If messageXml does not contain valid XML, then it will
     * not be possible to invoke the audit viewer policy.
     *
     * @param messageXml audit message (request / response or audit detail text) to run through the audit viewer policy
     * @param isRequest  true if the message is the request, false otherwise, null if neither.
     * @return String output from the audit viewer policy. Null if the audit viewer policy did not return
     *         {@link com.l7tech.policy.assertion.AssertionStatus#NONE}.
     * @throws AuditPolicyException any problem parsing the messageXml or executing the audit viewer policy.
     * @throws AuditViewerPolicyNotAvailableException if the audit viewer policy is not available.
     */
    public String evaluateAuditViewerPolicy(final String messageXml,
                                            final Boolean isRequest)
            throws AuditPolicyException {
        final Set<String> guids = policyCache.getPoliciesByTypeAndTag(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_VIEWER);

        if (guids.isEmpty() || guids.iterator().next() == null) {
            throw new AuditViewerPolicyNotAvailableException("Audit viewer policy is not available");
        }

        final Message message;
        try {
            message = new Message(XmlUtil.parse(messageXml));
        } catch (Exception e) {
            throw new AuditPolicyException("Cannot create message from saved audit record: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
        }

        final String guid = guids.iterator().next();
        ServerPolicyHandle handle = null;
        try {
            handle = policyCache.getServerPolicy(guid);
            if (handle != null) {
                return evaluatePolicy(message, handle, isRequest, PolicyType.TAG_AUDIT_VIEWER);
            }
            //should not happen
            throw new AuditViewerPolicyNotAvailableException("Could not find audit viewer policy in policy cache.");
        } finally {
            ResourceUtils.closeQuietly(handle);
        }
    }

    // - PRIVATE

    private void processResponseFault(final SoapFaultLevel faultlevel,
                                      final MessageSummaryAuditRecord messageAudit,
                                      final String responseFaultXml,
                                      final ServerPolicyHandle handle,
                                      final AuditLogListener listener,
                                      final AuditLogFormatter formatter) {
        //fault may be any content type as the Customize Error Response assertion can change the content type of a fault.
        //the actual contents of the customized fault will already be in responseFaultXml
        ContentTypeHeader typeHeader = null;
        if (faultlevel != null && faultlevel.getFaultTemplateContentType() != null) {
            final String contentType = faultlevel.getFaultTemplateContentType();
            try {
                typeHeader = ContentTypeHeader.parseValue(contentType);
                //no exception simply means that the type is syntactically correct, the type may not exist.
                //this is fine so long as the AMF policy can deal with it, which the default AMF policy can.
                //this should not happen as Customize Error Response only allows valid content types.
            } catch (IOException e) {
                final String msg = "Cannot parse content type of response fault message: " + contentType;
                auditProcessingProblem(messageAudit, listener, formatter, msg, ExceptionUtils.getDebugException(e), false);
            }
        }

        if(typeHeader == null){
            typeHeader = ContentTypeHeader.XML_DEFAULT;//has default encoding - UTF8
        }

        final Message faultMsg = new Message();
        try {
            faultMsg.initialize(typeHeader, responseFaultXml.getBytes(typeHeader.getEncoding()));
        } catch (IOException e) {
            final String msg = "Cannot create message from response fault message: " + ExceptionUtils.getMessage(e);
            auditProcessingProblem(messageAudit, listener, formatter, msg, ExceptionUtils.getDebugException(e), true);
            auditThatMessageFilterPolicyFailed(messageAudit, listener, formatter, false, null);
            messageAudit.setResponseXml(null);
            //cannot process message
            return;
        }

        String output = null;
        try {
            output = filterMessage(faultMsg, handle, false);
        } catch (AuditPolicyException e) {
            auditThatMessageFilterPolicyFailed(messageAudit, listener, formatter, false, e);
        } finally {
            messageAudit.setResponseXml(output);
        }
    }

    private void processMessage(final MessageSummaryAuditRecord messageAudit,
                                final Message realMessage,
                                final boolean isRequest,
                                final ServerPolicyHandle handle,
                                final AuditLogListener listener,
                                final AuditLogFormatter formatter) {
        String output = null;
        try {
            output = processMessage(realMessage, isRequest, handle);
        } catch (AuditPolicyException e) {
            auditThatMessageFilterPolicyFailed(messageAudit, listener, formatter, isRequest, e);
        } catch (CannotGetMessageBodyException e) {
            auditProcessingProblem(messageAudit, listener, formatter, ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e), true);
            auditThatMessageFilterPolicyFailed(messageAudit, listener, formatter, isRequest, e);
        } finally {
            if (isRequest) {
                messageAudit.setRequestXml(output);
            } else {
                messageAudit.setResponseXml(output);
            }
        }
    }

    private String processMessage(final Message realMessage, final boolean isRequest, final ServerPolicyHandle handle)
            throws AuditPolicyException, CannotGetMessageBodyException {
        final Message copyMsg = copyMessageFirstPart(realMessage, isRequest);
        return filterMessage(copyMsg, handle, isRequest);
    }

    /**
     * Filter the message through the AMF. The parameter 'copyMsg' should be a copy of the original Message.
     *
     * @param copyMsg   copy of the Message to be filter. Cannot be null. It will be modified and closed by this method.
     * @param handle    Service policy handle, cannot be null.
     * @param isRequest true when Message is the request.
     * @return modified message XML. If the AMF did not return a status of NONE, then null is returned. This is audited.
     */
    private String filterMessage(final Message copyMsg,
                                 final ServerPolicyHandle handle,
                                 final boolean isRequest)
            throws AuditPolicyException {
        return evaluatePolicy(copyMsg, handle, isRequest, PolicyType.TAG_AUDIT_MESSAGE_FILTER);
    }

    /**
     * Evaluate a policy, {@link PolicyType#TAG_AUDIT_MESSAGE_FILTER} or {@link PolicyType#TAG_AUDIT_VIEWER}, and
     * return contents of it's Request as a String following policy execution, provided the policy returned a status
     * code of 4{@link AssertionStatus#NONE}.
     *  
     * @return Contents of Request message after policy completes, providing status was AssertionStatus.NONE. null is
     *         returned for any other status.
     */
    private String evaluatePolicy(final Message copyMsg,
                                  final ServerPolicyHandle handle,
                                  final Boolean isRequest,
                                  final String policyType)
            throws AuditPolicyException {
        PolicyEnforcementContext pec = null;
        final String[] capturePolicyRequestOutput = new String[1];
        final CannotGetMessageBodyException[] caughtException = new CannotGetMessageBodyException[1];
        AssertionStatus result;

        try {
            pec = PolicyEnforcementContextFactory.createUnregisteredPolicyEnforcementContext(copyMsg, null, false);
            final PolicyEnforcementContext finalCtx = pec;

            //Our runnable must run before any runnable which may close the incoming message.
            finalCtx.runOnCloseFirst(new Runnable() {
                @Override
                public void run() {
                    try {
                        capturePolicyRequestOutput[0] = getMessageBodyTextOrErrorMsg(finalCtx.getRequest(), isRequest);
                    } catch (CannotGetMessageBodyException e) {
                        caughtException[0] = e;
                    }
                }
            });

            result = AuditContextFactory.doWithCustomAuditContext( AuditContextFactory.createLogOnlyAuditContext(), new Callable<AssertionStatus>() {
                @Override
                public AssertionStatus call() throws Exception {
                    return handle.checkRequest( finalCtx );
                }
            } );
        } catch (Exception e) {
            return handlePolicyException(e, policyType);
        } finally {
            ResourceUtils.closeQuietly(pec);
        }

        //pec must be closed first
        if (caughtException[0] == null && result == AssertionStatus.NONE) {
            return capturePolicyRequestOutput[0];
        }

        if (caughtException[0] != null) {
            handlePolicyException(caughtException[0], policyType);
        }

        throw new AuditPolicyException("Audit policy '" + policyType + "' returned status '" + result.getMessage() + "'");
    }

    private String handlePolicyException(final Exception e,
                                         final String policyType) throws AuditPolicyException {
        final String msg = "Unexpected exception caught from audit '" + policyType + "'policy : " +
                ExceptionUtils.getMessage(e);
        throw new AuditPolicyException(msg, ExceptionUtils.getDebugException(e));
    }

    private void auditProcessingProblem(
            final MessageSummaryAuditRecord messageAudit,
            final AuditLogListener listener,
            final AuditLogFormatter formatter,
            final String warningAuditMessage,
            final Exception exceptionIfAny, boolean isWarning) {
        auditMessage(messageAudit,
                (isWarning) ? Messages.EXCEPTION_WARNING_WITH_MORE_INFO : Messages.EXCEPTION_INFO_WITH_MORE_INFO,
                new String[]{warningAuditMessage}, exceptionIfAny, listener, formatter);
    }

    private void auditThatMessageFilterPolicyFailed(
            final MessageSummaryAuditRecord messageAudit,
            final AuditLogListener listener,
            final AuditLogFormatter formatter,
            final boolean isRequest,
            final Exception exceptionIfAny) {

        String extraMessage = "";
        if(exceptionIfAny != null){
            if(exceptionIfAny instanceof AuditPolicyException || exceptionIfAny instanceof CannotGetMessageBodyException){
                extraMessage = ExceptionUtils.getMessage(exceptionIfAny);
            }
        }

        final String[] params = new String[]{extraMessage, (isRequest) ? "Request" : "Response"};

        auditMessage(messageAudit, SystemMessages.AUDIT_MESSAGE_FILTER_POLICY_FAILED, params, ExceptionUtils.getDebugException(exceptionIfAny), listener, formatter);
    }

    private void auditMessage(final MessageSummaryAuditRecord messageAudit,
                              Messages.M auditMessage,
                              final String[] params,
                              final Exception exception,
                              final AuditLogListener listener,
                              final AuditLogFormatter formatter) {
        final AuditDetail auditDetail = new AuditDetail(auditMessage, params);
        final AuditDetail[] orderedDetails = messageAudit.getDetailsInOrder();
        final int maxOrdinal;
        if (orderedDetails.length > 0) {
            maxOrdinal = orderedDetails[orderedDetails.length - 1].getOrdinal();
            auditDetail.setOrdinal(maxOrdinal + 1);
        } else {
            auditDetail.setOrdinal(0);
        }

        auditDetail.setAuditRecord(messageAudit);
        messageAudit.getDetails().add(auditDetail);

        listener.notifyDetailFlushed(
                this.getClass().getName(),
                logger.getName(),
                auditMessage,
                params,
                formatter,
                exception);
    }

    //todo: The message is being copied as the AuditRecord's blob of request / response text does not have content type info.
    //todo: Work around the need to have to copy the request / response. Obtain the content type info from the PEC or else update audit records to have this info.
    //todo: If messages are large, running them through the AMF adds unnecessary memory usage overhead.
    private Message copyMessageFirstPart(Message msg, boolean isRequest) throws CannotGetMessageBodyException {
        String what = isRequest ? "request" : "response";
        try {
            final MimeKnob mk = msg.getMimeKnob();
            final PartInfo part = mk.getFirstPart();

            final Message copiedMsg = new Message();
            copiedMsg.initialize(stashManagerFactory.createStashManager(), part.getContentType(), part.getInputStream(false));

            return copiedMsg;

        } catch (Exception e) {
            String errMsg = MessageFormat.format("Cannot copy message, unable to get {0} XML: {1}", what, e.getMessage());
            throw new CannotGetMessageBodyException(errMsg, ExceptionUtils.getDebugException(e));
        }
    }

    private String getMessageBodyTextOrErrorMsg(final Message msg, final Boolean isRequest) throws CannotGetMessageBodyException {
        String what = (isRequest != null) ? isRequest ? "request" : "response" : "audit detail";
        try {
            final MimeKnob mk = msg.getMimeKnob();
            final PartInfo part = mk.getFirstPart();
            byte[] req = IOUtils.slurpStream(part.getInputStream(false));
            ContentTypeHeader cth = part.getContentType();
            Charset encoding = null;
            if (cth != null && cth.isTextualContentType()) {
                encoding = cth.getEncoding();
            } else {
                logger.log(Level.INFO, MessageFormat.format("Content-Type of {0} (\"{1}\") is unknown or not text; using {2} to save {0} text",
                        what, cth == null ? "null" : cth.getFullValue(), FALLBACK_ENCODING));
            }
            if (encoding == null) encoding = FALLBACK_ENCODING;
            return new String(req, encoding);
        } catch (Exception e) {
            String errMsg = MessageFormat.format("Unable to get {0} XML: {1}", what, e.getMessage());
            throw new CannotGetMessageBodyException(errMsg, ExceptionUtils.getDebugException(e));
        }
    }

    private static class CannotGetMessageBodyException extends Exception {
        private CannotGetMessageBodyException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private final PolicyCache policyCache;
    private final StashManagerFactory stashManagerFactory;

    private static final Charset FALLBACK_ENCODING = Charsets.ISO8859;
    private static final Logger logger = Logger.getLogger(AuditFilterPolicyManager.class.getName());
}
