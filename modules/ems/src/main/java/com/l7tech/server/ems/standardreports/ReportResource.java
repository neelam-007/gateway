package com.l7tech.server.ems.standardreports;

import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.value.ValueMap;
import org.apache.wicket.util.time.Time;

import java.io.InputStream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.atomic.AtomicReference;
import java.util.Date;
import java.text.SimpleDateFormat;

import com.l7tech.server.ems.ui.SecureResource;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.gateway.common.security.rbac.AttemptedReadSpecific;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.identity.User;

/**
 * Web resource for report downloads.
 *
 * TODO [steve] remove ownership based access check and restore rbac checks. 
 */
public class ReportResource extends SecureResource {

    //- PUBLIC

    /**
     *
     */
    public ReportResource() {
        super( null );//new AttemptedReadAny(EntityType.ESM_STANDARD_REPORT) );
    }

    //- PROTECTED

    @Override
    protected IResourceStream getSecureResourceStream() {
        IResourceStream resource = null;
        ValueMap parameters = getParameters();

        if ( parameters.containsKey("reportId") &&
             parameters.containsKey("type")) {
            String id = parameters.getString("reportId");
            String type = parameters.getString("type");

            if ( !hasPermission( new AttemptedReadSpecific(EntityType.ESM_STANDARD_REPORT, id) ) &&
                 !isOwner(id) ) {
                resource = getAccessDeniedStream();
            } else {
                StandardReportManager manager = getStandardReportManager();
                if ( manager != null ) {
                    try {
                        final StandardReport report = manager.findByPrimaryKey( Long.parseLong( id ) );
                        StandardReportArtifact typedArtifact = null;
                        if ( report != null ) {
                            for ( StandardReportArtifact artifact : report.getArtifacts() ) {
                                if ( artifact.getContentType().equals(type) ) {
                                    typedArtifact = artifact;
                                    break;
                                }
                            }
                        } else {
                            logger.warning("Report id not found when accessing report resource '"+id+"'.");
                        }

                        final StandardReportArtifact resourceArtifact = typedArtifact;
                        if ( resourceArtifact != null ) {
                            final ByteArrayInputStream in = new ByteArrayInputStream( resourceArtifact.getReportData() );
                            resource = new AbstractResourceStream(){
                                @Override
                                public String getContentType() {
                                    return resourceArtifact.getContentType();
                                }

                                @Override
                                public InputStream getInputStream() throws ResourceStreamNotFoundException {
                                    return in;
                                }

                                @Override
                                public void close() throws IOException {
                                    ResourceUtils.closeQuietly( in );
                                }

                                @Override
                                public Time lastModifiedTime() {
                                    return Time.milliseconds( report.getStatusTime() );
                                }
                            };
                        } else {
                            logger.warning("Report artifact not found when accessing report resource '"+id+"', type '"+type+"'.");
                            resource = new StringResourceStream( "" );
                        }
                    } catch ( NumberFormatException nfe ) {
                        logger.warning("Invalid report id when accessing report resource '"+id+"'.");
                        resource = new StringResourceStream( "" );
                    } catch (FindException e) {
                        logger.log( Level.WARNING, "Error finding report.", e );
                    }
                }
            }
        }

        if ( resource == null ){
            logger.warning("Not processing resource request for audit data, download details not found.");
            resource = new StringResourceStream( "" );
        }

        return resource;
    }

    @Override
    protected String getFilename() {
        String name = null;

        ValueMap parameters = getParameters();
        if ( parameters.containsKey("reportId") &&
             parameters.containsKey("type")) {
            String id = parameters.getString("reportId");
            String type = parameters.getString("type");

            String reportName = null;
            long reportTime = 0;
            String clusterName = null;
            StandardReportManager manager = getStandardReportManager();
            if ( manager != null ) {
                try {
                    final StandardReport report = manager.findByPrimaryKey( Long.parseLong( id ) );
                    if ( report != null ) {
                        reportName = report.getName();
                        reportTime = report.getStatusTime();
                        clusterName = report.getSsgCluster().getName();
                    }
                } catch ( NumberFormatException nfe ) {
                    logger.warning("Invalid report id when accessing report resource '"+id+"'.");
                } catch (FindException e) {
                    logger.log( Level.WARNING, "Error finding report for filename.", e );
                }
            }

            if ( "application/pdf".equals(type) ) {
                if ( reportName == null ) {
                    name = "report_" + id + ".pdf";
                } else {
                    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd HHmmss");
                    name = format.format( new Date(reportTime) ) + "_" + reportName + "_" + clusterName + ".pdf";
                }
            }
        }

        return name;
    }

    //- PACKAGE

    static StandardReportManager getStandardReportManager() {
        return StandardReportManagerRef.get();
    }

    static void setStandardReportManager( final StandardReportManager standardReportManager ) {
        StandardReportManagerRef.set( standardReportManager );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ReportResource.class.getName());

    private static AtomicReference<StandardReportManager> StandardReportManagerRef = new AtomicReference<StandardReportManager>();

    private boolean isOwner( final String reportId ) {
        boolean owner = false;

        StandardReportManager manager = getStandardReportManager();
        User user = JaasUtils.getCurrentUser();
        if ( manager != null && user != null ) {
            try {
                StandardReport report = manager.findByPrimaryKey( Long.parseLong( reportId ) );
                if ( report != null ) {
                    if ( user.getId().equals( report.getUserId() ) &&
                         user.getProviderId() == report.getProvider() ) {
                        owner = true;
                    }
                }
            } catch ( NumberFormatException nfe ) {
                // not owner
            } catch ( FindException e ) {
                logger.log( Level.WARNING, "Error finding report to check resource ownership", e );
            }
        }

        return owner;
    }
}