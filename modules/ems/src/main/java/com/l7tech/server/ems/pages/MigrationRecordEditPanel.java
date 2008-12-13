package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxEventBehavior;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.Component;
import org.apache.wicket.validation.validator.PatternValidator;
import org.apache.wicket.validation.validator.StringValidator;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collections;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.io.IOException;
import java.io.Serializable;

import com.l7tech.server.ems.SetupManager;
import com.l7tech.server.ems.SetupException;
import com.l7tech.server.ems.migration.MigrationRecord;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ExceptionUtils;

/**
 * Panel for edit of migration record properties
 */
public class MigrationRecordEditPanel extends Panel {

    public MigrationRecordEditPanel( final String id, final MigrationRecord record ) {
        super( id );

        final FeedbackPanel feedback = new FeedbackPanel("feedback");

        final TextField name = new TextField("name");
        name.add( new StringValidator.LengthBetweenValidator(0, 32) );

        Form editForm = new Form("editForm", new CompoundPropertyModel(record)){
            @Override
            protected void onSubmit() {
                MigrationRecordEditPanel.this.onSubmit();
            }
        };

        editForm.add( feedback );
        editForm.add( name );

        add( editForm );
    }

    /**
     * Override to customize onSubmit behaviour.
     */
    protected void onSubmit() {
    }
}