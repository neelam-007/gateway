package com.l7tech.server.cluster;

import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.TimeUnit;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ResolvingComparator;
import com.l7tech.util.Resolver;
import com.l7tech.gateway.standardreports.ReportGenerator;
import com.l7tech.gateway.common.mapping.MessageContextMapping;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;

import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.ResultSet;

import org.springframework.orm.hibernate3.support.HibernateDaoSupport;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.jdbc.Work;

/**
 * Gateway implementation for Report API.
 */
public class ReportApiImpl extends HibernateDaoSupport implements ReportApi {

    //- PUBLIC

    public ReportApiImpl( final PlatformTransactionManager transactionManager,
                          final RbacServices rbacServices,
                          final Timer reportTimer ) {
        this.transactionManager = transactionManager;
        this.rbacServices = rbacServices;
        this.reportTimer = reportTimer;
    }

    private void validateReportParams(String [] expectedParams, Set<String> receivedParamNames) throws ReportException{
        for(String s: expectedParams){
            if(!receivedParamNames.contains(s)){
                logger.info("Expected parameter: '" + s+"' missing from report submission");
                throw new ReportException("Expected parameter: '" + s+"' missing from report submission");
            }
        }
    }


    @Override
    public String submitReport( final ReportSubmission submission, final Collection<ReportOutputType> types ) throws ReportException {
        checkPermitted();
        final Map<String,Object> reportParameters = buildReportParameters( submission.getParameters() );

        String [] expectedParams = ReportApi.ReportType.getApplicableParameters(submission.getType());
        validateReportParams(expectedParams, reportParameters.keySet() );

        //validateReportParams validates all common params, the relative and absolute time params are only validated
        //when we know the values for IS_RELATIVE and IS_ABSOLUTE
        for(ReportSubmission.ReportParam reportParam: submission.getParameters()){
            if(reportParam.getName().equals(ReportApi.ReportParameters.IS_RELATIVE)){
                Boolean isRelative = (Boolean) reportParam.getValue();
                if(isRelative){
                    validateReportParams(ReportApi.ReportParameters.RELATIVE_TIME_PARAMS, reportParameters.keySet());
                }else{
                    validateReportParams(ReportApi.ReportParameters.ABSOLUTE_TIME_PARAMS, reportParameters.keySet());
                }
            }
        }

        ReportProcessingTask task = new ReportProcessingTask( submission, types, reportParameters );
        reports.put( task.getId(), new ReportData( task.getId() ) );
        logger.info("Report submission accepted '"+task.getId()+"'.");
        reportTimer.schedule( task, 0 );

        return task.getId();
    }

    @Override
    public Collection<ReportStatus> getReportStatus( final Collection<String> ids ) throws ReportException {
        List<ReportStatus> status = new ArrayList<ReportStatus>();

        for ( String id : ids ) {
            ReportStatus reportStatus = new ReportStatus();
            reportStatus.setId( id );

            ReportData data = reports.get(id);
            if ( data == null ) {
                throw new ReportException( "Report not found '"+id+"'." );
            } else {
                if ( data.hasArtifact() ) {
                    reportStatus.setStatus( ReportStatus.Status.COMPLETED );
                } else if ( data.getFailureMessage() != null ) {
                    reportStatus.setStatus( ReportStatus.Status.FAILED );
                }  else if ( data.getStartedTime() != null ) {
                    reportStatus.setStatus( ReportStatus.Status.RUNNING );
                } else {
                    reportStatus.setStatus( ReportStatus.Status.PENDING );
                }
                reportStatus.setTime( data.getTime() );
                reportStatus.setMessage( data.getFailureMessage() );
            }

            status.add( reportStatus );
        }

        logger.info("Reporting status '"+status+"' for ids '"+ids+"'.");

        return status;
    }

    @Override
    public ReportResult getReportResult( final String id, final ReportOutputType type ) throws ReportException {
        ReportResult result = new ReportResult();

        result.setId( id );
        result.setType( type );

        ReportData data = reports.get(id);
        if ( data == null ) {
            throw new ReportException( "Report not found '"+id+"'." );
        } else {
            byte[] reportData = data.getArtifact( type );
            if ( reportData == null ) {
                throw new ReportException( "Report '"+id+"' has no artifact of type '"+type+"'." );                
            }
            result.setData( new DataHandler(new ByteArrayDataSource( reportData, "application/octet-stream" )) );
        }

        return result;
    }

    @Override
    @Transactional(readOnly=true)
    public Collection<GroupingKey> getGroupingKeys() throws ReportException {
        final Collection<GroupingKey> keys = new ArrayList<GroupingKey>();
        keys.add( new GroupingKey( GroupingKey.GroupingKeyType.STANDARD, MessageContextMapping.MappingType.AUTH_USER.toString()) );
        keys.add( new GroupingKey( GroupingKey.GroupingKeyType.STANDARD, MessageContextMapping.MappingType.IP_ADDRESS.toString() ) );

        getHibernateTemplate().execute( new HibernateCallback(){
            @Override
            public Object doInHibernate( final Session session ) throws HibernateException, SQLException {
                session.doWork( new Work(){
                    @Override
                    public void execute( final Connection connection ) throws SQLException {
                        Statement statement = null;
                        ResultSet results = null;
                        try {
                            statement = connection.createStatement();
                            results = statement.executeQuery( MAPPING_SQL );

                            while ( results.next() ) {
                                keys.add( new GroupingKey( GroupingKey.GroupingKeyType.CUSTOM, results.getString(1) ) );
                            }
                        } catch ( SQLException se ) {
                            logger.log( Level.WARNING, "Error loading context mapping keys.", se );
                        } finally {
                            ResourceUtils.closeQuietly( results );
                            ResourceUtils.closeQuietly( statement );
                        }
                    }
                });
                return null;
            }
        } );

        return keys;
    }

    /**
     *
     */
    @ManagedResource(description="Reporting Service", objectName="l7tech:type=ReportService")
    public static class ManagedReportApiImpl {
        private final ReportApiImpl reportApiImpl;

        protected ManagedReportApiImpl( final ReportApi reportApi ) {
            this.reportApiImpl = unwrapProxy(reportApi);
        }

        @ManagedAttribute(description="Active Reports", currencyTimeLimit=30)
        public int getActiveReportCount() {
            synchronized( reportApiImpl.reports ) {
                return reportApiImpl.reports.size();
            }
        }

        @ManagedAttribute(description="Maximum Report Age", currencyTimeLimit=30)
        public long getReportMaxAge() {
            return MAX_REPORT_AGE;
        }

        @ManagedAttribute(description="Maximum Active Reports", currencyTimeLimit=30)
        public int getMaxActiveReports() {
            return MAX_REPORTS;
        }

        @ManagedAttribute(description="Active Report Ids", currencyTimeLimit=30)
        public Set<String> getActiveReportIdentifiers() {
            synchronized( reportApiImpl.reports ) {
                Set<String> keys = new TreeSet<String>();
                keys.addAll(reportApiImpl.reports.keySet());
                return keys;
            }
        }

        @ManagedOperation(description="Clear Active Reports", currencyTimeLimit=30)
        public void clearActiveReports() {
            synchronized( reportApiImpl.reports ) {
                reportApiImpl.reports.clear();
            }
        }

        private ReportApiImpl unwrapProxy( final ReportApi bean ) {
            if ( AopUtils.isAopProxy(bean) && bean instanceof Advised ) {
                Advised advised = (Advised) bean;
                try {
                    return (ReportApiImpl) advised.getTargetSource().getTarget();
                } catch ( Exception e ) {
                    throw new RuntimeException(e);
                }
            }

            return (ReportApiImpl) bean;
        }
    }

    //- PROTECTED

    @Override
    protected void initDao() throws Exception {
        reportTimer.schedule( new ReportCleanupTask(), 32951, 27371 );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ReportApiImpl.class.getName() );

    private static final int MAX_REPORTS = SyspropUtil.getInteger("com.l7tech.server.reports.max", 50);
    private static final long MAX_REPORT_AGE = SyspropUtil.getLong("com.l7tech.server.reports.expiry", TimeUnit.MINUTES.toMillis(30));

    private static final String MAPPING_SQL =
            "SELECT mapping1_key FROM message_context_mapping_keys WHERE mapping1_type='CUSTOM_MAPPING' UNION\n" +
            "SELECT mapping2_key FROM message_context_mapping_keys WHERE mapping2_type='CUSTOM_MAPPING' UNION\n" +
            "SELECT mapping3_key FROM message_context_mapping_keys WHERE mapping3_type='CUSTOM_MAPPING' UNION\n" +
            "SELECT mapping4_key FROM message_context_mapping_keys WHERE mapping4_type='CUSTOM_MAPPING' UNION\n" +
            "SELECT mapping5_key FROM message_context_mapping_keys WHERE mapping5_type='CUSTOM_MAPPING'";

    //TODO implement gateway DB report storage / retrieval
    private final Map<String,ReportData> reports = Collections.synchronizedMap(new HashMap<String,ReportData>());
    private final PlatformTransactionManager transactionManager;
    private final RbacServices rbacServices;
    private final Timer reportTimer;

    private void checkPermitted() throws ReportException {
        User user = JaasUtils.getCurrentUser();
        if ( user == null ) {
            throw new ReportException( "Permission denied." );
        }

        try {
            if ( !rbacServices.isPermittedForAnyEntityOfType(user, OperationType.READ, EntityType.SERVICE) ||
                 !rbacServices.isPermittedForAnyEntityOfType(user, OperationType.READ, EntityType.METRICS_BIN) ||
                 !rbacServices.isPermittedForAnyEntityOfType(user, OperationType.READ, EntityType.USER) ) {
                throw new ReportException( "Permission denied." );
            }
        } catch (FindException fe) {
            logger.log( Level.WARNING, "Error when checking permissions to run report.", fe);
            throw new ReportException( "Permission denied." );
        }
    }

    private Map<String,Object> buildReportParameters( final Collection<ReportSubmission.ReportParam> parameters ) {
        Map<String,Object> params = new HashMap<String,Object>();

        for ( ReportSubmission.ReportParam param : parameters ) {
            Object value = param.getValue();
            if ( logger.isLoggable(Level.FINE) ) {
                logger.fine("Accepted report parameter '"+param.getName()+"' = '"+value+"'.");
            }
            params.put( param.getName(), value );
        }

        return Collections.unmodifiableMap( params );
    }

    private static final class ReportData {
        private final String id;
        private final Map<ReportOutputType,byte[]> artifacts;
        private final String failureMessage;
        private final long time = System.currentTimeMillis();
        private final Long startedTime;

        ReportData( final String id, final Map<ReportOutputType,byte[]> artifacts, final long startedTime ) {
            this.id = id;
            this.artifacts = artifacts;
            this.failureMessage = null;
            this.startedTime = startedTime;
        }

        ReportData( final String id, final String failureMessage, final long startedTime ) {
            this.id = id;
            this.artifacts = Collections.emptyMap();
            this.failureMessage = failureMessage;
            this.startedTime = startedTime;
        }

        ReportData( final String id, final long startedTime ) {
            this.id = id;
            this.artifacts = Collections.emptyMap();
            this.failureMessage = null;
            this.startedTime = startedTime;
        }

        ReportData( final String id ) {
            this.id = id;
            this.artifacts = Collections.emptyMap();
            this.failureMessage = null;
            this.startedTime = null;
        }

        public String getId() {
            return id;
        }

        public boolean hasArtifact() {
            return !artifacts.isEmpty();
        }

        public byte[] getArtifact( final ReportOutputType type  ) {
            return artifacts.get( type );
        }

        public String getFailureMessage() {
            return failureMessage;
        }

        public long getTime() {
            return time;
        }

        public Long getStartedTime() {
            return startedTime;
        }

        public boolean isExpired( long checkTime ) {
            return (time+MAX_REPORT_AGE) < checkTime;
        }
    }

    /**
     * Cleanup task, kick out expired reports, or the oldest if there are too many reports.
     */
    private final class ReportCleanupTask extends TimerTask {
        @SuppressWarnings({"unchecked"})
        public void run() {
            synchronized( reports ) {
                long time = System.currentTimeMillis();

                // evict any expired
                Iterator<Map.Entry<String, ReportData>> reportEntryIterator = reports.entrySet().iterator();
                while ( reportEntryIterator.hasNext() ) {
                    Map.Entry<String,ReportData> entry = reportEntryIterator.next();
                    if ( entry.getValue().isExpired( time ) ) {
                        reportEntryIterator.remove();
                    }
                }

                // evict oldest
                int removalCount = reports.size() - MAX_REPORTS;
                if ( removalCount > 0 ) {
                    List<ReportData> dataList = new ArrayList<ReportData>();
                    dataList.addAll( reports.values() );
                    Collections.sort( dataList, new ResolvingComparator( new Resolver(){
                        public Object resolve( final Object key ) {
                            ReportData data = (ReportData) key;
                            return data.getTime();
                        }
                    }, false) );
                    reports.values().removeAll( dataList.subList( 0, removalCount ) );
                }
            }
        }
    }

    /**
     * Report generation task.
     */
    private final class ReportProcessingTask extends TimerTask {
        private final UUID reportId;
        private final ReportSubmission submission;
        private final Collection<ReportOutputType> types;
        private final Map<String,Object> reportParameters;

        private ReportProcessingTask( final ReportSubmission submission,
                                      final Collection<ReportOutputType> types,
                                      final  Map<String,Object> reportParameters ) {
            this.reportId = UUID.randomUUID();
            this.submission = submission;
            this.types = types;
            this.reportParameters = reportParameters;
        }

        public String getId() {
            return reportId.toString();
        }

        @Override
        public void run() {
            logger.info("Processing report submission '"+reportId.toString()+"'.");
            final long startTime = System.currentTimeMillis();

            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.setReadOnly( true );
            template.execute( new TransactionCallbackWithoutResult(){
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {
                    try {
                        reports.put( getId(), new ReportData( getId(), startTime ) );
        
                        final ReportGenerator generator = new ReportGenerator();

                        // Compile report
                        final ReportGenerator.ReportGenerationException[] compException = new ReportGenerator.ReportGenerationException[1];
                        final ReportGenerator.ReportHandle[] compiledHandle = new ReportGenerator.ReportHandle[1];
                        getHibernateTemplate().execute( new HibernateCallback(){
                            @Override
                            public Object doInHibernate( final Session session ) throws HibernateException, SQLException {
                                session.doWork( new Work(){
                                    @Override
                                    public void execute( final Connection connection ) throws SQLException {
                                        try {
                                            compiledHandle[0] = generator.compileReport( submission.getType(), reportParameters, connection );
                                        } catch ( ReportGenerator.ReportGenerationException rge ) {
                                            compException[0] = rge;
                                        }
                                    }
                                });
                                return null;
                            }
                        } );
                        if ( compException[0] != null ) {
                            logger.log( Level.WARNING, "Error compiling report '"+reportId.toString()+"'.", compException[0] );
                            throw new ReportException( ExceptionUtils.getMessage( compException[0] ) );
                        }

                        // Fill report
                        final ReportGenerator.ReportGenerationException[] fillException = new ReportGenerator.ReportGenerationException[1];
                        final ReportGenerator.ReportHandle[] filledHandle = new ReportGenerator.ReportHandle[1];
                        getHibernateTemplate().execute( new HibernateCallback(){
                            @Override
                            public Object doInHibernate( final Session session ) throws HibernateException, SQLException {
                                session.doWork( new Work(){
                                    @Override
                                    public void execute( final Connection connection ) throws SQLException {
                                        logger.info("Filling report '"+reportId.toString()+"'.");
                                        try {
                                            filledHandle[0] = generator.fillReport( compiledHandle[0], connection );
                                            logger.info("Completed fill for report '"+reportId.toString()+"'.");
                                        } catch ( ReportGenerator.ReportGenerationException rge ) {
                                            fillException[0] = rge;
                                        }
                                    }
                                });
                                return null;
                            }
                        } );
                        if ( fillException[0] != null ) {
                            logger.log( Level.WARNING, "Error filling report '"+reportId.toString()+"'.", fillException[0] );
                            throw new ReportException( ExceptionUtils.getMessage( fillException[0] ) );
                        }

                        // Generate report artifacts
                        Map<ReportOutputType,byte[]> artifacts = new HashMap<ReportOutputType,byte[]>();
                        for ( ReportOutputType type : types ) {
                            try {
                                switch ( type ) {
                                    case PDF:
                                        artifacts.put( type, generator.generateReportOutput( filledHandle[0], type.toString() ) );
                                        logger.info("Report artifact of type '"+type+"' created for id '"+reportId.toString()+"'.");
                                        break;
                                    case HTML:
                                        artifacts.put( type, generator.generateReportOutput( filledHandle[0], type.toString() ) );
                                        logger.info("Report artifact of type '"+type+"' created for id '"+reportId.toString()+"'.");
                                        break;
                                    default:
                                        throw new ReportException( "Report output type not supported '"+type+"'." );
                                }
                            } catch ( ReportGenerator.ReportGenerationException rge ) {
                                logger.log( Level.WARNING, "Error generating report output '"+reportId.toString()+"'.", rge );
                                throw new ReportException( ExceptionUtils.getMessage( rge ) );
                            }
                        }

                        reports.put( reportId.toString(), new ReportData( reportId.toString(), artifacts, startTime ) );
                    } catch ( ReportException re ) {
                        reports.put( reportId.toString(), new ReportData( reportId.toString(), ExceptionUtils.getMessage(re), startTime ) );
                    } catch ( Exception e ) {
                        logger.log( Level.WARNING, "Unexpected error processing report '"+reportId.toString()+"'.", e );
                        reports.put( reportId.toString(), new ReportData( reportId.toString(), "Error processing report.", startTime ) );
                    }
                }
            });
        }
    }
}
