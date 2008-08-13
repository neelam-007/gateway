package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.validation.validator.StringValidator;
import com.l7tech.server.ems.EmsSecurityManager;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;

/**
 * User settings web page 
 */
public class UserSettings extends EmsPage {

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private EmsSecurityManager securityManager;

    /**
     * Create user settings page
     */
    public UserSettings() {
        add( new FeedbackPanel("feedback") );
        add( new PasswordForm("passwordForm") );
    }

    /**
     * Change user passoword
     */
    private boolean changePassword(final String password,
                                   final String newPassword) {
        boolean changed;

        // log in the user
        ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
        HttpServletRequest request = servletWebRequest.getHttpServletRequest();
        changed = securityManager.changePassword( request.getSession(true), password, newPassword );

        return changed;
    }

    /**
     * Model for the user settings password form
     */
    public final class PasswordModel implements Serializable {
        String password;
        String newPassword;
        String newPasswordConfirm;
    }

    /**
     * Change password form
     */
    public final class PasswordForm extends Form {

        private final PasswordModel model = new PasswordModel();

        public PasswordForm(final String componentName) {
            super(componentName);

            PasswordTextField pass1 = new PasswordTextField("password", new PropertyModel(model, "password"));
            PasswordTextField pass2 = new PasswordTextField("newPassword", new PropertyModel(model, "newPassword"));
            PasswordTextField pass3 = new PasswordTextField("newPasswordConfirm", new PropertyModel(model, "newPasswordConfirm"));

            pass2.add( new StringValidator.LengthBetweenValidator(6, 256) );

            add(pass1.setRequired(true));
            add(pass2.setRequired(true));
            add(pass3.setRequired(true));

            add( new EqualPasswordInputValidator(pass2, pass3) );
        }

        public final void onSubmit() {
            if ( model.newPassword.equals(model.newPasswordConfirm) ) {
                if ( changePassword( model.password, model.newPassword ) ) {
                    info( new StringResourceModel("password.message.updated", this, null).getString() );
                }
            }
        }
    }}
