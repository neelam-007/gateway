package com.l7tech.portal.reports;

import com.l7tech.portal.reports.definition.ApiUsageReportDefinition;
import com.l7tech.portal.reports.format.Format;
import com.l7tech.portal.reports.parameter.ApiUsageReportParameters;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.*;

import static org.junit.Assert.*;

public class UsageReportGeneratorTest extends AbstractReportGeneratorTestUtility {
    private List<String> applicationKeys;
    private ArrayList<String> uuids;
    private ApiUsageReportParameters usageParameters;

    @Before
    public void setup() throws Exception{
        setupAbstractReportGeneratorTest();
        applicationKeys = new ArrayList<String>();
        uuids = new ArrayList<String>();
        // only mysql does not require all non-aggregate select columns to be present in the group by clause
        ApiUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_UUID = ApiUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_UUID.replace("group by bin_start_time , uuid", "group by bin_start_time, uuid, api_name, api_uri");
        ApiUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_UUID_FILTER_BY_API_KEY = ApiUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_UUID_FILTER_BY_API_KEY.replace("group by bin_start_time , uuid", "group by bin_start_time, uuid, api_name, api_uri");
        // hsql equivalent of mysql's 'IF' is 'CASEWHEN'
        ApiUsageReportDefinition.SQL_TEMPLATE = ApiUsageReportDefinition.SQL_TEMPLATE.replaceAll("if", "casewhen");
        ApiUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_UUID = ApiUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_UUID.replaceAll("if", "casewhen");
        ApiUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_UUID_FILTER_BY_API_KEY = ApiUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_UUID_FILTER_BY_API_KEY.replaceAll("if", "casewhen");
        ApiUsageReportDefinition.SQL_TEMPLATE_FILTER_BY_UUID = ApiUsageReportDefinition.SQL_TEMPLATE_FILTER_BY_UUID.replaceAll("if", "casewhen");
    }

    @After
    public void teardown() throws Exception {
        teardownAbstractReportGeneratorTest();
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNullDataSource() {
        generator = new MetricsReportGenerator(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getUsageReportNullParams() throws Exception {
        generator.generateApiUsageReport(null, true, null, null);
    }

    /**
     * Does not filter by key
     * <p/>
     * Groups data by uuid
     */
    @Test
    public void getUsageReportXml() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.XML, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId, 1);

        final String data = generator.generateApiUsageReport(usageParameters, true, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/uuid", 1);
        assertNumberOfNodes(document, "UsageReport/uuid/Usage", 1);
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/api_name", "test service");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/bin_start_time", "1");
    }

    /**
     * Does not filter by key.
     * <p/>
     * Groups data by uuid.
     */
    @Test
    public void getUsageReportJson() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.JSON, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId, 1);

        final String data = generator.generateApiUsageReport(usageParameters, true, null, null);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());
        final JSONArray jsonArray = jsonObject.getJSONArray("1234abcd");
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(1L, usage.getLong("sum_hits_total"));
        assertEquals(1L, usage.getLong("sum_hits_success"));
        assertEquals(0, usage.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage.getLong("sum_latency"));
        assertEquals(0, usage.getLong("sum_gateway_latency"));
        assertEquals(1L, usage.getLong("sum_back_latency"));
        assertEquals("test service", usage.getString("api_name"));
        assertEquals(1L, usage.getLong("bin_start_time"));
    }

    /**
     * Does not filter by key.
     * <p/>
     * Groups data by uuid.
     */
    @Test
    public void getUsageReportXmlMultipleUUIDs() throws Exception {
        uuids.add("1234abcd");
        uuids.add("1234abcde");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.XML, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        insertPublishedService(connection, 2, "1234abcde");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 2, 1, 1, "1234abcde");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        insertServiceMetricDetail(connection, generatedId2, 1);

        final String data = generator.generateApiUsageReport(usageParameters, true, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/uuid", 2);
        assertNumberOfNodes(document, "UsageReport/uuid/Usage", 2);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_success", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/api_name", "test service");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/bin_start_time", "1");

        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/sum_hits_success", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/api_name", "test service");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/bin_start_time", "1");
    }

    /**
     * Does not filter by key.
     * <p/>
     * Groups data by uuid.
     * <p/>
     * Should add rows for missing groups.
     */
    @Test
    public void getUsageReportXmlMultipleUUIDsAddsMissingGroups() throws Exception {
        uuids.add("1234abcd");
        uuids.add("1234abcde");
        uuids.add("missing");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.XML, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        insertPublishedService(connection, 2, "1234abcde");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 2, 1, 1, "1234abcde");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        insertServiceMetricDetail(connection, generatedId2, 1);

        final String data = generator.generateApiUsageReport(usageParameters, true, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/uuid", 3);
        assertNumberOfNodes(document, "UsageReport/uuid[@value='missing']", 1);
        assertNumberOfNodes(document, "UsageReport/uuid[@value='missing']/Usage", 0);
        assertNumberOfNodes(document, "UsageReport/uuid/Usage", 2);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_success", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/api_name", "test service");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage/bin_start_time", "1");

        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/sum_hits_success", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/api_name", "test service");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage/bin_start_time", "1");
    }

    /**
     * Does not filter by key.
     * <p/>
     * Does not group data.
     */
    @Test
    public void getUsageReportXmlMultipleUUIDsDoNotSeparateByUUID() throws Exception {
        uuids.add("1234abcd");
        uuids.add("1234abcde");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.XML, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        insertPublishedService(connection, 2, "1234abcde");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 2, 1, 1, "1234abcde");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        insertServiceMetricDetail(connection, generatedId2, 1);

        final String data = generator.generateApiUsageReport(usageParameters, false, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 1);
        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_hits_total", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_hits_success", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/bin_start_time", "1");
    }

    /**
     * Does not filter by key.
     * <p/>
     * Groups data by uuid.
     */
    @Test
    public void getUsageReportJsonMultipleUUIDs() throws Exception {
        uuids.add("1234abcd");
        uuids.add("1234abcde");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.JSON, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        insertPublishedService(connection, 2, "1234abcde");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 2, 1, 1, "1234abcde");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        insertServiceMetricDetail(connection, generatedId2, 1);

        final String data = generator.generateApiUsageReport(usageParameters, true, null, null);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(2, jsonObject.length());

        final JSONArray jsonArray1 = jsonObject.getJSONArray("1234abcd");
        assertEquals(1, jsonArray1.length());
        final JSONObject usage1 = (JSONObject) jsonArray1.get(0);
        assertEquals(1L, usage1.getLong("sum_hits_total"));
        assertEquals(1L, usage1.getLong("sum_hits_success"));
        assertEquals(0, usage1.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage1.getLong("sum_latency"));
        assertEquals(0, usage1.getLong("sum_gateway_latency"));
        assertEquals(1L, usage1.getLong("sum_back_latency"));
        assertEquals("test service", usage1.getString("api_name"));
        assertEquals(1L, usage1.getLong("bin_start_time"));

        final JSONArray jsonArray2 = jsonObject.getJSONArray("1234abcde");
        assertEquals(1, jsonArray1.length());
        final JSONObject usage2 = (JSONObject) jsonArray2.get(0);
        assertEquals(1L, usage2.getLong("sum_hits_total"));
        assertEquals(1L, usage2.getLong("sum_hits_success"));
        assertEquals(0, usage2.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage2.getLong("sum_latency"));
        assertEquals(0, usage2.getLong("sum_gateway_latency"));
        assertEquals(1L, usage2.getLong("sum_back_latency"));
        assertEquals("test service", usage2.getString("api_name"));
        assertEquals(1L, usage2.getLong("bin_start_time"));
    }

    /**
     * Does not filter by key.
     * <p/>
     * Groups data by uuid.
     * <p/>
     * Adds rows for missing groups.
     */
    @Test
    public void getUsageReportJsonMultipleUUIDsAddsMissingGroups() throws Exception {
        uuids.add("1234abcd");
        uuids.add("1234abcde");
        uuids.add("missing");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.JSON, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        insertPublishedService(connection, 2, "1234abcde");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 2, 1, 1, "1234abcde");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        insertServiceMetricDetail(connection, generatedId2, 1);

        final String data = generator.generateApiUsageReport(usageParameters, true, null, null);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(3, jsonObject.length());

        final JSONArray jsonArray1 = jsonObject.getJSONArray("1234abcd");
        assertEquals(1, jsonArray1.length());
        final JSONObject usage1 = (JSONObject) jsonArray1.get(0);
        assertEquals(1L, usage1.getLong("sum_hits_total"));
        assertEquals(1L, usage1.getLong("sum_hits_success"));
        assertEquals(0, usage1.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage1.getLong("sum_latency"));
        assertEquals(0, usage1.getLong("sum_gateway_latency"));
        assertEquals(1L, usage1.getLong("sum_back_latency"));
        assertEquals("test service", usage1.getString("api_name"));
        assertEquals(1L, usage1.getLong("bin_start_time"));

        final JSONArray jsonArray2 = jsonObject.getJSONArray("1234abcde");
        assertEquals(1, jsonArray1.length());
        final JSONObject usage2 = (JSONObject) jsonArray2.get(0);
        assertEquals(1L, usage2.getLong("sum_hits_total"));
        assertEquals(1L, usage2.getLong("sum_hits_success"));
        assertEquals(0, usage2.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage2.getLong("sum_latency"));
        assertEquals(0, usage2.getLong("sum_gateway_latency"));
        assertEquals(1L, usage2.getLong("sum_back_latency"));
        assertEquals("test service", usage2.getString("api_name"));
        assertEquals(1L, usage2.getLong("bin_start_time"));

        final JSONArray jsonArray3 = jsonObject.getJSONArray("missing");
        assertEquals(0, jsonArray3.length());
    }

    /**
     * Does not filter by key.
     * <p/>
     * Groups data by uuid.
     */
    @Test
    public void getUsageReportJsonMultipleRowsPerUUID() throws Exception {
        uuids.add("1234abcd");
        uuids.add("1234abcde");
        usageParameters = new ApiUsageReportParameters(0, 5, 1, Format.JSON, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        insertPublishedService(connection, 2, "1234abcde");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 1, 2, 2, "1234abcd");
        final long generatedId3 = insertServiceMetric(connection, 2, 1, 1, "1234abcde");
        final long generatedId4 = insertServiceMetric(connection, 2, 2, 2, "1234abcde");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        insertServiceMetricDetail(connection, generatedId2, 1);
        insertServiceMetricDetail(connection, generatedId3, 1);
        insertServiceMetricDetail(connection, generatedId4, 1);

        final String data = generator.generateApiUsageReport(usageParameters, true, null, null);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(2, jsonObject.length());

        final JSONArray jsonArray1 = jsonObject.getJSONArray("1234abcd");
        assertEquals(2, jsonArray1.length());
        final JSONObject usage1a = (JSONObject) jsonArray1.get(0);
        assertEquals(1L, usage1a.getLong("sum_hits_total"));
        assertEquals(1L, usage1a.getLong("sum_hits_success"));
        assertEquals(0, usage1a.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage1a.getLong("sum_latency"));
        assertEquals(0, usage1a.getLong("sum_gateway_latency"));
        assertEquals(1L, usage1a.getLong("sum_back_latency"));
        assertEquals("test service", usage1a.getString("api_name"));
        assertEquals(1L, usage1a.getLong("bin_start_time"));
        final JSONObject usage1b = (JSONObject) jsonArray1.get(1);
        assertEquals(1L, usage1b.getLong("sum_hits_total"));
        assertEquals(1L, usage1b.getLong("sum_hits_success"));
        assertEquals(0, usage1b.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage1b.getLong("sum_latency"));
        assertEquals(0, usage1b.getLong("sum_gateway_latency"));
        assertEquals(1L, usage1b.getLong("sum_back_latency"));
        assertEquals("test service", usage1b.getString("api_name"));
        assertEquals(2L, usage1b.getLong("bin_start_time"));

        final JSONArray jsonArray2 = jsonObject.getJSONArray("1234abcde");
        assertEquals(2, jsonArray2.length());
        final JSONObject usage2a = (JSONObject) jsonArray2.get(0);
        assertEquals(1L, usage2a.getLong("sum_hits_total"));
        assertEquals(1L, usage2a.getLong("sum_hits_success"));
        assertEquals(0, usage2a.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage2a.getLong("sum_latency"));
        assertEquals(0, usage2a.getLong("sum_gateway_latency"));
        assertEquals(1L, usage2a.getLong("sum_back_latency"));
        assertEquals("test service", usage2a.getString("api_name"));
        assertEquals(1L, usage2a.getLong("bin_start_time"));
        final JSONObject usage2b = (JSONObject) jsonArray2.get(1);
        assertEquals(1L, usage2b.getLong("sum_hits_total"));
        assertEquals(1L, usage2b.getLong("sum_hits_success"));
        assertEquals(0, usage2b.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage2b.getLong("sum_latency"));
        assertEquals(0, usage2b.getLong("sum_gateway_latency"));
        assertEquals(1L, usage2b.getLong("sum_back_latency"));
        assertEquals("test service", usage2b.getString("api_name"));
        assertEquals(2L, usage2b.getLong("bin_start_time"));
    }

    /**
     * Does not filter by key.
     * <p/>
     * Does not group data by UUID.
     */
    @Test
    public void getUsageReportJsonMultipleUUIDsDoNotSeparateByUUID() throws Exception {
        uuids.add("1234abcd");
        uuids.add("1234abcde");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.JSON, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        insertPublishedService(connection, 2, "1234abcde");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 2, 1, 1, "1234abcde");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        insertServiceMetricDetail(connection, generatedId2, 1);

        final String data = generator.generateApiUsageReport(usageParameters, false, null, null);

        final JSONArray jsonArray = new JSONArray(data);
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(2L, usage.getLong("sum_hits_total"));
        assertEquals(2L, usage.getLong("sum_hits_success"));
        assertEquals(0, usage.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage.getLong("sum_latency"));
        assertEquals(0, usage.getLong("sum_gateway_latency"));
        assertEquals(1L, usage.getLong("sum_back_latency"));
        assertEquals(1L, usage.getLong("bin_start_time"));
    }

    /**
     * Filters by key.
     * <p/>
     * Does not group data by UUID.
     */
    @Test
    public void getUsageReportXmlFilterByApiKeys() throws Exception {
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

        final String dataOneKey = generator.generateApiUsageReport(usageParameters, false, null, null);

        final Document documentOneKey = buildDocumentFromXml(dataOneKey);
        assertNumberOfNodes(documentOneKey, "UsageReport/Usage", 1);
        assertXPathTextContent(documentOneKey, "//sum_hits_total", "1");
        assertXPathTextContent(documentOneKey, "//sum_hits_success", "1");
        assertXPathTextContent(documentOneKey, "//sum_hits_total_errors", "0");
        assertXPathTextContent(documentOneKey, "//sum_latency", "1");
        assertXPathTextContent(documentOneKey, "//sum_gateway_latency", "0");
        assertXPathTextContent(documentOneKey, "//sum_back_latency", "1");
        assertXPathTextContent(documentOneKey, "//bin_start_time", "1");

        applicationKeys.add("key2");

        final String dataTwoKeys = generator.generateApiUsageReport(usageParameters, false, null, null);

        final Document documentTwoKeys = buildDocumentFromXml(dataTwoKeys);
        assertNumberOfNodes(documentTwoKeys, "UsageReport/Usage", 1);
        assertXPathTextContent(documentTwoKeys, "//sum_hits_total", "2");
        assertXPathTextContent(documentTwoKeys, "//sum_hits_success", "2");
        assertXPathTextContent(documentTwoKeys, "//sum_hits_total_errors", "0");
        assertXPathTextContent(documentTwoKeys, "//sum_latency", "1");
        assertXPathTextContent(documentTwoKeys, "//sum_gateway_latency", "0");
        assertXPathTextContent(documentTwoKeys, "//sum_back_latency", "1");
        assertXPathTextContent(documentTwoKeys, "//bin_start_time", "1");
    }

    /**
     * Filters by key.
     * <p/>
     * Does not group data by UUID.
     */
    @Test
    public void getUsageReportJsonFilterByApiKeys() throws Exception {
        uuids.add("1234abcd");
        applicationKeys.add("key1");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.JSON, uuids, applicationKeys);

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

        final String dataOneKey = generator.generateApiUsageReport(usageParameters, false, null, null);

        final JSONArray jsonArrayOneKey = new JSONArray(dataOneKey);
        assertEquals(1, jsonArrayOneKey.length());
        final JSONObject usageOneKey = (JSONObject) jsonArrayOneKey.get(0);
        assertEquals(1L, usageOneKey.getLong("sum_hits_total"));
        assertEquals(1L, usageOneKey.getLong("sum_hits_success"));
        assertEquals(0, usageOneKey.getLong("sum_hits_total_errors"));
        assertEquals(1L, usageOneKey.getLong("sum_latency"));
        assertEquals(0, usageOneKey.getLong("sum_gateway_latency"));
        assertEquals(1L, usageOneKey.getLong("sum_back_latency"));
        assertEquals(1L, usageOneKey.getLong("bin_start_time"));

        applicationKeys.add("key2");
        final String dataTwoKeys = generator.generateApiUsageReport(usageParameters, false, null, null);

        final JSONArray jsonArrayTwoKeys = new JSONArray(dataTwoKeys);
        assertEquals(1, jsonArrayTwoKeys.length());
        final JSONObject usageTwoKeys = (JSONObject) jsonArrayTwoKeys.get(0);
        assertEquals(2L, usageTwoKeys.getLong("sum_hits_total"));
        assertEquals(2L, usageTwoKeys.getLong("sum_hits_success"));
        assertEquals(0, usageTwoKeys.getLong("sum_hits_total_errors"));
        assertEquals(1L, usageTwoKeys.getLong("sum_latency"));
        assertEquals(0, usageTwoKeys.getLong("sum_gateway_latency"));
        assertEquals(1L, usageTwoKeys.getLong("sum_back_latency"));
        assertEquals(1L, usageTwoKeys.getLong("bin_start_time"));
    }

    /**
     * Filters by key.
     * <p/>
     * Groups data by UUID.
     */
    @Test
    public void getUsageReportXmlFilterByApiKeysAndGroupByUUID() throws Exception {
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

        final String data = generator.generateApiUsageReport(usageParameters, true, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/uuid", 1);
        assertNumberOfNodes(document, "UsageReport/uuid/Usage", 1);
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/bin_start_time", "1");
    }

    /**
     * Filters by key.
     * <p/>
     * Groups data by UUID.
     */
    @Test
    public void getUsageReportJsonFilterByApiKeysAndGroupByUUID() throws Exception {
        uuids.add("1234abcd");
        applicationKeys.add("key1");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.JSON, uuids, applicationKeys);

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

        final String data = generator.generateApiUsageReport(usageParameters, true, null, null);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());
        final JSONArray jsonArray = jsonObject.getJSONArray("1234abcd");
        assertEquals(1, jsonArray.length());
        final JSONObject usage = jsonArray.getJSONObject(0);
        assertEquals(1L, usage.getLong("sum_hits_total"));
        assertEquals(1L, usage.getLong("sum_hits_success"));
        assertEquals(0, usage.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage.getLong("sum_latency"));
        assertEquals(0, usage.getLong("sum_gateway_latency"));
        assertEquals(1L, usage.getLong("sum_back_latency"));
        assertEquals("test service", usage.getString("api_name"));
        assertEquals(1L, usage.getLong("bin_start_time"));
    }

    /**
     * Does not filter by key
     * <p/>
     * Does not group data by uuid
     */
    @Test
    public void getUsageReportXmlAddsMissingRows() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 5, 1, Format.XML, uuids, null);

        // missing metrics for period_start = 0, 1, 3, 5
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, 2, 1, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        final long generatedId2 = insertServiceMetric(connection, 1, 4, 4, "1234abcd");
        insertServiceMetricDetail(connection, generatedId2, 1);

        final String data = generator.generateApiUsageReport(usageParameters, false, 1L, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 6);

        // missing rows
        assertXPathTextContent(document, "UsageReport/Usage[1]/bin_start_time", "0");
        assertXPathTextContent(document, "UsageReport/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "UsageReport/Usage[4]/bin_start_time", "3");
        assertXPathTextContent(document, "UsageReport/Usage[6]/bin_start_time", "5");

        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[3]/bin_start_time", "2");

        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[5]/bin_start_time", "4");
    }

    /**
     * Does not filter by key
     * <p/>
     * Does not group data by uuid
     * <p/>
     * Adds default values for missing rows.
     */
    @Test
    public void getUsageReportXmlAddsMissingRowsWithDefaults() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 5, 1, Format.XML, uuids, null);

        // missing metrics for period_start = 0, 1, 3, 5
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, 2, 1, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        final long generatedId2 = insertServiceMetric(connection, 1, 4, 4, "1234abcd");
        insertServiceMetricDetail(connection, generatedId2, 1);

        defaultValues.put("sum_hits_total", 0);
        defaultValues.put("sum_hits_success", 0);
        defaultValues.put("sum_hits_total_errors", 0);

        final String data = generator.generateApiUsageReport(usageParameters, false, 1L, defaultValues);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 6);

        // missing rows
        assertXPathTextContent(document, "UsageReport/Usage[1]/bin_start_time", "0");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_hits_total", "0");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_hits_success", "0");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_hits_total_errors", "0");

        assertXPathTextContent(document, "UsageReport/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "UsageReport/Usage[2]/sum_hits_total", "0");
        assertXPathTextContent(document, "UsageReport/Usage[2]/sum_hits_success", "0");
        assertXPathTextContent(document, "UsageReport/Usage[2]/sum_hits_total_errors", "0");

        assertXPathTextContent(document, "UsageReport/Usage[4]/bin_start_time", "3");
        assertXPathTextContent(document, "UsageReport/Usage[4]/sum_hits_total", "0");
        assertXPathTextContent(document, "UsageReport/Usage[4]/sum_hits_success", "0");
        assertXPathTextContent(document, "UsageReport/Usage[4]/sum_hits_total_errors", "0");

        assertXPathTextContent(document, "UsageReport/Usage[6]/bin_start_time", "5");
        assertXPathTextContent(document, "UsageReport/Usage[6]/sum_hits_total", "0");
        assertXPathTextContent(document, "UsageReport/Usage[6]/sum_hits_success", "0");
        assertXPathTextContent(document, "UsageReport/Usage[6]/sum_hits_total_errors", "0");

        // actual rows
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[3]/bin_start_time", "2");

        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[5]/bin_start_time", "4");
    }

    /**
     * Does not filter by key.
     * <p/>
     * Does not group data by uuid.
     */
    @Test
    public void getUsageReportJsonAddsMissingRows() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 5, 1, Format.JSON, uuids, null);

        // missing metrics for period_start = 0, 1, 3, 5
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, 2, 1, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        final long generatedId2 = insertServiceMetric(connection, 1, 4, 4, "1234abcd");
        insertServiceMetricDetail(connection, generatedId2, 1);

        final String data = generator.generateApiUsageReport(usageParameters, false, 1L, null);

        final JSONArray jsonArray = new JSONArray(data);
        assertEquals(6, jsonArray.length());

        final JSONObject missing1 = jsonArray.getJSONObject(0);
        assertEquals(1, missing1.length());
        assertEquals(0, missing1.get("bin_start_time"));

        final JSONObject missing2 = jsonArray.getJSONObject(1);
        assertEquals(1, missing2.length());
        assertEquals(1, missing2.get("bin_start_time"));

        final JSONObject missing3 = jsonArray.getJSONObject(3);
        assertEquals(1, missing3.length());
        assertEquals(3, missing3.get("bin_start_time"));

        final JSONObject missing4 = jsonArray.getJSONObject(5);
        assertEquals(1, missing4.length());
        assertEquals(5, missing4.get("bin_start_time"));

        final JSONObject data1 = (JSONObject) jsonArray.get(2);
        assertEquals(1L, data1.getLong("sum_hits_total"));
        assertEquals(1L, data1.getLong("sum_hits_success"));
        assertEquals(0, data1.getLong("sum_hits_total_errors"));
        assertEquals(1L, data1.getLong("sum_latency"));
        assertEquals(0, data1.getLong("sum_gateway_latency"));
        assertEquals(1L, data1.getLong("sum_back_latency"));
        assertEquals(2L, data1.getLong("bin_start_time"));

        final JSONObject data2 = (JSONObject) jsonArray.get(4);
        assertEquals(1L, data2.getLong("sum_hits_total"));
        assertEquals(1L, data2.getLong("sum_hits_success"));
        assertEquals(0, data2.getLong("sum_hits_total_errors"));
        assertEquals(1L, data2.getLong("sum_latency"));
        assertEquals(0, data2.getLong("sum_gateway_latency"));
        assertEquals(1L, data2.getLong("sum_back_latency"));
        assertEquals(4L, data2.getLong("bin_start_time"));
    }

    /**
     * Does not filter by key.
     * <p/>
     * Does not group data by uuid.
     * <p/>
     * Adds default values to missing rows.
     */
    @Test
    public void getUsageReportJsonAddsMissingRowsWithDefaultValues() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 5, 1, Format.JSON, uuids, null);

        // missing metrics for period_start = 0, 1, 3, 5
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, 2, 1, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        final long generatedId2 = insertServiceMetric(connection, 1, 4, 4, "1234abcd");
        insertServiceMetricDetail(connection, generatedId2, 1);

        defaultValues.put("sum_hits_total", 0);
        defaultValues.put("sum_hits_success", 0);
        defaultValues.put("sum_hits_total_errors", 0);

        final String data = generator.generateApiUsageReport(usageParameters, false, 1L, defaultValues);

        final JSONArray jsonArray = new JSONArray(data);
        assertEquals(6, jsonArray.length());

        final JSONObject missing1 = jsonArray.getJSONObject(0);
        assertEquals(4, missing1.length());
        assertEquals(0, missing1.get("bin_start_time"));
        assertEquals(0, missing1.get("sum_hits_total"));
        assertEquals(0, missing1.get("sum_hits_success"));
        assertEquals(0, missing1.get("sum_hits_total_errors"));

        final JSONObject missing2 = jsonArray.getJSONObject(1);
        assertEquals(4, missing2.length());
        assertEquals(1, missing2.get("bin_start_time"));
        assertEquals(0, missing2.get("sum_hits_total"));
        assertEquals(0, missing2.get("sum_hits_success"));
        assertEquals(0, missing2.get("sum_hits_total_errors"));

        final JSONObject missing3 = jsonArray.getJSONObject(3);
        assertEquals(4, missing3.length());
        assertEquals(3, missing3.get("bin_start_time"));
        assertEquals(0, missing3.get("sum_hits_total"));
        assertEquals(0, missing3.get("sum_hits_success"));
        assertEquals(0, missing3.get("sum_hits_total_errors"));

        final JSONObject missing4 = jsonArray.getJSONObject(5);
        assertEquals(4, missing4.length());
        assertEquals(5, missing4.get("bin_start_time"));
        assertEquals(0, missing4.get("sum_hits_total"));
        assertEquals(0, missing4.get("sum_hits_success"));
        assertEquals(0, missing4.get("sum_hits_total_errors"));

        final JSONObject data1 = (JSONObject) jsonArray.get(2);
        assertEquals(1L, data1.getLong("sum_hits_total"));
        assertEquals(1L, data1.getLong("sum_hits_success"));
        assertEquals(0, data1.getLong("sum_hits_total_errors"));
        assertEquals(1L, data1.getLong("sum_latency"));
        assertEquals(0, data1.getLong("sum_gateway_latency"));
        assertEquals(1L, data1.getLong("sum_back_latency"));
        assertEquals(2L, data1.getLong("bin_start_time"));

        final JSONObject data2 = (JSONObject) jsonArray.get(4);
        assertEquals(1L, data2.getLong("sum_hits_total"));
        assertEquals(1L, data2.getLong("sum_hits_success"));
        assertEquals(0, data2.getLong("sum_hits_total_errors"));
        assertEquals(1L, data2.getLong("sum_latency"));
        assertEquals(0, data2.getLong("sum_gateway_latency"));
        assertEquals(1L, data2.getLong("sum_back_latency"));
        assertEquals(4L, data2.getLong("bin_start_time"));
    }

    /**
     * Does not filter by key.
     * <p/>
     * Groups data by uuid.
     * <p/>
     * Adds default values for missing rows.
     */
    @Test
    public void getUsageReportXmlAddsMissingRowsWithDefaultsAndGroups() throws Exception {
        uuids.add("1234abcd");
        uuids.add("1234abcde");
        uuids.add("missing");
        usageParameters = new ApiUsageReportParameters(0, 4, 1, Format.XML, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        insertPublishedService(connection, 2, "1234abcde");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 1, 3, 3, "1234abcd");
        final long generatedId3 = insertServiceMetric(connection, 2, 1, 1, "1234abcde");
        final long generatedId4 = insertServiceMetric(connection, 2, 3, 3, "1234abcde");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        insertServiceMetricDetail(connection, generatedId2, 1);
        insertServiceMetricDetail(connection, generatedId3, 1);
        insertServiceMetricDetail(connection, generatedId4, 1);

        defaultValues.put("sum_hits_total", 0);
        defaultValues.put("sum_hits_success", 0);
        defaultValues.put("sum_hits_total_errors", 0);

        final String data = generator.generateApiUsageReport(usageParameters, true, 1L, defaultValues);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/uuid", 3);

        // service 1
        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcd']/Usage", 5);
        // missing rows
        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcd']/Usage[1]/*", 4);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[1]/bin_start_time", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[1]/sum_hits_total", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[1]/sum_hits_success", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[1]/sum_hits_total_errors", "0");

        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcd']/Usage[3]/*", 4);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[3]/bin_start_time", "2");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[3]/sum_hits_total", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[3]/sum_hits_success", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[3]/sum_hits_total_errors", "0");

        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcd']/Usage[5]/*", 4);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[5]/bin_start_time", "4");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[5]/sum_hits_total", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[5]/sum_hits_success", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[5]/sum_hits_total_errors", "0");

        // existing rows
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/sum_hits_success", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/api_name", "test service");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/sum_hits_success", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/api_name", "test service");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/bin_start_time", "3");

        // service 2
        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcde']/Usage", 5);
        // missing rows
        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcde']/Usage[1]/*", 4);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[1]/bin_start_time", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[1]/sum_hits_total", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[1]/sum_hits_success", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[1]/sum_hits_total_errors", "0");

        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcde']/Usage[3]/*", 4);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[3]/bin_start_time", "2");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[3]/sum_hits_total", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[3]/sum_hits_success", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[3]/sum_hits_total_errors", "0");

        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcde']/Usage[5]/*", 4);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[5]/bin_start_time", "4");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[5]/sum_hits_total", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[5]/sum_hits_success", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[5]/sum_hits_total_errors", "0");
        // existing rows
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/sum_hits_success", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/api_name", "test service");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/sum_hits_success", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/api_name", "test service");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/bin_start_time", "3");

        // missing group
        assertNumberOfNodes(document, "UsageReport/uuid[@value='missing']/Usage", 5);
        // missing rows
        assertNumberOfNodes(document, "UsageReport/uuid[@value='missing']/Usage[1]/*", 4);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[1]/bin_start_time", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[1]/sum_hits_total", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[1]/sum_hits_success", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[1]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[2]/sum_hits_total", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[2]/sum_hits_success", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[2]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[3]/bin_start_time", "2");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[3]/sum_hits_total", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[3]/sum_hits_success", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[3]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[4]/bin_start_time", "3");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[4]/sum_hits_total", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[4]/sum_hits_success", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='missing']/Usage[4]/sum_hits_total_errors", "0");
    }

    /**
     * Does not filter by key.
     * <p/>
     * Groups data by uuid.
     */
    @Test
    public void getUsageReportXmlAddsMissingRowsAndGroups() throws Exception {
        uuids.add("1234abcd");
        uuids.add("1234abcde");
        usageParameters = new ApiUsageReportParameters(0, 4, 1, Format.XML, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        insertPublishedService(connection, 2, "1234abcde");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 1, 3, 3, "1234abcd");
        final long generatedId3 = insertServiceMetric(connection, 2, 1, 1, "1234abcde");
        final long generatedId4 = insertServiceMetric(connection, 2, 3, 3, "1234abcde");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        insertServiceMetricDetail(connection, generatedId2, 1);
        insertServiceMetricDetail(connection, generatedId3, 1);
        insertServiceMetricDetail(connection, generatedId4, 1);

        final String data = generator.generateApiUsageReport(usageParameters, true, 1L, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/uuid", 2);

        // service 1
        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcd']/Usage", 5);
        // missing rows
        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcd']/Usage[1]/*", 1);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[1]/bin_start_time", "0");
        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcd']/Usage[3]/*", 1);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[3]/bin_start_time", "2");
        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcd']/Usage[5]/*", 1);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[5]/bin_start_time", "4");
        // existing rows
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/sum_hits_success", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/api_name", "test service");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/sum_hits_success", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/api_name", "test service");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcd']/Usage[4]/bin_start_time", "3");

        // service 2
        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcde']/Usage", 5);
        // missing rows
        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcde']/Usage[1]/*", 1);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[1]/bin_start_time", "0");
        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcde']/Usage[3]/*", 1);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[3]/bin_start_time", "2");
        assertNumberOfNodes(document, "UsageReport/uuid[@value='1234abcde']/Usage[5]/*", 1);
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[5]/bin_start_time", "4");
        // existing rows
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/sum_hits_success", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/api_name", "test service");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/sum_hits_total", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/sum_hits_success", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/api_name", "test service");
        assertXPathTextContent(document, "/UsageReport/uuid[@value='1234abcde']/Usage[4]/bin_start_time", "3");
    }

    /**
     * Does not filter by key
     * <p/>
     * Does not group data by uuid
     * <p/>
     * Using overloaded method with no interval argument - should not insert missing data.
     */
    @Test
    public void getUsageReportXmlNoInterval() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 5, 1, Format.XML, uuids, null);

        // missing metrics for period_start = 0, 1, 3, 5
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, 2, 1, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        final long generatedId2 = insertServiceMetric(connection, 1, 4, 4, "1234abcd");
        insertServiceMetricDetail(connection, generatedId2, 1);

        final String data = generator.generateApiUsageReport(usageParameters, false);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 2);

        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[1]/bin_start_time", "2");

        assertXPathTextContent(document, "UsageReport/Usage[2]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/Usage[2]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/Usage[2]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[2]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/Usage[2]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[2]/bin_start_time", "4");
    }

    /**
     * Does not filter by key
     * <p/>
     * Does not group data by uuid
     * <p/>
     * Null interval specified so default values should be ignored.
     */
    @Test
    public void getUsageReportXmlNullIntervalIgnoresDefaults() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 5, 1, Format.XML, uuids, null);

        // missing metrics for period_start = 0, 1, 3, 5
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, 2, 1, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1);
        final long generatedId2 = insertServiceMetric(connection, 1, 4, 4, "1234abcd");
        insertServiceMetricDetail(connection, generatedId2, 1);

        defaultValues.put("sum_hits_total", 0);

        final String data = generator.generateApiUsageReport(usageParameters, false, null, defaultValues);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 2);

        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[1]/bin_start_time", "2");

        assertXPathTextContent(document, "UsageReport/Usage[2]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/Usage[2]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/Usage[2]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[2]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/Usage[2]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/Usage[2]/bin_start_time", "4");
    }

    /**
     * Ensure latency is calculated correctly across the whole cluster.
     * <p/>
     * Latency should be an average across the whole cluster, not a sum.
     */
    @Test
    public void getUsageReportXmlMultipleNodes() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.XML, uuids, null);
        final Integer numRequests1 = 10;
        final Integer backSum1 = 100;
        final Integer frontSum1 = 1000;
        final Integer numRequests2 = 20;
        final Integer backSum2 = 200;
        final Integer frontSum2 = 2000;

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, "node1", 1, 1, 1, 1, 1, numRequests1, numRequests1, numRequests1, 1, 1, backSum1, 1, 1, frontSum1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 1, "node2", 1, 1, 1, 1, 1, numRequests2, numRequests2, numRequests2, 1, 1, backSum2, 1, 1, frontSum2, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1, numRequests1, numRequests1, numRequests1, 1, 1, backSum1, 1, 1, frontSum1);
        insertServiceMetricDetail(connection, generatedId2, 1, numRequests2, numRequests2, numRequests2, 1, 1, backSum2, 1, 1, frontSum2);

        final String data = generator.generateApiUsageReport(usageParameters, false, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 1);
        assertXPathTextContent(document, "UsageReport/Usage/sum_hits_total", "30");
        assertXPathTextContent(document, "UsageReport/Usage/sum_hits_success", "30");
        assertXPathTextContent(document, "UsageReport/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/Usage/sum_latency", "100");
        assertXPathTextContent(document, "UsageReport/Usage/sum_gateway_latency", "90");
        assertXPathTextContent(document, "UsageReport/Usage/sum_back_latency", "10");
        assertXPathTextContent(document, "UsageReport/Usage/bin_start_time", "1");
    }

    /**
     * Ensure latency is calculated correctly across the whole cluster when grouping.
     * <p/>
     * Latency should be an average across the whole cluster, not a sum.
     */
    @Test
    public void getUsageReportXmlMultipleNodesSeparateByUUID() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.XML, uuids, null);
        final Integer numRequests1 = 10;
        final Integer backSum1 = 100;
        final Integer frontSum1 = 1000;
        final Integer numRequests2 = 20;
        final Integer backSum2 = 200;
        final Integer frontSum2 = 2000;

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, "node1", 1, 1, 1, 1, 1, numRequests1, numRequests1, numRequests1, 1, 1, backSum1, 1, 1, frontSum1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 1, "node2", 1, 1, 1, 1, 1, numRequests2, numRequests2, numRequests2, 1, 1, backSum2, 1, 1, frontSum2, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId1, 1, numRequests1, numRequests1, numRequests1, 1, 1, backSum1, 1, 1, frontSum1);
        insertServiceMetricDetail(connection, generatedId2, 1, numRequests2, numRequests2, numRequests2, 1, 1, backSum2, 1, 1, frontSum2);

        final String data = generator.generateApiUsageReport(usageParameters, true, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/uuid", 1);
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_total", "30");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_success", "30");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_latency", "100");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_gateway_latency", "90");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/sum_back_latency", "10");
        assertXPathTextContent(document, "UsageReport/uuid[@value='1234abcd']/Usage/bin_start_time", "1");
    }

    @Test
    public void getUsageReportXmlNoData() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.XML, uuids, null);

        final String data = generator.generateApiUsageReport(usageParameters, false, null, null);

        assertEquals("<UsageReport/>", data.trim());
    }

    @Test
    public void getUsageReportXmlNoDataGroupByUUID() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.XML, uuids, null);

        final String data = generator.generateApiUsageReport(usageParameters, true, null, null);

        assertEquals("<UsageReport><uuid value=\"1234abcd\"/></UsageReport>", data.trim());
    }

    @Test
    public void getUsageReportJsonNoData() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.JSON, uuids, null);

        final String data = generator.generateApiUsageReport(usageParameters, false, null, null);

        assertEquals("[]", data);
    }

    @Test
    public void getUsageReportJsonNoDataGroupByUUID() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.JSON, uuids, null);

        final String data = generator.generateApiUsageReport(usageParameters, true, null, null);

        assertEquals("{\"1234abcd\":[]}", data);
    }

    /**
     * Ensure no divide by zero error.
     */
    @Test
    public void getUsageReportJsonZero() throws Exception {
        uuids.add("1234abcd");
        usageParameters = new ApiUsageReportParameters(0, 2, 1, Format.JSON, uuids, null);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, "node", 1, 1, 1, 1, 3600000, 0, 0, 0, 0, 0, 0, 0, 0, 0, "1234abcd");
        insertMappingKey(connection, 1, null, null);
        insertMappingValue(connection, 1, 1, null);
        insertServiceMetricDetail(connection, generatedId, 1);

        final String data = generator.generateApiUsageReport(usageParameters, true, null, null);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());
        final JSONArray jsonArray = jsonObject.getJSONArray("1234abcd");
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(0, usage.getLong("sum_hits_total"));
        assertEquals(0, usage.getLong("sum_hits_success"));
        assertEquals(0, usage.getLong("sum_hits_total_errors"));
        assertEquals(0L, usage.getLong("sum_latency"));
        assertEquals(0, usage.getLong("sum_gateway_latency"));
        assertEquals(0L, usage.getLong("sum_back_latency"));
        assertEquals("test service", usage.getString("api_name"));
        assertEquals(1L, usage.getLong("bin_start_time"));
    }
}
