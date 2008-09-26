package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.apache.wicket.spring.injection.annot.SpringBean;

import java.io.Serializable;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.l7tech.server.ems.EmsAccountManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.util.ExceptionUtils;

/**
 * Page for new user creation
 */
public class EnterpriseUsersNewPanel extends Panel {

    //- PUBLIC

    public EnterpriseUsersNewPanel( final String id ) {
        super( id );

        //
        add( new UserForm("newUserForm") );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EnterpriseUsersNewPanel.class.getName() );

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private EmsAccountManager emsAccountManager;

    /**
     * Model for user form
     */
    public final class UserModel implements Serializable {
        String username;
        String firstName;
        String lastName;
        String password;
        String passwordConfirm;
    }

    /**
     * User form
     */
    public final class UserForm extends Form {

        private final UserModel model = new UserModel();

        public UserForm(final String componentName) {
            super(componentName);

            PasswordTextField pass1 = new PasswordTextField("password", new PropertyModel(model, "password"));
            PasswordTextField pass2 = new PasswordTextField("passwordConfirm", new PropertyModel(model, "passwordConfirm"));

            pass1.add( new StringValidator.LengthBetweenValidator(6, 256) );

            add(new RequiredTextField("userId", new PropertyModel(model, "username")).add(new StringValidator.LengthBetweenValidator(3, 128)));
            add(new TextField("lastName", new PropertyModel(model, "lastName")).add(new StringValidator.LengthBetweenValidator(1, 128)));
            add(new TextField("firstName", new PropertyModel(model, "firstName")).add(new StringValidator.LengthBetweenValidator(1, 128)));
            add(pass1.setRequired(true));
            add(pass2.setRequired(true));

            add( new EqualPasswordInputValidator(pass1, pass2) );
        }

        public final void onSubmit() {
            try {
                InternalUser user = new InternalUser();
                user.setLogin( model.username );
                user.setName( model.username );
                user.setFirstName( model.firstName );
                user.setLastName( model.lastName );
                user.setCleartextPassword( model.password );

                emsAccountManager.save( user );
                setResponsePage( EnterpriseUsers.class );
            } catch (DuplicateObjectException se) {
                // username already taken so warn the user
                error( new StringResourceModel("message.duplicate", this, null, new Object[]{ model.username } ).getString() );
            } catch (InvalidPasswordException se) {
                // password is not acceptable
                error( ExceptionUtils.getMessage( se ) );
            } catch (SaveException se) {
                error( ExceptionUtils.getMessage( se ) );
                logger.log( Level.WARNING, "Error saving user", se );
            }
        }
    }
}
