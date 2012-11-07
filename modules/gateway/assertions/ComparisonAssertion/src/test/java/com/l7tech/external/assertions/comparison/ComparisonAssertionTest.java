package com.l7tech.external.assertions.comparison;

import com.l7tech.external.assertions.comparison.server.convert.ValueConverter;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ComparisonOperator;
import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

import com.l7tech.util.Functions;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;

public class ComparisonAssertionTest {
    @Test
    public void testSerialization() throws Exception {
        AssertionRegistry assreg = new AssertionRegistry();
        assreg.registerAssertion(ComparisonAssertion.class);
        WspConstants.setTypeMappingFinder(assreg);
        Assertion ass = WspReader.getDefault().parseStrictly(PRE_FANGTOOTH_POLICY_XML, WspReader.INCLUDE_DISABLED);

        String xml = WspWriter.getPolicyXml(ass);
        assertEquals(xml, PRE_FANGTOOTH_POLICY_XML);
    }

    @Test
    public void testSerializationBackwardsCompatibility() throws Exception {
        AssertionRegistry assreg = new AssertionRegistry();
        assreg.registerAssertion(ComparisonAssertion.class);
        WspConstants.setTypeMappingFinder(assreg);
        Assertion ass = WspReader.getDefault().parseStrictly(PRE_FANGTOOTH_POLICY_XML, WspReader.INCLUDE_DISABLED);

        String xml = WspWriter.getPolicyXml(ass);
        assertEquals(xml, PRE_FANGTOOTH_POLICY_XML);

        AllAssertion allAss = (AllAssertion) ass;
        final ComparisonAssertion assertion = (ComparisonAssertion) allAss.getChildren().get(0);

        // default value of failIfVariableNotFound switch should true to maintain backwards compatibility
        assertEquals(true, assertion.isFailIfVariableNotFound());
    }

    /**
     * Tests that required resource bundle keys are present
     */
    @Test
    public void testPredicates() {
        for ( ComparisonOperator op : ComparisonOperator.values() ) {
            String right =  op.isUnary() ? null : "right";
            System.out.println(new BinaryPredicate(op, right, true, true).toString());
            System.out.println(new BinaryPredicate(op, right, true, false).toString());
            System.out.println(new BinaryPredicate(op, right, false, true).toString());
            System.out.println(new BinaryPredicate(op, right, false, false).toString());
        }
    }

    @Test
    public void testClone() {
        final ComparisonAssertion ca = new ComparisonAssertion();
        ca.setExpression1( "expression goes here" );
        ca.setFailIfVariableNotFound(true);
        ca.setMultivaluedComparison( MultivaluedComparison.ANY );
        ca.setPredicates(new EmptyPredicate());

        final ComparisonAssertion cloned = ca.clone();
        assertEquals( "Expression", "expression goes here", cloned.getExpression1() );
        assertEquals( "MultivaluedComparison", MultivaluedComparison.ANY, cloned.getMultivaluedComparison() );
        assertEquals( "Predicates length", 1L, (long) cloned.getPredicates().length );
        assertEquals( "Fail if variable not found", true, cloned.isFailIfVariableNotFound());
        assertNotSame( "Predicates array copied", ca.getPredicates(), cloned.getPredicates() );
        assertNotSame( "Predicates copied", ca.getPredicates()[0], cloned.getPredicates()[0] );
    }

    @Test
    public void testResources() {
        CollectionUtils.foreach( EnumSet.allOf(MultivaluedComparison.class), false, new Functions.UnaryVoid<MultivaluedComparison>(){
            @Override
            public void call( final MultivaluedComparison multivaluedComparison ) {
                ComparisonAssertion.resources.getString( "multivaluedComparison."+multivaluedComparison+".label" );
            }
        } );
    }

    /**
     * It must be possible to convert between all types listed in the valueClasses property for a DataType.
     */
    @Test
    public void testValueClassesForEachDataTypes() throws Exception {
        final List<DataType> dataTypes = ComparisonAssertion.DATA_TYPES;
        for (DataType dataType : dataTypes) {
            if (dataType == DataType.UNKNOWN) {
                continue;
            }

            final Class[] valueClasses = dataType.getValueClasses();
            for (Class valueClass : valueClasses) {
                final Object o = getInstance(valueClass);
                final ValueConverter converter = ValueConverter.Factory.getConverterOrHelperConverter((Comparable) o, dataType);
                assertNotNull("Unknown type '" + valueClass.getName() +
                "'. Support must be added for a DataType and all it's valueClasses before it can be added to the comparison assertions list of supported types.",
                        converter);
            }
        }
    }

    private Object getInstance(Class clazz) throws Exception {
        try {
            final Constructor declaredConstructor = clazz.getConstructor();
            return declaredConstructor.newInstance();
        } catch (NoSuchMethodException e) {
            // Update this list as new types are added which don't have a default public constructor
            if (clazz == BigInteger.class) {
                return new BigInteger("1");
            } else if (clazz == Calendar.class) {
                return Calendar.getInstance();
            } else if (clazz == BigDecimal.class) {
                return new BigDecimal("1.0");
            } else if (clazz == Boolean.class) {
                return new Boolean(true);
            } else if (clazz == Boolean.TYPE) {
                return true;
            } else if (clazz == Long.class) {
                return new Long(1L);
            } else if (clazz == Long.TYPE) {
                return 1L;
            }
        }

        return null;
    }

    public static final String PRE_FANGTOOTH_POLICY_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:ComparisonAssertion>\n" +
            "            <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
            "            <L7p:Expression1 stringValue=\"${blargle}\"/>\n" +
            "            <L7p:Operator operatorNull=\"null\"/>\n" +
            "            <L7p:Predicates predicates=\"included\">\n" +
            "                <L7p:item binary=\"included\">\n" +
            "                    <L7p:CaseSensitive booleanValue=\"false\"/>\n" +
            "                    <L7p:Negated booleanValue=\"true\"/>\n" +
            "                    <L7p:Operator operator=\"GT\"/>\n" +
            "                    <L7p:RightValue stringValue=\"blah\"/>\n" +
            "                </L7p:item>\n" +
            "                <L7p:item cardinality=\"included\">\n" +
            "                    <L7p:Max intValue=\"15\"/>\n" +
            "                    <L7p:Min intValue=\"4\"/>\n" +
            "                </L7p:item>\n" +
            "                <L7p:item regex=\"included\">\n" +
            "                    <L7p:Pattern stringValue=\"f.*$\"/>\n" +
            "                </L7p:item>\n" +
            "                <L7p:item stringLength=\"included\">\n" +
            "                    <L7p:Max intValue=\"44454\"/>\n" +
            "                    <L7p:Min intValue=\"5\"/>\n" +
            "                </L7p:item>\n" +
            "                <L7p:item binary=\"included\">\n" +
            "                    <L7p:Negated booleanValue=\"true\"/>\n" +
            "                    <L7p:Operator operator=\"GE\"/>\n" +
            "                    <L7p:RightValue stringValue=\"343\"/>\n" +
            "                </L7p:item>\n" +
            "            </L7p:Predicates>\n" +
            "        </L7p:ComparisonAssertion>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";
}
