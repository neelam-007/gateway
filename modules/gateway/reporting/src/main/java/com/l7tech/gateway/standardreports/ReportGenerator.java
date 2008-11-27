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
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    public ReportHandle compileReport( final String reportType,
                                       final Map<String,Object> reportParameters,
                                       final Connection connection ) throws ReportGenerationException {
        ReportTemplate template = reportTemplates.get(reportType);
        if ( template == null ) {
            throw new ReportGenerationException( "Unknown report type '"+reportType+"'." );            
        }

        //
        Map<String, Object> reportParams = new HashMap<String,Object>( reportParameters );
        reportParams.putAll( getParameters() );
        processReportParameters( connection, reportParams );

        //
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();

        JasperReport jasperReport;
        try {
            // Compile sub-reports
            for ( ReportTemplate subReportTemplate : template.getSubReports() ) {
                Document runtimeDocument = getRuntimeDocument( template, subReportTemplate, reportParams );
                Map<String, Object> transformParameterMap = getTransformationParameters( runtimeDocument );

                JasperReport subJasperReport = compileReportTemplate( transformerFactory, documentBuilderFactory, subReportTemplate, transformParameterMap );
                reportParams.put( template.getName(), subJasperReport );
            }

            // Compile main report
            Document runtimeDocument = getRuntimeDocument( template, null, reportParams );
            Map<String, Object> transformParameterMap = getTransformationParameters( runtimeDocument );
            jasperReport = compileReportTemplate( transformerFactory, documentBuilderFactory, template, transformParameterMap );
        } catch ( Exception e ) {
            throw new ReportGenerationException( "Unexpected error during report compilation.", e );                        
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
        private final String type;
        private final Map<String,Object> reportParameters;
        private final JasperReport jasperReport;
        private final JasperPrint jasperPrint;

        private ReportHandle( final String type,
                              final Map<String, Object> reportParameters,
                              final JasperReport jasperReport,
                              final JasperPrint jasperPrint ) {
            this.type = type;
            this.reportParameters = Collections.unmodifiableMap(reportParameters);
            this.jasperReport = jasperReport;
            this.jasperPrint = jasperPrint;
        }

        String getType(){
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

    private static final String TEMPLATE_FILE_ABSOLUTE = "TEMPLATE_FILE_ABSOLUTE";
    private static final String SUBREPORT_DIRECTORY = "SUBREPORT_DIRECTORY";

    private static final String HOURLY_MAX_RETENTION_NUM_DAYS = "HOURLY_MAX_RETENTION_NUM_DAYS";
    private static final String STYLES_FROM_TEMPLATE = "STYLES_FROM_TEMPLATE";
    private static final String REPORT_SCRIPTLET = "REPORT_SCRIPTLET";
    private static final String DISPLAY_STRING_TO_MAPPING_GROUP = "DISPLAY_STRING_TO_MAPPING_GROUP";
    private static final String MAPPING_GROUP_TO_DISPLAY_STRING = "MAPPING_GROUP_TO_DISPLAY_STRING";

    private static final Map<String,ReportTemplate> reportTemplates;

    static {
        System.setProperty( "jasper.reports.compiler.class", GatewayJavaReportCompiler.class.getName() );
        GatewayJavaReportCompiler.registerClass(Utilities.class);
        GatewayJavaReportCompiler.registerClass(Utilities.UNIT_OF_TIME.class);
        GatewayJavaReportCompiler.registerClass(UsageSummaryAndSubReportHelper.class);

        final Map<String,ReportTemplate> templates = new HashMap<String,ReportTemplate>();
        final String resourcePath = "/com/l7tech/gateway/standardreports";

        // Performance summary
        templates.put( "PERFORMANCE_SUMMARY", new ReportTemplate( "PERFORMANCE_SUMMARY", resourcePath+"/PS_Summary_Template.jrxml", resourcePath+"/PS_SummaryTransform.xsl", null ) );

        // Performance interval
        templates.put( "PERFORMANCE_INTERVAL", new ReportTemplate( "PERFORMANCE_INTERVAL", resourcePath+"/PS_IntervalMasterReport_Template.jrxml", resourcePath+"/PS_IntervalMasterTransform.xsl", Arrays.asList(
            new ReportTemplate( "", "", "", null )
        )) );

        // Usage summary
        templates.put( "USAGE_SUMMARY", new ReportTemplate( "USAGE_SUMMARY", resourcePath+"/Usage_Summary_Template.jrxml", resourcePath+"/UsageReportTransform.xsl", null ) );

        // Usage interval
        templates.put( "USAGE_INTERVAL", new ReportTemplate( "USAGE_INTERVAL", resourcePath+"/Usage_IntervalMasterReport_Template.jrxml", resourcePath+"/UsageReportIntervalTransform_Master.xsl", Arrays.asList(
            new ReportTemplate( "", "", "", null )
        )) );

        reportTemplates = Collections.unmodifiableMap( templates );
    }

    private Map<String, Object> getParameters() throws ReportGenerationException {
        Map<String, Object> parameters = new HashMap<String, Object>();

        //Required
        parameters.put(TEMPLATE_FILE_ABSOLUTE, "com/l7tech/gateway/standardreports/Styles.jrtx");
        parameters.put(SUBREPORT_DIRECTORY, ".");
        parameters.put(HOURLY_MAX_RETENTION_NUM_DAYS, 32); // TODO serverconfig property?

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
        parameters.put(STYLES_FROM_TEMPLATE, sMap);

        //Only required because jasper reports for some reason ignores the value of scriptletClass from the
        //jasperreport element attribute, so specifying it as a parameter explicitly fixes this issue
        parameters.put(REPORT_SCRIPTLET, new ScriptletHelper());

        return parameters;
    }

    /**
     * TODO parameter filtering / validation
     */
    @SuppressWarnings({"unchecked"})
    private void processReportParameters( final Connection connection, final Map<String, Object> reportParams ) throws ReportGenerationException {
        int numRelativeTimeUnits = (Integer)reportParams.get("RELATIVE_NUM_OF_TIME_UNITS");
        Utilities.UNIT_OF_TIME relUnitOfTime = Utilities.getUnitFromString(reportParams.get("RELATIVE_TIME_UNIT").toString());
        reportParams.put( "RELATIVE_TIME_UNIT", relUnitOfTime );
        long startTimeInPast = Utilities.getRelativeMilliSecondsInPast(numRelativeTimeUnits, relUnitOfTime );
        long endTimeInPast = Utilities.getMillisForEndTimePeriod(relUnitOfTime);

        List<String> keys = (List<String>) reportParams.get("MAPPING_KEYS");
        List<String> values = (List<String>) reportParams.get("MAPPING_VALUES");
        List<String> useAnd = (List<String>) reportParams.get("VALUE_EQUAL_OR_LIKE");
        Map<String, Set<String>> serivceIdsToOp = (Map<String, Set<String>>) reportParams.get("SERVICE_ID_TO_OPERATIONS_MAP");

        boolean useUser = Boolean.valueOf(reportParams.get("USE_USER").toString());
        List<String> authUsers = (List<String>) reportParams.get("AUTHENTICATED_USERS");
        int resolution = Utilities.getSummaryResolutionFromTimePeriod(30, startTimeInPast, endTimeInPast);

        boolean isContextMapping = Boolean.valueOf(reportParams.get("IS_CONTEXT_MAPPING").toString());
        boolean isDetail = Boolean.valueOf(reportParams.get("IS_DETAIL").toString());

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

        reportParams.put(DISPLAY_STRING_TO_MAPPING_GROUP, displayStringToGroup);
        reportParams.put(MAPPING_GROUP_TO_DISPLAY_STRING, groupToDisplayString);
    }

    /**
     * TODO fix runtime document generation for reports and sub reports 
     */
    @SuppressWarnings({"unchecked"})
    private Document getRuntimeDocument( final ReportTemplate template,
                                         final ReportTemplate subTemplate,
                                         final Map<String,Object> reportParameters  ) {
        Document runtimeDocument = null;

        if ( "PERFORMANCE_SUMMARY".equals( template.getName()) ||
             "PERFORMANCE_INTERVAL".equals( template.getName()) ) {
            runtimeDocument = Utilities.getPerfStatAnyRuntimeDoc( false, (LinkedHashMap<String,String>)reportParameters.get(MAPPING_GROUP_TO_DISPLAY_STRING) );
        }

        //Utilities.getUsageRuntimeDoc(useUser, keys, distinctMappingSets);

        return runtimeDocument;
    }

    /**
     *
     */
    private Map<String, Object> getTransformationParameters( final Document document ) {
        final Map<String, Object> params = new HashMap<String, Object>();

        if ( document != null ) {
            params.put("RuntimeDoc", document);
        }

        params.put("FrameMinWidth", 535);
        params.put("PageMinWidth", 595);
        params.put("ReportInfoStaticTextSize", 128);

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
            Transformer transformer = transformerFactory.newTransformer( template.getReportXslSource() );

            documentBuilderFactory.setNamespaceAware( true );
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            builder.setEntityResolver( getEntityResolver() );
            Document doc = builder.parse( template.getReportXmlSource() );

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            StreamResult result = new StreamResult(baos);
            XmlUtil.softXSLTransform(doc, result, transformer, params);

            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

            report = JasperCompileManager.compileReport(bais);
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
        private final String name;
        private final String reportXml;
        private final String reportXsl;
        private final Collection<ReportTemplate> subReports;

        ReportTemplate( final String name,
                        final String reportXml,
                        final String reportXsl,
                        final Collection<ReportTemplate> subReports ) {
            this.name = name;
            this.reportXml = reportXml;
            this.reportXsl = reportXsl;
            this.subReports = subReports == null ?
                    Collections.<ReportTemplate>emptyList() :
                    Collections.unmodifiableCollection(subReports);
        }

        public String getName() {
            return name;
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
