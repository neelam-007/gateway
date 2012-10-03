package com.l7tech.portal.reports;

import com.l7tech.portal.reports.definition.ApplicationLatencyRankingReportDefinition;
import com.l7tech.portal.reports.format.Format;
import com.l7tech.portal.reports.parameter.RankingReportParameters;
import org.json.JSONArray;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ApplicationLatencyRankingReportGeneratorTest extends AbstractReportGeneratorTestUtility {
    private RankingReportParameters parameters;

    @Before
    public void setup() throws Exception {
        setupAbstractReportGeneratorTest();
        parameters = new RankingReportParameters();
        // hsql equivalent of mysql's 'IF' is 'CASEWHEN'
        ApplicationLatencyRankingReportDefinition.QUERY = ApplicationLatencyRankingReportDefinition.QUERY.replaceAll("if", "casewhen");
    }

    @After
    public void teardown() throws Exception {
        teardownAbstractReportGeneratorTest();
    }

    @Test(expected = IllegalArgumentException.class)
    public void getReportNull() throws Exception {
        generator.generateApplicationLatencyRankingReport(null);
    }

    @Test
    public void getReportJson() throws Exception {
        parameters.setStartTime(1000);
        parameters.setEndTime(2000);
        parameters.setBinResolution(1);
        // get top 2
        parameters.setLimit(2);

        insertPublishedService(connection, 1, "uuid");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, null, keys);
        final long metric1 = insertServiceMetric(connection, 1, 1000, 1000, "uuid");
        final long metric2 = insertServiceMetric(connection, 1, 1100, 1100, "uuid");
        final long metric3 = insertServiceMetric(connection, 1, 1200, 1200, "uuid");
        // this metric should not be counted due to the start time
        final long metric4 = insertServiceMetric(connection, 1, 2000, 2000, "uuid");
        // this metric should not be counted due to resolution
        final long metric5 = insertServiceMetric(connection, 1, 1300, 1300, "uuid", 3);

        // application 1 - has the most latency
        insertMappingValue(connection, 1111, 1, Arrays.asList("key1", "method"));
        insertServiceMetricDetail(connection, metric1, 1111, 1000, 1000, 1000, 0, 50, 500, 0, 50, 3000);
        insertServiceMetricDetail(connection, metric2, 1111, 1000, 1000, 1000, 0, 50, 500, 0, 50, 3000);
        insertServiceMetricDetail(connection, metric3, 1111, 1000, 1000, 1000, 0, 50, 500, 0, 50, 3000);
        // this detail should not be counted due to the start time
        insertServiceMetricDetail(connection, metric4, 1111);
        // this detail should not be counted due to the resolution
        insertServiceMetricDetail(connection, metric5, 1111);

        // application 2 - has the least latency
        insertMappingValue(connection, 2222, 1, Arrays.asList("key2", "method"));
        insertServiceMetricDetail(connection, metric1, 2222, 1000, 1000, 1000, 0, 50, 500, 0, 50, 1000);
        insertServiceMetricDetail(connection, metric2, 2222, 1000, 1000, 1000, 0, 50, 500, 0, 50, 1000);
        insertServiceMetricDetail(connection, metric3, 2222, 1000, 1000, 1000, 0, 50, 500, 0, 50, 1000);

        // application 3
        insertMappingValue(connection, 3333, 1, Arrays.asList("key3", "method"));
        insertServiceMetricDetail(connection, metric1, 3333, 1000, 1000, 1000, 0, 50, 500, 0, 50, 2000);
        insertServiceMetricDetail(connection, metric2, 3333, 1000, 1000, 1000, 0, 50, 500, 0, 50, 2000);
        insertServiceMetricDetail(connection, metric3, 3333, 1000, 1000, 1000, 0, 50, 500, 0, 50, 2000);

        final String data = generator.generateApplicationLatencyRankingReport(parameters);

        final JSONArray jsonArray = new JSONArray(data);
        assertEquals(2, jsonArray.length());
        assertEquals("key1", jsonArray.getJSONObject(0).get("api_key"));
        assertEquals(3, jsonArray.getJSONObject(0).get("latency"));
        assertEquals("key3", jsonArray.getJSONObject(1).get("api_key"));
        assertEquals(2, jsonArray.getJSONObject(1).get("latency"));
    }

    @Test
    public void getReportJsonNone() throws Exception {
        parameters.setStartTime(1000);
        parameters.setEndTime(2000);
        parameters.setBinResolution(1);
        parameters.setLimit(2);

        final String data = generator.generateApplicationLatencyRankingReport(parameters);

        assertEquals("[]", data);
    }

    @Test
    public void getReportXml() throws Exception {
        parameters.setStartTime(1000);
        parameters.setEndTime(2000);
        parameters.setBinResolution(1);
        // get top 2
        parameters.setLimit(2);
        parameters.setFormat(Format.XML);

        insertPublishedService(connection, 1, "uuid");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, null, keys);
        final long metric1 = insertServiceMetric(connection, 1, 1000, 1000, "uuid");
        final long metric2 = insertServiceMetric(connection, 1, 1100, 1100, "uuid");
        final long metric3 = insertServiceMetric(connection, 1, 1200, 1200, "uuid");
        // this metric should not be counted due to the start time
        final long metric4 = insertServiceMetric(connection, 1, 2000, 2000, "uuid");
        // this metric should not be counted due to resolution
        final long metric5 = insertServiceMetric(connection, 1, 1300, 1300, "uuid", 3);

        // application 1 - has the most latency
        insertMappingValue(connection, 1111, 1, Arrays.asList("key1", "method"));
        insertServiceMetricDetail(connection, metric1, 1111, 1000, 1000, 1000, 0, 50, 500, 0, 50, 3000);
        insertServiceMetricDetail(connection, metric2, 1111, 1000, 1000, 1000, 0, 50, 500, 0, 50, 3000);
        insertServiceMetricDetail(connection, metric3, 1111, 1000, 1000, 1000, 0, 50, 500, 0, 50, 3000);
        // this detail should not be counted due to the start time
        insertServiceMetricDetail(connection, metric4, 1111);
        // this detail should not be counted due to the resolution
        insertServiceMetricDetail(connection, metric5, 1111);

        // application 2 - has the least latency
        insertMappingValue(connection, 2222, 1, Arrays.asList("key2", "method"));
        insertServiceMetricDetail(connection, metric1, 2222, 1000, 1000, 1000, 0, 50, 500, 0, 50, 1000);
        insertServiceMetricDetail(connection, metric2, 2222, 1000, 1000, 1000, 0, 50, 500, 0, 50, 1000);
        insertServiceMetricDetail(connection, metric3, 2222, 1000, 1000, 1000, 0, 50, 500, 0, 50, 1000);

        // application 3
        insertMappingValue(connection, 3333, 1, Arrays.asList("key3", "method"));
        insertServiceMetricDetail(connection, metric1, 3333, 1000, 1000, 1000, 0, 50, 500, 0, 50, 2000);
        insertServiceMetricDetail(connection, metric2, 3333, 1000, 1000, 1000, 0, 50, 500, 0, 50, 2000);
        insertServiceMetricDetail(connection, metric3, 3333, 1000, 1000, 1000, 0, 50, 500, 0, 50, 2000);

        final String data = generator.generateApplicationLatencyRankingReport(parameters);

        final Document document = buildDocumentFromXml(data);
        assertNumberOfNodes(document, "UsageReport/Usage", 2);
        assertXPathTextContent(document, "UsageReport/Usage[1]/api_key", "key1");
        assertXPathTextContent(document, "UsageReport/Usage[1]/latency", "3");
        assertXPathTextContent(document, "UsageReport/Usage[2]/api_key", "key3");
        assertXPathTextContent(document, "UsageReport/Usage[2]/latency", "2");
    }

    @Test
    public void getReportXmlNone() throws Exception {
        parameters.setStartTime(1000);
        parameters.setEndTime(2000);
        parameters.setBinResolution(1);
        parameters.setLimit(2);
        parameters.setFormat(Format.XML);

        final String data = generator.generateApplicationLatencyRankingReport(parameters);

        assertEquals("<UsageReport/>", data);
    }

    /**
     * Ensure no divide by zero occurs.
     */
    @Test
    public void getReportZero() throws Exception {
        parameters.setStartTime(1000);
        parameters.setEndTime(2000);
        parameters.setBinResolution(1);
        // get top 2
        parameters.setLimit(2);

        insertPublishedService(connection, 1, "uuid");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, null, keys);
        final long metric1 = insertServiceMetric(connection, 1, 1000, 1000, "uuid");

        // application
        insertMappingValue(connection, 1111, 1, Arrays.asList("key1", "method"));
        insertServiceMetricDetail(connection, metric1, 1111, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        final String data = generator.generateApplicationLatencyRankingReport(parameters);

        final JSONArray jsonArray = new JSONArray(data);
        assertEquals(1, jsonArray.length());
        assertEquals("key1", jsonArray.getJSONObject(0).get("api_key"));
        assertEquals(0, jsonArray.getJSONObject(0).get("latency"));
    }

    @Test
    public void getReportNoLimit() throws Exception {
        parameters.setStartTime(1000);
        parameters.setEndTime(2000);
        parameters.setBinResolution(1);
        // no limit
        parameters.setLimit(null);

        insertPublishedService(connection, 1, "uuid");
        final Map<String, String> keys = new LinkedHashMap<String, String>();
        keys.put("API_KEY", "Custom Mapping");
        keys.put("API_METHOD", "Custom Mapping");
        insertMappingKey(connection, 1, null, keys);
        final long metric1 = insertServiceMetric(connection, 1, 1000, 1000, "uuid");
        final long metric2 = insertServiceMetric(connection, 1, 1100, 1100, "uuid");
        final long metric3 = insertServiceMetric(connection, 1, 1200, 1200, "uuid");

        // application 1 - has the most latency
        insertMappingValue(connection, 1111, 1, Arrays.asList("key1", "method"));
        insertServiceMetricDetail(connection, metric1, 1111, 1000, 1000, 1000, 0, 50, 500, 0, 50, 3000);
        insertServiceMetricDetail(connection, metric2, 1111, 1000, 1000, 1000, 0, 50, 500, 0, 50, 3000);
        insertServiceMetricDetail(connection, metric3, 1111, 1000, 1000, 1000, 0, 50, 500, 0, 50, 3000);

        // application 2 - has the least latency
        insertMappingValue(connection, 2222, 1, Arrays.asList("key2", "method"));
        insertServiceMetricDetail(connection, metric1, 2222, 1000, 1000, 1000, 0, 50, 500, 0, 50, 1000);
        insertServiceMetricDetail(connection, metric2, 2222, 1000, 1000, 1000, 0, 50, 500, 0, 50, 1000);
        insertServiceMetricDetail(connection, metric3, 2222, 1000, 1000, 1000, 0, 50, 500, 0, 50, 1000);

        // application 3
        insertMappingValue(connection, 3333, 1, Arrays.asList("key3", "method"));
        insertServiceMetricDetail(connection, metric1, 3333, 1000, 1000, 1000, 0, 50, 500, 0, 50, 2000);
        insertServiceMetricDetail(connection, metric2, 3333, 1000, 1000, 1000, 0, 50, 500, 0, 50, 2000);
        insertServiceMetricDetail(connection, metric3, 3333, 1000, 1000, 1000, 0, 50, 500, 0, 50, 2000);

        final String data = generator.generateApplicationLatencyRankingReport(parameters);

        final JSONArray jsonArray = new JSONArray(data);
        assertEquals(3, jsonArray.length());
        assertEquals("key1", jsonArray.getJSONObject(0).get("api_key"));
        assertEquals(3, jsonArray.getJSONObject(0).get("latency"));
        assertEquals("key3", jsonArray.getJSONObject(1).get("api_key"));
        assertEquals(2, jsonArray.getJSONObject(1).get("latency"));
        assertEquals("key2", jsonArray.getJSONObject(2).get("api_key"));
        assertEquals(1, jsonArray.getJSONObject(2).get("latency"));
    }
}
