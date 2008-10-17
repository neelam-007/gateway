package com.l7tech.test.util;

/**
 * Service interface for test launchers.
 */
public interface GuiTestLauncher {

    /**
     * Run GUI test for all annotated methods of testHolder
     *
     * @param testHolder The test object
     */
    public void startTest(final Object testHolder) throws GuiTestException;
}
