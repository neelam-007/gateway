package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.io.Serializable;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.l7tech.server.ems.EmsAccountManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.ExceptionUtils;

/**
 * Page for editing a user
 */
public class EnterpriseUsersResetPasswordPanel extends Panel {

    //- PUBLIC

    public EnterpriseUsersResetPasswordPanel( final String id, final String username ) {
        super(id);

        //
        PasswordResetForm passwordForm = new PasswordResetForm("resetPasswordForm", buildUserModel(username));
        add( passwordForm );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EnterpriseUsersResetPasswordPanel.class.getName() );

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private EmsAccountManager emsAccountManager;

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
    public final class UserModel implements Serializable {
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
    public final class PasswordResetForm extends Form {
        public PasswordResetForm( final String componentName, final UserModel userModel ) {
            super(componentName, new CompoundPropertyModel(userModel));

            PasswordTextField pass1 = new PasswordTextField("password");
            PasswordTextField pass2 = new PasswordTextField("passwordConfirm");

            pass1.add( new StringValidator.LengthBetweenValidator(6, 128) );

            add(new Label("userId"));
            add(pass1.setRequired(true));
            add(pass2.setRequired(true));

            add( new EqualPasswordInputValidator(pass1, pass2) );
        }

        public final void onSubmit() {
            updateUser( (UserModel)getModelObject(), this );
        }
    }
}