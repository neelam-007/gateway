package com.l7tech.policy.assertion.xmlsec;

import org.junit.Test;


import static junit.framework.Assert.*;

public class SamlPolicyAssertionTest {
    @Test
    public void testClone() throws Exception {

        SamlPolicyAssertion samlPolicyAssertion = new SamlPolicyAssertion() {};
        samlPolicyAssertion.setAttributeStatement(new SamlAttributeStatement());
        samlPolicyAssertion.setAuthenticationStatement(new SamlAuthenticationStatement());
        samlPolicyAssertion.setAuthorizationStatement(new SamlAuthorizationStatement());

        // Get all statements which required to be cloned correctly
        final SamlAttributeStatement attributeStatement = samlPolicyAssertion.getAttributeStatement();
        final SamlAuthenticationStatement authenticationStatement = samlPolicyAssertion.getAuthenticationStatement();
        final SamlAuthorizationStatement authorizationStatement = samlPolicyAssertion.getAuthorizationStatement();

        attributeStatement.setAttributes(new SamlAttributeStatement.Attribute[]{new SamlAttributeStatement.Attribute()});
        authenticationStatement.setCustomAuthenticationMethods("Custom");
        authenticationStatement.setAuthenticationMethods(new String[]{"Method1"});
        authorizationStatement.setAction("My Action");

        final SamlPolicyAssertion clone = (SamlPolicyAssertion) samlPolicyAssertion.clone();
        // Modify the original and validate that the clone is not affected
        attributeStatement.getAttributes()[0].setName("Updated");
        assertNull(clone.getAttributeStatement().getAttributes()[0].getName());

        authenticationStatement.setCustomAuthenticationMethods("Updated");
        assertEquals("Custom", clone.getAuthenticationStatement().getCustomAuthenticationMethods());
        authenticationStatement.getAuthenticationMethods()[0] = "Method2";
        assertEquals("Method1", clone.getAuthenticationStatement().getAuthenticationMethods()[0]);
        authorizationStatement.setAction("Updated");
        assertEquals("My Action", clone.getAuthorizationStatement().getAction());

    }

}
