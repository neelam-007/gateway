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
 * By default, one of these will be created and logged with every message that gets processed.
 *
 * @author alex
 * @version $Revision$
 */
public class MessageSummaryAuditRecord extends AuditRecord {
    /** @deprecated to be called only for serialization and persistence purposes! */
    public MessageSummaryAuditRecord() {
    }

    public MessageSummaryAuditRecord(Level level, String nodeId, String requestId, AssertionStatus status, String clientAddr,
                                     String requestXml, int requestContentLength,
                                     String responseXml, int responseContentLength,
                                     long serviceOid, String serviceName,
                                     boolean authenticated, long identityProviderOid, String userName, String userId)
    {
        super(level, nodeId, clientAddr, null, null);
        StringBuffer msg = new StringBuffer("Message ");
        if (status == AssertionStatus.NONE) {
            msg.append("processed successfully");
        } else {
            msg.append("was not processed: ");
            msg.append(status.getMessage());
            msg.append(" (").append(status.getNumeric()).append(")");
        }
        this.name = serviceName;
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

    public int getStatus() {
        return status;
    }

    public long getServiceOid() {
        return serviceOid;
    }

    public String getServiceName() {
        return serviceName;
    }

    public long getIdentityProviderOid() {
        return identityProviderOid;
    }

    public String getUserName() {
        return userName;
    }

    public String getUserId() {
        return userId;
    }

    public String getRequestXml() {
        return requestXml;
    }

    public String getResponseXml() {
        return responseXml;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public int getRequestContentLength() {
        return requestContentLength;
    }

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
    public void setServiceName( String serviceName ) {
        this.serviceName = serviceName;
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

    /** Name of the PublishedService that this request was resolved to, or null if it has not yet been successfully resolved. */
    protected String serviceName;

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
