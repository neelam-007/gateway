package com.l7tech.test.conditional;

/**
 * This will run the annotated unit test or class on builds that are not the daily builds.
 * It will also run the test locally when run using idea.
 */
public class IgnoreOnDaily implements IgnoreCondition {
    @Override
    public boolean isSatisfied() {
        return IgnoreOnDaily.isDaily();
    }

    public static boolean isDaily() {
        String buildType = System.getProperty("build.type");
        return buildType != null && "daily".equalsIgnoreCase(buildType);
    }
}
