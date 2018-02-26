package com.l7tech.server.extension.registry;

import com.ca.apim.gateway.extension.Extension;
import com.ca.apim.gateway.extension.ExtensionAccess;
import com.ca.apim.gateway.extension.ExtensionRegistry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.logging.Logger;

public class AbstractRegistryImplTest {

    private static Logger LOGGER = Logger.getLogger(AbstractRegistryImplTest.class.getName());

    private ExtensionRegistry<TestExtension> extensionRegistry;
    private ExtensionAccess<TestExtension> extensionAccess;

    @Before
    public void before() {
        AbstractRegistryImpl<TestExtension> registry = new AbstractRegistryImpl<TestExtension>() {
            @Override
            protected Logger getLogger() {
                return LOGGER;
            }
        };
        extensionAccess = registry;
        extensionRegistry = registry;
    }

    @Test
    public void testRegisterAndRetrieve() {
        String key1 = "key1";
        TestExtension extension1 = new TestExtension();
        String key2 = "key2";
        TestExtension extension2 = new TestExtension();
        TestExtension extension1Repeated = new TestExtension();

        Assert.assertNull(extensionAccess.getExtension(key1));
        Assert.assertEquals(0, extensionAccess.getAllExtensions().size());

        extensionRegistry.register(key1, extension1);
        Assert.assertEquals(extension1, extensionAccess.getExtension(key1));
        Assert.assertNull(extensionAccess.getExtension(key2));
        Assert.assertEquals(1, extensionAccess.getAllExtensions().size());

        extensionRegistry.register(key2, extension2);
        Assert.assertEquals(extension1, extensionAccess.getExtension(key1));
        Assert.assertEquals(extension2, extensionAccess.getExtension(key2));
        Assert.assertEquals(2, extensionAccess.getAllExtensions().size());

        extensionRegistry.register(key1, extension1Repeated);
        Assert.assertEquals(extension1Repeated, extensionAccess.getExtension(key1));
        Assert.assertEquals(extension2, extensionAccess.getExtension(key2));
        Assert.assertEquals(2, extensionAccess.getAllExtensions().size());
        Assert.assertFalse(extensionAccess.getAllExtensions().contains(extension1));
        Assert.assertTrue(extensionAccess.getAllExtensions().contains(extension2));
        Assert.assertTrue(extensionAccess.getAllExtensions().contains(extension1Repeated));
    }

    @Test
    public void testUnregister() {
        String key1 = "key1";
        TestExtension extension1 = new TestExtension();
        String key2 = "key2";
        TestExtension extension2 = new TestExtension();

        extensionRegistry.register(key1, extension1);
        extensionRegistry.register(key2, extension2);
        Assert.assertEquals(extension1, extensionAccess.getExtension(key1));
        Assert.assertEquals(extension2, extensionAccess.getExtension(key2));
        Assert.assertEquals(2, extensionAccess.getAllExtensions().size());

        extensionRegistry.unregister(key1);
        Assert.assertNull(extensionAccess.getExtension(key1));
        Assert.assertEquals(extension2, extensionAccess.getExtension(key2));
        Assert.assertEquals(1, extensionAccess.getAllExtensions().size());

        extensionRegistry.unregister(key2);
        Assert.assertNull(extensionAccess.getExtension(key1));
        Assert.assertNull(extensionAccess.getExtension(key2));
        Assert.assertEquals(0, extensionAccess.getAllExtensions().size());
    }

    @Test
    public void registerWithTags() {
        String key1 = "key1";
        TestExtension extension1 = new TestExtension();
        String key2 = "key2";
        TestExtension extension2 = new TestExtension();
        String tag1 = "tag1";
        String tag2 = "tag2";
        String tag3 = "tag3";
        String tag4 = "tag4";
        String tag5 = "tag5";
        String tag6 = "tag6";

        extensionRegistry.register(key1, extension1, tag1, tag2, tag3);
        extensionRegistry.register(key2, extension2, tag5, tag4, tag3);
        Assert.assertEquals(extension1, extensionAccess.getExtension(key1));
        Assert.assertEquals(extension2, extensionAccess.getExtension(key2));
        Assert.assertEquals(2, extensionAccess.getAllExtensions().size());
        Assert.assertEquals(0, extensionAccess.getTaggedExtensions().size());
        Collection<TestExtension> taggedExtensions = extensionAccess.getTaggedExtensions(tag1);
        Assert.assertEquals(1, taggedExtensions.size());
        Assert.assertTrue(taggedExtensions.contains(extension1));

        taggedExtensions = extensionAccess.getTaggedExtensions(tag1, tag2);
        Assert.assertEquals(1, taggedExtensions.size());
        Assert.assertTrue(taggedExtensions.contains(extension1));

        taggedExtensions = extensionAccess.getTaggedExtensions(tag1, tag2, tag3);
        Assert.assertEquals(2, taggedExtensions.size());
        Assert.assertTrue(taggedExtensions.contains(extension1));
        Assert.assertTrue(taggedExtensions.contains(extension2));

        taggedExtensions = extensionAccess.getTaggedExtensions(tag4);
        Assert.assertEquals(1, taggedExtensions.size());
        Assert.assertTrue(taggedExtensions.contains(extension2));

        taggedExtensions = extensionAccess.getTaggedExtensions(tag6);
        Assert.assertEquals(0, taggedExtensions.size());
    }

    @Test
    public void reregisterWithTags() {
        String key1 = "key1";
        TestExtension extension1 = new TestExtension();
        String key2 = "key2";
        TestExtension extension2 = new TestExtension();
        TestExtension extension1Repeated = new TestExtension();
        String tag1 = "tag1";
        String tag2 = "tag2";
        String tag3 = "tag3";
        String tag4 = "tag4";
        String tag5 = "tag5";
        String tag6 = "tag6";

        extensionRegistry.register(key1, extension1, tag1, tag2, tag3);
        extensionRegistry.register(key2, extension2, tag5, tag4, tag3);
        extensionRegistry.register(key1, extension1Repeated, tag2, tag4, tag6);
        Assert.assertEquals(extension1Repeated, extensionAccess.getExtension(key1));
        Assert.assertEquals(extension2, extensionAccess.getExtension(key2));
        Assert.assertEquals(2, extensionAccess.getAllExtensions().size());
        Assert.assertEquals(0, extensionAccess.getTaggedExtensions().size());
        Collection<TestExtension> taggedExtensions = extensionAccess.getTaggedExtensions(tag1);
        Assert.assertEquals(0, taggedExtensions.size());

        taggedExtensions = extensionAccess.getTaggedExtensions(tag1, tag2);
        Assert.assertEquals(1, taggedExtensions.size());
        Assert.assertTrue(taggedExtensions.contains(extension1Repeated));

        taggedExtensions = extensionAccess.getTaggedExtensions(tag1, tag2, tag3);
        Assert.assertEquals(2, taggedExtensions.size());
        Assert.assertTrue(taggedExtensions.contains(extension1Repeated));
        Assert.assertTrue(taggedExtensions.contains(extension2));

        taggedExtensions = extensionAccess.getTaggedExtensions(tag4);
        Assert.assertEquals(2, taggedExtensions.size());
        Assert.assertTrue(taggedExtensions.contains(extension1Repeated));
        Assert.assertTrue(taggedExtensions.contains(extension2));

        taggedExtensions = extensionAccess.getTaggedExtensions(tag3);
        Assert.assertEquals(1, taggedExtensions.size());
        Assert.assertTrue(taggedExtensions.contains(extension2));

        taggedExtensions = extensionAccess.getTaggedExtensions(tag6);
        Assert.assertEquals(1, taggedExtensions.size());
        Assert.assertTrue(taggedExtensions.contains(extension1Repeated));
    }

    @Test
    public void unregisterWithTags() {
        String key1 = "key1";
        TestExtension extension1 = new TestExtension();
        String key2 = "key2";
        TestExtension extension2 = new TestExtension();
        String tag1 = "tag1";
        String tag2 = "tag2";
        String tag3 = "tag3";
        String tag4 = "tag4";
        String tag5 = "tag5";

        extensionRegistry.register(key1, extension1, tag1, tag2, tag3);
        extensionRegistry.register(key2, extension2, tag5, tag4, tag3);
        extensionRegistry.unregister(key1);
        Assert.assertNull(extensionAccess.getExtension(key1));
        Assert.assertEquals(extension2, extensionAccess.getExtension(key2));
        Assert.assertEquals(1, extensionAccess.getAllExtensions().size());
        Collection<TestExtension> taggedExtensions = extensionAccess.getTaggedExtensions(tag1);
        Assert.assertEquals(0, taggedExtensions.size());

        taggedExtensions = extensionAccess.getTaggedExtensions(tag1, tag2);
        Assert.assertEquals(0, taggedExtensions.size());

        taggedExtensions = extensionAccess.getTaggedExtensions(tag3);
        Assert.assertEquals(1, taggedExtensions.size());
        Assert.assertTrue(taggedExtensions.contains(extension2));

    }

    private class TestExtension implements Extension {
    }
}