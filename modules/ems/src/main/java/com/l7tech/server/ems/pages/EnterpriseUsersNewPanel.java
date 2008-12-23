package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.validation.validator.StringValidator;
import org.apache.wicket.validation.validator.PatternValidator;
import org.apache.wicket.validation.validator.EmailAddressValidator;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.Component;

import java.io.Serializable;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collection;

import com.l7tech.server.ems.EsmAccountManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.DuplicateObjectException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Config;

/**
 * Page for new user creation
 */
public class EnterpriseUsersNewPanel extends Panel {

    //- PUBLIC

    public EnterpriseUsersNewPanel( final String id, final Collection<? extends Component> refreshComponents ) {
        super( id );

        //
        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        UserForm userForm = new UserForm("newUserForm");
        add( feedback.setOutputMarkupId(true) );
        add( userForm );
        add( new YuiAjaxButton( "submit", userForm ){
            @Override
            protected void onSubmit( final AjaxRequestTarget target, final Form form ) {
                target.addComponent( feedback );
                for ( Component component : refreshComponents ) {
                    target.addComponent( component );
                }
            }
            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent( feedback );
            }
        } );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EnterpriseUsersNewPanel.class.getName() );

    @SpringBean
    private EsmAccountManager emsAccountManager;

    @SpringBean
    private Config config;

    /**
     * Model for user form
     */
    public final class UserModel implements Serializable {
        String userId;
        String firstName;
        String lastName;
        String email;
        String description;
        String password;
        String passwordConfirm;
    }

    /**
     * User form
     */
    public final class UserForm extends Form {

        public UserForm(final String componentName) {
            super(componentName, new CompoundPropertyModel(new UserModel()));

            PasswordTextField pass1 = new PasswordTextField("password");
            PasswordTextField pass2 = new PasswordTextField("passwordConfirm");

            pass1.add( new StringValidator.LengthBetweenValidator(config.getIntProperty("password.length.min", 6), config.getIntProperty("password.length.max", 32)) );

            add(new RequiredTextField("userId").add(new StringValidator.LengthBetweenValidator(3, 128)).add(new PatternValidator("^[^#,+\"\\\\<>;]{3,128}$")));
            add(new TextField("email").add(new StringValidator.LengthBetweenValidator(1, 128)).add(EmailAddressValidator.getInstance()));
            add(new TextField("lastName").add(new StringValidator.LengthBetweenValidator(1, 32)));
            add(new TextField("firstName").add(new StringValidator.LengthBetweenValidator(1, 32)));
            add(new TextField("description").add(new StringValidator.LengthBetweenValidator(1, 255)));
            add(pass1.setRequired(true));
            add(pass2.setRequired(true));

            add( new EqualPasswordInputValidator(pass1, pass2) );
        }

        @Override
        public final void onSubmit() {
            UserModel model = (UserModel) getModelObject();
            try {
                InternalUser user = new InternalUser();
                user.setLogin( model.userId );
                user.setEmail( model.email );
                user.setName( model.userId );
                user.setFirstName( model.firstName );
                user.setLastName( model.lastName );
                user.setCleartextPassword( model.password );
                user.setDescription( model.description );

                emsAccountManager.save( user );
                setResponsePage( EnterpriseUsers.class );
            } catch (DuplicateObjectException se) {
                // username already taken so warn the user
                error( new StringResourceModel("message.duplicate", this, null, new Object[]{ model.userId } ).getString() );
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
