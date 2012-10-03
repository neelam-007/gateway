package com.l7tech.portal.reports;

import com.l7tech.portal.reports.definition.ApiUsageReportDefinition;
import com.l7tech.portal.reports.format.Format;
import com.l7tech.portal.reports.parameter.ApiUsageReportParameters;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.*;

import static org.junit.Assert.*;

public class UsageComparisonReportGeneratorTest extends AbstractReportGeneratorTestUtility {
    private List<Long> apiIds;
    private List<String> applicationKeys;
    private ApiUsageReportParameters usageParameters;
    private ArrayList<String> uuids;

    @Before
    public void setup() throws Exception{
        setupAbstractReportGeneratorTest();
        apiIds = new ArrayList<Long>();
        applicationKeys = new ArrayList<String>();
        uuids = new ArrayList<String>();
        // only mysql does not require all non-aggregate select columns to be present in the group by clause
        //ApiUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_API_ID = ApiUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_API_ID.replace("group by api_id, bin_start_time", "group by api_id, bin_start_time, api_name, api_uri");
        ApiUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_UUID = ApiUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_UUID.replace("group by api_id, bin_start_time", "group by api_id, bin_start_time, api_name, api_uri");
    }

    @After
    public void teardown() throws Exception {
        teardownAbstractReportGeneratorTest();
    }

    @Test(expected = IllegalArgumentException.class)
    public void getApiUsageComparisonReportNullParams() throws Exception {
        generator.generateApiUsageComparisonReport(null);
    }

    @Test
    public void getApiUsageComparisonReportXml() throws Exception {
        apiIds.add(1L);
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.XML, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId, 1);

        final String data = generator.generateApiUsageComparisonReport(usageParameters);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 1);
        assertXPathTextContent(document, "//sum_hits_total", "1");
        assertXPathTextContent(document, "//uuid", "1234abcd");
        assertXPathTextContent(document, "//api_name", "test service");
    }

    @Test
    public void getApiUsageComparisonReportJson() throws Exception {
        apiIds.add(1L);
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.JSON, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId, 1);

        final String data = generator.generateApiUsageComparisonReport(usageParameters);

        final JSONArray jsonArray = new JSONArray(data);
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(1L, usage.getLong("sum_hits_total"));
        assertEquals("1234abcd", usage.getString("uuid"));
        assertEquals("test service", usage.getString("api_name"));
    }

    @Test
    public void getApiUsageComparisonReportXmlMultipleApiIds() throws Exception {
        apiIds.add(1L);
        apiIds.add(2L);
        uuids.add("1234abcd");
        uuids.add("1234efgh");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.XML, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);

        insertPublishedService(connection, 2, "1234efgh");
        final long generatedId2 = insertServiceMetric(connection, 2, 1, 1, "1234efgh");
        insertServiceMetricDetail(connection, generatedId2, 1);

        final String data = generator.generateApiUsageComparisonReport(usageParameters);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 2);
        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/uuid", "1234abcd");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/api_name", "test service");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/uuid", "1234efgh");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/api_name", "test service");
    }

    @Test
    public void getApiUsageComparisonReportJsonMultipleApApiIds() throws Exception {
        apiIds.add(1L);
        apiIds.add(2L);
        uuids.add("1234abcd");
        uuids.add("1234efgh");

        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.JSON, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);

        insertPublishedService(connection, 2, "1234efgh");
        final long generatedId2 = insertServiceMetric(connection, 2, 1, 1, "1234efgh");
        insertServiceMetricDetail(connection, generatedId2, 1);

        final String data = generator.generateApiUsageComparisonReport(usageParameters);

        final JSONArray jsonArray = new JSONArray(data);
        assertEquals(2, jsonArray.length());
        final JSONObject usage1 = (JSONObject) jsonArray.get(0);
        assertEquals(1L, usage1.getLong("sum_hits_total"));
        assertEquals("1234abcd", usage1.getString("uuid"));
        assertEquals("test service", usage1.getString("api_name"));
        final JSONObject usage2 = (JSONObject) jsonArray.get(1);
        assertEquals(1L, usage2.getLong("sum_hits_total"));
        assertEquals("1234efgh", usage2.getString("uuid"));
        assertEquals("test service", usage2.getString("api_name"));
    }

    @Test
    public void getApiUsageComparisonReportFilterByApiKeys() throws Exception {
        apiIds.add(1L);
        uuids.add("1234abcd");
        applicationKeys.add("key1");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.XML, uuids, applicationKeys);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId, 1);

        insertMappingValue(connection, 2, 1, Arrays.asList("key2", "method2"));
        insertServiceMetricDetail(connection, generatedId, 2);

        final String dataOneKey = generator.generateApiUsageComparisonReport(usageParameters);

        final Document documentOneKey = buildDocumentFromXml(dataOneKey);
        assertNumberOfNodes(documentOneKey, "UsageReport/Usage", 1);
        assertXPathTextContent(documentOneKey, "//sum_hits_total", "1");
        assertXPathTextContent(documentOneKey, "//uuid", "1234abcd");
        assertXPathTextContent(documentOneKey, "//api_name", "test service");

        applicationKeys.add("key2");

        final String dataTwoKeys = generator.generateApiUsageComparisonReport(usageParameters);

        final Document documentTwoKeys = buildDocumentFromXml(dataTwoKeys);
        assertNumberOfNodes(documentTwoKeys, "UsageReport/Usage", 1);
        assertXPathTextContent(documentTwoKeys, "//sum_hits_total", "2");
        assertXPathTextContent(documentTwoKeys, "//uuid", "1234abcd");
        assertXPathTextContent(documentTwoKeys, "//api_name", "test service");
    }

    @Test
    public void getApiUsageComparisonReportXmlNoData() throws Exception {
        apiIds.add(1L);
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.XML, uuids, null);

        final String data = generator.generateApiUsageComparisonReport(usageParameters);

        assertEquals("<UsageReport/>", data.trim());
    }

    @Test
    public void getApiUsageComparisonReportJsonNoData() throws Exception {
        apiIds.add(1L);
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.JSON, uuids, null);

        final String data = generator.generateApiUsageComparisonReport(usageParameters);

        assertEquals("[]", data);
    }

    /**
     * FIXME params with api ids are broken.
     */
    @Ignore
    @Test(expected = ReportGenerationException.class)
    public void getApiUsageComparisonReportCannotExecuteQuery() throws Exception {
        connection.createStatement().execute("drop view api_usage_view");
        connection.createStatement().execute("create view api_usage_view as select * from service_metrics");

        apiIds.add(1L);
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.JSON, apiIds, null);

        generator.generateApiUsageComparisonReport(usageParameters);
    }
}
