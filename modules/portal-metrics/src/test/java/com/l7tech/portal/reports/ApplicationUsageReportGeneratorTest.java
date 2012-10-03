package com.l7tech.portal.reports;

import com.l7tech.portal.reports.definition.ApplicationUsageReportDefinition;
import com.l7tech.portal.reports.format.Format;
import com.l7tech.portal.reports.parameter.ApplicationUsageReportParameters;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.*;

import static org.junit.Assert.*;

public class ApplicationUsageReportGeneratorTest extends AbstractReportGeneratorTestUtility {
    private List<String> applicationKeys;
    private ApplicationUsageReportParameters applicationUsageParameters;

    @Before
    public void setup() throws Exception{
        setupAbstractReportGeneratorTest();
        applicationKeys = new ArrayList<String>();
        // hsql equivalent of mysql's 'IF' is 'CASEWHEN'
        ApplicationUsageReportDefinition.SQL_TEMPLATE = ApplicationUsageReportDefinition.SQL_TEMPLATE.replaceAll("if", "casewhen");
        ApplicationUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_KEY = ApplicationUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_KEY.replaceAll("if", "casewhen");
    }

    @After
    public void teardown() throws Exception {
        teardownAbstractReportGeneratorTest();
    }

    @Test(expected = IllegalArgumentException.class)
    public void getApplicationUsageReportNullParams() throws Exception {
        generator.generateApplicationUsageReport(null, true, null, null);
    }

    /**
     * Groups by key.
     */
    @Test
    public void getApplicationUsageReportXml() throws Exception {
        applicationKeys.add("key1");
        applicationKeys.add("missing");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 2, 1, Format.XML, applicationKeys);
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId, 1);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, true, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/api_key", 2);
        assertNumberOfNodes(document, "UsageReport/api_key[@value='missing']", 1);
        assertNumberOfNodes(document, "UsageReport/api_key[@value='missing']/Usage", 0);
        assertNumberOfNodes(document, "UsageReport/api_key/Usage", 1);
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/bin_start_time", "1");
    }

    /**
     * Groups by key.
     */
    @Test
    public void getApplicationUsageReportJson() throws Exception {
        applicationKeys.add("key1");
        applicationKeys.add("missing");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 2, 1, Format.JSON, applicationKeys);
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId, 1);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, true, null, null);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(2, jsonObject.length());

        final JSONArray jsonArray1 = jsonObject.getJSONArray("key1");
        assertEquals(1, jsonArray1.length());
        final JSONObject usage = (JSONObject) jsonArray1.get(0);
        assertEquals(1L, usage.getLong("sum_hits_total"));
        assertEquals(1L, usage.getLong("sum_hits_success"));
        assertEquals(0, usage.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage.getLong("sum_latency"));
        assertEquals(0, usage.getLong("sum_gateway_latency"));
        assertEquals(1L, usage.getLong("sum_back_latency"));
        assertEquals(1L, usage.getLong("bin_start_time"));

        final JSONArray jsonArray2 = jsonObject.getJSONArray("missing");
        assertEquals(0, jsonArray2.length());
    }

    /**
     * Groups by key.
     */
    @Test
    public void getApplicationUsageReportXmlMultipleApiKeys() throws Exception {
        applicationKeys.add("key1");
        applicationKeys.add("key2");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 3, 1, Format.XML, applicationKeys);
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

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, true, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/api_key", 2);
        assertNumberOfNodes(document, "UsageReport/api_key/Usage", 2);
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/bin_start_time", "1");

        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage/bin_start_time", "1");
    }

    /**
     * Does not group by key.
     */
    @Test
    public void getApplicationUsageReportXmlMultipleApiKeysDoNotSeparateByApiKey() throws Exception {
        applicationKeys.add("key1");
        applicationKeys.add("key2");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 3, 1, Format.XML, applicationKeys);
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1);

        insertMappingValue(connection, 2, 1, Arrays.asList("key2", "method2"));
        insertServiceMetricDetail(connection, generatedId1, 2);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, false, null, null);

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
     * Groups by key.
     */
    @Test
    public void getApplicationUsageReportJsonMultipleApiKeys() throws Exception {
        applicationKeys.add("key1");
        applicationKeys.add("key2");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 3, 1, Format.JSON, applicationKeys);
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

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, true, null, null);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(2, jsonObject.length());

        final JSONArray jsonArray1 = jsonObject.getJSONArray("key1");
        assertEquals(1, jsonArray1.length());
        final JSONObject usage1 = (JSONObject) jsonArray1.get(0);
        assertEquals(1L, usage1.getLong("sum_hits_total"));
        assertEquals(1L, usage1.getLong("sum_hits_success"));
        assertEquals(0, usage1.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage1.getLong("sum_latency"));
        assertEquals(0, usage1.getLong("sum_gateway_latency"));
        assertEquals(1L, usage1.getLong("sum_back_latency"));
        assertEquals(1L, usage1.getLong("bin_start_time"));

        final JSONArray jsonArray2 = jsonObject.getJSONArray("key2");
        assertEquals(1, jsonArray2.length());
        final JSONObject usage2 = (JSONObject) jsonArray2.get(0);
        assertEquals(1L, usage2.getLong("sum_hits_total"));
        assertEquals(1L, usage2.getLong("sum_hits_success"));
        assertEquals(0, usage2.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage2.getLong("sum_latency"));
        assertEquals(0, usage2.getLong("sum_gateway_latency"));
        assertEquals(1L, usage2.getLong("sum_back_latency"));
        assertEquals(1L, usage2.getLong("bin_start_time"));
    }

    /**
     * Groups by key.
     */
    @Test
    public void getApplicationUsageReportJsonMultipleApiKeysDoNotSeparateByApiKey() throws Exception {
        applicationKeys.add("key1");
        applicationKeys.add("key2");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 3, 1, Format.JSON, applicationKeys);
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

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, false, null, null);

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
     * Does not group by key.
     */
    @Test
    public void getApplicationUsageReportXmlAddsMissingRows() throws Exception {
        applicationKeys.add("key1");
        applicationKeys.add("key2");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 4, 1, Format.XML, applicationKeys);
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 1, 3, 3, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1);
        insertServiceMetricDetail(connection, generatedId2, 1);

        insertMappingValue(connection, 2, 1, Arrays.asList("key2", "method2"));
        insertServiceMetricDetail(connection, generatedId1, 2);
        insertServiceMetricDetail(connection, generatedId2, 2);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, false, 1L, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 5);

        // missing rows
        assertNumberOfNodes(document, "UsageReport/Usage[1]/*", 1);
        assertXPathTextContent(document, "UsageReport/Usage[1]/bin_start_time", "0");
        assertNumberOfNodes(document, "UsageReport/Usage[3]/*", 1);
        assertXPathTextContent(document, "UsageReport/Usage[3]/bin_start_time", "2");
        assertNumberOfNodes(document, "UsageReport/Usage[5]/*", 1);
        assertXPathTextContent(document, "UsageReport/Usage[5]/bin_start_time", "4");

        // actual rows
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_hits_total", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_hits_success", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_hits_total", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_hits_success", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/bin_start_time", "3");
    }

    /**
     * Does not group by key.
     */
    @Test
    public void getApplicationUsageReportXmlAddsMissingRowsWithDefaults() throws Exception {
        applicationKeys.add("key1");
        applicationKeys.add("key2");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 4, 1, Format.XML, applicationKeys);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 1, 3, 3, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1);
        insertServiceMetricDetail(connection, generatedId2, 1);

        insertMappingValue(connection, 2, 1, Arrays.asList("key2", "method2"));
        insertServiceMetricDetail(connection, generatedId1, 2);
        insertServiceMetricDetail(connection, generatedId2, 2);

        defaultValues.put("sum_hits_total", 0);
        defaultValues.put("sum_hits_success", 0);
        defaultValues.put("sum_hits_total_errors", 0);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, false, 1L, defaultValues);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 5);

        // missing rows
        assertNumberOfNodes(document, "UsageReport/Usage[1]/*", 4);
        assertXPathTextContent(document, "UsageReport/Usage[1]/bin_start_time", "0");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_hits_total", "0");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_hits_success", "0");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_hits_total_errors", "0");


        assertNumberOfNodes(document, "UsageReport/Usage[3]/*", 4);
        assertXPathTextContent(document, "UsageReport/Usage[3]/bin_start_time", "2");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_hits_total", "0");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_hits_success", "0");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_hits_total_errors", "0");

        assertNumberOfNodes(document, "UsageReport/Usage[5]/*", 4);
        assertXPathTextContent(document, "UsageReport/Usage[5]/bin_start_time", "4");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_hits_total", "0");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_hits_success", "0");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_hits_total_errors", "0");

        // actual rows
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_hits_total", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_hits_success", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_hits_total", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_hits_success", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/bin_start_time", "3");
    }

    /**
     * Groups by key.
     */
    @Test
    public void getApplicationUsageReportXmlAddsMissingRowsAndGroups() throws Exception {
        applicationKeys.add("key1");
        applicationKeys.add("key2");
        applicationKeys.add("missing");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 4, 1, Format.XML, applicationKeys);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 1, 3, 3, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1);
        insertServiceMetricDetail(connection, generatedId2, 1);

        insertMappingValue(connection, 2, 1, Arrays.asList("key2", "method2"));
        insertServiceMetricDetail(connection, generatedId1, 2);
        insertServiceMetricDetail(connection, generatedId2, 2);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, true, 1L, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/api_key", 3);

        assertNumberOfNodes(document, "UsageReport/api_key[@value='key1']/Usage", 5);
        // missing rows
        assertNumberOfNodes(document, "UsageReport/api_key[@value='key1']/Usage[1]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[1]/bin_start_time", "0");
        assertNumberOfNodes(document, "UsageReport/api_key[@value='key1']/Usage[3]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[3]/bin_start_time", "2");
        assertNumberOfNodes(document, "UsageReport/api_key[@value='key1']/Usage[5]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[5]/bin_start_time", "4");
        // actual rows
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[2]/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[2]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[2]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[2]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[2]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[2]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[4]/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[4]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[4]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[4]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[4]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[4]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage[4]/bin_start_time", "3");

        assertNumberOfNodes(document, "UsageReport/api_key[@value='key2']/Usage", 5);
        // missing rows
        assertNumberOfNodes(document, "UsageReport/api_key[@value='key2']/Usage[1]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[1]/bin_start_time", "0");
        assertNumberOfNodes(document, "UsageReport/api_key[@value='key2']/Usage[3]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[3]/bin_start_time", "2");
        assertNumberOfNodes(document, "UsageReport/api_key[@value='key2']/Usage[5]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[5]/bin_start_time", "4");
        // actual rows
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[2]/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[2]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[2]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[2]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[2]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[2]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[4]/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[4]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[4]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[4]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[4]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[4]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key2']/Usage[4]/bin_start_time", "3");

        assertNumberOfNodes(document, "UsageReport/api_key[@value='missing']/Usage", 5);
        // missing rows
        assertNumberOfNodes(document, "UsageReport/api_key[@value='missing']/Usage[1]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_key[@value='missing']/Usage[1]/bin_start_time", "0");
        assertNumberOfNodes(document, "UsageReport/api_key[@value='missing']/Usage[2]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_key[@value='missing']/Usage[2]/bin_start_time", "1");
        assertNumberOfNodes(document, "UsageReport/api_key[@value='missing']/Usage[3]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_key[@value='missing']/Usage[3]/bin_start_time", "2");
        assertNumberOfNodes(document, "UsageReport/api_key[@value='missing']/Usage[4]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_key[@value='missing']/Usage[4]/bin_start_time", "3");
        assertNumberOfNodes(document, "UsageReport/api_key[@value='missing']/Usage[5]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_key[@value='missing']/Usage[5]/bin_start_time", "4");
    }

    /**
     * Does not group by key.
     * <p/>
     * Using overloaded method with no interval - should not add missing rows.
     */
    @Test
    public void getApplicationUsageReportXmlNoInterval() throws Exception {
        applicationKeys.add("key1");
        applicationKeys.add("key2");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 4, 1, Format.XML, applicationKeys);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 1, 3, 3, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId1, 1);
        insertServiceMetricDetail(connection, generatedId2, 1);

        insertMappingValue(connection, 2, 1, Arrays.asList("key2", "method2"));
        insertServiceMetricDetail(connection, generatedId1, 2);
        insertServiceMetricDetail(connection, generatedId2, 2);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, false);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 2);

        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_hits_total", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_hits_success", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/bin_start_time", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_hits_total", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_hits_success", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_back_latency", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/bin_start_time", "3");
    }

    /**
     * Ensure latency is calculated correctly across the whole cluster.
     * <p/>
     * Latency should be an average across the whole cluster, not a sum.
     */
    @Test
    public void getApplicationUsageReportXmlMultipleNodes() throws Exception {
        applicationKeys.add("key1");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 2, 1, Format.XML, applicationKeys);
        final Integer numRequests1 = 10;
        final Integer backSum1 = 100;
        final Integer frontSum1 = 1000;
        final Integer numRequests2 = 20;
        final Integer backSum2 = 200;
        final Integer frontSum2 = 2000;

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, "node1", 1, 1, 1, 1, 1, numRequests1, numRequests1, numRequests1, 1, 1, backSum1, 1, 1, frontSum1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 1, "node2", 1, 1, 1, 1, 1, numRequests2, numRequests2, numRequests2, 1, 1, backSum2, 1, 1, frontSum2, "1234abcd");

        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));

        insertServiceMetricDetail(connection, generatedId1, 1, numRequests1, numRequests1, numRequests1, 1, 1, backSum1, 1, 1, frontSum1);
        insertServiceMetricDetail(connection, generatedId2, 1, numRequests2, numRequests2, numRequests2, 1, 1, backSum2, 1, 1, frontSum2);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, false, null, null);

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
     * Ensure latency is calculated correctly across the whole cluster.
     * <p/>
     * Latency should be an average across the whole cluster, not a sum.
     */
    @Test
    public void getApplicationUsageReportXmlMultipleNodesSeparateByApiKey() throws Exception {
        applicationKeys.add("key1");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 2, 1, Format.XML, applicationKeys);
        final Integer numRequests1 = 10;
        final Integer backSum1 = 100;
        final Integer frontSum1 = 1000;
        final Integer numRequests2 = 20;
        final Integer backSum2 = 200;
        final Integer frontSum2 = 2000;

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId1 = insertServiceMetric(connection, 1, "node1", 1, 1, 1, 1, 1, numRequests1, numRequests1, numRequests1, 1, 1, backSum1, 1, 1, frontSum1, "1234abcd");
        final long generatedId2 = insertServiceMetric(connection, 1, "node2", 1, 1, 1, 1, 1, numRequests2, numRequests2, numRequests2, 1, 1, backSum2, 1, 1, frontSum2, "1234abcd");

        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));

        insertServiceMetricDetail(connection, generatedId1, 1, numRequests1, numRequests1, numRequests1, 1, 1, backSum1, 1, 1, frontSum1);
        insertServiceMetricDetail(connection, generatedId2, 1, numRequests2, numRequests2, numRequests2, 1, 1, backSum2, 1, 1, frontSum2);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, true, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/api_key/Usage", 1);
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_hits_total", "30");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_hits_success", "30");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_latency", "100");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_gateway_latency", "90");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/sum_back_latency", "10");
        assertXPathTextContent(document, "UsageReport/api_key[@value='key1']/Usage/bin_start_time", "1");
    }

    @Test
    public void getApplicationUsageReportXmlNoData() throws Exception {
        applicationKeys.add("key1");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 2, 1, Format.XML, applicationKeys);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, false, null, null);

        assertEquals("<UsageReport/>", data.trim());
    }

    @Test
    public void getApplicationUsageReportXmlNoDataGrouped() throws Exception {
        applicationKeys.add("key1");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 2, 1, Format.XML, applicationKeys);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, true, null, null);

        assertEquals("<UsageReport><api_key value=\"key1\"/></UsageReport>", data.trim());
    }

    @Test
    public void getApplicationUsageReportJsonNoData() throws Exception {
        applicationKeys.add("key1");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 2, 1, Format.JSON, applicationKeys);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, false, null, null);

        assertEquals("[]", data);
    }

    @Test
    public void getApplicationUsageReportJsonNoDataGrouped() throws Exception {
        applicationKeys.add("key1");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 2, 1, Format.JSON, applicationKeys);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, true, null, null);

        assertEquals("{\"key1\":[]}", data);
    }

    @Test(expected = ReportGenerationException.class)
    public void getApplicationUsageReportCannotExecuteQuery() throws Exception {
        connection.createStatement().execute("drop view api_key_or_method_usage_view");
        connection.createStatement().execute("create view api_key_or_method_usage_view as select * from service_metrics");

        applicationKeys.add("key1");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 2, 1, Format.JSON, applicationKeys);

        generator.generateApplicationUsageReport(applicationUsageParameters, true, null, null);
    }

    /**
     * Ensure no divide by zero error.
     */
    @Test
    public void getApplicationUsageReportZero() throws Exception {
        applicationKeys.add("key1");
        applicationKeys.add("missing");
        applicationUsageParameters = new ApplicationUsageReportParameters(0, 2, 1, Format.JSON, applicationKeys);
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        final String data = generator.generateApplicationUsageReport(applicationUsageParameters, true, null, null);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(2, jsonObject.length());

        final JSONArray jsonArray1 = jsonObject.getJSONArray("key1");
        assertEquals(1, jsonArray1.length());
        final JSONObject usage = (JSONObject) jsonArray1.get(0);
        assertEquals(0, usage.getLong("sum_hits_total"));
        assertEquals(0, usage.getLong("sum_hits_success"));
        assertEquals(0, usage.getLong("sum_hits_total_errors"));
        assertEquals(0, usage.getLong("sum_latency"));
        assertEquals(0, usage.getLong("sum_gateway_latency"));
        assertEquals(0, usage.getLong("sum_back_latency"));
        assertEquals(1L, usage.getLong("bin_start_time"));

        final JSONArray jsonArray2 = jsonObject.getJSONArray("missing");
        assertEquals(0, jsonArray2.length());
    }
}
