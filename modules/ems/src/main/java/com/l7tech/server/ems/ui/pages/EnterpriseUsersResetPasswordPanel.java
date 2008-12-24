package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.ajax.AjaxRequestTarget;

import java.io.Serializable;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.l7tech.server.ems.EsmAccountManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Config;

/**
 * Page for editing a user
 */
public class EnterpriseUsersResetPasswordPanel extends Panel {

    //- PUBLIC

    public EnterpriseUsersResetPasswordPanel( final String id, final String username ) {
        super(id);

        //
        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        add( feedback.setOutputMarkupId(true) );

        PasswordResetForm passwordForm = new PasswordResetForm("resetPasswordForm", buildUserModel(username));
        add( passwordForm );

        add( new YuiAjaxButton( "submit", passwordForm ){
            @Override
            protected void onSubmit( final AjaxRequestTarget target, final Form form ) {
                target.addComponent( feedback );
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent( feedback );
            }
        });
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EnterpriseUsersResetPasswordPanel.class.getName() );

    @SpringBean
    private EsmAccountManager emsAccountManager;

    @SpringBean
    private Config config;

    private UserModel buildUserModel( final String username ) {
        UserModel model = null;
        try {
            InternalUser user = emsAccountManager.findByLogin( username );
            if ( user != null ) {
                model = new UserModel( user.getLogin() );
            } else {
                error( new StringResourceModel("message.deleted", this, null, new Object[]{ username } ).getString() );
            }
        } catch (FindException fe) {
            error( ExceptionUtils.getMessage( fe ) );
            logger.log( Level.WARNING, "Error creating user", fe );
        }

        if ( model == null ) {
            model = new UserModel("");
        }

        return model;
    }

    private void updateUser( final UserModel model, final Form form ) {
        try {
            InternalUser user = emsAccountManager.findByLogin( model.userId );
            if ( user != null ) {
                user.setCleartextPassword( model.password );
                emsAccountManager.update( user );
                form.info( new StringResourceModel("message.reset", this, null, new Object[]{ model.userId } ).getString() );
            } else {
                form.error( new StringResourceModel("message.deleted", this, null, new Object[]{ model.userId } ).getString() );
            }
        } catch (InvalidPasswordException se) {
            // password is not acceptable
            form.error( ExceptionUtils.getMessage( se ) );
        } catch (FindException fe) {
            form.error( ExceptionUtils.getMessage( fe ) );
            logger.log( Level.WARNING, "Error creating user", fe );
        } catch (UpdateException ue) {
            form.error( ExceptionUtils.getMessage( ue ) );
            logger.log( Level.WARNING, "Error saving user", ue );
        }

    }

    /**
     * Model for user form
     */
    private static final class UserModel implements Serializable {
        final String userId;
        String password;
        String passwordConfirm;

        UserModel( final String username ) {
            this.userId = username;
        }
    }

    /**
     * Password form
     */
    private  final class PasswordResetForm extends Form {
        public PasswordResetForm( final String componentName, final UserModel userModel ) {
            super(componentName, new CompoundPropertyModel(userModel));

            PasswordTextField pass1 = new PasswordTextField("password");
            PasswordTextField pass2 = new PasswordTextField("passwordConfirm");

            pass1.add( new StringValidator.LengthBetweenValidator(config.getIntProperty("password.length.min", 6), config.getIntProperty("password.length.max", 32)) );

            add(pass1.setRequired(true));
            add(pass2.setRequired(true));

            add( new EqualPasswordInputValidator(pass1, pass2) );
        }

        @Override
        public final void onSubmit() {
            updateUser( (UserModel)getModelObject(), this );
        }
    }
}