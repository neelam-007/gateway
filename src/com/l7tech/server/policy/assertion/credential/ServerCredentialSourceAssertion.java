/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server.policy.assertion.credential;

import com.l7tech.common.audit.AssertionMessages;
import com.l7tech.common.audit.Auditor;
import com.l7tech.common.message.Message;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionResult;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author alex
 * @version $Revision$
 */
public abstract class ServerCredentialSourceAssertion implements ServerAssertion {
    private final Auditor auditor;

    protected ServerCredentialSourceAssertion(Assertion data, ApplicationContext springContext) {
        if (data == null) {
            throw new IllegalArgumentException();
        }
        if (!data.isCredentialSource())  {
            throw new IllegalArgumentException("Not a credential source " + data);
        }
        _data = data;
        this.auditor = new Auditor(this, springContext, logger);
    }

    /**
     * Server-side processing for all <code>CredentialSourceAssertion</code>s.
     *
     * @param context
     * @return
     * @throws IOException
     * @throws PolicyAssertionException
     */
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException
    {
        final HashMap authParams = new HashMap();
        try {
            LoginCredentials pc = context.getCredentials();
            if ( pc == null ) {
                // No finder has been run yet!
                pc = findCredentials( context.getRequest(), authParams );
            }

            if ( pc == null ) {
                context.setAuthenticationMissing();
                challenge( context, authParams );
                auditor.logAndAudit(AssertionMessages.AUTH_REQUIRED);
                return AssertionStatus.AUTH_REQUIRED;
            } else {
                context.setCredentials( pc );
                return checkCredentials(pc, authParams);
            }
        } catch (CredentialFinderException cfe) {
            AssertionStatus status = cfe.getStatus();
            if (status == null) {
                auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO, null, cfe);
                throw new PolicyAssertionException(cfe.getMessage(), cfe);
            } else {
                context.addResult( new AssertionResult( _data, status, cfe.getMessage() ) );
                challenge( context, authParams );
                // Suppress exception trace by omitting exception argument
                auditor.logAndAudit(AssertionMessages.EXCEPTION_INFO, null, cfe);

                if ( status == AssertionStatus.AUTH_REQUIRED )
                    context.setAuthenticationMissing();
                else
                    context.setRequestPolicyViolated();
                return status;
            }
        }
    }

    /**
     * Extract credentials from the request, if possible, or accumulate info for a challenge into authParams.
     *
     * @param request the request to search for credentials.  Must not be null.
     * @param authParams  a Map in which to accumulate challenge information.  Must not be null.
     *                    Keys may be added to this map that will be needed by a subsequent call to {@link #challenge}
     * @return the LoginCredentials found in this request, or null if none were found.
     * @throws IOException if there is a problem reading enough of the request to gather the needed information
     * @throws CredentialFinderException if there is a serious problem with the request and no further assertions should be attempted
     */
    protected abstract LoginCredentials findCredentials( Message request, Map authParams ) throws IOException, CredentialFinderException;

    /**
     * Check if extracted credentials are internally consistent, but without matching against any particular identity.
     * This might check for things like missing authentication parameters, expired sessions or nonces, etc.
     *
     * @param pc the credentials that were extracted.  Must not be null.
     * @param authParams  a Map in which to accumulate challenge information.  Must not be null.
     *                    Keys may be added to this map that will be needed by a subsequent call to {@link #challenge}
     * @return the LoginCredentials found in this request, or null if none were found.
     * @throws CredentialFinderException if the credentials are not internally consistent
     */
    protected abstract AssertionStatus checkCredentials( LoginCredentials pc, Map authParams ) throws CredentialFinderException;

    /**
     * Are you looking for a <b><i>CHALLENGE</i></b>?  If so, then call this method.
     * Configures the response to send a challenge from data collected in the specified authParams.
     *
     * @param context  context containing the request being challenged and
     *                 the response that will be used to send back the challenge.  May not be null.
     * @param authParams  the authParams containing the challenge data collected by findCredentials().  May not be null.
     */
    protected abstract void challenge( PolicyEnforcementContext context, Map authParams );

    protected Assertion _data;
    final Logger logger = Logger.getLogger(getClass().getName());
}
