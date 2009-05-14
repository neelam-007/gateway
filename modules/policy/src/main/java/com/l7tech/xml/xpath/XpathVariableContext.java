package com.l7tech.xml.xpath;

import java.util.concurrent.Callable;

/**
 * Holds a thread-local {@link XpathVariableFinder}.
 */
public class XpathVariableContext {
    private static final ThreadLocal<XpathVariableFinder> currentFinder = new ThreadLocal<XpathVariableFinder>();

    /**
     * Get the current XpathVariableFinder.
     *
     * @return  the current finder, or null if there isn't one.
     */
    public static XpathVariableFinder getCurrentVariableFinder() {
        return currentFinder.get();
    }

    /**
     * Perform some action using the specified variable finder as the current variable finder.
     *
     * @param variableFinder the variable finder to use while the action is performed.  This may be null.
     * @param action the action to perform with this variable finder.  Required.
     * @return the result of performing the action.
     * @throws Exception Any exception thrown by the action will be passed through uncaught.
     */
    public static <RT> RT doWithVariableFinder(XpathVariableFinder variableFinder, Callable<RT> action) throws Exception {
        XpathVariableFinder previousFinder = currentFinder.get();
        try {
            currentFinder.set(variableFinder);
            return action.call();
        } finally {
            currentFinder.set(previousFinder);
        }
    }
}
