/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.service.PublishedService;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.security.token.SecurityTokenType;

import java.util.logging.Level;
import java.util.Set;
import java.util.Iterator;
import java.io.OutputStream;
import java.io.IOException;

/**
 * An {@link AuditRecord} that describes the processing of a single message.
 * <p>
 * By default, these are not saved unless the {@link com.l7tech.policy.assertion.AuditAssertion} is used or the
 * SSG's message processing audit threshold is set to {@link Level#INFO} or lower.
 *
 * @see com.l7tech.server.ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD
 *
 * @author alex
 * @version $Revision$
 */
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
                                     boolean authenticated, SecurityTokenType authenticationType, long identityProviderOid, String userName, String userId)
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
    }

    /**
     * Gets the {@link AssertionStatus} resulting from applying the service policy to this request
     * @return the {@link AssertionStatus} resulting from applying the service policy to this request
     */
    public int getStatus() {
        return status;
    }

    /**
     * Gets the OID of the {@link PublishedService} this request was resolved to, or {@link PublishedService#DEFAULT_OID} if it could not be resolved.
     * @return the OID of the {@link PublishedService} this request was resolved to, or {@link PublishedService#DEFAULT_OID} if it could not be resolved.
     */
    public long getServiceOid() {
        return serviceOid;
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
    public String getOperationName() {
        if (operationName == null) {
            if (operationNameHaver != null)
                operationName = operationNameHaver.toString();
        }
        return operationName;
    }

    public void setDetails(Set<AuditDetail> details) {
        if (details != null) {
            for (Iterator<AuditDetail> iterator = details.iterator(); iterator.hasNext();) {
                Object detailObj = iterator.next();
                if (detailObj instanceof MessageSummaryAuditDetail) {
                    MessageSummaryAuditDetail msad = (MessageSummaryAuditDetail) detailObj;
                    if (!msad.shouldSave()) iterator.remove(); // we don't want to save this.
                }
            }
        }

        super.setDetails(details);
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

    public void serializeSignableProperties(OutputStream out) throws IOException {
        super.serializeSignableProperties(out);
        if (operationName != null) out.write(operationName.getBytes());

        if (requestXml != null) out.write(requestXml.getBytes());
        if (responseXml != null) out.write(responseXml.getBytes());
    }
}
