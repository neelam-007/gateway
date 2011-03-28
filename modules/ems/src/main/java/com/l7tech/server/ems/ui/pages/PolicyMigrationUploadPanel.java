package com.l7tech.server.ems.ui.pages;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.server.ems.enterprise.EnterpriseFolder;
import com.l7tech.server.ems.enterprise.EnterpriseFolderManager;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.lang.Bytes;
import javax.inject.Inject;
import org.apache.wicket.validation.validator.StringValidator;

import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.Serializable;

import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.ems.migration.MigrationRecordManager;
import com.l7tech.server.ems.migration.MigrationArtifactResource;
import com.l7tech.server.ems.enterprise.SsgClusterManager;
import com.l7tech.server.ems.enterprise.SsgCluster;
import com.l7tech.objectmodel.ObjectModelException;

/**
 * Panel for generation / upload of SSL key / certificate.
 */
public class PolicyMigrationUploadPanel extends Panel {

    private static final Logger logger = Logger.getLogger(PolicyMigrationUploadPanel.class.getName());
    private static final int MAX_ARCHIVE_FILE_UPLOAD_BYTES = SyspropUtil.getInteger("com.l7tech.ems.migrationFile.maxBytes", 1024 * 1024);

    private String successScript = null;

    @Inject
    private EnterpriseFolderManager enterpriseFolderManager;

    @Inject
    private SsgClusterManager clusterManager;

    @Inject
    private MigrationRecordManager migrationRecordManager;

    public PolicyMigrationUploadPanel( final String id ) {
        super( id );

        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        final FileUploadField archive = new FileUploadField("archive", new Model<FileUpload>());

        TextField<String> label = new TextField<String>("label");
        label.add( new StringValidator.LengthBetweenValidator(0, 32) );

        final UploadFormModel model = new UploadFormModel();
        final Form<UploadFormModel> archiveForm = new YuiFileUploadForm<UploadFormModel>("archiveForm", new CompoundPropertyModel<UploadFormModel>(model)){
            @Override
            @SuppressWarnings({"UnusedDeclaration"})
             protected void onError( final AjaxRequestTarget target ) {
                target.addComponent( feedback );
            }

            @Override
            @SuppressWarnings({"UnusedDeclaration", "SuspiciousSystemArraycopy"})
            protected void onSubmit( final AjaxRequestTarget target ) {
                logger.fine("Processing migration archive upload.");

                boolean success = false;
                FileUpload upload = null;
                try {
                    upload = archive.getFileUpload();
                    if ( upload != null ) {
                        final byte[] data = MigrationArtifactResource.unzip( upload.getInputStream() );

                        if ( data != null ) {
                            final Collection<SsgCluster> validClusters = clusterManager.findAll();
                            final Map<String,SsgCluster> clusters = new HashMap<String,SsgCluster>();
                            for(SsgCluster cluster : validClusters) {
                                clusters.put(cluster.getGuid(), cluster);
                            }
                            migrationRecordManager.create( model.getLabel(), data, getClusterCallback( clusters ));
                            success = true;
                        } else {
                            logger.fine("Archive not present in uploaded zip!");
                            feedback.error( "Error processing archive, please try again." );
                        }
                    } else {
                        logger.fine("Archive not present in upload!");
                        feedback.error( "Error processing archive, please try again." );
                    }
                } catch ( ObjectModelException ome ) {
                    logger.log( Level.WARNING, "Error processing migration archive upload: " + ExceptionUtils.getMessage(ome), ExceptionUtils.getDebugException(ome) );
                    feedback.error( "Error processing archive, please try again." );
                } catch (IOException ioe) {
                    logger.log( Level.WARNING, "IO error processing migration archive upload '"+ ExceptionUtils.getMessage(ioe)+"'.", ExceptionUtils.getDebugException(ioe) );
                    feedback.error( "Error processing archive, please try again." );
                } finally {
                    if (upload != null) upload.closeStreams();
                }

                if ( success ) {
                    if ( successScript != null && target != null ) {
                        target.prependJavascript(successScript);
                    }
    
                    PolicyMigrationUploadPanel.this.onSubmit( target );
                }

                if ( target != null ) {
                    target.addComponent( feedback );
                }
            }
        };

        archiveForm.setMaxSize( Bytes.bytes(MAX_ARCHIVE_FILE_UPLOAD_BYTES) );
        archiveForm.setOutputMarkupId( true );
        archiveForm.add( feedback.setOutputMarkupId(true) );
        archiveForm.add( label );
        archiveForm.add( archive );

        add(archiveForm);
    }

    /**
     * Get the cluster validation / creation callback.
     */
    private Functions.TernaryThrows<Pair<SsgCluster, SsgCluster>, String, String, String, SaveException> getClusterCallback( final Map<String, SsgCluster> clusters ) {
        return new Functions.TernaryThrows<Pair<SsgCluster,SsgCluster>,String,String,String,SaveException>() {
            @Override
            public Pair<SsgCluster,SsgCluster> call( final String sourceClusterId,
                                                     final String sourceClusterName,
                                                     final String targetClusterId ) throws SaveException {
                SsgCluster source = clusters.get( sourceClusterId );
                final SsgCluster target = targetClusterId==null ? null : clusters.get( targetClusterId );

                if ( source == null ) {
                    if ( targetClusterId == null ) {
                        // this is an offline migration from another ESM, so create an offline cluster
                        source = createOfflineCluster( sourceClusterId, sourceClusterName, clusters );
                    } else {
                        throw new SaveException( "Invalid archive, source Gateway Cluster not recognised: " + sourceClusterId );
                    }
                }

                if ( targetClusterId != null && target == null ) {
                    throw new SaveException( "Invalid archive, target Gateway Cluster not recognised: " + targetClusterId );
                }

                return new Pair<SsgCluster,SsgCluster>(source,target);
            }
        };
    }

    /**
     * Create an offline cluster
     */
    private SsgCluster createOfflineCluster( final String sourceClusterId,
                                             final String sourceClusterName,
                                             final Map<String, SsgCluster> clusters ) throws SaveException {
        final SsgCluster source;
        try {
            final EnterpriseFolder folder = enterpriseFolderManager.findRootFolder();
            if ( folder == null ) {
                throw new SaveException( "Root folder not found" );
            }

            // ensure cluster name is unique
            String uniqueName = sourceClusterName;
            final Set<String> clusterNames = new HashSet<String>( Functions.map( clusters.values(), new Functions.Unary<String, SsgCluster>() {
                @Override
                public String call( final SsgCluster ssgCluster ) {
                    return ssgCluster.getName();
                }
            } ) );

            for ( int i=0; i< 10000; i++ ) {
                if ( clusterNames.contains( uniqueName ) ) {
                    uniqueName = sourceClusterName + (i+1);
                } else {
                    break;
                }
            }

            source = clusterManager.create( uniqueName, sourceClusterId, folder );
        } catch ( FindException e ) {
            throw new SaveException( "Error accessing root folder", e );
        }
        return source;
    }

    public void setSuccessScript( final String script ) {
        this.successScript = script;
    }

    /**
     * Override to customize onSubmit behaviour.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    protected void onSubmit( final AjaxRequestTarget target ) {
    }

    private static final class UploadFormModel implements Serializable {
        private String label = "";

        public String getLabel() {
            return label;
        }

        public void setLabel( final String label ) {
            this.label = label==null ? "" : label;
        }
    }
}
