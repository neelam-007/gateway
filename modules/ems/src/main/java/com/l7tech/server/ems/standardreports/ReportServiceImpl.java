package com.l7tech.server.ems.standardreports;

import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.audit.AuditContextUtils;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.gateway.GatewayContext;
import com.l7tech.server.ems.gateway.GatewayContextFactory;
import com.l7tech.server.ems.gateway.GatewayException;
import com.l7tech.server.management.api.node.ReportApi;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.xml.ws.soap.SOAPFaultException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ReportService handles report submissions and artifact retrieval. 
 */
public class ReportServiceImpl implements InitializingBean, ReportService {

    //- PUBLIC

    public ReportServiceImpl( final PlatformTransactionManager transactionManager,
                              final StandardReportManager reportManager,
                              final SsgClusterManager clusterManager,
                              final GatewayContextFactory contextFactory,
                              final Timer timer  ) {
        this.transactionManager = transactionManager;
        this.reportManager = reportManager;
        this.clusterManager = clusterManager;
        this.contextFactory = contextFactory;
        this.timer = timer;
    }

    /**
     * Add a ReportSubmission to the processing queue for the given cluster.
     *
     * @param clusterId The target cluster for the report.
     * @param user The user submitting the report.
     * @param reportSubmission The report to be generated.
     */
    @Override
    public void enqueueReport( final String clusterId,
                               final User user,
                               final ReportApi.ReportSubmission reportSubmission ) throws ReportException {
        SsgCluster cluster;
        try {
            cluster = clusterManager.findByGuid( clusterId );
        } catch ( FindException fe ) {
            throw new ReportException( "Error finding cluster.", fe );
        }

        if ( cluster == null ) throw new ReportException( "Unknown cluster '"+clusterId+"'." );

        if ( !cluster.getTrustStatus() )  throw new ReportException( "Cluster trust not established '"+clusterId+"'." );

        StandardReport report = new StandardReport();
        report.setName( reportSubmission.getName() != null && reportSubmission.getName().trim().length()>0 ? reportSubmission.getName().trim() : "Unnamed Report" );
        report.setDescription( "" );
        report.setProvider( user.getProviderId() );
        report.setUserId( user.getId() );
        report.setSsgCluster( cluster );
        report.setStatusTime( System.currentTimeMillis() );
        report.setStatus( "SUBMITTED" );
        report.setTime( report.getStatusTime() );

        final String host = cluster.getSslHostName();
        final int port = cluster.getAdminPort();
        final String submissionId;
        try {
            GatewayContext context = contextFactory.createGatewayContext( user, cluster.getGuid(), host, port );
            ReportApi reportApi = context.getReportApi();
            submissionId = reportApi.submitReport( reportSubmission, Arrays.asList( ReportApi.ReportOutputType.PDF, ReportApi.ReportOutputType.HTML ) );
        } catch ( SOAPFaultException sfe ) {
            final ConnectException ce = ExceptionUtils.getCauseIfCausedBy(sfe, ConnectException.class);
            if (ce != null) {
                // Return the ConnectionException as the direct cause of ReportException.
                // The reason to skip SOAPFaultException is because Wicket throws WicketNotSerializableException
                // due to com.sun.xml.messaging.saaj.soap.ver1_1.SOAPPart1_1Impl not serializable.
                // (bugzilla 6856)
                throw new ReportException( "Cannot contact gateway '"+(host+":"+port)+"'.", ce );
            }
            throw sfe;
        } catch ( GatewayException ge ) {
            throw new ReportException( "Error submitting report generation to gateway '"+(host+":"+port)+"'.", ge );
        } catch ( ReportApi.ReportException re ) {
            throw new ReportException( "Error submitting report generation to gateway '"+(host+":"+port)+"'.", re );
        }

        report.setSubmissionHost( host );
        report.setSubmissionId( submissionId );

        try {
            reportManager.save( report );
        } catch ( SaveException se ) {
            throw new ReportException( "Error saving report record.", se );
        }

        logger.info("Report submitted with id '"+submissionId+"'.");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        timer.schedule( new TimerTask(){
            @Override
            public void run() {
                processReport();
            }
        }, 30187, 10321 );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( ReportServiceImpl.class.getName() );

    private final PlatformTransactionManager transactionManager;
    private final StandardReportManager reportManager;
    private final SsgClusterManager clusterManager;
    private final GatewayContextFactory contextFactory;
    private final Timer timer;

    private void processReport() {
        boolean wasSystem = AuditContextUtils.isSystem();
        AuditContextUtils.setSystem(true);
        try {
            TransactionTemplate template = new TransactionTemplate(transactionManager);
            template.execute( new TransactionCallbackWithoutResult(){
                @Override
                protected void doInTransactionWithoutResult(final TransactionStatus transactionStatus) {
                        processReportsWithTransaction();
                }
            } );
        } finally {
            AuditContextUtils.setSystem(wasSystem);
        }
    }

    /**
     * Report status is one of :
     *
     *   SUBMITTED (our own status)
     *   PENDING   (report api)
     *   RUNNING   (report api)
     *   COMPLETED (report api)
     *   FAILED    (report api)
     */
    private void processReportsWithTransaction() {
        try {
            for ( StandardReport report : reportManager.findByStatus("SUBMITTED") ) {
                logger.info("Processing report submission '"+report.getSubmissionId()+"'.");

                SsgCluster cluster = report.getSsgCluster();
                if ( cluster == null ) {
                    logger.info("Missing cluster for report submission '"+report.getSubmissionId()+"', marking FAILED.");
                    report.setStatus( ReportApi.ReportStatus.Status.FAILED.toString() );
                    report.setStatusTime( System.currentTimeMillis() );
                } else {
                    final String host = report.getSubmissionHost();
                    final int port = cluster.getAdminPort();
                    try {
                        GatewayContext context = contextFactory.createGatewayContext( null, null, host, port );
                        ReportApi reportApi = context.getReportApi();
                        Collection<ReportApi.ReportStatus> statusCollection =
                                reportApi.getReportStatus( Arrays.asList( report.getSubmissionId() ) );
                        ReportApi.ReportStatus status = null;
                        for ( ReportApi.ReportStatus rs : statusCollection ) {
                            if ( report.getSubmissionId().equals( rs.getId() ) ) {
                                status = rs;
                                break;
                            }
                        }

                        if ( status == null ) {
                            logger.info("Could not get status for report submission '"+report.getSubmissionId()+"', marking FAILED.");
                            report.setStatus( ReportApi.ReportStatus.Status.FAILED.toString() );
                            report.setStatusTime( System.currentTimeMillis() );
                        } else {
                            logger.info("Report submission '"+report.getSubmissionId()+"', is '"+status.getStatus()+"'.");
                            switch ( status.getStatus() ) {
                                case PENDING:
                                case RUNNING:
                                    break;
                                case COMPLETED:
                                    try {
                                        // Get artifacts
                                        ReportApi.ReportResult result = reportApi.getReportResult( report.getSubmissionId(), ReportApi.ReportOutputType.PDF );
                                        StandardReportArtifact artifact = new StandardReportArtifact();
                                        artifact.setContentType( "application/pdf" );
                                        artifact.setReport( report );
                                        ByteArrayOutputStream out = new ByteArrayOutputStream( 4094 );
                                        result.getData().writeTo( out );
                                        artifact.setReportData( out.toByteArray() );
                                        report.getArtifacts().add( artifact );

                                        ReportApi.ReportResult resultHtml = reportApi.getReportResult( report.getSubmissionId(), ReportApi.ReportOutputType.HTML );
                                        StandardReportArtifact artifactHtml = new StandardReportArtifact();
                                        artifactHtml.setContentType( "application/zip" );
                                        artifactHtml.setReport( report );
                                        ByteArrayOutputStream outHtml = new ByteArrayOutputStream( 4094 );
                                        resultHtml.getData().writeTo( outHtml );
                                        artifactHtml.setReportData( outHtml.toByteArray() );
                                        report.getArtifacts().add( artifactHtml );

                                        report.setStatus( ReportApi.ReportStatus.Status.COMPLETED.toString() );
                                        report.setStatusTime( System.currentTimeMillis() );
                                    } catch ( IOException e ) {
                                        logger.log( Level.WARNING, "Error getting result for report '"+report.getSubmissionId()+"'.", e );
                                        report.setStatus( ReportApi.ReportStatus.Status.FAILED.toString() );
                                        report.setStatusTime( System.currentTimeMillis() );
                                    }
                                    break;
                                default:
                                    report.setStatus( ReportApi.ReportStatus.Status.FAILED.toString() );
                                    report.setStatusTime( System.currentTimeMillis() );
                                    if ( status.getMessage() != null ) {
                                        if ( status.getMessage().length() > 255 ) {
                                            report.setStatusMessage( status.getMessage().substring(0, 255) );
                                        } else {
                                            report.setStatusMessage( status.getMessage() );
                                        }
                                    }
                                    break;
                            }
                        }
                    } catch ( GatewayException ge ) {
                        logger.log( Level.WARNING, "Error getting status for report '"+report.getSubmissionId()+"'.", ge );
                    } catch ( ReportApi.ReportException re ) {
                        logger.log( Level.WARNING, "Error getting status for report '"+report.getSubmissionId()+"'.", re );
                        report.setStatus( ReportApi.ReportStatus.Status.FAILED.toString() );
                        report.setStatusTime( System.currentTimeMillis() );
                    } catch ( SOAPFaultException sfe ) {
                        if ( GatewayContext.isNetworkException(sfe) ) {
                            logger.log( Level.FINE, "Connection failed for cluster '"+host+"'." );
                        } else if ( "Authentication Required".equals(sfe.getMessage()) ){
                            logger.log( Level.FINE, "Trust failed for cluster '"+host+"'." );
                        } else{
                            logger.log( Level.WARNING, "Error getting status for report '"+report.getSubmissionId()+"'.", sfe );
                        }
                    }

                    try {
                        reportManager.update( report );
                    } catch ( UpdateException se ) {
                        logger.log( Level.WARNING, "Error updating status for report '"+report.getSubmissionId()+"'.", se );
                    }
                }
            }
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error accessing reports.", fe );        
        }
    }
}
