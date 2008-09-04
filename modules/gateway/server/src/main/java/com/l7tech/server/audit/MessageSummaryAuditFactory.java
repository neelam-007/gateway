/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.MessageSummaryAuditDetail;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.TcpKnob;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.common.io.IOUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.mapping.MessageContextMappingManager;
import com.l7tech.server.ServerConfig;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.gateway.common.mapping.MessageContextMapping;

import javax.wsdl.Operation;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * A MessageSummaryAuditRecord must be generated upon the conclusion of the processing of a message,
 * whether successful or otherwise.
 *
 * @author alex
 * @version $Revision$
 */
public class MessageSummaryAuditFactory implements PropertyChangeListener {
    private final String nodeId;
    private static final Logger logger = Logger.getLogger(MessageSummaryAuditFactory.class.getName());
    private static final String FALLBACK_ENCODING = "ISO8859-1";
    private static final Set<String> KNOWN_GOOD_ENCODINGS = new HashSet<String>(Arrays.asList("utf-8", "utf-16", "iso8859-1"));

    private MessageContextMappingManager messageContextMappingManager;
    private boolean addMappingsIntoAudit;

    public MessageSummaryAuditFactory(String nodeId) {
        if (nodeId == null) {
            throw new IllegalArgumentException("Cluster Node ID is required");
        }
        this.nodeId = nodeId;
        addMappingsIntoAudit = Boolean.valueOf(ServerConfig.getInstance().getProperty(ServerConfig.PARAM_ADD_MAPPINGS_INTO_AUDIT));
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
        authenticated = context.isAuthenticated();
        if (authenticated) {
            StringBuffer usernamebuf = new StringBuffer();
            StringBuffer useridbuf = new StringBuffer();
            List<AuthenticationResult> allCreds = context.getAllAuthenticationResults();
            for (AuthenticationResult aARes : allCreds) {
                String tmp = aARes.getUser().getLogin();
                if (tmp == null || tmp.length() < 1) {
                    tmp = aARes.getUser().getName();
                }
                if (usernamebuf.length() > 0) usernamebuf.append(", ");
                usernamebuf.append(tmp);
                tmp = aARes.getUser().getId();
                if (tmp != null && tmp.length() > 0) {
                    if (useridbuf.length() > 0) useridbuf.append(", ");
                    useridbuf.append(tmp);
                }
            }
            if (usernamebuf.length() > 0)
                userName = usernamebuf.toString();
            if (useridbuf.length() > 0)
                userId = useridbuf.toString();
            // todo, refactor so that we record all authentication types
            authType = authType(context);
        }

        // Request info
        String requestId;
        Long mapping_values_oid = null;
        {
            Message request = context.getRequest();
            requestId = context.getRequestId().toString();
            if (context.isAuditSaveRequest()) {
                int[] requestContentLengths = new int[] { -1 };
                requestXml = getMessageBodyText(request, requestContentLengths, true);
                requestContentLength = requestContentLengths[0];
            }

            if (addMappingsIntoAudit) {
                mapping_values_oid = saveMessageContextMapping(context.getMappings());
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
                                             authenticated, authType, identityProviderOid, userName, userId,
                                             mapping_values_oid);
    }

    public void setMessageContextMappingManager(MessageContextMappingManager messageContextMappingManager) {
        this.messageContextMappingManager = messageContextMappingManager;
    }

    public void propertyChange(PropertyChangeEvent event) {
        if (ServerConfig.PARAM_ADD_MAPPINGS_INTO_AUDIT.equals(event.getPropertyName())) {
            if (Boolean.valueOf((String)event.getNewValue())) {
                addMappingsIntoAudit = true;
            } else {
                addMappingsIntoAudit = false;
                logger.info("Adding message context mappings to Gateway Audit Events is currently disabled.");
            }
        }
    }

    private Long saveMessageContextMapping(List<MessageContextMapping> mappings) {
        if (messageContextMappingManager == null) return null;

        MessageContextMappingKeys keysEntity = new MessageContextMappingKeys();
        keysEntity.setCreateTime(System.currentTimeMillis());
        MessageContextMappingValues valuesEntity = new MessageContextMappingValues();
        valuesEntity.setCreateTime(System.currentTimeMillis());

        for (int i = 0; i < mappings.size(); i++) {
            MessageContextMapping mapping = mappings.get(i);
            keysEntity.setTypeAndKey(i, mapping.getMappingType(), mapping.getKey());
            valuesEntity.setValue(i, mapping.getValue());
        }

        try {
            long mapping_keys_oid = messageContextMappingManager.saveMessageContextMappingKeys(keysEntity);
            valuesEntity.setMappingKeysOid(mapping_keys_oid);

            long mapping_values_oid = messageContextMappingManager.saveMessageContextMappingValues(valuesEntity);
            return mapping_values_oid;
        } catch (Exception e) {
            logger.warning("Faied to save the keys or values of the message context mapping.");
            return null;
        }
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
            byte[] req = IOUtils.slurpStream(part.getInputStream(false));
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
