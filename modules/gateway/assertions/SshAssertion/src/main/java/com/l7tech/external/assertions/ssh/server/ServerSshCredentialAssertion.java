package com.l7tech.external.assertions.ssh.server;

import com.l7tech.external.assertions.ssh.SshCredentialAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.message.SshKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.SshSecurityToken;
import com.l7tech.security.token.UsernamePasswordSecurityToken;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.Map;

/**
 * Server side implementation of the SshCredentialAssertion.
 *
 * @see com.l7tech.external.assertions.ssh.SshCredentialAssertion
 */
public class ServerSshCredentialAssertion extends ServerCredentialSourceAssertion<SshCredentialAssertion> {

    //- PUBLIC

    public ServerSshCredentialAssertion(SshCredentialAssertion data) {
        super(data);
    }

    //- PROTECTED

    @Override
    protected void challenge(PolicyEnforcementContext context, Map authParams) {
    }

    @Override
    protected LoginCredentials findCredentials(Message request, Map authParams) throws IOException, CredentialFinderException {
        LoginCredentials loginCredentials = null;
        SshKnob sshRequestKnob = request.getKnob(SshKnob.class);
        if (sshRequestKnob == null) {
            logAndAudit(AssertionMessages.SSH_CREDENTIAL_NOT_SSH);
        } else {
            loginCredentials = findCredentials(sshRequestKnob.getPasswordAuthentication(), sshRequestKnob.getPublicKeyAuthentication());
        }
        return loginCredentials;
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

    private LoginCredentials findCredentials(PasswordAuthentication passwordAuthentication,
                                             SshKnob.PublicKeyAuthentication publicKeyAuthentication) throws IOException {
        if (assertion.isPermitPublicKeyCredential() && publicKeyAuthentication != null) {
            logAndAudit(AssertionMessages.SSH_CREDENTIAL_AUTH_USER, publicKeyAuthentication.getUserName());
            return LoginCredentials.makeLoginCredentials( new SshSecurityToken(
                SecurityTokenType.SSH_CREDENTIAL, publicKeyAuthentication),
                assertion.getClass());
        }

        if (assertion.isPermitPasswordCredential() && passwordAuthentication != null) {
            logAndAudit(AssertionMessages.SSH_CREDENTIAL_AUTH_USER, passwordAuthentication.getUserName());
             return LoginCredentials.makeLoginCredentials( new UsernamePasswordSecurityToken(
                SecurityTokenType.SSH_CREDENTIAL, passwordAuthentication.getUserName(), passwordAuthentication.getPassword()),
                assertion.getClass());
        }

        // no password nor public key
        logAndAudit(AssertionMessages.SSH_CREDENTIAL_NO_AUTH);
        return null;
    }
}
