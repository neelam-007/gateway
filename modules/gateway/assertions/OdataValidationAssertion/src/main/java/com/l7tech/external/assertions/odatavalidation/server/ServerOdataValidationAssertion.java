package com.l7tech.external.assertions.odatavalidation.server;

import com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.util.ExceptionUtils;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Server side implementation of the OdataValidationAssertion.
 *
 * @see com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion
 */
public class ServerOdataValidationAssertion extends AbstractMessageTargetableServerAssertion<OdataValidationAssertion> {
    private final String[] variablesUsed;

    public ServerOdataValidationAssertion(final OdataValidationAssertion assertion) {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus doCheckRequest(final PolicyEnforcementContext context,
                                          final Message msg,
                                          final String targetName,
                                          final AuthenticationContext authContext) throws IOException {
        InputStream metadataStream = getMetadataDocumentStream(context);

        try {
            EntityProvider.readMetadata(metadataStream, false);
        } catch (EntityProviderException e) {
            logAndAudit(AssertionMessages.ODATA_VALIDATION_INVALID_SMD, ExceptionUtils.getMessage(e));
            return AssertionStatus.FAILED;
        }

        return AssertionStatus.NONE;
    }

    private ByteArrayInputStream getMetadataDocumentStream(PolicyEnforcementContext context) { // TODO: clean up, audit
        String variable;

        try {
            variable = (String) context.getVariable(assertion.getOdataMetadataSource());
        } catch (NoSuchVariableException e) {
            e.printStackTrace();
            return null;
        }

        return new ByteArrayInputStream(variable.getBytes());
    }
}
