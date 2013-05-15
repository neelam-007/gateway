/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 */

package com.l7tech.policy.assertion;

import java.util.*;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;

import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.credential.http.HttpBasic;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.AllAssertions;
import com.l7tech.test.BugNumber;
import com.l7tech.util.HexUtils;
import org.junit.Test;
import org.junit.Assert;

import static org.hamcrest.CoreMatchers.*;

/**
 * Test Assertion/CompositeAssertion data structure management.
 */
@SuppressWarnings({"unchecked"})
public class AssertionTest {
    private static final String ASSERTION_PRE_50 = "rO0ABXNyADFjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uQ3VzdG9tQXNzZXJ0aW9uSG9sZGVyZtcreFwddTICAANMAAhjYXRlZ29yeXQAKkxjb20vbDd0ZWNoL3BvbGljeS9hc3NlcnRpb24vZXh0L0NhdGVnb3J5O0wAD2N1c3RvbUFzc2VydGlvbnQAMUxjb20vbDd0ZWNoL3BvbGljeS9hc3NlcnRpb24vZXh0L0N1c3RvbUFzc2VydGlvbjtMAA9kZXNjcmlwdGlvblRleHR0ABJMamF2YS9sYW5nL1N0cmluZzt4cgAlY29tLmw3dGVjaC5wb2xpY3kuYXNzZXJ0aW9uLkFzc2VydGlvbttfY5k8vaKxAgAAeHBwcHA=";

    @Test
    public void testAddingChildToParent() throws Exception {
        CompositeAssertion root = new AllAssertion();
        Assert.assertEquals(1, root.getOrdinal());
        Assert.assertNull(root.getParent());

        Assertion t1 = new TrueAssertion();
        Assert.assertEquals(1, root.getOrdinal());
        Assert.assertNull(t1.getParent());

        root.getChildren().add(t1);
        root.treeChanged();
        Assert.assertEquals(1, root.getOrdinal());
        Assert.assertEquals(2, t1.getOrdinal());
        Assert.assertTrue(t1.getParent() == root);
        Assert.assertNull(root.getParent());
    }

    @Test
    public void testNormalPolicyBuilding() throws Exception {
        Assertion t1 = new TrueAssertion();
        Assertion t2 = new TrueAssertion();
        Assertion t3 = new TrueAssertion();
        Assertion t4 = new TrueAssertion();
        Assertion t5 = new TrueAssertion();
        CompositeAssertion root = new AllAssertion(Arrays.asList(t1, t2, t3, t4, t5));
        Assert.assertEquals(1, root.getOrdinal());
        Assert.assertEquals(2, t1.getOrdinal());
        Assert.assertEquals(5, t4.getOrdinal());
        Assert.assertEquals(6, t5.getOrdinal());
        Assert.assertEquals(root, t3.getParent());
        Assert.assertTrue(root.getChildren().contains(t2));

        Assertion ct1 = new TrueAssertion();
        Assertion ct2 = new TrueAssertion();
        CompositeAssertion c1 = new OneOrMoreAssertion(Arrays.asList(ct1, ct2));
        root.getChildren().add(c1);
        root.treeChanged();
        Assert.assertEquals(root, c1.getParent());
        Assert.assertTrue(root.getChildren().contains(c1));
        Assert.assertEquals(7, c1.getOrdinal());
        Assert.assertEquals(8, ct1.getOrdinal());
        Assert.assertEquals(9, ct2.getOrdinal());
        Assert.assertTrue(root.getAssertionWithOrdinal(7) == c1);

        final FalseAssertion f1 = new FalseAssertion();
        root.getChildren().add(4, f1);
        root.treeChanged();
        Assert.assertEquals(6, f1.getOrdinal());
        Assert.assertEquals(10, ct2.getOrdinal());
        Assert.assertTrue(root.getAssertionWithOrdinal(9) == ct1);
    }

    @Test
    public void testFindFeatureName() throws Exception {
        String compClassname = "com.l7tech.external.assertions.comparison.ComparisonAssertion";
        String defaultCompFs = Assertion.makeDefaultFeatureSetName(compClassname, "com.l7tech.external.assertions.comparison");
        Assert.assertEquals("assertion:Comparison", defaultCompFs);
    }

    @Test
    public void testAnnotationSanity() {
        for ( Assertion assertion : AllAssertions.GATEWAY_EVERYTHING ) {
            String name = assertion.getClass().getName();
            if (name.startsWith("Request")) {
                Assert.assertTrue("Has request annotation : " + name, Assertion.isRequest(assertion));

            } else if (name.startsWith("Response")) {
                Assert.assertTrue("Has response annotation : " + name, Assertion.isResponse(assertion));
            }

            Assert.assertFalse("Has both request and response annotations : " + name,
                    Assertion.isRequest(assertion) && Assertion.isResponse(assertion));                

            if (name.contains("Wss") && !name.contains("Wssp")) {
                Assert.assertTrue("Has WSS annotation : " + name, Assertion.isWSSecurity(assertion));
            }
        }
    }

    @Test
    public void testDeserializePre50IsEnabled() throws Exception {
        ObjectInputStream in = new ObjectInputStream( new ByteArrayInputStream(HexUtils.decodeBase64(ASSERTION_PRE_50)) );
        Assertion assertion = (Assertion) in.readObject();
        Assert.assertTrue("Assertion is enabled", assertion.isEnabled());
    }

    @Test
    public void testCloneCustomAssertion() {
        final CustomAssertionHolder holder = new CustomAssertionHolder();
        final TestCustomAssertion holderAssertion = new TestCustomAssertion("test");

        holder.setCustomAssertion( holderAssertion );
        holder.setDescriptionText("Description");
        holder.setCategory(Category.ACCESS_CONTROL);

        final CustomAssertionHolder cloneHolder = (CustomAssertionHolder) holder.getCopy();
        final TestCustomAssertion cloneHolderAssertion = (TestCustomAssertion)cloneHolder.getCustomAssertion();

        Assert.assertEquals("Assertion bean property", holderAssertion.getTestValue(), cloneHolderAssertion.getTestValue());
        Assert.assertEquals("Assertion category", holder.getCategory(), cloneHolder.getCategory());
        Assert.assertEquals("Assertion description", holder.getDescriptionText(), cloneHolder.getDescriptionText());

        // Test that we have a deep copy of the CAH (ensure the CustomAssertion instance is not the same in the clone)
        cloneHolderAssertion.setTestValue("Updated");

        Assert.assertNotSame("Assertion bean property", holderAssertion.getTestValue(), cloneHolderAssertion.getTestValue());
    }

    public static class TestCustomAssertion implements CustomAssertion {
        private String testValue;

        public TestCustomAssertion() {
        }

        public TestCustomAssertion( final String value ) {
            testValue = value;
        }

        @Override
        public String getName() {
            return "Test";
        }

        public String getTestValue() {
            return testValue;
        }

        public void setTestValue(String testValue) {
            this.testValue = testValue;
        }
    }

    /**
     * Tests that disable assertions are checked accordingly.
     */
    @Test
    public void testSearchingEnableDisableAssertions() {
        FalseAssertion falseAssertion = new FalseAssertion();
        falseAssertion.setEnabled(false);
        final List kids = Arrays.asList(new TrueAssertion(), falseAssertion);
        CompositeAssertion rootAssertion = new AllAssertion(kids);

        //should not contain the false assertion because it's disabled
        Assert.assertFalse("Disabled false assertion is not contained", Assertion.contains(rootAssertion, FalseAssertion.class));
        Assert.assertTrue("Enabled true assertion is contained", Assertion.contains(rootAssertion, TrueAssertion.class));

        //should find or contains the disabled assertion
        Assert.assertTrue("Disabled false assertion is found", Assertion.contains(rootAssertion, FalseAssertion.class, false));
        Assert.assertTrue("Disabled false assertion is found", Assertion.find(rootAssertion, FalseAssertion.class, false) != null);

        //should not find or contain the disabled assertion
        Assert.assertFalse("Disabled false assertion is NOT found (ignored)", Assertion.contains(rootAssertion, FalseAssertion.class, true));
        Assert.assertFalse("Disabled false assertion is NOT found (ignored)", Assertion.find(rootAssertion, FalseAssertion.class, true) != null);
    }

    @Test
    @BugNumber(8653)
    public void testSimplifyRemovesComments() throws Exception{
        AllAssertion root = new AllAssertion();
        final HttpBasic httpBasic = new HttpBasic();
        httpBasic.setAssertionComment(new Assertion.Comment());
        root.addChild(httpBasic);
        AllAssertion all = new AllAssertion();
        all.setAssertionComment(new Assertion.Comment());

        final AuditAssertion audit = new AuditAssertion();
        audit.setAssertionComment(new Assertion.Comment());
        all.addChild(audit);

        root.addChild(all);

        final Assertion assertion = Assertion.simplify(root, true, false);
        checkCommentsAreNull(assertion);
    }

    private void checkCommentsAreNull(Assertion assertion){
        Assert.assertNull("Comment should have been removed", assertion.getAssertionComment());
        if(assertion instanceof CompositeAssertion){
            CompositeAssertion comp = (CompositeAssertion) assertion;
            final List<Assertion> children = comp.getChildren();
            for (Assertion child : children) {
                checkCommentsAreNull(child);
            }
        }
    }

    /**
     * Test Assertion Comment cloning
     */
    @Test
    public void testCloningAssertionComment()
    {
        TrueAssertion assertion = new TrueAssertion();

        Assertion.Comment comment = new Assertion.Comment();
        comment.setComment("left comment", Assertion.Comment.LEFT_COMMENT);
        comment.setComment("right comment", Assertion.Comment.RIGHT_COMMENT);
        assertion.setAssertionComment(comment);

        final TrueAssertion cloneAssertion = (TrueAssertion)assertion.getCopy();

        Assert.assertNotNull(assertion.getAssertionComment());
        Assert.assertNotNull(cloneAssertion.getAssertionComment());

        assertion.getAssertionComment().setComment("left comment changed", Assertion.Comment.LEFT_COMMENT);

        Assert.assertEquals("right comment has same value",
                assertion.getAssertionComment().getAssertionComment(Assertion.Comment.RIGHT_COMMENT),
                cloneAssertion.getAssertionComment().getAssertionComment(Assertion.Comment.RIGHT_COMMENT));

        // Expected behavior is that left comment of original and clone assertions have different value
        Assert.assertThat("left comment has different value",
                assertion.getAssertionComment().getAssertionComment(Assertion.Comment.LEFT_COMMENT),
                not(equalTo(cloneAssertion.getAssertionComment().getAssertionComment(Assertion.Comment.LEFT_COMMENT))));

        // Verify that assertions comment properties have different values/entries
        Assert.assertThat("comment properties have different value",
                assertion.getAssertionComment().getProperties(),
                not(equalTo(cloneAssertion.getAssertionComment().getProperties())));

        // Verify that both assertions have different comment properties reference
        Assert.assertNotSame("comment properties reference is different",
                assertion.getAssertionComment().getProperties(),
                cloneAssertion.getAssertionComment().getProperties());

        cloneAssertion.getAssertionComment().setComment("right comment changed", Assertion.Comment.RIGHT_COMMENT);

        Assert.assertThat("right comment has different value",
                assertion.getAssertionComment().getAssertionComment(Assertion.Comment.RIGHT_COMMENT),
                not(equalTo(cloneAssertion.getAssertionComment().getAssertionComment(Assertion.Comment.RIGHT_COMMENT))));
    }
}
