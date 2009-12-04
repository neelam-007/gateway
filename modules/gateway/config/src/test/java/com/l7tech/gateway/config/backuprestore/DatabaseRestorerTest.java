package com.l7tech.gateway.config.backuprestore;

import org.junit.Test;
import org.junit.Assert;

/**
 * @author jbufu
 */
public class DatabaseRestorerTest {
    @Test
    public void testUnescapedBackslash() throws Exception {
        Assert.assertNull(DatabaseRestorer.fixUnescapedBackslash(null));
        Assert.assertEquals("", DatabaseRestorer.fixUnescapedBackslash(""));

        Assert.assertEquals("\\\\", DatabaseRestorer.fixUnescapedBackslash("\\"));
        Assert.assertEquals("\\\\", DatabaseRestorer.fixUnescapedBackslash("\\\\"));
        Assert.assertEquals("\\\\\\\\", DatabaseRestorer.fixUnescapedBackslash("\\\\\\"));
        Assert.assertEquals("\\r", DatabaseRestorer.fixUnescapedBackslash("\\r"));
        Assert.assertEquals("\\n", DatabaseRestorer.fixUnescapedBackslash("\\n"));
        Assert.assertEquals("\\\'", DatabaseRestorer.fixUnescapedBackslash("\\\'"));
        Assert.assertEquals("\\\"", DatabaseRestorer.fixUnescapedBackslash("\\\""));

        Assert.assertEquals("\\\\foo", DatabaseRestorer.fixUnescapedBackslash("\\foo"));
        Assert.assertEquals("\\\\foo", DatabaseRestorer.fixUnescapedBackslash("\\\\foo"));
        Assert.assertEquals("\\rfoo", DatabaseRestorer.fixUnescapedBackslash("\\rfoo"));
        Assert.assertEquals("\\nfoo", DatabaseRestorer.fixUnescapedBackslash("\\nfoo"));
        Assert.assertEquals("\\\'foo", DatabaseRestorer.fixUnescapedBackslash("\\\'foo"));
        Assert.assertEquals("\\\"foo", DatabaseRestorer.fixUnescapedBackslash("\\\"foo"));

        Assert.assertEquals("foo\\\\", DatabaseRestorer.fixUnescapedBackslash("foo\\"));
        Assert.assertEquals("foo\\\\", DatabaseRestorer.fixUnescapedBackslash("foo\\\\"));
        Assert.assertEquals("foo\\r", DatabaseRestorer.fixUnescapedBackslash("foo\\r"));
        Assert.assertEquals("foo\\n", DatabaseRestorer.fixUnescapedBackslash("foo\\n"));
        Assert.assertEquals("foo\\\'", DatabaseRestorer.fixUnescapedBackslash("foo\\\'"));
        Assert.assertEquals("foo\\\"", DatabaseRestorer.fixUnescapedBackslash("foo\\\""));

        Assert.assertEquals("foo\\\\bar", DatabaseRestorer.fixUnescapedBackslash("foo\\bar"));
        Assert.assertEquals("foo\\\\bar", DatabaseRestorer.fixUnescapedBackslash("foo\\\\bar"));
        Assert.assertEquals("foo\\rbar", DatabaseRestorer.fixUnescapedBackslash("foo\\rbar"));
        Assert.assertEquals("foo\\nbar", DatabaseRestorer.fixUnescapedBackslash("foo\\nbar"));
        Assert.assertEquals("foo\\\'bar", DatabaseRestorer.fixUnescapedBackslash("foo\\\'bar"));
        Assert.assertEquals("foo\\\"bar", DatabaseRestorer.fixUnescapedBackslash("foo\\\"bar"));
    }

}
