package com.l7tech.external.assertions.ftpcredential.server;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.FtpRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.external.assertions.ftpcredential.FtpCredentialAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.CredentialFinderException;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.assertion.credential.ServerCredentialSourceAssertion;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.net.PasswordAuthentication;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server side implementation of the FtpCredentialAssertion.
 *
 * @see com.l7tech.external.assertions.ftpcredential.FtpCredentialAssertion
 * @author Steve Jones
 */
public class ServerFtpCredentialAssertion extends ServerCredentialSourceAssertion<FtpCredentialAssertion> {

    //- PUBLIC

    public ServerFtpCredentialAssertion(FtpCredentialAssertion data, ApplicationContext springContext) {
        super(data, springContext);

        this.auditor = new Auditor(this, springContext, logger);
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static void onModuleUnloaded() {
        // This assertion doesn't have anything to do in response to this, but it implements this anyway
        // since it will be used as an example by future modular assertion authors
        logger.log(Level.INFO, "ServerFtpCredentialAssertion is preparing itself to be unloaded");
    }

    //- PROTECTED

    @Override
    protected void challenge(PolicyEnforcementContext context, Map authParams) {
    }

    @Override
    protected LoginCredentials findCredentials(Message request, Map authParams) throws IOException, CredentialFinderException {
        FtpRequestKnob ftpRequestKnob = request.getKnob(FtpRequestKnob.class);
        if (ftpRequestKnob == null) {
            auditor.logAndAudit(AssertionMessages.FTP_CREDENTIAL_NOT_FTP);
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

    private static final Logger logger = Logger.getLogger(ServerFtpCredentialAssertion.class.getName());

    private final Auditor auditor;

    private LoginCredentials findCredentials( PasswordAuthentication passwordAuthentication ) throws IOException {
        if ( passwordAuthentication == null ) {
            auditor.logAndAudit(AssertionMessages.FTP_CREDENTIAL_NO_AUTH);
            return null;
        }

        auditor.logAndAudit(AssertionMessages.FTP_CREDENTIAL_AUTH_USER, passwordAuthentication.getUserName());

        return new LoginCredentials(
                passwordAuthentication.getUserName(),
                passwordAuthentication.getPassword(),
                CredentialFormat.CLEARTEXT,
                assertion.getClass(),
                null,
                null,
                SecurityTokenType.FTP_CREDENTIAL);
    }
}
