package com.l7tech.portal.reports;

import com.l7tech.portal.reports.definition.*;
import com.l7tech.portal.reports.format.*;
import com.l7tech.portal.reports.parameter.*;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.xml.sax.SAXException;

import javax.sql.DataSource;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Report data generator for service metrics.
 */
public class MetricsReportGenerator {
    public static final String BIN_START_TIME = "bin_start_time";
    public static final String API_ID = "api_id";
    public static final String UUID = "uuid";
    public static final String API_KEY = "api_key";
    public static final String API_METHOD = "api_method";
    private final DataSource dataSource;
    private final XmlResultSetFormatter xmlFormatter;
    private final JsonResultSetFormatter jsonFormatter;

    public MetricsReportGenerator(final DataSource dataSource) {
        Validate.notNull(dataSource, "DataSource cannot be null");
        this.dataSource = dataSource;
        xmlFormatter = new XmlResultSetFormatter("UsageReport", "Usage");
        jsonFormatter = new JsonResultSetFormatter();
    }

    public XmlResultSetFormatter getXmlFormatter() {
        return xmlFormatter;
    }

    public JsonResultSetFormatter getJsonFormatter() {
        return jsonFormatter;
    }

    /**
     * Generates data for an API usage report.
     *
     * @param params         the input parameters used to generate the report.
     * @param groupDataByApi whether data should be grouped by API id. If false, data will be aggregated.
     * @return the usage report data formatted as a String.
     * @throws SQLException              if an error occurs opening or closing a connection to the database.
     * @throws ReportGenerationException if an unexpected error occurs attempting to generate the report data.
     */
    public String generateApiUsageReport(@NotNull final ApiUsageReportParameters params, final boolean groupDataByApi) throws SQLException {
        return generateApiUsageReport(params, groupDataByApi, null, null);
    }

    /**
     * Generates data for an API usage report.
     *
     * @param params                     the input parameters used to generate the report.
     * @param groupDataByApi             whether data should be grouped by API id. If false, data will be aggregated.
     * @param millisecondIntervalForDate the regular date interval in milliseconds that the report is expected to have data for. If any data is detected as missing, empty data will be added.
     * @param defaultValues              a map of default values to insert if missing data is detected. Key = name, value = value to insert.
     * @return the usage report data formatted as a String.
     * @throws SQLException              if an error occurs opening or closing a connection to the database.
     * @throws ReportGenerationException if an unexpected error occurs attempting to generate the report data.
     */
    public String generateApiUsageReport(@NotNull final ApiUsageReportParameters params, final boolean groupDataByApi, @Nullable final Long millisecondIntervalForDate, @Nullable final Map<String, Object> defaultValues) throws SQLException {
        if (groupDataByApi) {
            return doReport(new ApiUsageReportDefinition(params, true), UUID, params.getUuids(), BIN_START_TIME, millisecondIntervalForDate, defaultValues);
        } else {
            return doReport(new ApiUsageReportDefinition(params, false), null, null, BIN_START_TIME, millisecondIntervalForDate, defaultValues);
        }
    }

    /**
     * Generates data for an API usage comparison report.
     *
     * @param params the input parameters used to generate the report.
     * @return the usage comparison report data formatted as a String.
     * @throws SQLException              if an error occurs opening or closing a connection to the database.
     * @throws ReportGenerationException if an unexpected error occurs attempting to generate the report data.
     */
    public String generateApiUsageComparisonReport(@NotNull final ApiUsageReportParameters params) throws SQLException {
        return doReport(new ApiUsageComparisonReportDefinition(params), null, null, null, null, null);
    }

    /**
     * Generates data for an application (API key) usage report.
     *
     * @param params            the input parameters used to generate the report.
     * @param groupDataByApiKey whether data should be grouped by API key.  If false, data will be aggregated.
     * @return the application usage report data formatted as a String.
     * @throws SQLException              if an error occurs opening or closing a connection to the database.
     * @throws ReportGenerationException if an unexpected error occurs attempting to generate the report data.
     */
    public String generateApplicationUsageReport(@NotNull final ApplicationUsageReportParameters params, final boolean groupDataByApiKey) throws SQLException {
        return generateApplicationUsageReport(params, groupDataByApiKey, null, null);
    }

    /**
     * Generates data for an application (API key) usage report.
     *
     * @param params                     the input parameters used to generate the report.
     * @param groupDataByApiKey          whether data should be grouped by API key.  If false, data will be aggregated.
     * @param millisecondIntervalForDate the regular date interval in milliseconds that the report is expected to have data for. If any data is detected as missing, empty data will be added.
     * @param defaultValues              a map of default values to insert if missing data is detected. Key = name, value = value to insert.
     * @return the application usage report data formatted as a String.
     * @throws SQLException              if an error occurs opening or closing a connection to the database.
     * @throws ReportGenerationException if an unexpected error occurs attempting to generate the report data.
     */
    public String generateApplicationUsageReport(@NotNull final ApplicationUsageReportParameters params, final boolean groupDataByApiKey, @Nullable final Long millisecondIntervalForDate, @Nullable final Map<String, Object> defaultValues) throws SQLException {
        if (groupDataByApiKey) {
            return doReport(new ApplicationUsageReportDefinition(params, true), API_KEY, params.getApplicationKeys(), BIN_START_TIME, millisecondIntervalForDate, defaultValues);
        } else {
            return doReport(new ApplicationUsageReportDefinition(params, false), null, null, BIN_START_TIME, millisecondIntervalForDate, defaultValues);
        }
    }

    /**
     * Generates data for an API method usage report.
     *
     * @param params               the input parameters used to generate the report.
     * @param groupDataByApiMethod whether data should be grouped by API method.  If false, data will be aggregated.
     * @return the method usage report data formatted as a String.
     * @throws SQLException              if an error occurs opening or closing a connection to the database.
     * @throws ReportGenerationException if an unexpected error occurs attempting to generate the report data.
     */
    public String generateMethodUsageReport(@NotNull final MethodUsageReportParameters params, final boolean groupDataByApiMethod) throws SQLException {
        return generateMethodUsageReport(params, groupDataByApiMethod, null, null);
    }

    /**
     * Generates data for an API method usage report.
     *
     * @param params                     the input parameters used to generate the report.
     * @param groupDataByApiMethod       whether data should be grouped by API method.  If false, data will be aggregated.
     * @param millisecondIntervalForDate the regular date interval in milliseconds that the report is expected to have data for. If any data is detected as missing, empty data will be added.
     * @param defaultValues              a map of default values to insert if missing data is detected. Key = name, value = value to insert.
     * @return the method usage report data formatted as a String.
     * @throws SQLException              if an error occurs opening or closing a connection to the database.
     * @throws ReportGenerationException if an unexpected error occurs attempting to generate the report data.
     */
    public String generateMethodUsageReport(@NotNull final MethodUsageReportParameters params, final boolean groupDataByApiMethod, @Nullable final Long millisecondIntervalForDate, @Nullable final Map<String, Object> defaultValues) throws SQLException {
        if (groupDataByApiMethod) {
            return doReport(new MethodUsageReportDefinition(params, true), API_METHOD, params.getApiMethods(), BIN_START_TIME, millisecondIntervalForDate, defaultValues);
        } else {
            return doReport(new MethodUsageReportDefinition(params, false), null, null, BIN_START_TIME, millisecondIntervalForDate, defaultValues);
        }
    }


    /**
     * Generate data for the quota usage report.
     *
     * @param params An ApiQuotaUsageReportParameters object containing a lists of valid mappings for UUIDS and QuotaRanges.
     * @return JSON/XML string which will have the following <br/>
     *         Api Key <br/>
     *         hits <br/>
     *         range (1=sec,2=min,3=hour,4=day,5=month)<br/>
     *         uuid<br/>
     * @throws SQLException              if an error occurs opening or closing a connection to the database.
     * @throws ReportGenerationException if an unexpected error occurs attempting to generate the report data.
     */
    public String generateApiQuotaUsageReport(@NotNull final ApiQuotaUsageReportParameters params) throws SQLException {
        HashMap<DefaultReportParameters.QuotaRange, List<String>> api_ranges = params.getApiRanges();
        Map<DefaultReportParameters.QuotaRange, String> return_list = new HashMap<DefaultReportParameters.QuotaRange, String>();
        for (DefaultReportParameters.QuotaRange range : api_ranges.keySet()) {

            HashMap<DefaultReportParameters.QuotaRange, List<String>> temp_api_ranges = new HashMap<DefaultReportParameters.QuotaRange, List<String>>();
            temp_api_ranges.put(range, api_ranges.get(range));
            ApiQuotaUsageReportParameters new_param = getQuotaParameters(params, temp_api_ranges);
            String report_output = doReport(new ApiQuotaUsageReportDefinition(new_param));
            return_list.put(range, report_output);
        }
        try {
            return transform(return_list, params);
        } catch (Exception e) {
            throw new ReportGenerationException("Error parsing the JSON/XML", e);
        }
    }

    /**
     * Generate data for an application (API key) ranking report (most requested applications).
     * <p/>
     * The data will be in descending order by number of requests.
     *
     * @param params input parameters used to generate the report.
     * @return the application ranking report formatted as a string.
     * @throws SQLException              if an error occurs opening or closing a connection to the database.
     * @throws ReportGenerationException if an unexpected error occurs attempting to generate the report data.
     */
    public String generateApplicationRankingReport(@NotNull final RankingReportParameters params) throws SQLException {
        return doReport(new ApplicationRankingReportDefinition(params));
    }

    /**
     * Generate data for an application (API key) latency ranking report (applications with the highest latency).
     * <p/>
     * The data will be in descending order by latency.
     *
     * @param params input parameters used to generate the report.
     * @return the application latency ranking report formatted as a string.
     * @throws SQLException              if an error occurs opening or closing a connection to the database.
     * @throws ReportGenerationException if an unexpected error occurs attempting to generate the report data.
     */
    public String generateApplicationLatencyRankingReport(@NotNull final RankingReportParameters params) throws SQLException {
        return doReport(new ApplicationLatencyRankingReportDefinition(params));
    }

    /**
     * Generate data for a latency ranking report (metric bins with the highest latency).
     * <p/>
     * Useful for finding information about latency spikes.
     * <p/>
     * The data will be in descending order by latency.
     *
     * @param params input parameters used to generate the report.
     * @return the latency ranking report formatted as a string.
     * @throws SQLException              if an error occurs opening or closing a connection to the database.
     * @throws ReportGenerationException if an unexpected error occurs attempting to generate the report data.
     */
    public String generateLatencyRankingReport(@NotNull final RankingReportParameters params) throws SQLException {
        return doReport(new LatencyRankingReportDefinition(params));
    }

    /**
     * This is extracted to a restricted method which can be overridden in unit tests (to avoid time-sensitive unit tests).
     */
    ApiQuotaUsageReportParameters getQuotaParameters(ApiQuotaUsageReportParameters params, HashMap<DefaultReportParameters.QuotaRange, List<String>> temp_api_ranges) {
        return new ApiQuotaUsageReportParameters(params.getApiKey(), temp_api_ranges, params.getFormat());
    }


    /**
     * Transform the JSON/XML from standard reports format to the format described in
     * Jira ticket LRS1179
     *
     * @param data   A map of the Quota range and the standard XMl/JSON report
     * @param params ApiQuotaUsageReportParameters object
     * @return Transformed XML/JSON
     * @throws JSONException                - Json Parse Exception
     * @throws ParserConfigurationException XML Exception
     * @throws IOException                  XML parser exception
     * @throws SAXException                 XML parser exception
     * @throws TransformerException         XML transformation execption
     */
    private String transform(final Map<DefaultReportParameters.QuotaRange, String> data, final ApiQuotaUsageReportParameters params) throws JSONException, ParserConfigurationException, IOException, SAXException, TransformerException {

        params.getApiRanges();
        if (params.getFormat() == Format.JSON) {
            return jsonFormatter.formatQuotaUsageJSON(data, params);
        } else if (params.getFormat() == Format.XML) {
            return xmlFormatter.formatQuotaUsageXML(data, params);
        }
        return null;
    }

    private String doReport(final ApiQuotaUsageReportDefinition reportDefinition) throws SQLException {
        Connection connection = null;

        // Since the this particular ApiQuotaUsageReportDefinition will ALWAYS have ONLY ONE entry in the APIRange hash map, it's not "un-prudent" to
        // always get the 1st element.
        DefaultReportParameters.QuotaRange quota = (DefaultReportParameters.QuotaRange) reportDefinition.getParams().getApiRanges().keySet().toArray()[0];
        List<String> uuids = reportDefinition.getParams().getApiRanges().get(quota);
        try {
            connection = dataSource.getConnection();
            final ResultSet resultSet = getResultSet(reportDefinition, connection, quota, uuids);
            final ResultSetFormatOptions options = new ResultSetFormatOptions();
            options.setGroupingColumnName(API_KEY);
            return formatResultSet(reportDefinition, resultSet, options);

        } finally {
            if (connection != null) {
                connection.close();
            }
        }

    }

    private String doReport(final ReportDefinition reportDefinition) throws SQLException {
        return doReport(reportDefinition, null, null, null, null, null);
    }

    private String doReport(final ReportDefinition reportDefinition, final String groupingColumnName, final List<String> expectedGroups, final String dateColumnName, final Long millisecondIntervalForDate, final Map<String, Object> defaultValues) throws SQLException {
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            final ResultSet resultSet = getResultSet(reportDefinition, connection);
            final DefaultReportParameters params = reportDefinition.getParams();
            final ResultSetFormatOptions options = new ResultSetFormatOptions();
            options.setGroupingColumnName(groupingColumnName);
            options.setExpectedGroups(expectedGroups);
            if (StringUtils.isNotBlank(dateColumnName) && millisecondIntervalForDate != null) {
                options.setNumericColumnName(dateColumnName);
                options.setExpectedInterval(millisecondIntervalForDate);
                options.setExpectedStartValue(params.getStartTime());
                options.setExpectedEndValue(params.getEndTime());
                options.setDefaultColumnValues(defaultValues);
            }
            return formatResultSet(reportDefinition, resultSet, options);
        } finally {
            if (connection != null) {
                connection.close();
            }
        }
    }

    private String formatResultSet(final ReportDefinition reportDefinition, final ResultSet resultSet, final ResultSetFormatOptions options) {
        String reportData;
        try {
            switch (reportDefinition.getParams().getFormat()) {
                case JSON:
                    reportData = jsonFormatter.format(resultSet, options);
                    break;
                case XML:
                    reportData = xmlFormatter.format(resultSet, options);
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported format: " + reportDefinition.getParams().getFormat());
            }
        } catch (final FormatException e) {
            throw new ReportGenerationException("Error formatting result set: "
                    + e.getMessage(), e);
        }
        return reportData;
    }

    private ResultSet getResultSet(final ReportDefinition reportDefinition, final Connection connection) {
        ResultSet resultSet = null;
        try {
            final String query = reportDefinition.generateSQLQuery();
            final PreparedStatement statement = connection.prepareStatement(query);
            reportDefinition.setSQLParams(statement);
            resultSet = statement.executeQuery();
        } catch (final SQLException e) {
            throw new ReportGenerationException("Error retrieving result set: "
                    + e.getMessage(), e);
        }
        return resultSet;
    }

    private ResultSet getResultSet(final ApiQuotaUsageReportDefinition reportDefinition, final Connection connection, final DefaultReportParameters.QuotaRange quota, final List<String> uuids) {
        ResultSet resultSet = null;
        try {
            final String query = reportDefinition.generateSQLQuery(uuids, quota);
            final PreparedStatement statement = connection.prepareStatement(query);
            reportDefinition.setSQLParams(statement, uuids, quota);
            resultSet = statement.executeQuery();
        } catch (final SQLException e) {
            throw new ReportGenerationException("Error retrieving result set: "
                    + e.getMessage(), e);
        }
        return resultSet;
    }
}
