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
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.PolicyCache;
import com.l7tech.server.policy.ServerPolicyHandle;
import com.l7tech.util.*;

import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Invoke the audit-message-filter or audit-viewer internal policies.
 */
public class AuditFilterPolicyManager {

    // - PUBLIC

    public AuditFilterPolicyManager(PolicyCache policyCache) {
        this.policyCache = policyCache;
    }

    public boolean isAuditViewerPolicyAvailable(){
        final Set<String> policyViewerPolicies =
                policyCache.getPoliciesByTypeAndTag(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_VIEWER);

        return !policyViewerPolicies.isEmpty();
    }
    
    /**
     * Filter the request and response messages if they are included in the AuditRecord by running them through
     * the {@link PolicyType#TAG_AUDIT_MESSAGE_FILTER} internal policy. The actual request and response messages in the
     * context are not modified, only the requestXml and responseXml properties of the audit record.
     *
     * The Audit Message Filter policy is ran for the request and response independently.
     *
     * @param auditRecord the audit record which may contain request or response messages to filter. If the audit record
     * is not a message audit then nothing in the AuditRecord is modified. Cannot be null.
     * @param context PolicyEnforcementContext which contains the original request and response. Cannot be null.
     * @param listener AuditLogListener to notify if any audit details are added to the audi record in case of AMF failure.
     * @param formatter AuditLogFormatter to use for formatting any audit / log messages created.  
     */
    public void filterAuditRecord(final AuditRecord auditRecord,
                                  final PolicyEnforcementContext context,
                                  final AuditLogListener listener,
                                  final AuditLogFormatter formatter)  {
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
            handle = policyCache.getServerPolicy( guid );
            if ( handle != null ) {
                if(requestXml != null){
                    final Message reqMsg = context.getRequest();
                    final Message copyMsg = copyMessageFirstPart(reqMsg, true);
                    messageAudit.setRequestXml(filterMessage(messageAudit, copyMsg, handle, listener, formatter, true));
                }

                if(responseXml != null){
                    final Message responseMsg = context.getResponse();
                    final Message copyMsg = copyMessageFirstPart(responseMsg, false);
                    messageAudit.setResponseXml(filterMessage(messageAudit, copyMsg, handle, listener, formatter, false));
                }
            }
        } finally {
            ResourceUtils.closeQuietly( handle );
        }
    }

    /**
     * Evaluate the audit viewer policy for the input String xml. If messageXml does not contain valid XML, then it will
     * not be possible to invoke the audit viewer policy.
     *
     * @param messageXml audit message (request / response or audit detail text) to run through the audit viewer policy
     * @param isRequest true if the message is the request, false otherwise, null if neither.
     * @return String output from the audit viewer policy. Null if the audit viewer policy did not return
     * {@link com.l7tech.policy.assertion.AssertionStatus#NONE}.
     * @throws Exception any problem parsing the messageXml or executing the audit viewer policy.
     */
    public String evaluateAuditViewerPolicy(final String messageXml,
                                            final Boolean isRequest) throws Exception{
        final Set<String> guids = policyCache.getPoliciesByTypeAndTag(PolicyType.INTERNAL, PolicyType.TAG_AUDIT_VIEWER);

        if (guids.isEmpty() || guids.iterator().next() == null) {
            return null;
        }

        final Message message;
        try {
            message = new Message(XmlUtil.parse(messageXml));
        } catch (Exception e) {
            throw new Exception("Cannot create message from saved audit record: " + ExceptionUtils.getMessage(e),
                    ExceptionUtils.getDebugException(e));
        }

        final String guid = guids.iterator().next();
        ServerPolicyHandle handle = null;
        try {
            handle = policyCache.getServerPolicy( guid );
            if ( handle != null ) {
                return evaluatePolicy(message, handle, isRequest);
            }
        } finally {
            ResourceUtils.closeQuietly( handle );
        }

        return null;
    }

    // - PRIVATE

    /**
     * Filter the message through the AMF. The parameter 'copyMsg' should be a copy of the original Message.
     *
     * @param messageAudit if the AMF fails an audit detail will be added to the audit record.
     * @param copyMsg   copy of the Message to be filter. Cannot be null. It will be modified and closed by this method.
     * @param handle    Service policy handle, cannot be null.
     * @param isRequest true when Message is the request.
     * @return modified message XML. If the AMF did not return a status of NONE, then null is returned. This is audited.
     */
    private String filterMessage(final MessageSummaryAuditRecord messageAudit,
                                 final Message copyMsg,
                                 final ServerPolicyHandle handle,
                                 final AuditLogListener listener,
                                 final AuditLogFormatter formatter,
                                 final boolean isRequest) {

        try {
            final String output = evaluatePolicy(copyMsg, handle, isRequest);
            if(output != null){
                return output;
            }
            addAuditMessageFilterPolicyFailedAudit(messageAudit, listener, formatter, isRequest, null);
        } catch (Exception e) {
            addAuditMessageFilterPolicyFailedAudit(messageAudit, listener, formatter, isRequest, ExceptionUtils.getDebugException(e));
        }

        return null;
    }

    /**
     *
     * @return Contents of Request message after policy completes, providing status was AssertionStatus.NONE. null is
     * retunred for any other stauts.
     * @throws Exception Any problems executing policy.
     */
    private String evaluatePolicy(final Message copyMsg,
                                  final ServerPolicyHandle handle,
                                  final Boolean isRequest) throws Exception {
        PolicyEnforcementContext pec = null;
        final String[] capturePolicyRequestOutput = new String[1];
        AssertionStatus result;

        try {
            pec = PolicyEnforcementContextFactory.createUnregisteredPolicyEnforcementContext(copyMsg, null, false);
            final PolicyEnforcementContext finalCtx = pec;

            //Our runnable must run before any runnable which may close the incoming message.
            finalCtx.runOnCloseFirst(new Runnable() {
                @Override
                public void run() {
                    capturePolicyRequestOutput[0] = getMessageBodyTextOrErrorMsg(finalCtx.getRequest(), isRequest);
                }
            });
            result = handle.checkRequest(finalCtx);
        } finally {
            ResourceUtils.closeQuietly(pec);
        }

        //pec must be closed first
        if(result == AssertionStatus.NONE){
            return capturePolicyRequestOutput[0];
        }

        //fall through for any other status
        return null;
    }


    private void addAuditMessageFilterPolicyFailedAudit(
            final MessageSummaryAuditRecord messageAudit,
            final AuditLogListener listener,
            final AuditLogFormatter formatter,
            final boolean isRequest,
            final Exception exceptionIfAny){
        final AuditDetail auditDetail = new AuditDetail(SystemMessages.AUDIT_MESSAGE_FILTER_POLICY_FAILED, (isRequest) ? "request" : "response");
        final AuditDetail[] orderedDetails = messageAudit.getDetailsInOrder();
        final int maxOrdinal = orderedDetails[orderedDetails.length - 1].getOrdinal();
        auditDetail.setOrdinal(maxOrdinal + 1);

        auditDetail.setAuditRecord(messageAudit);
        messageAudit.getDetails().add(auditDetail);

        listener.notifyDetailFlushed(
                this.getClass().getName(),
                logger.getName(),
                SystemMessages.AUDIT_MESSAGE_FILTER_POLICY_FAILED,
                new String[]{(isRequest) ? "request" : "response"},
                formatter,
                exceptionIfAny);

    }

    private Message copyMessageFirstPart(Message msg, boolean isRequest) {
        String what = isRequest ? "request" : "response";
        try {
            final MimeKnob mk = msg.getMimeKnob();
            final PartInfo part = mk.getFirstPart();
            byte[] req = IOUtils.slurpStream(part.getInputStream(false));
            ContentTypeHeader cth = part.getContentType();
            final Message copiedMsg = new Message();
            copiedMsg.initialize(cth, req);
            return copiedMsg;
        } catch (Exception e) {
            String errMsg = MessageFormat.format("Unable to get {0} XML: {1}", what, e.getMessage());
            logger.log(Level.WARNING, errMsg);
            return null;
        }
    }

    private String getMessageBodyTextOrErrorMsg(final Message msg, final Boolean isRequest) {
        String what = (isRequest != null)? isRequest ? "request" : "response": "audit detail";
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
            logger.log(Level.WARNING, errMsg);
            return errMsg;
        }
    }

    private final PolicyCache policyCache;
    
    private static final Charset FALLBACK_ENCODING = Charsets.ISO8859;
    private static final Logger logger = Logger.getLogger(AuditFilterPolicyManager.class.getName());
}
