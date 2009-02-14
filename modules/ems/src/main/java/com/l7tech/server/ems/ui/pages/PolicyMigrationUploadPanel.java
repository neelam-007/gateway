package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.validation.validator.StringValidator;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;
import java.util.HashSet;
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

    @SpringBean
    private SsgClusterManager clusterManager;

    @SpringBean
    private MigrationRecordManager migrationRecordManager;

    public PolicyMigrationUploadPanel( final String id ) {
        super( id );

        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        final FileUploadField archive = new FileUploadField("archive");

        TextField label = new TextField("label");
        label.add( new StringValidator.LengthBetweenValidator(0, 32) );

        final UploadFormModel model = new UploadFormModel();
        final Form archiveForm = new YuiFileUploadForm("archiveForm", new CompoundPropertyModel(model)){
            @Override
            @SuppressWarnings({"UnusedDeclaration"})
             protected void onError( final AjaxRequestTarget target ) {
                target.addComponent( feedback );
            }

            @Override
            @SuppressWarnings({"UnusedDeclaration", "SuspiciousSystemArraycopy"})
            protected void onSubmit( final AjaxRequestTarget target ) {
                logger.fine("Processing SSL update.");

                boolean success = false;
                FileUpload upload = null;
                try {
                    upload = archive.getFileUpload();
                    if ( upload != null ) {
                        byte[] data = MigrationArtifactResource.unzip( upload.getInputStream() );

                        if ( data != null ) {
                            Collection<SsgCluster> validClusters = clusterManager.findAll();
                            Collection<String> validClusterGuids = new HashSet<String>();
                            for(SsgCluster cluster : validClusters) {
                                validClusterGuids.add(cluster.getGuid());
                            }
                            migrationRecordManager.create( model.getLabel(), data, validClusterGuids);
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
                    logger.log( Level.WARNING, "Error processing migration archive upload.", ome );
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
            this.label = label;
        }
    }
}
