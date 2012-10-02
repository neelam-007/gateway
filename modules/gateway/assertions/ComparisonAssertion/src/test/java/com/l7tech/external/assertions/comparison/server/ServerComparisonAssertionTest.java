package com.l7tech.external.assertions.comparison.server;

import com.l7tech.external.assertions.comparison.*;
import com.l7tech.external.assertions.comparison.server.convert.ValueConverter;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.Message;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.variable.DataType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.server.policy.assertion.AssertionStatusException;
import com.l7tech.test.BugNumber;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author alex
 */
@SuppressWarnings({"JavaDoc"})
public class ServerComparisonAssertionTest {

    @Before
    public void setUp() throws Exception {
        testAudit = new TestAudit();
        ValueConverter.Factory.setDateParser(new DateTimeConfigUtils());
    }

    @Test
    public void testStringVariable() throws Exception {
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", new String[]{"12345", "12345"});

        ComparisonAssertion comp = make(MultivaluedComparison.ANY);

        AssertionStatus stat = doit(context, comp);
        assertEquals(AssertionStatus.NONE, stat);
    }

    @Test
    @BugNumber(11015)
    public void testString() throws Exception {
        AssertionRegistry.installEnhancedMetadataDefaults();
        AssertionNodeNameFactory<ComparisonAssertion> funky;
        final ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.external.assertions.comparison.ComparisonAssertion");
        final String policyAssertionName = "Compare Expression: ${asdf} is an Integer, is equal to 12345, contains 234 (case sensitive), has at least 1 value and has exactly 5 characters; ";
        final String multiAssertionText = resources.getString("multivaluedComparison.label") + " ";
        String labelKey = null;
        ComparisonAssertion comp = make(MultivaluedComparison.ALL);
        funky = comp.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
        labelKey = "multivaluedComparison." + MultivaluedComparison.ALL.name() + ".text";
        assertEquals("policy node name", funky.getAssertionName(comp, true), policyAssertionName + multiAssertionText + resources.getString(labelKey));

        comp = make(MultivaluedComparison.ANY);
        funky = comp.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
        labelKey = "multivaluedComparison." + MultivaluedComparison.ANY.name() + ".text";
        assertEquals("policy node name", funky.getAssertionName(comp, true), policyAssertionName + multiAssertionText + resources.getString(labelKey));

        comp = make(MultivaluedComparison.FAIL);
        funky = comp.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
        labelKey = "multivaluedComparison." + MultivaluedComparison.FAIL.name() + ".text";
        assertEquals("policy node name", funky.getAssertionName(comp, true), policyAssertionName + multiAssertionText + resources.getString(labelKey));

        comp = make(MultivaluedComparison.FIRST);
        funky = comp.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
        labelKey = "multivaluedComparison." + MultivaluedComparison.FIRST.name() + ".text";
        assertEquals("policy node name", funky.getAssertionName(comp, true), policyAssertionName + multiAssertionText + resources.getString(labelKey));

        comp = make(MultivaluedComparison.LAST);
        funky = comp.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
        labelKey = "multivaluedComparison." + MultivaluedComparison.LAST.name() + ".text";
        assertEquals("policy node name", funky.getAssertionName(comp, true), policyAssertionName + multiAssertionText + resources.getString(labelKey));
    }

    private ComparisonAssertion make(MultivaluedComparison multivaluedComparison) {
        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setMultivaluedComparison(multivaluedComparison);
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
        context.setVariable("asdf", new String[]{"12345", "12346"});

        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setExpression1("${asdf}");
        comp.setPredicates(
                new DataTypePredicate(DataType.INTEGER),
                new NumericRangePredicate<Integer>(12345, 12346)
        );

        AssertionStatus stat = doit(context, comp);
        assertEquals(AssertionStatus.NONE, stat);
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
        comp.setPredicates(preds.toArray(new Predicate[preds.size()]));
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
    @BugNumber(12054)
    public void testNullRightValueContainsNotNegated() throws Exception {
        testContainsStrategy(false);
    }

    @Test
    @BugNumber(12054)
    public void testNullRightValueContainsNegated() throws Exception {
        testContainsStrategy(true);
    }

    private void testContainsStrategy(boolean negate) throws Exception {
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", "asdf");
        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setExpression1("${asdf}");
        final ComparisonOperator operator = ComparisonOperator.CONTAINS;
        comp.setPredicates(new BinaryPredicate(operator, "${nonexistent}", true, negate));
        AssertionStatus stat = doit(context, comp);
        assertEquals(AssertionStatus.FALSIFIED, stat);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertTrue(testAudit.isAuditPresent(AssertionMessages.COMPARISON_RIGHT_IS_NULL));
        assertTrue(testAudit.isAuditPresentContaining(MessageFormat.format(AssertionMessages.COMPARISON_RIGHT_IS_NULL.getMessage(), operator.getName())));
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
    @BugNumber(12677)
    public void testNullLeftValueWithVariableAsExpression() throws Exception {
        String comparisonValue = "shouldNotMatch";
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        ComparisonAssertion comp = new ComparisonAssertion();
        comp.setExpression1("${nonexistent}");
        comp.setPredicates(new BinaryPredicate(ComparisonOperator.EQ, comparisonValue, true, false));
        comp.setTreatVariableAsExpression(true);
        AssertionStatus stat = doit(context, comp);
        assertEquals(AssertionStatus.FALSIFIED, stat);
        testAudit.isAuditPresent(AssertionMessages.COMPARISON_NOT);
    }

    @Test
    public void testMultivaluedComparisonForDataTypes() throws Exception {
        final Object[] stringValues = new String[]{"12345", "12346"};
        doTestMultivalueComparisonForDataTypes(AssertionStatus.NONE, MultivaluedComparison.ALL, stringValues);
        doTestMultivalueComparisonForDataTypes(AssertionStatus.NONE, MultivaluedComparison.ANY, stringValues);
        doTestMultivalueComparisonForDataTypes(AssertionStatus.NONE, MultivaluedComparison.FIRST, stringValues);
        doTestMultivalueComparisonForDataTypes(AssertionStatus.NONE, MultivaluedComparison.LAST, stringValues);
        doTestMultivalueComparisonForDataTypes(AssertionStatus.NONE, MultivaluedComparison.ANY, new String[]{"asdf", "erer", "1", "zzz"}, 2, 3);
        doTestMultivalueComparisonForDataTypes(AssertionStatus.NONE, MultivaluedComparison.FIRST, new String[]{"1", "zzz"});
        doTestMultivalueComparisonForDataTypes(AssertionStatus.NONE, MultivaluedComparison.LAST, new String[]{"asdf", "erer", "1"});

        doTestMultivalueComparisonForDataTypes(AssertionStatus.FALSIFIED, MultivaluedComparison.ALL, new String[]{"asdf", "erer"}, 0, 0);
        doTestMultivalueComparisonForDataTypes(AssertionStatus.FALSIFIED, MultivaluedComparison.ANY, new String[]{"asdf", "erer"}, 0, 0);
        doTestMultivalueComparisonForDataTypes(AssertionStatus.FALSIFIED, MultivaluedComparison.FIRST, new String[]{"asdf", "erer"}, 0, 0);
        doTestMultivalueComparisonForDataTypes(AssertionStatus.FALSIFIED, MultivaluedComparison.LAST, new String[]{"asdf", "erer"}, 0, 0);
    }

    private void doTestMultivalueComparisonForDataTypes(final AssertionStatus expectedStatus,
                                                        final MultivaluedComparison multivaluedComparison,
                                                        final Object[] values) throws Exception {
        doTestMultivalueComparisonForDataTypes(expectedStatus, multivaluedComparison, values, null, null);
    }

    private void doTestMultivalueComparisonForDataTypes(final AssertionStatus expectedStatus,
                                                        final MultivaluedComparison multivaluedComparison,
                                                        final Object[] values,
                                                        final Integer intRangeStart,
                                                        final Integer intRangeEnd) throws Exception {
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", values);
        final ComparisonAssertion comp = new ComparisonAssertion();
        comp.setMultivaluedComparison(multivaluedComparison);
        comp.setExpression1("${asdf}");
        comp.setPredicates(new DataTypePredicate(DataType.INTEGER));
        final State[] stateHolder = new State[1];
        final AssertionStatus stat = doit(context, new ServerComparisonAssertion(comp) {
            @Override
            protected State makeState(@NotNull final Object value, final Map<String, Object> variables) {
                stateHolder[0] = super.makeState(value, variables);
                return stateHolder[0];
            }
        });
        assertEquals("Status", expectedStatus, stat);
        assertNotNull("State", stateHolder[0]);
        assertTrue("State is array", stateHolder[0].getValue() instanceof Object[]);
        List<Object> expectedIntegers = Arrays.asList((Object[]) stateHolder[0].getValue());
        if (intRangeStart != null && intRangeEnd != null) {
            expectedIntegers = expectedIntegers.subList(intRangeStart, intRangeEnd);
        } else switch (multivaluedComparison) {
            case FIRST:
                expectedIntegers = expectedIntegers.subList(0, 1);
                break;
            case LAST:
                expectedIntegers = expectedIntegers.subList(expectedIntegers.size() - 1, expectedIntegers.size());
                break;
        }
        final List<Object> expectedStrings = new ArrayList<Object>(Arrays.asList((Object[]) stateHolder[0].getValue()));
        expectedStrings.removeAll(expectedIntegers);
        final Functions.BinaryVoid<Class<?>, Object> typeValidator = new Functions.BinaryVoid<Class<?>, Object>() {
            @Override
            public void call(final Class<?> expected, final Object value) {
                assertTrue("State value is " + expected + " (actual:" + value.getClass() + ", " + value + ")"
                        , expected.isInstance(value));
            }
        };
        CollectionUtils.foreach(expectedIntegers, true, Functions.partial(typeValidator, BigInteger.class));
        CollectionUtils.foreach(expectedStrings, true, Functions.partial(typeValidator, String.class));
    }

    @Test
    public void testMultivaluedComparisonForSingleValued() throws Exception {
        for (boolean negated : new boolean[]{false, true}) {
            AssertionStatus NN = negated ? AssertionStatus.FALSIFIED : AssertionStatus.NONE;
            AssertionStatus NF = negated ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;

            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.ALL, new EmptyPredicate(false, negated), new Object[]{"", "", ""});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.ALL, new EmptyPredicate(false, negated), new Object[]{"", "", "a"});
            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.ANY, new EmptyPredicate(false, negated), new Object[]{"", "a", ""});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.ANY, new EmptyPredicate(false, negated), new Object[]{"a", "a", "a"});
            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.FIRST, new EmptyPredicate(false, negated), new Object[]{"", "", ""});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.FIRST, new EmptyPredicate(false, negated), new Object[]{"a", "", ""});
            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.LAST, new EmptyPredicate(false, negated), new Object[]{"", "", ""});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.LAST, new EmptyPredicate(false, negated), new Object[]{"", "", "a"});

            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.ALL, new NumericRangePredicate<Integer>(1, 5), new Object[]{1, 3, 5});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.ALL, new NumericRangePredicate<Integer>(1, 5), new Object[]{1, 3, 7});
            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.ANY, new NumericRangePredicate<Integer>(1, 5), new Object[]{1, 13, 17});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.ANY, new NumericRangePredicate<Integer>(1, 5), new Object[]{11, 13, 17});
            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.FIRST, new NumericRangePredicate<Integer>(1, 5), new Object[]{1, 3, 5});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.FIRST, new NumericRangePredicate<Integer>(1, 5), new Object[]{11, 3, 5});
            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.LAST, new NumericRangePredicate<Integer>(1, 5), new Object[]{1, 3, 5});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.LAST, new NumericRangePredicate<Integer>(1, 5), new Object[]{1, 3, 15});

            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.ALL, new RegexPredicate("a", negated), new Object[]{"a", "a", "a"});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.ALL, new RegexPredicate("a", negated), new Object[]{"a", "", "a"});
            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.ANY, new RegexPredicate("a", negated), new Object[]{"", "a", ""});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.ANY, new RegexPredicate("a", negated), new Object[]{"", "", ""});
            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.FIRST, new RegexPredicate("a", negated), new Object[]{"a", "", ""});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.FIRST, new RegexPredicate("a", negated), new Object[]{"", "", ""});
            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.LAST, new RegexPredicate("a", negated), new Object[]{"", "", "a"});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.LAST, new RegexPredicate("a", negated), new Object[]{"", "", ""});

            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.ALL, new StringLengthPredicate(1, 1, negated), new Object[]{"a", "a", "a"});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.ALL, new StringLengthPredicate(1, 1, negated), new Object[]{"a", "", "a"});
            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.ANY, new StringLengthPredicate(1, 1, negated), new Object[]{"", "a", ""});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.ANY, new StringLengthPredicate(1, 1, negated), new Object[]{"", "", ""});
            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.FIRST, new StringLengthPredicate(1, 1, negated), new Object[]{"a", "", ""});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.FIRST, new StringLengthPredicate(1, 1, negated), new Object[]{"", "", ""});
            doTestMultivaluedComparisonForSingleValued(negated, NN, MultivaluedComparison.LAST, new StringLengthPredicate(1, 1, negated), new Object[]{"", "", "a"});
            doTestMultivaluedComparisonForSingleValued(negated, NF, MultivaluedComparison.LAST, new StringLengthPredicate(1, 1, negated), new Object[]{"", "", ""});
        }
    }


    private void doTestMultivaluedComparisonForSingleValued(final boolean negated,
                                                            final AssertionStatus expectedStatus,
                                                            final MultivaluedComparison multivaluedComparison,
                                                            final Predicate predicate,
                                                            final Object[] values) throws Exception {
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", values);
        final ComparisonAssertion comp = new ComparisonAssertion();
        comp.setMultivaluedComparison(multivaluedComparison);
        comp.setExpression1("${asdf}");
        predicate.setNegated(negated);
        comp.setPredicates(predicate);
        final AssertionStatus stat = doit(context, comp);
        assertEquals("Status", expectedStatus, stat);
    }

    @Test
    public void testMultivaluedComparisonForBinaryPredicate() throws Exception {
        for (boolean negated : new boolean[]{false, true}) {
            AssertionStatus NN = negated ? AssertionStatus.FALSIFIED : AssertionStatus.NONE;
            AssertionStatus NF = negated ? AssertionStatus.NONE : AssertionStatus.FALSIFIED;

            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.ALL, ComparisonOperator.GT, "-1");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.ALL, ComparisonOperator.GE, "0");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.ALL, ComparisonOperator.LT, "10");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.ALL, ComparisonOperator.LE, "9");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.ALL, ComparisonOperator.NE, "10");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.ALL, ComparisonOperator.EQ, "5");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.ALL, ComparisonOperator.GT, "5");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.ALL, ComparisonOperator.GE, "5");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.ALL, ComparisonOperator.LT, "5");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.ALL, ComparisonOperator.LE, "5");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.ALL, ComparisonOperator.NE, "5");

            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.ANY, ComparisonOperator.EQ, "1");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.ANY, ComparisonOperator.GT, "1");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.ANY, ComparisonOperator.GE, "8");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.ANY, ComparisonOperator.LT, "8");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.ANY, ComparisonOperator.LE, "8");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.ANY, ComparisonOperator.NE, "8");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.ANY, ComparisonOperator.EQ, "15");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.ANY, ComparisonOperator.GT, "9");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.ANY, ComparisonOperator.GE, "15");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.ANY, ComparisonOperator.LT, "0");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.ANY, ComparisonOperator.LE, "-1");

            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.FIRST, ComparisonOperator.EQ, "0");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.FIRST, ComparisonOperator.GT, "-1");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.FIRST, ComparisonOperator.GE, "-1");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.FIRST, ComparisonOperator.LT, "1");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.FIRST, ComparisonOperator.LE, "1");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.FIRST, ComparisonOperator.NE, "1");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.FIRST, ComparisonOperator.EQ, "1");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.FIRST, ComparisonOperator.GT, "0");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.FIRST, ComparisonOperator.GE, "1");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.FIRST, ComparisonOperator.LT, "0");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.FIRST, ComparisonOperator.LE, "-1");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.FIRST, ComparisonOperator.NE, "0");

            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.LAST, ComparisonOperator.EQ, "9");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.LAST, ComparisonOperator.GT, "8");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.LAST, ComparisonOperator.GE, "9");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.LAST, ComparisonOperator.LT, "10");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.LAST, ComparisonOperator.LE, "9");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NN, MultivaluedComparison.LAST, ComparisonOperator.NE, "10");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.LAST, ComparisonOperator.EQ, "10");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.LAST, ComparisonOperator.GT, "9");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.LAST, ComparisonOperator.GE, "10");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.LAST, ComparisonOperator.LT, "9");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.LAST, ComparisonOperator.LE, "8");
            doTestMultivaluedComparisonForBinaryPredicate(negated, NF, MultivaluedComparison.LAST, ComparisonOperator.NE, "9");
        }
    }

    /**
     * Just validates the audits generated by comparing a known type to another known type instance.
     */
    @Test
    public void testBigInteger_Compare_Same_No_Type() throws Exception {
        testBinaryWithConversion(new BigInteger("1"), new BigInteger("1"), "Converting java.math.BigInteger value into int");
    }

    // Date tests where no DataTypePredicate is included. The comparison will default to 'Date/Time' only when
    // the type of the left object is Date or Calendar. Long will not be treated as a date/time as it treated already as an Integer type.

    @Test
    public void testDateTime_LHS_Date_RHS_Date() throws Exception {
        final long now = System.currentTimeMillis();
        testBinaryWithConversion(new Date(now), new Date(now), "Converting java.util.Date value into dateTime");
    }

    @Test
    public void testDateTime_LHS_Date_RHS_Calendar() throws Exception {
        final long now = System.currentTimeMillis();
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        testBinaryWithConversion(new Date(now), cal, "Converting java.util.GregorianCalendar value into dateTime");
    }

    @Test
    public void testDateTime_LHS_Date_RHS_Long() throws Exception {
        final long now = System.currentTimeMillis();
        testBinaryWithConversion(new Date(now), now, "Converting java.lang.Long value into dateTime");
    }

    @Test
    public void testDateTime_LHS_Date_RHS_String() throws Exception {
        final long now = System.currentTimeMillis();
        testBinaryWithConversion(new Date(now), String.valueOf(now), "Converting java.lang.String value into dateTime");
    }

    // Left hand side is Calendar

    @Test
    public void testDateTime_LHS_Calendar_RHS_Date() throws Exception {
        final long now = System.currentTimeMillis();
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);

        testBinaryWithConversion(cal, new Date(now), "Converting java.util.Date value into dateTime");
    }

    @Test
    public void testDateTime_LHS_Calendar_RHS_Calendar() throws Exception {
        final long now = System.currentTimeMillis();
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        final Calendar calRight = Calendar.getInstance();
        calRight.setTimeInMillis(now);

        testBinaryWithConversion(cal, calRight, "Converting java.util.GregorianCalendar value into dateTime");
    }

    @Test
    public void testDateTime_LHS_Calendar_RHS_Long() throws Exception {
        final long now = System.currentTimeMillis();
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);

        testBinaryWithConversion(cal, now, "Converting java.lang.Long value into dateTime");
    }

    @Test
    public void testDateTime_LHS_Calendar_RHS_String() throws Exception {
        final long now = System.currentTimeMillis();
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);

        testBinaryWithConversion(cal, String.valueOf(now), "Converting java.lang.String value into dateTime");
    }

    // Tests with a Date / Time data type predicate

    @Test
    public void testDateTime_DateType_LHS_Date_RHS_Date() throws Exception {
        final long now = System.currentTimeMillis();
        testBinaryWithConversion(new Date(now), new Date(now), DataType.DATE_TIME);
    }

    @Test
    public void testDateTime_DateType_LHS_Date_RHS_Calendar() throws Exception {
        final long now = System.currentTimeMillis();
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        testBinaryWithConversion(new Date(now), cal, "Converting java.util.GregorianCalendar value into dateTime", DataType.DATE_TIME);
    }

    // Validate that when a date/time data type predicate is used, that longs on the left are interpreted as dateTime

    /**
     * When a date/time data type predicate is specified, then a long used as either the left or the right value must
     * be converted into a Date/Time value.
     * As 'Long' is a valueClass of Date/Time and has been historically, then no 'conversion' is needed as both
     * types are assignable from a 'Date/Time' variable.
     */
    @Test
    public void testDateTime_DateType_LHS_Long_RHS_Long() throws Exception {
        final long now = System.currentTimeMillis();
        testBinaryWithConversion(now, now, DataType.DATE_TIME);
    }

    /**
     * Requires conversion of Date into a Long
     */
    @Test
    public void testDateTime_DateType_LHS_Long_RHS_Date() throws Exception {
        final long now = System.currentTimeMillis();
        testBinaryWithConversion(now, new Date(now), "Converting java.util.Date value into dateTime", DataType.DATE_TIME);
    }

    /**
     * Requires conversion of Calendar into a Long
     */
    @Test
    public void testDateTime_DateType_LHS_Long_RHS_Calendar() throws Exception {
        final long now = System.currentTimeMillis();
        final Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        testBinaryWithConversion(now, cal, "Converting java.util.GregorianCalendar value into dateTime", DataType.DATE_TIME);
    }

    /**
     * Requires conversion of Calendar into a Long
     */
    @Test
    public void testDateTime_DateType_LHS_Long_RHS_String() throws Exception {
        final long now = System.currentTimeMillis();
        testBinaryWithConversion(now, String.valueOf(now), "Converting java.lang.String value into dateTime", DataType.DATE_TIME);
    }

    /**
     * If both left and right are the same type and are off an applicable valueClass for the DataType, then no conversion
     * will be done and they will just be compared.
     *
     */
    @Test
    public void testDateTime_DateType_LHS_Long_RHS_Long_NoMatch() throws Exception {
        final long now = System.currentTimeMillis();
        // expect a fail as the two longs don't match, it's irrelevant that the right value is not a valid timestamp
        testBinaryWithConversion(now, now / 10000L, DataType.DATE_TIME, true);
    }

    @Test
    public void testDateTime_NotComparable() throws Exception {
        testBinaryWithConversion(new Date(), new BigInteger("1"), null, true);
    }

    // - PRIVATE
    private TestAudit testAudit;

    private <L, R> void testBinaryWithConversion(L left, R right, String expectedAudit) throws Exception{
        testBinaryWithConversion(left, right, expectedAudit, null, false);
    }

    private <L, R> void testBinaryWithConversion(L left, R right, @Nullable DataType requiredType) throws Exception{
        testBinaryWithConversion(left, right, null, requiredType, false);
    }

    private <L, R> void testBinaryWithConversion(L left, R right, @Nullable DataType requiredType, boolean expectFail) throws Exception{
        testBinaryWithConversion(left, right, null, requiredType, expectFail);
    }

    private <L, R> void testBinaryWithConversion(L left, R right, @Nullable String expectedAudit, @Nullable DataType requiredType) throws Exception{
        testBinaryWithConversion(left, right, expectedAudit, requiredType, false);
    }

    private <L, R> void testBinaryWithConversion(L left, R right, @Nullable String expectedAudit, @Nullable DataType requiredType, boolean expectFail) throws Exception{
        PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("left", left);
        context.setVariable("right", right);

        ComparisonAssertion comp = new ComparisonAssertion();

        comp.setExpression1("${left}");
        final BinaryPredicate binaryPredicate = new BinaryPredicate(ComparisonOperator.EQ, "${right}", false, false);
        List<Predicate> allPredicates = new ArrayList<Predicate>();
        allPredicates.add(binaryPredicate);
        if (requiredType != null) {
            allPredicates.add(0, new DataTypePredicate(requiredType));
        }

        comp.setPredicates(allPredicates.toArray(new Predicate[allPredicates.size()]));

        AssertionStatus stat = doit(context, comp);
        for (String s : testAudit) {
            System.out.println(s);
        }

        if (!expectFail) {
            assertEquals(AssertionStatus.NONE, stat);
            assertTrue(testAudit.isAuditPresent(AssertionMessages.COMPARISON_OK));
            if (expectedAudit != null) {
                assertTrue(testAudit.isAuditPresent(AssertionMessages.COMPARISON_CONVERTING));
                assertTrue(testAudit.isAuditPresentContaining(expectedAudit));
            } else {
                // if no expected conversion message, then nothing should have been converted
                // note there are cases when a conversion is not really happening, if this is 'fixed', then this case needs to be updated.
                // e.g. 'converting' a Date into a 'Date', when no data type predicate is used.
                assertFalse("No conversion audit should have been generated", testAudit.isAuditPresent(AssertionMessages.COMPARISON_CONVERTING));
            }
        } else {
            assertEquals(AssertionStatus.FALSIFIED, stat);
        }
    }

    private void doTestMultivaluedComparisonForBinaryPredicate(final boolean negated,
                                                               final AssertionStatus expectedStatus,
                                                               final MultivaluedComparison multivaluedComparison,
                                                               final ComparisonOperator operator,
                                                               final String rightValue) throws Exception {
        final Object values = new Object[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
        final PolicyEnforcementContext context = PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
        context.setVariable("asdf", values);
        final ComparisonAssertion comp = new ComparisonAssertion();
        comp.setMultivaluedComparison(multivaluedComparison);
        comp.setExpression1("${asdf}");
        comp.setPredicates(new BinaryPredicate(operator, rightValue, true, negated));
        final AssertionStatus stat = doit(context, comp);
        assertEquals("Status", expectedStatus, stat);
    }

    private AssertionStatus doit(final PolicyEnforcementContext context,
                                 final ComparisonAssertion comparisonAssertion)
            throws IOException, PolicyAssertionException {
        final ServerComparisonAssertion serverAssertion = new ServerComparisonAssertion(comparisonAssertion);
        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        return doit(context, serverAssertion);
    }


    private AssertionStatus doit(final PolicyEnforcementContext context,
                                 final ServerComparisonAssertion serverComparisonAssertion)
            throws IOException, PolicyAssertionException {
        try {
            return serverComparisonAssertion.checkRequest(context);
        } catch (AssertionStatusException e) {
            return e.getAssertionStatus();
        }
    }
}
