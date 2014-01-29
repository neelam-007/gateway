package com.l7tech.skunkworks.rest.tools;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.IgnoreOnDaily;
import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Rule;

public abstract class RestEntityTestBase {

    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    private static DatabaseBasedRestManagementEnvironment databaseBasedRestManagementEnvironment;

    @BeforeClass
    public static void beforeClass() throws PolicyAssertionException, IllegalAccessException, InstantiationException {
        //need to only start this on a nightly build
        if(!IgnoreOnDaily.isDaily() && databaseBasedRestManagementEnvironment == null){
            databaseBasedRestManagementEnvironment = new DatabaseBasedRestManagementEnvironment();
        }
    }

    public static DatabaseBasedRestManagementEnvironment getDatabaseBasedRestManagementEnvironment() {
        return databaseBasedRestManagementEnvironment;
    }

    protected RestResponse processRequest(String uri, HttpMethod method, @Nullable String contentType, String body) throws Exception {
        return databaseBasedRestManagementEnvironment.processRequest(uri, null, method, contentType, body);
    }
}
