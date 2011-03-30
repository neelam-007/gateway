package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.validation.validator.StringValidator;

import java.io.Serializable;

/**
 * Panel for migration artifact download options.
 */
public class PolicyMigrationDownloadPanel extends Panel {

    public PolicyMigrationDownloadPanel( final String id, final DownloadFormModel model ) {
        super( id );

        final FeedbackPanel feedback = new FeedbackPanel("feedback");

        final PasswordTextField password = new PasswordTextField("password");
        password.add( new StringValidator.LengthBetweenValidator( 0, 256 ) );

        final PasswordTextField passwordConfirmation = new PasswordTextField("passwordConfirmation");
        passwordConfirmation.add( new StringValidator.LengthBetweenValidator( 0, 256 ) );

        final AjaxCheckBox encryptedCheckBox = new AjaxCheckBox("encrypted"){
            @Override
            protected void onUpdate( final AjaxRequestTarget target ) {
                final boolean enablePasswordEntry = model.isEncrypted();
                password.setEnabled( enablePasswordEntry );
                passwordConfirmation.setEnabled( enablePasswordEntry );
                target.addComponent( password );
                target.addComponent( passwordConfirmation );
            }
        };

        final Form<DownloadFormModel> downloadForm = new Form<DownloadFormModel>("downloadForm", new CompoundPropertyModel<DownloadFormModel>(model));
        downloadForm.add( new EqualPasswordInputValidator( password, passwordConfirmation ) );

        downloadForm.add( feedback.setOutputMarkupId(true) );
        downloadForm.add( encryptedCheckBox );
        downloadForm.add( password.setOutputMarkupId( true ) );
        downloadForm.add( passwordConfirmation.setOutputMarkupId( true ) );

        add(downloadForm);
    }

    public static final class DownloadFormModel implements Serializable {
        private boolean encrypted = true;
        private String password = "";
        private String passwordConfirmation = "";

        public boolean isEncrypted() {
            return encrypted;
        }

        public void setEncrypted( final boolean encrypted ) {
            this.encrypted = encrypted;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword( final String password ) {
            this.password = password;
        }

        public String getPasswordConfirmation() {
            return passwordConfirmation;
        }

        public void setPasswordConfirmation( final String passwordConfirmation ) {
            this.passwordConfirmation = passwordConfirmation;
        }
    }
}
