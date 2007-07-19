/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.audit.AuditDetail;
import com.l7tech.common.audit.MessageSummaryAuditDetail;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.common.message.HttpResponseKnob;
import com.l7tech.common.message.Message;
import com.l7tech.common.message.TcpKnob;
import com.l7tech.common.message.MimeKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.common.security.token.SecurityTokenType;
import com.l7tech.common.util.HexUtils;
import com.l7tech.identity.User;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.service.PublishedService;

import javax.wsdl.Operation;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.text.MessageFormat;
import java.io.UnsupportedEncodingException;

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
    private static final String FALLBACK_ENCODING = "ISO8859-1";

    private static final Set<String> KNOWN_GOOD_ENCODINGS = new HashSet<String>(Arrays.asList("utf-8", "utf-16", "iso8859-1"));

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
        SecurityTokenType authType = null;
        String userId = null;

        // Service info
        PublishedService service = context.getService();
        if (service != null) {
            serviceOid = service.getOid();
            serviceName = service.displayName();
        }

        // User info
        // TODO refactor into context.glorkUsernameSomehow()
        authenticated = context.isAuthenticated();
        if ( authenticated ) {
            User u = context.getLastAuthenticatedUser();
            if (u == null) {
                LoginCredentials creds = context.getLastCredentials();
                if (creds != null) userName = creds.getLogin();
            } else {
                identityProviderOid = u.getProviderId();
                userId = u.getId();
                userName = u.getName();
                if (userName == null) userName = u.getLogin();
            }
            authType = authType(context);
        }

        // Request info
        String requestId;
        {
            Message request = context.getRequest();
            requestId = context.getRequestId().toString();
            if (context.isAuditSaveRequest()) {
                int[] requestContentLengths = new int[] { -1 };
                requestXml = getMessageBodyText(request, requestContentLengths, true);
                requestContentLength = requestContentLengths[0];
            }

            TcpKnob reqTcp = (TcpKnob)request.getKnob(TcpKnob.class);
            if (reqTcp != null)
                clientAddr = reqTcp.getRemoteAddress();
        }

        // Response info
        int responseHttpStatus;
        {
            Message response = context.getResponse();
            if (context.isAuditSaveResponse()) {
                int[] responseContentLengths = new int[]{responseContentLength};
                responseXml = getMessageBodyText(response, responseContentLengths, false);
                responseContentLength = responseContentLengths[0];
            }

            responseHttpStatus = -1;
            HttpResponseKnob respKnob = (HttpResponseKnob) response.getKnob(HttpResponseKnob.class);
            if (respKnob != null) {
                responseHttpStatus = respKnob.getStatus();
            }
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
                                             authenticated, authType, identityProviderOid, userName, userId);
    }

    private String getMessageBodyText(Message msg, int[] lengthHolder, boolean isRequest) {
        String what = isRequest ? "request" : "response";
        try {
            final MimeKnob mk = (MimeKnob) msg.getKnob(MimeKnob.class);
            if (mk == null) {
                logger.fine(MessageFormat.format("{0} has not been initialized; not attempting to save body text", what));
                lengthHolder[0] = 0;
                return null;
            }
            final PartInfo part = mk.getFirstPart();
            byte[] req = HexUtils.slurpStream(part.getInputStream(isRequest));
            lengthHolder[0] = req.length;
            ContentTypeHeader cth = part.getContentType();
            String encoding = null;
            if (cth != null && cth.isText()) {
                String declaredEncoding = cth.getEncoding();
                if (KNOWN_GOOD_ENCODINGS.contains(declaredEncoding.toLowerCase())) {
                    encoding = declaredEncoding;
                } else {
                    try {
                        new String(new byte[0], declaredEncoding);
                        encoding = declaredEncoding;
                    } catch (UnsupportedEncodingException e) {
                        logger.log(Level.INFO, MessageFormat.format("Unsupported {0} character encoding \"{1}\"; using {2} to save {0} text", what, declaredEncoding, FALLBACK_ENCODING));
                    }
                }
            } else {
                logger.log(Level.INFO, MessageFormat.format("Content-Type of {0} (\"{1}\") is unknown or not text; using {2} to save {0} text",
                        what, cth == null ? "null" : cth.getFullValue(), FALLBACK_ENCODING));
            }
            if (encoding == null) encoding = FALLBACK_ENCODING;
            return new String(req, encoding);
        } catch (Exception e) {
            logger.log(Level.WARNING, MessageFormat.format("Unable to get {0} XML", what), e);
            return null;
        }
    }

    public AuditDetail makeEvent(PolicyEnforcementContext context, String faultMessage) {
        AuditDetail detail = null;

        if (context.isAuditSaveResponse()) {
            detail = new MessageSummaryAuditDetail(faultMessage);              
        }

        return detail;
    }

    private SecurityTokenType authType(PolicyEnforcementContext context) {
        SecurityTokenType authType = null;
        LoginCredentials creds = context.getLastCredentials();
        if (creds != null) {
            authType = creds.getType();
        }
        return authType;
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
