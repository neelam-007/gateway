/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.common.audit;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.service.PublishedService;

import java.util.logging.Level;

/**
 * An {@link AuditRecord} that describes the processing of a single message.
 * <p>
 * By default, these are not saved unless the {@link com.l7tech.policy.assertion.AuditAssertion} is used or the
 * SSG's message processing audit threshold is set to {@link Level#INFO} or lower.
 *
 * @see {@link com.l7tech.server.ServerConfig#PARAM_AUDIT_MESSAGE_THRESHOLD}
 *
 * @author alex
 * @version $Revision$
 */
public class MessageSummaryAuditRecord extends AuditRecord {
    /** @deprecated to be called only for serialization and persistence purposes! */
    public MessageSummaryAuditRecord() {
    }

    /**
     *
     * @param level the java.util.logging.Level of this record.
     * @param nodeId the ID of the cluster node from which this AuditRecord originates (see com.l7tech.cluster.ClusterStatusAdmin.getClusterStatus())
     * @param clientAddr the IP address of the client that made the request
     * @param serviceName the name of the service that received the request that generated the AuditRecord
     * @param requestId the unique ID of the request
     * @param status the {@link AssertionStatus} resulting from applying the service policy to this request
     * @param requestXml the text of the request received from the client. Not saved by default.
     * @param requestContentLength the length of the request, in bytes.
     * @param responseXml the text of the response sent to the client. Not saved by default.
     * @param responseContentLength the length of the response, in bytes.
     * @param serviceOid the OID of the {@link PublishedService} this request was resolved to, or {@link PublishedService#DEFAULT_OID} if it could not be resolved.
     * @param serviceName the name of the {@link PublishedService} this request was resolved to, or null if it could not be resolved.
     * @param authenticated true if the request was authenticated, false otherwise
     * @param identityProviderOid the OID of the {@link IdentityProviderConfig IdentityProvider} against which the user authenticated, or {@link IdentityProviderConfig#DEFAULT_OID} if the request was not authenticated.
     * @param userName the name or login of the user who was authenticated, or null if the request was not authenticated.
     * @param userId the OID or DN of the user who was authenticated, or null if the request was not authenticated.
     */
    public MessageSummaryAuditRecord(Level level, String nodeId, String requestId, AssertionStatus status, String clientAddr,
                                     String requestXml, int requestContentLength,
                                     String responseXml, int responseContentLength,
                                     long serviceOid, String serviceName,
                                     boolean authenticated, long identityProviderOid, String userName, String userId)
    {
        super(level, nodeId, clientAddr, serviceName, null);
        StringBuffer msg = new StringBuffer("Message ");
        if (status == AssertionStatus.NONE) {
            msg.append("processed successfully");
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
        this.serviceOid = serviceOid;
        this.authenticated = authenticated;
        this.identityProviderOid = identityProviderOid;
        this.userName = userName;
        this.userId = userId;
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
     * Gets the OID of the {@link IdentityProviderConfig IdentityProvider} against which the user authenticated, or {@link IdentityProviderConfig#DEFAULT_OID} if the request was not authenticated.
     * @return the OID of the {@link IdentityProviderConfig IdentityProvider} against which the user authenticated, or {@link IdentityProviderConfig#DEFAULT_OID} if the request was not authenticated.
     */
    public long getIdentityProviderOid() {
        return identityProviderOid;
    }

    /**
     * Gets the name or login of the user who was authenticated, or null if the request was not authenticated.
     * @return the name or login of the user who was authenticated, or null if the request was not authenticated.
     */
    public String getUserName() {
        return userName;
    }

    /**
     * Gets the OID or DN of the user who was authenticated, or null if the request was not authenticated.
     * @return the OID or DN of the user who was authenticated, or null if the request was not authenticated.
     */
    public String getUserId() {
        return userId;
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

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setStatus( int status ) {
        this.status = status;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setRequestXml( String requestXml ) {
        this.requestXml = requestXml;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setResponseXml( String responseXml ) {
        this.responseXml = responseXml;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setServiceOid( long serviceOid ) {
        this.serviceOid = serviceOid;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setIdentityProviderOid( long identityProviderOid ) {
        this.identityProviderOid = identityProviderOid;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setUserName( String userName ) {
        this.userName = userName;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setUserId( String userId ) {
        this.userId = userId;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setAuthenticated( boolean authenticated ) {
        this.authenticated = authenticated;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setRequestContentLength( int requestContentLength ) {
        this.requestContentLength = requestContentLength;
    }

    /** @deprecated to be called only for serialization and persistence purposes! */
    public void setResponseContentLength( int responseContentLength ) {
        this.responseContentLength = responseContentLength;
    }

    /** Status of the request so far, or AssertionStatus.UNDEFINED if it's not yet known. */
    protected int status;

    /** String containing XML from request, or null if the current request has no XML */
    protected String requestXml;

    /** String containing XML from response, or null if the current response has no XML */
    protected String responseXml;

    /** OID of the PublishedService that this request was resolved to, or -1 if it has not yet been successfully resolved. */
    protected long serviceOid = PublishedService.DEFAULT_OID;

    /** OID of the IdentityProvider that the requesting user, if any, belongs to.  -1 indicates unknown. */
    protected long identityProviderOid = IdentityProviderConfig.DEFAULT_OID;

    /** Login or name of the user that is making the request if known, or null otherwise. */
    protected String userName;

    /** Unique ID of the user that is making the request (if known), or null otherwise. */
    protected String userId;

    /** <code>true</code> indicates that the request was successfully authenticated, or <code>false</code> otherwise. */
    protected boolean authenticated;

    /** Length of the request */
    protected int requestContentLength = -1;

    /** Length of the response */
    protected int responseContentLength = -1;
}
