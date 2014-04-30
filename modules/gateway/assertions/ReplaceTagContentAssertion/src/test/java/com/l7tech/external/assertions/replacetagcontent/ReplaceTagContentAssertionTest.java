package com.l7tech.external.assertions.replacetagcontent;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.AssertionNodeNameFactory;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReplaceTagContentAssertionTest {
    private ReplaceTagContentAssertion assertion;
    private AssertionNodeNameFactory factory;

    @Before
    public void setup() {
        assertion = new ReplaceTagContentAssertion();
        factory = assertion.meta().get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
    }

    @Test
    public void nodeName() {
        assertion.setSearchFor("foo");
        assertion.setReplaceWith("bar");
        assertion.setTagsToSearch("a,b,c");
        assertEquals("Request: Replace foo in a,b,c with bar", factory.getAssertionName(assertion, true));
    }

    @Test
    public void nodeNameNullSearchFor() {
        assertion.setReplaceWith("bar");
        assertion.setTagsToSearch("a");
        assertEquals("Request: Replace Tag Content", factory.getAssertionName(assertion, true));
    }

    @Test
    public void nodeNameNullTagsToSearch() {
        assertion.setSearchFor("foo");
        assertion.setReplaceWith("bar");
        assertEquals("Request: Replace Tag Content", factory.getAssertionName(assertion, true));
    }

    @Test
    public void nodeNameNullReplaceWith() {
        assertion.setSearchFor("foo");
        assertion.setTagsToSearch("a,b,c");
        assertEquals("Request: Replace Tag Content", factory.getAssertionName(assertion, true));
    }

    @Test
    public void nodeNameDoNotDecorate() {
        assertion.setSearchFor("foo");
        assertion.setReplaceWith("bar");
        assertion.setTagsToSearch("a,b,c");
        assertEquals("Replace Tag Content", factory.getAssertionName(assertion, false));
    }
}
