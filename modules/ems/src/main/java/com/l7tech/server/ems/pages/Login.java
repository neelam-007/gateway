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
import com.l7tech.server.ems.EmsSession;
import com.l7tech.server.ems.EmsApplication;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Map;
import java.util.TimeZone;

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

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private UserPropertyManager userPropertyManager;
    
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

        boolean success = securityManager.login( request.getSession(true), username, password );

        if ( success ) {
            User user = securityManager.getLoginInfo(request.getSession(true)).getUser();
            setUserPreferences( user, (EmsSession) getSession() );
        }

        return success;
    }

    /**
     *
     */
    private void setUserPreferences( final User user, final EmsSession session ) {
        String format = EmsApplication.DEFAULT_DATE_FORMAT;
        String zoneid = TimeZone.getDefault().getID();

        try {
            Map<String,String> props = userPropertyManager.getUserProperties( user );
            String dateFormat = props.get("dateformat");
            String timeFormat = props.get("timeformat");

            format = EmsApplication.getDateFormat(dateFormat, timeFormat);
            zoneid = props.get("timezone");
        } catch ( FindException fe ) {
            // use default format            
        }

        session.setDateTimeFormatPattern( format );
        session.setTimeZoneId(zoneid);
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
