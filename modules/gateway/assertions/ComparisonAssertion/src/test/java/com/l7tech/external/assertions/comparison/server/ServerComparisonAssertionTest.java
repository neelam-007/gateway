/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.external.assertions.comparison.server;

import com.l7tech.external.assertions.comparison.*;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.variable.DataType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.ComparisonOperator;
import com.l7tech.util.Functions;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerComparisonAssertionTest {
    private static final Logger log = Logger.getLogger(ServerComparisonAssertionTest.class.getName());

    @Test
    public void testStringVariable() throws Exception {
        PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", new String[] { "12345", "12345" });

        ComparisonAssertion comp = make();

        AssertionStatus stat = doit(context, comp);
        assertEquals(AssertionStatus.NONE, stat);
    }

    @Test
    public void testString() throws Exception {
        AssertionRegistry.installEnhancedMetadataDefaults();
        ComparisonAssertion comp = make();
        AssertionNodeNameFactory funky = (AssertionNodeNameFactory) comp.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
        assertEquals("policy node name", funky.getAssertionName(comp, true), "Compare Expression: ${asdf} is an Integer, is equal to 12345, contains 234 (case sensitive), has at least 1 value and has exactly 5 characters");
    }

    private ComparisonAssertion make() {
        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setExpression1("${asdf}");
        comp.setPredicates(
            new DataTypePredicate(DataType.INTEGER),
            new BinaryPredicate(ComparisonOperator.EQ, "12345", false, false),
            new BinaryPredicate(ComparisonOperator.CONTAINS, "234", true, false),
            new CardinalityPredicate(1, -1, false),
            new StringLengthPredicate(5, 5, false)
        );
        return comp;
    }

    @Test
    public void testNumeric() throws Exception {
        PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", new String[] { "12345", "12346" });

        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setExpression1("${asdf}");
        comp.setPredicates(
            new DataTypePredicate(DataType.INTEGER),
            new NumericRangePredicate(12345, 12346)
        );

        AssertionStatus stat = doit(context, comp);
        assertEquals(stat, AssertionStatus.NONE);
    }

    @Test
    public void testGeneratedIntegerComparison() throws Exception {
        int value = 12345;
        PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", Integer.toString(value));
        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setExpression1("${asdf}");
        List<Predicate> preds = new ArrayList<Predicate>();
        preds.add(new BinaryPredicate(ComparisonOperator.EQ, "12345", false, false));
        preds.add(new BinaryPredicate(ComparisonOperator.NE, "2345", false, false));
        preds.add(new BinaryPredicate(ComparisonOperator.LT, "12346", false, false));
        preds.add(new BinaryPredicate(ComparisonOperator.LE, "12345", false, false));
        preds.add(new BinaryPredicate(ComparisonOperator.LE, "12346", false, false));
        preds.add(new BinaryPredicate(ComparisonOperator.GT, "12344", false, false));
        preds.add(new BinaryPredicate(ComparisonOperator.GE, "12345", false, false));
        preds.add(new BinaryPredicate(ComparisonOperator.GE, "12344", false, false));
        preds.add(new BinaryPredicate(ComparisonOperator.CONTAINS, "234", false, false));
        preds.add(new BinaryPredicate(ComparisonOperator.EMPTY, null, false, true));
        comp.setPredicates(preds.toArray(new Predicate[0]));
        AssertionStatus stat = doit(context, comp);
        assertEquals(stat, AssertionStatus.NONE);
    }

    @Test
    public void testNullRightValue() throws Exception {
        PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", "asdf");
        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setExpression1("${asdf}");
        comp.setPredicates(new BinaryPredicate(ComparisonOperator.EQ, "${nonexistent}", true, false));
        AssertionStatus stat = doit(context, comp);
        assertEquals(AssertionStatus.FALSIFIED, stat);
    }

    @Test
    public void testNullLeftValue() throws Exception {
        PolicyEnforcementContext context = new PolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", "asdf");
        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setExpression1("${nonexistent}");
        comp.setPredicates(new BinaryPredicate(ComparisonOperator.EQ, "${asdf}", true, false));
        AssertionStatus stat = doit(context, comp);
        assertEquals(AssertionStatus.FAILED, stat);
    }

    private AssertionStatus doit(PolicyEnforcementContext context, ComparisonAssertion comp)
        throws IOException, PolicyAssertionException {
        ServerComparisonAssertion sca = new ServerComparisonAssertion(comp, ApplicationContexts.getTestApplicationContext());
        return sca.checkRequest(context);
    }
}