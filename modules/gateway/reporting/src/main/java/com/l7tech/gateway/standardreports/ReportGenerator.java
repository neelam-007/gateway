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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.*;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
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
import com.l7tech.common.io.ResourceMapEntityResolver;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.IOUtils;
import com.l7tech.server.management.api.node.ReportApi;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperExportManager;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.EntityResolver;

/**
 * Generates a report.
 */
public class ReportGenerator {

    //- PUBLIC

    public ReportHandle compileReport( final ReportApi.ReportType reportType,
                                       final Map<String,Object> reportParameters,
                                       final Connection connection ) throws ReportGenerationException {

        ReportTemplate template = reportTemplates.get(reportType);
        if ( template == null ) {
            throw new ReportGenerationException( "Unknown report type '"+reportType+"'." );            
        }

        //
        Map<String, Object> reportParams = new HashMap<String,Object>( reportParameters );
        reportParams.putAll( getParameters(reportType) );
        processReportParameters( reportType, connection, reportParams );

        //
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        JasperReport jasperReport;
        try {
            // Compile sub-reports
            for ( ReportTemplate subReportTemplate : template.getSubReports() ) {
                Document runtimeDocument = null;
                Map<String, Object> transformParameterMap = null;
                //only usage sub reports require transormation, all require compilation
                if(subReportTemplate.isTransformedRequired()){
                    runtimeDocument = getRuntimeDocument( subReportTemplate, subReportTemplate.getParameterMapName(), reportParams );
                    transformParameterMap = getTransformationParameters( reportType, subReportTemplate.getParameterMapName(), runtimeDocument );
                }
                JasperReport subJasperReport = compileReportTemplate( transformerFactory, documentBuilderFactory, subReportTemplate, transformParameterMap );
                reportParams.put( subReportTemplate.getParameterMapName(), subJasperReport );
            }
            
            // Compile main report
            Document runtimeDocument = getRuntimeDocument( template, null, reportParams );
            Map<String, Object> transformParameterMap = getTransformationParameters( reportType, template.getParameterMapName(), runtimeDocument );
            jasperReport = compileReportTemplate( transformerFactory, documentBuilderFactory, template, transformParameterMap );
        } catch ( Exception e ) {
            throw new ReportGenerationException( "Unexpected error during report compilation: " + e.getMessage(), e );                        
        }

        return new ReportHandle( reportType, reportParams, jasperReport, null );
    }

    public ReportHandle fillReport( final ReportHandle handle,
                                    final Connection connection ) throws ReportGenerationException {
        if ( handle.getJasperReport()==null ) throw new ReportGenerationException( "ReportHandle not compiled." );

        Map<String, Object> reportParams = new HashMap<String,Object>( handle.getReportParameters() );
        JasperPrint jasperPrint;
        try {
            jasperPrint = JasperFillManager.fillReport( handle.getJasperReport(), reportParams, connection);
        } catch ( JRException jre ) {
            throw new ReportGenerationException( "Error filling report.", jre );
        }

        return new ReportHandle( handle.getType(), reportParams, handle.getJasperReport(), jasperPrint );
    }

    public byte[] generateReportOutput( final ReportHandle handle,
                                        final String type ) throws ReportGenerationException {
        if ( handle.getJasperPrint()==null ) throw new ReportGenerationException( "ReportHandle not filled." );
        if ( !"PDF".equals( type ) ) throw new ReportGenerationException( "Unsupported report type '"+type+"'." );

        byte[] report;
        try {
            report = JasperExportManager.exportReportToPdf( handle.getJasperPrint() );
        } catch ( JRException jre ) {
            throw new ReportGenerationException( "Error creating report output.", jre );        
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
        private final Map<String,Object> reportParameters;
        private final JasperReport jasperReport;
        private final JasperPrint jasperPrint;

        private ReportHandle( final ReportApi.ReportType type,
                              final Map<String, Object> reportParameters,
                              final JasperReport jasperReport,
                              final JasperPrint jasperPrint ) {
            this.type = type;
            this.reportParameters = Collections.unmodifiableMap(reportParameters);
            this.jasperReport = jasperReport;
            this.jasperPrint = jasperPrint;
        }

        ReportApi.ReportType getType(){
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

    private static final Logger logger = Logger.getLogger( ReportGenerator.class.getName() );

    private static final Map<ReportApi.ReportType ,ReportTemplate> reportTemplates;

    static {
        System.setProperty( "jasper.reports.compiler.class", GatewayJavaReportCompiler.class.getName() );
        GatewayJavaReportCompiler.registerClass(Utilities.class);
        GatewayJavaReportCompiler.registerClass(Utilities.UNIT_OF_TIME.class);
        GatewayJavaReportCompiler.registerClass(UsageSummaryAndSubReportHelper.class);
        GatewayJavaReportCompiler.registerClass(TimePeriodDataSource.class);
        GatewayJavaReportCompiler.registerClass(PerformanceSummaryChartCustomizer.class);
        GatewayJavaReportCompiler.registerClass(UsageSummaryAndSubReportHelper.class);
        GatewayJavaReportCompiler.registerClass(UsageReportHelper.class);

        final Map<ReportApi.ReportType,ReportTemplate> templates = new HashMap<ReportApi.ReportType,ReportTemplate>();
        final String resourcePath = "/com/l7tech/gateway/standardreports";

        // Performance summary
        templates.put( ReportApi.ReportType.PERFORMANCE_SUMMARY, new ReportTemplate(ReportApi.ReportType.PERFORMANCE_SUMMARY, "not used", resourcePath+"/PS_Summary_Template.jrxml", resourcePath+"/PS_SummaryTransform.xsl", null ) );

        // Performance interval
        templates.put( ReportApi.ReportType.PERFORMANCE_INTERVAL, new ReportTemplate( ReportApi.ReportType.PERFORMANCE_INTERVAL,"not used", resourcePath+"/PS_IntervalMasterReport_Template.jrxml", resourcePath+"/PS_IntervalMasterTransform.xsl", Arrays.asList(
            new ReportTemplate( ReportApi.ReportType.PERFORMANCE_INTERVAL, ReportApi.ReportParameters.SUB_INTERVAL_SUB_REPORT, resourcePath+"/PS_SubIntervalMasterReport.jrxml", null, null ),
            new ReportTemplate( ReportApi.ReportType.PERFORMANCE_INTERVAL, ReportApi.ReportParameters.SUB_REPORT, resourcePath+"/PS_SubIntervalMasterReport_subreport0.jrxml", null, null )
        )) );

        // Usage summary
        templates.put( ReportApi.ReportType.USAGE_SUMMARY, new ReportTemplate(ReportApi.ReportType.USAGE_SUMMARY, "not used", resourcePath+"/Usage_Summary_Template.jrxml", resourcePath+"/UsageReportTransform.xsl", null ) );

        // Usage interval
        templates.put( ReportApi.ReportType.USAGE_INTERVAL, new ReportTemplate( ReportApi.ReportType.USAGE_INTERVAL, "not used", resourcePath+"/Usage_IntervalMasterReport_Template.jrxml", resourcePath+"/UsageReportIntervalTransform_Master.xsl", Arrays.asList(
                new ReportTemplate( ReportApi.ReportType.USAGE_INTERVAL, "SUB_INTERVAL_SUB_REPORT", resourcePath+"/Usage_SubIntervalMasterReport_Template.jrxml", resourcePath+"/UsageReportSubIntervalTransform_Master.xsl", null ),
                new ReportTemplate( ReportApi.ReportType.USAGE_INTERVAL, "SUB_REPORT", resourcePath+"/Usage_SubIntervalMasterReport_subreport0_Template.jrxml", resourcePath+"/Usage_SubReport.xsl", null )
        )) );

        reportTemplates = Collections.unmodifiableMap( templates );
    }

    private Map<String, Object> getParameters(ReportApi.ReportType reportType ) throws ReportGenerationException {
        Map<String, Object> parameters = new HashMap<String, Object>();

        //Required
        parameters.put(ReportApi.ReportParameters.TEMPLATE_FILE_ABSOLUTE, "com/l7tech/gateway/standardreports/Styles.jrtx");
        parameters.put(ReportApi.ReportParameters.SUBREPORT_DIRECTORY, ".");
        parameters.put(ReportApi.ReportParameters.HOURLY_MAX_RETENTION_NUM_DAYS, 32); // TODO serverconfig property?

        Map sMap = null;
        InputStream styleIn = null;
        try {
            styleIn = ReportGenerator.class.getResourceAsStream( "/StyleGenerator.jasper" );
            JasperPrint jp = JasperFillManager.fillReport(styleIn, parameters);
            sMap = jp.getStylesMap();
        } catch ( Exception e ) {
            logger.log( Level.WARNING, "Error creating report style template.", e );
        } finally {
            ResourceUtils.closeQuietly( styleIn );
        }
        if(sMap == null) throw new ReportGenerationException("Error creating report style template.");
        parameters.put(ReportApi.ReportParameters.STYLES_FROM_TEMPLATE, sMap);

        return parameters;
    }

    /**
     * TODO parameter filtering / validation
     */
    @SuppressWarnings({"unchecked"})
    private void processReportParameters( final ReportApi.ReportType reportType, final Connection connection, final Map<String, Object> reportParams ) throws ReportGenerationException {

        Boolean isRelative = (Boolean) reportParams.get(ReportApi.ReportParameters.IS_RELATIVE);

        String timeZone = (String) reportParams.get(ReportApi.ReportParameters.SPECIFIC_TIME_ZONE);
        Long startTimeInPast = null;
        Long endTimeInPast = null;

        if(isRelative){
            int numRelativeTimeUnits = (Integer)reportParams.get(ReportApi.ReportParameters.RELATIVE_NUM_OF_TIME_UNITS);
            Utilities.UNIT_OF_TIME relUnitOfTime = Utilities.getUnitFromString(reportParams.get(ReportApi.ReportParameters.RELATIVE_TIME_UNIT).toString());
            reportParams.put( ReportApi.ReportParameters.RELATIVE_TIME_UNIT, relUnitOfTime );
            startTimeInPast = Utilities.getRelativeMilliSecondsInPast(numRelativeTimeUnits, relUnitOfTime, timeZone );
            endTimeInPast = Utilities.getMillisForEndTimePeriod(relUnitOfTime, timeZone);
        }else{
            String absoluteStartTime = (String) reportParams.get(ReportApi.ReportParameters.ABSOLUTE_START_TIME);
            String absoluteEndTime = (String) reportParams.get(ReportApi.ReportParameters.ABSOLUTE_END_TIME);
            try{
                startTimeInPast = Utilities.getAbsoluteMilliSeconds(absoluteStartTime, timeZone);
                endTimeInPast = Utilities.getAbsoluteMilliSeconds(absoluteEndTime, timeZone);
            }catch(ParseException pe){
                throw new ReportGenerationException("Could not parse absolute time. " + pe.getMessage());
            }
        }

        if(reportType == ReportApi.ReportType.PERFORMANCE_INTERVAL || reportType == ReportApi.ReportType.USAGE_INTERVAL){
            Utilities.UNIT_OF_TIME intervalUnitOfTime = Utilities.getUnitFromString(reportParams.get(ReportApi.ReportParameters.INTERVAL_TIME_UNIT).toString());
            reportParams.put(ReportApi.ReportParameters.INTERVAL_TIME_UNIT, intervalUnitOfTime);
        }

        List<String> keys = (List<String>) reportParams.get(ReportApi.ReportParameters.MAPPING_KEYS);
        List<String> values = (List<String>) reportParams.get(ReportApi.ReportParameters.MAPPING_VALUES);
        List<String> useAnd = (List<String>) reportParams.get(ReportApi.ReportParameters.VALUE_EQUAL_OR_LIKE);
        Map<String, Set<String>> serivceIdsToOp = (Map<String, Set<String>>) reportParams.get(ReportApi.ReportParameters.SERVICE_ID_TO_OPERATIONS_MAP);

        boolean useUser = Boolean.valueOf(reportParams.get(ReportApi.ReportParameters.USE_USER).toString());
        List<String> authUsers = (List<String>) reportParams.get(ReportApi.ReportParameters.AUTHENTICATED_USERS);
        int resolution = Utilities.getSummaryResolutionFromTimePeriod(30, startTimeInPast, endTimeInPast);

        boolean isContextMapping = Boolean.valueOf(reportParams.get(ReportApi.ReportParameters.IS_CONTEXT_MAPPING).toString());
        boolean isDetail = Boolean.valueOf(reportParams.get(ReportApi.ReportParameters.IS_DETAIL).toString());

        String sql;
        if( isContextMapping ) {
            sql = Utilities.getUsageDistinctMappingQuery(startTimeInPast, endTimeInPast, serivceIdsToOp, keys, values, useAnd, resolution, isDetail, useUser, authUsers);
        }else{
            sql = Utilities.getNoMappingQuery(true, startTimeInPast, endTimeInPast, serivceIdsToOp.keySet(), resolution);
        }

        LinkedHashMap<String, String> groupToDisplayString = new LinkedHashMap<String, String>();
        LinkedHashMap<String, String> displayStringToGroup = new LinkedHashMap<String, String>();

        if( isContextMapping ){
            LinkedHashSet<List<String>> distinctMappingSets = getDistinctMappingSets(connection, sql);
            reportParams.put(ReportApi.ReportParameters.DISTINCT_MAPPING_SETS, distinctMappingSets);
            LinkedHashSet<String> mappingValuesLegend = Utilities.getMappingLegendValues(keys, distinctMappingSets);
            //We need to look up the mappingValues from both the group value and also the display string value

            int index = 1;
            for(String s: mappingValuesLegend){
                String group = "Group "+index;
                //System.out.println("Group: " + group+" s: " + s);
                groupToDisplayString.put(group, s);
                displayStringToGroup.put(s, group);
                index++;
            }

            if(reportType == ReportApi.ReportType.USAGE_SUMMARY){
                UsageSummaryAndSubReportHelper helper = new UsageSummaryAndSubReportHelper();
                LinkedHashMap<String, String> keyToColumnName = Utilities.getKeyToColumnValues(distinctMappingSets);
                helper.setKeyToColumnMap(keyToColumnName);
                reportParams.put(ReportApi.ReportParameters.REPORT_SCRIPTLET, helper);
            }else if(reportType == ReportApi.ReportType.USAGE_INTERVAL){
                UsageSummaryAndSubReportHelper summaryAndSubReportHelper = new UsageSummaryAndSubReportHelper();
                LinkedHashMap<String, String> keyToColumnName = Utilities.getKeyToColumnValues(distinctMappingSets);
                summaryAndSubReportHelper.setKeyToColumnMap(keyToColumnName);
                reportParams.put(ReportApi.ReportParameters.SUB_REPORT_HELPER, summaryAndSubReportHelper);

                UsageReportHelper reportHelper = new UsageReportHelper();
                LinkedHashMap<Integer, String> groupIndexToGroup = Utilities.getGroupIndexToGroupString(mappingValuesLegend);
                reportHelper.setKeyToColumnMap(keyToColumnName);
                reportHelper.setIndexToGroupMap(groupIndexToGroup);
                reportParams.put(ReportApi.ReportParameters.REPORT_SCRIPTLET, reportHelper);

            }

        } else {
            LinkedHashSet<String> serviceValues = getServiceDisplayStrings(connection, sql);
            //We need to look up the mappingValues from both the group value and also the display string value
            int index = 1;
            for(String s: serviceValues){
                String service = "Service "+index;
                //System.out.println("Service: " + service+" s: " + s);
                groupToDisplayString.put(service, s);
                displayStringToGroup.put(s, service);
                index++;
            }
        }

        reportParams.put(ReportApi.ReportParameters.DISPLAY_STRING_TO_MAPPING_GROUP, displayStringToGroup);
        reportParams.put(ReportApi.ReportParameters.MAPPING_GROUP_TO_DISPLAY_STRING, groupToDisplayString);

        //add required report scriptlets
        if(reportType == ReportApi.ReportType.PERFORMANCE_SUMMARY){
            //Only required because jasper reports for some reason ignores the value of scriptletClass from the
            //jasperreport element attribute, so specifying it as a parameter explicitly fixes this issue
            reportParams.put(ReportApi.ReportParameters.REPORT_SCRIPTLET, new ScriptletHelper());
        }

    }

    /**
     * For interval ReportTypes, the master report is identified by the template.getType() and subReportParamName
     * being null 
     */
    @SuppressWarnings({"unchecked"})
    private Document getRuntimeDocument( final ReportTemplate template, final String subReportParamName,
                                         final Map<String,Object> reportParameters  ) {
        Document runtimeDocument = null;

        if(template.getType() == ReportApi.ReportType.PERFORMANCE_SUMMARY ||
                template.getType() == ReportApi.ReportType.PERFORMANCE_INTERVAL){
            runtimeDocument = Utilities.getPerfStatAnyRuntimeDoc(
                    Boolean.valueOf(reportParameters.get(ReportApi.ReportParameters.IS_CONTEXT_MAPPING).toString()),
                    (LinkedHashMap<String,String>)reportParameters.get(ReportApi.ReportParameters.MAPPING_GROUP_TO_DISPLAY_STRING) );
            return runtimeDocument;
        }

        ReportApi.ReportType templateType = template.getType();
        if(templateType != ReportApi.ReportType.USAGE_SUMMARY &&
                templateType != ReportApi.ReportType.USAGE_INTERVAL){
            throw new IllegalArgumentException("Report type: " + templateType.toString()+" is not currently supported for runtime transformation");
        }

        LinkedHashSet<List<String>> distinctMappingSets =
                (LinkedHashSet<List<String>>) reportParameters.get(ReportApi.ReportParameters.DISTINCT_MAPPING_SETS);
        Boolean useUser = (Boolean) reportParameters.get(ReportApi.ReportParameters.USE_USER);
        List<String> keys = (List<String>) reportParameters.get(ReportApi.ReportParameters.MAPPING_KEYS);
        
        if(template.getType() == ReportApi.ReportType.USAGE_SUMMARY){
            runtimeDocument = Utilities.getUsageRuntimeDoc(useUser, keys, distinctMappingSets);
        }else if(template.getType() == ReportApi.ReportType.USAGE_INTERVAL && subReportParamName == null){
            runtimeDocument = Utilities.getUsageIntervalMasterRuntimeDoc(useUser, keys, distinctMappingSets);
        }else if(subReportParamName.equals(ReportApi.ReportParameters.SUB_INTERVAL_SUB_REPORT)){
            runtimeDocument = Utilities.getUsageSubIntervalMasterRuntimeDoc(useUser, keys, distinctMappingSets);
        }else if(subReportParamName.equals(ReportApi.ReportParameters.SUB_REPORT)){
            runtimeDocument = Utilities.getUsageSubReportRuntimeDoc(useUser, keys, distinctMappingSets);
        }

        return runtimeDocument;
    }

    /**
     *
     */
    private Map<String, Object> getTransformationParameters( ReportApi.ReportType reportType, String subReportParamName, final Document document ) {
        final Map<String, Object> params = new HashMap<String, Object>();

        if(document == null) throw new NullPointerException("Document cannot be null, as it is required for every transform");
        //all reports require the runtimedoc        
        params.put("RuntimeDoc", document);

        if(ReportApi.ReportType.PERFORMANCE_SUMMARY == reportType ||
                ReportApi.ReportType.PERFORMANCE_INTERVAL == reportType){
            return params;
        }

        if(ReportApi.ReportType.USAGE_SUMMARY == reportType ||
                (ReportApi.ReportType.USAGE_INTERVAL == reportType && subReportParamName == null)){
            params.put("FrameMinWidth", 820);
            params.put("PageMinWidth", 850);
            params.put("ReportInfoStaticTextSize", 128);
            params.put("TitleInnerFrameBuffer", 7);
        }else if(ReportApi.ReportType.USAGE_INTERVAL == reportType
                && (subReportParamName.equals(ReportApi.ReportParameters.SUB_INTERVAL_SUB_REPORT)
                || subReportParamName.equals(ReportApi.ReportParameters.SUB_REPORT))){
            params.put("PageMinWidth", 820);
        }

        return params;
    }

    /**
     *
     */
    private JasperReport compileReportTemplate( final TransformerFactory transformerFactory,
                                                final DocumentBuilderFactory documentBuilderFactory,
                                                final ReportTemplate template,
                                                final Map<String,Object> params ) throws ReportGenerationException {
        final JasperReport report;
        try {

            InputStream inputStream = null;
            if(template.isTransformedRequired()){
                Transformer transformer = transformerFactory.newTransformer( template.getReportXslSource() );

                documentBuilderFactory.setNamespaceAware( true );
                DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
                builder.setEntityResolver( getEntityResolver() );
                Document doc = builder.parse( template.getReportXmlSource() );
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                StreamResult result = new StreamResult(baos);
                XmlUtil.softXSLTransform(doc, result, transformer, params);
                inputStream = new ByteArrayInputStream(baos.toByteArray());
                IOUtils.spewStream(baos.toByteArray(), new FileOutputStream(new File("Output_" + template.getParameterMapName()+".xml")));
            }else{
                inputStream = template.getReportXmlSource().getByteStream();
            }

            report = JasperCompileManager.compileReport(inputStream);
            
        } catch (TransformerException e) {
           logger.log( Level.WARNING, "Error generating report.", e);
            throw new ReportGenerationException( ExceptionUtils.getMessage(e) );
        } catch (IOException e) {
            logger.log( Level.WARNING, "Error generating report.", e);
             throw new ReportGenerationException( ExceptionUtils.getMessage(e) );
        } catch (SAXException e) {
            logger.log( Level.WARNING, "Error generating report.", e);
             throw new ReportGenerationException( ExceptionUtils.getMessage(e) );
        } catch (ParserConfigurationException e) {
            logger.log( Level.WARNING, "Error generating report.", e);
             throw new ReportGenerationException( ExceptionUtils.getMessage(e) );
        } catch (JRException e) {
            logger.log( Level.WARNING, "Error generating report.", e);
             throw new ReportGenerationException( ExceptionUtils.getMessage(e) );
        }

        return report;
    }

    private EntityResolver getEntityResolver() {
        Map<String,String> idsToResources = new HashMap<String,String>();
        idsToResources.put( "http://jasperreports.sourceforge.net/dtds/jasperreport.dtd", "com/l7tech/gateway/standardreports/jasperreport.dtd");
        return new ResourceMapEntityResolver( null, idsToResources, null );
    }

    public static LinkedHashMap<String, String> getKeyToColumnValues(LinkedHashSet<List<String>> distinctMappingSets) {
        LinkedHashSet<String> mappingValues = getMappingValues(distinctMappingSets);
        LinkedHashMap<String, String> keyToColumnName = new LinkedHashMap<String, String>();
        int count = 1;
        //System.out.println("Key to column map");
        for (String s : mappingValues) {
            keyToColumnName.put(s, "COLUMN_"+count);
            //System.out.println(s+" " + "COLUMN_"+count);
            count++;
        }
        return keyToColumnName;
    }
    
    private static LinkedHashSet<String> getMappingValues(LinkedHashSet<List<String>> distinctMappingSets){
        LinkedHashSet<String> mappingValues = new LinkedHashSet<String>();

        for(List<String> set: distinctMappingSets){
            List<String> mappingStrings = new ArrayList<String>();
            boolean first = true;
            String authUser = null;
            for(String s: set){
                if(first){
                    authUser = s;
                    first = false;
                    continue;
                }
                mappingStrings.add(s);
            }
            String mappingValue = Utilities.getMappingValueString(authUser, mappingStrings.toArray(new String[]{}));
            mappingValues.add(mappingValue);
        }

        return mappingValues;
    }

    /**
     * Get the ordered set of distinct mapping sets for the keys and values in the sql string from the db
     */
    public static LinkedHashSet<List<String>> getDistinctMappingSets(Connection connection, String sql) throws ReportGenerationException {
        LinkedHashSet<List<String>> returnSet = new LinkedHashSet<List<String>>();

        Statement stmt = null;
        ResultSet rs = null;
        try{
            stmt = connection.createStatement();
            rs = stmt.executeQuery(sql);

            while(rs.next()){
                List<String> mappingStrings = new ArrayList<String>();
                String authUser = rs.getString(Utilities.AUTHENTICATED_USER);
                mappingStrings.add(authUser);
                for(int i = 0; i < Utilities.NUM_MAPPING_KEYS; i++){
                    mappingStrings.add(rs.getString("MAPPING_VALUE_"+(i+1)));
                }
                returnSet.add(mappingStrings);
            }
        } catch( SQLException ex ) {
            throw new ReportGenerationException("Error generating mapping set.", ex);
        } finally {
            ResourceUtils.closeQuietly( rs );
            ResourceUtils.closeQuietly( stmt );
        }

        return returnSet;
    }

    private LinkedHashSet<String> getServiceDisplayStrings(Connection connection, String sql) throws ReportGenerationException {
        LinkedHashSet<String> set = new LinkedHashSet<String>();

        Statement stmt = null;
        ResultSet rs = null;
        try{
            stmt = connection.createStatement();
            rs = stmt.executeQuery(sql);

            while( rs.next() ){
                String service = rs.getString(Utilities.SERVICE_NAME) + "[" + rs.getString(Utilities.ROUTING_URI) +"]";
                set.add(service);
            }
        } catch( SQLException ex ){
            throw new ReportGenerationException("Error generating service display values.", ex);
        } finally {
            ResourceUtils.closeQuietly( rs );
            ResourceUtils.closeQuietly( stmt );
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

        //todo [Donal] Master templates don't need a parameter map name, we care about their type, sub reports need a
        //todo [Donal] parameter map name, and we don't care about their type need to split out
        /**
         *
         * @param type
         * @param parameterMapName this is an important param which has two purposes: 1) it is the parameter name required
         * in a master report for identifiying a sub report 2) it is used in ReportGenerator for making run time decisions
         * when getting transformation parameters
         * @param reportXml
         * @param reportXsl
         * @param subReports
         */
        ReportTemplate( final ReportApi.ReportType type,
                        final String parameterMapName,
                        final String reportXml,
                        final String reportXsl,
                        final Collection<ReportTemplate> subReports ) {
            this.type = type;
            this.parameterMapName = parameterMapName;
            this.reportXml = reportXml;
            this.reportXsl = reportXsl;
            this.subReports = subReports == null ?
                    Collections.<ReportTemplate>emptyList() :
                    Collections.unmodifiableCollection(subReports);
            requiresTransform = (this.reportXsl != null);
        }

        public boolean isTransformedRequired(){
            return requiresTransform;
        }

        public ReportApi.ReportType getType() {
            return type;
        }

        public String getParameterMapName() {
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
            InputSource inputSource = new InputSource( ReportGenerator.class.getResourceAsStream( getReportXml() ) );
            inputSource.setSystemId( getReportXml() );
            return inputSource;
        }

        public StreamSource getReportXslSource() {
            StreamSource xsltsource = new StreamSource( ReportGenerator.class.getResourceAsStream( getReportXsl() ) );
            xsltsource.setSystemId( getReportXsl() );
            return xsltsource;
        }
    }
    
}
