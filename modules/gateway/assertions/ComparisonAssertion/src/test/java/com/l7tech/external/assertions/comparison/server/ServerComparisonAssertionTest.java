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
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.audit.LogOnlyAuditor;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ComparisonOperator;

import static org.junit.Assert.*;

import com.l7tech.util.Functions;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author alex
 */
public class ServerComparisonAssertionTest {

    @Test
    public void testStringVariable() throws Exception {
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", new String[] { "12345", "12345" });

        ComparisonAssertion comp = make();

        AssertionStatus stat = doit(context, comp);
        assertEquals(AssertionStatus.NONE, stat);
    }

    @Test
    public void testString() throws Exception {
        AssertionRegistry.installEnhancedMetadataDefaults();
        ComparisonAssertion comp = make();
        AssertionNodeNameFactory<ComparisonAssertion> funky = comp.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
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
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", new String[] { "12345", "12346" });

        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setExpression1("${asdf}");
        comp.setPredicates(
            new DataTypePredicate(DataType.INTEGER),
            new NumericRangePredicate<Integer>(12345, 12346)
        );

        AssertionStatus stat = doit(context, comp);
        assertEquals(AssertionStatus.NONE,stat);
    }

    @Test
    public void testGeneratedIntegerComparison() throws Exception {
        int value = 12345;
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
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
        comp.setPredicates( preds.toArray( new Predicate[preds.size()] ) );
        AssertionStatus stat = doit(context, comp);
        assertEquals(AssertionStatus.NONE, stat);
    }

    @Test
    public void testNullRightValue() throws Exception {
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", "asdf");
        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setExpression1("${asdf}");
        comp.setPredicates(new BinaryPredicate(ComparisonOperator.EQ, "${nonexistent}", true, false));
        AssertionStatus stat = doit(context, comp);
        assertEquals(AssertionStatus.FALSIFIED, stat);
    }

    @Test
    public void testNullLeftValue() throws Exception {
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", "asdf");
        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setExpression1("${nonexistent}");
        comp.setPredicates(new BinaryPredicate(ComparisonOperator.EQ, "${asdf}", true, false));
        AssertionStatus stat = doit(context, comp);
        assertEquals(AssertionStatus.FAILED, stat);
    }

    @Test
    public void testMultivaluedComparisonForDataTypes() throws Exception {
        final Object[] stringValues = new String[] { "12345", "12346" };
        doTestMultivalueComparisonForDataTypes( AssertionStatus.NONE, MultivaluedComparison.ALL, stringValues );
        doTestMultivalueComparisonForDataTypes( AssertionStatus.NONE, MultivaluedComparison.ANY, stringValues );
        doTestMultivalueComparisonForDataTypes( AssertionStatus.NONE, MultivaluedComparison.FIRST, stringValues );
        doTestMultivalueComparisonForDataTypes( AssertionStatus.NONE, MultivaluedComparison.LAST, stringValues );
        doTestMultivalueComparisonForDataTypes( AssertionStatus.NONE, MultivaluedComparison.ANY, new String[]{ "asdf", "erer", "1", "zzz" }, 2, 3 );
        doTestMultivalueComparisonForDataTypes( AssertionStatus.NONE, MultivaluedComparison.FIRST, new String[]{ "1", "zzz" } );
        doTestMultivalueComparisonForDataTypes( AssertionStatus.NONE, MultivaluedComparison.LAST, new String[]{ "asdf", "erer", "1" } );

        doTestMultivalueComparisonForDataTypes( AssertionStatus.FALSIFIED, MultivaluedComparison.ALL, new String[]{ "asdf", "erer" }, 0, 0 );
        doTestMultivalueComparisonForDataTypes( AssertionStatus.FALSIFIED, MultivaluedComparison.ANY, new String[]{ "asdf", "erer" }, 0, 0 );
        doTestMultivalueComparisonForDataTypes( AssertionStatus.FALSIFIED, MultivaluedComparison.FIRST, new String[]{ "asdf", "erer" }, 0, 0 );
        doTestMultivalueComparisonForDataTypes( AssertionStatus.FALSIFIED, MultivaluedComparison.LAST, new String[]{ "asdf", "erer" }, 0, 0 );
    }

    private void doTestMultivalueComparisonForDataTypes( final AssertionStatus expectedStatus,
                                                         final MultivaluedComparison multivaluedComparison,
                                                         final Object[] values ) throws Exception {
        doTestMultivalueComparisonForDataTypes( expectedStatus, multivaluedComparison, values, null, null );
    }

    private void doTestMultivalueComparisonForDataTypes( final AssertionStatus expectedStatus,
                                                         final MultivaluedComparison multivaluedComparison,
                                                         final Object[] values,
                                                         final Integer intRangeStart,
                                                         final Integer intRangeEnd ) throws Exception {
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", values);
        final ComparisonAssertion comp = new ComparisonAssertion();
        comp.setMultivaluedComparison( multivaluedComparison );
        comp.setExpression1( "${asdf}" );
        comp.setPredicates( new DataTypePredicate(DataType.INTEGER) );
        final State[] stateHolder = new State[1];
        final AssertionStatus stat = doit(context, new ServerComparisonAssertion(comp){
            @Override
            protected State makeState( final Object value, final Map<String, Object> variables ) {
                stateHolder[0] =  super.makeState( value, variables );
                return stateHolder[0];
            }
        });
        assertEquals( "Status", expectedStatus, stat );
        assertNotNull( "State", stateHolder[0] );
        assertTrue( "State is array", stateHolder[0].getValue() instanceof Object[] );
        List<Object> expectedIntegers = Arrays.asList( (Object[]) stateHolder[0].getValue() );
        if ( intRangeStart != null && intRangeEnd != null ) {
            expectedIntegers = expectedIntegers.subList( intRangeStart, intRangeEnd );
        } else switch ( multivaluedComparison ) {
            case FIRST:
                expectedIntegers = expectedIntegers.subList( 0, 1 );
                break;
            case LAST:
                expectedIntegers = expectedIntegers.subList( expectedIntegers.size()-1, expectedIntegers.size() );
                break;
        }
        final List<Object> expectedStrings = new ArrayList<Object>( Arrays.asList( (Object[]) stateHolder[0].getValue() ) );
        expectedStrings.removeAll( expectedIntegers );
        final Functions.BinaryVoid<Class<?>,Object> typeValidator = new Functions.BinaryVoid<Class<?>,Object>() {
            @Override
            public void call( final Class<?> expected, final Object value ) {
                assertTrue( "State value is " + expected + " (actual:" + value.getClass() + ", " + value + ")"
                        , expected.isInstance( value ) );
            }
        };
        CollectionUtils.foreach( expectedIntegers, true, Functions.partial( typeValidator, BigInteger.class ) );
        CollectionUtils.foreach( expectedStrings, true, Functions.partial( typeValidator, String.class ) );
    }

    @Test
    public void testMultivaluedComparisonForSingleValued() throws Exception {
        for ( boolean negated : new boolean[]{ false, true } ) {
            AssertionStatus NN = negated ? AssertionStatus.FALSIFIED : AssertionStatus.NONE;
            AssertionStatus NF = negated ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;;

            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.ALL, new EmptyPredicate( false, negated ), new Object[]{ "", "", "" } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.ALL, new EmptyPredicate( false, negated ), new Object[]{ "", "", "a" } );
            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.ANY, new EmptyPredicate( false, negated ), new Object[]{ "", "a", "" } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.ANY, new EmptyPredicate( false, negated ), new Object[]{ "a", "a", "a" } );
            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.FIRST, new EmptyPredicate( false, negated ), new Object[]{ "", "", "" } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.FIRST, new EmptyPredicate( false, negated ), new Object[]{ "a", "", "" } );
            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.LAST, new EmptyPredicate( false, negated ), new Object[]{ "", "", "" } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.LAST, new EmptyPredicate( false, negated ), new Object[]{ "", "", "a" } );

            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.ALL, new NumericRangePredicate<Integer>( 1, 5 ), new Object[]{ 1, 3, 5 } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.ALL, new NumericRangePredicate<Integer>( 1, 5 ), new Object[]{ 1, 3, 7 } );
            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.ANY, new NumericRangePredicate<Integer>( 1, 5 ), new Object[]{ 1, 13, 17 } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.ANY, new NumericRangePredicate<Integer>( 1, 5 ), new Object[]{ 11, 13, 17 } );
            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.FIRST, new NumericRangePredicate<Integer>( 1, 5 ), new Object[]{ 1, 3, 5 } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.FIRST, new NumericRangePredicate<Integer>( 1, 5 ), new Object[]{ 11, 3, 5 } );
            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.LAST, new NumericRangePredicate<Integer>( 1, 5 ), new Object[]{ 1, 3, 5  } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.LAST, new NumericRangePredicate<Integer>( 1, 5 ), new Object[]{ 1, 3, 15  } );

            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.ALL, new RegexPredicate( "a", negated ), new Object[]{ "a", "a", "a" } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.ALL, new RegexPredicate( "a", negated ), new Object[]{ "a", "", "a" } );
            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.ANY, new RegexPredicate( "a", negated ), new Object[]{ "", "a", "" } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.ANY, new RegexPredicate( "a", negated ), new Object[]{ "", "", "" } );
            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.FIRST, new RegexPredicate( "a", negated ), new Object[]{ "a", "", "" } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.FIRST, new RegexPredicate( "a", negated ), new Object[]{ "", "", "" } );
            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.LAST, new RegexPredicate( "a", negated ), new Object[]{ "", "", "a" } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.LAST, new RegexPredicate( "a", negated ), new Object[]{ "", "", "" } );

            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.ALL, new StringLengthPredicate( 1, 1, negated ), new Object[]{ "a", "a", "a" } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.ALL, new StringLengthPredicate( 1, 1, negated ), new Object[]{ "a", "", "a" } );
            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.ANY, new StringLengthPredicate( 1, 1, negated ), new Object[]{ "", "a", "" } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.ANY, new StringLengthPredicate( 1, 1, negated ), new Object[]{ "", "", "" } );
            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.FIRST, new StringLengthPredicate( 1, 1, negated ), new Object[]{ "a", "", "" } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.FIRST, new StringLengthPredicate( 1, 1, negated ), new Object[]{ "", "", "" } );
            doTestMultivaluedComparisonForSingleValued( negated, NN, MultivaluedComparison.LAST, new StringLengthPredicate( 1, 1, negated ), new Object[]{ "", "", "a" } );
            doTestMultivaluedComparisonForSingleValued( negated, NF, MultivaluedComparison.LAST, new StringLengthPredicate( 1, 1, negated ), new Object[]{ "", "", "" } );
        }
    }


    private void doTestMultivaluedComparisonForSingleValued( final boolean negated,
                                                             final AssertionStatus expectedStatus,
                                                             final MultivaluedComparison multivaluedComparison,
                                                             final Predicate predicate,
                                                             final Object[] values ) throws Exception {
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", values );
        final ComparisonAssertion comp = new ComparisonAssertion();
        comp.setMultivaluedComparison( multivaluedComparison );
        comp.setExpression1( "${asdf}" );
        predicate.setNegated( negated );
        comp.setPredicates( predicate );
        final AssertionStatus stat = doit(context, comp);
        assertEquals( "Status", expectedStatus, stat );
    }

    @Test
    public void testMultivaluedComparisonForBinaryPredicate() throws Exception {
        for ( boolean negated : new boolean[]{ false, true } ) {
            AssertionStatus NN = negated ? AssertionStatus.FALSIFIED : AssertionStatus.NONE;
            AssertionStatus NF = negated ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;;

            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.ALL, ComparisonOperator.GT, "-1" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.ALL, ComparisonOperator.GE, "0" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.ALL, ComparisonOperator.LT, "10" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.ALL, ComparisonOperator.LE, "9" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.ALL, ComparisonOperator.NE, "10" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.ALL, ComparisonOperator.EQ, "5" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.ALL, ComparisonOperator.GT, "5" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.ALL, ComparisonOperator.GE, "5" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.ALL, ComparisonOperator.LT, "5" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.ALL, ComparisonOperator.LE, "5" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.ALL, ComparisonOperator.NE, "5" );

            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.ANY, ComparisonOperator.EQ, "1" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.ANY, ComparisonOperator.GT, "1" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.ANY, ComparisonOperator.GE, "8" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.ANY, ComparisonOperator.LT, "8" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.ANY, ComparisonOperator.LE, "8" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.ANY, ComparisonOperator.NE, "8" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.ANY, ComparisonOperator.EQ, "15" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.ANY, ComparisonOperator.GT, "9" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.ANY, ComparisonOperator.GE, "15" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.ANY, ComparisonOperator.LT, "0" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.ANY, ComparisonOperator.LE, "-1" );

            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.FIRST, ComparisonOperator.EQ, "0" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.FIRST, ComparisonOperator.GT, "-1" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.FIRST, ComparisonOperator.GE, "-1" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.FIRST, ComparisonOperator.LT, "1" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.FIRST, ComparisonOperator.LE, "1" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.FIRST, ComparisonOperator.NE, "1" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.FIRST, ComparisonOperator.EQ, "1" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.FIRST, ComparisonOperator.GT, "0" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.FIRST, ComparisonOperator.GE, "1" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.FIRST, ComparisonOperator.LT, "0" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.FIRST, ComparisonOperator.LE, "-1" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.FIRST, ComparisonOperator.NE, "0" );

            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.LAST, ComparisonOperator.EQ, "9" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.LAST, ComparisonOperator.GT, "8" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.LAST, ComparisonOperator.GE, "9" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.LAST, ComparisonOperator.LT, "10" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.LAST, ComparisonOperator.LE, "9" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NN, MultivaluedComparison.LAST, ComparisonOperator.NE, "10" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.LAST, ComparisonOperator.EQ, "10" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.LAST, ComparisonOperator.GT, "9" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.LAST, ComparisonOperator.GE, "10" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.LAST, ComparisonOperator.LT, "9" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.LAST, ComparisonOperator.LE, "8" );
            doTestMultivaluedComparisonForBinaryPredicate( negated, NF, MultivaluedComparison.LAST, ComparisonOperator.NE, "9" );
        }
    }

    private void doTestMultivaluedComparisonForBinaryPredicate( final boolean negated,
                                                                final AssertionStatus expectedStatus,
                                                                final MultivaluedComparison multivaluedComparison,
                                                                final ComparisonOperator operator,
                                                                final String rightValue ) throws Exception {
        final Object values = new Object[]{ 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", values );
        final ComparisonAssertion comp = new ComparisonAssertion();
        comp.setMultivaluedComparison( multivaluedComparison );
        comp.setExpression1( "${asdf}" );
        comp.setPredicates( new BinaryPredicate( operator, rightValue, true, negated ) );
        final AssertionStatus stat = doit(context, comp);
        assertEquals( "Status", expectedStatus, stat );
    }

    private AssertionStatus doit( final PolicyEnforcementContext context,
                                  final ComparisonAssertion comparisonAssertion )
            throws IOException, PolicyAssertionException {
        return doit( context, new ServerComparisonAssertion(comparisonAssertion) );
    }


    private AssertionStatus doit( final PolicyEnforcementContext context,
                                  final ServerComparisonAssertion serverComparisonAssertion )
            throws IOException, PolicyAssertionException {
        ApplicationContexts.inject( serverComparisonAssertion, Collections.singletonMap( "auditFactory", new Auditor.AuditorFactory(){
            @Override
            public Auditor newInstance( final Object source, final Logger logger ) {
                return new LogOnlyAuditor( logger );
            }
        } ) );
        try {
            return serverComparisonAssertion.checkRequest(context);
        } catch ( AssertionStatusException e ) {
            return e.getAssertionStatus();
        }
    }
}