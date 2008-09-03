package com.l7tech.gateway.config.manager.db;

import java.util.Set;
import java.util.Map;

/**
 * version 4.5 checker
 * <p>
 * Note that this checker is identical to the DbVersion44Checker except for getVersion().
 */
public class DbVersion45Checker extends DbVersionChecker {
    private static final String POLICY_TABLE_NAME = "policy";
    private static final String GUID_COLUMN = "guid";
    private static final String INTERNAL_TAG = "internal_tag";

    private static final String SERVICE_METRICS_TABLE_NAME = "service_metrics";
    private static final String SERVICE_STATE = "service_state";

    private static final String WSDM_SUBSCRIPTION_TABLE_NAME = "wsdm_subscription";

    private static final String REVOCATION_CHECK_POLICY_TABLE = "revocation_check_policy";
    private static final String CONTINUE_SERVER_UNAVAILABLE = "continue_server_unavailable";

    public boolean doCheck(Map<String, Set<String>> tableData) {
        boolean passed = false;

        if (tableData != null) {
            Set<String> data = tableData.get(POLICY_TABLE_NAME);
            if(data != null) {
                passed = data.contains(GUID_COLUMN.toLowerCase()) && data.contains(INTERNAL_TAG.toLowerCase());
            }

            if(passed) {
                data = tableData.get(SERVICE_METRICS_TABLE_NAME);
                if(data != null) {
                    passed = data.contains(SERVICE_STATE.toLowerCase());
                }
            }

            if(passed) {
                passed = tableData.containsKey(WSDM_SUBSCRIPTION_TABLE_NAME);
            }

            //check if the REVOCATION_CHECK_POLICY_TABLE contains the new column: DONT_REVOKE_ON_NETWORK_FAILURE
            if(passed) {
                data = tableData.get(REVOCATION_CHECK_POLICY_TABLE);
                if ( data != null ) {
                    passed = data.contains(CONTINUE_SERVER_UNAVAILABLE.toLowerCase());
                }
            }
        }
        return passed;
    }

    public String getVersion() {
        return "4.5.0";
    }
}
