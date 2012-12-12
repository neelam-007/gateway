package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.DefaultAssertionMetadata;
import org.junit.Test;

import java.awt.*;

import static org.junit.Assert.*;

public class DefaultAssertionPolicyNodeTest {
    private static final String TEST_ICON_RESOURCE = "com/l7tech/console/resources/Bean24.gif";
    private static final String DEFAULT_NODE_ICON = "com/l7tech/console/resources/policy16.gif";
    private static final String TEST_BASE_64_IMAGE = "fakeBase64Image";
    private DefaultAssertionPolicyNode node;

    @Test
    public void base64EncodedIconImage() {
        node = new DefaultAssertionPolicyNode(new StubAssertion(TEST_BASE_64_IMAGE, TEST_ICON_RESOURCE));
        assertEquals(TEST_BASE_64_IMAGE, node.base64EncodedIconImage(true));
        assertEquals(TEST_BASE_64_IMAGE, node.base64EncodedIconImage(false));
    }

    @Test
    public void base64EncodedImageNull() {
        node = new DefaultAssertionPolicyNode((new StubAssertion(null, TEST_ICON_RESOURCE)));
        assertNull(node.base64EncodedIconImage(true));
        assertNull(node.base64EncodedIconImage(false));
    }

    @Test
    public void iconResource() {
        node = new DefaultAssertionPolicyNode((new StubAssertion(null, TEST_ICON_RESOURCE)));

        assertEquals(TEST_ICON_RESOURCE, node.iconResource(true));
        assertEquals(TEST_ICON_RESOURCE, node.iconResource(false));
    }

    @Test
    public void iconResourceNull() {
        node = new DefaultAssertionPolicyNode((new StubAssertion(null, null)));
        assertEquals(DEFAULT_NODE_ICON, node.iconResource(true));
        assertEquals(DEFAULT_NODE_ICON, node.iconResource(false));
    }

    private class StubAssertion extends Assertion {
        private final String base64Image;
        private final String iconResource;

        private StubAssertion(final String base64Image, final String iconResource) {
            this.base64Image = base64Image;
            this.iconResource = iconResource;
        }

        @Override
        public AssertionMetadata meta() {
            final DefaultAssertionMetadata meta = new DefaultAssertionMetadata(this);
            meta.put(AssertionMetadata.BASE_64_NODE_IMAGE, base64Image);
            meta.put(AssertionMetadata.POLICY_NODE_ICON, iconResource);
            return meta;
        }
    }
}
