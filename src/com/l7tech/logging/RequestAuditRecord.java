/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.logging;

import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.User;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.XmlRequest;
import com.l7tech.message.XmlResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MessageProcessor;
import com.l7tech.service.PublishedService;

import java.io.IOException;
import java.util.logging.Level;

/**
 * A RequestAuditRecord must be generated upon the conclusion of the processing of a message,
 * whether successful or otherwise.
 *
 * @author alex
 * @version $Revision$
 */
public class RequestAuditRecord extends AuditRecord {
    public RequestAuditRecord( String message ) {
        this( message, AssertionStatus.UNDEFINED );
    }

    public RequestAuditRecord( String text, AssertionStatus status ) {
        super( Level.INFO, text );
        this.status = status;

        Request currentRequest = MessageProcessor.getCurrentRequest();
        if ( currentRequest != null ) {
            if ( currentRequest instanceof XmlRequest ) {
                XmlRequest xreq = (XmlRequest)currentRequest;
                try {
                    requestXml = xreq.getRequestXml();
                } catch (IOException e) {
                    requestXml = null;
                }
            }

            try {
                requestContentLength = new Integer( (String)currentRequest.getParameter( Request.PARAM_HTTP_CONTENT_LENGTH ) ).intValue();
            } catch ( NumberFormatException nfe ) {
            }
            if ( requestContentLength == -1 && requestXml != null ) requestContentLength = requestXml.length();

            User u = currentRequest.getUser();
            if ( u != null ) {
                identityProviderOid = u.getProviderId();
                userName = u.getLogin();
                if (userName == null) userName = u.getName();
                if (userName == null) userName = u.getUniqueIdentifier();
            }

            clientAddr = (String)currentRequest.getParameter(Request.PARAM_REMOTE_ADDR);

            PublishedService service = (PublishedService)currentRequest.getParameter( Request.PARAM_SERVICE );
            if ( service != null ) {
                serviceOid = service.getOid();
                serviceName = service.getName();
            }
        }
        authenticated = currentRequest.isAuthenticated();
        if ( authenticated ) {
            User u = currentRequest.getUser();
            identityProviderOid = u.getProviderId();
            userId = u.getUniqueIdentifier();
            userName = u.getName();
            if (userName == null) userName = u.getLogin();
        }

        PublishedService service = (PublishedService)currentRequest.getParameter(Request.PARAM_SERVICE);
        if (service != null) {
            serviceOid = service.getOid();
            serviceName = service.getName();
        }

        Response currentResponse = MessageProcessor.getCurrentResponse();
        if ( currentResponse != null ) {
            if ( currentResponse instanceof XmlResponse ) {
                XmlResponse xresp = (XmlResponse)currentResponse;
                try {
                    responseXml = xresp.getResponseXml();
                } catch (IOException e) {
                    responseXml = null;
                }
            }

            try {
                responseContentLength = new Integer( (String)currentResponse.getParameter( Response.PARAM_HTTP_CONTENT_LENGTH ) ).intValue();
            } catch ( NumberFormatException nfe ) {
            }
            if ( responseContentLength == -1 && responseXml != null ) responseContentLength = responseXml.length();
        }
    }

    public AssertionStatus getStatus() {
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

    public String getClientAddr() {
        return clientAddr;
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

    /** Status of the request so far, or AssertionStatus.UNDEFINED if it's not yet known. */
    private AssertionStatus status;

    /** String containing XML from request, or null if the current request has no XML */
    private String requestXml;

    /** String containing XML from response, or null if the current response has no XML */
    private String responseXml;

    /** OID of the PublishedService that this request was resolved to, or -1 if it has not yet been successfully resolved. */
    private long serviceOid = PublishedService.DEFAULT_OID;

    /** Name of the PublishedService that this request was resolved to, or null if it has not yet been successfully resolved. */
    private String serviceName;

    /** OID of the IdentityProvider that the requesting user, if any, belongs to.  -1 indicates unknown. */
    private long identityProviderOid = IdentityProviderConfig.DEFAULT_OID;

    /** Login or name of the user that is making the request if known, or null otherwise. */
    private String userName;

    /** Unique ID of the user that is making the request (if known), or null otherwise. */
    private String userId;

    /** <code>true</code> indicates that the request was successfully authenticated, or <code>false</code> otherwise. */
    private boolean authenticated;

    /** IP address or hostname of the client responsible for this request if known, or null otherwise. */
    private String clientAddr;

    /** Length of the request */
    private int requestContentLength = -1;

    /** Length of the response */
    private int responseContentLength = -1;
}
