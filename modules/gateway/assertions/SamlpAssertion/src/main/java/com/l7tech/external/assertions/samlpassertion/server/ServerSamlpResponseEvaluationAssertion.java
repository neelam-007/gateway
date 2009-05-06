package com.l7tech.external.assertions.samlpassertion.server;

import com.l7tech.external.assertions.samlpassertion.SamlpResponseEvaluationAssertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.xmlsec.SamlAttributeStatement;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.security.saml.SamlConstants;
import com.l7tech.message.Message;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.xml.soap.SoapUtil;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: vchan
 */
public class ServerSamlpResponseEvaluationAssertion extends AbstractServerAssertion<SamlpResponseEvaluationAssertion> {
    private static final Logger logger = Logger.getLogger(ServerSamlpResponseEvaluationAssertion.class.getName());

    private static final String STATUSCODE_SAMLP_SUCCESS = SamlConstants.NS_SAMLP + ":" + SamlConstants.STATUS_SUCCESS;
    private static final String STATUSCODE_SAMLP2_SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:" + SamlConstants.STATUS_SUCCESS;
    private static final String AUTHZ_DECISION_PERMIT = "Permit";

    private static final String VAR_SEP = ".";
    private static final String VAR_PREFIX_DEFAULT = "samlpResponse";
    private static final String VAR_STATUS = "status";
    private static final String VAR_AUTHZ_DECISION = "authz.decision";
    private static final String VAR_ATTRIBUTE = "attribute";

    private final Auditor auditor;
    private final String[] variablesUsed;
    private final String variablePrefix;

    /**
     * Constructor.
     *
     * @param assertion the SAMLP response assertion
     * @param spring the ApplicationContext
     */
    public ServerSamlpResponseEvaluationAssertion(SamlpResponseEvaluationAssertion assertion, ApplicationContext spring) {
        super(assertion);
        this.auditor = new Auditor(this, spring, logger);
        this.variablesUsed = assertion.getVariablesUsed();

        if (assertion.getVariablePrefixOverride() != null)
            this.variablePrefix = assertion.getVariablePrefixOverride();
        else
            this.variablePrefix = VAR_PREFIX_DEFAULT;
    }

    /**
     * @see AbstractServerAssertion#checkRequest(com.l7tech.server.message.PolicyEnforcementContext)
     */
    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        try {
            // initialize stuff
            final Map<String, Object> variablesMap = context.getVariableMap(variablesUsed, auditor);

            /*
             * 1) Determine where the target message is supposed to go
             */
            final Message msg;
            try {
                msg = context.getTargetMessage(assertion);

                // parse the message into the required

            } catch (NoSuchVariableException e) {
                auditor.logAndAudit(AssertionMessages.NO_SUCH_VARIABLE, e.getVariable());
                throw new SamlpAssertionException(e);
            }

            /*
             * 2) Call the configured evaluator
             */
            final AbstractSamlpResponseEvaluator.ResponseBean responseValues = evaluateMessage(msg);
            // time to check message contents
            checkResponseData( responseValues, variablesMap );

            // log for debugging
            if (logger.isLoggable(Level.FINER)) {
                logger.finer(responseValues.toString());
            }

            /*
             * 3) Set the values into the appropriate context variables
             */
            populateContextVariables(context, responseValues);

        } catch (SamlpResponseEvaluationException badResp) {
//            auditor.logAndAudit(AssertionMessages.SAMLP_EVALUATOR_BAD_RESP, ExceptionUtils.getMessage(badResp));
            logger.log(Level.WARNING, "SAMLP response invalidated: " + ExceptionUtils.getMessage(badResp));
            return AssertionStatus.FALSIFIED;
        } catch (SamlpAssertionException samlEx) {
//            auditor.logAndAudit(AssertionMessages.SAMLP_EVALUATOR_ERROR, new String[0], samlEx);
            logger.log(Level.WARNING, "SAMLP response evaluator: " + ExceptionUtils.getMessage(samlEx));
            return AssertionStatus.FAILED;
        }

//        auditor.logAndAudit(AssertionMessages.SAMLP_EVALUATOR_COMPLETE, getResponseType());
        return AssertionStatus.NONE;
    }


    private AbstractSamlpResponseEvaluator.ResponseBean evaluateMessage(Message msg) throws SamlpAssertionException {

        // check the message
        Node samlpPayload;
        try {
            if (msg.isSoap()) {
                samlpPayload = SoapUtil.getPayloadElement(msg.getXmlKnob().getDocumentReadOnly());
            } else if (msg.isXml()) {
                samlpPayload = msg.getXmlKnob().getDocumentReadOnly();
            } else {
                throw new SamlpAssertionException("Response message is not XML");
            }

            if (samlpPayload == null) {
                throw new SamlpAssertionException("SAMLP payload not found");
            }
        } catch (IOException ioex) {
            throw new SamlpAssertionException("Failed to get SAMLP XML from message", ioex);

        } catch (SAXException saex) {
            throw new SamlpAssertionException("Failed to get SAMLP XML from message", saex);

        } catch (InvalidDocumentFormatException badDoc) {
            throw new SamlpAssertionException("Failed to extract SAMLP payload from SOAP body", badDoc);
        }

        // call the appropriate evaluator
        AbstractSamlpResponseEvaluator<?> evaluator;
        try {
            if (assertion.getSamlVersion() == 2) {
                evaluator = new com.l7tech.external.assertions.samlpassertion.server.v2.ResponseEvaluator(assertion);
            } else {
                 evaluator = new com.l7tech.external.assertions.samlpassertion.server.v1.ResponseEvaluator(assertion);
            }

            return evaluator.parseMessage(samlpPayload);

        } catch (SamlpAssertionException samex) {
            logger.log(Level.WARNING, "Failed to unmarshal response message: " + ExceptionUtils.getMessage(samex));
            throw samex;
        }
    }

    protected void checkResponseData( final AbstractSamlpResponseEvaluator.ResponseBean responseValues,
                                      final Map<String,Object> variablesMap ) throws SamlpResponseEvaluationException {

        if (responseValues == null) {
            throw new SamlpResponseEvaluationException("Response message cannot be null");
        }

        if (assertion.getSamlVersion() == 2)
            checkStatusCode(STATUSCODE_SAMLP2_SUCCESS, responseValues);
        else
            checkStatusCode(STATUSCODE_SAMLP_SUCCESS, responseValues);

        checkConformanceValues();
        checkAuthzDecision(responseValues);
        checkAttributeValues(responseValues, variablesMap);
    }

    /**
     * Checks all necessary conformance values like version, issue date/times,  etc...
     */
    private void checkConformanceValues() {

        // check common stuff -- version, issueInstant, responseTo... etc

    }

    private void checkStatusCode(final String expectedValue, final AbstractSamlpResponseEvaluator.ResponseBean responseValues) throws SamlpResponseEvaluationException {

        String error = null;
        // Status Code
        if (!responseValues.getStatusCodes().isEmpty()) {
            if (!expectedValue.equals(responseValues.getStatusCodes().get(0))) {
                error = "Bad response statusCode";
            }
        } else {
            error = "Missing response status code";
        }

        if (assertion.isResponseStatusFalsifyAssertion() && error != null) {
            throw new SamlpResponseEvaluationException(error);
        }
    }

    private void checkAuthzDecision( final AbstractSamlpResponseEvaluator.ResponseBean responseValues ) throws SamlpResponseEvaluationException {

        // only check when configured
        if (assertion.getAuthorizationStatement() == null)
            return;

        String expectedValue;
        if (assertion.getAuthzDecisionOption() == 0) {
            expectedValue = AUTHZ_DECISION_PERMIT;
        } else {
            expectedValue = assertion.getAuthzDecisionVariable();
        }

        if (expectedValue == null) {
            logger.warning("AuthzDecision expected value not set in the assertion, using default value");
            expectedValue = AUTHZ_DECISION_PERMIT;
        }

        String error = null;
        // Status Code
        if (responseValues.getAuthzDecision() == null) {
            error = "Missing authorization decision code";

        } else if (!expectedValue.equals(responseValues.getAuthzDecision())) {
            error = "Bad authorization decision code: " + responseValues.getAuthzDecision() + "; expecting: " + expectedValue;
        }

        if (assertion.isAuthzDecisionFalsifyAssertion() && error != null) {
            throw new SamlpResponseEvaluationException(error);
        }
    }

    private void checkAttributeValues( final AbstractSamlpResponseEvaluator.ResponseBean responseValues,
                                       final Map<String,Object> variablesMap ) throws SamlpResponseEvaluationException {

        // only check when configured
        if (assertion.getAttributeStatement() == null)
            return;

        String missing = null;

        Map<String, ResponseAttributeData> theAttribs = responseValues.getAttributes();
        String key;
        int passCount = 0;
        final boolean isSamlv1 = (assertion.getSamlVersion() == 1);
        for (SamlAttributeStatement.Attribute sas : assertion.getAttributeStatement().getAttributes()) {

            key = sas.getName();
            if (theAttribs.containsKey(key) && matchAttribute(sas, theAttribs.get(key), variablesMap)) {
                passCount++;
            } else {
                StringBuffer sb = new StringBuffer();
                sb.append("[");
                sb.append((isSamlv1 ? sas.getNamespace() : sas.getNameFormat()));
                if (!isSamlv1)
                    sb.append(", ").append(sas.getFriendlyName());
                sb.append("] ").append(key);
                missing = sb.toString();
                break; // fail on first missing attribute
            }
        }

        if (logger.isLoggable(Level.FINER)) {
            logger.finer("Attribute check passed:"+passCount);
        }

        if (missing != null) {
            throw new SamlpResponseEvaluationException("failed attribute check for: " + missing);
        }
    }


    private boolean matchAttribute( final SamlAttributeStatement.Attribute configAttr,
                                    final ResponseAttributeData actual,
                                    final Map<String,Object> variablesMap ) {

        boolean matches;

        // Saml v2 check
        if (assertion.getSamlVersion() == 2) {
            String nfValue = (configAttr.getNameFormat() == null ?
                    SamlConstants.ATTRIBUTE_NAME_FORMAT_UNSPECIFIED : configAttr.getNameFormat());

            matches = checkVariableEquals(configAttr.getName(), actual.getName(), variablesMap) &&
                        checkVariableEquals(nfValue, actual.getNamespaceOrFormat(), variablesMap);

            if (configAttr.getFriendlyName() != null && !configAttr.getFriendlyName().isEmpty()) {
                matches &= checkVariableEquals(configAttr.getFriendlyName(), actual.getFriendlyName(), variablesMap);
            }
        // Saml v1.1 check
        } else {
            matches = checkVariableEquals(configAttr.getName(), actual.getName(), variablesMap) &&
                        checkVariableEquals(configAttr.getNamespace(), actual.getNamespaceOrFormat(), variablesMap);
        }

        // what about non-strings??
        if (matches) {
            matches = false;
            String expected = configAttr.getValue();
            for (Object o : actual.getAttributeValues()) {

                if (configAttr.isAnyValue() && o != null) {
                    matches = true;
                    break;
                }
                if (expected.equals(o)) {
                    matches = true;
                    break;
                }
            }
        }
        return matches;
    }

    private void populateContextVariables( final PolicyEnforcementContext pec,
                                           final AbstractSamlpResponseEvaluator.ResponseBean responseValues )
        throws SamlpAssertionException
    {
        pec.setVariable(createVarName(VAR_STATUS), responseValues.getStatusCodes().get(0));

        if (assertion.getAuthorizationStatement() != null) {
            pec.setVariable(createVarName(VAR_AUTHZ_DECISION), responseValues.getAuthzDecision());
        }

        List<ResponseAttributeData> atl = responseValues.getAttributesList();
        for (ResponseAttributeData ad : atl) {
            pec.setVariable(createAttributeVarName(ad.getName()), ad.getAttributeValuesAsArray());
        }
    }

    private String createVarName(String varSuffix) {
        return new StringBuffer(variablePrefix).append(VAR_SEP).append(varSuffix).toString();
    }

    private String createAttributeVarName(String varSuffix) {
        StringBuffer sb = new StringBuffer(variablePrefix).append(VAR_SEP).append(VAR_ATTRIBUTE).append(VAR_SEP);
        sb.append(varSuffix);
        return sb.toString();
    }

    protected String getVariableValue(String var, Map<String,Object> variablesMap) {
        if (var != null)
            return ExpandVariables.process(var, variablesMap, auditor);
        return null;
    }

    protected boolean checkVariableEquals(String valueWithVars, String actual, Map<String,Object> variablesMap) {
        String fullVal = getVariableValue(valueWithVars, variablesMap);
        if (fullVal != null)
            return fullVal.equals(actual);
        return false;
    }
}