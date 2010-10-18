package com.l7tech.policy;

import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.test.BugNumber;
import com.l7tech.util.Functions;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test for PolicyUtil.
 */
public class PolicyUtilTest {
    @Test
    public void testVisitLeaf() throws Exception {
        PolicyUtil.visitDescendantsAndSelf(leafAssertion, visitor, translator);
        assertEquals(1, visitOrder.size());
        assertEquals(leafAssertion, visitOrder.get(0));
        assertEquals("Leaf assertion shall have been visited exactly once", 1, (long)visitCounts.get(leafAssertion));
    }

    @BugNumber(9111)
    @Test
    public void testVisitDisabledLeaf() throws Exception {
        leafAssertion.setEnabled(false);
        PolicyUtil.visitDescendantsAndSelf(leafAssertion, visitor, translator);
        assertEquals("Disabled assertions shall not be visited", 0, visitOrder.size());
    }

    @Test
    public void testVisitSubtree() throws Exception {
        PolicyUtil.visitDescendantsAndSelf(simplePolicy, visitor, translator);
        assertEquals(5, visitOrder.size());
        assertEquals(simplePolicy, visitOrder.get(0));
        assertEquals("Leaf assertion shall have been visited exactly once", 1, (long)visitCounts.get(leafAssertion));
    }

    @Test
    public void testVisitInclude() throws Exception {
        PolicyUtil.visitDescendantsAndSelf(includePolicy, visitor, translator);
        assertEquals(17, visitOrder.size());
        assertEquals(includePolicy, visitOrder.get(0));
        assertEquals("Leaf assertion shall have been visited twice since its fragment was included twice", 2, (long)visitCounts.get(leafAssertion));
    }

    @Test
    public void testVisitIncludeWithDisabledTarget() throws Exception {
        simplePolicy.setEnabled(false);
        PolicyUtil.visitDescendantsAndSelf(includePolicy, visitor, translator);
        assertNull("Children of disabled include target shall not have been visited", visitCounts.get(leafAssertion));
        assertEquals(7, visitOrder.size());
    }


    List<Assertion> visitOrder = new ArrayList<Assertion>();
    Map<Assertion,Integer> visitCounts = new HashMap<Assertion,Integer>();

    Assertion simplePolicy = new AllAssertion(Arrays.<Assertion>asList(
            new FalseAssertion(),
            new OneOrMoreAssertion(Arrays.asList(
                    leafAssertion = new TrueAssertion(),
                    new TrueAssertion()
            ))
    ));
    Assertion leafAssertion;

    // Include the same fragment twice, just to keep things interesting
    Assertion includePolicy = new AllAssertion(Arrays.<Assertion>asList(
            new TrueAssertion(),
            new OneOrMoreAssertion(Arrays.asList(
                    new TrueAssertion(),
                    includeAssertion = new Include("asdf"),
                    new FalseAssertion()
            )),
            new Include("asdf")
    ));
    Assertion includeAssertion;

    Map<Assertion,Integer> translateCount = new HashMap<Assertion, Integer>();
    Map<Assertion,Integer> translationFinishedCount = new HashMap<Assertion, Integer>();
    AssertionTranslator translator = new AssertionTranslator() {
        @Override
        public Assertion translate(Assertion sourceAssertion) throws PolicyAssertionException {
            count(sourceAssertion, translateCount);
            if (sourceAssertion instanceof Include) {
                Include include = (Include) sourceAssertion;
                if ("asdf".equals(include.getPolicyGuid()))
                    return simplePolicy;
            }
            return sourceAssertion;
        }

        @Override
        public void translationFinished(Assertion sourceAssertion) {
            count(sourceAssertion, translationFinishedCount);
        }
    };

    Functions.UnaryVoid<Assertion> visitor = new Functions.UnaryVoid<com.l7tech.policy.assertion.Assertion>() {
        @Override
        public void call(Assertion assertion) {
            visitOrder.add(assertion);
            count(assertion, visitCounts);
        }
    };

    static void count(Assertion assertion, Map<Assertion, Integer> counter) {
        if (!counter.containsKey(assertion))
            counter.put(assertion, 1);
        else
            counter.put(assertion, counter.get(assertion) + 1);
    }
}
