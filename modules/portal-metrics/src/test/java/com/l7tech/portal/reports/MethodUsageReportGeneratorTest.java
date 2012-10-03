package com.l7tech.portal.reports;

import com.l7tech.portal.reports.definition.MethodUsageReportDefinition;
import com.l7tech.portal.reports.format.Format;
import com.l7tech.portal.reports.parameter.MethodUsageReportParameters;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.*;

import static org.junit.Assert.*;

public class MethodUsageReportGeneratorTest extends AbstractReportGeneratorTestUtility {
    private List<String> apiMethods;
    private MethodUsageReportParameters methodUsageParameters;

    @Before
    public void setup() throws Exception{
        setupAbstractReportGeneratorTest();
        apiMethods = new ArrayList<String>();
        // hsql equivalent of mysql's 'IF' is 'CASEWHEN'
        MethodUsageReportDefinition.SQL_TEMPLATE = MethodUsageReportDefinition.SQL_TEMPLATE.replaceAll("if", "casewhen");
        MethodUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_METHOD = MethodUsageReportDefinition.SQL_TEMPLATE_GROUP_BY_METHOD.replaceAll("if", "casewhen");
    }

    @Test(expected = IllegalArgumentException.class)
    public void getMethodUsageReportNullParams() throws Exception {
        generator.generateMethodUsageReport(null, true, null, null);
    }

    @After
    public void teardown() throws Exception {
        teardownAbstractReportGeneratorTest();
    }

    /**
     * Groups by method.
     */
    @Test
    public void getMethodUsageReportXml() throws Exception {
        apiMethods.add("method1");
        apiMethods.add("missing");
        methodUsageParameters = new MethodUsageReportParameters(0, 2, 1, Format.XML, apiMethods);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId, 1);

        final String data = generator.generateMethodUsageReport(methodUsageParameters, true, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/api_method", 2);
        assertNumberOfNodes(document, "UsageReport/api_method/Usage", 1);
        assertNumberOfNodes(document, "UsageReport/api_method[@value='missing']", 1);
        assertNumberOfNodes(document, "UsageReport/api_method[@value='missing']/Usage", 0);
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/bin_start_time", "1");
    }

    /**
     * Groups by method.
     */
    @Test
    public void getMethodUsageReportJson() throws Exception {
        apiMethods.add("method1");
        apiMethods.add("missing");
        methodUsageParameters = new MethodUsageReportParameters(0, 2, 1, Format.JSON, apiMethods);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId, 1);

        final String data = generator.generateMethodUsageReport(methodUsageParameters, true, null, null);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(2, jsonObject.length());

        final JSONArray jsonArray1 = jsonObject.getJSONArray("method1");
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
     * Groups by method.
     */
    @Test
    public void getMethodUsageReportXmlMultipleMethods() throws Exception {
        apiMethods.add("method1");
        apiMethods.add("method2");
        methodUsageParameters = new MethodUsageReportParameters(0, 2, 1, Format.XML, apiMethods);

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

        final String data = generator.generateMethodUsageReport(methodUsageParameters, true, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/api_method", 2);
        assertNumberOfNodes(document, "UsageReport/api_method/Usage", 2);
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage/bin_start_time", "1");

        assertXPathTextContent(document, "UsageReport/api_method[@value='method2']/Usage/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method2']/Usage/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method2']/Usage/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method2']/Usage/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method2']/Usage/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method2']/Usage/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method2']/Usage/bin_start_time", "1");
    }

    /**
     * Does not group by method.
     */
    @Test
    public void getMethodUsageReportXmlMultipleMethodsDoNotSeparateByMethod() throws Exception {
        apiMethods.add("method1");
        apiMethods.add("method2");
        methodUsageParameters = new MethodUsageReportParameters(0, 2, 1, Format.XML, apiMethods);

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

        final String data = generator.generateMethodUsageReport(methodUsageParameters, false, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 1);
        assertXPathTextContent(document, "//sum_hits_total", "2");
        assertXPathTextContent(document, "//sum_hits_success", "2");
        assertXPathTextContent(document, "//sum_hits_total_errors", "0");
        assertXPathTextContent(document, "//sum_latency", "1");
        assertXPathTextContent(document, "//sum_gateway_latency", "0");
        assertXPathTextContent(document, "//sum_back_latency", "1");
        assertXPathTextContent(document, "//bin_start_time", "1");
    }

    /**
     * Groups by method.
     */
    @Test
    public void getMethodUsageReportJsonMultipleMethods() throws Exception {
        apiMethods.add("method1");
        apiMethods.add("method2");
        methodUsageParameters = new MethodUsageReportParameters(0, 2, 1, Format.JSON, apiMethods);

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

        final String data = generator.generateMethodUsageReport(methodUsageParameters, true, null, null);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(2, jsonObject.length());

        final JSONArray jsonArray1 = jsonObject.getJSONArray("method1");
        assertEquals(1, jsonArray1.length());
        final JSONObject usage1 = (JSONObject) jsonArray1.get(0);
        assertEquals(1L, usage1.getLong("sum_hits_total"));
        assertEquals(1L, usage1.getLong("sum_hits_success"));
        assertEquals(0, usage1.getLong("sum_hits_total_errors"));
        assertEquals(1L, usage1.getLong("sum_latency"));
        assertEquals(0, usage1.getLong("sum_gateway_latency"));
        assertEquals(1L, usage1.getLong("sum_back_latency"));
        assertEquals(1L, usage1.getLong("bin_start_time"));

        final JSONArray jsonArray2 = jsonObject.getJSONArray("method2");
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
     * Does not group by method.
     */
    @Test
    public void getMethodUsageReportJsonMultipleMethodsDoNotSeparateByMethod() throws Exception {
        apiMethods.add("method1");
        apiMethods.add("method2");
        methodUsageParameters = new MethodUsageReportParameters(0, 2, 1, Format.JSON, apiMethods);

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

        final String data = generator.generateMethodUsageReport(methodUsageParameters, false, null, null);

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
     * Does not group by method.
     */
    @Test
    public void getMethodUsageReportXmlAddsMissingRows() throws Exception {
        apiMethods.add("method1");
        apiMethods.add("method2");
        methodUsageParameters = new MethodUsageReportParameters(0, 4, 1, Format.XML, apiMethods);

        // missing metrics for period_start = 0, 2, 4
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

        final String data = generator.generateMethodUsageReport(methodUsageParameters, false, 1L, null);

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
     * Does not group by method.
     */
    @Test
    public void getMethodUsageReportXmlAddsMissingRowsWithDefaults() throws Exception {
        apiMethods.add("method1");
        apiMethods.add("method2");
        methodUsageParameters = new MethodUsageReportParameters(0, 4, 1, Format.XML, apiMethods);

        // missing metrics for period_start = 0, 2, 4
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

        final String data = generator.generateMethodUsageReport(methodUsageParameters, false, 1L, defaultValues);

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
     * Groups by method.
     */
    @Test
    public void getMethodUsageReportXmlAddsMissingRowsAndGroups() throws Exception {
        apiMethods.add("method1");
        apiMethods.add("method2");
        apiMethods.add("missing");
        methodUsageParameters = new MethodUsageReportParameters(0, 4, 1, Format.XML, apiMethods);

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

        final String data = generator.generateMethodUsageReport(methodUsageParameters, true, 1L, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/api_method", 3);

        assertNumberOfNodes(document, "UsageReport/api_method[@value='method1']/Usage", 5);
        // missing rows
        assertNumberOfNodes(document, "UsageReport/api_method[@value='method1']/Usage[1]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[1]/bin_start_time", "0");
        assertNumberOfNodes(document, "UsageReport/api_method[@value='method1']/Usage[3]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[3]/bin_start_time", "2");
        assertNumberOfNodes(document, "UsageReport/api_method[@value='method1']/Usage[5]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[5]/bin_start_time", "4");
        // actual rows
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[2]/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[2]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[2]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[2]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[2]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[2]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[4]/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[4]/sum_hits_success", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[4]/sum_hits_total_errors", "0");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[4]/sum_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[4]/sum_gateway_latency", "0");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[4]/sum_back_latency", "1");
        assertXPathTextContent(document, "UsageReport/api_method[@value='method1']/Usage[4]/bin_start_time", "3");

        assertNumberOfNodes(document, "UsageReport/api_method[@value='missing']/Usage", 5);
        // missing rows
        assertNumberOfNodes(document, "UsageReport/api_method[@value='missing']/Usage[1]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_method[@value='missing']/Usage[1]/bin_start_time", "0");
        assertNumberOfNodes(document, "UsageReport/api_method[@value='missing']/Usage[2]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_method[@value='missing']/Usage[2]/bin_start_time", "1");
        assertNumberOfNodes(document, "UsageReport/api_method[@value='missing']/Usage[3]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_method[@value='missing']/Usage[3]/bin_start_time", "2");
        assertNumberOfNodes(document, "UsageReport/api_method[@value='missing']/Usage[4]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_method[@value='missing']/Usage[4]/bin_start_time", "3");
        assertNumberOfNodes(document, "UsageReport/api_method[@value='missing']/Usage[5]/*", 1);
        assertXPathTextContent(document, "UsageReport/api_method[@value='missing']/Usage[5]/bin_start_time", "4");
    }

    /**
     * Does not group by method.
     * <p/>
     * Using overloaded method without interval - should not add missing rows.
     */
    @Test
    public void getMethodUsageReportXmlNoInterval() throws Exception {
        apiMethods.add("method1");
        apiMethods.add("method2");
        methodUsageParameters = new MethodUsageReportParameters(0, 4, 1, Format.XML, apiMethods);

        // missing metrics for period_start = 0, 2, 4
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

        final String data = generator.generateMethodUsageReport(methodUsageParameters, false);

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

    @Test
    public void getMethodUsageReportXmlNoData() throws Exception {
        apiMethods.add("method1");
        methodUsageParameters = new MethodUsageReportParameters(0, 2, 1, Format.XML, apiMethods);

        final String data = generator.generateMethodUsageReport(methodUsageParameters, false, null, null);

        assertEquals("<UsageReport/>", data.trim());
    }

    @Test
    public void getMethodUsageReportXmlNoDataGrouped() throws Exception {
        apiMethods.add("method1");
        methodUsageParameters = new MethodUsageReportParameters(0, 2, 1, Format.XML, apiMethods);

        final String data = generator.generateMethodUsageReport(methodUsageParameters, true, null, null);

        assertEquals("<UsageReport><api_method value=\"method1\"/></UsageReport>", data.trim());
    }

    @Test
    public void getMethodUsageReportJsonNoData() throws Exception {
        apiMethods.add("method1");
        methodUsageParameters = new MethodUsageReportParameters(0, 2, 1, Format.JSON, apiMethods);

        final String data = generator.generateMethodUsageReport(methodUsageParameters, false, null, null);

        assertEquals("[]", data);
    }

    @Test
    public void getMethodUsageReportJsonNoDataGrouped() throws Exception {
        apiMethods.add("method1");
        methodUsageParameters = new MethodUsageReportParameters(0, 2, 1, Format.JSON, apiMethods);

        final String data = generator.generateMethodUsageReport(methodUsageParameters, true, null, null);

        assertEquals("{\"method1\":[]}", data);
    }

    /**
     * Ensure no divide by zero error.
     */
    @Test
    public void getMethodUsageReportZero() throws Exception {
        apiMethods.add("method1");
        apiMethods.add("missing");
        methodUsageParameters = new MethodUsageReportParameters(0, 2, 1, Format.JSON, apiMethods);

        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        final String data = generator.generateMethodUsageReport(methodUsageParameters, true, null, null);

        final JSONObject jsonObject = new JSONObject(data);
        assertEquals(2, jsonObject.length());

        final JSONArray jsonArray1 = jsonObject.getJSONArray("method1");
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
