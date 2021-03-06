package com.l7tech.server.policy.assertion.credential.http;

import com.l7tech.common.http.HttpConstants;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.security.token.http.HttpBasicToken;
import com.l7tech.util.Charsets;
import com.l7tech.util.HexUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;

public class ServerHttpBasic extends ServerHttpCredentialSource<HttpBasic> {
    public static final Charset ENCODING = Charsets.ISO8859; // http client will not encode this with utf-8, see bugzilla #2733
    public static final String SCHEME = "Basic";
    public static final String REALM = "L7SSGBasicRealm";

    public ServerHttpBasic(HttpBasic data) {
        super(data);
    }

    @Override
    protected Map<String, String> challengeParams(Message request, Map<String, String> authParams) {
        return Collections.emptyMap();
    }

    @Override
    protected String scheme() {
        return SCHEME;
    }

    @Override
    public LoginCredentials findCredentials(Message request, Map<String, String> authParams) throws IOException, CredentialFinderException {
        String authnHeader = request.getHttpRequestKnob().getHeaderSingleValue(HttpConstants.HEADER_AUTHORIZATION);
        return findCredentials(authnHeader);
    }

    public LoginCredentials findCredentials( String authnHeader) throws IOException {
        if (authnHeader == null || authnHeader.length() == 0) {
            logAndAudit(AssertionMessages.HTTPCREDS_NO_AUTHN_HEADER);
            return null;
        }

        int spos = authnHeader.indexOf(" ");
        if ( spos < 0 ) {
            logAndAudit(AssertionMessages.HTTPCREDS_BAD_AUTHN_HEADER, "no space");
            return null;
        }

        final String scheme = authnHeader.substring( 0, spos );
        final String base64 = authnHeader.substring( spos + 1 );
        if ( !scheme().equals(scheme) ) {
            logAndAudit(AssertionMessages.HTTPCREDS_NA_AUTHN_HEADER);
            return null;
        }

        final String userPassRealm = new String(HexUtils.decodeBase64( base64, true ), ENCODING);
        final int cpos = userPassRealm.indexOf(":");
        if ( cpos >= 0 ) {
            final String login = userPassRealm.substring(0, cpos);
            final String pass = userPassRealm.substring(cpos + 1);

            logAndAudit(AssertionMessages.HTTPCREDS_FOUND_USER, login);

            return LoginCredentials.makeLoginCredentials(new HttpBasicToken(login, pass.toCharArray()), assertion.getClass());
        } else {
            // No colons
            logAndAudit(AssertionMessages.HTTPCREDS_BAD_AUTHN_HEADER, "no colon");
            return null;
        }
    }

    @Override
    protected String realm() {
        String realm = assertion.getRealm();
        if ( realm == null ) realm = REALM;
        return realm;
    }

    @Override
    protected AssertionStatus checkAuthParams(Map<String, String> authParams) {
        return AssertionStatus.NONE;
    }
}
