package com.l7tech.gateway.standardreports;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.ArrayList;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.ParseException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import com.l7tech.util.ResourceUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.common.io.ResourceMapEntityResolver;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.util.SyspropUtil;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;

/**
 * Generates a report.
 */
public class ReportGenerator {
    private static final int PAGE_WIDTH_NO_BORDERS = 820;
    private static final int SUB_REPORT_WIDTH = 707;
    private static final int PAGE_WIDTH_INCLUDING_BORDERS = 850;
    private static final int REPORT_INFO_FRAME_RIGHT_PADDING = 7;
    private static final String REPORT_DATA_SOURCE = "REPORT_DATA_SOURCE";

    //- PUBLIC

    public ReportHandle compileReport(final ReportApi.ReportType reportType,
                                      final Map<String, Object> reportParameters,
                                      final Connection connection) throws ReportGenerationException {

        ReportTemplate template = reportTemplates.get(reportType);
        if (template == null) {
            throw new ReportGenerationException("Unknown report type '" + reportType + "'.");
        }

        //
        Map<String, Object> reportParams = new HashMap<String, Object>(reportParameters);
        reportParams.putAll(getParameters());
        processReportParameters(reportType, connection, reportParams);

        //
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        final JasperReport jasperReport;
        try {
            // Compile sub-reports
            for (ReportTemplate subReportTemplate : template.getSubReports()) {
                Document runtimeDocument;
                Map<String, Object> transformParameterMap = null;
                //only usage sub reports require transormation, all require compilation
                if (subReportTemplate.isTransformedRequired()) {
                    runtimeDocument = getRuntimeDocument(subReportTemplate, reportParams);
                    transformParameterMap = getTransformationParameters(reportType, subReportTemplate, runtimeDocument);
                }
                JasperReport subJasperReport = compileReportTemplate(transformerFactory, documentBuilderFactory, subReportTemplate, transformParameterMap);
                reportParams.put(subReportTemplate.getParameterMapName(), subJasperReport);
            }

            // Compile main report
            Document runtimeDocument = getRuntimeDocument(template, reportParams);
            Map<String, Object> transformParameterMap = getTransformationParameters(reportType, template, runtimeDocument);
            jasperReport = compileReportTemplate(transformerFactory, documentBuilderFactory, template, transformParameterMap);
        } catch (Exception e) {
            throw new ReportGenerationException("Unexpected error during report compilation: " + e.getMessage(), e);
        }

        return new ReportHandle(reportType, reportParams, jasperReport, null);
    }

    public ReportHandle fillReport(final ReportHandle handle,
                                   final Connection connection) throws ReportGenerationException {
        if (handle.getJasperReport() == null) throw new ReportGenerationException("ReportHandle not compiled.");

        Map<String, Object> reportParams = new HashMap<String, Object>(handle.getReportParameters());
        final JasperPrint jasperPrint;
        PreparedStatementDataSource psds = null;
        try {
            psds = new PreparedStatementDataSource(connection);
            reportParams.put(REPORT_DATA_SOURCE, psds);
            jasperPrint = JasperFillManager.fillReport(handle.getJasperReport(), reportParams, psds);
        } catch (JRException jre) {
            throw new ReportGenerationException("Error filling report.", jre);
        } finally {
            if (psds != null) psds.close();
        }

        return new ReportHandle(handle.getType(), reportParams, handle.getJasperReport(), jasperPrint);
    }

    public byte[] generateReportOutput(final ReportHandle handle,
                                       final String type) throws ReportGenerationException {
        if (handle.getJasperPrint() == null) throw new ReportGenerationException("ReportHandle not filled.");

        final byte[] report;
        if ("PDF".equals(type)) {
            try {
//                handle.getJasperPrint().setProperty(JRPdfExporterParameter.PROPERTY_PDF_VERSION.toString(), JRPdfExporterParameter.PDF_VERSION_1_6.toString());
                report = JasperExportManager.exportReportToPdf(handle.getJasperPrint());
            } catch (JRException jre) {
                throw new ReportGenerationException("Error creating report output.", jre);
            }
        } else if ("HTML".equals(type)) {
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream(20000);
                Map<String, byte[]> imagesMap = new HashMap<String, byte[]>();
                JRHtmlExporter exporter = new JRHtmlExporter();
                exporter.setParameter(JRHtmlExporterParameter.JASPER_PRINT, handle.getJasperPrint());
                exporter.setParameter(JRHtmlExporterParameter.IMAGES_MAP, imagesMap);
                exporter.setParameter(JRHtmlExporterParameter.IMAGES_URI, "images/");
                exporter.setParameter(JRHtmlExporterParameter.IS_OUTPUT_IMAGES_TO_DIR, Boolean.FALSE);
                exporter.setParameter(JRHtmlExporterParameter.OUTPUT_STREAM, output);
                exporter.exportReport();

                // Build ZIP file with report html and images.
                byte[] htmlData = output.toByteArray();
                ByteArrayOutputStream reportOut = new ByteArrayOutputStream(20000);
                ZipOutputStream zipOut = new ZipOutputStream(reportOut);
                zipOut.putNextEntry(new ZipEntry("report.html"));
                zipOut.write(htmlData);
                zipOut.closeEntry();

                for (Map.Entry<String, byte[]> entry : imagesMap.entrySet()) {
                    logger.info("Writing zip entry '" + entry.getKey() + "' " + entry.getValue().getClass().getName());
                    zipOut.putNextEntry(new ZipEntry("images/" + entry.getKey()));
                    zipOut.write(entry.getValue());
                    zipOut.closeEntry();
                }

                zipOut.close();

                report = reportOut.toByteArray();
            } catch (JRException jre) {
                throw new ReportGenerationException("Error creating report output.", jre);
            } catch (IOException ioe) {
                throw new ReportGenerationException("Error creating report output.", ioe);
            }
        } else {
            throw new ReportGenerationException("Unsupported report type '" + type + "'.");
        }

        return report;
    }

    public static final class ReportGenerationException extends Exception {
        public ReportGenerationException(String message) {
            super(message);
        }

        public ReportGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class ReportHandle {
        private final ReportApi.ReportType type;
        private final Map<String, Object> reportParameters;
        private final JasperReport jasperReport;
        private final JasperPrint jasperPrint;

        private ReportHandle(final ReportApi.ReportType type,
                             final Map<String, Object> reportParameters,
                             final JasperReport jasperReport,
                             final JasperPrint jasperPrint) {
            this.type = type;
            this.reportParameters = Collections.unmodifiableMap(reportParameters);
            this.jasperReport = jasperReport;
            this.jasperPrint = jasperPrint;
        }

        ReportApi.ReportType getType() {
            return type;
        }

        public Map<String, Object> getReportParameters() {
            return reportParameters;
        }

        JasperReport getJasperReport() {
            return jasperReport;
        }

        JasperPrint getJasperPrint() {
            return jasperPrint;
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ReportGenerator.class.getName());

    private static final Map<ReportApi.ReportType, ReportTemplate> reportTemplates;

    static {
        SyspropUtil.setProperty("jasper.reports.compiler.class", GatewayJavaReportCompiler.class.getName());
        GatewayJavaReportCompiler.registerClass(Utilities.class);
        GatewayJavaReportCompiler.registerClass(Utilities.UNIT_OF_TIME.class);
        GatewayJavaReportCompiler.registerClass(UsageSummaryAndSubReportHelper.class);
        GatewayJavaReportCompiler.registerClass(TimePeriodDataSource.class);
        GatewayJavaReportCompiler.registerClass(PerformanceSummaryChartCustomizer.class);
        GatewayJavaReportCompiler.registerClass(UsageSummaryAndSubReportHelper.class);
        GatewayJavaReportCompiler.registerClass(UsageReportHelper.class);
        GatewayJavaReportCompiler.registerClass(PreparedStatementDataSource.class);

        final Map<ReportApi.ReportType, ReportTemplate> templates = new HashMap<ReportApi.ReportType, ReportTemplate>();
        final String resourcePath = "/com/l7tech/gateway/standardreports";

        // Performance summary
        templates.put(ReportApi.ReportType.PERFORMANCE_SUMMARY, new ReportTemplate(ReportApi.ReportType.PERFORMANCE_SUMMARY, resourcePath + "/PS_Summary_Template.jrxml", resourcePath + "/PS_SummaryTransform.xsl", null));

        // Performance interval
        templates.put(ReportApi.ReportType.PERFORMANCE_INTERVAL, new ReportTemplate(ReportApi.ReportType.PERFORMANCE_INTERVAL, resourcePath + "/PS_IntervalMasterReport_Template.jrxml", resourcePath + "/PS_IntervalMasterTransform.xsl", Arrays.asList(
                new ReportTemplate(ReportApi.ReportParameters.SUB_INTERVAL_SUB_REPORT, ReportApi.ReportType.PERFORMANCE_INTERVAL, resourcePath + "/PS_SubIntervalMasterReport.jrxml", null, false),
                new ReportTemplate(ReportApi.ReportParameters.SUB_REPORT, ReportApi.ReportType.PERFORMANCE_INTERVAL, resourcePath + "/PS_SubIntervalMasterReport_subreport0.jrxml", null, false)
        )));

        // Usage summary
        templates.put(ReportApi.ReportType.USAGE_SUMMARY, new ReportTemplate(ReportApi.ReportType.USAGE_SUMMARY, resourcePath + "/Usage_Summary_Template.jrxml", resourcePath + "/UsageReportTransform.xsl", null));

        // Usage interval
        templates.put(ReportApi.ReportType.USAGE_INTERVAL, new ReportTemplate(ReportApi.ReportType.USAGE_INTERVAL, resourcePath + "/Usage_IntervalMasterReport_Template.jrxml", resourcePath + "/UsageReportIntervalTransform_Master.xsl", Arrays.asList(
                new ReportTemplate("SUB_INTERVAL_SUB_REPORT", ReportApi.ReportType.USAGE_INTERVAL, resourcePath + "/Usage_SubIntervalMasterReport_Template.jrxml", resourcePath + "/UsageReportSubIntervalTransform_Master.xsl", false),
                new ReportTemplate("SUB_REPORT", ReportApi.ReportType.USAGE_INTERVAL, resourcePath + "/Usage_SubIntervalMasterReport_subreport0_Template.jrxml", resourcePath + "/Usage_SubReport.xsl", false)
        )));

        reportTemplates = Collections.unmodifiableMap(templates);
    }

    private Map<String, Object> getParameters() throws ReportGenerationException {
        Map<String, Object> parameters = new HashMap<String, Object>();

        //Required
        parameters.put(ReportApi.ReportParameters.TEMPLATE_FILE_ABSOLUTE, "com/l7tech/gateway/standardreports/Styles.jrtx");
        parameters.put(ReportApi.ReportParameters.SUBREPORT_DIRECTORY, ".");

        Map sMap = null;
        InputStream styleIn = null;
        try {
            Map<String, Object> paramsForStyleGenerator = new HashMap<String, Object>();
            paramsForStyleGenerator.putAll(parameters);
            styleIn = ReportGenerator.class.getResourceAsStream("/StyleGenerator.jasper");

            if (null == styleIn) {  // EM-1040: Java 8 seems to have changed the compiled StyleGenerator file location
                styleIn = ReportGenerator.class.getResourceAsStream("/com/l7tech/gateway/standardreports/StyleGenerator.jasper");
            }

            //do not pass the Map 'parameters' into fillReport as this map is returned to the caller
            //and will have some parameters overwritten by fillReport, which is not desierable as we already
            //have user supplied values for them e.g. IS_IGNORE_PAGINATION and possibly time zone also
            JasperPrint jp = JasperFillManager.fillReport(styleIn, paramsForStyleGenerator);
            sMap = jp.getStylesMap();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error creating report style template.", e);
        } finally {
            ResourceUtils.closeQuietly(styleIn);
        }
        if (sMap == null) throw new ReportGenerationException("Error creating report style template.");
        parameters.put(ReportApi.ReportParameters.STYLES_FROM_TEMPLATE, sMap);

        return parameters;
    }

    /**
     * TODO parameter filtering / validation
     */
    @SuppressWarnings({"unchecked"})
    private void processReportParameters(final ReportApi.ReportType reportType, final Connection connection, final Map<String, Object> reportParams) throws ReportGenerationException {

        Boolean isRelative = (Boolean) reportParams.get(ReportApi.ReportParameters.IS_RELATIVE);

        String timeZone = (String) reportParams.get(ReportApi.ReportParameters.SPECIFIC_TIME_ZONE);
        Long startTimeInPast;
        Long endTimeInPast;

        final Utilities.UNIT_OF_TIME relUnitOfTime;
        if (isRelative) {
            int numRelativeTimeUnits = (Integer) reportParams.get(ReportApi.ReportParameters.RELATIVE_NUM_OF_TIME_UNITS);
            relUnitOfTime = Utilities.getUnitFromString(reportParams.get(ReportApi.ReportParameters.RELATIVE_TIME_UNIT).toString());
            reportParams.put(ReportApi.ReportParameters.RELATIVE_TIME_UNIT, relUnitOfTime);
            startTimeInPast = Utilities.getRelativeMilliSecondsInPast(numRelativeTimeUnits, relUnitOfTime, timeZone);
            endTimeInPast = Utilities.getMillisForEndTimePeriod(relUnitOfTime, timeZone);
        } else {
            relUnitOfTime = null;
            String absoluteStartTime = (String) reportParams.get(ReportApi.ReportParameters.ABSOLUTE_START_TIME);
            String absoluteEndTime = (String) reportParams.get(ReportApi.ReportParameters.ABSOLUTE_END_TIME);
            try {
                startTimeInPast = Utilities.getAbsoluteMilliSeconds(absoluteStartTime, timeZone);
                endTimeInPast = Utilities.getAbsoluteMilliSeconds(absoluteEndTime, timeZone);
            } catch (ParseException pe) {
                throw new ReportGenerationException("Could not parse absolute time. " + pe.getMessage());
            }
        }

        if (reportType == ReportApi.ReportType.PERFORMANCE_INTERVAL || reportType == ReportApi.ReportType.USAGE_INTERVAL) {
            Utilities.UNIT_OF_TIME intervalUnitOfTime = Utilities.getUnitFromString(reportParams.get(ReportApi.ReportParameters.INTERVAL_TIME_UNIT).toString());
            reportParams.put(ReportApi.ReportParameters.INTERVAL_TIME_UNIT, intervalUnitOfTime);
        }

        Map<String, Set<String>> serivceIdsToOp = (Map<String, Set<String>>) reportParams.get(ReportApi.ReportParameters.SERVICE_ID_TO_OPERATIONS_MAP);

        int resolution = Utilities.getSummaryResolutionFromTimePeriod(startTimeInPast, endTimeInPast, timeZone, isRelative, relUnitOfTime);

        boolean isContextMapping = Boolean.valueOf(reportParams.get(ReportApi.ReportParameters.IS_CONTEXT_MAPPING).toString());
        boolean isDetail = Boolean.valueOf(reportParams.get(ReportApi.ReportParameters.IS_DETAIL).toString());

        LinkedHashMap<String, List<ReportApi.FilterPair>>
                keysToFilterPairs = (LinkedHashMap<String, List<ReportApi.FilterPair>>)
                reportParams.get(ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS);

        boolean isUsage = (reportType == ReportApi.ReportType.USAGE_SUMMARY
                || reportType == ReportApi.ReportType.USAGE_INTERVAL);

        boolean isUsingKeys = !keysToFilterPairs.isEmpty();

        if (reportType == ReportApi.ReportType.PERFORMANCE_SUMMARY || reportType == ReportApi.ReportType.PERFORMANCE_INTERVAL) {
            //Only required for performance statistics reports. Usage reports must always have a key other than operation
            //added.
            reportParams.put(ReportApi.ReportParameters.IS_USING_KEYS, isUsingKeys);
        }

        Pair<String, List<Object>> sqlAndParamsPair;
        //this is a context mapping query using keys 1-5 and auth user
        if (isContextMapping && isUsingKeys) {
            sqlAndParamsPair = Utilities.getDistinctMappingQuery(
                    startTimeInPast, endTimeInPast, serivceIdsToOp, keysToFilterPairs, resolution, isDetail, isUsage);
        }//this is a context mapping query when we just need operation, dealt with like it's not context mapping
        else if (isContextMapping) {
            sqlAndParamsPair = Utilities.getPerformanceStatisticsMappingQuery(true, startTimeInPast, endTimeInPast,
                    serivceIdsToOp, keysToFilterPairs, resolution, isDetail, isUsage);
        }//this is a report on the original service_metrics table
        else {
            sqlAndParamsPair = Utilities.getNoMappingQuery(true, startTimeInPast, endTimeInPast, serivceIdsToOp.keySet(), resolution);
        }

        LinkedHashMap<String, String> displayStringToGroup = new LinkedHashMap<String, String>();

        if (isContextMapping && isUsingKeys) {
            LinkedHashSet<List<String>> distinctMappingSets = getDistinctMappingSets(connection, sqlAndParamsPair);
            reportParams.put(ReportApi.ReportParameters.DISTINCT_MAPPING_SETS, distinctMappingSets);
            Collection<String> mappingValuesLegend = RuntimeDocUtilities.getMappingLegendValues(keysToFilterPairs, distinctMappingSets);
            //We need to look up the mappingValues from both the group value and also the display string value

            int index = 1;
            for (String s : mappingValuesLegend) {
                String group = "Group " + index;
                displayStringToGroup.put(s, group);
                index++;
            }

            if (reportType == ReportApi.ReportType.USAGE_SUMMARY) {
                UsageSummaryAndSubReportHelper helper = new UsageSummaryAndSubReportHelper();
                LinkedHashMap<String, String> keyToColumnName = RuntimeDocUtilities.getKeyToColumnValues(distinctMappingSets);
                helper.setKeyToColumnMap(keyToColumnName);
                LinkedHashMap<Integer, String> groupIndexToGroup = Utilities.getGroupIndexToGroupString(mappingValuesLegend.size());
                helper.setIndexToGroupMap(groupIndexToGroup);

                reportParams.put(ReportApi.ReportParameters.REPORT_SCRIPTLET, helper);
            } else if (reportType == ReportApi.ReportType.USAGE_INTERVAL) {
                UsageSummaryAndSubReportHelper summaryAndSubReportHelper = new UsageSummaryAndSubReportHelper();
                LinkedHashMap<String, String> keyToColumnName = RuntimeDocUtilities.getKeyToColumnValues(distinctMappingSets);
                summaryAndSubReportHelper.setKeyToColumnMap(keyToColumnName);
                reportParams.put(ReportApi.ReportParameters.SUB_REPORT_HELPER, summaryAndSubReportHelper);

                UsageReportHelper reportHelper = new UsageReportHelper();
                LinkedHashMap<Integer, String> groupIndexToGroup = Utilities.getGroupIndexToGroupString(mappingValuesLegend.size());
                reportHelper.setKeyToColumnMap(keyToColumnName);
                reportHelper.setIndexToGroupMap(groupIndexToGroup);
                reportParams.put(ReportApi.ReportParameters.REPORT_SCRIPTLET, reportHelper);
            }

        } else {
            LinkedHashSet<Pair<String, String>> serviceValues = getServiceDisplayStrings(connection, sqlAndParamsPair);
            LinkedHashMap<String, Pair<String, String>> groupToDisplayString = new LinkedHashMap<String, Pair<String, String>>();
            //We need to look up the mappingValues from both the group value and also the display string value
            int index = 1;
            for (Pair<String, String> pair : serviceValues) {
                String shortServiceName = "Service " + index;
                String serviceTrunc = Utilities.getServiceStringTruncatedNoEscape(pair.getKey(), Utilities.SERVICE_DISPLAY_NAME_LENGTH);
                String routingTrunc = Utilities.getRoutingUriStringTruncatedNoEscape(pair.getValue(), Utilities.ROUTING_URI_LENGTH);
                groupToDisplayString.put(shortServiceName, new Pair<String, String>(serviceTrunc, routingTrunc));
                //displayStringToGroup must not have truncated values
                String displayName = Utilities.getServiceDisplayStringNotTruncatedNoEscape(pair.getKey(), pair.getValue());
                displayStringToGroup.put(displayName, shortServiceName);
                index++;
            }
            reportParams.put(ReportApi.ReportParameters.MAPPING_GROUP_TO_DISPLAY_STRING, groupToDisplayString);
        }

        if (reportType == ReportApi.ReportType.PERFORMANCE_SUMMARY || reportType == ReportApi.ReportType.PERFORMANCE_INTERVAL) {
            reportParams.put(ReportApi.ReportParameters.DISPLAY_STRING_TO_MAPPING_GROUP, displayStringToGroup);
        }

        //add required report scriptlets
        if (reportType == ReportApi.ReportType.PERFORMANCE_SUMMARY) {
            //Only required because jasper reports for some reason ignores the value of scriptletClass from the
            //jasperreport element attribute, so specifying it as a parameter explicitly fixes this issue
            reportParams.put(ReportApi.ReportParameters.REPORT_SCRIPTLET, new ScriptletHelper());
        }

    }

    @SuppressWarnings({"unchecked"})
    private Document getRuntimeDocument(final ReportTemplate template,
                                        final Map<String, Object> reportParameters) {
        final JasperDocument runtimeDocument;

        final LinkedHashSet<List<String>> distinctMappingSets =
                (LinkedHashSet<List<String>>) reportParameters.get(ReportApi.ReportParameters.DISTINCT_MAPPING_SETS);

        final LinkedHashMap<String, List<ReportApi.FilterPair>>
                keysToFilterPairs = (LinkedHashMap<String, List<ReportApi.FilterPair>>)
                reportParameters.get(ReportApi.ReportParameters.KEYS_TO_LIST_FILTER_PAIRS);

        if (template.getType() == ReportApi.ReportType.PERFORMANCE_SUMMARY ||
                template.getType() == ReportApi.ReportType.PERFORMANCE_INTERVAL) {

            final boolean isCtxMapping = Boolean.valueOf(reportParameters.get(ReportApi.ReportParameters.IS_CONTEXT_MAPPING).toString());
            final boolean isUsingKeys = Boolean.valueOf(reportParameters.get(ReportApi.ReportParameters.IS_USING_KEYS).toString());

            if (isCtxMapping && isUsingKeys) {
                runtimeDocument = RuntimeDocUtilities.getPerfStatAnyRuntimeDoc(keysToFilterPairs, distinctMappingSets);
            } else {
                runtimeDocument =
                        RuntimeDocUtilities.getPerfStatAnyRuntimeDoc((LinkedHashMap<String, Pair<String, String>>)
                                reportParameters.get(ReportApi.ReportParameters.MAPPING_GROUP_TO_DISPLAY_STRING));
            }
            return runtimeDocument.getDocument();
        }

        final ReportApi.ReportType templateType = template.getType();
        if (templateType != ReportApi.ReportType.USAGE_SUMMARY &&
                templateType != ReportApi.ReportType.USAGE_INTERVAL) {
            throw new IllegalArgumentException("Report type: " + templateType.toString() + " is not currently supported for runtime transformation");
        }

        if (template.getType() == ReportApi.ReportType.USAGE_SUMMARY) {
            runtimeDocument = RuntimeDocUtilities.getUsageRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        } else if (template.getType() == ReportApi.ReportType.USAGE_INTERVAL && template.isMasterReport()) {
            runtimeDocument = RuntimeDocUtilities.getUsageIntervalMasterRuntimeDoc(keysToFilterPairs, distinctMappingSets);
        } else if (template.getParameterMapName().equals(ReportApi.ReportParameters.SUB_INTERVAL_SUB_REPORT)) {
            runtimeDocument = RuntimeDocUtilities.getUsageSubIntervalMasterRuntimeDoc(distinctMappingSets);
        } else if (template.getParameterMapName().equals(ReportApi.ReportParameters.SUB_REPORT)) {
            runtimeDocument = RuntimeDocUtilities.getUsageSubReportRuntimeDoc(distinctMappingSets);
        } else {
            //programming error. Above else conditions check every possible report configuration currently.
            throw new IllegalStateException("Unknown report confirugration found.");
        }

        return runtimeDocument.getDocument();
    }

    /**
     *
     */
    private Map<String, Object> getTransformationParameters(ReportApi.ReportType reportType, final ReportTemplate reportTemplate, final Document document) {
        final Map<String, Object> params = new HashMap<String, Object>();

        if (document == null)
            throw new NullPointerException("Document cannot be null, as it is required for every transform");
        //all reports require the runtimedoc        
        params.put("RuntimeDoc", document);

        if (ReportApi.ReportType.PERFORMANCE_SUMMARY == reportType ||
                ReportApi.ReportType.PERFORMANCE_INTERVAL == reportType) {
            return params;
        }

        //at this point we are only left with usage reports

        if (ReportApi.ReportType.USAGE_SUMMARY == reportType ||
                (ReportApi.ReportType.USAGE_INTERVAL == reportType && reportTemplate.isMasterReport())) {
            params.put("FrameMinWidth", PAGE_WIDTH_NO_BORDERS);
            params.put("PageMinWidth", PAGE_WIDTH_INCLUDING_BORDERS);
            params.put("TitleInnerFrameBuffer", REPORT_INFO_FRAME_RIGHT_PADDING);
            return params;
        }

        //at this point we are only left with usage interval sub reports

        if (!reportTemplate.isMasterReport()) {//should always be
            String subReportParamName = reportTemplate.getParameterMapName();
            if (ReportApi.ReportType.USAGE_INTERVAL == reportType) {
                if (subReportParamName.equals(ReportApi.ReportParameters.SUB_INTERVAL_SUB_REPORT)) {
                    params.put("PageMinWidth", PAGE_WIDTH_NO_BORDERS);
                } else if (subReportParamName.equals(ReportApi.ReportParameters.SUB_REPORT)) {
                    params.put("PageMinWidth", SUB_REPORT_WIDTH);
                }
                return params;
            }
        }

        throw new IllegalStateException("Unknown combination of report type and report template");

    }

    /**
     *
     */
    private JasperReport compileReportTemplate(final TransformerFactory transformerFactory,
                                               final DocumentBuilderFactory documentBuilderFactory,
                                               final ReportTemplate template,
                                               final Map<String, Object> params) throws ReportGenerationException {
        final JasperReport report;
        try {

            InputStream inputStream;
            if (template.isTransformedRequired()) {
                Transformer transformer = transformerFactory.newTransformer(template.getReportXslSource());

                documentBuilderFactory.setNamespaceAware(true);
                DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
                builder.setEntityResolver(getEntityResolver());
                Document doc = builder.parse(template.getReportXmlSource());
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                StreamResult result = new StreamResult(baos);
                XmlUtil.softXSLTransform(doc, result, transformer, params);
                inputStream = new ByteArrayInputStream(baos.toByteArray());
            } else {
                inputStream = template.getReportXmlSource().getByteStream();
            }

            report = JasperCompileManager.compileReport(inputStream);

        } catch (TransformerException e) {
            logger.log(Level.WARNING, "Error generating report.", e);
            throw new ReportGenerationException(ExceptionUtils.getMessage(e));
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error generating report.", e);
            throw new ReportGenerationException(ExceptionUtils.getMessage(e));
        } catch (SAXException e) {
            logger.log(Level.WARNING, "Error generating report.", e);
            throw new ReportGenerationException(ExceptionUtils.getMessage(e));
        } catch (ParserConfigurationException e) {
            logger.log(Level.WARNING, "Error generating report.", e);
            throw new ReportGenerationException(ExceptionUtils.getMessage(e));
        } catch (JRException e) {
            logger.log(Level.WARNING, "Error generating report.", e);
            throw new ReportGenerationException(ExceptionUtils.getMessage(e));
        }

        return report;
    }

    private EntityResolver getEntityResolver() {
        Map<String, String> idsToResources = new HashMap<String, String>();
        idsToResources.put("http://jasperreports.sourceforge.net/dtds/jasperreport.dtd", "com/l7tech/gateway/standardreports/jasperreport.dtd");
        return new ResourceMapEntityResolver(null, idsToResources, null);
    }


    /**
     * Get the ordered set of distinct mapping sets for the keys and values in the sql string from the db
     */
    public static LinkedHashSet<List<String>> getDistinctMappingSets(Connection connection,
                                                                     Pair<String, List<Object>> sqlAndParamsPair)
            throws ReportGenerationException {
        LinkedHashSet<List<String>> returnSet = new LinkedHashSet<List<String>>();

        PreparedStatementDataSource psds = null;
        try {
            psds = new PreparedStatementDataSource(connection);
            psds.configure(sqlAndParamsPair.getKey(), sqlAndParamsPair.getValue());

            while (psds.next()) {
                List<String> mappingStrings = new ArrayList<String>();
                String authUser = (String) psds.getFieldValue(new JRFieldAdapter() {
                    @Override
                    public String getName() {
                        return PreparedStatementDataSource.ColumnName.AUTHENTICATED_USER.getColumnName();
                    }
                });

                mappingStrings.add(authUser);
                for (int i = 0; i < Utilities.NUM_MAPPING_KEYS; i++) {
                    final int index = i;
                    String aMapStr = (String) psds.getFieldValue(new JRFieldAdapter() {
                        @Override
                        public String getName() {
                            PreparedStatementDataSource.ColumnName columnName =
                                    PreparedStatementDataSource.ColumnName.getColumnName("MAPPING_VALUE_" + (index + 1));
                            return columnName.getColumnName();
                        }
                    });

                    mappingStrings.add(aMapStr);
                }
                returnSet.add(mappingStrings);
            }
        } catch (JRException ex) {
            throw new ReportGenerationException("Error generating mapping set.", ex);
        } catch (SQLException ex) {
            throw new ReportGenerationException("Error generating mapping set.", ex);
        } finally {
            if (psds != null) psds.close();
        }

        return returnSet;
    }

    private LinkedHashSet<Pair<String, String>> getServiceDisplayStrings(Connection connection,
                                                                         Pair<String, List<Object>> sqlAndParamsPair)
            throws ReportGenerationException {
        LinkedHashSet<Pair<String, String>> set = new LinkedHashSet<Pair<String, String>>();

        PreparedStatementDataSource psds = null;
        try {
            psds = new PreparedStatementDataSource(connection);
            psds.configure(sqlAndParamsPair.getKey(), sqlAndParamsPair.getValue());

            while (psds.next()) {
                String serviceName = (String) psds.getFieldValue(new JRFieldAdapter() {
                    @Override
                    public String getName() {
                        return PreparedStatementDataSource.ColumnName.SERVICE_NAME.getColumnName();
                    }
                });

                String routingUri = (String) psds.getFieldValue(new JRFieldAdapter() {
                    @Override
                    public String getName() {
                        return PreparedStatementDataSource.ColumnName.ROUTING_URI.getColumnName();
                    }
                });

                set.add(new Pair<String, String>(serviceName, routingUri));
            }
        } catch (SQLException ex) {
            throw new ReportGenerationException("Error generating service display values.", ex);
        } catch (JRException ex) {
            throw new ReportGenerationException("Error generating service display values.", ex);
        } finally {
            if (psds != null) psds.close();
        }

        return set;
    }

    private static final class ReportTemplate {
        private final String parameterMapName;
        private final ReportApi.ReportType type;
        private final String reportXml;
        private final String reportXsl;
        private final Collection<ReportTemplate> subReports;
        private final boolean requiresTransform;

        private final boolean isMasterReport;

        /**
         * Constructor for a Master report i.e. has subReports
         *
         * @param type
         * @param reportXml
         * @param reportXsl
         * @param subReports
         */
        ReportTemplate(final ReportApi.ReportType type,
                       final String reportXml,
                       final String reportXsl,
                       final Collection<ReportTemplate> subReports) {
            this.type = type;
            this.reportXml = reportXml;
            this.reportXsl = reportXsl;
            this.subReports = subReports == null ?
                    Collections.<ReportTemplate>emptyList() :
                    Collections.unmodifiableCollection(subReports);
            requiresTransform = (this.reportXsl != null);
            this.isMasterReport = true;
            this.parameterMapName = null;
        }

        /**
         * Constructor for a sub report or a report with no sub reports
         *
         * @param type
         * @param parameterMapName
         * @param reportXml
         * @param reportXsl
         */
        ReportTemplate(final String parameterMapName,
                       final ReportApi.ReportType type,
                       final String reportXml,
                       final String reportXsl,
                       final boolean isMasterReport) {
            this.parameterMapName = parameterMapName;
            this.type = type;
            this.reportXml = reportXml;
            this.reportXsl = reportXsl;
            this.subReports = Collections.emptyList();
            requiresTransform = (this.reportXsl != null);
            this.isMasterReport = isMasterReport;
        }

        public boolean isMasterReport() {
            return isMasterReport;
        }

        public boolean isTransformedRequired() {
            return requiresTransform;
        }

        public ReportApi.ReportType getType() {
            return type;
        }

        public String getParameterMapName() {
            if (isMasterReport) throw new IllegalStateException("Master reports do not have parameter names");

            return parameterMapName;
        }

        public String getReportXml() {
            return reportXml;
        }

        public String getReportXsl() {
            return reportXsl;
        }

        public Collection<ReportTemplate> getSubReports() {
            return subReports;
        }

        public InputSource getReportXmlSource() {
            InputSource inputSource = new InputSource(ReportGenerator.class.getResourceAsStream(getReportXml()));
            inputSource.setSystemId(getReportXml());
            return inputSource;
        }

        public StreamSource getReportXslSource() {
            StreamSource xsltsource = new StreamSource(ReportGenerator.class.getResourceAsStream(getReportXsl()));
            xsltsource.setSystemId(getReportXsl());
            return xsltsource;
        }
    }

}
