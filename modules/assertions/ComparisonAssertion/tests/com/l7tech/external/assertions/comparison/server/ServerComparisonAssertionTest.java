/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server;

import com.l7tech.common.ApplicationContexts;
import com.l7tech.common.message.Message;
import com.l7tech.common.util.ComparisonOperator;
import com.l7tech.common.util.Functions;
import com.l7tech.external.assertions.comparison.*;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.server.message.PolicyEnforcementContext;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerComparisonAssertionTest extends TestCase {
    private static final Logger log = Logger.getLogger(ServerComparisonAssertionTest.class.getName());

    public ServerComparisonAssertionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ServerComparisonAssertionTest.class);
    }

    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }

    public void testStringVariable() throws Exception {
        PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", new String[] { "12345", "12345" });

        ComparisonAssertion comp = make();

        ServerComparisonAssertion sca = new ServerComparisonAssertion(comp, ApplicationContexts.getTestApplicationContext());
        AssertionStatus stat = sca.checkRequest(context);
        assertEquals(AssertionStatus.NONE, stat);
    }

    public void testString() throws Exception {
        AssertionRegistry.installEnhancedMetadataDefaults();
        ComparisonAssertion comp = make();
        Functions.Unary<String, Assertion> funky = (Functions.Unary<String, Assertion>) comp.meta().get(AssertionMetadata.POLICY_NODE_NAME);
        System.out.println(funky.call(comp));
    }

    private ComparisonAssertion make() {
        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setExpression1("${asdf}");
        comp.setPredicates(
            new DataTypePredicate(DataType.INTEGER),
            new BinaryPredicate(ComparisonOperator.EQ, "12345", false, false),
//            new BinaryPredicate(ComparisonOperator.CONTAINS, "234", true, false),
            new CardinalityPredicate(1, -1, false),
            new StringLengthPredicate(5, 5, false)
        );
        return comp;
    }

    public void testNumeric() throws Exception {
        PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", new String[] { "12345", "12346" });

        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setExpression1("${asdf}");
        comp.setPredicates(
            new DataTypePredicate(DataType.INTEGER),
            new NumericRangePredicate(12345, 12346)
        );

        ServerComparisonAssertion sca = new ServerComparisonAssertion(comp, ApplicationContexts.getTestApplicationContext());
        AssertionStatus stat = sca.checkRequest(context);
        assertEquals(stat, AssertionStatus.NONE);
    }

}