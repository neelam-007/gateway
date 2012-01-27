package com.l7tech.console.panels;

import com.l7tech.console.util.MockSsmPreferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.util.SyspropUtil;
import org.junit.Test;

/**
 *
 */
public class LogPanelTest {

    /**
     * Ensure the LogPanel does not require an admin connection during construction.
     *
     * <p>The LogPanel is used offline to view saved log and audit events.</p>
     */
    @Test
    public void testConstructorOffline() {
        // Test only meaningful if forms compiled
        // Using JUnit assumptions does not work with a code coverage build (Assume.assumeTrue(...))
        if( !SyspropUtil.getBoolean( "module.skip.forms" ) ) {
            TopComponents.getInstance().setPreferences( new MockSsmPreferences() );
            new LogPanel();
        }
    }
}
