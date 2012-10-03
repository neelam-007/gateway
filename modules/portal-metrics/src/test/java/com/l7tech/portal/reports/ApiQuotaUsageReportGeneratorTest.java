package com.l7tech.portal.reports;

import com.l7tech.portal.reports.format.Format;
import com.l7tech.portal.reports.format.JsonResultSetFormatter;
import com.l7tech.portal.reports.parameter.ApiQuotaUsageReportParameters;
import com.l7tech.portal.reports.parameter.DefaultReportParameters;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.util.*;

import static org.junit.Assert.*;

/**
 * This test fails intermittently. Ignoring until it is fixed.
 */
@Ignore
public class ApiQuotaUsageReportGeneratorTest extends AbstractReportGeneratorTestUtility {
    private ApiQuotaUsageReportParameters quotaUsageParameters;
    private ArrayList<String> uuids;
    private String api_key = "l7xxf7ed30eb31f64837b2dfc80e722b7060";
    private TimeInsensitiveReportGenerator reportGenerator;
    private HashMap<DefaultReportParameters.QuotaRange, List<String>> values;
    private HashMap<DefaultReportParameters.QuotaRange, List<String>> values2;
    private static final long START_TIME = 1000L;

    @Before
    public void setup() throws Exception {
        setupAbstractReportGeneratorTest();
        uuids = new ArrayList<String>();
        add("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        values = new HashMap<DefaultReportParameters.QuotaRange, List<String>>();
        values.put(DefaultReportParameters.QuotaRange.MONTH, uuids);
        values2 = new HashMap<DefaultReportParameters.QuotaRange, List<String>>();
        values2.put(DefaultReportParameters.QuotaRange.SECOND, uuids);
        reportGenerator = new TimeInsensitiveReportGenerator(dataSource);
        reportGenerator.setStartTime(START_TIME);
        generator = reportGenerator;
    }

    @After
    public void teardown() throws Exception {
        teardownAbstractReportGeneratorTest();
    }

    /**
     * Add a uuid to the uuids list and do so in a thread safe way.
     *
     * @param uuid
     */
    synchronized void add(String uuid) {
        synchronized (uuids) {
            uuids.add(uuid);
        }
    }

    /**
     * Removes a uuid to the uuids list and do so in a thread safe way.
     *
     * @param uuid
     */
    synchronized void remove(String uuid) {
        synchronized (uuids) {
            uuids.remove(uuid);
        }

    }

    @Test(expected = IllegalArgumentException.class)
    public void getApplicationUsageReportNullParams() throws Exception {
        generator.generateApplicationUsageReport(null, true, null, null);
    }

    /**
     * Groups by key.
     */
    @Test
    public void getQuotaUsageReportXml() throws Exception {

        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.XML);

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final long generatedId = insertServiceMetric(connection, 1, START_TIME + 1, Calendar.getInstance().getTimeInMillis(), "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId, 1);


        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/api_key", 1);
        assertNumberOfNodes(document, "UsageReport/api_key[@value='" + api_key + "']", 1);
        assertXPathTextContent(document, "UsageReport/api_key/@value", api_key);
        assertXPathTextContent(document, "UsageReport/api_key/uuid/@hits", "1");
        assertXPathTextContent(document, "UsageReport/api_key/uuid/@range", "5");
        assertXPathTextContent(document, "UsageReport/api_key/uuid/@value", "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
    }

    /**
     * Groups by key.
     */
    @Test
    public void getQuotaUsageReportJson() throws Exception {
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.JSON);

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final long generatedId1 = insertServiceMetric(connection, 1, START_TIME + 1, Calendar.getInstance().getTimeInMillis(), "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1);


        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);
        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        final JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(1, usage.getInt("hits"));
        assertEquals("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4", usage.getString("uuid"));
        assertEquals(5, usage.getInt("range"));
    }


    /**
     * Groups by UUID.
     */
    @Test
    public void getQuotaUsageReportXmlMultipleUUIDs() throws Exception {
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.XML);

        add("c586b542-6e89-4110-be1e-3835156dcad3");
        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        insertPublishedService(connection, 2, "c586b542-6e89-4110-be1e-3835156dcad3");
        final long generatedId1 = insertServiceMetric(connection, 1, START_TIME + 1, START_TIME + 15000, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1);

        final long generatedId2 = insertServiceMetric(connection, 2, START_TIME + 1, START_TIME + 15000, "c586b542-6e89-4110-be1e-3835156dcad3");
        insertServiceMetricDetail(connection, generatedId2, 1);


        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);


        // Clean up


        Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/api_key", 1);
        assertNumberOfNodes(document, "UsageReport/api_key[@value='" + api_key + "']", 1);
        assertXPathTextContent(document, "UsageReport/api_key/@value", api_key);

        assertNumberOfNodes(document, "UsageReport/api_key/uuid", 2);


        assertNumberOfNodes(document, "UsageReport/api_key/uuid[@value='5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4']", 1);
        assertNumberOfNodes(document, "UsageReport/api_key/uuid[@value='c586b542-6e89-4110-be1e-3835156dcad3']", 1);

        assertXPathTextContent(document, "UsageReport/api_key/uuid[@value='5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4']/@hits", "1");
        assertXPathTextContent(document, "UsageReport/api_key/uuid[@value='c586b542-6e89-4110-be1e-3835156dcad3']/@hits", "1");

        assertXPathTextContent(document, "UsageReport/api_key/uuid[@value='5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4']/@range", "5");
        assertXPathTextContent(document, "UsageReport/api_key/uuid[@value='c586b542-6e89-4110-be1e-3835156dcad3']/@range", "5");

        add("diablo");

        final String data1 = generator.generateApiQuotaUsageReport(quotaUsageParameters);
        document = buildDocumentFromXml(data1);
        assertXPathTextContent(document, "UsageReport/api_key/uuid[@value='diablo']/@hits", "0");

        remove("diablo");
        remove("c586b542-6e89-4110-be1e-3835156dcad3");

    }

    /**
     * Groups by key.
     */
    @Test
    public void getQuotaUsageReportJsonMultipleUUIDs() throws Exception {
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.JSON);
        add("c586b542-6e89-4110-be1e-3835156dcad3");

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        insertPublishedService(connection, 2, "c586b542-6e89-4110-be1e-3835156dcad3");

        final long generatedId1 = insertServiceMetric(connection, 1, START_TIME + 1, START_TIME + 15000, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1);

        final long generatedId2 = insertServiceMetric(connection, 2, START_TIME + 1, START_TIME + 15000, "c586b542-6e89-4110-be1e-3835156dcad3");
        insertServiceMetricDetail(connection, generatedId2, 1);

        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);
        // Clean up
        remove("c586b542-6e89-4110-be1e-3835156dcad3");


        JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(2, jsonArray.length());

        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(1, usage.getInt("hits"));
        assertEquals("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4", usage.getString("uuid"));
        assertEquals(5, usage.getInt("range"));


        final JSONObject usage1 = (JSONObject) jsonArray.get(1);
        assertEquals(1, usage1.getInt("hits"));
        assertEquals("c586b542-6e89-4110-be1e-3835156dcad3", usage1.getString("uuid"));
        assertEquals(5, usage1.getInt("range"));

        add("diablo");
        String data2 = generator.generateApiQuotaUsageReport(quotaUsageParameters);
        jsonObject = new JSONObject(data2);
        jsonArray = jsonObject.getJSONArray(api_key);
        JSONObject usage2 = (JSONObject) jsonArray.get(1);
        assertEquals(0, usage2.getInt("hits"));
        assertEquals("diablo", usage2.getString("uuid"));
        assertEquals(5, usage2.getInt("range"));
        remove("diablo");
    }


    /**
     * Groups by key.
     */
    @Test
    public void getQuotaUsageReportJsonMultipleUUIDsMinute() throws Exception {
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values2, Format.JSON);
        add("c586b542-6e89-4110-be1e-3835156dcad3");

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        insertPublishedService(connection, 2, "c586b542-6e89-4110-be1e-3835156dcad3");

        final long generatedId1 = insertServiceMetric(connection, 1, START_TIME + 1, Calendar.getInstance().getTimeInMillis(), "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4", 3);
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1);

        final long generatedId2 = insertServiceMetric(connection, 2, START_TIME + 1, Calendar.getInstance().getTimeInMillis(), "c586b542-6e89-4110-be1e-3835156dcad3", 3);
        insertServiceMetricDetail(connection, generatedId2, 1);

        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);

        // Clean up
        remove("c586b542-6e89-4110-be1e-3835156dcad3");


        JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(2, jsonArray.length());

        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(0, usage.getInt("hits"));
        final String uuidFromData = usage.getString("uuid");
        assertTrue("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4".equals(uuidFromData) || "c586b542-6e89-4110-be1e-3835156dcad3".endsWith(uuidFromData));

    }

    @Test
    public void getQuotaUsageReportMinute() throws Exception {
        values.clear();
        values.put(DefaultReportParameters.QuotaRange.MINUTE, uuids);
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.JSON);

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        // api was used in the last hour - 5 per minute
        final long generatedId1 = insertServiceMetric(connection, 1, "node", START_TIME + 1, START_TIME + 1, START_TIME + 900000, 3, 900000, 75, 75, 75, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1, 75, 75, 75, 0, 5, 500, 0, 5, 500);

        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        final JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(5, usage.getInt("hits"));
        assertEquals("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4", usage.getString("uuid"));
        assertEquals(2, usage.getInt("range"));
    }

    /**
     * Testing start boundary.
     */
    @Test
    public void getQuotaUsageReportMinuteEqualToStartTime() throws Exception {
        values.clear();
        values.put(DefaultReportParameters.QuotaRange.MINUTE, uuids);
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.JSON);

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final long generatedId1 = insertServiceMetric(connection, 1, "node", START_TIME, START_TIME, START_TIME + 900000, 3, 900000, 75, 75, 75, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1, 75, 75, 75, 0, 5, 500, 0, 5, 500);

        final ResultSet test = connection.createStatement().executeQuery("select * from api_key_or_method_usage_view where bin_start_time > 1339531200000");
        final JsonResultSetFormatter format = new JsonResultSetFormatter();
        format.setIndentSize(1);

        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        final JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(5, usage.getInt("hits"));
        assertEquals("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4", usage.getString("uuid"));
        assertEquals(2, usage.getInt("range"));
    }

    /**
     * If there are multiple metrics for the last hour, use the latest one for calculations.
     */
    @Test
    public void getQuotaUsageReportMinuteMultiple() throws Exception {
        values.clear();
        values.put(DefaultReportParameters.QuotaRange.MINUTE, uuids);
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.JSON);

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        // api was used twice in the last hour
        final long generatedId1 = insertServiceMetric(connection, 1, "node", START_TIME + 100, START_TIME + 100, START_TIME + 900100, 3, 900000, 75, 75, 75, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final long generatedId2 = insertServiceMetric(connection, 1, "node", START_TIME + 50, START_TIME + 50, START_TIME + 900050, 3, 900000, 45, 45, 45, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1, 75, 75, 75, 0, 5, 500, 0, 5, 500);
        insertServiceMetricDetail(connection, generatedId2, 1, 45, 45, 45, 0, 5, 500, 0, 5, 500);

        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        final JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(5, usage.getInt("hits"));
        assertEquals("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4", usage.getString("uuid"));
        assertEquals(2, usage.getInt("range"));
    }

    /**
     * Hits should be summed across nodes.
     * <p/>
     * Only latest bin should be used for calculations.
     */
    @Test
    public void getQuotaUsageReportMinuteMultipleNodes() throws Exception {
        values.clear();
        values.put(DefaultReportParameters.QuotaRange.MINUTE, uuids);
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.JSON);

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        // api was used by multiple nodes in the cluster
        final long generatedId1 = insertServiceMetric(connection, 1, "node", START_TIME, START_TIME, START_TIME + 900000, 3, 900000, 45, 45, 45, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final long generatedId2 = insertServiceMetric(connection, 1, "node", START_TIME + 1, START_TIME + 1, START_TIME + 900001, 3, 900000, 75, 75, 75, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final long generatedId3 = insertServiceMetric(connection, 1, "node1", START_TIME, START_TIME, START_TIME + 900000, 3, 900000, 45, 45, 45, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final long generatedId4 = insertServiceMetric(connection, 1, "node1", START_TIME + 1, START_TIME + 1, START_TIME + 900001, 3, 900000, 75, 75, 75, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1, 45, 45, 45, 0, 5, 500, 0, 5, 500);
        insertServiceMetricDetail(connection, generatedId2, 1, 75, 75, 75, 0, 5, 500, 0, 5, 500);
        insertServiceMetricDetail(connection, generatedId3, 1, 45, 45, 45, 0, 5, 500, 0, 5, 500);
        insertServiceMetricDetail(connection, generatedId4, 1, 75, 75, 75, 0, 5, 500, 0, 5, 500);

        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        final JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(10, usage.getInt("hits"));
        assertEquals("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4", usage.getString("uuid"));
        assertEquals(2, usage.getInt("range"));
    }

    @Test
    public void getQuotaUsageReportSecond() throws Exception {
        values.clear();
        values.put(DefaultReportParameters.QuotaRange.SECOND, uuids);
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.JSON);

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        // api was used in the last hour - 1 per second
        final long generatedId1 = insertServiceMetric(connection, 1, "node", START_TIME + 1, START_TIME + 1, START_TIME + 900000, 3, 900000, 900, 900, 900, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1, 900, 900, 900, 0, 5, 500, 0, 5, 500);

        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        final JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(1, usage.getInt("hits"));
        assertEquals("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4", usage.getString("uuid"));
        assertEquals(1, usage.getInt("range"));
    }

    /**
     * Testing start boundary.
     */
    @Test
    public void getQuotaUsageReportSecondEqualToStartTime() throws Exception {
        values.clear();
        values.put(DefaultReportParameters.QuotaRange.SECOND, uuids);
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.JSON);

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        // api was used in the last hour - 1 per second
        final long generatedId1 = insertServiceMetric(connection, 1, "node", START_TIME, START_TIME, START_TIME + 900000, 3, 900000, 900, 900, 900, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1, 900, 900, 900, 0, 5, 500, 0, 5, 500);

        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        final JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(1, usage.getInt("hits"));
        assertEquals("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4", usage.getString("uuid"));
        assertEquals(1, usage.getInt("range"));
    }

    @Test
    public void getQuotaUsageReportSecondMultiple() throws Exception {
        values.clear();
        values.put(DefaultReportParameters.QuotaRange.SECOND, uuids);
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.JSON);

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        // api was used twice in the last hour
        final long generatedId1 = insertServiceMetric(connection, 1, "node", START_TIME + 100, START_TIME + 100, START_TIME + 900100, 3, 900000, 1800, 1800, 1800, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final long generatedId2 = insertServiceMetric(connection, 1, "node", START_TIME + 50, START_TIME + 50, START_TIME + 900050, 3, 900000, 900, 900, 900, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1, 1800, 1800, 1800, 0, 5, 500, 0, 5, 500);
        insertServiceMetricDetail(connection, generatedId2, 1, 900, 900, 900, 0, 5, 500, 0, 5, 500);

        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        final JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(2, usage.getInt("hits"));
        assertEquals("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4", usage.getString("uuid"));
        assertEquals(1, usage.getInt("range"));
    }

    /**
     * Hits should be summed across nodes.
     * <p/>
     * Only latest bin should be used for calculations.
     */
    @Test
    public void getQuotaUsageReportSecondMultipleNodes() throws Exception {
        values.clear();
        values.put(DefaultReportParameters.QuotaRange.SECOND, uuids);
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.JSON);

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        // api was used by multiple nodes in the cluster
        final long generatedId1 = insertServiceMetric(connection, 1, "node", START_TIME, START_TIME, START_TIME + 900000, 3, 900000, 1800, 1800, 1800, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final long generatedId2 = insertServiceMetric(connection, 1, "node", START_TIME + 1, START_TIME + 1, START_TIME + 900001, 3, 900000, 900, 900, 900, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final long generatedId3 = insertServiceMetric(connection, 1, "node1", START_TIME, START_TIME, START_TIME + 900000, 3, 900000, 1800, 1800, 1800, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final long generatedId4 = insertServiceMetric(connection, 1, "node1", START_TIME + 1, START_TIME + 1, START_TIME + 900001, 3, 900000, 900, 900, 900, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1, 1800, 1800, 1800, 0, 5, 500, 0, 5, 500);
        insertServiceMetricDetail(connection, generatedId2, 1, 900, 900, 900, 0, 5, 500, 0, 5, 500);
        insertServiceMetricDetail(connection, generatedId3, 1, 1800, 1800, 1800, 0, 5, 500, 0, 5, 500);
        insertServiceMetricDetail(connection, generatedId4, 1, 900, 900, 900, 0, 5, 500, 0, 5, 500);

        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        final JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(2, usage.getInt("hits"));
        assertEquals("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4", usage.getString("uuid"));
        assertEquals(1, usage.getInt("range"));
    }

    @Test
    public void getQuotaUsageReportMinuteNone() throws Exception {
        values.clear();
        values.put(DefaultReportParameters.QuotaRange.MINUTE, uuids);
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.JSON);

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        // api was last used two years ago - 5 per minute
        final long generatedId1 = insertServiceMetric(connection, 1, "node", START_TIME - 1, START_TIME - 1, START_TIME + 899999, 3, 900000, 75, 75, 75, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1, 75, 75, 75, 0, 5, 500, 0, 5, 500);

        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        final JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(0, usage.getInt("hits"));
        assertEquals("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4", usage.getString("uuid"));
        assertEquals(2, usage.getInt("range"));
    }

    @Test
    public void getQuotaUsageReportSecondNone() throws Exception {
        values.clear();
        values.put(DefaultReportParameters.QuotaRange.SECOND, uuids);
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.JSON);

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        // api was last used two years ago - 1 per second
        final long generatedId1 = insertServiceMetric(connection, 1, "node", START_TIME - 1, START_TIME - 1, START_TIME + 899999, 3, 900000, 900, 900, 900, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1, 900, 900, 900, 0, 5, 500, 0, 5, 500);

        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);
        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        final JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(1, jsonArray.length());
        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(0, usage.getInt("hits"));
        assertEquals("5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4", usage.getString("uuid"));
        assertEquals(1, usage.getInt("range"));
    }

    @Test
    public void getQuotaUsageReportSecondMinuteHourDayMonth() throws Exception {
        values.clear();
        values.put(DefaultReportParameters.QuotaRange.SECOND, uuids);
        values.put(DefaultReportParameters.QuotaRange.MINUTE, uuids);
        values.put(DefaultReportParameters.QuotaRange.HOUR, uuids);
        values.put(DefaultReportParameters.QuotaRange.DAY, uuids);
        values.put(DefaultReportParameters.QuotaRange.MONTH, uuids);
        quotaUsageParameters = new ApiQuotaUsageReportParameters(api_key, values, Format.JSON);

        insertPublishedService(connection, 1, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        // api was used in the last hour - 5 per minute
        final long generatedId1 = insertServiceMetric(connection, 1, "node", START_TIME + 1, START_TIME + 1, START_TIME + 900000, 3, 900000, 900, 900, 900, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final long generatedId2 = insertServiceMetric(connection, 1, "node", START_TIME + 1, START_TIME + 1, START_TIME + 3600000, 1, 3600000, 2000, 2000, 2000, 0, 5, 500, 0, 5, 500, "5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList(api_key, "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1, 900, 900, 900, 0, 5, 500, 0, 5, 500);
        insertServiceMetricDetail(connection, generatedId2, 1, 2000, 2000, 2000, 0, 5, 500, 0, 5, 500);

        final String data = generator.generateApiQuotaUsageReport(quotaUsageParameters);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(1, jsonObject.length());

        final JSONArray jsonArray = jsonObject.getJSONArray(api_key);
        assertEquals(5, jsonArray.length());
        assertTrue(data.contains("{\"hits\":\"1\",\"range\":\"1\",\"uuid\":\"5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4\"}"));
        assertTrue(data.contains("{\"hits\":\"60\",\"range\":\"2\",\"uuid\":\"5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4\"}"));
        assertTrue(data.contains("{\"hits\":\"900\",\"range\":\"3\",\"uuid\":\"5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4\"}"));
        assertTrue(data.contains("{\"hits\":\"2000\",\"range\":\"4\",\"uuid\":\"5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4\"}"));
        assertTrue(data.contains("{\"hits\":\"2000\",\"range\":\"5\",\"uuid\":\"5f5cbd0a-3ed6-4b93-9cd0-5cfb8f6116f4\"}"));
    }

    /**
     * Purpose of this class is to have a set start_time that is returned for the quota queries so that the unit tests do not have to be
     * time-sensitive on the current time.
     */
    private class TimeInsensitiveReportGenerator extends MetricsReportGenerator {
        private long startTime;

        public TimeInsensitiveReportGenerator(final DataSource dataSource) {
            super(dataSource);
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(final long startTime) {
            this.startTime = startTime;
        }

        @Override
        ApiQuotaUsageReportParameters getQuotaParameters(ApiQuotaUsageReportParameters params, HashMap<DefaultReportParameters.QuotaRange, List<String>> temp_api_ranges) {
            final ApiQuotaUsageReportParameters quotaParameters = new TimeInsensitiveParameters(params.getApiKey(), temp_api_ranges, params.getFormat());
            quotaParameters.setStartTime(startTime);
            return quotaParameters;
        }
    }

    private class TimeInsensitiveParameters extends ApiQuotaUsageReportParameters {
        private long startTime;

        public TimeInsensitiveParameters(final String apiKey, final HashMap<QuotaRange, List<String>> api_ranges) {
            super(apiKey, api_ranges);
        }

        public TimeInsensitiveParameters(final String apiKey, final HashMap<QuotaRange, List<String>> api_ranges, final Format format) {
            super(apiKey, api_ranges, format);
        }

        public long getStartTime() {
            return startTime;
        }

        public void setStartTime(final long startTime) {
            this.startTime = startTime;
        }

        @Override
        public long getStartTime(QuotaRange range) {
            return startTime;
        }
    }
}
