package com.l7tech.server.policy.assertion.transport;

import static com.l7tech.common.protocol.SecureSpanConstants.HttpHeaders.HEADER_DOMAINIDSTATUS;
import com.l7tech.common.protocol.DomainIdStatusHeader;
import com.l7tech.common.protocol.DomainIdStatusCode;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.transport.RemoteDomainIdentityInjection;
import com.l7tech.message.HttpRequestKnob;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.util.ExceptionUtils;

import javax.mail.internet.MimeUtility;
import java.io.IOException;
import java.util.Map;

/**
 * Server side implementation of the RemoteDomainIdentityInjection assertion.
 * This ensures that the client attempted to include domain identifiers, setting policy violated flag if not,
 * gathers up the identifier values from the HTTP headers, and stashes them into context variables.
 */
public class ServerRemoteDomainIdentityInjection extends AbstractServerAssertion<RemoteDomainIdentityInjection> {
    private final String variableUser;
    private final String variableDomain;
    private final String variableProgram;

    public ServerRemoteDomainIdentityInjection(RemoteDomainIdentityInjection assertion) {
        super(assertion);
        final String prefix = assertion.getVariablePrefix();
        variableUser = prefix + ".user";
        variableDomain = prefix + ".domain";
        variableProgram = prefix + ".program";
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {
        HttpRequestKnob knob = context.getRequest().getKnob(HttpRequestKnob.class);
        if (knob == null) {
            logAndAudit( AssertionMessages.DOMAINID_REQUEST_NOT_HTTP );
            return AssertionStatus.NOT_APPLICABLE;
        }

        String statusHeaderFullValue = knob.getHeaderSingleValue(HEADER_DOMAINIDSTATUS);
        if (statusHeaderFullValue == null) {
            context.setRequestPolicyViolated();
            logAndAudit( AssertionMessages.DOMAINID_NOT_ATTEMPTED );
            return AssertionStatus.FALSIFIED;
        }

        DomainIdStatusHeader statusHeader;
        try {
            statusHeader = DomainIdStatusHeader.parseValue(statusHeaderFullValue);
        } catch (IOException e) {
            logAndAudit( AssertionMessages.DOMAINID_BAD_REQUEST, HEADER_DOMAINIDSTATUS, ExceptionUtils.getMessage( e ) );
            return AssertionStatus.BAD_REQUEST;
        }

        DomainIdStatusCode status = statusHeader.getStatus();
        Map<String, String> identifiers = statusHeader.getParams();

        switch (status) {
            case NOTATTEMPTED:
                context.setRequestPolicyViolated();
                logAndAudit( AssertionMessages.DOMAINID_NOT_ATTEMPTED );
                return AssertionStatus.FALSIFIED;

            case DECLINED:
                logAndAudit( AssertionMessages.DOMAINID_DECLINED );
                return AssertionStatus.FALSIFIED;

            case FAILED:
                logAndAudit( AssertionMessages.DOMAINID_FAILED );
                return AssertionStatus.FALSIFIED;

            case INCLUDED:
                // Proceed and gather the data
                break;

            default:
                // can't happen
                logAndAudit( AssertionMessages.EXCEPTION_SEVERE_WITH_MORE_INFO, "Unrecognized status code: " + status.name() );
                return AssertionStatus.SERVER_ERROR;
        }

        // Now extract values
        boolean gotAll = true;
        if (!exportVar(variableUser, "username", identifiers, knob, context)) gotAll = false;
        if (!exportVar(variableDomain, "namespace", identifiers, knob, context)) gotAll = false;
        if (!exportVar(variableProgram, "program", identifiers, knob, context)) gotAll = false;

        if (!gotAll) {
            logAndAudit( AssertionMessages.DOMAINID_INCOMPLETE );
            return AssertionStatus.FALSIFIED;
        }

        return AssertionStatus.NONE;
    }

    private boolean exportVar(String variableName, String identifierName, Map<String, String> identifiers, HttpRequestKnob knob, PolicyEnforcementContext context) throws IOException {
        String headerName = identifiers.get(identifierName);
        if (headerName == null) {
            logAndAudit( AssertionMessages.DOMAINID_IDENTIFIER_MISSING, "identifierName" );
            return false;
        }

        String rawHeaderValue = knob.getHeaderSingleValue(headerName);
        if (rawHeaderValue == null) {
            logAndAudit( AssertionMessages.DOMAINID_BAD_REQUEST, "identifier " + identifierName, "header named " + headerName + " was promised but was not found" );
            return false;
        }

        String value = MimeUtility.decodeText(rawHeaderValue);
        context.setVariable(variableName, value);
        return true;
    }

}
