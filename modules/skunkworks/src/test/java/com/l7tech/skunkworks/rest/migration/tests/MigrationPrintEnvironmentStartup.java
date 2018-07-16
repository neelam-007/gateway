package com.l7tech.skunkworks.rest.migration.tests;

import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import org.junit.Test;

/**
 * Just a dummy test class to print the source and target environment startup times.<br/>
 * It seems {@code System.out.print} doesn't work inside {@code @BeforeClass} i.e. the output cannot be found in the teamcity build log.<br/>
 * Default timeout for the source and target environments is 10min, and can be tweaked with {@code "test.migration.waitTimeMinutes"}.
 * <p/>
 * IMPORTANT:<br/>
 * If the values printed below become larger then the timeout, then consider increasing the wait time using
 * {@code "test.migration.waitTimeMinutes"}, otherwise nightly tests will fail.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class MigrationPrintEnvironmentStartup extends MigrationTestBase {
    @Test
    public void printEnvironmentStartupTimes() throws Exception {
        System.out.println("============================================================================================");
        System.out.println("Environment startup times (from suite):");
        System.out.println("============================================================================================");
        System.out.println("Source: " + String.valueOf(MigrationTestBase.getSourceEnvironmentStartupTime() == -1 ? "NOT STARTED" : MigrationTestBase.getSourceEnvironmentStartupTime()) + " ms.");
        System.out.println("Target: " + String.valueOf(MigrationTestBase.getTargetEnvironmentStartupTime() == -1 ? "NOT STARTED" : MigrationTestBase.getTargetEnvironmentStartupTime()) + " ms.");
        System.out.println("============================================================================================");
    }
}
