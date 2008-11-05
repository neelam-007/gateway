package com.l7tech.server.ems.pages;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.License;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.server.UpdatableLicenseManager;
import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.EmsApplication;
import com.l7tech.util.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.SimpleDateFormat;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Page for system settings
 */
@NavigationPage(page="SystemSettings",pageIndex=100,section="Settings",sectionIndex=200,pageUrl="SystemSettings.html")
public class SystemSettings extends EmsPage {

    private static final int MAX_LICENSE_FILE_UPLOAD_BYTES = SyspropUtil.getInteger("com.l7tech.ems.licenseFile.maxBytes", 1024 * 500);
    private static final Logger logger = Logger.getLogger(SystemSettings.class.getName());
    private static final String COPYRIGHT = "Copyright (c) 2008 by Layer 7 Technologies, Inc. All rights reserved.";

    @SuppressWarnings({"UnusedDeclaration"})
    @SpringBean(name="licenseManager")
    private UpdatableLicenseManager licenseManager;

    /**
     * Create system settings page
     */
    public SystemSettings() {
        initComponentsForProductInfo();

        initComponentsForSystemInfo();

        initComponentsForLicense();
    }

    /**
     * Initialize components that display product information.
     */
    private void initComponentsForProductInfo() {
        // Product Name
        Label productNameLabel = new Label("product.name", BuildInfo.getProductName());
        add(productNameLabel);

        // Product Version
        Label productVersionLabel = new Label("product.version", BuildInfo.getProductVersion());
        add(productVersionLabel);

        // Build Number
        Label buildNumberLabel = new Label("build.number", BuildInfo.getBuildNumber());
        add(buildNumberLabel);

        // Copyright Notice
        Label copyrightLabel = new Label("copyright", COPYRIGHT);
        add(copyrightLabel);
    }

    /**
     * Initialize components that display system information
     */
    private void initComponentsForSystemInfo() {
        // Host Name
        HttpServletRequest request = ((ServletWebRequest) getRequest()).getHttpServletRequest();
        String hostName;
        try {
            URL url = new URL(request.getRequestURL().toString());
            hostName = url.getHost();
        } catch (MalformedURLException e) {
            logger.warning("The URL specified an unkown protocol.");
            hostName = "Unknown host name";
        }
        Label hostNameLabel = new Label("host.name", hostName);
        add(hostNameLabel);

        // Host IP Address
        Label hostIpLabel = new Label("host.ip", request.getLocalAddr());
        add(hostIpLabel);

        // Operating System Information
        String osInfo = System.getProperty("os.name") + " " +
                        System.getProperty("os.arch") + " " +
                        System.getProperty("os.version");
        Label osInfoLabel = new Label("operating.system", osInfo);
        add(osInfoLabel);

        // Java VM Information
        String jvmInfo = System.getProperty("java.vm.vendor") + " " +
                         System.getProperty("java.vm.name") + " " +
                         System.getProperty("java.vm.version");
        Label jvmInfoLabel = new Label("jvm", jvmInfo);
        add(jvmInfoLabel);

        // Java VM Memory Details
        Label totalMemoryLabel = new Label("memory.total", "" + Runtime.getRuntime().totalMemory());
        add(totalMemoryLabel);

        Label freeMemoryLabel = new Label("memory.free", "" + Runtime.getRuntime().freeMemory());
        add(freeMemoryLabel);

        Label maximumMemoryLabel = new Label("memory.maximum", "" + Runtime.getRuntime().maxMemory());
        add(maximumMemoryLabel);

        // Time when EMS process started
        SimpleDateFormat dateFormat = new SimpleDateFormat(getSession().getDateTimeFormatPattern());
        long timeStarted = ((EmsApplication)getApplication()).getTimeStarted();
        Label timeStartedLabel = new Label("time.ems.process.started", dateFormat.format(new Date(timeStarted)));
        add(timeStartedLabel);
    }

    /**
     * Initialize components that display licnese information.
     */
    private void initComponentsForLicense() {
        LicenseModel licenseModel = new LicenseModel();

        // Feedback
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

        add(new LicenseForm("licenseForm"));
    }

    /**
     * Get the date string for license starting.
     */
    private String getlicenseStartText( final License license ) {
        String licenseStartText = "";

        final Date startDate = license.getStartDate();
        if ( startDate != null ) {
            String m = DateUtils.makeRelativeDateMessage(startDate, true);
            m = m != null && m.length() > 0 ? " (" + m + ")" : m;
            SimpleDateFormat dateFormat = new SimpleDateFormat(getSession().getDateTimeFormatPattern());
            licenseStartText = dateFormat.format(startDate) + m;
        }

        return licenseStartText;
    }

    /**
     * Get the date string for license expiring.
     */
    private String getlicenseEndText( final License license ) {
        String licenseEndText = "";

        final Date endDate = license.getExpiryDate();
        if ( endDate != null ) {
            String m = DateUtils.makeRelativeDateMessage(endDate, false);
            m = m != null && m.length() > 0 ? " (" + m + ")" : m;
            SimpleDateFormat dateFormat = new SimpleDateFormat(getSession().getDateTimeFormatPattern());
            licenseEndText = dateFormat.format(endDate)  + m;
        }

        return licenseEndText;
    }

    /**
     * Get the string of license attributes.
     */
    private String getLicenseAttributeText( final License license ) {
        StringBuilder licenseAttributeText = new StringBuilder();

        Set<String> attrList = license.getAttributes();
        if ( attrList != null ) {
            int i = 0;
            for (String attr: attrList) {
                licenseAttributeText.append(attr);
                if (i++ < attrList.size() - 1) {
                    licenseAttributeText.append("\n");
                }
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
            long id = getLicense().getId();
            return id <= 0 ? "" : String.valueOf(id);
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
            add(new YuiButton("license.submit"));

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
