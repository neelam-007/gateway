/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.logging;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.PrincipalCredentials;
import com.l7tech.message.Request;
import com.l7tech.message.XmlRequest;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.service.PublishedService;
import com.l7tech.logging.AuditRecord;
import com.l7tech.server.MessageProcessor;

import java.util.logging.Level;
import java.io.IOException;

/**
 * @author alex
 * @version $Revision$
 */
public class RequestAuditRecord extends AuditRecord {
    public RequestAuditRecord( Level level, String message ) {
        this( level, message, AssertionStatus.UNDEFINED );
    }

    public RequestAuditRecord( Level level, String message, AssertionStatus status ) {
        super( level, message );
        _status = status;

        Request currentRequest = MessageProcessor.getCurrentRequest();
        if ( currentRequest != null ) {
            if ( currentRequest instanceof XmlRequest ) {
                XmlRequest xreq = (XmlRequest)currentRequest;
                try {
                    _requestXml = xreq.getRequestXml();
                } catch (IOException e) {
                    _requestXml = null;
                }
            }

            PrincipalCredentials pc = currentRequest.getPrincipalCredentials();
            if ( pc != null ) {
                User u = pc.getUser();
                if ( u != null ) _userLogin = u.getLogin();
            }

            _remoteAddr = (String)currentRequest.getParameter( Request.PARAM_REMOTE_ADDR );

            PublishedService service = (PublishedService)currentRequest.getParameter( Request.PARAM_SERVICE );
            if ( service != null ) {
                _serviceOid = service.getOid();
                _serviceName = service.getName();
            }
        }

    }
    /** Status of the request so far, or AssertionStatus.UNDEFINED if it's not yet known. */
    protected AssertionStatus _status;

    /** String containing XML from request, or null if the current request has no XML */
    protected String _requestXml;

    /** OID of the PublishedService that this request was resolved to, or -1 if it has not yet been successfully resolved. */
    protected long _serviceOid = PublishedService.DEFAULT_OID;

    /** Name of the PublishedService that this request was resolved to, or null if it has not yet been successfully resolved. */
    protected String _serviceName;

    /** OID of the IdentityProvider that the requesting user, if any, belongs to.  -1 indicates unknown. */
    protected long _providerOid = IdentityProviderConfig.DEFAULT_OID;

    /** Login of the user that is making the request if known, or null otherwise. */
    protected String _userLogin;

    /** <code>true</code> indicates that the request was successfully authenticated, or <code>false</code> otherwise. */
    protected boolean _authenticated;

    /** IP address or hostname of the client responsible for this request if known, or null otherwise. */
    protected String _remoteAddr;

    /** Length of the request */
    protected int _requestContentLength;

    /** Length of the response */
    protected int _responseContentLength;
}
