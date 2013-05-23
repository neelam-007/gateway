/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.PartInfo;
import com.l7tech.gateway.common.audit.AuditDetail;
import com.l7tech.gateway.common.audit.MessageSummaryAuditDetail;
import com.l7tech.gateway.common.audit.MessageSummaryAuditRecord;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.message.MimeKnob;
import com.l7tech.message.TcpKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.mapping.MessageContextMappingManager;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.Charsets;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;

import javax.wsdl.Binding;
import javax.wsdl.Operation;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A MessageSummaryAuditRecord must be generated upon the conclusion of the processing of a message,
 * whether successful or otherwise.
 *
 * @author alex
 * @version $Revision$
 */
public class MessageSummaryAuditFactory implements PropertyChangeListener{
    private final String nodeId;
    private static final Logger logger = Logger.getLogger(MessageSummaryAuditFactory.class.getName());
    private static final Charset FALLBACK_ENCODING = Charsets.ISO8859;

    private MessageContextMappingManager messageContextMappingManager;
    private boolean addMappingsIntoAudit;

    private IdentityProviderFactory identityProviderFactory;

    public MessageSummaryAuditFactory(String nodeId) {
        if (nodeId == null) {
            throw new IllegalArgumentException("Cluster Node ID is required");
        }
        this.nodeId = nodeId;
        addMappingsIntoAudit = ConfigFactory.getBooleanProperty(ServerConfigParams.PARAM_ADD_MAPPINGS_INTO_AUDIT,false);
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
        final AuthenticationContext authContext = context.getDefaultAuthenticationContext();
        final User authUser;
        authenticated = authContext.isAuthenticated();
        if (authenticated) {
            StringBuffer usernamebuf = new StringBuffer();
            StringBuffer useridbuf = new StringBuffer();
            List<AuthenticationResult> allCreds = authContext.getAllAuthenticationResults();
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
            authUser = authContext.getLastAuthenticatedUser();
        } else {
            authUser = null;
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

            TcpKnob reqTcp = request.getKnob(TcpKnob.class);
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
            HttpResponseKnob respKnob = response.getKnob(HttpResponseKnob.class);
            if (respKnob != null) {
                responseHttpStatus = respKnob.getStatus();
            }
        }

        long start = context.getRoutingStartTime();
        if (start <= 0) start = System.currentTimeMillis();
        long end = context.getRoutingEndTime();
        if (end <= 0) end = start;

        int routingLatency = (int)(end - start);

        final Object operationNameHaver = new Object() {
            public String toString() {
                return getOperationName(context);
            }
        };

        // Mapping info
        Number mapping_values_oid = null;
        if (addMappingsIntoAudit) {
            mapping_values_oid = new Number(){
                private long mvoid=Long.MIN_VALUE;
                public long longValue() {
                    if ( mvoid==Long.MIN_VALUE ) {
                        Long result = saveMessageContextMapping(operationNameHaver, authUser, context.getMappings());
                        mvoid = result == null ? -1 : result;
                    }
                    return mvoid;
                }
                public int intValue() { return (int)longValue(); }
                public float floatValue() { return (float)longValue(); }
                public double doubleValue() { return (double)longValue(); }
            };
        }

        MessageSummaryAuditRecord ret = new MessageSummaryAuditRecord(context.getAuditLevel(), nodeId, requestId, status, clientAddr,
                                             context.isAuditSaveRequest() ? requestXml : null,
                                             requestContentLength,
                                             context.isAuditSaveResponse() ? responseXml : null,
                                             responseContentLength,
                                             responseHttpStatus,
                                             routingLatency,
                                             serviceOid, serviceName, operationNameHaver,
                                             authenticated, authType, identityProviderOid, userName, userId,
                                             mapping_values_oid);
        ret.originalPolicyEnforcementContext(context);
        return ret;
    }

    public void setMessageContextMappingManager(MessageContextMappingManager messageContextMappingManager) {
        this.messageContextMappingManager = messageContextMappingManager;
    }

    public void setIdentityProviderFactory(IdentityProviderFactory identityProviderFactory) {
        this.identityProviderFactory = identityProviderFactory;
    }

    public void propertyChange(PropertyChangeEvent event) {
        if ( ServerConfigParams.PARAM_ADD_MAPPINGS_INTO_AUDIT.equals(event.getPropertyName())) {
            if (Boolean.valueOf((String)event.getNewValue())) {
                addMappingsIntoAudit = true;
            } else {
                addMappingsIntoAudit = false;
                logger.info("Adding message context mappings to Gateway Audit Events is currently disabled.");
            }
        }
    }

    private Long saveMessageContextMapping(Object operationHaver, User user, List<MessageContextMapping> mappings) {
        if (messageContextMappingManager == null) return null;

        MessageContextMappingKeys keysEntity = new MessageContextMappingKeys();
        MessageContextMappingValues valuesEntity = new MessageContextMappingValues();

        keysEntity.setCreateTime(System.currentTimeMillis());
        valuesEntity.setCreateTime(System.currentTimeMillis());
        valuesEntity.setServiceOperation(operationHaver.toString());

        if ( mappings != null ) {
            int index = 0;
            for ( MessageContextMapping mapping : mappings ) {
                if ( mapping.getMappingType() == MessageContextMapping.MappingType.AUTH_USER ) {
                    if (user != null) {
                        valuesEntity.setAuthUserId(describe(user.getProviderId(), user.getId()));
                        valuesEntity.setAuthUserUniqueId(user.getId());
                        valuesEntity.setAuthUserProviderId( user.getProviderId() );
                    }
                } else {
                    keysEntity.setTypeAndKey(index, mapping.getMappingType(), mapping.getKey());
                    valuesEntity.setValue(index, mapping.getValue());
                    index++;
                }
            }
        }

        try {
            long mapping_keys_oid = messageContextMappingManager.saveMessageContextMappingKeys(keysEntity);
            valuesEntity.setMappingKeysOid(mapping_keys_oid);

            return messageContextMappingManager.saveMessageContextMappingValues(valuesEntity);
        } catch (Exception e) {
            logger.warning("Failed to save the keys or values of the message context mapping.");
            return null;
        }
    }

    private String getMessageBodyText(Message msg, int[] lengthHolder, boolean isRequest) {
        String what = isRequest ? "request" : "response";
        try {
            final MimeKnob mk = msg.getKnob(MimeKnob.class);
            if (mk == null || !msg.isInitialized()) {
                logger.fine(MessageFormat.format("{0} has not been initialized; not attempting to save body text", what));
                lengthHolder[0] = 0;
                return null;
            }
            final PartInfo part = mk.getFirstPart();
            byte[] req = IOUtils.slurpStream(part.getInputStream(false));
            lengthHolder[0] = req.length;
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

    public AuditDetail makeEvent(PolicyEnforcementContext context, String faultMessage) {
        AuditDetail detail = null;

        if (context.isAuditSaveResponse()) {
            detail = new MessageSummaryAuditDetail(faultMessage);              
        }

        return detail;
    }

    private SecurityTokenType authType(PolicyEnforcementContext context) {
        SecurityTokenType authType = null;
        LoginCredentials creds = context.getDefaultAuthenticationContext().getLastCredentials();
        if (creds != null) {
            authType = creds.getType();
        }
        return authType;
    }

    private String getOperationName(PolicyEnforcementContext context) {
        String operationName = null;
        try {
            final Pair<Binding,Operation> pair = context.getBindingAndOperation();
            if(pair != null){
                operationName = pair.right.getName();
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "Couldn't determine operation name: " + e.toString());
        }
        return operationName;
    }

    private String describe( final Long providerOid, final String userId ) {
        String description = null;

        try {
            final IdentityProvider provider = identityProviderFactory.getProvider( providerOid );
            if ( provider != null ) {
                final User user = provider.getUserManager().findByPrimaryKey( userId );
                description =
                        (user==null ? userId : getUserDescription(user)) +
                        " [" + provider.getConfig().getName() + "]";
            }
        } catch ( Exception fe ) {
            logger.log( Level.WARNING, "Error accessing user details.", fe );
        }

        if ( description == null ) {
            description = userId + " [#" + providerOid + "]";
        }

        return description;
    }

    private String getUserDescription( final User user ) {
        String userName = user.getLogin();
        if (userName == null || "".equals(userName)) userName = user.getName();
        if (userName == null || "".equals(userName)) userName = user.getId();
        return userName;
    }
     
}
