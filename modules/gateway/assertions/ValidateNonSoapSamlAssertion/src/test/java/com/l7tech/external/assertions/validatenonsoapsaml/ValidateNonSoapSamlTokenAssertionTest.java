package com.l7tech.external.assertions.validatenonsoapsaml;

import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.assertion.xmlsec.SamlAuthenticationStatement;
import com.l7tech.policy.variable.Syntax;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ValidateNonSoapSamlTokenAssertionTest {

    @Test
    public void testAllVariablesUsed() throws Exception {

        final ValidateNonSoapSamlTokenAssertion assertion = new ValidateNonSoapSamlTokenAssertion();
        //message target
        assertion.setTarget(TargetMessageType.OTHER);
        final String myInputVariable = "myInputVariable";
        assertion.setOtherTargetMessageVariable(myInputVariable);

        // subject confirmation data recipient
        final String dataRecipient = "myrecipient";
        assertion.setSubjectConfirmationDataRecipient(Syntax.getVariableExpression(dataRecipient));

        // name qualifier
        final String nameQualifier = "mynamequalififer";
        assertion.setNameQualifier(Syntax.getVariableExpression(nameQualifier));

        // audience restriction
        final String audienceRestriction = "myaudiencerestriction";
        assertion.setAudienceRestriction(Syntax.getVariableExpression(audienceRestriction));

        final SamlAuthenticationStatement authStmt = new SamlAuthenticationStatement();
        final String customAuthMethod = "mycustomauthmethod";
        authStmt.setCustomAuthenticationMethods(Syntax.getVariableExpression(customAuthMethod));
        assertion.setAuthenticationStatement(authStmt);

        final String[] variablesUsed = assertion.getVariablesUsed();
        final Set<String> vars = new HashSet<String>(Arrays.asList(variablesUsed));

        assertTrue("Missing message target variable reference", vars.contains(myInputVariable));
        assertTrue("Missing recipient variable reference", vars.contains(dataRecipient));
        assertTrue("Missing name qualifier variable reference", vars.contains(nameQualifier));
        assertTrue("Missing name audience restriction variable reference", vars.contains(audienceRestriction));
        assertTrue("Missing name custom auth statement variable reference", vars.contains(customAuthMethod));
    }
}
