package com.l7tech.server.ems.standardreports;

import com.l7tech.gateway.common.security.rbac.AttemptedReadSpecific;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.ems.ui.SecureResource;
import com.l7tech.server.util.JaasUtils;
import com.l7tech.util.IOUtils;
import com.l7tech.util.ResourceUtils;
import org.apache.wicket.util.resource.AbstractResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.ResourceStreamNotFoundException;
import org.apache.wicket.util.resource.StringResourceStream;
import org.apache.wicket.util.time.Time;
import org.apache.wicket.util.value.ValueMap;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Web resource for report downloads.
 *
 * TODO remove ownership based access check and restore rbac checks.
 */
public class ReportViewResource extends SecureResource {

    //- PUBLIC

    /**
     *
     */
    public ReportViewResource() {
        super( null );//new AttemptedReadAny(EntityType.ESM_STANDARD_REPORT) );
    }

    //- PROTECTED

    @Override
    protected IResourceStream getSecureResourceStream() {
        IResourceStream resource = null;
        ValueMap parameters = getParameters();

        if ( parameters.containsKey("reportId") ) {
            String id = parameters.getString("reportId");
            String file = "report.html";
            if ( parameters.containsKey("file") ) {
                file = parameters.getString("file");
            }

            if ( !hasPermission( new AttemptedReadSpecific(EntityType.ESM_STANDARD_REPORT, id) ) &&
                 !isOwner(id) ) {
                resource = getAccessDeniedStream();
            } else {
                StandardReportManager manager = getStandardReportManager();
                if ( manager != null ) {
                    try {
                        final StandardReport report = manager.findByPrimaryKey( Goid.parseGoid(id) );
                        StandardReportArtifact typedArtifact = null;
                        if ( report != null ) {
                            for ( StandardReportArtifact artifact : report.getArtifacts() ) {
                                if ( artifact.getContentType().equals("application/zip") ) {
                                    typedArtifact = artifact;
                                    break;
                                }
                            }
                        } else {
                            logger.warning("Report id not found when accessing report resource '"+id+"'.");
                        }

                        final StandardReportArtifact resourceArtifact = typedArtifact;
                        if ( resourceArtifact != null ) {
                            byte[] data = null;
                            ZipInputStream zipIn = null;

                            try {
                                zipIn = new ZipInputStream( new ByteArrayInputStream(resourceArtifact.getReportData()) );
                                ZipEntry entry;
                                while( (entry = zipIn.getNextEntry()) != null) {
                                    if ( file.equals(entry.getName()) ) {
                                        data = IOUtils.slurpStream( zipIn );
                                        break;
                                    }
                                }
                            } catch (IOException e) {
                                logger.log(Level.WARNING, "Report artifact part read error when accessing report resource '"+id+"', file '"+file+"'.", e);
                            } finally {
                                ResourceUtils.closeQuietly( zipIn );
                            }

                            if ( data == null ) {
                                logger.warning("Report artifact part not found when accessing report resource '"+id+"', file '"+file+"'.");
                                resource = new StringResourceStream( "" );
                            } else {
                                final String contentType = file.startsWith("images/") ? "image/gif" : "text/html";
                                final ByteArrayInputStream in = new ByteArrayInputStream( data );
                                resource = new AbstractResourceStream(){
                                    @Override
                                    public String getContentType() {
                                        return contentType;
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
                            }
                        } else {
                            logger.warning("Report artifact not found when accessing report resource '"+id+"', file '"+file+"'.");
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
            logger.warning("Not processing resource request for report data, download details not found.");
            resource = new StringResourceStream( "" );
        }

        return resource;
    }

    //- PACKAGE

    static StandardReportManager getStandardReportManager() {
        return StandardReportManagerRef.get();
    }

    static void setStandardReportManager( final StandardReportManager standardReportManager ) {
        StandardReportManagerRef.set( standardReportManager );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(ReportViewResource.class.getName());

    private static AtomicReference<StandardReportManager> StandardReportManagerRef = new AtomicReference<StandardReportManager>();

    private boolean isOwner( final String reportId ) {
        boolean owner = false;

        StandardReportManager manager = getStandardReportManager();
        User user = JaasUtils.getCurrentUser();
        if ( manager != null && user != null ) {
            try {
                StandardReport report = manager.findByPrimaryKey( Goid.parseGoid( reportId ) );
                if ( report != null ) {
                    if ( user.getId().equals( report.getUserId() ) &&
                         user.getProviderId().equals(report.getProvider() )){
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