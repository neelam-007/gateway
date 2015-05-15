package com.l7tech.external.assertions.odata.server;

import com.l7tech.external.assertions.odata.server.producer.jdbc.Util;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author rraquepo, 9/8/14
 */
public class UtilTest {
    @Test
    public void testNormalizeQuota() throws Exception {
        String normalizeQuery = Util.normalizeQuery(CUSTOM_QUERY_QUOTA);
        assertEquals(EXPECTED_NORMALIZED_QUERY_QUOTA, normalizeQuery);
    }

    @Test
    public void testParseFieldsFromQueryQuota() throws Exception {
        List<String> fields = Util.parseFieldsFromQuery(Util.normalizeQuery(CUSTOM_QUERY_QUOTA));
        assertEquals(" Should match expected field ", EXPECTED_FIELDS_QUOTA.length, fields.size());
        for (int i = 0; i < fields.size(); i++) {
            String field1 = fields.get(i);
            String field2 = EXPECTED_FIELDS_QUOTA[i];
            assertTrue(i + "th field should match (" + field1 + "," + field2 + ")", field1.endsWith(field2));
        }
    }

    @Test
    public void testNormalizeLatency() throws Exception {
        String normalizeQuery = Util.normalizeQuery(CUSTOM_QUERY_LATENCY);
        assertEquals(EXPECTED_NORMALIZED_QUERY_LATENCY, normalizeQuery);
    }

    @Test
    public void testParseFieldsFromQueryLatency() throws Exception {
        List<String> fields = Util.parseFieldsFromQuery(Util.normalizeQuery(CUSTOM_QUERY_LATENCY));
        assertEquals(" Should match expected field ", EXPECTED_FIELDS_LATENCY.length, fields.size());
        for (int i = 0; i < fields.size(); i++) {
            String field1 = fields.get(i);
            String field2 = EXPECTED_FIELDS_LATENCY[i];
            assertTrue(i + "th field should match (" + field1 + "," + field2 + ")", field1.endsWith(field2));
        }
    }

    @Test
    public void testParseField() throws Exception {
        List<String> fields = Util.parseFieldsFromQuery(Util.normalizeQuery("SELECT DATE_FORMAT(FROM_UNIXTIME((SSG_REQUEST_START_TIME/1000)),'%M %e,%Y') as TIME, concat(ROUND((count(API_METRICS.UUID)/QUOTA)*100),'%') as QUOTA_USAGE_PERCENTAGE, dummy_func(1,2,INNER_FUNCT(3,4)) as dummy_field  FROM DUAL"));
        assertEquals(" Should match expected field ", 3, fields.size());
        assertEquals("DATE_FORMAT(FROM_UNIXTIME((SSG_REQUEST_START_TIME/1000)),'%M %e,%Y') as TIME", fields.get(0));
        assertEquals("concat(ROUND((count(API_METRICS.UUID)/QUOTA)*100),'%') as QUOTA_USAGE_PERCENTAGE", fields.get(1));
        assertEquals("dummy_func(1,2,INNER_FUNCT(3,4)) as dummy_field", fields.get(2));
    }

    @Test
    public void testUnPascalized() throws Exception {
        String test_string = "ATestField";
        String expected = "A_Test_Field";
        assertEquals(expected.toUpperCase(), Util.unPascalize(test_string).toUpperCase());
    }


    public final String CUSTOM_QUERY_QUOTA = "\tSELECT '0' AS UUID, \n" +
            "\tSSG_REQUEST_START_TIME, \n" +
            "\tORGANIZATION.NAME as ORGANIZATION_NAME, \n" +
            "\tAPI_METRICS.ORGANIZATION_UUID, \n" +
            "\tACCOUNT_PLAN.NAME as ACCOUNT_PLAN_NAME, \n" +
            "\tACCOUNT_PLAN_UUID, \n" +
            "\tcount(API_METRICS.UUID) as HITS_COUNT,\n" +
            "\tcount(API_METRICS.UUID)/QUOTA as QUOTA_USAGE,\n" +
            "\tconcat(ROUND((count(API_METRICS.UUID)/QUOTA)*100),'%') as QUOTA_USAGE_PERCENTAGE, \n" +
            "\tQUOTA,\n" +
            "\tQUOTA_INTERVAL\n" +
            "\tFROM API_METRICS \n" +
            "\tLEFT JOIN API ON API_METRICS.SSG_PORTAL_API_ID = API.UUID \n" +
            "\tLEFT JOIN ORGANIZATION ON API_METRICS.ORGANIZATION_UUID = ORGANIZATION.UUID\n" +
            "\tLEFT JOIN ACCOUNT_PLAN ON API_METRICS.ACCOUNT_PLAN_UUID = ACCOUNT_PLAN.UUID\n";

    public final String[] EXPECTED_FIELDS_QUOTA = "UUID,SSG_REQUEST_START_TIME,ORGANIZATION_NAME,ORGANIZATION_UUID,ACCOUNT_PLAN_NAME,ACCOUNT_PLAN_UUID,HITS_COUNT,QUOTA_USAGE,QUOTA_USAGE_PERCENTAGE,QUOTA,QUOTA_INTERVAL".split(",");

    public final String EXPECTED_NORMALIZED_QUERY_QUOTA = "SELECT '0' AS UUID, SSG_REQUEST_START_TIME, ORGANIZATION.NAME as ORGANIZATION_NAME, API_METRICS.ORGANIZATION_UUID, ACCOUNT_PLAN.NAME as ACCOUNT_PLAN_NAME, ACCOUNT_PLAN_UUID, count(API_METRICS.UUID) as HITS_COUNT, count(API_METRICS.UUID)/QUOTA as QUOTA_USAGE, concat(ROUND((count(API_METRICS.UUID)/QUOTA)*100),'%') as QUOTA_USAGE_PERCENTAGE, QUOTA, QUOTA_INTERVAL FROM API_METRICS LEFT JOIN API ON API_METRICS.SSG_PORTAL_API_ID = API.UUID LEFT JOIN ORGANIZATION ON API_METRICS.ORGANIZATION_UUID = ORGANIZATION.UUID LEFT JOIN ACCOUNT_PLAN ON API_METRICS.ACCOUNT_PLAN_UUID = ACCOUNT_PLAN.UUID";

    public final String CUSTOM_QUERY_LATENCY = "\tSELECT '0' AS UUID, SSG_REQUEST_START_TIME, \n" +
            "\tDATE_FORMAT(FROM_UNIXTIME((SSG_REQUEST_START_TIME/1000)),'%M %e,%Y') as TIME, \n" +
            "\tSSG_PORTAL_API_ID,API.NAME as API_NAME, \n" +
            "\tORGANIZATION.NAME as ORGANIZATION_NAME, API_METRICS.ORGANIZATION_UUID, \n" +
            "\tAPPLICATION.NAME as APPLICATION_NAME, APPLICATION_UUID, \n" +
            "\tAVG(TOTAL_LATENCY) as TOTAL_LATENCY, AVG(BACKEND_LATENCY) as BACKEND_LATENCY, AVG(PROXY_LATENCY) as PROXY_LATENCY,\n" +
            "\tMIN(TOTAL_LATENCY) as MIN_TOTAL_LATENCY, MIN(BACKEND_LATENCY) as MIN_BACKEND_LATENCY, MIN(PROXY_LATENCY) as MIN_PROXY_LATENCY,\n" +
            "\tMAX(TOTAL_LATENCY) as MAX_TOTAL_LATENCY, MAX(BACKEND_LATENCY) as MAX_BACKEND_LATENCY, MAX(PROXY_LATENCY) as MAX_PROXY_LATENCY\n" +
            "\tFROM API_METRICS \n" +
            "\tLEFT JOIN API ON API_METRICS.SSG_PORTAL_API_ID = API.UUID \n" +
            "\tLEFT JOIN ORGANIZATION ON API_METRICS.ORGANIZATION_UUID = ORGANIZATION.UUID\n" +
            "\tLEFT JOIN APPLICATION ON API_METRICS.APPLICATION_UUID = APPLICATION.UUID\n";

    public final String[] EXPECTED_FIELDS_LATENCY = "UUID,SSG_REQUEST_START_TIME,TIME,SSG_PORTAL_API_ID,API_NAME,ORGANIZATION_NAME,ORGANIZATION_UUID,APPLICATION_NAME,APPLICATION_UUID,TOTAL_LATENCY,BACKEND_LATENCY,PROXY_LATENCY,MIN_TOTAL_LATENCY,MIN_BACKEND_LATENCY,MIN_PROXY_LATENCY,MAX_TOTAL_LATENCY,MAX_BACKEND_LATENCY,MAX_PROXY_LATENCY".split(",");

    public final String EXPECTED_NORMALIZED_QUERY_LATENCY = "SELECT '0' AS UUID, SSG_REQUEST_START_TIME, DATE_FORMAT(FROM_UNIXTIME((SSG_REQUEST_START_TIME/1000)),'%M %e,%Y') as TIME, SSG_PORTAL_API_ID,API.NAME as API_NAME, ORGANIZATION.NAME as ORGANIZATION_NAME, API_METRICS.ORGANIZATION_UUID, APPLICATION.NAME as APPLICATION_NAME, APPLICATION_UUID, AVG(TOTAL_LATENCY) as TOTAL_LATENCY, AVG(BACKEND_LATENCY) as BACKEND_LATENCY, AVG(PROXY_LATENCY) as PROXY_LATENCY, MIN(TOTAL_LATENCY) as MIN_TOTAL_LATENCY, MIN(BACKEND_LATENCY) as MIN_BACKEND_LATENCY, MIN(PROXY_LATENCY) as MIN_PROXY_LATENCY, MAX(TOTAL_LATENCY) as MAX_TOTAL_LATENCY, MAX(BACKEND_LATENCY) as MAX_BACKEND_LATENCY, MAX(PROXY_LATENCY) as MAX_PROXY_LATENCY FROM API_METRICS LEFT JOIN API ON API_METRICS.SSG_PORTAL_API_ID = API.UUID LEFT JOIN ORGANIZATION ON API_METRICS.ORGANIZATION_UUID = ORGANIZATION.UUID LEFT JOIN APPLICATION ON API_METRICS.APPLICATION_UUID = APPLICATION.UUID";
}
