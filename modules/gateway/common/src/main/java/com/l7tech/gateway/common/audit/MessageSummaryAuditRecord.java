/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.gateway.common.audit;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.Goid;
import com.l7tech.security.rbac.RbacAttribute;
import com.l7tech.objectmodel.SecurityZone;
import com.l7tech.objectmodel.ZoneableEntity;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.util.Functions;
import org.jetbrains.annotations.Nullable;

import javax.persistence.Transient;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.logging.Level;

/**
 * An {@link AuditRecord} that describes the processing of a single message.
 * <p>
 * By default, these are not saved unless the {@link com.l7tech.policy.assertion.AuditAssertion} is used or the
 * SSG's message processing audit threshold is set to {@link Level#INFO} or lower.
 *
 * See also com.l7tech.server.ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD
 *
 * @author alex
 * @version $Revision$
 */
public class MessageSummaryAuditRecord extends AuditRecord implements ZoneableEntity {
    public static final String ATTR_SERVICE_GOID = "serviceGoid";

    private static final long serialVersionUID = 3558223265338839803L;

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    public MessageSummaryAuditRecord() {
    }

    /**
     *
     * @param level the java.util.logging.Level of this record.
     * @param nodeId the ID of the cluster node from which this AuditRecord originates (see com.l7tech.cluster.ClusterStatusAdmin.getClusterStatus())
     * @param clientAddr the IP address of the client that made the request
     * @param requestId the unique ID of the request
     * @param status the {@link AssertionStatus} resulting from applying the service policy to this request
     * @param requestXml the text of the request received from the client. Not saved by default.
     * @param requestContentLength the length of the request, in bytes.
     * @param responseXml the text of the response sent to the client. Not saved by default.
     * @param responseContentLength the length of the response, in bytes.
     * @param serviceGoid the GOID of the {@link PublishedService} this request was resolved to, or {@link PublishedService#DEFAULT_GOID} if it could not be resolved.
     * @param serviceName the name of the {@link PublishedService} this request was resolved to, or null if it could not be resolved.
     * @param operationNameHaver an Object on which toString() will be called if the operation name is needed, or null if one will not be provided.
     * @param authenticated true if the request was authenticated, false otherwise
     * @param authenticationType the authentication type for the request (may be null)
     * @param identityProviderOid the OID of the {@link IdentityProviderConfig IdentityProvider} against which the user authenticated, or {@link IdentityProviderConfig#DEFAULT_GOID} if the request was not authenticated.
     * @param userName the name or login of the user who was authenticated, or null if the request was not authenticated.
     * @param userId the OID or DN of the user who was authenticated, or null if the request was not authenticated.
     * @param mappingValueIdHaver generator to look up a mapping value ID if needed, or null if this won't be supported.
     */
    public MessageSummaryAuditRecord(Level level, String nodeId, String requestId, AssertionStatus status,
                                     String clientAddr, String requestXml, int requestContentLength,
                                     String responseXml, int responseContentLength, int httpRespStatus, int routingLatency,
                                     Goid serviceGoid, String serviceName, Object operationNameHaver,
                                     boolean authenticated, SecurityTokenType authenticationType, Goid identityProviderOid,
                                     String userName, String userId, Functions.Nullary<Goid> mappingValueIdHaver)
    {
        super(level, nodeId, clientAddr, identityProviderOid, userName, userId, serviceName, null);
        StringBuffer msg = new StringBuffer("Message ");
        if (status == AssertionStatus.NONE) {
            if(httpRespStatus >= HttpConstants.STATUS_ERROR_RANGE_START &&
               httpRespStatus < HttpConstants.STATUS_ERROR_RANGE_END) {
                msg.append("processed with HTTP error code");
            }
            else {
                msg.append("processed successfully");
            }
        } else {
            msg.append("was not processed: ");
            msg.append(status.getMessage());
            msg.append(" (").append(status.getNumeric()).append(")");
        }
        this.requestId = requestId;
        this.setMessage(msg.toString());
        this.status = status.getNumeric();
        this.requestXml = requestXml;
        this.requestContentLength = requestContentLength;
        this.responseXml = responseXml;
        this.responseContentLength = responseContentLength;
        this.responseHttpStatus = httpRespStatus;
        this.routingLatency = routingLatency;
        this.operationNameHaver = operationNameHaver;
        this.serviceGoid = serviceGoid;
        this.authenticated = authenticated;
        this.authenticationType = authenticationType;
        this.mappingValueIdHaver = mappingValueIdHaver;
    }

    /**
     * Gets the {@link AssertionStatus} resulting from applying the service policy to this request
     * @return the {@link AssertionStatus} resulting from applying the service policy to this request
     */
    public int getStatus() {
        return status;
    }

    /**
     * Gets the GOID of the {@link PublishedService} this request was resolved to, or {@link PublishedService#DEFAULT_GOID} if it could not be resolved.
     * @return the GOID of the {@link PublishedService} this request was resolved to, or {@link PublishedService#DEFAULT_GOID} if it could not be resolved.
     */
    public Goid getServiceGoid() {
        return serviceGoid;
    }

    @Override
    public String getStrRequestId() {
        return super.getStrRequestId();
    }

    /**
     * @deprecated for serialization use only
     */
    @Deprecated
    @Override
    public void setStrRequestId(String requestId) {
        super.setStrRequestId(requestId);
    }

    /**
     * Gets the text of the request received from the client.
     * @return the text of the request received from the client.
     */
    public String getRequestXml() {
        return requestXml;
    }

    /**
     * Gets the text of the response sent to the client.
     * @return the text of the response sent to the client.
     */
    public String getResponseXml() {
        return responseXml;
    }

    /**
     * Returns true if the request was authenticated, false otherwise
     * @return true if the request was authenticated, false otherwise
     */
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Gets the authentication type for this request (if authenticated)
     * @return the SecurityTokenType or null
     */
    public SecurityTokenType getAuthenticationType() {
        return authenticationType;
    }

    /**
     * Gets the length of the request received from the client, in bytes.
     * @return the length of the request, in bytes.
     */
    public int getRequestContentLength() {
        return requestContentLength;
    }

    /**
     * Gets the length of the response sent to the client, in bytes.
     * @return the length of the response, in bytes.
     */
    public int getResponseContentLength() {
        return responseContentLength;
    }

    /**
     * @return the HTTP status code of the back-end response
     */
    public int getResponseHttpStatus() {
        return responseHttpStatus;
    }

    /**
     * @return the time (in milliseconds) spent routing the request to the protected service
     */
    public int getRoutingLatency() {
        return routingLatency;
    }

    /** @return the name of the operation the request was for if it's a SOAP service, or likely null otherwise */
    @RbacAttribute
    public String getOperationName() {
        if (operationName == null) {
            if (operationNameHaver != null)
                operationName = operationNameHaver.toString();
        }
        return operationName;
    }

    public Goid getMappingValuesId() {
        if ( mappingValuesId == null ) {
            if (mappingValueIdHaver != null) {
                mappingValuesId = mappingValueIdHaver.call();

                // TODO check under what circumstances a negative OID was possible
                if (mappingValuesId != null && mappingValuesId.getHi() == 0 && mappingValuesId.getLow() < 0)
                    mappingValuesId = null;
            }

        }
        return mappingValuesId;
    }

    public void setMappingValuesId(Goid mappingValuesId) {
        this.mappingValueIdHaver = null;
        this.mappingValuesId = mappingValuesId;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    protected void setOperationName(String operationName) {
        this.operationNameHaver = null;
        this.operationName = operationName;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    protected void setStatus( int status ) {
        this.status = status;
    }

    /**
     * Property may be modified by the Audit Message Filter Policy.
     */
    public void setRequestXml( String requestXml ) {
        this.requestXml = requestXml;
    }

    /**
     * Security zone is inherited from the PublishedService that produced this message summary but not mapped by Hibernate for performance reasons.
     *
     * Callers who care about the SecurityZone must ensure that it is attached prior to use.
     *
     * @return the SecurityZone of the PublishedService that produced this message summary. May be null if the SecurityZone was not attached or the PublishedService has no SecurityZone.
     */
    @Nullable
    @Transient
    public SecurityZone getSecurityZone() {
        return securityZone;
    }

    /**
     * Property may be modified by the Audit Message Filter Policy.
     */
    public void setResponseXml( String responseXml ) {
        this.responseXml = responseXml;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    protected void setServiceGoid( Goid serviceGoid ) {
        this.serviceGoid = serviceGoid;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    protected void setAuthenticated( boolean authenticated ) {
        this.authenticated = authenticated;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    public void setAuthenticationType(SecurityTokenType authenticationType) {
        this.authenticationType = authenticationType;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    protected void setRequestContentLength( int requestContentLength ) {
        this.requestContentLength = requestContentLength;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    protected void setResponseContentLength( int responseContentLength ) {
        this.responseContentLength = responseContentLength;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    protected void setResponseHttpStatus(int responseHttpStatus) {
        this.responseHttpStatus = responseHttpStatus;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    @Deprecated
    protected void setRoutingLatency(int routingLatency) {
        this.routingLatency = routingLatency;
    }

    public void setSecurityZone(@Nullable final SecurityZone securityZone) {
        this.securityZone = securityZone;
    }

    public MessageContextMappingValues getMappingValuesEntity() {
        return mappingValuesEntity;
    }

    public void setMappingValuesEntity(MessageContextMappingValues mappingValuesEntity) {
        this.mappingValuesEntity = mappingValuesEntity;
    }

    public MessageContextMapping[] obtainMessageContextMappings() {
        if (mappingValuesEntity == null) return new MessageContextMapping[0];
        MessageContextMappingKeys mappingKeysEntity = mappingValuesEntity.getMappingKeysEntity();
        if (mappingKeysEntity == null) return new MessageContextMapping[0];

        List<MessageContextMapping> mappings = mappingKeysEntity.obtainMappingsWithEmptyValues();
        String[] mappingValues = mappingValuesEntity.obtainValues();
        for (int i = 0; i < mappings.size(); i++) {
            mappings.get(i).setValue(mappingValues[i]);
        }

        return mappings.toArray(new MessageContextMapping[mappings.size()]);
    }

    /**
     * Get the associated original policy enforcement context; used while running within the Gateway.
     * <P/>
     * Return is typed as Object because this code lives in gateway-common and doesn't have access to the PEC or
     * even the ProcessingContext.
     *
     * @return the original PolicyEnforcementContext, if still available, or null.
     */
    public Object originalPolicyEnforcementContext() {
        return originalPolicyEnforcementContext;
    }

    /**
     * Set the associated original policy enforcement context.
     * <P/>
     * Param is typed as Object because this code lives in gateway-common and doesn't have access to the PEC or
     * even the ProcessingContext.
     *
     * @param originalPolicyEnforcementContext the original PEC to make available, or null to clear it.
     */
    public void originalPolicyEnforcementContext(Object originalPolicyEnforcementContext) {
        this.originalPolicyEnforcementContext = originalPolicyEnforcementContext;
    }

    /** Status of the request so far, or AssertionStatus.UNDEFINED if it's not yet known. */
    protected int status;

    /** String containing XML from request, or null if the current request has no XML */
    protected String requestXml;

    /** String containing XML from response, or null if the current response has no XML */
    protected String responseXml;

    /** GOID of the PublishedService that this request was resolved to, or the default if it has not yet been successfully resolved. */
    protected Goid serviceGoid = PublishedService.DEFAULT_GOID;

    /** <code>true</code> indicates that the request was successfully authenticated, or <code>false</code> otherwise. */
    protected boolean authenticated;

    /** If authenticated, this is the authentication type / method */
    protected SecurityTokenType authenticationType;

    /** Length of the request */
    protected int requestContentLength = -1;

    /** Length of the response */
    protected int responseContentLength = -1;

    /** HTTP status code of protected service response */
    private int responseHttpStatus = -1;

    /** Time (in milliseconds) spent routing the request to the protected service*/
    private int routingLatency = -1;

    /** Name of the operation the request was for if it's a SOAP service, or likely null otherwise */
    private String operationName;

    /** Used to lazily populate operationName if it is not yet set. */
    private Object operationNameHaver;

    private Goid mappingValuesId;

    /** Used to lazily populate mapping_values_id if it is not yet set. */
    private Functions.Nullary<Goid> mappingValueIdHaver;

    private MessageContextMappingValues mappingValuesEntity;

    /** Holds the original policy enforcement context for Message Summary Audit Records. */
    private transient Object originalPolicyEnforcementContext;

    private SecurityZone securityZone;

    @Override
    public void serializeOtherProperties(OutputStream out, boolean includeAllOthers, boolean useOldId) throws IOException {
        // status:request_id:service_oid:operation_name:authenticated:authenticationType:request_length:response_length:request_zipxml:
        // response_zipxml:response_status:routing_latency

        out.write(Integer.toString(status).getBytes());
        out.write(SERSEP.getBytes());

        if (requestId != null) out.write(requestId.getBytes());
        out.write(SERSEP.getBytes());

        out.write(useOldId?Long.toString(serviceGoid.getLow()).getBytes():Goid.toString(serviceGoid).getBytes());
        out.write(SERSEP.getBytes());

        if (getOperationName() != null) out.write(getOperationName().getBytes()); // this is lazy, must use getter
        out.write(SERSEP.getBytes());

        if (authenticated) out.write("1".getBytes()); // todo abstract this out
        else out.write("0".getBytes());
        out.write(SERSEP.getBytes());

        if (authenticationType != null) out.write(authenticationType.toString().getBytes());
        out.write(SERSEP.getBytes());

        out.write(Integer.toString(requestContentLength).getBytes());
        out.write(SERSEP.getBytes());

        out.write(Integer.toString(responseContentLength).getBytes());
        out.write(SERSEP.getBytes());

        if (includeAllOthers && requestXml != null) out.write(requestXml.getBytes());
        out.write(SERSEP.getBytes());

        if (includeAllOthers && responseXml != null) out.write(responseXml.getBytes());
        out.write(SERSEP.getBytes());

        out.write(Integer.toString(responseHttpStatus).getBytes());
        out.write(SERSEP.getBytes());

        out.write(Integer.toString(routingLatency).getBytes());
        out.write(SERSEP.getBytes());
    }
}
