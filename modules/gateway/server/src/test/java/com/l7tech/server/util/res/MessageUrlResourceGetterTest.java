package com.l7tech.server.util.res;

import com.l7tech.util.SyspropUtil;
import org.junit.Test;

import static com.l7tech.server.util.res.MessageUrlResourceGetter.maybeStripFragment;
import static org.junit.Assert.assertEquals;

/**
 * Unit test for {@link MessageUrlResourceGetter}.
 */
public class MessageUrlResourceGetterTest {

    @Test
    public void testMaybeStripFragment() {
        assertEquals("urn:foo", maybeStripFragment("urn:foo"));
        assertEquals("urn:foo", maybeStripFragment("urn:foo#bar"));

        // Now with sysprop turned off
        try {
            SyspropUtil.setProperty(MessageUrlResourceGetter.PROP_STRIP_FRAGMENT, "false");
            assertEquals("urn:foo", maybeStripFragment("urn:foo"));
            assertEquals("urn:foo#bar", maybeStripFragment("urn:foo#bar"));
        } finally {
            SyspropUtil.clearProperties(MessageUrlResourceGetter.PROP_STRIP_FRAGMENT);
        }
    }
}
