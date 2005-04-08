package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import x0Assertion.oasisNamesTcSAML1.ActionType;
import x0Assertion.oasisNamesTcSAML1.AuthorizationDecisionStatementType;
import x0Assertion.oasisNamesTcSAML1.DecisionType;
import x0Assertion.oasisNamesTcSAML1.SubjectStatementAbstractType;

import java.text.MessageFormat;
import java.util.Collection;

/**
 * @author emil
 * @version 27-Jan-2005
 */
class SamlAuthorizationDecisionStatementValidate extends SamlStatementValidate {
    private SamlAuthorizationStatement authorizationStatementRequirements;

    /**
     * Construct  the <code>SamlAssertionValidate</code> for the statement assertion
     *
     * @param requestWssSaml     the saml statement assertion
     * @param applicationContext the application context to allow access to components and services
     */
    SamlAuthorizationDecisionStatementValidate(RequestWssSaml requestWssSaml, ApplicationContext applicationContext) {
        super(requestWssSaml, applicationContext);
        authorizationStatementRequirements = requestWssSaml.getAuthorizationStatement();
        if (authorizationStatementRequirements == null) {
            throw new IllegalArgumentException("Authorization requirements have not been specified");
        }

    }

    /**
     * Validate the authentication statement
     *
     * @param document
     * @param statementAbstractType
     * @param wssResults
     * @param validationResults     where the results are collected
     */
    protected void validate(Document document,
                            SubjectStatementAbstractType statementAbstractType,
                            ProcessorResult wssResults, Collection validationResults) {
        if (!(statementAbstractType instanceof AuthorizationDecisionStatementType)) {
            throw new IllegalArgumentException("Expected " + AuthorizationDecisionStatementType.class);
        }
        AuthorizationDecisionStatementType authorizationDecisionStatementType = (AuthorizationDecisionStatementType)statementAbstractType;

        String resource = authorizationDecisionStatementType.getResource();
        if (resource == null) {
            SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("No Resource specified", authorizationDecisionStatementType.toString(), null, null);
            logger.finer(result.toString());
            validationResults.add(result);
            return;
        }

        if (!resource.equals(authorizationStatementRequirements.getResource())) {
            SamlAssertionValidate.Error result =
              new SamlAssertionValidate.Error("Resource does not match, received {0}, expected {1}",
                                              authorizationDecisionStatementType.toString(),
                                              new Object[]{resource, authorizationStatementRequirements.getResource()}, null);
            validationResults.add(result);
            logger.finer(result.toString());
            return;
        }

        DecisionType.Enum decision = authorizationDecisionStatementType.getDecision();
        if (decision == null) {
            SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("No Decision specified", authorizationDecisionStatementType.toString(), null, null);
            validationResults.add(result);
            logger.finer(result.toString());
            return;
        }
        if (!DecisionType.PERMIT.equals(decision)) {
            SamlAssertionValidate.Error result = new SamlAssertionValidate.Error("Permit Decision expected", authorizationDecisionStatementType.toString(), null, null);
            validationResults.add(result);
            logger.finer(result.toString());
            return;
        }
        String constraintsAction = authorizationStatementRequirements.getAction();
        if (constraintsAction == null || "".equals(constraintsAction)) {
            logger.fine("No Action constraint requested");
            return;
        }

        String constraintsActionNameSpace = authorizationStatementRequirements.getActionNamespace();
        ActionType[] actionArray = authorizationDecisionStatementType.getActionArray();

        for (int i = 0; i < actionArray.length; i++) {
            ActionType actionType = actionArray[i];
            if (constraintsActionNameSpace != null && !"".equals(constraintsActionNameSpace)) {
                if (!constraintsActionNameSpace.equals(actionType.getNamespace())) {
                    continue;
                }
            }
            if (constraintsAction.equals(actionType.getStringValue())) {
                logger.finer("Matched Action " + constraintsAction);
                return;
            }
        }
        validationResults.add(new SamlAssertionValidate.Error(MessageFormat.format("Could not match action/namespace {0}/{1}",
                                                                                   new Object[]{constraintsAction, constraintsActionNameSpace}), authorizationDecisionStatementType.toString(), null, null));
    }
}
