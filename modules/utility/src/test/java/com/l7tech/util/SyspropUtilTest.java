package com.l7tech.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 *
 */
public class SyspropUtilTest {

    @Test
    public void testClearProperties() throws Exception {
        SyspropUtil.setProperty("com.l7tech.foo", "fooval");
        SyspropUtil.setProperty("com.l7tech.bar", "barval");
        SyspropUtil.setProperty("com.l7tech.baz", "bazval");
        assertEquals("fooval", SyspropUtil.getString("com.l7tech.foo", null));
        assertEquals("barval", SyspropUtil.getString("com.l7tech.bar", null));
        assertEquals("bazval", SyspropUtil.getString("com.l7tech.baz", null));

        SyspropUtil.clearProperties("com.l7tech.foo", "com.l7tech.bar");

        assertNull(SyspropUtil.getString("com.l7tech.foo", null));
        assertNull(SyspropUtil.getString("com.l7tech.bar", null));
        assertEquals("bazval", SyspropUtil.getString("com.l7tech.baz", null));
    }

}
