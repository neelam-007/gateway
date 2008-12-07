/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.gateway.common.audit;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.mapping.MessageContextMappingValues;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.mapping.MessageContextMappingKeys;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.security.token.SecurityTokenType;

import javax.persistence.*;
import java.util.logging.Level;
import java.util.List;
import java.io.OutputStream;
import java.io.IOException;

import org.hibernate.annotations.Type;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

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
@Entity
@Table(name="audit_message")
@OnDelete(action= OnDeleteAction.CASCADE)
public class MessageSummaryAuditRecord extends AuditRecord {
    public static final String ATTR_SERVICE_OID = "serviceOid";

    /** @deprecated to be called only for serialization and persistence purposes! */
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
     * @param serviceOid the OID of the {@link PublishedService} this request was resolved to, or {@link PublishedService#DEFAULT_OID} if it could not be resolved.
     * @param serviceName the name of the {@link PublishedService} this request was resolved to, or null if it could not be resolved.
     * @param operationNameHaver an Object on which toString() will be called if the operation name is needed, or null if one will not be provided.
     * @param authenticated true if the request was authenticated, false otherwise
     * @param authenticationType the authentication type for the request (may be null)
     * @param identityProviderOid the OID of the {@link IdentityProviderConfig IdentityProvider} against which the user authenticated, or {@link IdentityProviderConfig#DEFAULT_OID} if the request was not authenticated.
     * @param userName the name or login of the user who was authenticated, or null if the request was not authenticated.
     * @param userId the OID or DN of the user who was authenticated, or null if the request was not authenticated.
     */
    public MessageSummaryAuditRecord(Level level, String nodeId, String requestId, AssertionStatus status,
                                     String clientAddr, String requestXml, int requestContentLength,
                                     String responseXml, int responseContentLength, int httpRespStatus, int routingLatency,
                                     long serviceOid, String serviceName, Object operationNameHaver,
                                     boolean authenticated, SecurityTokenType authenticationType, long identityProviderOid,
                                     String userName, String userId, Number mappingValueOidHaver)
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
        this.serviceOid = serviceOid;
        this.authenticated = authenticated;
        this.authenticationType = authenticationType;
        this.mappingValueOidHaver = mappingValueOidHaver;
    }

    /**
     * Gets the {@link AssertionStatus} resulting from applying the service policy to this request
     * @return the {@link AssertionStatus} resulting from applying the service policy to this request
     */
    @Column(name="status", nullable=false)
    public int getStatus() {
        return status;
    }

    /**
     * Gets the OID of the {@link PublishedService} this request was resolved to, or {@link PublishedService#DEFAULT_OID} if it could not be resolved.
     * @return the OID of the {@link PublishedService} this request was resolved to, or {@link PublishedService#DEFAULT_OID} if it could not be resolved.
     */
    @Column(name="service_oid")
    public long getServiceOid() {
        return serviceOid;
    }

    @Override
    @Column(name="request_id", nullable=false, length=40)
    public String getStrRequestId() {
        return super.getStrRequestId();
    }

    @Override
    public void setStrRequestId(String requestId) {
        super.setStrRequestId(requestId);
    }

    /**
     * Gets the text of the request received from the client.
     * @return the text of the request received from the client.
     */
    @Column(name="request_zipxml", length=Integer.MAX_VALUE)
    @Type(type="com.l7tech.server.util.CompressedStringType")
    @Basic(fetch=FetchType.LAZY)
    public String getRequestXml() {
        return requestXml;
    }

    /**
     * Gets the text of the response sent to the client.
     * @return the text of the response sent to the client.
     */
    @Column(name="response_zipxml", length=Integer.MAX_VALUE)
    @Type(type="com.l7tech.server.util.CompressedStringType")
    @Basic(fetch=FetchType.LAZY)
    public String getResponseXml() {
        return responseXml;
    }

    /**
     * Returns true if the request was authenticated, false otherwise
     * @return true if the request was authenticated, false otherwise
     */
    @Column(name="authenticated")
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Gets the authentication type for this request (if authenticated)
     * @return the SecurityTokenType or null
     */
    @Column(name="authenticationType")
    @Type(type="com.l7tech.server.util.SecurityTokenUserType")
    public SecurityTokenType getAuthenticationType() {
        return authenticationType;
    }

    /**
     * Gets the length of the request received from the client, in bytes.
     * @return the length of the request, in bytes.
     */
    @Column(name="request_length", nullable=false)
    public int getRequestContentLength() {
        return requestContentLength;
    }

    /**
     * Gets the length of the response sent to the client, in bytes.
     * @return the length of the response, in bytes.
     */
    @Column(name="response_length")
    public int getResponseContentLength() {
        return responseContentLength;
    }

    /**
     * @return the HTTP status code of the back-end response
     */
    @Column(name="response_status")
    public int getResponseHttpStatus() {
        return responseHttpStatus;
    }

    /**
     * @return the time (in milliseconds) spent routing the request to the protected service
     */
    @Column(name="routing_latency")
    public int getRoutingLatency() {
        return routingLatency;
    }

    /** @return the name of the operation the request was for if it's a SOAP service, or likely null otherwise */
    @Column(name="operation_name", length=255)
    public String getOperationName() {
        if (operationName == null) {
            if (operationNameHaver != null)
                operationName = operationNameHaver.toString();
        }
        return operationName;
    }

    @Column(name="mapping_values_oid")
    public Long getMappingValuesOid() {
        if ( mappingValuesOid == null ) {
            if (mappingValueOidHaver != null) {
                mappingValuesOid = mappingValueOidHaver.longValue();
                if ( mappingValuesOid <= 0 ) mappingValuesOid = null;
            }

        }
        return mappingValuesOid;
    }

    public void setMappingValuesOid(Long mappingValuesOid) {
        this.mappingValueOidHaver = null;
        this.mappingValuesOid = mappingValuesOid;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    protected void setOperationName(String operationName) {
        this.operationNameHaver = null;
        this.operationName = operationName;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    protected void setStatus( int status ) {
        this.status = status;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    protected void setRequestXml( String requestXml ) {
        this.requestXml = requestXml;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    protected void setResponseXml( String responseXml ) {
        this.responseXml = responseXml;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    protected void setServiceOid( long serviceOid ) {
        this.serviceOid = serviceOid;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    protected void setAuthenticated( boolean authenticated ) {
        this.authenticated = authenticated;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setAuthenticationType(SecurityTokenType authenticationType) {
        this.authenticationType = authenticationType;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    protected void setRequestContentLength( int requestContentLength ) {
        this.requestContentLength = requestContentLength;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    protected void setResponseContentLength( int responseContentLength ) {
        this.responseContentLength = responseContentLength;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    protected void setResponseHttpStatus(int responseHttpStatus) {
        this.responseHttpStatus = responseHttpStatus;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    protected void setRoutingLatency(int routingLatency) {
        this.routingLatency = routingLatency;
    }

    @ManyToOne
    @JoinColumn(name="mapping_values_oid", insertable=false, updatable=false)
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

    /** Status of the request so far, or AssertionStatus.UNDEFINED if it's not yet known. */
    protected int status;

    /** String containing XML from request, or null if the current request has no XML */
    protected String requestXml;

    /** String containing XML from response, or null if the current response has no XML */
    protected String responseXml;

    /** OID of the PublishedService that this request was resolved to, or -1 if it has not yet been successfully resolved. */
    protected long serviceOid = PublishedService.DEFAULT_OID;

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

    private Long mappingValuesOid;

    /** Used to lazily populate mapping_values_oid if it is not yet set. */
    private Number mappingValueOidHaver;

    private MessageContextMappingValues mappingValuesEntity;

    public void serializeOtherProperties(OutputStream out, boolean includeAllOthers) throws IOException {
        // status:request_id:service_oid:operation_name:authenticated:authenticationType:request_length:response_length:request_zipxml:
        // response_zipxml:response_status:routing_latency

        out.write(Integer.toString(status).getBytes());
        out.write(SERSEP.getBytes());

        if (requestId != null) out.write(requestId.getBytes());
        out.write(SERSEP.getBytes());

        out.write(Long.toString(serviceOid).getBytes());
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
