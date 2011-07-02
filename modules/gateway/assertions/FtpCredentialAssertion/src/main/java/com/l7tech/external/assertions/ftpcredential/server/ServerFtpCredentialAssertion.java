package com.l7tech.external.assertions.ftpcredential.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.FtpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernamePasswordSecurityToken;
import com.l7tech.external.assertions.ftpcredential.FtpCredentialAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.Map;

/**
 * Server side implementation of the FtpCredentialAssertion.
 *
 * @see com.l7tech.external.assertions.ftpcredential.FtpCredentialAssertion
 * @author Steve Jones
 */
public class ServerFtpCredentialAssertion extends ServerCredentialSourceAssertion<FtpCredentialAssertion> {

    //- PUBLIC

    public ServerFtpCredentialAssertion(FtpCredentialAssertion data) {
        super(data);
    }

    //- PROTECTED

    @Override
    protected void challenge(PolicyEnforcementContext context, Map authParams) {
    }

    @Override
    protected LoginCredentials findCredentials(Message request, Map authParams) throws IOException, CredentialFinderException {
        FtpRequestKnob ftpRequestKnob = request.getKnob(FtpRequestKnob.class);
        if (ftpRequestKnob == null) {
            logAndAudit(AssertionMessages.FTP_CREDENTIAL_NOT_FTP);
            throw new CredentialFinderException("Request is not FTP.", AssertionStatus.NOT_APPLICABLE);
        }
        return findCredentials( ftpRequestKnob.getCredentials() );
    }

    @Override
    protected AssertionStatus checkCredentials(LoginCredentials pc, Map authParams) throws CredentialFinderException {
        AssertionStatus status = AssertionStatus.AUTH_REQUIRED;

        if (pc != null) {
            status = AssertionStatus.NONE;
        }

        return status;
    }

    //- PRIVATE

    private LoginCredentials findCredentials( PasswordAuthentication passwordAuthentication ) throws IOException {
        if ( passwordAuthentication == null ) {
            logAndAudit(AssertionMessages.FTP_CREDENTIAL_NO_AUTH);
            return null;
        }

        logAndAudit(AssertionMessages.FTP_CREDENTIAL_AUTH_USER, passwordAuthentication.getUserName());

        return LoginCredentials.makeLoginCredentials( new UsernamePasswordSecurityToken(
                SecurityTokenType.FTP_CREDENTIAL, 
                passwordAuthentication.getUserName(),
                passwordAuthentication.getPassword()),
                assertion.getClass());
    }
}
