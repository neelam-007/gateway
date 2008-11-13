/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.message.HttpResponseKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpCredentialSourceAssertion;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public abstract class ServerHttpCredentialSource<AT extends HttpCredentialSourceAssertion> extends ServerCredentialSourceAssertion<AT> implements ServerAssertion {
    private static final Logger logger = Logger.getLogger(ServerHttpCredentialSource.class.getName());

    private final Auditor auditor;

    protected ServerHttpCredentialSource(AT data, ApplicationContext springContext) {
        super(data, springContext);
        this.auditor = new Auditor(this, springContext, logger);
    }

    public AssertionStatus checkCredentials(LoginCredentials pc, Map<String, String> authParams) throws CredentialFinderException {
        if ( pc == null ) return AssertionStatus.AUTH_REQUIRED;
        String requestRealm = pc.getRealm();
        String assertRealm = realm();

        if ( requestRealm == null || requestRealm.length() == 0 ) requestRealm = assertRealm;

        if ( requestRealm.equals( assertRealm ) ) {
            return checkAuthParams(authParams);
        } else {
            throw new CredentialFinderException( "Realm mismatch: Expected '" + assertRealm + "', got '"+ requestRealm, AssertionStatus.AUTH_FAILED );
        }
    }

    protected void challenge(PolicyEnforcementContext context, Map<String, String> authParams) {
        String scheme = scheme();
        StringBuffer challengeHeader = new StringBuffer( scheme );
        challengeHeader.append( " " );
        String realm = realm();
        if ( realm != null && realm.length() > 0 ) {
            challengeHeader.append( HttpCredentialSourceAssertion.PARAM_REALM );
            challengeHeader.append( "=" );
            challengeHeader.append( quoted( realm ) );
        }

        Map<String, String> challengeParams = challengeParams(context.getRequest(), authParams);
        String name, value;
        Iterator<String> i = challengeParams.keySet().iterator();
        if ( i.hasNext() ) challengeHeader.append( ", " );

        while ( i.hasNext() ) {
            name = i.next();
            value = challengeParams.get(name);
            if ( name != null && value != null ) {
                challengeHeader.append( name );
                challengeHeader.append( "=" );
                challengeHeader.append( quoted( value ) );
                if ( i.hasNext() ) challengeHeader.append( ", " );
            }
        }

        String challenge = challengeHeader.toString();

        auditor.logAndAudit(AssertionMessages.HTTPCREDS_CHALLENGING, challenge);
        HttpResponseKnob httpResponse = context.getResponse().getHttpResponseKnob();
        httpResponse.addChallenge(challenge);
    }

    /**
     * Returns the authentication realm to use for this assertion.
     *
     * @return the authentication realm to use for this assertion.  Never null.
     */
    protected abstract String realm();

    private String quoted( String value ) {
        if ( value == null )
            return "\"\"";
        else
            return '"' + value + '"';
    }

    protected abstract AssertionStatus checkAuthParams(Map<String, String> authParams);
    protected abstract Map<String, String> challengeParams(Message request, Map<String, String> authParams);
    protected abstract String scheme();

    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        if (context.getRequest().getKnob(HttpRequestKnob.class) == null) {
            auditor.logAndAudit(AssertionMessages.HTTP_CS_CANNOT_EXTRACT_CREDENTIALS);
            return AssertionStatus.NOT_APPLICABLE;
        }
        return super.checkRequest(context);
    }
}
