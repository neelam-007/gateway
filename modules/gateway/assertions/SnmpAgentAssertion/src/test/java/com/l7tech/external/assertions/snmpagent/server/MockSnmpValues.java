package com.l7tech.external.assertions.snmpagent.server;

import java.util.HashMap;

/**
 * User: rseminoff
 * Date: 5/17/12
 *
 * These are constants used by the various mocks for the SnmpAgentAssertion unit tests.
 *
 */
public class MockSnmpValues {

    // These contains the values, commands, etc of what we support and expect in return in order to pass tests.
    // Any values outside of these are used to make the assertion fail.
    public static final String GET_COMMAND = "GET";
    public static final String GET_NEXT_COMMAND = "GETNEXT";

    public static final String SYNTAX_INTEGER32 = "integer";
    public static final String SYNTAX_COUNTER32 = "counter";
    public static final String SYNTAX_STRING    = "string";

    public static final int GET_SERVICE_OID = 1;
    public static final int GET_SERVICE_NAME = 2;
    public static final int GET_REQUESTS_RECEIVED = 3;
    public static final int GET_REQUESTS_AUTHORIZED = 4;
    public static final int GET_REQUESTS_COMPLETED = 5;
    public static final int GET_FAILED_ROUTES_DAY = 6;
    public static final int GET_FAILED_ROUTES_HOUR = 7;
    public static final int GET_FAILED_ROUTES_FINE = 8;
    public static final int GET_AVERAGE_BACKEND_TIME_DAY = 9;
    public static final int GET_AVERAGE_BACKEND_TIME_HOUR = 10;
    public static final int GET_AVERAGE_BACKEND_TIME_FINE = 11;
    public static final int GET_AVERAGE_FRONTEND_TIME_DAY = 12;
    public static final int GET_AVERAGE_FRONTEND_TIME_HOUR = 13;
    public static final int GET_AVERAGE_FRONTEND_TIME_FINE = 14;
    public static final int GET_POLICY_VIOLATIONS_DAY = 15;
    public static final int GET_POLICY_VIOLATIONS_HOUR = 16;
    public static final int GET_POLICY_VIOLATIONS_FINE = 17;

    public static final String TEST_SERVICE_URL = "https://localhost:8080/snmp/management";
    public static final String TEST_SERVICE_ADDRESS = ".1.3.6.1.4.1.17304.7.1";
    public static final String TEST_SERVICE_TABLE = ".1.3.6.1.4.1.17304.7";

    public static final String TEST_SERVICE_GOID_STR = "0000000000000000000000000000c000";  // The GOID in normal representation
    public static final String TEST_SERVICE_GOID_INT = "0.0.0.49152";                       // The GOID represented as uints

    public static final String TEST_SERVICE_NAME = "Layer 7 Unit Test Service";

    public static final int TEST_REQUESTS_RECEIVED_FINE = 6510;
    public static final int TEST_REQUESTS_AUTHORIZED_UNPROCESSED_FINE = 6502;
    public static final int TEST_REQUESTS_AUTHORIZED_PROCESSED_FINE = 6500;

    public static final int TEST_MAX_BACKEND_POLICY_RESPONSE_FINE = 4000;
    public static final int TEST_MIN_BACKEND_POLICY_RESPONSE_FINE = 2000;

    public static final int TEST_MAX_FRONTEND_POLICY_RESPONSE_FINE = 5000;
    public static final int TEST_MIN_FRONTEND_POLICY_RESPONSE_FINE = 3000;

    public static final int FINE_MS_VALUE = 600000; // 10 minutes.
    public static final int TOTAL_FINE_METRICS_PER_HOUR = 6;
    public static final int TOTAL_FINE_METRICS_PER_DAY = TOTAL_FINE_METRICS_PER_HOUR * 24;

    public static final HashMap<Integer, SnmpServiceDetail> snmpServiceMap  = new HashMap<Integer, SnmpServiceDetail> () {{
        put(0, null);   // There is no service zero.
        put(GET_SERVICE_OID, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_STRING;
            expectedValue = TEST_SERVICE_GOID_STR;    // The expected OID return.
        }});
        put(GET_SERVICE_NAME, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_STRING;
            expectedValue = TEST_SERVICE_NAME;
        }});
        put(GET_REQUESTS_RECEIVED, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_COUNTER32;
            expectedValue = Long.toString(TEST_REQUESTS_RECEIVED_FINE);
        }});
        put(GET_REQUESTS_AUTHORIZED, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_COUNTER32;
            expectedValue = Long.toString(TEST_REQUESTS_AUTHORIZED_UNPROCESSED_FINE);
        }});
        put(GET_REQUESTS_COMPLETED, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_COUNTER32;
            expectedValue = Long.toString(TEST_REQUESTS_AUTHORIZED_PROCESSED_FINE);
        }});
        put(GET_FAILED_ROUTES_DAY, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_INTEGER32;
            expectedValue = Integer.toString(TEST_REQUESTS_AUTHORIZED_UNPROCESSED_FINE - TEST_REQUESTS_AUTHORIZED_PROCESSED_FINE);
        }});
        put(GET_FAILED_ROUTES_HOUR, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_INTEGER32;
            expectedValue = Integer.toString(TEST_REQUESTS_AUTHORIZED_UNPROCESSED_FINE - TEST_REQUESTS_AUTHORIZED_PROCESSED_FINE);
        }});
        put(GET_FAILED_ROUTES_FINE, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_INTEGER32;
            expectedValue = Integer.toString(TEST_REQUESTS_AUTHORIZED_UNPROCESSED_FINE - TEST_REQUESTS_AUTHORIZED_PROCESSED_FINE);
        }});
        put(GET_AVERAGE_BACKEND_TIME_DAY, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_INTEGER32;
            expectedValue = "2003";   // Calculate later - the day
        }});
        put(GET_AVERAGE_BACKEND_TIME_HOUR, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_INTEGER32;
            expectedValue = "2003"; // Calculate later - the latest hour
        }});
        put(GET_AVERAGE_BACKEND_TIME_FINE, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_INTEGER32;
            expectedValue = "2003";   // Calculate later - the latest 10 min stretch.
        }});
        put(GET_AVERAGE_FRONTEND_TIME_DAY, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_INTEGER32;
            expectedValue = "3000";   // Calculate later
        }});
        put(GET_AVERAGE_FRONTEND_TIME_HOUR, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_INTEGER32;
            expectedValue = "3000";   // Calculate later
        }});
        put(GET_AVERAGE_FRONTEND_TIME_FINE, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_INTEGER32;
            expectedValue = "3000";   // Calculate later
        }});
        put(GET_POLICY_VIOLATIONS_DAY, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_INTEGER32;
            expectedValue = "8";
        }});
        put(GET_POLICY_VIOLATIONS_HOUR, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_INTEGER32;
            expectedValue = "8";
        }});
        put(GET_POLICY_VIOLATIONS_FINE, new SnmpServiceDetail() {{
            syntaxType = SYNTAX_INTEGER32;
            expectedValue = "8";
        }});

    }};


    public static class SnmpServiceDetail {

        public String syntaxType;
        public String   expectedValue;

    }

}
