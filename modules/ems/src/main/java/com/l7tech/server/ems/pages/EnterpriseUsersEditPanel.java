package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
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
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.ExceptionUtils;

/**
 * Page for editing a user
 */
public class EnterpriseUsersEditPanel extends Panel {

    //- PUBLIC

    public EnterpriseUsersEditPanel( final String id, final String username ) {
        super(id);
        
        //
        UserForm editForm = new UserForm("editUserForm", buildUserModel(username));
        add( editForm );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EnterpriseUsersEditPanel.class.getName() );

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private EmsAccountManager emsAccountManager;

    private UserModel buildUserModel( final String username ) {
        UserModel model = null;
        try {

            InternalUser user = emsAccountManager.findByLogin( username );
            if ( user != null ) {
                model = new UserModel( user.getLogin() );
                model.email = user.getEmail();
                model.firstName = user.getFirstName();
                model.lastName = user.getLastName();
                model.description = user.getDescription();
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
                user.setEmail( model.email );
                user.setFirstName( model.firstName );
                user.setLastName( model.lastName );
                user.setDescription( model.description );

                emsAccountManager.update( user );
            } else {
                form.error( new StringResourceModel("message.deleted", this, null, new Object[]{ model.userId } ).getString() );
            }
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
        String email;
        String firstName;
        String lastName;
        String description;

        UserModel( final String username ) {
            this.userId = username;
        }
    }

    /**
     * User form
     */
    public final class UserForm extends Form {
        public UserForm( final String componentName, final UserModel userModel ) {
            super(componentName, new CompoundPropertyModel(userModel));

            add(new Label("userId"));
            add(new TextField("email").add(new StringValidator.LengthBetweenValidator(1, 128)));
            add(new TextField("lastName").add(new StringValidator.LengthBetweenValidator(1, 32)));
            add(new TextField("firstName").add(new StringValidator.LengthBetweenValidator(1, 32)));
            add(new TextField("description").add(new StringValidator.LengthBetweenValidator(1, 255)));
        }

        public final void onSubmit() {
            updateUser( (UserModel) getModelObject(), this );
        }
    }
}
