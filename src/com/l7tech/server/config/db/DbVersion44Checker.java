package com.l7tech.server.config.db;

import java.util.Set;
import java.util.Map;

/**
 * Version checker for 4.4
 */
public class DbVersion44Checker extends DbVersionChecker {
    public static final String CONNECTOR_TABLE = "connector";
    private static final String JMS_ENDPOINTS_TABLE_NAME = "jms_endpoint";
    private static final String REPLY_TO_QUEUE_NAME_COLUMN ="reply_to_queue_name";
    private static final String OUTBOUND_MESSAGE_TYPE_COLUMN = "outbound_message_type";
    private static final String DISABLED_COLUMN = "disabled";

    public boolean doCheck(Map<String, Set<String>> tableData) {
        boolean passed = false;

        if (tableData != null) {
            Set<String> data = tableData.get(JMS_ENDPOINTS_TABLE_NAME);
            if (data != null) {
                passed =  data.contains(REPLY_TO_QUEUE_NAME_COLUMN.toLowerCase()) &&
                          data.contains(OUTBOUND_MESSAGE_TYPE_COLUMN.toLowerCase()) &&
                          data.contains(DISABLED_COLUMN.toLowerCase());
            }
        }
        return passed;
    }

    public String getVersion() {
        return "4.4.0";
    }
}
