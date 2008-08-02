package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.validation.EqualPasswordInputValidator;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.validation.validator.StringValidator;
import org.xml.sax.SAXException;

import java.io.Serializable;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.l7tech.util.SyspropUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.ems.SetupManager;
import com.l7tech.server.ems.EmsSecurityManager;
import com.l7tech.server.ems.SetupException;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.objectmodel.UpdateException;

import javax.servlet.http.HttpServletRequest;

/**
 * Page for Setup
 */
public class Setup extends WebPage {

    private static final int MAX_LICENSE_FILE_UPLOAD_BYTES = SyspropUtil.getInteger("com.l7tech.ems.licenseFile.maxBytes", 1024 * 500);
    private static final Logger logger = Logger.getLogger(Setup.class.getName());

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private SetupManager setupManager;

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean
    private EmsSecurityManager securityManager;

    /**
     * Create setup page
     */
    public Setup() {
        add( new FeedbackPanel("feedback") );
        add(new SetupForm("setupForm"));
    }

    /**
     * Perform initial setup 
     */
    private boolean initialSetup(final String license,
                                 final String username,
                                 final String password) {
        boolean setup = false;

        try {
            // setup
            setupManager.performInitialSetup(license, username, password);

            // log in the user
            ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
            HttpServletRequest request = servletWebRequest.getHttpServletRequest();
            setup = securityManager.login( request.getSession(true), username, password );
        } catch (SetupException e) {
            error(ExceptionUtils.getMessage(e));
            logger.log( Level.WARNING, "Error installing new license", e );
        }

        return setup;
    }

    /**
     * Model for setup form
     */
    public final class SetupModel implements Serializable {
        String username;
        String password;
        String passwordConfirm;
    }

    /**
     * Setup form
     */
    public final class SetupForm extends Form {

        private final SetupModel model = new SetupModel();
        private final FileUploadField fileUpload = new FileUploadField("license");

        public SetupForm(final String componentName) {
            super(componentName);

            PasswordTextField pass1 = new PasswordTextField("password", new PropertyModel(model, "password"));
            PasswordTextField pass2 = new PasswordTextField("passwordConfirm", new PropertyModel(model, "passwordConfirm"));

            pass1.add( new StringValidator.LengthBetweenValidator(6, 256) );

            add(fileUpload.setRequired(true));
            add(new RequiredTextField("username", new PropertyModel(model, "username")).add(new StringValidator.LengthBetweenValidator(3, 128)));
            add(pass1.setRequired(true));
            add(pass2.setRequired(true));

            add( new EqualPasswordInputValidator(pass1, pass2) );

            setMaxSize(Bytes.bytes(MAX_LICENSE_FILE_UPLOAD_BYTES));
        }

        public final void onSubmit() {
            String license = null;

            final FileUpload upload = fileUpload.getFileUpload();
            if ( upload != null ) {
                try {
                    license = XmlUtil.nodeToString(XmlUtil.parse(upload.getInputStream(), false));
                } catch ( IOException e ) {
                    error( ExceptionUtils.getMessage(e) );
                    logger.log( Level.WARNING, "Error accessing license '"+ExceptionUtils.getMessage(e)+"'." );
                } catch ( SAXException e ) {
                    error( ExceptionUtils.getMessage(e) );
                    logger.log( Level.WARNING, "Error parsing license '"+ExceptionUtils.getMessage(e)+"'." );
                } finally {
                    upload.closeStreams();
                }
            }

            if ( model.password.equals(model.passwordConfirm) ) {
                if ( initialSetup( license, model.username, model.password ) ) {
                    // On success display the system settings page
                    setResponsePage(new SystemSettings());
                }
            }
        }
    }
}
