/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.audit;

import com.l7tech.cluster.ClusterInfoManager;
import com.l7tech.common.audit.MessageSummaryAuditRecord;
import com.l7tech.identity.User;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.message.XmlRequest;
import com.l7tech.message.XmlResponse;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.MessageProcessor;
import com.l7tech.service.PublishedService;

import java.io.IOException;
import java.util.logging.Level;

/**
 * A MessageSummaryAuditRecord must be generated upon the conclusion of the processing of a message,
 * whether successful or otherwise.
 *
 * @author alex
 * @version $Revision$
 */
public class MessageSummaryAuditFactory {
    private MessageSummaryAuditFactory() {}
    private static String nodeId = ClusterInfoManager.getInstance().thisNodeId();

    public static MessageSummaryAuditRecord makeEvent(Level level, AssertionStatus status ) {
        String requestXml = null;
        int requestContentLength = -1;
        String responseXml = null;
        int responseContentLength = -1;
        long identityProviderOid = -1;
        String userName = null;
        String clientAddr = null;
        long serviceOid = -1;
        String serviceName = null;
        boolean authenticated = false;
        String userId = null;

        Request currentRequest = MessageProcessor.getCurrentRequest();
        String requestId = currentRequest.getId().toString();
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
            if (u == null) {
                LoginCredentials creds = currentRequest.getPrincipalCredentials();
                if (creds != null) userName = creds.getLogin();
            } else {
                identityProviderOid = u.getProviderId();
                userId = u.getUniqueIdentifier();
                userName = u.getName();
                if (userName == null) userName = u.getLogin();
            }
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

            if (responseXml != null) responseContentLength = responseXml.length();
        }

        return new MessageSummaryAuditRecord(level, nodeId, requestId, status, clientAddr,
                                             currentRequest.isAuditSaveRequest() ? requestXml : null,
                                             requestContentLength,
                                             currentRequest.isAuditSaveResponse() ? responseXml : null,
                                             responseContentLength,
                                             serviceOid, serviceName,
                                             authenticated, identityProviderOid, userName, userId);
    }

}
