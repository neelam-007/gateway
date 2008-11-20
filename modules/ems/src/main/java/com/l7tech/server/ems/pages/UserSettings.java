package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.validation.validator.StringValidator;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;

import com.l7tech.server.ems.EmsSecurityManager;
import com.l7tech.server.ems.EmsApplication;
import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.NavigationModel;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Config;

import javax.servlet.http.HttpServletRequest;
import java.io.Serializable;
import java.util.Map;
import java.util.Collections;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * User settings web page 
 */
@NavigationPage(page="UserSettings",section="Settings",sectionIndex=200,pageUrl="UserSettings.html")
public class UserSettings extends EmsPage {

    //- PUBLIC

    /**
     * Create user settings page
     */
    public UserSettings() {
        Map<String,String> preferences = Collections.emptyMap();
        try {
            preferences = userPropertyManager.getUserProperties(getUser());
            logger.info("Loaded user preferences: " + preferences);
        } catch ( FindException fe ) {
            logger.log( Level.WARNING, "Error loading user preferences.", fe );       
        }

        final FeedbackPanel passwordFeedback = new FeedbackPanel("password.feedback");
        Form passwordForm = new PasswordForm("passwordForm");
        passwordFeedback.setFilter( new ContainerFeedbackMessageFilter(passwordForm) );
        add( passwordFeedback.setOutputMarkupId(true) );
        add( passwordForm );
        add( new YuiAjaxButton("password.submit", passwordForm){
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) { target.addComponent(passwordFeedback); }
            @Override
            protected void onError(AjaxRequestTarget target, Form form) { target.addComponent(passwordFeedback); }
        } );

        final FeedbackPanel accountFeedback = new FeedbackPanel("account.feedback");
        Form accountForm = new PreferencesForm("preferencesForm", preferences );
        accountFeedback.setFilter( new ContainerFeedbackMessageFilter(accountForm) );
        add( accountFeedback.setOutputMarkupId(true) );
        add( accountForm );
        add( new YuiAjaxButton("account.submit", accountForm){
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) { target.addComponent(accountFeedback); }
            @Override
            protected void onError(AjaxRequestTarget target, Form form) { target.addComponent(accountFeedback); }
        } );
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( UserSettings.class.getName() );

    @SpringBean
    private EmsSecurityManager securityManager;

    @SpringBean
    private UserPropertyManager userPropertyManager;

    @SpringBean
    private Config config;

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

    private boolean storePreferences( final Map<String,String> preferences ) {
        boolean updated = false;

        try {
            logger.info("Saving user preferences: " + preferences);
            userPropertyManager.saveUserProperties( getUser(), preferences );
            updated = true;

            String dateFormat = preferences.get("dateformat");
            String timeFormat = preferences.get("timeformat");

            getSession().setDateTimeFormatPattern( EmsApplication.getDateFormat(dateFormat, timeFormat) );
            getSession().setTimeZoneId( preferences.get("timezone") );
            getSession().setPreferredPage( preferences.get("homepage") );
        } catch ( UpdateException ue ) {
            logger.log( Level.WARNING, "Error saving user preferences.", ue );       
        }

        return updated;
    }

    /**
     * Model for the user settings password form
     */
    private static final class PasswordModel implements Serializable {
        String password;
        String newPassword;
        String newPasswordConfirm;
    }

    /**
     * Change password form
     */
    private final class PasswordForm extends Form {

        private final PasswordModel model = new PasswordModel();

        public PasswordForm(final String componentName) {
            super(componentName);

            PasswordTextField pass1 = new PasswordTextField("password", new PropertyModel(model, "password"));
            PasswordTextField pass2 = new PasswordTextField("newPassword", new PropertyModel(model, "newPassword"));
            PasswordTextField pass3 = new PasswordTextField("newPasswordConfirm", new PropertyModel(model, "newPasswordConfirm"));

            pass2.add( new StringValidator.LengthBetweenValidator(config.getIntProperty("password.length.min", 6), config.getIntProperty("password.length.max", 32) ));

            add(pass1.setRequired(true));
            add(pass2.setRequired(true));
            add(pass3.setRequired(true));

            add( new EqualPasswordInputValidator(pass2, pass3) );
        }

        @Override
        public final void onSubmit() {
            if ( model.newPassword.equals(model.newPasswordConfirm) ) {
                if ( changePassword( model.password, model.newPassword ) ) {
                    this.info( new StringResourceModel("password.message.updated", this, null).getString() );
                } else {
                    this.error( new StringResourceModel("password.message.updatefailed", this, null).getString() );
                }
            }
        }
    }

    private static final class DateChoiceRenderer implements IChoiceRenderer {
        private final boolean isDate;

        private DateChoiceRenderer( boolean isDate ) {
            this.isDate = isDate;
        }

        @Override
        public String getIdValue(Object o, int i) {
            return o.toString();
        }

        @Override
        public Object getDisplayValue(Object o) {
            return isDate ? EmsApplication.getDateFormatExample(o.toString()) : EmsApplication.getTimeFormatExample(o.toString());
        }
    }

    /**
     * Change password form
     */
    private final class PreferencesForm extends Form {

        public PreferencesForm(final String componentName, final Map<String,String> preferences) {
            super(componentName, new CompoundPropertyModel(preferences));

            List<String> zoneIds = Arrays.asList(TimeZone.getAvailableIDs());
            Collections.sort( zoneIds );

            final NavigationModel navigationModel = new NavigationModel("com.l7tech.server.ems.pages");

            add( new DropDownChoice( "timezone", zoneIds ) );
            add( new DropDownChoice( "dateformat", EmsApplication.getDateFormatkeys(), new DateChoiceRenderer(true) ) );
            add( new DropDownChoice( "timeformat", EmsApplication.getTimeFormatkeys(), new DateChoiceRenderer(false) ) );
            add( new GroupingDropDownChoice( "homepage", new ArrayList<String>(navigationModel.getNavigationPages()) ){
                @Override
                protected String getOptionGroupForChoice(final Object object) {
                    return  new StringResourceModel("section."+navigationModel.getNavigationSectionForPage(object.toString())+".label", this, null).getString();
                }

                @Override
                protected Object getOptionDisplayValue(final Object object) {
                    return  new StringResourceModel("page."+super.getOptionDisplayValue(object)+".label", this, null).getString();
                }
            } );
        }

        @Override
        @SuppressWarnings({"unchecked"})
        public final void onSubmit() {
            if ( storePreferences( (Map<String,String>) this.getModelObject() ) ) {
                this.info( new StringResourceModel("preferences.message.updated", this, null).getString() );
            } else {
                this.error( new StringResourceModel("preferences.message.error", this, null).getString() );
            }
        }
    }

}
