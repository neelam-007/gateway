package com.l7tech.skunkworks.rest.tools;

import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.RunOnNightly;
import org.junit.BeforeClass;
import org.junit.Rule;

public abstract class RestEntityTestBase {

    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private static DatabaseBasedRestManagementEnvironment databaseBasedRestManagementEnvironment;

    @BeforeClass
    public static void beforeClass() throws PolicyAssertionException, IllegalAccessException, InstantiationException {
        //need to only start this on a nightly build
        if(!RunOnNightly.class.newInstance().isSatisfied() && databaseBasedRestManagementEnvironment == null){
            databaseBasedRestManagementEnvironment = new DatabaseBasedRestManagementEnvironment();
        }
    }

    public static DatabaseBasedRestManagementEnvironment getDatabaseBasedRestManagementEnvironment() {
        return databaseBasedRestManagementEnvironment;
    }
}
