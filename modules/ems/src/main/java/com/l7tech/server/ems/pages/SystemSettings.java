package com.l7tech.server.ems.pages;

import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.lang.Bytes;
import org.xml.sax.SAXException;
import com.l7tech.util.SyspropUtil;
import com.l7tech.util.TextUtils;
import com.l7tech.util.DateUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.UpdatableLicenseManager;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.License;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.objectmodel.UpdateException;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;

/**
 * Page for system settings
 */
public class SystemSettings extends WebPage {

    private static final int MAX_LICENSE_FILE_UPLOAD_BYTES = SyspropUtil.getInteger("com.l7tech.ems.licenseFile.maxBytes", 1024 * 500);
    private static final Logger logger = Logger.getLogger(SystemSettings.class.getName());

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean(name="licenseManager")
    private UpdatableLicenseManager licenseManager;

    /**
     * Create system settings page 
     */
    public SystemSettings() {
        LicenseModel licenseModel = new LicenseModel();

        //
        add( new FeedbackPanel("feedback") );

        // labels
        add(new Label("licenseStatus", new StringResourceModel("license.signaturevalid.${valid}", this, new Model(licenseModel))));
        add(new Label("licenseId", new PropertyModel(licenseModel, "id")));
        add(new Label("licenseDescription", new PropertyModel(licenseModel, "description")));
        add(new Label("licenseAttributes", new PropertyModel(licenseModel, "attributes")));
        add(new Label("licenseIssuer", new StringResourceModel("license.issuer.${issuerAvailable}", this, new Model(licenseModel))));
        add(new Label("licensee", new PropertyModel(licenseModel, "licensee")));
        add(new Label("licenseContact", new PropertyModel(licenseModel, "contact")));
        add(new Label("licenseStartDate", new PropertyModel(licenseModel, "startDate")));
        add(new Label("licenseEndDate",  new PropertyModel(licenseModel, "endDate")));

        // text area
        add(new TextArea("licenseGrants", new PropertyModel(licenseModel, "grants")));

        add(new Label("licenseMessage", new StringResourceModel("license.message.${messageType}", this, new Model(licenseModel))));

        add(new LicenseForm("licenseForm"));
    }

    /**
     *
     */
    private String getlicenseStartText( final License license ) {
        String licenseStartText = "";

        final Date startDate = license.getStartDate();
        if ( startDate != null ) {
            String m = DateUtils.makeRelativeDateMessage(startDate, true);
            m = m != null && m.length() > 0 ? " (" + m + ")" : m;
            licenseStartText = startDate.toString() + m;
        }

        return licenseStartText;
    }

    /**
     *
     */
    private String getlicenseEndText( final License license ) {
        String licenseEndText = "";

        final Date endDate = license.getExpiryDate();
        if ( endDate != null ) {
            String m = DateUtils.makeRelativeDateMessage(endDate, false);
            m = m != null && m.length() > 0 ? " (" + m + ")" : m;
            licenseEndText = endDate.toString() + m;
        }

        return licenseEndText;
    }

    /**
     *
     */
    private String getLicenseAttributeText( final License license ) {
        StringBuilder licenseAttributeText = new StringBuilder();

        Set<String> attrList = license.getAttributes();
        int i = 0;
        for (String attr: attrList) {
            licenseAttributeText.append(attr);
            if (i++ < attrList.size() - 1) {
                licenseAttributeText.append("\n");
            }
        }

        return licenseAttributeText.toString();
    }

    /**
     * Add new license 
     */
    private boolean licenseSetup(final String license) {
        try {
            licenseManager.installNewLicense(license);
            info( new StringResourceModel("license.message.updated", this, null).getString() );
        } catch (InvalidLicenseException e) {
            error(ExceptionUtils.getMessage(e));
            logger.log( Level.WARNING, "Error installing new license", e );
        } catch (UpdateException e) {
            error(ExceptionUtils.getMessage(e));
            logger.log( Level.WARNING, "Error installing new license", e );
        }

        return true;
    }

    /**
     * Model for the current license
     */
    private final class LicenseModel implements IDetachable {
        private License license;

        public String getId() {
            return String.valueOf(getLicense().getId());
        }

        public String getDescription() {
            return TextUtils.toString(getLicense().getDescription());
        }

        public String getGrants() {
            return TextUtils.toString(getLicense().getGrants());
        }

        public String getContact() {
            return TextUtils.toString(getLicense().getLicenseeContactEmail());
        }

        public String getAttributes() {
            return getLicenseAttributeText(getLicense());
        }

        public String getLicensee() {
            return TextUtils.toString(getLicense().getLicenseeName());
        }

        public String getStartDate() {
            return getlicenseStartText( getLicense() );
        }

        public String getEndDate() {
            return getlicenseEndText( getLicense() );
        }

        public boolean isValid() {
            return getLicense().isValidSignature();
        }

        public boolean isIssuerAvailable() {
            return getLicense().getTrustedIssuer() != null;
        }

        public String getIssuer() {
            X509Certificate issuer = getLicense().getTrustedIssuer();
            return issuer==null ? "" : issuer.getSubjectDN().getName();
        }

        public License getLicense() {
            License currentLicense = license;

            if ( currentLicense == null ) {
                try {
                    currentLicense = licenseManager.getCurrentLicense();
                    license = currentLicense;
                } catch ( InvalidLicenseException e ) {
                    logger.log( Level.WARNING, "Error accessing license '"+ExceptionUtils.getMessage(e)+"'." );
                }
            }

            return currentLicense;
        }

        public void detach() {
            license = null;
        }
    }

    /**
     * License update form
     */
    private final class LicenseForm extends Form {

        private final FileUploadField fileUpload = new FileUploadField("license");

        public LicenseForm(final String componentName) {
            super(componentName);

            add(fileUpload);

            setMaxSize(Bytes.bytes(MAX_LICENSE_FILE_UPLOAD_BYTES));
        }

        public final void onSubmit() {
            final FileUpload upload = fileUpload.getFileUpload();
            if ( upload != null ) {
                try {
                    String license = XmlUtil.nodeToString(XmlUtil.parse(upload.getInputStream(), false));
                    licenseSetup( license );
                } catch ( IOException e ) {
                    error( ExceptionUtils.getMessage(e) );
                    logger.log( Level.WARNING, "Error accessing license '"+ExceptionUtils.getMessage(e)+"'." );
                } catch ( SAXException e ) {
                    error( ExceptionUtils.getMessage(e) );
                    logger.log( Level.WARNING, "Error parsing license '"+ExceptionUtils.getMessage(e)+"'." );
                } finally {
                    upload.closeStreams();
                }
            } else {
                logger.log( Level.INFO, "License not found in upload." );                
            }
        }
    }
}
