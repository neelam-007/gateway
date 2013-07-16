package com.l7tech.portal.reports;

import com.l7tech.portal.reports.definition.UsageSummaryReportDefinition;
import com.l7tech.portal.reports.format.Format;
import com.l7tech.portal.reports.parameter.UsageSummaryReportParameters;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.*;

import static org.junit.Assert.assertEquals;

/**
 * Has HQSL specific error back in pbody
 */
@Ignore
public class UsageSummaryReportGeneratorTest extends AbstractReportGeneratorTestUtility {
    private List<String> applicationKeys;
    private UsageSummaryReportParameters usageSummaryReportParameters;

    @Before
    public void setup() throws Exception{
        setupAbstractReportGeneratorTest();
        applicationKeys = new ArrayList<String>();
        // hsql equivalent of mysql's 'IF' is 'CASEWHEN'
        //UsageSummaryReportDefinition.SQL_TEMPLATE = UsageSummaryReportDefinition.SQL_TEMPLATE.replace("(date(FROM_UNIXTIME(bin_start_time/1000)))", "TIMESTAMP((bin_start_time/1000))");
        //UsageSummaryReportDefinition.SQL_TEMPLATE = UsageSummaryReportDefinition.SQL_TEMPLATE.replace("(date(date_day))", "TIMESTAMP(bin_start_time/1000)");
        UsageSummaryReportDefinition.SQL_TEMPLATE = "select sum(hits_total) as sum_hits_total, bin_start_time/1000 as bin_start_date from api_key_or_method_usage_view where bin_start_time >= ? and bin_end_time < ? and resolution = 1 and api_key in (:apiKeys) group by   bin_start_time/1000  order by bin_start_date asc";
    }

    @Test(expected = IllegalArgumentException.class)
    public void getApplicationUsageReportNullParams() throws Exception {
        generator.generateUsageSummaryReport(null, null, null);
    }

    /**
     * Groups by key.
     */
    @Test
    public void getApplicationUsageReportXml() throws Exception {
        applicationKeys.add("key1");
        applicationKeys.add("missing");
        usageSummaryReportParameters = new UsageSummaryReportParameters(0, 2, 1, Format.XML, applicationKeys);
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId, 1);

        final String data = generator.generateUsageSummaryReport(usageSummaryReportParameters, null, null);

        final Document document = buildDocumentFromXml(data);
        assertXPathTextContent(document, "UsageReport/Usage/sum_hits_total", "1");
        assertXPathTextContent(document, "UsageReport/Usage/bin_start_time", "1");
    }

    /**
     * Groups by key.
     */
    @Test
    public void getApplicationUsageReportJson() throws Exception {
        /*applicationKeys.add("key1");
        applicationKeys.add("missing");
        usageSummaryReportParameters = new UsageSummaryReportParameters(0, 2, 1, Format.JSON, applicationKeys);
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId, 1);

        final String data = generator.generateUsageSummaryReport(usageSummaryReportParameters, null, null);

        final JSONArray jsonArray = new JSONArray(data);
        assertEquals(1, jsonArray.length());

        final JSONObject usage = (JSONObject) jsonArray.get(0);
        assertEquals(1L, usage.getLong("sum_hits_total"));
        assertEquals(1L, usage.getLong("bin_start_time"));*/
    }

    /**
     * Groups by key.
     */
    @Test
    public void getApplicationUsageReportXmlMultipleApiKeys() throws Exception {
        /*applicationKeys.add("key1");
        applicationKeys.add("key2");
        usageSummaryReportParameters = new UsageSummaryReportParameters(0, 3, 1, Format.XML, applicationKeys);
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

        final String data = generator.generateUsageSummaryReport(usageSummaryReportParameters, null, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 1);
        assertXPathTextContent(document, "UsageReport/Usage/sum_hits_total", "2");
        assertXPathTextContent(document, "UsageReport/Usage/bin_start_time", "1");*/
    }

    /**
     * Does not group by key.
     */
    @Test
    public void getApplicationUsageReportXmlAddsMissingRows() throws Exception {
        /*applicationKeys.add("key1");
        applicationKeys.add("key2");
        usageSummaryReportParameters = new UsageSummaryReportParameters(0, 4, 1, Format.XML, applicationKeys);
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

        final String data = generator.generateUsageSummaryReport(usageSummaryReportParameters, 1L, null);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 5);

        // missing rows
        assertNumberOfNodes(document, "UsageReport/Usage[1]*//*", 1);
        assertXPathTextContent(document, "UsageReport/Usage[1]/bin_start_time", "0");
        assertNumberOfNodes(document, "UsageReport/Usage[3]*//*", 1);
        assertXPathTextContent(document, "UsageReport/Usage[3]/bin_start_time", "2");
        assertNumberOfNodes(document, "UsageReport/Usage[5]*//*", 1);
        assertXPathTextContent(document, "UsageReport/Usage[5]/bin_start_time", "4");

        // actual rows
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_hits_total", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_hits_total", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/bin_start_time", "3");*/
    }

    /**
     * Does not group by key.
     */
    @Test
    public void getApplicationUsageReportXmlAddsMissingRowsWithDefaults() throws Exception {
        /*applicationKeys.add("key1");
        applicationKeys.add("key2");
        usageSummaryReportParameters = new UsageSummaryReportParameters(0, 4, 1, Format.XML, applicationKeys);

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
        defaultValues.put("sum_hits_total_errors", 0);

        final String data = generator.generateUsageSummaryReport(usageSummaryReportParameters, 1L, defaultValues);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 5);

        // missing rows
        assertNumberOfNodes(document, "UsageReport/Usage[1]*//*", 4);
        assertXPathTextContent(document, "UsageReport/Usage[1]/bin_start_time", "0");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_hits_total", "0");
        assertXPathTextContent(document, "UsageReport/Usage[1]/sum_hits_total_errors", "0");


        assertNumberOfNodes(document, "UsageReport/Usage[3]*//*", 4);
        assertXPathTextContent(document, "UsageReport/Usage[3]/bin_start_time", "2");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_hits_total", "0");
        assertXPathTextContent(document, "UsageReport/Usage[3]/sum_hits_total_errors", "0");

        assertNumberOfNodes(document, "UsageReport/Usage[5]*//*", 4);
        assertXPathTextContent(document, "UsageReport/Usage[5]/bin_start_time", "4");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_hits_total", "0");
        assertXPathTextContent(document, "UsageReport/Usage[5]/sum_hits_total_errors", "0");

        // actual rows
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_hits_total", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/bin_start_time", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/sum_hits_total", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[4]/bin_start_time", "3");*/
    }

    /**
     * Does not group by key.
     * <p/>
     * Using overloaded method with no interval - should not add missing rows.
     */
    @Test
    public void getApplicationUsageReportXmlNoInterval() throws Exception {
        /*applicationKeys.add("key1");
        applicationKeys.add("key2");
        usageSummaryReportParameters = new UsageSummaryReportParameters(0, 4, 1, Format.XML, applicationKeys);

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

        final String data = generator.generateUsageSummaryReport(usageSummaryReportParameters);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 2);

        assertXPathTextContent(document, "/UsageReport/Usage[1]/sum_hits_total", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[1]/bin_start_time", "1");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/sum_hits_total", "2");
        assertXPathTextContent(document, "/UsageReport/Usage[2]/bin_start_time", "3");*/
    }

    @Test
    public void getApplicationUsageReportXmlNoData() throws Exception {
        /*applicationKeys.add("key1");
        usageSummaryReportParameters = new UsageSummaryReportParameters(0, 2, 1, Format.XML, applicationKeys);

        final String data = generator.generateUsageSummaryReport(usageSummaryReportParameters, null, null);

        assertEquals("<UsageReport/>", data.trim());*/
    }

    @Test
    public void getApplicationUsageReportXmlNoDataGrouped() throws Exception {
        /*applicationKeys.add("key1");
        usageSummaryReportParameters = new UsageSummaryReportParameters(0, 2, 1, Format.XML, applicationKeys);

        final String data = generator.generateUsageSummaryReport(usageSummaryReportParameters, null, null);

        assertEquals("<UsageReport/>", data.trim());*/
    }

    @Test
    public void getApplicationUsageReportJsonNoData() throws Exception {
        /*applicationKeys.add("key1");
        usageSummaryReportParameters = new UsageSummaryReportParameters(0, 2, 1, Format.JSON, applicationKeys);

        final String data = generator.generateUsageSummaryReport(usageSummaryReportParameters, null, null);

        assertEquals("[]", data);*/
    }

    @Test
    public void getApplicationUsageReportJsonNoDataGrouped() throws Exception {
        /*applicationKeys.add("key1");
        usageSummaryReportParameters = new UsageSummaryReportParameters(0, 2, 1, Format.JSON, applicationKeys);

        final String data = generator.generateUsageSummaryReport(usageSummaryReportParameters, null, null);

        assertEquals("[]", data);*/
    }

    @Test(expected = ReportGenerationException.class)
    public void getApplicationUsageReportCannotExecuteQuery() throws Exception {
        connection.createStatement().execute("drop view api_key_or_method_usage_view");
        connection.createStatement().execute("create view api_key_or_method_usage_view as select * from service_metrics");

        applicationKeys.add("key1");
        usageSummaryReportParameters = new UsageSummaryReportParameters(0, 2, 1, Format.JSON, applicationKeys);

        generator.generateUsageSummaryReport(usageSummaryReportParameters, null, null);
    }

    /*//**
     * Ensure no divide by zero error.
     *//*
    @Test
    public void getApplicationUsageReportZero() throws Exception {
        applicationKeys.add("key1");
        applicationKeys.add("missing");
        usageSummaryReportParameters = new ApplicationUsageReportParameters(0, 2, 1, Format.JSON, applicationKeys);
        insertPublishedService(connection, 1, "1234abcd");
        final long generatedId = insertServiceMetric(connection, 1, 1, 1, "1234abcd");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, keys);
        insertMappingValue(connection, 1, 1, Arrays.asList("key1", "method1"));
        insertServiceMetricDetail(connection, generatedId, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        final String data = generator.generateApplicationUsageReport(usageSummaryReportParameters, true, null, null);

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
    }*/
}
