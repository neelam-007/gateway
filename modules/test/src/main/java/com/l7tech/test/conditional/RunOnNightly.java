package com.l7tech.test.conditional;

/**
 * This will run the annotated unit test or class on nightly builds only.
 * It will also run the test locally when run using idea.
 */
public class RunOnNightly implements IgnoreCondition {
    @Override
    public boolean isSatisfied() {
        return !RunOnNightly.isNightly();
    }

    public static boolean isNightly() {
        return !(System.getProperty("nightly") == null && !(System.getProperty("sun.java.command") != null && System.getProperty("sun.java.command").contains("com.intellij.rt.execution.junit.JUnitStarter")));
    }
}
