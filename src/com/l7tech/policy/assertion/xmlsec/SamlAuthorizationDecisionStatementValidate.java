package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.ProcessorResult;
import org.springframework.context.ApplicationContext;
import org.w3c.dom.Document;
import x0Assertion.oasisNamesTcSAML1.AuthorizationDecisionStatementType;
import x0Assertion.oasisNamesTcSAML1.SubjectStatementAbstractType;
import x0Assertion.oasisNamesTcSAML1.DecisionType;
import x0Assertion.oasisNamesTcSAML1.ActionType;

import java.util.Collection;
import java.text.MessageFormat;

/**
 * @author emil
 * @version 27-Jan-2005
 */
class SamlAuthorizationDecisionStatementValidate extends SamlStatementValidate {
    /**
     * Construct  the <code>SamlStatementValidate</code> for the statement assertion
     *
     * @param statementAssertion the saml statement assertion
     * @param applicationContext the application context to allow access to components and services
     */
    SamlAuthorizationDecisionStatementValidate(SamlStatementAssertion statementAssertion, ApplicationContext applicationContext) {
        super(statementAssertion, applicationContext);
    }

    /**
     * Validate the authentication statement
     *
     * @param document
     * @param statementAbstractType
     * @param wssResults
     * @param validationResults     where the results are collected
     */
    protected void validateStatement(Document document,
                                     SubjectStatementAbstractType statementAbstractType,
                                     ProcessorResult wssResults, Collection validationResults) {
        if (!(statementAbstractType instanceof AuthorizationDecisionStatementType)) {
            throw new IllegalArgumentException("Expected " + AuthorizationDecisionStatementType.class);
        }
        AuthorizationDecisionStatementType authorizationDecisionStatementType = (AuthorizationDecisionStatementType)statementAbstractType;
        SamlAuthorizationStatement samlAuthorizationStatement = (SamlAuthorizationStatement)statementAssertionConstraints;

        String resource = authorizationDecisionStatementType.getResource();
        if (resource == null) {
            validationResults.add(new Error("No Resource specified", authorizationDecisionStatementType.toString(), null, null));
            return;
        }

        if (!resource.equals(samlAuthorizationStatement.getResource())) {
            validationResults.add(new Error(MessageFormat.format("Resource does not match, received {0}, expected {1}", new Object[]{resource, samlAuthorizationStatement.getResource()}),
              authorizationDecisionStatementType.toString(), null, null));
            return;
        }

        DecisionType.Enum decision = authorizationDecisionStatementType.getDecision();
        if (decision == null) {
            validationResults.add(new Error("No Decision specified", authorizationDecisionStatementType.toString(), null, null));
            return;
        }
        if (!DecisionType.PERMIT.equals(decision)) {
            validationResults.add(new Error("Permit Decision expected", authorizationDecisionStatementType.toString(), null, null));
            return;
        }
        String constraintsAction = samlAuthorizationStatement.getAction();
        if (constraintsAction == null) {
            validationResults.add(new Error("No Action specified", samlAuthorizationStatement, null, null));
            return;
        }

        String constraintsActionNameSpace = samlAuthorizationStatement.getActionNamespace();
        ActionType[] actionArray = authorizationDecisionStatementType.getActionArray();

        for (int i = 0; i < actionArray.length; i++) {
            ActionType actionType = actionArray[i];
            if (constraintsActionNameSpace != null) {
               if (!constraintsActionNameSpace.equals(actionType.getNamespace())) {
                   continue;
               }
            }
            if (constraintsAction.equals(actionType.getStringValue())) {
                logger.finer("Matched Action "+constraintsAction);
                return;
            }
        }
        validationResults.add(new Error(MessageFormat.format("Could not match action/namespace {0}/{1}",
          new Object[] {constraintsAction, constraintsActionNameSpace}), authorizationDecisionStatementType.toString(), null, null));
    }
}
