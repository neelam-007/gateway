package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import com.l7tech.server.ems.SetupManager;
import com.l7tech.server.ems.EmsSecurityManager;
import com.l7tech.server.ems.SetupException;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Login page
 */
public class Login extends WebPage {
    private static final Logger logger = Logger.getLogger(Login.class.getName());

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private SetupManager setupManager;

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private EmsSecurityManager securityManager;

    /**
     * Create login page
     */
    public Login() {
        ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
        HttpServletRequest request = servletWebRequest.getHttpServletRequest();

        // Log out on access
        if ( !request.getMethod().equals("POST") ) {
            securityManager.logout( request.getSession(true) );
        }

        if ( !isSetup() ) {
            // If not configured send the user to the setup page
            setResponsePage(new Setup());
        } else {
            add( new FeedbackPanel("feedback") );
            add( new LoginForm("loginForm") );
        }
    }

    /**
     * Model for login form
     */
    public final class LoginModel implements Serializable {
        String username;
        String password;
    }

    /**
     * Login form
     */
    public final class LoginForm extends Form {
        private final LoginModel model = new LoginModel();

        public LoginForm(final String componentName) {
            super(componentName);

            add(new RequiredTextField("username", new PropertyModel(model, "username")));
            add(new PasswordTextField("password", new PropertyModel(model, "password")).setRequired(true));
        }        

        public final void onSubmit() {
            if ( login( model.username, model.password ) ) {
                setResponsePage(getApplication().getHomePage());
            } else {
                error( new StringResourceModel("message.invalid", this, null).getString() );
            }
        }
    }

    /**
     * Perform login
     */
    private boolean login( final String username, final String password ) {
        ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
        HttpServletRequest request = servletWebRequest.getHttpServletRequest();

        return securityManager.login( request.getSession(true), username, password );
    }

    /**
     * Check if Ems server is configured 
     */
    private boolean isSetup() {
        boolean setup = false;

        try {
            setup = setupManager.isSetupPerformed();
        } catch ( SetupException e ) {
            logger.log( Level.WARNING, "Error when determining if configured.", e );
        }

        return setup;
    }
}
