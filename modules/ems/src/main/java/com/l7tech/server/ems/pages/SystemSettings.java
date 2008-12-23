package com.l7tech.server.ems.pages;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.InvalidLicenseException;
import com.l7tech.gateway.common.License;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdateAny;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gateway.common.admin.Administrative;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.server.UpdatableLicenseManager;
import com.l7tech.server.DefaultKey;
import com.l7tech.server.ems.NavigationPage;
import com.l7tech.server.ems.EsmApplication;
import com.l7tech.server.ems.setup.SetupManager;
import com.l7tech.server.ems.setup.SetupException;
import com.l7tech.util.*;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.upload.FileUpload;
import org.apache.wicket.markup.html.form.upload.FileUploadField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IDetachable;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.Application;
import org.apache.wicket.Component;
import org.apache.wicket.feedback.ContainerFeedbackMessageFilter;
import org.apache.wicket.validation.validator.NumberValidator;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.xml.sax.SAXException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.Serializable;
import java.security.cert.X509Certificate;
import java.security.SignatureException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.text.ParseException;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Page for system settings
 */
@Administrative(licensed=false)
@NavigationPage(page="SystemSettings",pageIndex=100,section="Settings",sectionIndex=200,pageUrl="SystemSettings.html")
public class SystemSettings extends EsmPage {

    private static final int MAX_LICENSE_FILE_UPLOAD_BYTES = SyspropUtil.getInteger("com.l7tech.ems.licenseFile.maxBytes", 1024 * 500);
    private static final Logger logger = Logger.getLogger(SystemSettings.class.getName());

    @SpringBean(name="licenseManager")
    private UpdatableLicenseManager licenseManager;

    @SpringBean(name="setupManager")
    private SetupManager setupManager;

    @SpringBean(name="defaultKey")
    private DefaultKey defaultKey;

    @SpringBean(name="serverConfig")
    private Config config;

    /**
     * Create system settings page
     */
    public SystemSettings() {
        final WebMarkupContainer dynamicDialogHolder = new WebMarkupContainer("dynamic.holder");
        dynamicDialogHolder.setOutputMarkupId(true);

        add(dynamicDialogHolder);

        initComponentsForProductInfo();

        initComponentsForSystemInfo(dynamicDialogHolder);

        initComponentsForLicense(dynamicDialogHolder);

        initComponentsForGlobalSettings();

        if ( isAdminEnabled() ) {
            dynamicDialogHolder.add( new EmptyPanel("dynamic.holder.content") );
        } else {
            showLicenseWarningDialog( dynamicDialogHolder, null );
        }
    }

    private boolean isAdminEnabled() {
        return licenseManager.isFeatureEnabled( "set:admin");
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
    }

    /**
     * Initialize components that display system information
     */
    private void initComponentsForSystemInfo( final WebMarkupContainer dynamicDialogHolder ) {
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
        long timeStarted;
        Application app = getApplication();
        if (app instanceof EsmApplication) {
            timeStarted = ((EsmApplication)app).getTimeStarted();
        } else { // This case is applied for a testing purpose, since PagesTest is running.
            timeStarted = System.currentTimeMillis();
        }
        Label timeStartedLabel = new Label("time.ems.process.started", getSession().buildDateFormat().format(new Date(timeStarted)));
        add(timeStartedLabel);

        final ListenerModel listenerModel = new ListenerModel();

        final Label listenerAddr = new Label("listener.addr", new PropertyModel(listenerModel, "listenerAddr"));
        add(listenerAddr.setOutputMarkupId(true));

        final Label listenerPort = new Label("listener.port", new PropertyModel(listenerModel, "listenerPort"));
        add(listenerPort.setOutputMarkupId(true));

        Form listenerChangeForm = new Form("listenerForm");
        listenerChangeForm.add( new YuiAjaxButton("listenerChangeButton", listenerChangeForm) {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                if ( !isAdminEnabled() ) {
                    showLicenseWarningDialog( dynamicDialogHolder, ajaxRequestTarget );
                } else {
                    ListenerEditPanel listenerEditPanel = new ListenerEditPanel( YuiDialog.getContentId() );
                    YuiDialog dialog = new YuiDialog("dynamic.holder.content", "Change Listener Settings", YuiDialog.Style.OK_CANCEL, listenerEditPanel, new YuiDialog.OkCancelCallback(){
                        @Override
                        public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button) {
                            if ( button == YuiDialog.Button.OK ) {
                                listenerModel.detach();
                                target.addComponent( listenerAddr );
                                target.addComponent( listenerPort );
                            }
                        }
                    } );
                    dynamicDialogHolder.replace(dialog);
                    if ( ajaxRequestTarget != null ) {
                        ajaxRequestTarget.addComponent(dynamicDialogHolder);
                    }
                }
            }
        }.add( new AttemptedUpdateAny( EntityType.CLUSTER_PROPERTY ) ) );
        add(listenerChangeForm);

        final SslModel sslModel = new SslModel();

        final Label sslIssuerLabel = new Label("ssl.cert.issuer", new PropertyModel(sslModel, "issuerDn"));
        add(sslIssuerLabel.setOutputMarkupId(true));

        final Label sslSerialNumberLabel = new Label("ssl.cert.serialNumber", new PropertyModel(sslModel, "serialNumber"));
        add(sslSerialNumberLabel.setOutputMarkupId(true));

        final Label sslSubjectLabel = new Label("ssl.cert.subject", new PropertyModel(sslModel, "subjectDn"));
        add(sslSubjectLabel.setOutputMarkupId(true));

        final Label sslThumbprint = new Label("ssl.cert.thumbprint", new PropertyModel(sslModel, "thumbprint"));
        add(sslThumbprint.setOutputMarkupId(true));

        Form sslChangeForm = new Form("sslForm");
        sslChangeForm.add( new YuiAjaxButton("sslChangeButton", sslChangeForm) {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                if ( !isAdminEnabled() ) {
                    showLicenseWarningDialog( dynamicDialogHolder, ajaxRequestTarget );
                } else {
                    SslEditPanel sslEditPanel = new SslEditPanel( YuiDialog.getContentId() ){
                        @Override
                        @SuppressWarnings({"UnusedDeclaration"})
                        protected void onSubmit(final AjaxRequestTarget target) {
                            sslModel.detach();

                            if ( target != null ) {
                                target.addComponent( sslIssuerLabel );
                                target.addComponent( sslSerialNumberLabel );
                                target.addComponent( sslSubjectLabel );
                                target.addComponent( sslThumbprint );
                            }
                        }
                    };
                    YuiDialog dialog = new YuiDialog("dynamic.holder.content", "Change SSL Settings", YuiDialog.Style.OK_CANCEL, sslEditPanel, new YuiDialog.OkCancelCallback(){
                        @Override
                        public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button ) {
                            //NOTE, due to YUI AJAX form submission for file upload this action is not run.
                        }
                    } );
                    sslEditPanel.setSuccessScript( dialog.getSuccessScript() );
                    dynamicDialogHolder.replace(dialog);
                    if ( ajaxRequestTarget != null ) {
                        ajaxRequestTarget.addComponent(dynamicDialogHolder);
                    }
                }
            }
        }.add( new AttemptedUpdateAny( EntityType.SSG_KEY_ENTRY ) ) );
        add(sslChangeForm);
    }

    /**
     * Initialize components that display licnese information.
     */
    private void initComponentsForLicense( final WebMarkupContainer dynamicDialogHolder ) {
        final LicenseModel licenseModel = new LicenseModel();

        // Feedback
        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        add( feedback.setOutputMarkupId(true) );

        // labels
        WebMarkupContainer licenseDetailsContainer = new WebMarkupContainer("license.container");
        licenseDetailsContainer.setOutputMarkupId(true);
        licenseDetailsContainer.setOutputMarkupPlaceholderTag(true);
        licenseDetailsContainer.add(new Label("licenseStatus", new StringResourceModel("license.signaturevalid.${valid}", this, new Model(licenseModel))));
        licenseDetailsContainer.add(new Label("licenseId", new PropertyModel(licenseModel, "id")));
        licenseDetailsContainer.add(new Label("licenseDescription", new PropertyModel(licenseModel, "description")));
        licenseDetailsContainer.add(new Label("licenseAttributes", new PropertyModel(licenseModel, "attributes")));
        licenseDetailsContainer.add(new Label("licenseIssuer", new StringResourceModel("license.issuer.${issuerAvailable}", this, new Model(licenseModel))));
        licenseDetailsContainer.add(new Label("licensee", new PropertyModel(licenseModel, "licensee")));
        licenseDetailsContainer.add(new Label("licenseContact", new PropertyModel(licenseModel, "contact")));
        licenseDetailsContainer.add(new Label("licenseStartDate", new PropertyModel(licenseModel, "startDate")));
        licenseDetailsContainer.add(new Label("licenseEndDate",  new PropertyModel(licenseModel, "endDate")));
        add( licenseDetailsContainer );

        if ( licenseModel.getLicense() == null ) {
            licenseDetailsContainer.setVisible(false);
        }

        // text area
        licenseDetailsContainer.add(new TextArea("licenseGrants", new PropertyModel(licenseModel, "grants")));

        final Component[] refreshComponents = new Component[]{licenseDetailsContainer};
        Form licenseDeleteForm = new Form("licenseDeleteForm");
        licenseDeleteForm.add( new YuiAjaxButton("deleteLicenseButton", licenseDeleteForm) {
            @Override
            protected void onSubmit(AjaxRequestTarget ajaxRequestTarget, Form form) {
                String warningText =
                    "<p>This will irrevocably destroy the existing license and cannot be undone.</p><br/>" +
                    "<p>Really delete the existing license?</p>";
                Label label = new Label(YuiDialog.getContentId(), warningText);
                label.setEscapeModelStrings(false);
                YuiDialog dialog = new YuiDialog("dynamic.holder.content", "Confirm License Deletion", YuiDialog.Style.OK_CANCEL, label, new YuiDialog.OkCancelCallback(){
                    @Override
                    public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button) {
                        if ( button == YuiDialog.Button.OK ) {
                            licenseDelete();
                            licenseModel.detach();
                            for ( Component component : refreshComponents ) {
                                component.setVisible(false);
                                target.addComponent(component);
                            }
                            info( new StringResourceModel("license.message.deleted", SystemSettings.this, null).getString() );
                            target.addComponent(feedback);
                        }
                        dynamicDialogHolder.replace( new EmptyPanel("dynamic.holder.content") );
                        target.addComponent( dynamicDialogHolder );
                    }
                } );
                dynamicDialogHolder.replace(dialog);
                if ( ajaxRequestTarget != null ) {
                    ajaxRequestTarget.addComponent(dynamicDialogHolder);
                }
            }
        }.add( new AttemptedUpdateAny( EntityType.CLUSTER_PROPERTY ) ) );
        licenseDetailsContainer.add(licenseDeleteForm);

        add(new LicenseForm("licenseForm", refreshComponents, dynamicDialogHolder, feedback).setOutputMarkupId(true));
    }

    private void initComponentsForGlobalSettings() {
        String timeUnit = config.getProperty("em.server.session.timeout", "30m");
        final GlobalSettings settingsModel = new GlobalSettings( (int)(TimeUnit.parse(timeUnit, TimeUnit.MINUTES) / (1000L*60L)) );
        RequiredTextField sessionTimeout = new RequiredTextField("timeout", new PropertyModel( settingsModel, "sessionTimeout" ) );
        sessionTimeout.add( new NumberValidator.RangeValidator(1,1440) );

        final FeedbackPanel feedback = new FeedbackPanel("globalFeedback");

        AttemptedOperation attemptedOperation =  new AttemptedUpdateAny( EntityType.CLUSTER_PROPERTY ) ;
        Form globalForm = new SecureForm("globalForm", attemptedOperation);
        globalForm.add( new YuiAjaxButton("global.submit", globalForm) {
            @Override
            protected void onSubmit( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                try {
                    setupManager.setSessionTimeout( settingsModel.getSessionTimeout() );
                    this.getForm().info( new StringResourceModel("global.message.updated", SystemSettings.this, null).getString() );
                } catch ( SetupException se ) {
                    this.getForm().error( new StringResourceModel("global.message.error", SystemSettings.this, null).getString() );
                    logger.log( Level.WARNING, "Error updating session timeout cluster property.", se );
                }
                ajaxRequestTarget.addComponent( feedback );
            }
            @Override
            protected void onError( final AjaxRequestTarget ajaxRequestTarget, final Form form ) {
                ajaxRequestTarget.addComponent( feedback );
            }
        }.add( attemptedOperation ) );

        globalForm.add( sessionTimeout );
        feedback.setFilter( new ContainerFeedbackMessageFilter(globalForm) );        

        add( feedback.setOutputMarkupId(true) );
        add( globalForm );
    }

    /**
     * Display unlicensed warning dialog. 
     */
    private void showLicenseWarningDialog( final WebMarkupContainer dynamicDialogHolder, final AjaxRequestTarget target ) {
        String warningText =
            "<p>Enterprise Service Manager is not licensed.</p><br/>" +
            "<p>Please install a license to access Enterprise Service Manager functionality.</p>";
        Label label = new Label(YuiDialog.getContentId(), warningText);
        label.setEscapeModelStrings(false);
        YuiDialog dialog = new YuiDialog("dynamic.holder.content", "Enterprise Service Manager Not Licensed", YuiDialog.Style.CLOSE, label, null );

        if ( target == null ) {
            dynamicDialogHolder.add(dialog);
        } else {
            dynamicDialogHolder.replace(dialog);
            target.addComponent(dynamicDialogHolder);
        }
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
            licenseStartText = getSession().buildDateFormat().format(startDate) + m;
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
            licenseEndText = getSession().buildDateFormat().format(endDate)  + m;
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
        boolean installed = false;

        try {
            licenseManager.installNewLicense(license);
            installed = true;
            info( new StringResourceModel("license.message.updated", this, null).getString() );
        } catch (InvalidLicenseException e) {
            error(ExceptionUtils.getMessage(e));
            logger.log( Level.WARNING, "Error installing new license '"+ExceptionUtils.getMessage(e)+"'.", ExceptionUtils.getDebugException(e) );
        } catch (UpdateException e) {
            error(ExceptionUtils.getMessage(e));
            logger.log( Level.WARNING, "Error installing new license", e );
        }

        return installed;
    }

    /**
     * Add new license
     */
    private void licenseDelete() {
        try {
            setupManager.deleteLicense();
        } catch ( DeleteException de ) {
            info( "Could not delete license '" + ExceptionUtils.getMessage(de) + "'." );
            logger.log( Level.WARNING, "Error deleting license.", de );
        }
    }

    /**
     * Model for the current license
     */
    private final class LicenseModel implements IDetachable {
        private License license;

        public String getId() {
            long id = getLicense()==null ? -1 : getLicense().getId();
            return id <= 0 ? "" : String.valueOf(id);
        }

        public String getDescription() {
            return TextUtils.toString(getLicense()==null ? null : getLicense().getDescription());
        }

        public String getGrants() {
            return TextUtils.toString(getLicense()==null ? null : getLicense().getGrants());
        }

        public String getContact() {
            return TextUtils.toString(getLicense()==null ? null : getLicense().getLicenseeContactEmail());
        }

        public String getAttributes() {
            return getLicense()==null ? "" : getLicenseAttributeText(getLicense());
        }

        public String getLicensee() {
            return TextUtils.toString(getLicense()==null ? null : getLicense().getLicenseeName());
        }

        public String getStartDate() {
            return getLicense()==null ? "" : getlicenseStartText( getLicense() );
        }

        public String getEndDate() {
            return getLicense()==null ? "" : getlicenseEndText( getLicense() );
        }

        public boolean isValid() {
            return getLicense() != null && getLicense().isValidSignature();
        }

        public boolean isIssuerAvailable() {
            return getLicense() != null && getLicense().getTrustedIssuer() != null;
        }

        public String getIssuer() {
            X509Certificate issuer = getLicense()==null ? null : getLicense().getTrustedIssuer();
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

        @Override
        public void detach() {
            license = null;
        }
    }

    /**
     * License update form
     */
    private final class LicenseForm extends YuiFileUploadForm {

        private final FileUploadField fileUpload = new FileUploadField("license");
        private final Component[] components;
        private final WebMarkupContainer dynamicDialogHolder;
        private final FeedbackPanel formFeedback;

        public LicenseForm(final String componentName, final Component[] components, final WebMarkupContainer dynamicDialogHolder, final FeedbackPanel formFeedback ) {
            super(componentName);

            this.components = components;
            this.dynamicDialogHolder = dynamicDialogHolder;
            this.formFeedback = formFeedback;

            add(fileUpload);
            add(new YuiButton("license.submit"));

            setMaxSize(Bytes.bytes(MAX_LICENSE_FILE_UPLOAD_BYTES));
            add( new AttemptedUpdateAny( EntityType.CLUSTER_PROPERTY ) );
        }

        @Override
        public final void onSubmit( final AjaxRequestTarget target ) {
            final FileUpload upload = fileUpload.getFileUpload();
            if ( upload != null ) {
                try {
                    final String license = XmlUtil.nodeToString(XmlUtil.parse(upload.getInputStream(), false));
                    EulaPanel eula = new EulaPanel( YuiDialog.getContentId(), new Model(new License(license, null, null)) );

                    YuiDialog dialog = new YuiDialog("dynamic.holder.content", "License Agreement", YuiDialog.Style.OK_CANCEL, eula, new YuiDialog.OkCancelCallback(){
                        @Override
                        public void onAction( final YuiDialog dialog, final AjaxRequestTarget target, final YuiDialog.Button button) {
                            if ( button == YuiDialog.Button.OK ) {
                                logger.fine("License installation confirmed.");
                                if ( licenseSetup( license ) ) {
                                    for ( Component component : components ) {
                                        component.setVisible(true);
                                        target.addComponent(component);
                                    }
                                    target.addComponent(LicenseForm.this);
                                }
                            } else {
                                logger.fine("License installation cancelled.");
                                info( new StringResourceModel("license.message.cancel", SystemSettings.this, null).getString() );
                            }
                            dynamicDialogHolder.replace( new EmptyPanel("dynamic.holder.content") );
                            target.addComponent(formFeedback);
                            target.addComponent(dynamicDialogHolder);
                        }
                    }, "600px");
                    dynamicDialogHolder.replace(dialog);
                    if ( target != null ) {
                        target.addComponent(dynamicDialogHolder);
                    }
                } catch ( IOException e ) {
                    error( ExceptionUtils.getMessage(e) );
                    logger.log( Level.WARNING, "Error accessing license '"+ExceptionUtils.getMessage(e)+"'." );
                } catch ( SAXException e ) {
                    error( ExceptionUtils.getMessage(e) );
                    logger.log( Level.WARNING, "Error parsing license '"+ExceptionUtils.getMessage(e)+"'." );
                } catch (TooManyChildElementsException e) {
                    error( ExceptionUtils.getMessage(e) );
                    logger.log( Level.WARNING, "Error parsing license '"+ExceptionUtils.getMessage(e)+"'." );
                } catch (SignatureException e) {
                    error( ExceptionUtils.getMessage(e) );
                    logger.log( Level.WARNING, "Error parsing license '"+ExceptionUtils.getMessage(e)+"'." );
                } catch (InvalidLicenseException e) {
                    error( ExceptionUtils.getMessage(e) );
                    logger.log( Level.WARNING, "Error parsing license '"+ExceptionUtils.getMessage(e)+"'." );
                } catch (ParseException e) {
                    error( ExceptionUtils.getMessage(e) );
                    logger.log( Level.WARNING, "Error parsing license '"+ExceptionUtils.getMessage(e)+"'." );
                } finally {
                    upload.closeStreams();
                }
            } else {
                logger.log( Level.FINE, "License not found in upload." );
            }
        }
    }

    private final class ListenerModel implements IDetachable {
        private String listenerAddr;
        private String listenerPort;

        private void init() {
            if ( listenerAddr==null || listenerPort==null ) {
                listenerAddr = config.getProperty("em.server.listenaddr", "*");
                if ( "0.0.0.0".equals(listenerAddr) ) {
                    listenerAddr = "*";
                }

                listenerPort = config.getProperty("em.server.listenport", "8182");
            }
        }

        public String getListenerAddr() {
            init();
            return listenerAddr;
        }

        public String getListenerPort() {
            init();
            return listenerPort;
        }

        @Override
        public void detach() {
            listenerAddr = null;
            listenerPort = null;
        }
    }

    private final class SslModel implements IDetachable {
        private X509Certificate certificate;

        private void init() {
            if ( certificate == null ) {
                try {
                    certificate = defaultKey.getSslInfo().getCertificate();
                } catch (IOException ioe) {
                    logger.log( Level.WARNING, "Error getting SSL information.", ioe);
                }
            }
        }

        public String getIssuerDn() {
            init();
            return certificate==null ? "" : certificate.getIssuerDN().getName();
        }

        public String getSerialNumber() {
            init();
            return certificate==null ? "" : hexFormat(certificate.getSerialNumber().toByteArray());
        }

        public String getSubjectDn() {
            init();
            return certificate==null ? "" : certificate.getSubjectDN().getName();    
        }

        public String getThumbprint() {
            init();
            try {
                return certificate==null ? "" : CertUtils.getCertificateFingerprint(certificate, "SHA1").substring(5);
            } catch ( GeneralSecurityException gse ) {
                return "";
            }
        }

        @Override
        public void detach() {
            certificate = null;
        }
    }

    private static final class GlobalSettings implements Serializable {
        private int sessionTimeout; // in minutes

        private GlobalSettings( final int sessionTimeout ) {
            this.sessionTimeout = sessionTimeout;
        }

        public int getSessionTimeout() {
            return sessionTimeout;
        }

        public void setSessionTimeout(int sessionTimeout) {
            this.sessionTimeout = sessionTimeout;
        }
    }

    private String hexFormat( final byte[] bytes ) {
        StringBuilder builder = new StringBuilder();

        for ( int i=0; i<bytes.length; i++ ) {
            String byteHex = HexUtils.hexDump(new byte[]{bytes[i]});
            builder.append(byteHex.toUpperCase());
            if ( i<bytes.length-1 ) {
                builder.append(':');
            }
        }

        return builder.toString();
    }
}
