package com.l7tech.test.conditional;

/**
 * {@link IgnoreCondition} implementation, which is satisfied if the unit test is running on Windows platform.<br/>
 */
public class RunsOnWindows implements IgnoreCondition {
    @Override
    public boolean isSatisfied() {
        return System.getProperty("os.name").startsWith("Windows");
    }
}
