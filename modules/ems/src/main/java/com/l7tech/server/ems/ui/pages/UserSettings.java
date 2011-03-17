package com.l7tech.server.ems.ui.pages;

import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import javax.inject.Inject;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.validation.validator.StringValidator;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.Page;

import com.l7tech.server.ems.ui.EsmSecurityManager;
import com.l7tech.server.ems.ui.EsmApplication;
import com.l7tech.server.ems.ui.NavigationPage;
import com.l7tech.server.ems.ui.NavigationModel;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.util.Config;
import com.l7tech.util.Functions;

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
public class UserSettings extends EsmStandardWebPage {

    //- PUBLIC

    /**
     * Create user settings page
     */
    public UserSettings() {
        Map<String,String> preferences = Collections.emptyMap();
        try {
            preferences = userPropertyManager.getUserProperties(getUser());
            logger.fine("Loaded user preferences: " + preferences);
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
        Form accountForm = new PreferencesForm("preferencesForm", populateDefaults(preferences) );
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

    @Inject
    private EsmSecurityManager securityManager;

    @Inject
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

    private Map<String,String> populateDefaults( Map<String,String> preferences ) {
        if ( !preferences.containsKey("dateformat") ) {
            preferences.put("dateformat", "formal");
        }
        if ( !preferences.containsKey("timeformat") ) {
            preferences.put("timeformat", "formal");
        }
        if ( !preferences.containsKey("timezone") ) {
            preferences.put("timezone", EsmApplication.DEFAULT_SYSTEM_TIME_ZONE);
        }
        if ( !preferences.containsKey("homepage") ) {
            preferences.put("homepage", EsmApplication.DEFAULT_HOME_PAGE);
        }
        
        return preferences;
    }

    private boolean storePreferences( final Map<String,String> preferences ) {
        boolean updated = false;

        try {
            populateDefaults( preferences );
            String dateFormat = preferences.get("dateformat");
            String timeFormat = preferences.get("timeformat");
            String zoneId = preferences.get("timezone");
            String preferredPage = preferences.get("homepage");

            if (dateFormat == null) {
                dateFormat = "formal";
                preferences.put("dateformat", "formal");
            }
            if (timeFormat == null) {
                timeFormat = "formal";
                preferences.put("timeformat", "formal");
            }
            if (!EsmApplication.isValidTimezoneId(zoneId)) {
                zoneId = TimeZone.getDefault().getID();
                preferences.put("timezone", EsmApplication.DEFAULT_SYSTEM_TIME_ZONE);
            }
            if (preferredPage == null) {
                preferences.put("homepage", EsmApplication.DEFAULT_HOME_PAGE);
            }

            getSession().setDateFormatPattern( EsmApplication.getDateFormat(dateFormat) );
            getSession().setDateTimeFormatPattern( EsmApplication.getDateTimeFormat(dateFormat, timeFormat) );
            getSession().setTimeZoneId( zoneId );
            getSession().setPreferredPage( preferredPage );

            logger.fine("Saving user preferences: " + preferences);
            userPropertyManager.saveUserProperties( getUser(), preferences );
            updated = true;
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
    private final class PasswordForm extends Form<PasswordModel> {

        private final PasswordModel model = new PasswordModel();

        public PasswordForm(final String componentName) {
            super(componentName);

            PasswordTextField pass1 = new PasswordTextField("password", new PropertyModel<String>(model, "password"));
            PasswordTextField pass2 = new PasswordTextField("newPassword", new PropertyModel<String>(model, "newPassword"));
            PasswordTextField pass3 = new PasswordTextField("newPasswordConfirm", new PropertyModel<String>(model, "newPasswordConfirm"));

            pass2.add( new StringValidator.LengthBetweenValidator(config.getIntProperty("password.length.min", 6), config.getIntProperty("password.length.max", 32) ));

            add(pass1.setRequired(true));
            add(pass2.setRequired(true));
            add(pass3.setRequired(true));

            add( new EqualPasswordInputValidator(pass2, pass3) );
            add( new DifferentPasswordInputValidator(pass1, pass2, pass3) );
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

    private static final class DateChoiceRenderer implements IChoiceRenderer<String> {
        private final boolean isDate;

        private DateChoiceRenderer( boolean isDate ) {
            this.isDate = isDate;
        }

        @Override
        public String getIdValue(String o, int i) {
            return o;
        }

        @Override
        public Object getDisplayValue(String o) {
            return isDate ? EsmApplication.getDateFormatExample(o) : EsmApplication.getTimeFormatExample(o);
        }
    }

    /**
     * Change user interface preference form
     */
    private final class PreferencesForm extends Form<Map<String,String>> {

        public PreferencesForm(final String componentName, final Map<String,String> preferences) {
            super(componentName, new CompoundPropertyModel<Map<String,String>>(preferences));

            List<String> zoneIds = new ArrayList<String>(Arrays.asList(TimeZone.getAvailableIDs()));
            Collections.sort( zoneIds );
            zoneIds.add(0, EsmApplication.DEFAULT_SYSTEM_TIME_ZONE);

            final NavigationModel navigationModel = new NavigationModel("com.l7tech.server.ems.ui.pages", new Functions.Unary<Boolean,Class<? extends Page>>(){
                @Override
                public Boolean call(Class<? extends Page> aClass) {
                    return securityManager.hasPermission( aClass );
                }
            });
            List<String> homePages = new ArrayList<String>(navigationModel.getNavigationPages());
            homePages.add(EsmApplication.LAST_VISITED_PAGE);

            add( new DropDownChoice<String>( "timezone", zoneIds ) );
            add( new DropDownChoice<String>( "dateformat", EsmApplication.getDateFormatkeys(), new DateChoiceRenderer(true) ) );
            add( new DropDownChoice<String>( "timeformat", EsmApplication.getTimeFormatkeys(), new DateChoiceRenderer(false) ) );
            add( new GroupingDropDownChoice<String>( "homepage", homePages ) {
                @Override
                protected String getOptionGroupForChoice(final Object object) {
                    if (EsmApplication.LAST_VISITED_PAGE.equals(object)) {
                        return "Settings"; // set the last visited page in Settings group.
                    } else {
                        return  new StringResourceModel("section."+navigationModel.getNavigationSectionForPage(object.toString())+".label", this, null).getString();
                    }
                }

                @Override
                protected Object getOptionDisplayValue(final String object) {
                    if (EsmApplication.LAST_VISITED_PAGE.equals(object)) {
                        return object;
                    } else {
                        return  new StringResourceModel("page."+super.getOptionDisplayValue(object)+".label", this, null).getString();
                    }
                }
            } );
        }

        @Override
        @SuppressWarnings({"unchecked"})
        public final void onSubmit() {
            if ( storePreferences( this.getModelObject() ) ) {
                this.info( new StringResourceModel("preferences.message.updated", this, null).getString() );
            } else {
                this.error( new StringResourceModel("preferences.message.error", this, null).getString() );
            }
        }
    }

    /**
     * A validator class to validate if the new password and retype new password must be different from the old password.
     * If all of them are the same, then an error will be sent to the feedback panel in the form.
     */
    private final class DifferentPasswordInputValidator extends AbstractFormValidator {

        // Form components to be validated.
        private final FormComponent[] formComponents;

        /**
         * Constructor to specify three password text fields to check if the new password is different from the old password.
         *
         * Note: the reason of specifying two new passwords is to check if two new passwords are the same.  If they are not
         * the same, the validator will not make a validation and report any error.
         *
         * @param fc1 The old password
         * @param fc2 The new password
         * @param fc3 The retype new password.
         */
        public DifferentPasswordInputValidator(FormComponent fc1, FormComponent fc2, FormComponent fc3) {
            if (fc1 == null || fc2 == null || fc3 ==null) {
                throw new IllegalArgumentException("FormComponent cannot be null");
            }
            formComponents = new FormComponent[] {fc1, fc2, fc3};
        }

        @Override
        public FormComponent[] getDependentFormComponents() {
            return formComponents;
        }

        @Override
        public void validate(Form<?> form) {
            String oldPassword = formComponents[0].getInput();
            String newPassword = formComponents[1].getInput();
            String retypeNewPassword = formComponents[2].getInput();

            // Precheck if these passwords are null.
            if (oldPassword == null || newPassword == null || retypeNewPassword == null) {
                return;
            }

            // If all of them are the same, report an error.
            if (newPassword.equals(retypeNewPassword) && oldPassword.equals(newPassword)) {
                String errorMessage = new StringResourceModel(resourceKey(), UserSettings.this, null).getString();
                form.error(errorMessage, variablesMap());
            }
        }

        @Override
        protected String resourceKey() {
            return "DifferentPasswordInputValidator.message";
        }

        @Override
        protected Map<String, Object> variablesMap() {
            Map<String, Object> map = super.variablesMap();
            map.put("OldPasswordFieldName", new StringResourceModel("passwordForm.password", UserSettings.this, null).getString());
            map.put("NewPasswordFieldName", new StringResourceModel("passwordForm.newPassword", UserSettings.this, null).getString());

            return map;
        }
    }
}
