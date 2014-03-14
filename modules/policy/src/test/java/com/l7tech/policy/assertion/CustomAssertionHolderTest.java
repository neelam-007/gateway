/*
 * Copyright (C) 2013 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.assertion;

import com.l7tech.policy.TestCustomMessageTargetable;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.CustomCredentialSource;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static com.l7tech.policy.assertion.AssertionMetadata.*;
import static com.l7tech.policy.assertion.CustomAssertionHolder.CUSTOM_ASSERTION;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CustomAssertionHolderTest {
    private static final String DESC = "My Custom Assertion Description";
    private static final String NAME = "My Custom Assertion";
    private static final String CUSTOM_ASSERTION_LEFT_COMMENT = "Custom Assertion Left Comment";
    private static final String CUSTOM_ASSERTION_RIGHT_COMMENT = "Custom Assertion Left Comment";

    @SuppressWarnings("serial")
    private static final Map TEST_CA_MAP_ELEMENTS = new HashMap<String, Integer>() {
        {
            put("one", 1);
            put("two", 2);
            put("three", 3);
            put("four", 4);
        }
    };
    private CustomAssertionHolder holder;

    @Before
    public void setup() throws Exception {
        holder = new CustomAssertionHolder();
        holder.setCustomAssertion(new StubCustomAssertion());
        holder.setDescriptionText(DESC);
    }

    @Test
    public void meta() {
        final AssertionMetadata meta = holder.meta();
        assertEquals(NAME, meta.get(SHORT_NAME));
        assertEquals(DESC, meta.get(DESCRIPTION));
    }

    @Test
    public void metaNullCustomAssertion() {
        holder.setCustomAssertion(null);
        final AssertionMetadata meta = holder.meta();
        assertEquals(CUSTOM_ASSERTION, meta.get(SHORT_NAME));
    }

    @Test
    public void metaNullDescription() {
        holder.setDescriptionText(null);
        final AssertionMetadata meta = holder.meta();
        assertEquals(CUSTOM_ASSERTION, meta.get(DESCRIPTION));
    }

    @Test
    public void testNodeNames() {
        holder.setPolicyNodeName("Policy Node Name Demo");
        final AssertionNodeNameFactory nameFactory = holder.meta().get(POLICY_NODE_NAME_FACTORY);
        assertEquals("Policy Node Name Demo", nameFactory.getAssertionName(holder, true));
    }

    @Test
    public void testSetIsUiAutoOpen() {
        Assertion.clearCachedMetadata(holder.getClass().getName());
        holder.setIsUiAutoOpen(true);
        String adviceClassName = holder.meta().get(POLICY_ADVICE_CLASSNAME);
        assertEquals("com.l7tech.console.tree.policy.advice.CustomAssertionHolderAdvice", adviceClassName);
    }

    @Test
    public void testUnsetIsUiAutoOpen() {
        Assertion.clearCachedMetadata(holder.getClass().getName());
        holder.setIsUiAutoOpen(false);
        String adviceClassName = holder.meta().get(POLICY_ADVICE_CLASSNAME);
        assertNotSame("com.l7tech.console.tree.policy.advice.CustomAssertionHolderAdvice", adviceClassName);
    }

    @SuppressWarnings("serial")
    private class StubCustomAssertion implements CustomAssertion {
        @Override
        public String getName() {
            return NAME;
        }
    }

    /**
     * target property for legacy assertion should be read-only i.e. always REQUEST
     */
    @Test
    public void targetLegacy() {
        holder.setTarget(TargetMessageType.REQUEST);
        assertEquals("target property for legacy assertion is read-only i.e. always REQUEST",
                holder.getTarget(), TargetMessageType.REQUEST);

        holder.setTarget(TargetMessageType.RESPONSE);
        assertEquals("target property for legacy assertion is read-only i.e. always REQUEST",
                holder.getTarget(), TargetMessageType.REQUEST);

        holder.setTarget(TargetMessageType.OTHER);
        assertEquals("target property for legacy assertion is read-only i.e. always REQUEST",
                holder.getTarget(), TargetMessageType.REQUEST);
    }

    @Test
    public void targetMessageTargetable() {
        holder.setCustomAssertion(new TestCustomMessageTargetable("test1", 1, true, TEST_CA_MAP_ELEMENTS));

        TargetMessageType tmType = TargetMessageType.REQUEST;
        holder.setTarget(tmType);
        assertEquals("Failed to set TargetMessageType to '" + tmType.toString() + "'",
                holder.getTarget(), tmType);

        tmType = TargetMessageType.RESPONSE;
        holder.setTarget(tmType);
        assertEquals("Failed to set TargetMessageType to '" + tmType.toString() + "'",
                holder.getTarget(), tmType);

        tmType = TargetMessageType.OTHER;
        holder.setTarget(tmType);
        assertEquals("Failed to set TargetMessageType to '" + tmType.toString() + "'",
                holder.getTarget(), tmType);
    }

    /**
     * otherTargetMessageVariable property for legacy assertion is read-only i.e. always null
     */
    @Test
    public void otherTargetMessageVariableLegacy() {
        holder.setOtherTargetMessageVariable("test123");
        assertEquals("otherTargetMessageVariable property for legacy assertion is read-only i.e. always null",
                null, holder.getOtherTargetMessageVariable());
    }

    @Test
    public void otherTargetMessageVariableMessageTargetable() {
        holder.setCustomAssertion(new TestCustomMessageTargetable("test1", 1, true, TEST_CA_MAP_ELEMENTS));

        final String otherTargetMessageValue = "test123";
        holder.setOtherTargetMessageVariable(otherTargetMessageValue);
        assertEquals("Test to set OtherTargetMessageVariable to '" + otherTargetMessageValue + "'",
                otherTargetMessageValue, holder.getOtherTargetMessageVariable());
    }

    /**
     * targetName property for legacy assertion is read-only i.e. always null
     */
    @Test
    public void targetNameLegacy() {
        holder.setTarget(TargetMessageType.REQUEST);
        assertEquals("targetName property for legacy assertion is read-only i.e. always null",
                null, holder.getTargetName());

        holder.setTarget(TargetMessageType.RESPONSE);
        assertEquals("targetName property for legacy assertion is read-only i.e. always null",
                null, holder.getTargetName());

        holder.setTarget(TargetMessageType.OTHER);
        assertEquals("targetName property for legacy assertion is read-only i.e. always null",
                null, holder.getTargetName());

        holder.setTarget(TargetMessageType.OTHER);
        holder.setOtherTargetMessageVariable("test123");
        assertEquals("targetName property for legacy assertion is read-only i.e. always null",
                null, holder.getTargetName());
    }

    @Test
    public void targetNameMessageTargetable() {
        holder.setCustomAssertion(new TestCustomMessageTargetable("test1", 1, true, TEST_CA_MAP_ELEMENTS));

        holder.setTarget(TargetMessageType.REQUEST);
        assertEquals("Test getTargetName with REQUEST",
                "Request", holder.getTargetName());

        holder.setTarget(TargetMessageType.RESPONSE);
        assertEquals("Test getTargetName with RESPONSE",
                "Response", holder.getTargetName());

        holder.setTarget(TargetMessageType.OTHER);
        assertNull("Test getTargetName without other target message variable set", holder.getTargetName());

        holder.setTarget(TargetMessageType.OTHER);
        final String otherTargetMessageValue = "test123";
        holder.setOtherTargetMessageVariable(otherTargetMessageValue);
        assertEquals("Test getTargetName with other target message variable set",
                "${" + otherTargetMessageValue + "}",
                holder.getTargetName());
    }

    @Test
    public void assertionClone() throws Exception {
        final CustomAssertionHolder assertion = holder;

        assertion.setCustomAssertion(new TestCustomMessageTargetable("param one", 1, true, TEST_CA_MAP_ELEMENTS));
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setOtherTargetMessageVariable("other message variable");
        assertion.setDescriptionText(DESC);
        assertion.setCategories(Category.LOGIC, Category.MESSAGE);
        assertion.setEnabled(false);

        // add left and right comment
        final Assertion.Comment comment = new Assertion.Comment();
        comment.setComment("left comment", Assertion.Comment.LEFT_COMMENT);
        comment.setComment("right comment", Assertion.Comment.RIGHT_COMMENT);
        assertion.setAssertionComment(comment);

        // Do the cloning
        final Assertion copy = assertion.getCopy();
        assertTrue("copy is of CustomAssertionHolder type", copy instanceof CustomAssertionHolder);
        final CustomAssertionHolder cloneAssertion = (CustomAssertionHolder)copy;

        // test Custom Assertion
        assertEquals("Custom Assertions are equal",
                assertion.getCustomAssertion(), cloneAssertion.getCustomAssertion());
        assertNotSame("Custom Assertions have different references",
                assertion.getCustomAssertion(), cloneAssertion.getCustomAssertion());
        CustomAssertion customAssertion = cloneAssertion.getCustomAssertion();
        cloneAssertion.setCustomAssertion(new TestCustomMessageTargetable());
        assertThat("Custom Assertions are different",
                assertion.getCustomAssertion(), not(equalTo(cloneAssertion.getCustomAssertion())));

        // set it back
        cloneAssertion.setCustomAssertion(customAssertion);
        assertEquals("Custom Assertions are equal",
                assertion.getCustomAssertion(), cloneAssertion.getCustomAssertion());
        assertNotSame("Custom Assertions have different references",
                assertion.getCustomAssertion(), cloneAssertion.getCustomAssertion());

        // test target
        assertEquals("Target is the same",
                assertion.getTarget(), cloneAssertion.getTarget());
        // make sure there is no reference copy
        cloneAssertion.setTarget(TargetMessageType.REQUEST);
        assertThat("Target is different",
                assertion.getTarget(), not(equalTo(cloneAssertion.getTarget())));

        // test OtherTargetMessageVariable
        cloneAssertion.setOtherTargetMessageVariable("other message variable change");
        assertThat("OtherTargetMessageVariable is different",
                assertion.getOtherTargetMessageVariable(), not(equalTo(cloneAssertion.getOtherTargetMessageVariable())));

        // test Description Text
        assertEquals("Description is the same",
                assertion.getDescriptionText(), cloneAssertion.getDescriptionText());
        cloneAssertion.setDescriptionText(DESC + "_CLONE_");
        assertThat("Description is different",
                assertion.getDescriptionText(), not(equalTo(cloneAssertion.getDescriptionText())));

        // test Category
        assertEquals("Categories are the same",
                assertion.getCategories(), cloneAssertion.getCategories());
        cloneAssertion.setCategories(Category.MESSAGE);
        assertThat("Categories are different",
                assertion.getCategories(), not(equalTo(cloneAssertion.getCategories())));

        // test isEnabled
        assertSame("Enabled is the same",
                assertion.isEnabled(), cloneAssertion.isEnabled());
        cloneAssertion.setEnabled(!assertion.isEnabled());
        assertNotSame("Enabled is different",
                assertion.isEnabled(), cloneAssertion.isEnabled());


        // test comments
        assertNotNull(assertion.getAssertionComment());
        assertNotNull(cloneAssertion.getAssertionComment());

        assertEquals("Comments are equal", assertion.getAssertionComment().getProperties(), cloneAssertion.getAssertionComment().getProperties());

        assertion.getAssertionComment().setComment("left comment changed", Assertion.Comment.LEFT_COMMENT);
        assertEquals("right comment has same value",
                assertion.getAssertionComment().getAssertionComment(Assertion.Comment.RIGHT_COMMENT),
                cloneAssertion.getAssertionComment().getAssertionComment(Assertion.Comment.RIGHT_COMMENT));
        assertThat("left comment has different value",
                assertion.getAssertionComment().getAssertionComment(Assertion.Comment.LEFT_COMMENT),
                not(equalTo(cloneAssertion.getAssertionComment().getAssertionComment(Assertion.Comment.LEFT_COMMENT))));


        // again test Custom Assertion (expected sourceTarget to be different)
        assertThat("Custom Assertions are different",
                assertion.getCustomAssertion(), not(equalTo(cloneAssertion.getCustomAssertion())));

        // test Custom Assertion Name
        assertEquals("Custom Assertion Name is the same",
                assertion.getCustomAssertion().getName(), cloneAssertion.getCustomAssertion().getName());

        assertTrue("Original Custom Assertion is of type TestMessageTargetableCustomAssertion",
                assertion.getCustomAssertion() instanceof TestCustomMessageTargetable);
        final TestCustomMessageTargetable customAssertionOrig = (TestCustomMessageTargetable)assertion.getCustomAssertion();
        assertTrue("Clone Custom Assertion is of type TestMessageTargetableCustomAssertion",
                cloneAssertion.getCustomAssertion() instanceof TestCustomMessageTargetable);
        final TestCustomMessageTargetable customAssertionClone = (TestCustomMessageTargetable)cloneAssertion.getCustomAssertion();

        // test Custom Assertion Param1
        assertEquals("Custom Assertion Param1 is the same",
                customAssertionOrig.getProp1(), customAssertionClone.getProp1());
        assertNotSame("Custom Assertion Param1 is different reference",
                customAssertionOrig.getProp1(), customAssertionClone.getProp1());
        customAssertionClone.setProp1("param one changed");
        assertThat("Custom Assertion Param1 is different",
                customAssertionOrig.getProp1(), not(equalTo(customAssertionClone.getProp1())));

        // test Custom Assertion Param2
        assertSame("Custom Assertion Param2 is the same",
                customAssertionOrig.getProp2(), customAssertionClone.getProp2());
        customAssertionClone.setProp2(2);
        assertNotSame("Custom Assertion Param2 is different",
                customAssertionOrig.getProp2(), customAssertionClone.getProp2());

        // test Custom Assertion Param3
        assertSame("Custom Assertion Param3 is the same",
                customAssertionOrig.getProp3(), customAssertionClone.getProp3());
        customAssertionClone.setProp3(false);
        assertNotSame("Custom Assertion Param3 is different",
                customAssertionOrig.getProp3(), customAssertionClone.getProp3());

        // test Custom Assertion Param3
        assertEquals("Custom Assertion Param4 is the same",
                customAssertionOrig.getProp4(), customAssertionClone.getProp4());
        assertNotSame("Custom Assertion Param4 is different reference",
                customAssertionOrig.getProp4(), customAssertionClone.getProp4());
        //noinspection serial
        customAssertionClone.setProp4(new HashMap<String, Integer>() {
            {
                put("five", 5);
                put("six", 6);
            }
        });
        assertThat("Custom Assertion Param4 is different",
                customAssertionOrig.getProp4(), not(equalTo(customAssertionClone.getProp4())));
        assertNotSame("Custom Assertion Param4 size is different",
                customAssertionOrig.getProp4().size(), customAssertionClone.getProp4().size());
    }

    @Test
    public void testSerializable() throws Exception {
        final Assertion writePolicy = makeTestPolicy();
        assertNotNull("Policy not NULL", writePolicy);
        assertTrue("Policy instance of ExactlyOneAssertion", writePolicy instanceof ExactlyOneAssertion);

        final ExactlyOneAssertion eoaInitial = (ExactlyOneAssertion)writePolicy;
        assertSame("Number of policy children is 1", eoaInitial.getChildren().size(), 1);
        assertTrue("The first element is of type CustomAssertionHolder", eoaInitial.getChildren().get(0) instanceof CustomAssertionHolder);

        final String initialPolicyString = WspWriter.getPolicyXml(writePolicy);
        final Assertion readPolicy = WspReader.getDefault().parseStrictly(initialPolicyString, WspReader.OMIT_DISABLED);

        // do a quick check
        assertNotNull("Read policy is not NULL", readPolicy);
        assertTrue("Read policy is of type ExactlyOneAssertion", readPolicy instanceof ExactlyOneAssertion);
        final String readPolicyString = WspWriter.getPolicyXml(readPolicy);
        //These are no longer equal as the jdk does not consistently serialize hashmap anymore
        //assertEquals("Both policies XMLs are the same", initialPolicyString, readPolicyString);

        final ExactlyOneAssertion eoaRead = (ExactlyOneAssertion)readPolicy;
        assertSame("Read policy number of children is 1", eoaRead.getChildren().size(), 1);
        assertTrue("Read policy first child is of type CustomAssertionHolder", eoaRead.getChildren().get(0) instanceof CustomAssertionHolder);

        final CustomAssertionHolder caOriginal = (CustomAssertionHolder)eoaInitial.getChildren().get(0);
        final CustomAssertionHolder caSerialized = (CustomAssertionHolder)eoaRead.getChildren().get(0);

        assertEquals("Target is the same between original and serialized assertion",
                caOriginal.getTarget(), caSerialized.getTarget());
        assertEquals("OtherTargetMessageVariable is the same between original and serialized assertion",
                caOriginal.getOtherTargetMessageVariable(), caSerialized.getOtherTargetMessageVariable());
        assertEquals("Description is the same between original and serialized assertion",
                caOriginal.getDescriptionText(), caSerialized.getDescriptionText());
        assertEquals("Categories are the same between original and serialized assertion",
                caOriginal.getCategories(), caSerialized.getCategories());
        assertEquals("Enabled is the same between original and serialized assertion",
                caOriginal.isEnabled(), caSerialized.isEnabled());
        assertEquals("CustomAssertion is the same between original and serialized assertion",
                caOriginal.getCustomAssertion(), caSerialized.getCustomAssertion());
        assertNotSame("CustomAssertion between original and serialized assertion has different reference",
                caOriginal.getCustomAssertion(), caSerialized.getCustomAssertion());

        assertNotNull(caOriginal.getAssertionComment());
        assertNotNull(caSerialized.getAssertionComment());

        assertEquals("Comments are equal",
                caOriginal.getAssertionComment().getProperties(),
                caSerialized.getAssertionComment().getProperties());
        assertEquals("left comment has same value",
                caOriginal.getAssertionComment().getAssertionComment(Assertion.Comment.LEFT_COMMENT),
                caSerialized.getAssertionComment().getAssertionComment(Assertion.Comment.LEFT_COMMENT));
        assertEquals("left comment is properly de-serialized",
                caOriginal.getAssertionComment().getAssertionComment(Assertion.Comment.LEFT_COMMENT),
                CUSTOM_ASSERTION_LEFT_COMMENT);
        assertEquals("right comment has same value",
                caOriginal.getAssertionComment().getAssertionComment(Assertion.Comment.RIGHT_COMMENT),
                caSerialized.getAssertionComment().getAssertionComment(Assertion.Comment.RIGHT_COMMENT));
        assertEquals("right comment is properly de-serialized",
                caOriginal.getAssertionComment().getAssertionComment(Assertion.Comment.RIGHT_COMMENT),
                CUSTOM_ASSERTION_RIGHT_COMMENT);
    }

    @Test
    public void testIsCustomCredentialSource() throws Exception {
        // default constructor
        holder = new CustomAssertionHolder();
        assertFalse(holder.isCustomCredentialSource());

        // add to non credential source category (null custom assertion)
        holder = new CustomAssertionHolder();
        holder.setCategories(Category.LOGIC,  Category.MESSAGE);
        assertFalse(holder.isCustomCredentialSource());

        // add to non credential source category
        holder = new CustomAssertionHolder();
        holder.setCustomAssertion(new StubCustomAssertion());
        assertFalse(holder.isCustomCredentialSource());

        // add to non credential source category
        holder = new CustomAssertionHolder();
        holder.setCustomAssertion(new StubCustomAssertion());
        holder.setCategories(Category.LOGIC,  Category.MESSAGE);
        assertFalse(holder.isCustomCredentialSource());

        // add to credential source category (ACCESS_CONTROL) (null custom assertion)
        holder = new CustomAssertionHolder();
        holder.setCategories(Category.LOGIC,  Category.ACCESS_CONTROL);
        assertTrue(holder.isCustomCredentialSource());

        // add to credential source category (ACCESS_CONTROL)
        holder = new CustomAssertionHolder();
        holder.setCustomAssertion(new StubCustomAssertion());
        holder.setCategories(Category.LOGIC,  Category.ACCESS_CONTROL);
        assertTrue(holder.isCustomCredentialSource());

        //noinspection serial
        class TestCustomCredentialSourceAssertion implements CustomAssertion, CustomCredentialSource {
            @Override public String getName() { return "Test CustomCredentialSource Assertion"; }
            @Override public boolean isCredentialSource() { return false; }
        }

        // custom assertion implementing CustomCredentialSource interface returning true
        TestCustomCredentialSourceAssertion mockCredSourceAssertion = spy(new TestCustomCredentialSourceAssertion());
        doReturn(false).when(mockCredSourceAssertion).isCredentialSource();

        // add custom assertion implementing CustomCredentialSource interface returning false (empty Categories)
        holder = new CustomAssertionHolder();
        holder.setCustomAssertion(mockCredSourceAssertion);
        assertFalse(holder.isCustomCredentialSource());

        // add custom assertion implementing CustomCredentialSource interface returning false
        holder = new CustomAssertionHolder();
        holder.setCustomAssertion(mockCredSourceAssertion);
        holder.setCategories(Category.LOGIC,  Category.CUSTOM_ASSERTIONS);
        assertFalse(holder.isCustomCredentialSource());

        // add custom assertion implementing CustomCredentialSource interface and credential source category (ACCESS_CONTROL)
        holder = new CustomAssertionHolder();
        holder.setCustomAssertion(mockCredSourceAssertion);
        holder.setCategories(Category.LOGIC,  Category.ACCESS_CONTROL);
        assertTrue(holder.isCustomCredentialSource());


        // custom assertion implementing CustomCredentialSource interface returning true
        mockCredSourceAssertion = spy(new TestCustomCredentialSourceAssertion());
        doReturn(true).when(mockCredSourceAssertion).isCredentialSource();

        // add custom assertion implementing CustomCredentialSource interface returning true (empty Categories)
        holder = new CustomAssertionHolder();
        holder.setCustomAssertion(mockCredSourceAssertion);
        assertTrue(holder.isCustomCredentialSource());

        // add custom assertion implementing CustomCredentialSource interface returning false
        holder = new CustomAssertionHolder();
        holder.setCustomAssertion(mockCredSourceAssertion);
        holder.setCategories(Category.LOGIC,  Category.CUSTOM_ASSERTIONS);
        assertTrue(holder.isCustomCredentialSource());

        // add custom assertion implementing CustomCredentialSource interface and credential source category (ACCESS_CONTROL)
        holder = new CustomAssertionHolder();
        holder.setCustomAssertion(mockCredSourceAssertion);
        holder.setCategories(Category.LOGIC,  Category.ACCESS_CONTROL);
        assertTrue(holder.isCustomCredentialSource());
    }

    /**
     * Utility function to create test policy
     *
     * @return test policy consistent of a single MessageTargetableCustomAssertion
     */
    private Assertion makeTestPolicy() {
        // Custom Assertion
        final CustomAssertionHolder customAssertion = new CustomAssertionHolder();
        customAssertion.setCategories(Category.ACCESS_CONTROL);
        customAssertion.setCustomAssertion(new TestCustomMessageTargetable("test string", 11, false, TEST_CA_MAP_ELEMENTS));
        final Assertion.Comment customAssertionComment = new Assertion.Comment();
        customAssertionComment.setComment(CUSTOM_ASSERTION_LEFT_COMMENT, Assertion.Comment.LEFT_COMMENT);
        customAssertionComment.setComment(CUSTOM_ASSERTION_RIGHT_COMMENT, Assertion.Comment.RIGHT_COMMENT);
        customAssertion.setAssertionComment(customAssertionComment);

        return new ExactlyOneAssertion(Arrays.asList(customAssertion));
    }
}
