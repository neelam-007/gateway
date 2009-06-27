package com.l7tech.server.policy.assertion.xmlsec;

import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.util.logging.Logger;
import java.util.Date;
import java.io.IOException;

import com.l7tech.policy.assertion.xmlsec.AddWssUsernameToken;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.message.Message;
import com.l7tech.message.SecurityKnob;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ISO8601Date;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.gateway.common.audit.AssertionMessages;

/**
 * Server assertion for addition of WS-Security UsernameToken
 */
public class ServerAddWssUsernameToken extends ServerAddWssEncryption<AddWssUsernameToken> {

    //- PUBLIC

    public ServerAddWssUsernameToken( final AddWssUsernameToken assertion, final ApplicationContext spring ) {
        super( assertion, assertion, assertion, logger );
        auditor = new Auditor(this, spring, logger);
        variableNames = assertion.getVariablesUsed();
        usernameHasVariables = assertion.getUsername() != null && assertion.getUsername().indexOf( "${" ) > -1;
        passwordHasVariables = assertion.getPassword() != null && assertion.getPassword().indexOf( "${" ) > -1;
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException {
        try {
            if (!message.isSoap()) {
                auditor.logAndAudit( AssertionMessages.ADD_WSS_USERNAME_NOT_SOAP, messageDescription);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        final XmlSecurityRecipientContext recipientContext = assertion.getRecipientContext();
        final SecurityKnob securityKnob = message.getSecurityKnob();
        final DecorationRequirements decorationRequirements = securityKnob.getAlternateDecorationRequirements( recipientContext );

        final String username = usernameHasVariables ?
                ExpandVariables.process(assertion.getUsername(), context.getVariableMap(variableNames, auditor), auditor) :
                assertion.getUsername();
        final String password = passwordHasVariables && assertion.isIncludePassword() ?
                ExpandVariables.process(assertion.getPassword(), context.getVariableMap(variableNames, auditor), auditor) :
                assertion.getPassword();

        final String created;
        if ( assertion.isIncludeCreated() ) {
            String millisDate = ISO8601Date.format(new Date());
            //truncate the milliseconds from our timesamp...
            //since it seems to cause problems w/WSE3 and some customers
            created = millisDate.replaceAll("\\.\\d+Z$", "Z");
        } else {
            created = "";
        }

        if ( assertion.isEncrypt() ) {
            final EncryptionContext encryptionContext;
            try {
                encryptionContext = buildEncryptionContext( context );
                DecorationRequirements wssReq = message.getSecurityKnob().getAlternateDecorationRequirements(encryptionContext.getRecipientContext());
                applyDecorationRequirements( wssReq, encryptionContext );
            } catch ( MultipleTokensException mte ) {
                auditor.logAndAudit(AssertionMessages.ADD_WSS_USERNAME_MORE_THAN_ONE_TOKEN);
                return AssertionStatus.BAD_REQUEST;
            }

            decorationRequirements.setEncryptUsernameToken( true );
            decorationRequirements.setSignUsernameToken( true );
            decorationRequirements.setSignTimestamp();
            decorationRequirements.setUseDerivedKeys( true );
        }

        decorationRequirements.setUsernameTokenCredentials( new UsernameTokenImpl(
                username != null ? username : null,
                password != null ? password.toCharArray() : null,
                created,
                assertion.isIncludeNonce() ? HexUtils.randomBytes(16) : null,
                assertion.isDigest() ) );

        return AssertionStatus.NONE;
    }

    @Override
    protected Auditor getAuditor() {
        return auditor;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerAddWssSecurityToken.class.getName());

    private final Auditor auditor;
    private final String[] variableNames;
    private final boolean usernameHasVariables;
    private final boolean passwordHasVariables;

}
