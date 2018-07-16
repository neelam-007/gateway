package com.l7tech.skunkworks.rest.tools;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.test.conditional.ConditionalIgnoreRule;
import com.l7tech.test.conditional.IgnoreOnDaily;
import org.jetbrains.annotations.Nullable;
import org.junit.BeforeClass;
import org.junit.Rule;

import java.util.Map;

public abstract class RestEntityTestBase {

    private static DatabaseBasedRestManagementEnvironment databaseBasedRestManagementEnvironment;
    @Rule
    public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    @BeforeClass
    public static void beforeClass() throws Exception {
        //need to only start this on a nightly build
        if(!IgnoreOnDaily.isDaily() && databaseBasedRestManagementEnvironment == null){
            databaseBasedRestManagementEnvironment = new DatabaseBasedRestManagementEnvironment();
        }

        System.out.println("============================================================================================");
        System.out.println("DB environment startup time: " + String.valueOf(getStartupTimeInMs() == -1 ? "NOT STARTED" : getStartupTimeInMs()) + " ms.");
        System.out.println("============================================================================================");
    }

    /**
     * Used for debug purposes.<br/>
     * It's known that startup time could increase in consecutive migration test runs, this is to trace the startup time,
     * and optionally tweak the environments creation timeout using "test.migration.waitTimeMinutes" property.
     */
    public static long getStartupTimeInMs() {
        if ( databaseBasedRestManagementEnvironment != null )
            return databaseBasedRestManagementEnvironment.getStartupTimeInMs();
        return -1;
    }

    public static DatabaseBasedRestManagementEnvironment getDatabaseBasedRestManagementEnvironment() {
        return databaseBasedRestManagementEnvironment;
    }

    protected RestResponse processRequest(String uri, HttpMethod method, @Nullable String contentType, String body) throws Exception {
        return processRequest(uri, method, contentType, body, null);

    }
    protected RestResponse processRequest(String uri, HttpMethod method, @Nullable String contentType, String body, Map<String,String> headers) throws Exception {
        return databaseBasedRestManagementEnvironment.processRequest(uri, null, method, contentType, body, headers);
    }
}
