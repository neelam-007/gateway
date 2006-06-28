package com.l7tech.server.policy.assertion.xmlsec;

import com.l7tech.common.security.xml.processor.ProcessorResult;
import com.l7tech.policy.assertion.xmlsec.RequestWssSaml;
import com.l7tech.policy.assertion.xmlsec.SamlAuthorizationStatement;
import org.w3c.dom.Document;
import org.apache.xmlbeans.XmlObject;
import x0Assertion.oasisNamesTcSAML1.ActionType;
import x0Assertion.oasisNamesTcSAML1.AuthorizationDecisionStatementType;
import x0Assertion.oasisNamesTcSAML1.DecisionType;

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
     */
    SamlAuthorizationDecisionStatementValidate(RequestWssSaml requestWssSaml) {
        super(requestWssSaml);
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
                            XmlObject statementAbstractType,
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

            // check action
            if (isNullOrEmpty(constraintsAction)) {
                logger.finer("Matched empty Action");
            } else if (constraintsAction.equals(actionType.getStringValue())) {
                logger.finer("Matched Action " + constraintsAction);
            } else {
                continue;
            }

            // check action namespace
            if (isNullOrEmpty(constraintsActionNameSpace)) {
                logger.finer("Matched empty Action Namespace");
            } else if(constraintsActionNameSpace.equals(actionType.getNamespace())) {
                logger.finer("Matched Action Namespace" + constraintsActionNameSpace);
            } else {
                continue;
            }

            // then this attribute is a match.
            return;
        }
        if (!isNullOrEmpty(constraintsActionNameSpace)) {
            validationResults.add(new SamlAssertionValidate.Error("No match action/namespace: {0}/{1}", authorizationDecisionStatementType.toString(),
                                                                                       new Object[]{constraintsAction, constraintsActionNameSpace}, null));
        } else {
            validationResults.add(new SamlAssertionValidate.Error("No match action: {0}", authorizationDecisionStatementType.toString(), constraintsAction, null));
        }
    }

    private boolean isNullOrEmpty(String s) {
        return s == null || "".equals(s);
    }
}
