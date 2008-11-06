package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import com.l7tech.server.ems.SetupManager;
import com.l7tech.server.ems.EmsSecurityManager;
import com.l7tech.server.ems.EmsSession;
import com.l7tech.server.ems.EmsApplication;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Map;
import java.util.TimeZone;

/**
 * Login page
 */
public class Login extends WebPage {

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

        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        add( feedback.setOutputMarkupId(true) );
        LoginForm form = new LoginForm("loginForm");
        form.add(
            new YuiAjaxButton("submit", form){
                protected void onSubmit(AjaxRequestTarget target, Form form) {}
                @Override
                protected void onError(AjaxRequestTarget target, Form form) {
                    target.addComponent(feedback);
                }
            }
        );
        add(form);

        EmptyPanel empty = new EmptyPanel("empty");
        empty.add( HeaderContributor.forCss( EmsPage.RES_CSS_SKIN ) );
        add(empty);
    }

    @Override
    public boolean isVersioned() {
        return false;
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
        String format = null;
        String zoneid = null;

        try {
            Map<String,String> props = userPropertyManager.getUserProperties( user );
            String dateFormat = props.get("dateformat");
            String timeFormat = props.get("timeformat");

            format = EmsApplication.getDateFormat(dateFormat, timeFormat);
            zoneid = props.get("timezone");
        } catch ( FindException fe ) {
            // use default format            
        }

        if ( format == null ) format = EmsApplication.DEFAULT_DATE_FORMAT;
        if ( zoneid == null ) zoneid = TimeZone.getDefault().getID();

        session.setDateTimeFormatPattern( format );
        session.setTimeZoneId(zoneid);
    }
}
