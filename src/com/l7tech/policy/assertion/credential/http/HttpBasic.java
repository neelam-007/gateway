/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.credential.*;
import com.l7tech.credential.http.HttpBasicCredentialFinder;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;
import org.apache.log4j.Category;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpBasic extends HttpCredentialSourceAssertion {
    private static final Category log = Category.getInstance(HttpBasic.class);

    public AssertionStatus doCheckRequest( Request request, Response response ) throws CredentialFinderException {
        PrincipalCredentials pc = request.getPrincipalCredentials();
        if ( pc == null ) return AssertionStatus.FALSIFIED;
        String realm = pc.getRealm();
        if ( ( realm == null && _realm == null ) || realm != null && realm.equals( _realm ) ) {
            return AssertionStatus.NONE;
        }
        return AssertionStatus.FALSIFIED;
    }

    public Class getCredentialFinderClass() {
        return HttpBasicCredentialFinder.class;
    }

    /**
     * Set up HTTP Basic auth on the PendingRequest.
     * @param request    The request to decorate.
     * @return AssertionStatus.NONE if this Assertion was applied to the request successfully; otherwise, some error code
     * @throws PolicyAssertionException if processing should not continue due to a serious error
     */
    public AssertionStatus decorateRequest(PendingRequest request) throws PolicyAssertionException {
        String username = request.getSsg().getUsername();
        char[] password = request.getSsg().getPassword();
        if (username == null || password == null || username.length() < 1) {
            log.info("HttpBasic: no credentials configured for the SSG " + request.getSsg());
            request.setCredentialsWouldHaveHelped(true);
            return AssertionStatus.NOT_FOUND;
        }
        request.setBasicAuthRequired(true);
        request.setHttpBasicUsername(username);
        request.setHttpBasicPassword(password);
        log.info("HttpBasic: setting credentials for SSG " + request.getSsg());
        return AssertionStatus.NONE;
    }
}
