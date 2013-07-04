package com.l7tech.policy.assertion.ext.targetable;

import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.VariableMetadata;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

/**
 * Test CustomMessageTargetableSupport
 */
public class CustomMessageTargetableSupportTest {

    @Before
    public void setUp() throws Exception {
    }

    /**
     * Test constructor and TargetName
     */
    @Test
    public void testConstructorAndTargetName() throws Exception {
        CustomMessageTargetableSupport customMessageTargetableSupport = new CustomMessageTargetableSupport();
        assertEquals("Default constructor sets target message to Request", customMessageTargetableSupport.getTargetMessageVariable(), "request");
        assertEquals("Default constructor sets target message to Request", customMessageTargetableSupport.getTargetName(), "Request");

        customMessageTargetableSupport = new CustomMessageTargetableSupport("request");
        assertEquals("Construct with target Request", customMessageTargetableSupport.getTargetMessageVariable(), "request");
        assertEquals("Construct with target Request", customMessageTargetableSupport.getTargetName(), "Request");

        customMessageTargetableSupport = new CustomMessageTargetableSupport("REqUest");
        assertEquals("Target Request is case insensitive", customMessageTargetableSupport.getTargetMessageVariable(), "REqUest");
        assertEquals("Target Request is case insensitive", customMessageTargetableSupport.getTargetName(), "Request");

        customMessageTargetableSupport = new CustomMessageTargetableSupport("response");
        assertEquals("Construct with target Response", customMessageTargetableSupport.getTargetMessageVariable(), "response");
        assertEquals("Construct with target Response", customMessageTargetableSupport.getTargetName(), "Response");

        customMessageTargetableSupport = new CustomMessageTargetableSupport("REspoNSe");
        assertEquals("Target Response is case insensitive", customMessageTargetableSupport.getTargetMessageVariable(), "REspoNSe");
        assertEquals("Target Response is case insensitive", customMessageTargetableSupport.getTargetName(), "Response");

        customMessageTargetableSupport = new CustomMessageTargetableSupport("testVar");
        assertEquals("Construct with target test variable", customMessageTargetableSupport.getTargetMessageVariable(), "testVar");
        assertEquals("Construct with target test variable", customMessageTargetableSupport.getTargetName(), "${testVar}");

        customMessageTargetableSupport = new CustomMessageTargetableSupport(true);
        assertEquals("Construct with target Request", customMessageTargetableSupport.getTargetMessageVariable(), "request");
        assertEquals("Construct with target Request", customMessageTargetableSupport.getTargetName(), "Request");
        assertEquals("Construct with TargetModifiedByGateway set to true", customMessageTargetableSupport.isTargetModifiedByGateway(), true);
    }

    /**
     * Test most of the properties:
     *  VariablesSet,
     *  VariablesUsed,
     *  isTargetModifiedByGateway,
     *  isSourceUsedByGateway,
     *  TargetMessageVariable
     *
     */
    @Test
    public void testProperties() throws Exception {
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Test for Request
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        CustomMessageTargetableSupport customMessageTargetableSupport = new CustomMessageTargetableSupport();
        assertFalse("Request: isTargetModifiedByGateway(false)", customMessageTargetableSupport.isTargetModifiedByGateway());
        assertTrue("Request: isSourceUsedByGateway(true)", customMessageTargetableSupport.isSourceUsedByGateway());
        assertTrue("For Request VariablesSet is empty", customMessageTargetableSupport.getVariablesSet().length == 0);
        assertTrue("For Request VariablesUsed is empty", customMessageTargetableSupport.getVariablesUsed().length == 0);

        customMessageTargetableSupport.setTargetMessageVariable("request");
        customMessageTargetableSupport.setTargetModifiedByGateway(true);
        customMessageTargetableSupport.setSourceUsedByGateway(true);
        assertTrue("Request: isTargetModifiedByGateway(true)", customMessageTargetableSupport.isTargetModifiedByGateway());
        assertTrue("Request: isSourceUsedByGateway(true)", customMessageTargetableSupport.isSourceUsedByGateway());
        assertTrue("For Request VariablesSet is empty", customMessageTargetableSupport.getVariablesSet().length == 0);
        assertTrue("For Request VariablesUsed is empty", customMessageTargetableSupport.getVariablesUsed().length == 0);

        customMessageTargetableSupport.setTargetMessageVariable("request");
        customMessageTargetableSupport.setTargetModifiedByGateway(false);
        customMessageTargetableSupport.setSourceUsedByGateway(false);
        assertFalse("Request: isTargetModifiedByGateway(false)", customMessageTargetableSupport.isTargetModifiedByGateway());
        assertFalse("Request: isSourceUsedByGateway(false)", customMessageTargetableSupport.isSourceUsedByGateway());
        assertTrue("For Request VariablesSet is empty", customMessageTargetableSupport.getVariablesSet().length == 0);
        assertTrue("For Request VariablesUsed is empty", customMessageTargetableSupport.getVariablesUsed().length == 0);

        customMessageTargetableSupport.setTargetMessageVariable("reQuest");
        customMessageTargetableSupport.setTargetModifiedByGateway(true);
        customMessageTargetableSupport.setSourceUsedByGateway(false);
        assertTrue("Request: isTargetModifiedByGateway(true)", customMessageTargetableSupport.isTargetModifiedByGateway());
        assertFalse("Request: isSourceUsedByGateway(false)", customMessageTargetableSupport.isSourceUsedByGateway());
        assertTrue("For Request VariablesSet is empty", customMessageTargetableSupport.getVariablesSet().length == 0);
        assertTrue("For Request VariablesUsed is empty", customMessageTargetableSupport.getVariablesUsed().length == 0);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Test for Response
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        customMessageTargetableSupport.setTargetMessageVariable("response");
        customMessageTargetableSupport.setTargetModifiedByGateway(false);
        customMessageTargetableSupport.setSourceUsedByGateway(true);
        assertFalse("Response: isTargetModifiedByGateway(false)", customMessageTargetableSupport.isTargetModifiedByGateway());
        assertTrue("Response: isSourceUsedByGateway(true)", customMessageTargetableSupport.isSourceUsedByGateway());
        assertTrue("For Response VariablesSet is empty", customMessageTargetableSupport.getVariablesSet().length == 0);
        assertTrue("For Response VariablesUsed is empty", customMessageTargetableSupport.getVariablesUsed().length == 0);

        customMessageTargetableSupport.setTargetMessageVariable("response");
        customMessageTargetableSupport.setTargetModifiedByGateway(true);
        customMessageTargetableSupport.setSourceUsedByGateway(true);
        assertTrue("Response: isTargetModifiedByGateway(true)", customMessageTargetableSupport.isTargetModifiedByGateway());
        assertTrue("Response: isSourceUsedByGateway(true)", customMessageTargetableSupport.isSourceUsedByGateway());
        assertTrue("For Response VariablesSet is empty", customMessageTargetableSupport.getVariablesSet().length == 0);
        assertTrue("For Response VariablesUsed is empty", customMessageTargetableSupport.getVariablesUsed().length == 0);

        customMessageTargetableSupport.setTargetMessageVariable("response");
        customMessageTargetableSupport.setTargetModifiedByGateway(false);
        customMessageTargetableSupport.setSourceUsedByGateway(false);
        assertFalse("Response: isTargetModifiedByGateway(false)", customMessageTargetableSupport.isTargetModifiedByGateway());
        assertFalse("Response: isSourceUsedByGateway(false)", customMessageTargetableSupport.isSourceUsedByGateway());
        assertTrue("For Response VariablesSet is empty", customMessageTargetableSupport.getVariablesSet().length == 0);
        assertTrue("For Response VariablesUsed is empty", customMessageTargetableSupport.getVariablesUsed().length == 0);

        customMessageTargetableSupport.setTargetMessageVariable("respoNse");
        customMessageTargetableSupport.setTargetModifiedByGateway(true);
        customMessageTargetableSupport.setSourceUsedByGateway(false);
        assertTrue("Response: isTargetModifiedByGateway(true)", customMessageTargetableSupport.isTargetModifiedByGateway());
        assertFalse("Response: isSourceUsedByGateway(false)", customMessageTargetableSupport.isSourceUsedByGateway());
        assertTrue("For Response VariablesSet is empty", customMessageTargetableSupport.getVariablesSet().length == 0);
        assertTrue("For Response VariablesUsed is empty", customMessageTargetableSupport.getVariablesUsed().length == 0);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Test for Message Target other then Request or Response
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // false, true
        customMessageTargetableSupport.setTargetMessageVariable("testVar");
        customMessageTargetableSupport.setTargetModifiedByGateway(false);
        customMessageTargetableSupport.setSourceUsedByGateway(true);
        assertFalse("Message Target other then Request or Response: isTargetModifiedByGateway(false)", customMessageTargetableSupport.isTargetModifiedByGateway());
        assertTrue("Message Target other then Request or Response: isSourceUsedByGateway(true)", customMessageTargetableSupport.isSourceUsedByGateway());
        assertTrue("For Message Target other then Request or Response, isTargetModifiedByGateway(false), isSourceUsedByGateway(true), VariablesSet is empty",
                customMessageTargetableSupport.getVariablesSet().length == 0);
        assertTrue("For Message Target other then Request or Response, VariablesUsed is not empty",
                customMessageTargetableSupport.getVariablesUsed().length == 1);
        assertEquals("For Message Target other then Request or Response, VariablesUsed is properly set",
                customMessageTargetableSupport.getVariablesUsed()[0], "testVar");

        // true, true
        customMessageTargetableSupport.setTargetMessageVariable("testVar");
        customMessageTargetableSupport.setTargetModifiedByGateway(true);
        customMessageTargetableSupport.setSourceUsedByGateway(true);
        assertTrue("Message Target other then Request or Response: isTargetModifiedByGateway(true)", customMessageTargetableSupport.isTargetModifiedByGateway());
        assertTrue("Message Target other then Request or Response: isSourceUsedByGateway(true)", customMessageTargetableSupport.isSourceUsedByGateway());
        assertTrue("For Message Target other then Request or Response, isTargetModifiedByGateway(true), isSourceUsedByGateway(true), VariablesSet is not empty",
                customMessageTargetableSupport.getVariablesSet().length == 1);
        assertTrue("For Message Target other then Request or Response, isTargetModifiedByGateway(true), isSourceUsedByGateway(true), VariablesSet is properly set",
                compareVariableMetaData(customMessageTargetableSupport.getVariablesSet()[0],
                        new VariableMetadata("testVal", false, false, null, true, DataType.MESSAGE)));
        assertTrue("For Message Target other then Request or Response, isTargetModifiedByGateway(true), isSourceUsedByGateway(true), VariablesUsed is not empty",
                customMessageTargetableSupport.getVariablesUsed().length == 1);
        assertEquals("For Message Target other then Request or Response, VariablesUsed is properly set",
                customMessageTargetableSupport.getVariablesUsed()[0], "testVar");

        // true, false
        customMessageTargetableSupport.setTargetMessageVariable("testVar");
        customMessageTargetableSupport.setTargetModifiedByGateway(true);
        customMessageTargetableSupport.setSourceUsedByGateway(false);
        assertTrue("Message Target other then Request or Response: isTargetModifiedByGateway(true)", customMessageTargetableSupport.isTargetModifiedByGateway());
        assertFalse("Message Target other then Request or Response: isSourceUsedByGateway(false)", customMessageTargetableSupport.isSourceUsedByGateway());
        assertTrue("For Message Target other then Request or Response, isTargetModifiedByGateway(true), isSourceUsedByGateway(false), VariablesSet is not empty",
                customMessageTargetableSupport.getVariablesSet().length == 1);
        assertTrue("For Message Target other then Request or Response, isTargetModifiedByGateway(true), isSourceUsedByGateway(false), VariablesSet is properly set",
                compareVariableMetaData(customMessageTargetableSupport.getVariablesSet()[0],
                        new VariableMetadata("testVal", false, false, null, true, DataType.MESSAGE)));
        assertTrue("For Message Target other then Request or Response, isTargetModifiedByGateway(true), isSourceUsedByGateway(false), VariablesUsed is empty",
                customMessageTargetableSupport.getVariablesUsed().length == 0);

        // false, false
        customMessageTargetableSupport.setTargetMessageVariable("testVar");
        customMessageTargetableSupport.setTargetModifiedByGateway(false);
        customMessageTargetableSupport.setSourceUsedByGateway(false);
        assertFalse("Message Target other then Request or Response: isTargetModifiedByGateway(false)", customMessageTargetableSupport.isTargetModifiedByGateway());
        assertFalse("Message Target other then Request or Response: isSourceUsedByGateway(false)", customMessageTargetableSupport.isSourceUsedByGateway());
        assertTrue("Message Target other then Request or Response, isTargetModifiedByGateway(false), isSourceUsedByGateway(false), VariablesSet is empty",
                customMessageTargetableSupport.getVariablesSet().length == 0);
        assertTrue("Message Target other then Request or Response, isTargetModifiedByGateway(false), isSourceUsedByGateway(false), VariablesUsed is empty",
                customMessageTargetableSupport.getVariablesUsed().length == 0);
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    @Test
    public void testNullTargetMessage() throws Exception {
        CustomMessageTargetableSupport customMessageTargetableSupport = new CustomMessageTargetableSupport();
        customMessageTargetableSupport.setTargetMessageVariable("");
        customMessageTargetableSupport.setTargetModifiedByGateway(true);
        customMessageTargetableSupport.setSourceUsedByGateway(true);

        assertTrue(customMessageTargetableSupport.getVariablesSet().length == 0);
        assertTrue(customMessageTargetableSupport.getVariablesUsed().length == 0);
        assertNull(customMessageTargetableSupport.getTargetName());
        assertNull(customMessageTargetableSupport.getTargetMessageVariable());
    }

    // helper function
    private boolean compareVariableMetaData(final VariableMetadata first, final VariableMetadata second) {
        //noinspection StringEquality
        return (first == second ||
                (first.getName().equals(second.getName()) &&
                 first.isPrefixed() == second.isPrefixed() &&
                 first.isMultivalued() == second.isMultivalued() &&
                 first.getCanonicalName().equals(second.getCanonicalName()) &&
                 first.isSettable() == second.isSettable() &&
                 first.getType().equals(second.getType()) &&
                 first.getReplacedBy() == null || second.getReplacedBy() == null ? first.getReplacedBy() == second.getReplacedBy() : first.getReplacedBy().equals(second.getReplacedBy())));
    }

    @Test
    public void testSetTargetMessage() throws Exception
    {
        class TestMessageTargetable implements CustomMessageTargetable {
            final CustomMessageTargetableSupport support;

            public TestMessageTargetable(boolean targetModifiedByGateway, boolean sourceUsedByGateway) {
                support = new CustomMessageTargetableSupport("testVar1");
                support.setTargetModifiedByGateway(targetModifiedByGateway);
                support.setSourceUsedByGateway(sourceUsedByGateway);
            }

            public String getTargetMessageVariable() { return support.getTargetMessageVariable(); }
            public void setTargetMessageVariable(String otherMessageVariable) { support.setTargetMessageVariable(otherMessageVariable); }
            public String getTargetName() { return support.getTargetName(); }
            public boolean isTargetModifiedByGateway() { return support.isTargetModifiedByGateway(); }
            public VariableMetadata[] getVariablesSet() { return support.getVariablesSet(); }
            public String[] getVariablesUsed() { return support.getVariablesUsed(); }
            public CustomMessageTargetableSupport getSupport() { return support; }
        }

        TestMessageTargetable targetable = new TestMessageTargetable(true, false);
        CustomMessageTargetableSupport customMessageTargetableSupport = new CustomMessageTargetableSupport(targetable);

        assertThat(targetable.getSupport(), not(equalTo(customMessageTargetableSupport)));
        assertEquals(targetable.getTargetMessageVariable(), customMessageTargetableSupport.getTargetMessageVariable());
        assertEquals(targetable.isTargetModifiedByGateway(), customMessageTargetableSupport.isTargetModifiedByGateway());
        assertTrue(targetable.getVariablesSet().length == 1);
        assertTrue(targetable.getVariablesSet().length == customMessageTargetableSupport.getVariablesSet().length);
        assertTrue(compareVariableMetaData(targetable.getVariablesSet()[0], customMessageTargetableSupport.getVariablesSet()[0]));
        assertFalse(Arrays.equals(targetable.getVariablesUsed(), customMessageTargetableSupport.getVariablesUsed()));

        targetable = new TestMessageTargetable(true, true);
        customMessageTargetableSupport = new CustomMessageTargetableSupport(targetable);
        assertEquals(targetable.getSupport(), customMessageTargetableSupport);
        assertEquals(targetable.getTargetMessageVariable(), customMessageTargetableSupport.getTargetMessageVariable());
        assertEquals(targetable.isTargetModifiedByGateway(), customMessageTargetableSupport.isTargetModifiedByGateway());
        assertTrue(targetable.getVariablesSet().length == 1);
        assertTrue(targetable.getVariablesSet().length == customMessageTargetableSupport.getVariablesSet().length);
        assertTrue(compareVariableMetaData(targetable.getVariablesSet()[0], customMessageTargetableSupport.getVariablesSet()[0]));
        assertTrue(Arrays.equals(targetable.getVariablesUsed(), customMessageTargetableSupport.getVariablesUsed()));

        targetable = new TestMessageTargetable(false, true);
        customMessageTargetableSupport = new CustomMessageTargetableSupport(targetable);
        assertEquals(targetable.getSupport(), customMessageTargetableSupport);
        assertEquals(targetable.getTargetMessageVariable(), customMessageTargetableSupport.getTargetMessageVariable());
        assertEquals(targetable.isTargetModifiedByGateway(), customMessageTargetableSupport.isTargetModifiedByGateway());
        assertTrue(targetable.getVariablesSet().length == 0);
        assertTrue(targetable.getVariablesSet().length == customMessageTargetableSupport.getVariablesSet().length);
        assertTrue(Arrays.equals(targetable.getVariablesUsed(), customMessageTargetableSupport.getVariablesUsed()));

        targetable = new TestMessageTargetable(false, false);
        customMessageTargetableSupport = new CustomMessageTargetableSupport(targetable);
        assertThat(targetable.getSupport(), not(equalTo(customMessageTargetableSupport)));
        assertEquals(targetable.getTargetMessageVariable(), customMessageTargetableSupport.getTargetMessageVariable());
        assertEquals(targetable.isTargetModifiedByGateway(), customMessageTargetableSupport.isTargetModifiedByGateway());
        assertTrue(targetable.getVariablesSet().length == 0);
        assertTrue(targetable.getVariablesSet().length == customMessageTargetableSupport.getVariablesSet().length);
        assertFalse(Arrays.equals(targetable.getVariablesUsed(), customMessageTargetableSupport.getVariablesUsed()));
    }

    @Test
    public void testEquals() throws Exception {
        CustomMessageTargetableSupport obj1 = new CustomMessageTargetableSupport();
        CustomMessageTargetableSupport obj2 = new CustomMessageTargetableSupport("reQuest");
        assertEquals(obj1, obj2);

        obj1.setTargetMessageVariable("resPonsE");
        obj2.setTargetMessageVariable("RESPONSE");
        obj1.setTargetModifiedByGateway(true);
        obj2.setTargetModifiedByGateway(true);
        obj1.setSourceUsedByGateway(true);
        obj2.setSourceUsedByGateway(true);
        assertEquals(obj1, obj2);

        obj1.setTargetMessageVariable("resPonsE");
        obj2.setTargetMessageVariable("RESPONSE1");
        obj1.setTargetModifiedByGateway(true);
        obj2.setTargetModifiedByGateway(true);
        obj1.setSourceUsedByGateway(true);
        obj2.setSourceUsedByGateway(true);
        assertThat(obj1, not(equalTo(obj2)));

        obj1.setTargetMessageVariable("testVal");
        obj2.setTargetMessageVariable("testVal");
        obj1.setTargetModifiedByGateway(true);
        obj2.setTargetModifiedByGateway(true);
        obj1.setSourceUsedByGateway(true);
        obj2.setSourceUsedByGateway(true);
        assertEquals(obj1, obj2);

        obj1.setTargetMessageVariable("testVal");
        obj2.setTargetMessageVariable("TestVal");
        obj1.setTargetModifiedByGateway(true);
        obj2.setTargetModifiedByGateway(true);
        obj1.setSourceUsedByGateway(true);
        obj2.setSourceUsedByGateway(true);
        assertThat(obj1, not(equalTo(obj2)));

        obj1.setTargetMessageVariable("resPonsE");
        obj2.setTargetMessageVariable("RESPONSE");
        obj1.setTargetModifiedByGateway(true);
        obj2.setTargetModifiedByGateway(false);
        obj1.setSourceUsedByGateway(true);
        obj2.setSourceUsedByGateway(true);
        assertThat(obj1, not(equalTo(obj2)));

        obj1.setTargetMessageVariable("resPonsE");
        obj2.setTargetMessageVariable("RESPONSE");
        obj1.setTargetModifiedByGateway(true);
        obj2.setTargetModifiedByGateway(true);
        obj1.setSourceUsedByGateway(true);
        obj2.setSourceUsedByGateway(false);
        assertThat(obj1, not(equalTo(obj2)));

        assertEquals(obj1, obj1);
        assertEquals(obj2, obj2);
    }
}
