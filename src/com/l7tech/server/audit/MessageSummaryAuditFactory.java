/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.common.audit.AuditDetail;
import com.l7tech.common.audit.AuditDetailEvent;
import com.l7tech.common.audit.MessageProcessingMessages;
import com.l7tech.common.audit.MessageSummaryAuditDetail;
import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.message.TcpKnob;
import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.service.PublishedService;

import javax.wsdl.Operation;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A MessageSummaryAuditRecord must be generated upon the conclusion of the processing of a message,
 * whether successful or otherwise.
 *
 * @author alex
 * @version $Revision$
 */
public class MessageSummaryAuditFactory {
    private final String nodeId;
    private static final Logger logger = Logger.getLogger(MessageSummaryAuditFactory.class.getName());

    public MessageSummaryAuditFactory(String nodeId) {
        if (nodeId == null) {
            throw new IllegalArgumentException("Cluster Node ID is required");
        }
        this.nodeId = nodeId;
    }

    public MessageSummaryAuditRecord makeEvent(final PolicyEnforcementContext context, AssertionStatus status ) {
        String requestXml = null;
        int requestContentLength = -1;
        String responseXml = null;
        int responseContentLength = -1;
        long identityProviderOid = -1;
        String userName = null;
        String clientAddr = null;
        long serviceOid = -1;
        String serviceName = null;
        boolean authenticated;
        String userId = null;

        // Service info
        PublishedService service = context.getService();
        if (service != null) {
            serviceOid = service.getOid();
            serviceName = service.getName();
        }

        // User info
        // TODO refactor into context.glorkUsernameSomehow()
        authenticated = context.isAuthenticated();
        if ( authenticated ) {
            User u = context.getAuthenticatedUser();
            if (u == null) {
                LoginCredentials creds = context.getCredentials();
                if (creds != null) userName = creds.getLogin();
            } else {
                identityProviderOid = u.getProviderId();
                userId = u.getUniqueIdentifier();
                userName = u.getName();
                if (userName == null) userName = u.getLogin();
            }
        }

        // Request info
        Message request = context.getRequest();
        String requestId;
        requestId = context.getRequestId().toString();
        if (context.isAuditSaveRequest()) {
            try {
                byte[] req = HexUtils.slurpStream(request.getMimeKnob().getFirstPart().getInputStream(true));
                String encoding = request.getMimeKnob().getFirstPart().getContentType().getEncoding();
                requestXml = new String(req, encoding);
                requestContentLength = requestXml.length();
            } catch (Throwable t) {
                requestXml = null;
            }

            if ( requestContentLength == -1 && requestXml != null ) requestContentLength = requestXml.length();
        }

        TcpKnob reqTcp = (TcpKnob)request.getKnob(TcpKnob.class);
        if (reqTcp != null)
            clientAddr = reqTcp.getRemoteAddress();

        // Response info
        Message response = context.getResponse();
        if (context.isAuditSaveResponse()) {
            if (response.getKnob(MimeKnob.class) != null) {
                try {
                    byte[] resp = HexUtils.slurpStream(response.getMimeKnob().getFirstPart().getInputStream(false));
                    String encoding = response.getMimeKnob().getFirstPart().getContentType().getEncoding();
                    responseXml = new String(resp, encoding);
                } catch (Throwable t) {
                    responseXml = null;
                }
            }

            if (responseXml != null) responseContentLength = responseXml.length();
        }

        int responseHttpStatus = -1;
        HttpResponseKnob respKnob = (HttpResponseKnob)response.getKnob(HttpResponseKnob.class);
        if (respKnob != null) {
            responseHttpStatus = respKnob.getStatus();
        }

        long start = context.getRoutingStartTime();
        if (start <= 0) start = System.currentTimeMillis();
        long end = context.getRoutingEndTime();
        if (end <= 0) end = start;

        int routingLatency = (int)(end - start);

        Object operationNameHaver = new Object() {
            public String toString() {
                return getOperationName(context);
            }
        };

        return new MessageSummaryAuditRecord(context.getAuditLevel(), nodeId, requestId, status, clientAddr,
                                             context.isAuditSaveRequest() ? requestXml : null,
                                             requestContentLength,
                                             context.isAuditSaveResponse() ? responseXml : null,
                                             responseContentLength,
                                             responseHttpStatus,
                                             routingLatency,
                                             serviceOid, serviceName, operationNameHaver,
                                             authenticated, identityProviderOid, userName, userId);
    }

    public AuditDetail makeEvent(PolicyEnforcementContext context, String faultMessage) {
        AuditDetail detail = null;

        if (context.isAuditSaveResponse()) {
            detail = new MessageSummaryAuditDetail(faultMessage);              
        }

        return detail;
    }


    private String getOperationName(PolicyEnforcementContext context) {
        String operationName = null;
        try {
            Operation op = context.getOperation();
            if (op != null) operationName = op.getName();
        } catch (Exception e) {
            logger.log(Level.INFO, "Couldn't determine operation name: " + e.toString());
        }
        return operationName;
    }

}
