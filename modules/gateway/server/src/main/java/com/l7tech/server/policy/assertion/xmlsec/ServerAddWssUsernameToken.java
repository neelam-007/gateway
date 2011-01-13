package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.message.SecurityKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.AddWssUsernameToken;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityRecipientContext;
import com.l7tech.security.token.UsernameTokenImpl;
import com.l7tech.security.xml.decorator.DecorationRequirements;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.ServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.HexUtils;
import com.l7tech.util.ISO8601Date;
import org.springframework.context.ApplicationContext;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Server assertion for addition of WS-Security UsernameToken
 */
public class ServerAddWssUsernameToken extends ServerAddWssEncryption<AddWssUsernameToken> {

    //- PUBLIC

    public ServerAddWssUsernameToken( final AddWssUsernameToken assertion, final ApplicationContext spring ) {
        super( assertion, assertion, assertion, assertion, logger );
        auditor = new Auditor(this, spring, logger);
        variableNames = assertion.getVariablesUsed();
    }

    @Override
    protected AssertionStatus doCheckRequest( final PolicyEnforcementContext context,
                                              final Message message,
                                              final String messageDescription,
                                              final AuthenticationContext authContext ) throws IOException, PolicyAssertionException
    {
        String password = assertion.isIncludePassword() ? assertion.getPassword() : null;
        return applyUsernameTokenSpecifiedCredentialsDecorationRequirements(addWssEncryptionSupport, context, message, messageDescription, assertion.getRecipientContext(),
                assertion.getUsername(), password, variableNames, assertion.isIncludeCreated(), assertion.isEncrypt(), assertion.isIncludeNonce(), assertion.isDigest(), this);
    }

    // Apply decoration requriements for a (possibly-encrypted) username token.
    static AssertionStatus applyUsernameTokenSpecifiedCredentialsDecorationRequirements(AddWssEncryptionSupport addWssEncryptionSupport,
                                                           PolicyEnforcementContext context,
                                                           Message message,
                                                           String messageDescription,
                                                           XmlSecurityRecipientContext recipientContext,
                                                           String usernameTemplate,
                                                           String passwordTemplate,
                                                           String[] variableNames,
                                                           boolean includeCreated,
                                                           boolean encrypt,
                                                           boolean includeNonce,
                                                           boolean digest,
                                                           ServerAssertion deferredAssertionOwner)
            throws IOException, PolicyAssertionException
    {
        final Audit auditor = addWssEncryptionSupport.getAuditor();
        try {
            if (!message.isSoap()) {
                auditor.logAndAudit( AssertionMessages.ADD_WSS_USERNAME_NOT_SOAP, messageDescription);
                return AssertionStatus.NOT_APPLICABLE;
            }
        } catch (SAXException e) {
            throw new CausedIOException(e);
        }

        final SecurityKnob securityKnob = message.getSecurityKnob();
        final DecorationRequirements decorationRequirements = securityKnob.getAlternateDecorationRequirements( recipientContext );

        final String username = usernameTemplate == null ? null : ExpandVariables.process(usernameTemplate, context.getVariableMap(variableNames, auditor), auditor);
        final String password = passwordTemplate == null ? null : ExpandVariables.process(passwordTemplate, context.getVariableMap(variableNames, auditor), auditor);

        final String created;
        if ( includeCreated ) {
            String millisDate = ISO8601Date.format(new Date());
            //truncate the milliseconds from our timesamp...
            //since it seems to cause problems w/WSE3 and some customers
            created = millisDate.replaceAll("\\.\\d+Z$", "Z");
        } else {
            created = "";
        }

        if ( encrypt ) {
            final AddWssEncryptionContext encryptionContext;
            try {
                encryptionContext = addWssEncryptionSupport.buildEncryptionContext( context );
                DecorationRequirements wssReq = message.getSecurityKnob().getAlternateDecorationRequirements(encryptionContext.getRecipientContext());
                addWssEncryptionSupport.applyDecorationRequirements( context, wssReq, encryptionContext, deferredAssertionOwner);
            } catch ( AddWssEncryptionSupport.MultipleTokensException mte ) {
                auditor.logAndAudit(AssertionMessages.ADD_WSS_USERNAME_MORE_THAN_ONE_TOKEN);
                return AssertionStatus.BAD_REQUEST;
            }

            decorationRequirements.setEncryptUsernameToken( true );
            decorationRequirements.setSignUsernameToken( true );
            decorationRequirements.setSignTimestamp(true);
            decorationRequirements.setUseDerivedKeys( true );
        }

        decorationRequirements.setUsernameTokenCredentials( new UsernameTokenImpl(
                username != null ? username : null,
                password != null ? password.toCharArray() : null,
                created,
                includeNonce ? HexUtils.randomBytes(16) : null,
                digest ) );

        return AssertionStatus.NONE;
    }

    @Override
    public Auditor getAuditor() {
        return auditor;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ServerAddWssSecurityToken.class.getName());

    private final Auditor auditor;
    private final String[] variableNames;
}
