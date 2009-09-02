/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.policy.variable;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.variable.BuiltinVariables;
import com.l7tech.policy.variable.VariableMetadata;

import java.util.Arrays;

import org.junit.Test;
import org.junit.Assert;

/**
 * Unit tests for PolicyService.
 */
public class VariablePrefixesTest {

    @Test
    public void testAmbiguousPrefixes() throws Exception {
        Variable var = ServerVariables.getVariable("gateway.time.yyyy-MM-dd");
        Assert.assertEquals(var.getName(), "gateway.time");

        var = ServerVariables.getVariable("gateway.asdfasdf");
        Assert.assertEquals(var.getName(), "gateway");

        // make sure the legacy wss message attribute variables are retrieved correctly
        var = ServerVariables.getVariable("request.wss.signingcertificate");
        Assert.assertEquals("request.wss.signingcertificate", var.getName());

        var = ServerVariables.getVariable("request.wss.signingcertificate.base64");
        Assert.assertEquals("request.wss.signingcertificate.base64",
                var.getName() + BuiltinVariables.getUnmatchedName( "request.wss.signingcertificate.base64" ));

        var = ServerVariables.getVariable("request.wss.signingcertificate.pem");
        Assert.assertEquals("request.wss.signingcertificate.pem",
                var.getName() + BuiltinVariables.getUnmatchedName( "request.wss.signingcertificate.pem" ));
    }

    @Test
    public void testBuiltinPrefixes() throws Exception {
        VariableMetadata meta = BuiltinVariables.getMetadata("gateway.time.yyyy-MM-dd");
        Assert.assertEquals(meta.getName(), "gateway.time");
        Assert.assertTrue(meta.isPrefixed());

        meta = BuiltinVariables.getMetadata("gateway.asdfasdfafd");
        Assert.assertEquals(meta.getName(), "gateway");
        Assert.assertTrue(meta.isPrefixed());
    }

/*
    public void testSuccessorUsedVariables() throws Exception {
        SetVariableAssertion fooUser = new SetVariableAssertion();
        fooUser.setExpression("${foo.result}");

        SetVariableAssertion barUser = new SetVariableAssertion();
        barUser.setExpression("${bar.result}");

        SetVariableAssertion bazSetter = new SetVariableAssertion();
        bazSetter.setVariableToSet("baz");

        SetVariableAssertion bazUser = new SetVariableAssertion();
        bazUser.setExpression("${baz}");

        RequestXpathAssertion fooXpath = new RequestXpathAssertion(new XpathExpression("//foo"));
        fooXpath.setVariablePrefix("foo");
        RequestXpathAssertion barXpath = new RequestXpathAssertion(new XpathExpression("//bar"));
        barXpath.setVariablePrefix("bar");

        RequestXpathAssertion unusedVarsXpath = new RequestXpathAssertion(new XpathExpression("//unused"));
        unusedVarsXpath.setVariablePrefix("unusedVariable");

        makeThingie(bazSetter, bazUser, fooXpath, fooUser, barXpath, barUser, unusedVarsXpath);

        Set<String> usedByFooSuccessors = PolicyVariableUtils.getVariablesUsedBySuccessors(fooXpath);
        assertTrue(usedByFooSuccessors.contains("foo.result"));
        assertFalse(usedByFooSuccessors.contains("baz"));

        Set<String> usedByBarSuccessors = PolicyVariableUtils.getVariablesUsedBySuccessors(barXpath);
        assertFalse(usedByBarSuccessors.contains("foo.result"));

        Set<String> usedByOtherSuccessors = PolicyVariableUtils.getVariablesUsedBySuccessors(bazSetter);
        assertTrue(usedByOtherSuccessors.contains("foo.result"));
        assertTrue(usedByOtherSuccessors.contains("bar.result"));
        assertTrue(usedByOtherSuccessors.contains("baz"));

        Set<String> usedByUnused = PolicyVariableUtils.getVariablesUsedBySuccessors(unusedVarsXpath);
        assertTrue(usedByUnused.isEmpty());
    }
*/

    private CompositeAssertion makeThingie(Assertion otherSetter, Assertion otherUser, Assertion xpath1, Assertion user1, Assertion xpath2, Assertion user2, Assertion xpath3) {
        return new AllAssertion(Arrays.asList(new Assertion[] {
            otherSetter,
            new OneOrMoreAssertion(Arrays.asList(new Assertion[] {
                otherUser,
                new AllAssertion(Arrays.asList(new Assertion[] {
                    xpath1,
                    new AllAssertion(Arrays.asList(new Assertion[] { user1 })),
                })),
                new AllAssertion(Arrays.asList(new Assertion[] {
                    xpath2,
                })),
            })),
            user2,
            xpath3,
        }));
    }
}
