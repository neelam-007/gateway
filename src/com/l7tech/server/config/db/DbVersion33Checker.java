package com.l7tech.server.config.db;

import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: megery
 * Date: Nov 28, 2005
 * Time: 12:55:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class DbVersion33Checker extends DbVersionChecker {
    public static final String TRUSTED_CERT_TABLE = "trusted_cert";
    public static final String CLIENT_CERT_TABLE = "client_cert";
    public static final String COMM_SCHEMAS_TABLE = "community_schemas";
    public static final String CLUSTER_PROP_TABLE = "cluster_properties";
    public static final String SAMPLE_MSG_TABLE = "sample_messages";

    public boolean doCheck(Map<String, Set<String>> tableData) {
        boolean passed = false;
        if (tableData != null) {
            Set<String> trustedCertColumns = tableData.get(TRUSTED_CERT_TABLE);
            Set<String> clientCertColumns = tableData.get(CLIENT_CERT_TABLE);
            Set<String> commSchemaColumns = tableData.get(COMM_SCHEMAS_TABLE);
            Set<String> clusterPropColumns = tableData.get(CLUSTER_PROP_TABLE);
            Set<String> sampleMsgColumns = tableData.get(SAMPLE_MSG_TABLE);

            passed =  (trustedCertColumns != null) &&
                                (clientCertColumns != null) &&
                                (commSchemaColumns != null) &&
                                (clusterPropColumns != null) &&
                                (sampleMsgColumns != null);
        }

        return passed;
    }

    public String getVersion() {
        return "3.3";
    }
}
