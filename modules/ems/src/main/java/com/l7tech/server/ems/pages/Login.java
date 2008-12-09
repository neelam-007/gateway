package com.l7tech.server.ems.pages;

import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.gateway.common.security.rbac.RequiredPermissionSet;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import com.l7tech.server.ems.EmsApplication;
import com.l7tech.server.ems.EmsSecurityManager;
import com.l7tech.server.ems.EmsSession;
import com.l7tech.server.ems.user.UserPropertyManager;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import com.l7tech.objectmodel.UpdateException;
import org.apache.wicket.spring.injection.annot.SpringBean;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Map;
import java.util.TimeZone;
import java.util.HashMap;

/**
 * Login page
 */
@RequiredPermissionSet()
@Administrative(licensed=false,authenticated=false)
public class Login extends WebPage {

    @SpringBean
    private EmsSecurityManager securityManager;

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
        YuiAjaxButton submitButton = new YuiAjaxButton("submit", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
            }

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedback);
            }
        };
        submitButton.setMarkupId("submit");
        form.add(submitButton);
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

        @Override
        public final void onSubmit() {
            try {
                if ( login( model.username, model.password ) ) {
                    setResponsePage( getApplication().getHomePage() );
                } else {
                    error( new StringResourceModel("message.invalid", this, null).getString() );
                }
            } catch ( EmsSecurityManager.NotLicensedException nle ) {
                error( new StringResourceModel("message.licenseinvalid", this, null).getString() );
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
        String dateformat = null;
        String datetimeformat = null;
        String zoneid = null;
        String preferredpage = null;

        boolean propsChanged = false;
        Map<String, String> props = new HashMap<String, String>();
        try {
            props = userPropertyManager.getUserProperties( user );
            String dateFormat = props.get("dateformat");
            String timeFormat = props.get("timeformat");

            if (dateFormat == null) {
                dateFormat = "formal";
                props.put("dateformat", "formal");
                propsChanged = true;
            }
            if (timeFormat == null) {
                timeFormat = "formal";
                props.put("timeformat", "formal");
                propsChanged = true;
            }

            dateformat = EmsApplication.getDateFormat(dateFormat);
            datetimeformat = EmsApplication.getDateTimeFormat(dateFormat, timeFormat);
            zoneid = props.get("timezone");
            preferredpage = props.get("homepage");
        } catch ( FindException fe ) {
            // use default format
        }

        if (dateformat == null) {
            dateformat = EmsApplication.DEFAULT_DATE_FORMAT;
            props.put("dateformat", "formal");
            propsChanged = true;

        }
        if (datetimeformat == null) {
            datetimeformat = EmsApplication.DEFAULT_DATETIME_FORMAT;
            props.put("timeformat", "formal");
            propsChanged = true;
        }
        if (!EmsApplication.isValidTimezoneId(zoneid)) {
            zoneid = TimeZone.getDefault().getID();
            props.put("timezone", EmsApplication.DEFAULT_SYSTEM_TIME_ZONE);
            propsChanged = true;
        }
        if (preferredpage == null) {
            preferredpage = EmsApplication.DEFAULT_HOME_PAGE;
            props.put("homepage", preferredpage);
            propsChanged = true;
        }

        session.setDateFormatPattern( dateformat );
        session.setDateTimeFormatPattern( datetimeformat );
        session.setTimeZoneId(zoneid);
        session.setPreferredPage(preferredpage);

        if (propsChanged) {
            try {
                userPropertyManager.saveUserProperties(user, props);
            } catch (UpdateException e) {
                // use the previous props
            }
        }
    }
}
