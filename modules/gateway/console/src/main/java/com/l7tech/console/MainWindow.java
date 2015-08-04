package com.l7tech.console;

import com.l7tech.console.action.*;
import com.l7tech.console.auditalerts.AuditAlertChecker;
import com.l7tech.console.auditalerts.AuditAlertConfigBean;
import com.l7tech.console.auditalerts.AuditAlertOptionsAction;
import com.l7tech.console.auditalerts.AuditAlertsNotificationPanel;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.console.logging.CascadingErrorHandler;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.panels.*;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.panels.licensing.ManageLicensesDialog;
import com.l7tech.console.panels.policydiff.PolicyDiffContext;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.security.AuthenticationProvider;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.security.PermissionRefreshListener;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.identity.IdentitiesRootNode;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.tree.policy.PolicyToolBar;
import com.l7tech.console.tree.servicesAndPolicies.AlterFilterAction;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.Authorizer;
import com.l7tech.gateway.common.VersionException;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.custom.CustomAssertionsRegistrar;
import com.l7tech.gateway.common.licensing.CompositeLicense;
import com.l7tech.gateway.common.licensing.FeatureLicense;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteAll;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.action.CustomTaskActionUI;
import com.l7tech.util.*;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.*;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.rmi.server.RMIClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;


/**
 * The console main window <CODE>MainWindow</CODE> class.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class MainWindow extends JFrame implements SheetHolder {
    static Logger log = Logger.getLogger(MainWindow.class.getName());
    /**
     * the resource path for the application
     */
    public static final String RESOURCE_PATH = "com/l7tech/console/resources";

    public static final String CONNECTION_PREFIX = " [connected to node: ";

    private static final long PING_INTERVAL = ConfigFactory.getLongProperty( "com.l7tech.console.sessionPingInterval", 50000L );

    /**
     * the resource bundle name
     */
    private static
    ResourceBundle resapplication = ResourceBundle.getBundle("com.l7tech.console.resources.console");

    private static Action goToAction = null;
    private static Action findAction = null;
    private static Action f3Action = null;
    private static Action shiftF3Action = null;

    /**
     * Reference to the component which currently has focus
     */
    private static JComponent focusOwner = null;
    private static boolean infoDialogShowing = false;

    /* this class classloader */
    private final ClassLoader cl = MainWindow.class.getClassLoader();

    private JMenuBar mainJMenuBar = null;
    private JMenu fileMenu = null;
    private JMenu editMenu = null;
    private JMenu tasksMenu = null;
    private JMenu viewMenu = null;
    private JMenu helpMenu = null;
    private JMenu newProviderSubMenu = null;
    private JMenu manageAdminUsersSubMenu = null;
    private JMenu filterServiceAndPolicyTreeMenu = null;
    private JMenu sortServiceAndPolicyTreeMenu = null;
    private JMenu customGlobalActionsMenu = null;

    private JMenuItem connectMenuItem = null;
    private JMenuItem disconnectMenuItem = null;
    private JMenuItem myAccountMenuItem = null;
    private JMenuItem exitMenuItem = null;
    private JMenuItem menuItemPref = null;
    private JMenuItem auditMenuItem = null;
    private JMenuItem viewLogMenuItem = null;
    private JMenuItem fromFileMenuItem = null;
    private JMenuItem manageJmsEndpointsMenuItem = null;
    private JMenuItem manageKerberosMenuItem = null;
    private JMenuItem manageCertificatesMenuItem = null;
    private JMenuItem managePrivateKeysMenuItem = null;
    private JMenuItem manageSecurePasswordsMenuItem = null;
    private JMenuItem revokeCertificatesMenuItem = null;
    private JMenuItem manageGlobalResourcesMenuItem = null;
    private JMenuItem manageClusterPropertiesMenuItem = null;
    private JMenuItem manageRolesMenuItem = null;
    private JMenuItem manageServiceResolutionMenuItem = null;
    private JMenuItem dashboardMenuItem;
    private JMenuItem manageClusterLicensesMenuItem = null;
    private JMenuItem helpTopicsMenuItem = null;
    private JMenuItem manageAuditAlertsMenuItem;
    private JMenuItem editPolicyMenuItem = null;
    private JMenuItem servicePropertiesMenuItem = null;
    private JMenuItem serviceUDDISettingsMenuItem = null;
    private JMenuItem deleteServiceMenuItem = null;
    private JMenuItem policyDiffMenuItem;

    private JPopupMenu appletManagePopUpMenu;

    // actions
    private Action refreshAction = null;
    private FindIdentityAction findIdentityAction = null;
    private Action prefsAction = null;
    private Action removeNodeAction = null;
    private Action connectAction = null;
    private Action disconnectAction = null;
    private Action toggleStatusBarAction = null;
    private Action togglePolicyMessageArea = null;
    private Action togglePolicyInputsAndOutputs = null;
    private MyAccountAction myAccountAction = null;
    private PublishServiceAction publishServiceAction = null;
    private PublishNonSoapServiceAction publishNonSoapServiceAction = null;
    private PublishRestServiceAction publishRestServiceAction = null;
    private PublishInternalServiceAction publishInternalServiceAction;
    private PublishReverseWebProxyAction publishReverseWebProxyAction;
    private CreateServiceWsdlAction createServiceAction = null;
    private CreatePolicyAction createPolicyAction;
    private ViewGatewayAuditsAction viewGatewayAuditsWindowAction;
    private ViewAuditsFromFileAction auditOrLogFromFileAction;
    private ViewLogsAction viewLogsAction;
    private ManageJmsEndpointsAction manageJmsEndpointsAction = null;
    private ManageKerberosAction manageKerberosAction = null;
    private ManageRolesAction manageRolesAction = null;
    private ManageResolutionConfigurationAction manageServiceResolutionAction = null;
    private HomeAction homeAction;
    private NewGroupAction newInernalGroupAction;
    private NewLdapProviderAction newLDAPProviderAction;
    private NewBindOnlyLdapProviderAction newBindOnlyLdapProviderAction;
    private NewPolicyBackedIdentityProviderAction newPolicyBackedIdentityProviderAction;
    private NewFederatedIdentityProviderAction newPKIProviderAction;
    private ManageCertificatesAction manageCertificatesAction = null;
    private ManagePrivateKeysAction managePrivateKeysAction = null;
    private ManageSecurePasswordsAction manageSecurePasswordsAction = null;
    private ManageSsgConnectorsAction manageSsgConnectorsAction = null;
    private ManageJdbcConnectionsAction manageJdbcConnectionsAction = null;
    private ManageScheduledTasksAction manageScheduledTasksAction = null;
    private ManageCassandraConnectionAction manageCassandraConnectionAction = null;
    private ManageTrustedEsmUsersAction manageTrustedEsmUsersAction = null;
    private RevokeCertificatesAction revokeCertificatesAction = null;
    private ManageGlobalResourcesAction manageGlobalResourcesAction = null;
    private ManageClusterPropertiesAction manageClusterPropertiesAction = null;
    private ShowDashboardAction showDashboardAction = null;
    private ManageClusterLicensesAction manageClusterLicensesAction = null;
    private NewInternalUserAction newInernalUserAction;
    private AuditAlertOptionsAction manageAuditAlertsAction;
    private ManageLogSinksAction manageLogSinksAction = null;
    private ManageEmailListenersAction manageEmailListenersAction = null;
    private ConfigureFtpAuditArchiverAction configureFtpAuditArchiver = null;
    private ManageUDDIRegistriesAction manageUDDIRegistriesAction = null;
    private ManageHttpConfigurationAction manageHttpConfigurationAction = null;
    private ManageEncapsulatedAssertionsAction manageEncapsulatedAssertionsAction = null;
    private ManagePolicyBackedServicesAction managePolicyBackedServicesAction = null;
    private ManageSecurityZonesAction manageSecurityZonesAction = null;
    private ManageSiteMinderConfigurationAction manageSiteMinderConfigurationAction = null;
    private ManageServerModuleFilesAction manageServerModuleFilesAction = null;
    private ManageWorkQueuesAction manageWorkQueuesAction = null;
    private ManageSolutionKitsAction manageSolutionKitsAction = null;

    private JPanel frameContentPane = null;
    private JPanel mainPane = null;
    private JPanel statusBarPane = null;
    private JLabel statusMsgLeft = null;
    private JLabel statusMsgRight = null;
    private JLabel filterStatusLabel = null;

    private JToolBar toolBarPane = null;
    private PolicyToolBar policyToolBar = null;
    private JSplitPane mainSplitPane = null;
    private JPanel mainLeftPanel = null;
    private JPanel mainSplitPaneRight = null;
    private JTabbedPane paletteTabbedPane;

    private EditableSearchComboBox<AbstractTreeNode> searchComboBox;
    private EditableSearchComboBox<AbstractLeafPaletteNode> assertionSearchComboBox;

    private final JLabel searchLabel = new JLabel(resapplication.getString("Search"));
    private final JLabel assertionSearchLabel = new JLabel(resapplication.getString("Search"));

    public static final String TITLE = "Gateway Management Console";
    private EventListenerList listenerList = new WeakEventListenerList();
    @SuppressWarnings({"FieldCanBeLocal"})
    private LogonListener closeWindowListener;
    @SuppressWarnings({"FieldCanBeLocal"})
    private LogonListener topMenuLogonListener;
    @SuppressWarnings({"FieldCanBeLocal"})
    private LicenseListener topMenuLicenseListener;
    @SuppressWarnings({"FieldCanBeLocal"})
    private PermissionRefreshListener topMenuPermissionRefreshListener;
    // cached credential manager
    private String connectionContext = "";
    private String connectionID = "";
    private ServicesAndPoliciesTree servicesAndPoliciesTree;
    private IdentityProvidersTree identityProvidersTree;
    private JMenuItem validateMenuItem;
    private JCheckBoxMenuItem showInputsAndOutputsMenuItem;
    private JMenuItem showAstnCommentsMenuItem;
    private JMenuItem showAstnLnsMenuItem;
    private JMenuItem importMenuItem;
    private JMenuItem exportMenuItem;
    private JMenuItem saveAndActivateMenuItem;
    private JMenuItem saveOnlyMenuItem;
    private JMenuItem migrateNamespacesMenuItem;
    private boolean disconnected = false;
    private SsmApplication ssmApplication;
    private IdentitiesRootNode identitiesRootNode;
    private JTextPane descriptionText;
    private JSplitPane verticalSplitPane;
    private double preferredVerticalSplitLocation = 0.57;
    private double preferredHorizontalSplitLocation = 0.27;
    private boolean maximizeOnStart = false;

    private final SsmPreferences preferences;

    private AuditAlertsNotificationPanel auditAlertBar;
    private AuditAlertChecker auditAlertChecker;
    private X509Certificate serverSslCert;
    private X509Certificate auditSigningCert;
    private RootNode rootNode;
    private String serviceUrl = null;
    public final static String FILTER_STATUS_NONE = "Filter: None";
    public final static String FILTER_STATUS_SERVICES = "Filter: Services";
    public final static String FILTER_STATUS_POLICY_FRAGMENTS = "Filter: Policy Fragments";

    private static final String WARNING_BANNER_PROP_NAME = "logon.warningBanner";
    public static final String L7_GO_TO = "l7goto";
    public static final String L7_FIND = "l7search";
    public static final String L7_ESC = "l7esc";
    public static final String L7_F3 = "l7f3";
    public static final String L7_SHIFT_F3 = "l7shiftf3";
    private final ActiveKeypairJob activeKeypairJob = new ActiveKeypairJob();

    private static final String PATH_SEPARATOR = "/";
    private static final String ELLIPSIS = "...";

    /**
     * MainWindow constructor comment.
     *
     * @param app the application bean
     */
    public MainWindow(SsmApplication app) {
        super(TITLE);
        ssmApplication = app;
        this.preferences = (SsmPreferences) app.getApplicationContext().getBean("preferences");
        if (preferences == null) throw new IllegalStateException("Internal error: no preferences bean");

        initialize();
    }

    public ActiveKeypairJob getActiveKeypairJob() {
        return activeKeypairJob;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            toFront();
            if (maximizeOnStart) {
                setExtendedState(MAXIMIZED_BOTH);
                resetSplitLocations();
            }
        }
    }

    /**
     * add the ConnectionListener
     * <p/>
     * The listener is stored as a weak reference, to ensure it's not auto removed a reference must be kept
     * somewhere else.
     *
     * @param listener the ConnectionListener
     */
    private void addLogonListener(LogonListener listener) {
        listenerList.add(LogonListener.class, listener);
    }

    public void addPermissionRefreshListener(PermissionRefreshListener listener) {
        listenerList.add(PermissionRefreshListener.class, listener);
    }

    public void firePermissionRefresh() {
        for (PermissionRefreshListener listener : listenerList.getListeners(PermissionRefreshListener.class)) {
            listener.onPermissionRefresh();
        }
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type (connection event).
     */
    private void fireConnected() {
        TopComponents.getInstance().setConnectionLost(false);
        User u = Registry.getDefault().getSecurityProvider().getUser();
        if (u == null) throw new IllegalStateException("Logon apparently worked, but no User is available");
        LogonEvent event = new LogonEvent(this, LogonEvent.LOGON);
        LogonListener[] listeners = listenerList.getListeners(LogonListener.class);
        for (LogonListener listener : listeners) {
            listener.onLogon(event);
        }
        disconnected = false;
        setFilterAndSortMenuEnabled(true);
        getSearchComboBox().setEnabled(true);
        getAssertionSearchComboBox().setEnabled(true);
        searchLabel.setEnabled(true);
        assertionSearchLabel.setEnabled(true);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type (connection event).
     */
    private void fireDisconnected() {
        Registry.getDefault().getLicenseManager().setLicense(null);
        LogonEvent event = new LogonEvent(this, LogonEvent.LOGOFF);
        ssmApplication.getApplicationContext().publishEvent(event);
        EventListener[] listeners = listenerList.getListeners(LogonListener.class);
        for (EventListener listener : listeners) {
            try {
                ((LogonListener) listener).onLogoff(event);
            } catch (Exception e) {
                log.log(Level.WARNING, "Error delivering logoff event: " + ExceptionUtils.getMessage(e), e);
            }
        }
        disconnected = true;
        descriptionText.setText("");
        setFilterAndSortMenuEnabled(false);
        getSearchComboBox().setEnabled(false);
        getAssertionSearchComboBox().setEnabled(false);
        searchLabel.setEnabled(false);
        assertionSearchLabel.setEnabled(false);
        getSearchComboBox().clearSearch();
        getAssertionSearchComboBox().clearSearch();
        getCustomGlobalActionsMenu().removeAll();
        getCustomGlobalActionsMenu().setEnabled(false);
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    /**
     * @return the SsmApplication for which this MainWindow was created.
     */
    public SsmApplication getSsmApplication() {
        return ssmApplication;
    }

    /**
     * Return the ConnectMenuItem property value.
     *
     * @return JMenuItem
     */
    private JMenuItem getConnectMenuItem() {
        if (connectMenuItem == null) {
            connectMenuItem = new JMenuItem(getConnectAction());
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/connect2.gif"));
            connectMenuItem.setIcon(icon);
            int mnemonic = connectMenuItem.getText().toCharArray()[0];
            connectMenuItem.setMnemonic(mnemonic);
            connectMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return connectMenuItem;
    }

    /**
     * Return the DisconnectMenuItem property value.
     *
     * @return JMenuItem
     */
    private JMenuItem getDisconnectMenuItem() {
        if (disconnectMenuItem != null)
            return disconnectMenuItem;

        disconnectMenuItem = new JMenuItem(getDisconnectAction());
        //disconnectMenuItem.setFocusable(false);
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/disconnect.gif"));
        disconnectMenuItem.setIcon(icon);

        int mnemonic = disconnectMenuItem.getText().toCharArray()[0];
        disconnectMenuItem.setMnemonic(mnemonic);
        disconnectMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));

        return disconnectMenuItem;
    }

    /**
     * Return the My Account menu item.
     *
     * @param accel if true, an accelerator key will be set on the menu item if it is created.
     * @return JMenuItem
     */
    private JMenuItem getMyAccountMenuItem(final boolean accel) {
        if (myAccountMenuItem == null) {
            myAccountMenuItem = new JMenuItem(getMyAccountAction());
        }
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/user16.png"));
        myAccountMenuItem.setIcon(icon);
        if (accel) {
            int mnemonic = myAccountMenuItem.getText().toCharArray()[2];
            myAccountMenuItem.setMnemonic(mnemonic);
            myAccountMenuItem.setAccelerator(
                    KeyStroke.getKeyStroke(Character.toUpperCase(mnemonic), ActionEvent.ALT_MASK));
        }
        return myAccountMenuItem;
    }

    /**
     * Return the menuItemPref property value.
     *
     * @return JMenuItem
     */
    private JMenuItem getMenuItemPreferences(boolean accel) {
        if (menuItemPref == null) {
            menuItemPref = new JMenuItem(getPreferencesAction());
            //menuItemPref.setFocusable(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/preferences.gif"));
            menuItemPref.setIcon(icon);
            if (accel) {
                int mnemonic = menuItemPref.getText().toCharArray()[0];
                menuItemPref.setMnemonic(mnemonic);
                menuItemPref.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
            }
        }
        return menuItemPref;
    }


    /**
     * Return the ExitMenuItem property value.
     *
     * @return JMenuItem
     */
    private JMenuItem getExitMenuItem() {
        if (exitMenuItem == null) {
            exitMenuItem = new JMenuItem();
            //exitMenuItem.setFocusable(false);
            exitMenuItem.setText(resapplication.getString("ExitMenuItem.name"));
            exitMenuItem.setToolTipText(resapplication.getString("ExitMenuItem.desc"));
            int mnemonic = 'X';
            exitMenuItem.setMnemonic(mnemonic);
            exitMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return exitMenuItem;
    }


    /**
     * Return the helpTopicsMenuItem property value.
     *
     * @return JMenuItem
     */
    private JMenuItem getHelpTopicsMenuItem() {
        if (helpTopicsMenuItem == null) {
            helpTopicsMenuItem = new JMenuItem();
            //helpTopicsMenuItem.setFocusable(false);
            helpTopicsMenuItem.setText(resapplication.getString("Help_TopicsMenuItem_text_name"));
            helpTopicsMenuItem.setToolTipText(resapplication.getString("Help_TopicsMenuItem_text_description"));
            int mnemonic = helpTopicsMenuItem.getText().toCharArray()[0];
            helpTopicsMenuItem.setMnemonic(mnemonic);
            helpTopicsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0));
        }
        return helpTopicsMenuItem;
    }


    /**
     * Return the fileMenu property value.
     *
     * @return JMenu
     */
    private JMenu getFileMenu() {
        if (fileMenu == null) {
            JMenu menu = new JMenu();
            //fileMenu.setFocusable(false);
            menu.setText(resapplication.getString("File"));
            //menu.add(getNewPolicyMenuItem());
            menu.add(getSaveAndActivateMenuItem());
            menu.add(getSaveOnlyMenuItem());
            menu.add(getExportMenuItem());
            menu.add(getImportMenuItem());
            menu.add(getValidateMenuItem());

            menu.addSeparator();
            menu.add(getEditPolicyMenuItem());
            menu.add(getServicePropertiesMenuItem());
            menu.add(getServiceUDDISettingsMenuItem());
            menu.add(getDeleteServiceMenuItem());
            menu.add(getPolicyDiffMenuItem());

            menu.addSeparator();

            menu.add(getConnectMenuItem());
            menu.add(getDisconnectMenuItem());
            menu.add(getMyAccountMenuItem(true));
            if (!isApplet()) {
                menu.add(getMenuItemPreferences(true));
                menu.add(getExitMenuItem());
            }
            int mnemonic = menu.getText().toCharArray()[0];
            menu.setMnemonic(mnemonic);

            fileMenu = menu;
        }
        return fileMenu;
    }

    private JMenuItem getEditPolicyMenuItem() {
        if (editPolicyMenuItem == null) {
            editPolicyMenuItem = new JMenuItem();
            editPolicyMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/policy16.gif"));
            editPolicyMenuItem.setIcon(icon);
            editPolicyMenuItem.setText("Edit Policy");
            //int mnemonic = editPolicyMenuItem.getText().toCharArray()[0];
            //editPolicyMenuItem.setMnemonic(mnemonic);
            //editPolicyMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return editPolicyMenuItem;
    }

    private JMenuItem getServicePropertiesMenuItem() {
        if (servicePropertiesMenuItem == null) {
            servicePropertiesMenuItem = new JMenuItem();
            servicePropertiesMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Edit16.gif"));
            servicePropertiesMenuItem.setIcon(icon);
            servicePropertiesMenuItem.setText("Service Properties");
            //int mnemonic = servicePropertiesMenuItem.getText().toCharArray()[0];
            //servicePropertiesMenuItem.setMnemonic(mnemonic);
            //servicePropertiesMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return servicePropertiesMenuItem;
    }

    private JMenuItem getServiceUDDISettingsMenuItem() {
        if (serviceUDDISettingsMenuItem == null) {
            serviceUDDISettingsMenuItem = new JMenuItem();
            serviceUDDISettingsMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Edit16.gif"));
            serviceUDDISettingsMenuItem.setIcon(icon);
            serviceUDDISettingsMenuItem.setText("Publish to UDDI");
            //int mnemonic = servicePropertiesMenuItem.getText().toCharArray()[0];
            //servicePropertiesMenuItem.setMnemonic(mnemonic);
            //servicePropertiesMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return serviceUDDISettingsMenuItem;
    }

    private JMenuItem getDeleteServiceMenuItem() {
        if (deleteServiceMenuItem == null) {
            deleteServiceMenuItem = new JMenuItem();
            deleteServiceMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/delete.gif"));
            deleteServiceMenuItem.setIcon(icon);
            deleteServiceMenuItem.setText("Delete Service");
            //int mnemonic = deleteServiceMenuItem.getText().toCharArray()[0];
            //deleteServiceMenuItem.setMnemonic(mnemonic);
            //deleteServiceMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return deleteServiceMenuItem;
    }

    private JMenuItem getPolicyDiffMenuItem() {
        if (policyDiffMenuItem == null) {
            policyDiffMenuItem = new JMenuItem();
            policyDiffMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/policyDiff16.png"));
            policyDiffMenuItem.setIcon(icon);
        }

        PolicyDiffContext.setPolicyDiffMenuItem(policyDiffMenuItem);

        return policyDiffMenuItem;
    }

    private JMenuItem getValidateMenuItem() {
        if (validateMenuItem == null) {
            validateMenuItem = new JMenuItem();
            validateMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/validate.gif"));
            validateMenuItem.setIcon(icon);
            validateMenuItem.setText("Validate");
            int mnemonic = validateMenuItem.getText().toCharArray()[0];
            validateMenuItem.setMnemonic(mnemonic);
            validateMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return validateMenuItem;
    }

    private JCheckBoxMenuItem getShowInputsAndOutputsMenuItem() {
        if (null == showInputsAndOutputsMenuItem) {
            boolean inputsAndOutputsVisible = getPreferences().isPolicyInputsAndOutputsVisible();

            showInputsAndOutputsMenuItem = new JCheckBoxMenuItem(getInputsAndOutputsToggleAction());

            showInputsAndOutputsMenuItem.setSelected(inputsAndOutputsVisible);
            showInputsAndOutputsMenuItem.setMnemonic(KeyEvent.VK_U);
            showInputsAndOutputsMenuItem.setDisplayedMnemonicIndex(4);
            showInputsAndOutputsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, InputEvent.ALT_DOWN_MASK));
        }

        return showInputsAndOutputsMenuItem;
    }

    private JMenuItem getShowAssertionCommentsMenuItem() {
        if (showAstnCommentsMenuItem == null) {
            showAstnCommentsMenuItem = new JMenuItem();
            showAstnCommentsMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/About16.gif"));
            showAstnCommentsMenuItem.setIcon(icon);
            showAstnCommentsMenuItem.setText("Show Comments");
            showAstnCommentsMenuItem.setMnemonic(KeyEvent.VK_C);
            showAstnCommentsMenuItem.setDisplayedMnemonicIndex(5);
            showAstnCommentsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
        }
        return showAstnCommentsMenuItem;
    }

    private JMenuItem getShowAssertionLineNumbersMenuItem() {
        if (showAstnLnsMenuItem == null) {
            showAstnLnsMenuItem = new JMenuItem();
            showAstnLnsMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/ShowLineNumbers16.png"));
            showAstnLnsMenuItem.setIcon(icon);
            showAstnLnsMenuItem.setText("Show Assertion Numbers");
            showAstnLnsMenuItem.setMnemonic(KeyEvent.VK_N);
            showAstnLnsMenuItem.setDisplayedMnemonicIndex(15);
            showAstnLnsMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
        }
        return showAstnLnsMenuItem;
    }

    private JMenuItem getImportMenuItem() {
        if (importMenuItem == null) {
            importMenuItem = new JMenuItem();
            importMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/saveTemplate.gif"));
            importMenuItem.setIcon(icon);
            importMenuItem.setText("Import Policy");
            int mnemonic = importMenuItem.getText().toCharArray()[0];
            importMenuItem.setMnemonic(mnemonic);
            importMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return importMenuItem;
    }

    private JMenuItem getExportMenuItem() {
        if (exportMenuItem == null) {
            exportMenuItem = new JMenuItem();
            exportMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/saveTemplate.gif"));
            exportMenuItem.setIcon(icon);
            exportMenuItem.setText("Export Policy");
            exportMenuItem.setMnemonic(KeyEvent.VK_R);
            exportMenuItem.setDisplayedMnemonicIndex(4);
            exportMenuItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
        }
        return exportMenuItem;
    }

    public JMenu getFilterServiceAndPolicyTreeMenu() {
        if (filterServiceAndPolicyTreeMenu == null) {
            filterServiceAndPolicyTreeMenu = new JMenu("Filter Service and Policy Tree");
        }
        return filterServiceAndPolicyTreeMenu;
    }

    public JMenu getSortServiceAndPolicyTreeMenu() {
        if (sortServiceAndPolicyTreeMenu == null) {
            sortServiceAndPolicyTreeMenu = new JMenu("Sort Service and Policy Tree By");
        }
        return sortServiceAndPolicyTreeMenu;
    }

    /**
     * @return the preferences bean. never null.
     */
    public SsmPreferences getPreferences() {
        return preferences;
    }

    /**
     * Show an InformationDialog centered on the parent window.
     * <p/>
     * On windows if an InformationDialog is shown as a result of a key press e.g. F3, and the key is held down,
     * this can result in many dialogs being created, which then persist as the focusLost listeners are not invoked
     * as each new dialog is shown. As a result this method ensures that only a single InformationDialog is showing
     * at a time. While a dialog is showing, any further dialogs are ignored.
     * <p/>
     * Only access from the swing thread, if not an IllegalStateException will be thrown
     *
     * @param iDialog InformationDialog to show
     */
    public static void showInformationDialog(final InformationDialog iDialog,
                                             final Runnable continuation) throws IllegalStateException {

        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Cannot invoke showInformationDialog if caller is not on the event dispatching thread");
        }

        if (infoDialogShowing) {
            return;
        }
        infoDialogShowing = true;

        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (continuation != null) continuation.run();
                infoDialogShowing = false;
            }
        };

        iDialog.pack();
        Utilities.centerOnParentWindow(iDialog);
        iDialog.setModal(false);
        DialogDisplayer.display(iDialog, runnable);
    }

    private JMenuItem getSaveAndActivateMenuItem() {
        if (saveAndActivateMenuItem == null) {
            saveAndActivateMenuItem = new JMenuItem();
            saveAndActivateMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Save16.gif"));
            saveAndActivateMenuItem.setIcon(icon);
            saveAndActivateMenuItem.setText("Save and Activate");
            int mnemonic = saveAndActivateMenuItem.getText().toCharArray()[0];
            saveAndActivateMenuItem.setMnemonic(mnemonic);
            saveAndActivateMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK | ActionEvent.CTRL_MASK));
        }
        return saveAndActivateMenuItem;
    }

    private JMenuItem getSaveOnlyMenuItem() {
        if (saveOnlyMenuItem == null) {
            saveOnlyMenuItem = new JMenuItem();
            saveOnlyMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Save16.gif"));
            saveOnlyMenuItem.setIcon(icon);
            saveOnlyMenuItem.setText("Save");
            int mnemonic = saveOnlyMenuItem.getText().toCharArray()[0];
            saveOnlyMenuItem.setMnemonic(mnemonic);
            saveOnlyMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return saveOnlyMenuItem;
    }

    private JMenuItem getMigrateNamespacesMenuItem() {
        if (migrateNamespacesMenuItem != null)
            return migrateNamespacesMenuItem;

        JMenuItem ret = new JMenuItem("Migrate Namespaces");
        ret.setEnabled(false);
        int mnemonic = ret.getText().toCharArray()[0];
        ret.setMnemonic(mnemonic);
        migrateNamespacesMenuItem = ret;

        return ret;
    }

    private BaseAction getCreatePolicyAction() {
        if (createPolicyAction == null) {
            createPolicyAction = new CreatePolicyAction();
            createPolicyAction.setEnabled(false);
        }
        return createPolicyAction;
    }

    private JMenu getEditMenu() {
        if (editMenu == null) {
            JMenu menu = new JMenu();
            menu.setText(resapplication.getString("Edit"));
            menu.setMnemonic(KeyEvent.VK_E);

            ClipboardActions.getGlobalCutAction().putValue(Action.NAME, resapplication.getString("Cut_MenuItem_text"));
            JMenuItem mi;
//            mi = new JMenuItem(ClipboardActions.GLOBAL_CUT_ACTION);
//            menu.add(mi); // TODO  Cut is disabled because it's problematic to have Cut without Undo

            ClipboardActions.getGlobalCopyAction().putValue(Action.NAME, resapplication.getString("Copy_MenuItem_text"));
            mi = new JMenuItem(ClipboardActions.getGlobalCopyAction());
            menu.add(mi);

            ClipboardActions.getGlobalCopyAllAction().putValue(Action.NAME, resapplication.getString("CopyAll_MenuItem_text"));
            mi = new JMenuItem(ClipboardActions.getGlobalCopyAllAction());
            menu.add(mi);

            ClipboardActions.getGlobalPasteAction().putValue(Action.NAME, resapplication.getString("Paste_MenuItem_text"));
            mi = new JMenuItem(ClipboardActions.getGlobalPasteAction());
            menu.add(mi);

            //goto only applies to policy window currently, but could be extended
            menu.add(getGoToMenuItem());

            //search only applies to policy window, but could be extended
            menu.add(getFindMenuItem());
            menu.add(getF3MenuItem());
            menu.add(getShiftF3MenuItem());

            menu.add(getMigrateNamespacesMenuItem());

            editMenu = menu;
        }
        return editMenu;
    }

    private JMenuItem getGoToMenuItem() {
        return new JMenuItem(getGlobalGoToAction());
    }

    private JMenuItem getShiftF3MenuItem() {
        return new JMenuItem(getGlobalShiftF3Action());
    }

    private JMenuItem getF3MenuItem() {
        return new JMenuItem(getGlobalF3Action());
    }

    private JMenuItem getFindMenuItem() {
        return new JMenuItem(getGlobalFindAction());
    }

    private static Action getGlobalGoToAction() {
        if (goToAction == null) {
            goToAction = new ProxyAction(L7_GO_TO, KeyEvent.VK_G, KeyStroke.getKeyStroke(KeyEvent.VK_G, InputEvent.CTRL_MASK));
            goToAction.putValue(Action.NAME, resapplication.getString("Goto_MenuItem_text"));
        }
        return goToAction;
    }

    private static Action getGlobalFindAction() {
        if (findAction == null) {
            findAction = new ProxyAction(L7_FIND, KeyEvent.VK_F, KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_MASK));
            findAction.putValue(Action.NAME, resapplication.getString("Find_MenuItem_text"));
        }
        return findAction;
    }

    private static Action getGlobalF3Action() {
        if (f3Action == null) {
            f3Action = new ProxyAction(L7_F3, KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0));
            f3Action.putValue(Action.NAME, resapplication.getString("F3_MenuItem_text"));
        }
        return f3Action;
    }

    private static Action getGlobalShiftF3Action() {
        if (shiftF3Action == null) {
            shiftF3Action = new ProxyAction(L7_SHIFT_F3, KeyEvent.VK_V, KeyStroke.getKeyStroke(KeyEvent.VK_F3, KeyEvent.SHIFT_MASK));
            shiftF3Action.putValue(Action.NAME, resapplication.getString("Shift_F3_MenuItem_text"));
        }
        return shiftF3Action;
    }

//    private static Action getGlobalEscAction(){
//        if(findAction == null){
//            findAction = new ProxyAction(L7_FIND, KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0));
//            findAction.putValue(Action.NAME, "SHOULD NOT BE SHOWN");
//        }
//        return findAction;
//    }

    /**
     * Proxy Action allows for an Action to configure a menu item in a global menu like the Edit menu, but allows
     * the actual implementation of the Action to be specified by the Component on which it applies.
     * <p/>
     * When the ProxyAction fires it will look up the component with focus, and if it contains an action with the name
     * of the 'actionCommand' property, it will be invoked.
     */
    private static class ProxyAction extends AbstractAction {
        private final String actionCommand;

        public ProxyAction(String actionCommand, int mnemonic, KeyStroke accelerator) {
            super(actionCommand);
            this.actionCommand = actionCommand;
            if (mnemonic > 0) putValue(Action.MNEMONIC_KEY, new Integer(mnemonic));
            if (accelerator != null) putValue(Action.ACCELERATOR_KEY, accelerator);
        }

        public ProxyAction(Action actionToRun, int mnemonic, KeyStroke accelerator) {
            this((String) actionToRun.getValue(Action.NAME),
                    mnemonic,
                    accelerator);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    if (focusOwner == null)
                        return null;

                    Action a = findActionInComponentOrParent(focusOwner, actionCommand);

                    if (a != null) {
                        a.actionPerformed(new ActionEvent(focusOwner,
                                ActionEvent.ACTION_PERFORMED,
                                actionCommand));
                    }
                    return null;
                }
            });
        }
    }

    /**
     * Called when the component with focus changes
     * <p/>
     * Many components are configured via their associated Action e.g. enabled state, text
     * Any such Action whose state is determined by the component with focus should be updated in this method.
     * <p/>
     * Manage the state of actions responsible for goto Ctrl + G, Ctrl + F,  Next search result F3 and previous search
     * result Shift + F3 (search not implemented yet)
     */
    private static void updateActionsDependentOnFocusedComponent() {
        boolean enableGoTo = false;
        boolean enableFind = false;
        boolean enableF3 = false;
        boolean enableShiftF3 = false;
        try {
            if (focusOwner == null)
                return;

            ActionMap am = focusOwner.getActionMap();
            if (am == null)
                return;

            enableGoTo = findActionInComponentOrParent(focusOwner, L7_GO_TO) != null;
            enableFind = findActionInComponentOrParent(focusOwner, L7_FIND) != null;
            enableF3 = findActionInComponentOrParent(focusOwner, L7_F3) != null;
            enableShiftF3 = findActionInComponentOrParent(focusOwner, L7_SHIFT_F3) != null;

        } finally {
            getGlobalGoToAction().setEnabled(enableGoTo);
            getGlobalFindAction().setEnabled(enableFind);
            getGlobalF3Action().setEnabled(enableF3);
            getGlobalShiftF3Action().setEnabled(enableShiftF3);
        }
    }

    private static Action findActionInComponentOrParent(final JComponent comp, final String actionCommand) {

        final ActionMap actionMap = comp.getActionMap();
        Action a = actionMap.get(actionCommand);

        if (a == null) {
            //search through it's parents
            Container parent = comp.getParent();
            while (parent != null) {
                if (!(parent instanceof JComponent)) {
                    break;
                }
                JComponent jParent = (JComponent) parent;
                Action parentAction = jParent.getActionMap().get(actionCommand);
                if (parentAction != null) {
                    a = parentAction;
                    break;
                }

                parent = jParent.getParent();
            }
        }

        return a;
    }

    /**
     * Return the tasksMenu property value.
     * <p/>
     * NOTICE: CHECK THAT THE MENU IS ADDED FOR THE APPLET ALSO.
     * See {@link MainWindow#getToolBarPane()}
     *
     * @return JMenu
     */
    private JMenu getTasksMenu() {
        if (tasksMenu == null) {
            JMenu menu = new JMenu();
            //tasksMenu.setFocusable(false);
            menu.setText(resapplication.getString("Tasks"));

            menu.add(getNewProviderSubMenu());
            menu.add(getNewInternalUserAction());
            menu.add(getNewInternalGroupAction());
            menu.add(getManageAdminUsersSubMenu());
            menu.add(getFindIdentityAction());

            menu.addSeparator();

            menu.add(getPublishServiceAction());
            menu.add(getCreateServiceAction());
            menu.add(getCreatePolicyAction());
            menu.add(getPublishNonSoapServiceAction());
            menu.add(getPublishRestServiceAction());
            menu.add(getPublishInternalServiceAction());
            menu.add(getPublishReverseWebProxyAction());
            menu.addSeparator();

            menu.add(getManageCertificatesMenuItem());
            menu.add(getManagePrivateKeysMenuItem());
            menu.add(getManageSecurePasswordsMenuItem());
            menu.add(getRevokeCertificatesMenuItem());
            menu.add(getManageGlobalResourcesMenuItem());
            menu.add(getManageClusterPropertiesActionMenuItem());
            menu.add(getManageSsgConnectorsAction());
            menu.add(getManageDataSourcesAction());
            menu.add(getManageJmsEndpointsMenuItem());
            menu.add(getManageKerberosMenuItem());
            menu.add(getManageRolesMenuItem());
            menu.add(getManageScheduledTasksAction());
            menu.add(getManageSecurityZonesAction());
            menu.add(getManageAuditAlertOptionsMenuItem());
            menu.add(getManageLogSinksAction());
            menu.add(getManageEmailListenersAction());
            menu.add(getConfigureFtpAuditArchiverAction());
            menu.add(getManageTrustedEsmUsersAction());
            menu.add(getManageUDDIRegistriesAction());
            menu.add(getManageHttpConfigurationAction());
            menu.add(getManageServiceResolutionMenuItem());
            menu.add(getManageEncapsulatedAssertionsAction());
            menu.add(getSiteMinderConfigurationAction());
            menu.add(getManageServerModuleFilesAction());
            menu.add(getManageWorkQueuesAction());
            menu.add(getManageSolutionKitsAction());

            menu.add(getCustomGlobalActionsMenu());

            int mnemonic = menu.getText().toCharArray()[0];
            menu.setMnemonic(mnemonic);

            tasksMenu = menu;
        }
        return tasksMenu;
    }

    private JMenu getManageDataSourcesAction() {
        JMenu dataSourcesMenu = new JMenu("Manage Data Sources");
        dataSourcesMenu.add(getManageJdbcConnectionsAction());
        dataSourcesMenu.add(getManageCassandraConnectionsAction());
        return dataSourcesMenu;
    }

    private JMenuItem getManageAuditAlertOptionsMenuItem() {
        if (manageAuditAlertsMenuItem == null) manageAuditAlertsMenuItem = new JMenuItem(getManageAuditAlertsAction());
        return manageAuditAlertsMenuItem;
    }

    private Action getManageAuditAlertsAction() {
        if (manageAuditAlertsAction == null) {
            manageAuditAlertsAction = AuditAlertOptionsAction.getInstance();
            manageAuditAlertsAction.addAuditWatcher(getAuditAlertBar());
            disableUntilLogin(manageAuditAlertsAction);
        }
        return manageAuditAlertsAction;
    }

    private JMenu getNewProviderSubMenu() {
        if (newProviderSubMenu == null) {
            newProviderSubMenu = new JMenu();
            //newProviderSubMenu.setFocusable(false);

            newProviderSubMenu.setText("Create Identity Provider");
            newProviderSubMenu.setIcon(new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/CreateIdentityProvider16x16.gif")));

            newProviderSubMenu.add(getNewProviderAction());
            newProviderSubMenu.add(getNewBindOnlyLdapProviderAction());
            newProviderSubMenu.add(getNewFederatedIdentityProviderAction());
            newProviderSubMenu.add(getNewPolicyBackedIdentityProviderAction());
        }
        return newProviderSubMenu;
    }

    private JMenu getManageAdminUsersSubMenu() {
        if (manageAdminUsersSubMenu == null) {
            manageAdminUsersSubMenu = new JMenu();

            manageAdminUsersSubMenu.setText("Manage Account Policies");
            manageAdminUsersSubMenu.setIcon(new ImageIcon(ImageCache.getInstance().getIcon("com/l7tech/console/resources/ManageUserAccounts16.png")));

            final SecureAction mangeAdminUsers = new ManageAdminUserAccountAction();
            disableUntilLogin(mangeAdminUsers);
            manageAdminUsersSubMenu.add(mangeAdminUsers);

            final SecureAction forceResetAction = new ForceAdminPasswordResetAction();
            disableUntilLogin(forceResetAction);
            manageAdminUsersSubMenu.add(forceResetAction);

            final SecureAction managePasswdPolicyAction = new IdentityProviderManagePasswordPolicyAction();
            disableUntilLogin(managePasswdPolicyAction);
            manageAdminUsersSubMenu.add(managePasswdPolicyAction);
        }
        return manageAdminUsersSubMenu;
    }

    /**
     * Return the viewMenu property value.
     *
     * @return JMenu
     */
    private JMenu getViewMenu() {
        if (viewMenu != null) return viewMenu;

        JMenu menu = new JMenu();
        //viewMenu.setFocusable(false);
        menu.setText(resapplication.getString("View"));
        JCheckBoxMenuItem jcm = new JCheckBoxMenuItem(getPolicyMessageAreaToggle());

        boolean policyMessageAreaVisible = getPreferences().isPolicyMessageAreaVisible();
        jcm.setSelected(policyMessageAreaVisible);
        menu.add(jcm);

        menu.add(getShowInputsAndOutputsMenuItem());

        if (!isApplet()) {
            jcm = new JCheckBoxMenuItem(getToggleStatusBarToggleAction());
            jcm.setSelected(getPreferences().isStatusBarBarVisible());
            menu.add(jcm);
        }

        menu.add(getShowAssertionCommentsMenuItem());
        menu.add(getShowAssertionLineNumbersMenuItem());

        menu.addSeparator();

        JMenuItem item = new JMenuItem(getRefreshAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        menu.add(item);

        menu.addSeparator();

        menu.add(getDashboardMenuItem());
        menu.add(getAuditMenuItem());
        menu.add(getViewLogMenuItem());
        menu.add(getFromFileMenuItem());

        //Add tree filter menu items
        menu.addSeparator();
        this.addLogonListener(((ServicesAndPoliciesTree) getServicesAndPoliciesTree()).getSortComponents());  //register component to logon listener
        menu.add(((ServicesAndPoliciesTree) getServicesAndPoliciesTree()).getSortComponents().addFilterMenu(getFilterServiceAndPolicyTreeMenu()));
        final int filterMenuIndex = menu.getMenuComponentCount() - 1;
        //Add tree sort filter menu items
        menu.addSeparator();
        menu.add(((ServicesAndPoliciesTree) getServicesAndPoliciesTree()).getSortComponents().addSortMenu(getSortServiceAndPolicyTreeMenu()));
        final int sortMenuIndex = menu.getMenuComponentCount() - 1;

        int mnemonic = menu.getText().toCharArray()[0];
        menu.setMnemonic(mnemonic);

        viewMenu = menu;
        viewMenu.addMenuListener(new MenuListener() {
            @Override
            public void menuCanceled(MenuEvent e) {
            }

            @Override
            public void menuDeselected(MenuEvent e) {
            }

            @Override
            public void menuSelected(MenuEvent e) {
                //when the user clicks sorting options through the root node, it will may change the state and because
                //the view menu will not get refreshed, we need to get the new state of the sorting
                getFilterServiceAndPolicyTreeMenu().removeAll();
                getSortServiceAndPolicyTreeMenu().removeAll();
                viewMenu.add(((ServicesAndPoliciesTree) getServicesAndPoliciesTree()).getSortComponents().addFilterMenu(getFilterServiceAndPolicyTreeMenu()), filterMenuIndex);
                viewMenu.add(((ServicesAndPoliciesTree) getServicesAndPoliciesTree()).getSortComponents().addSortMenu(getSortServiceAndPolicyTreeMenu()), sortMenuIndex);
            }
        });

        return viewMenu;
    }

    /**
     * Return the helpMenu property value.
     *
     * @return JMenu
     */
    private JMenu getHelpMenu() {
        if (helpMenu != null) return helpMenu;

        helpMenu = new JMenu();
        //helpMenu.setFocusable(false);
        helpMenu.setText(resapplication.getString("Help"));
        helpMenu.add(getHelpTopicsMenuItem());
        helpMenu.add(getManageClusterLicensesMenuItem());
        helpMenu.add(new AboutAction());
        int mnemonic = helpMenu.getText().toCharArray()[0];
        helpMenu.setMnemonic(mnemonic);

        return helpMenu;
    }

    private AuditAlertsNotificationPanel getAuditAlertBar() {
        if (auditAlertBar == null) {
            auditAlertBar = new AuditAlertsNotificationPanel(getAuditChecker());
            addLogonListener(auditAlertBar);
        }
        return auditAlertBar;
    }

    private AuditAlertChecker getAuditChecker() {
        if (auditAlertChecker == null) {
            auditAlertChecker = new AuditAlertChecker(new AuditAlertConfigBean(preferences));
        }
        return auditAlertChecker;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the connect <CODE>Action</CODE> implementation
     */
    private Action getConnectAction() {
        if (connectAction != null) return connectAction;
        String atext = resapplication.getString("ConnectMenuItem.name");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/connect2.gif"));
        String aDesc = resapplication.getString("ConnectMenuItem.desc");
        connectAction =
                new AbstractAction(atext, icon) {
                    /**
                     * Invoked when an action occurs.
                     *
                     * @param event the event that occured
                     */
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                if (isApplet()) {
                                    AppletMain applet = (AppletMain) TopComponents.getInstance().getComponent(AppletMain.COMPONENT_NAME);
                                    if (applet == null) {
                                        log.warning("No applet currently attached");
                                        return;
                                    }
                                    String sessionId = applet.getSessionID();
                                    String host = applet.getHostAndPort();
                                    if (sessionId == null || host == null) {
                                        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                                "Session Error",
                                                "An error occurred connecting to the Gateway,\n please disconnect and try again.",
                                                JOptionPane.ERROR_MESSAGE,
                                                null,
                                                null);
                                    } else if (!applet.isValidSessionID(sessionId) ||
                                            !isValidSessionID(sessionId, host)) {
                                        applet.invalidateSessionID();
                                        applet.redirectToServlet();
                                    }
                                } else {
                                    enableOrDisableConnectionComponents(false); // Fixed bug 10238 to disable the connection button or the connection menu item immediately.
                                    LogonDialog.logon(TopComponents.getInstance().getTopParent(), logonListener);
                                }
                            }
                        });
                    }
                };
        connectAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        return connectAction;
    }

    private HomeAction getHomeAction() {
        if (homeAction == null) {
            homeAction = new HomeAction();
        }

        return homeAction;
    }

    /**
     * Enable or disable the connection button in the tool bar and the connection menu item in the main menu.
     *
     * @param enabled: a boolean value to indicate if the connection components will be enabled or not.
     */
    public void enableOrDisableConnectionComponents(boolean enabled) {
        getConnectAction().setEnabled(enabled);
        getConnectMenuItem().setEnabled(enabled);
    }

    /**
     * Check if the current session id is still valid or not.
     *
     * @return true if the session id is valid.
     */
    public boolean isValidSessionID(final String sessionId, final String host) {
        boolean validId = false;

        final SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
        final AuthenticationProvider authProv = securityProvider.getAuthenticationProvider();
        try {
            authProv.login(sessionId, host);
            validId = true;
        } catch (LoginException e) {
            log.log(Level.FINE, "Login failed.", ExceptionUtils.getDebugException(e));
        } catch (VersionException ve) {
            log.log(Level.WARNING, "Login failed due to software version mismatch.", ExceptionUtils.getDebugException(ve));
        }

        if (validId) {
            logonListener.onAuthSuccess(sessionId, false);
        }

        return validId;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the disconnect <CODE>Action</CODE> implementation
     */
    public Action getDisconnectAction() {
        if (disconnectAction != null) return disconnectAction;
        String atext = resapplication.getString("DisconnectMenuItem.name");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/disconnect.gif"));

        String aDesc = resapplication.getString("DisconnectMenuItem.desc");
        disconnectAction =
                new AbstractAction(atext, icon) {
                    /**
                     * Invoked when an action occurs.
                     *
                     * @param event the event that occurred
                     */
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        try {
                            if (isConnected()) {
                                getWorkSpacePanel().updatePolicyTabsProperties();
                            }
                            getWorkSpacePanel().clearWorkspace(); // vetoable

                            // Must disable actions first, since doing so may attempt to make admin calls
                            disconnectFromGateway();

                            SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
                            if (securityProvider != null) securityProvider.logoff();

                            if (isApplet()) {
                                AppletMain applet = (AppletMain) TopComponents.getInstance().getComponent(AppletMain.COMPONENT_NAME);
                                if (applet != null)
                                    applet.invalidateSessionID();
                            }
                        } catch (ActionVetoException e) {
                            // action vetoed
                        }
                    }
                };
        disconnectAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        return disconnectAction;
    }

    private MyAccountAction getMyAccountAction() {
        if (myAccountAction == null) {
            myAccountAction = new MyAccountAction();
        }
        // disable until login
        myAccountAction.setEnabled(false);
        addLogonListener(myAccountAction);
        return myAccountAction;
    }

    /**
     * @return the <code>Action</code> for the publish service
     */
    private PublishServiceAction getPublishServiceAction() {
        if (publishServiceAction != null) {
            return publishServiceAction;
        }
        publishServiceAction = new PublishServiceAction();
        disableUntilLogin(publishServiceAction);
        return publishServiceAction;
    }

    private PublishNonSoapServiceAction getPublishNonSoapServiceAction() {
        if (publishNonSoapServiceAction != null) {
            return publishNonSoapServiceAction;
        }
        publishNonSoapServiceAction = new PublishNonSoapServiceAction();
        disableUntilLogin(publishNonSoapServiceAction);
        return publishNonSoapServiceAction;
    }

    private PublishRestServiceAction getPublishRestServiceAction() {
        if (publishRestServiceAction != null) {
            return publishRestServiceAction;
        }
        publishRestServiceAction = new PublishRestServiceAction();
        disableUntilLogin(publishRestServiceAction);
        return publishRestServiceAction;
    }

    /**
     * @return the <code>Action</code> for the create service
     */
    private CreateServiceWsdlAction getCreateServiceAction() {
        if (createServiceAction != null) {
            return createServiceAction;
        }
        createServiceAction = new CreateServiceWsdlAction();
        disableUntilLogin(createServiceAction);
        return createServiceAction;
    }


    private NewInternalUserAction getNewInternalUserAction() {
        if (newInernalUserAction != null) return newInernalUserAction;
        newInernalUserAction = new NewInternalUserAction(null) {
            @Override
            public String getName() {
                return "Create Internal User";
            }
        };
        MainWindow.this.addLogonListener(newInernalUserAction);
        newInernalUserAction.setEnabled(false);
        addPermissionRefreshListener(newInernalUserAction);
        return newInernalUserAction;
    }

    private NewGroupAction getNewInternalGroupAction() {
        if (newInernalGroupAction != null) return newInernalGroupAction;
        newInernalGroupAction = new NewGroupAction(null) {
            @Override
            public String getName() {
                return "Create Internal Group";
            }
        };
        MainWindow.this.addLogonListener(newInernalGroupAction);
        newInernalGroupAction.setEnabled(false);
        addPermissionRefreshListener(newInernalGroupAction);
        return newInernalGroupAction;
    }

    private NewFederatedIdentityProviderAction getNewFederatedIdentityProviderAction() {
        if (newPKIProviderAction != null) return newPKIProviderAction;
        newPKIProviderAction = new NewFederatedIdentityProviderAction(null) {
            @Override
            public void onLogon(LogonEvent e) {
                super.onLogon(e);
                final DefaultMutableTreeNode root =
                        (DefaultMutableTreeNode) getIdentitiesTree().getModel().getRoot();
                node = (AbstractTreeNode) root;
            }

        };

        MainWindow.this.addLogonListener(newPKIProviderAction);
        newPKIProviderAction.setEnabled(false);
        addPermissionRefreshListener(newPKIProviderAction);
        return newPKIProviderAction;
    }

    private NewLdapProviderAction getNewProviderAction() {
        if (newLDAPProviderAction != null) return newLDAPProviderAction;
        newLDAPProviderAction = new NewLdapProviderAction() {
            @Override
            public void onLogon(LogonEvent e) {
                super.onLogon(e);
                final DefaultMutableTreeNode root =
                        (DefaultMutableTreeNode) getIdentitiesTree().getModel().getRoot();
                node = (AbstractTreeNode) root;
            }
        };
        MainWindow.this.addLogonListener(newLDAPProviderAction);
        newLDAPProviderAction.setEnabled(false);
        addPermissionRefreshListener(newLDAPProviderAction);
        return newLDAPProviderAction;
    }

    private NewBindOnlyLdapProviderAction getNewBindOnlyLdapProviderAction() {
        if (newBindOnlyLdapProviderAction != null) return newBindOnlyLdapProviderAction;
        newBindOnlyLdapProviderAction = new NewBindOnlyLdapProviderAction() {
            @Override
            public void onLogon(LogonEvent e) {
                super.onLogon(e);
                final DefaultMutableTreeNode root =
                        (DefaultMutableTreeNode) getIdentitiesTree().getModel().getRoot();
                node = (AbstractTreeNode) root;
            }
        };
        MainWindow.this.addLogonListener(newBindOnlyLdapProviderAction);
        newBindOnlyLdapProviderAction.setEnabled(false);
        addPermissionRefreshListener(newBindOnlyLdapProviderAction);
        return newBindOnlyLdapProviderAction;
    }

    private NewPolicyBackedIdentityProviderAction getNewPolicyBackedIdentityProviderAction() {
        if (newPolicyBackedIdentityProviderAction != null) return newPolicyBackedIdentityProviderAction;
        newPolicyBackedIdentityProviderAction = new NewPolicyBackedIdentityProviderAction() {
            @Override
            public void onLogon(LogonEvent e) {
                super.onLogon(e);
                final DefaultMutableTreeNode root =
                    (DefaultMutableTreeNode) getIdentitiesTree().getModel().getRoot();
                node = (AbstractTreeNode) root;
            }
        };
        MainWindow.this.addLogonListener(newPolicyBackedIdentityProviderAction);
        newPolicyBackedIdentityProviderAction.setEnabled(false);
        addPermissionRefreshListener(newPolicyBackedIdentityProviderAction);
        return newPolicyBackedIdentityProviderAction;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that refreshes the tree
     */
    private Action getRefreshAction() {
        if (refreshAction != null) return refreshAction;
        String atext = resapplication.getString("Refresh_MenuItem_text");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Refresh16.gif"));

        refreshAction =
                new AbstractAction(atext, icon) {
                    // need to hold a reference to the listener here to prevent GC
                    private LogonListener ll = new LogonListener() {
                        @Override
                        public void onLogon(LogonEvent e) {
                            refreshAction.setEnabled(true);
                        }

                        @Override
                        public void onLogoff(LogonEvent e) {
                            refreshAction.setEnabled(false);
                        }
                    };

                    {
                        addLogonListener(ll);
                    }

                    @Override
                    public void actionPerformed(ActionEvent event) {
                        Collection<Refreshable> alreadyRefreshed = new ArrayList<Refreshable>();
                        // no matter what, if id provider tree exists, always refresh it
                        if (identityProvidersTree != null) {
                            identityProvidersTree.refresh(identitiesRootNode);
                            alreadyRefreshed.add(identityProvidersTree);
                        }
                        // no matter what, if policy templates exist, always refresh it
                        getAssertionPaletteTree().firePropertyChange(PolicyTemplatesFolderNode.REFRESH_POLICY_TEMPLATES, 0, 1);
                        // no matter what, if service tree exists, always refresh it
                        if (servicesAndPoliciesTree != null) {
                            servicesAndPoliciesTree.refresh(rootNode);
                            alreadyRefreshed.add(servicesAndPoliciesTree);
                        }

                        // No matter what, always refresh all policy editor panels
                        final WorkSpacePanel cw = TopComponents.getInstance().getCurrentWorkspace();
                        alreadyRefreshed.addAll(cw.refreshWorkspace());

                        final KeyboardFocusManager kbm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                        final Component c = kbm.getFocusOwner();
                        log.finest("the focus owner is " + (c == null ? "NULL" : c.getClass()));
                        if (c instanceof Refreshable) {

                            try {
                                Refreshable r = (Refreshable) c;
                                if (!alreadyRefreshed.contains(r)) {
                                    if (r.canRefresh()) {
                                        log.finest("Invoke refresh on " + c.getClass());
                                        r.refresh();
                                    }
                                }
                            } finally {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (kbm.getFocusOwner() != c) {
                                            c.requestFocusInWindow();
                                        }
                                    }
                                });
                            }
                        }
                    }
                };
        refreshAction.setEnabled(false);
        refreshAction.putValue(Action.SHORT_DESCRIPTION, atext);
        return refreshAction;
    }

    public void refreshPoliciesFolderNode() {
        servicesAndPoliciesTree.refresh(rootNode);
    }

    public void refreshIdentityProvidersTree() {
        identityProvidersTree.refresh(identitiesRootNode);
    }

    public void clearFilter() {
        AlterFilterAction.applyfilter(AlterFilterAction.FilterType.ALL, getFilterStatusLabel());
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that toggles the status bar
     */
    private Action getToggleStatusBarToggleAction() {
        if (toggleStatusBarAction != null) return toggleStatusBarAction;

        String atext = resapplication.getString("toggle.status.bar.action.name");
        String aDesc = resapplication.getString("toggle.status.bar.action.desc");

        toggleStatusBarAction =
                new AbstractAction(atext) {
                    /**
                     * Invoked when an action occurs.
                     *
                     * @param event the event that occured
                     * @see Action#removePropertyChangeListener(java.beans.PropertyChangeListener)
                     */
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        JCheckBoxMenuItem item = (JCheckBoxMenuItem) event.getSource();
                        final boolean selected = item.isSelected();
                        getStatusBarPane().setVisible(selected);
                        try {
                            SsmPreferences p = preferences;
                            p.seStatusBarVisible(selected);
                            p.store();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                };
        toggleStatusBarAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        return toggleStatusBarAction;
    }

    private Action getInputsAndOutputsToggleAction() {
        if ( togglePolicyInputsAndOutputs != null )
            return togglePolicyInputsAndOutputs;

        String atext = resapplication.getString("toggle.policy.inputsAndOutputs.action.name");
        String aDesc = resapplication.getString("toggle.policy.inputsAndOutputs.action.desc");

        togglePolicyInputsAndOutputs =
            new AbstractAction( atext ) {
                @Override
                public void actionPerformed(ActionEvent event) {
                    JCheckBoxMenuItem item = (JCheckBoxMenuItem) event.getSource();
                    final boolean selected = item.isSelected();

                    final WorkSpacePanel cw = TopComponents.getInstance().getCurrentWorkspace();
                    final WorkSpacePanel.TabbedPane tabbedPane = cw.getTabbedPane();

                    int numTabs = tabbedPane.getTabCount();

                    for (int i = 0; i < numTabs; i++) {
                        Component c = tabbedPane.getComponentAt(i);

                        if (c != null && c instanceof PolicyEditorPanel) {
                            PolicyEditorPanel pe = (PolicyEditorPanel) c;

                            if (null != pe.getPolicyNode().getInterfaceDescription()) {
                                pe.setPolicyInputsAndOutputsVisible(selected);
                                pe.getToolBar().updateToggleInputsAndOutputsButton();
                            }
                        }
                    }

                    try {
                        SsmPreferences p = preferences;
                        p.setPolicyInputsAndOutputsVisible(selected);
                        p.store();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            };

        togglePolicyInputsAndOutputs.putValue( Action.SHORT_DESCRIPTION, aDesc );

        return togglePolicyInputsAndOutputs;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that toggles the  policy message area
     */
    private Action getPolicyMessageAreaToggle() {
        if (togglePolicyMessageArea != null) return togglePolicyMessageArea;

        String atext = resapplication.getString("toggle.policy.msg.action.name");
        String aDesc = resapplication.getString("toggle.policy.msg.action.desc");

        togglePolicyMessageArea =
                new AbstractAction(atext) {
                    /**
                     * Invoked when an action occurs.
                     *
                     * @param event the event that occured
                     * @see Action#removePropertyChangeListener(java.beans.PropertyChangeListener)
                     */
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        JCheckBoxMenuItem item = (JCheckBoxMenuItem) event.getSource();
                        final boolean selected = item.isSelected();
                        final WorkSpacePanel cw = TopComponents.getInstance().getCurrentWorkspace();
                        final JComponent c = cw.getComponent();
                        if (c != null && c instanceof PolicyEditorPanel) {
                            PolicyEditorPanel pe = (PolicyEditorPanel) c;
                            pe.setMessageAreaVisible(selected);
                        }
                        try {
                            SsmPreferences p = preferences;
                            p.setPolicyMessageAreaVisible(selected);
                            p.store();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                };
        togglePolicyMessageArea.putValue(Action.SHORT_DESCRIPTION, aDesc);
        return togglePolicyMessageArea;
    }


    /**
     * create the Action (the component that is used by several
     * controls) that returns the 'find' action.
     *
     * @return the find <CODE>Action</CODE>
     */
    private Action getFindIdentityAction() {
        if (findIdentityAction != null) return findIdentityAction;

        Options options = new Options();
        options.setEnableDeleteAction(true);

        findIdentityAction = new FindIdentityAction(options);
        String aDesc = resapplication.getString("Search_IdentityProvider_MenuItem_text_description");
        findIdentityAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        addPermissionRefreshListener(findIdentityAction);

        return findIdentityAction;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that invokes the
     *         preferences dialog
     */
    private Action getPreferencesAction() {
        if (prefsAction != null) return prefsAction;
        String atext = resapplication.getString("PreferencesMenuItem.name");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/preferences.gif"));
        String aDesc = resapplication.getString("PreferencesMenuItem.desc");
        prefsAction =
                new AbstractAction(atext, icon) {
                    @Override
                    public void actionPerformed(ActionEvent event) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                PreferencesDialog dialog = new PreferencesDialog(TopComponents.getInstance().getTopParent(), true, isApplet());
                                DialogDisplayer.display(dialog);
                            }
                        });
                    }
                };
        prefsAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        return prefsAction;
    }

    /**
     * create the Action (the component that is used by several controls)
     *
     * @return the <CODE>Action</CODE> implementation that invokes the
     *         remove node action
     */
    private Action getRemoveNodeAction() {
        if (removeNodeAction != null) return removeNodeAction;
        String atext = resapplication.getString("Delete_MenuItem_text");
        Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/Delete16.gif"));
        removeNodeAction =
                new AbstractAction(atext, icon) {
                    /**
                     * Invoked when an action occurs.
                     */
                    @Override
                    public void actionPerformed(ActionEvent event) {
                    }
                };
        removeNodeAction.putValue(Action.SHORT_DESCRIPTION, atext);
        removeNodeAction.setEnabled(false);
        return removeNodeAction;
    }

    /**
     * enable or disable menus and buttons, depending on the
     * connection status
     *
     * @param connected true if connected, false otherwise
     */
    private void toggleConnectedMenus(boolean connected) {
        getFindIdentityAction().setEnabled(connected);
        getDisconnectAction().setEnabled(connected);
        getCreatePolicyAction().setEnabled(connected);
        getConnectAction().setEnabled(!connected);
        // these are enabled if connected AND a service is selected in the tree
        if (!connected) {
            getEditPolicyMenuItem().setEnabled(connected);
            getServicePropertiesMenuItem().setEnabled(connected);
            getServiceUDDISettingsMenuItem().setEnabled(connected);
            getDeleteServiceMenuItem().setEnabled(connected);
            getPolicyDiffMenuItem().setEnabled(connected);
        }
        getHomeAction().setEnabled(connected);
    }


    /**
     * Return the JFrameContentPane property value.
     *
     * @return JPanel
     */
    private JPanel getFrameContentPane() {
        if (frameContentPane == null) {
            frameContentPane = new JPanel();
            frameContentPane.setBorder(null);
            frameContentPane.setPreferredSize(new Dimension(700, 600));
            frameContentPane.setLayout(new BorderLayout());
            frameContentPane.add(getToolBarPane(), "North");
            frameContentPane.add(getMainPane(), "Center");
        }
        return frameContentPane;
    }


    /**
     * Return the jJPanelEditor property value.
     *
     * @return JPanel
     */
    private JPanel getMainSplitPaneRight() {
        if (mainSplitPaneRight != null)
            return mainSplitPaneRight;

        mainSplitPaneRight = new JPanel();
        mainSplitPaneRight.setLayout(new GridBagLayout());
        mainSplitPaneRight.setBorder(null);
        addComponentToGridBagContainer(mainSplitPaneRight, getDropComponent());

        return mainSplitPaneRight;
    }

    private WorkSpacePanel getWorkSpacePanel() {
        return TopComponents.getInstance().getCurrentWorkspace();
    }

    private JComponent getDropComponent() {
        JTextPane panel = new JTextPane();
        panel.setEditable(false);
        panel.setBackground(Color.gray);

        panel.setTransferHandler(new FileDropTransferHandler(new FileDropTransferHandler.FileDropListener() {
            @Override
            public boolean acceptFiles(File[] files) {
                boolean accepted = false;
                for (File file : files) {
                    accepted = accepted | getAuditOrLogsFromFileAction().openFile(file);
                }

                return accepted;
            }

            @Override
            public boolean isDropEnabled() {
                return true;
            }
        }, new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                boolean accept = false;
                if (name != null && (name.endsWith(".ssga") || name.endsWith(".ssgl"))) {
                    accept = true;
                }
                return accept;
            }
        }));

        return panel;
    }

    /**
     * Return the palette tree view property value.
     *
     * @return JTree
     */
    private JTree getAssertionPaletteTree() {
        JTree tree = (JTree) TopComponents.getInstance().getComponent(AssertionsTree.NAME);
        if (tree != null)
            return tree;
        tree = new AssertionsTree();
        tree.setShowsRootHandles(true);
        tree.setBorder(null);
        final JTree finalTree = tree;
        tree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) finalTree.getLastSelectedPathComponent();

                String description = null;
                if (selectedNode instanceof AbstractAssertionPaletteNode) {
                    AbstractAssertionPaletteNode apn = (AbstractAssertionPaletteNode) selectedNode;
                    description = apn.getDescriptionText();
                }

                if (selectedNode instanceof CustomAccessControlNode) {
                    CustomAccessControlNode can = (CustomAccessControlNode) selectedNode;
                    description = can.getDescriptionText();
                }

                String contentType = descriptionText.getContentType();

                if (description == null) {
                    description = ""; // clear currently displayed description
                    descriptionText.setContentType("text");
                }
                descriptionText.setText(description);
                descriptionText.getCaret().setDot(0);

                descriptionText.setContentType(contentType);
            }
        });
        TopComponents.getInstance().registerComponent(AssertionsTree.NAME, tree);
        return tree;
    }

    /**
     * Return the identities tree.
     *
     * @return JTree
     */
    private IdentityProvidersTree getIdentitiesTree() {
        IdentityProvidersTree tree = (IdentityProvidersTree) TopComponents.getInstance().getComponent(IdentityProvidersTree.NAME);
        if (tree != null) return tree;
        identityProvidersTree = new IdentityProvidersTree();
        identityProvidersTree.setShowsRootHandles(true);
        identityProvidersTree.setBorder(null);
        TopComponents.getInstance().registerComponent(IdentityProvidersTree.NAME, identityProvidersTree);
        return identityProvidersTree;
    }

    /**
     * Return the JTreeView property value.
     *
     * @return JTree
     */
    private JTree getServicesAndPoliciesTree() {
        JTree tree = (JTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        if (tree != null)
            return tree;

        servicesAndPoliciesTree = new ServicesAndPoliciesTree();
        servicesAndPoliciesTree.initializeSortComponents(getFilterStatusLabel());
        servicesAndPoliciesTree.setShowsRootHandles(true);
        servicesAndPoliciesTree.setRootVisible(true);
        TopComponents.getInstance().registerComponent(ServicesAndPoliciesTree.NAME, servicesAndPoliciesTree);
        servicesAndPoliciesTree.addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
                boolean enable = servicesAndPoliciesTree.getSmartSelectedNodes().size() == 1;
                boolean soap = false;
                if (enable) {
                    Object pathItem = servicesAndPoliciesTree.getSelectionModel().getSelectionPaths()[0].getLastPathComponent();
                    if (!(pathItem instanceof ServiceNode)) {
                        enable = false;
                    } else {
                        soap = ((ServiceNode) pathItem).getEntityHeader().isSoap();
                    }
                }
                getEditPolicyMenuItem().setEnabled(enable);
                getServicePropertiesMenuItem().setEnabled(enable);
                getServiceUDDISettingsMenuItem().setEnabled(enable && soap);
                getDeleteServiceMenuItem().setEnabled(enable);
                getPolicyDiffMenuItem().setEnabled(enable);
                if (enable) {
                    // go get the actions from the node
                    ServiceNode node = (ServiceNode) (servicesAndPoliciesTree.getSelectionModel().getSelectionPaths()[0].getLastPathComponent());
                    getEditPolicyMenuItem().setAction(new EditPolicyAction(node));
                    getServicePropertiesMenuItem().setAction(new EditServiceProperties(node));
                    if (soap) getServiceUDDISettingsMenuItem().setAction(new EditServiceUDDISettingsAction(node));
                    getDeleteServiceMenuItem().setAction((node instanceof ServiceNodeAlias) ? new DeleteServiceAliasAction((ServiceNodeAlias) node) : new DeleteServiceAction(node));
                    getPolicyDiffMenuItem().setAction(new DiffPolicyAction(node));
                }
            }
        });
        return servicesAndPoliciesTree;
    }

    public void setServiceUrl(String url) {
        serviceUrl = url;
    }

    /**
     * @return The gateway's name that the manager is connecting to.
     */
    private String getServiceUrl() {
        String url;
        if (serviceUrl != null) {
            url = serviceUrl;
        } else {
            url = preferences.getString(SsmPreferences.SERVICE_URL);
        }
        return url;
    }

    /**
     * Initialize the workspace. Invoked on successful login.
     */
    private void initializeWorkspace() {
        DefaultTreeModel treeModel = new FilteredTreeModel(null);
        final AbstractTreeNode paletteRootNode = new AssertionsPaletteRootNode("Policy Assertions");
        treeModel.setRoot(paletteRootNode);
        final JTree assertionPaletteTree = getAssertionPaletteTree();
        assertionPaletteTree.setRootVisible(true);
        assertionPaletteTree.setModel(treeModel);
        TreePath path = new TreePath(paletteRootNode.getPath());
        assertionPaletteTree.setSelectionPath(path);
        assertionPaletteTree.getModel().addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                getAssertionSearchComboBox().updateSearchableItems(getAllSearchablePaletteNodes());
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                getAssertionSearchComboBox().updateSearchableItems(getAllSearchablePaletteNodes());
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
                getAssertionSearchComboBox().updateSearchableItems(getAllSearchablePaletteNodes());
            }

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                getAssertionSearchComboBox().updateSearchableItems(getAllSearchablePaletteNodes());
            }
        });
        getAssertionSearchComboBox().updateSearchableItems(getAllSearchablePaletteNodes());

        identitiesRootNode = new IdentitiesRootNode("Identity Providers");
        treeModel = new FilteredTreeModel(null);
        treeModel.setRoot(identitiesRootNode);
        getIdentitiesTree().setRootNode(identitiesRootNode);

        final JTree identitiesTree = getIdentitiesTree();
        identitiesTree.setRootVisible(true);
        identitiesTree.setModel(treeModel);

        final String url = getServiceUrl();
        try {
            final Folder rootFolder = Registry.getDefault().getFolderAdmin().findByPrimaryKey(Folder.ROOT_FOLDER_ID);
            if (rootFolder != null) {
                rootNode = new RootNode(url, new FolderHeader(rootFolder));
            } else {
                log.log(Level.WARNING, "Root folder not found with goid " + Folder.ROOT_FOLDER_ID);
                rootNode = new RootNode(url);
            }

        } catch (final FindException | PermissionDeniedException e) {
            log.log(Level.WARNING, "Unable to retrieve root folder: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            rootNode = new RootNode(url);
        }
        rootNode.setSortComponents(((ServicesAndPoliciesTree) getServicesAndPoliciesTree()).getSortComponents());

        DefaultTreeModel servicesTreeModel = new FilteredTreeModel(null);
        servicesTreeModel.setRoot(rootNode);
        getServicesAndPoliciesTree().setModel(servicesTreeModel);
        servicesTreeModel.addTreeModelListener(new TreeModelListener() {
            @Override
            public void treeNodesChanged(TreeModelEvent e) {
                getSearchComboBox().updateSearchableItems(getAllSearchableServiceAndPolicyNodes());
            }

            @Override
            public void treeNodesInserted(TreeModelEvent e) {
                getSearchComboBox().updateSearchableItems(getAllSearchableServiceAndPolicyNodes());
            }

            @Override
            public void treeNodesRemoved(TreeModelEvent e) {
                getSearchComboBox().updateSearchableItems(getAllSearchableServiceAndPolicyNodes());
            }

            @Override
            public void treeStructureChanged(TreeModelEvent e) {
                getSearchComboBox().updateSearchableItems(getAllSearchableServiceAndPolicyNodes());
            }
        });
        getSearchComboBox().updateSearchableItems(getAllSearchableServiceAndPolicyNodes());

        getServicesAndPoliciesTree().setShowsRootHandles(true);
        getServicesAndPoliciesTree().setRootVisible(true);

        // disable items that depend on serivces and policies tree selection
        getEditPolicyMenuItem().setEnabled(false);
        getServicePropertiesMenuItem().setEnabled(false);
        getServiceUDDISettingsMenuItem().setEnabled(false);
        getDeleteServiceMenuItem().setEnabled(false);
        getPolicyDiffMenuItem().setEnabled(false);

        TreeSelectionListener treeSelectionListener =
                new TreeSelectionListener() {
                    private final JTree assertionPalette =
                            assertionPaletteTree;
                    private final JTree services = getServicesAndPoliciesTree();

                    @Override
                    public void valueChanged(TreeSelectionEvent e) {
                        Object o = e.getSource();
                        if (o == assertionPalette) {
                            if (!isRemovePath(e)) {
                                log.finer("Clearing selection services");
                                services.clearSelection();
                            }
                        } else if (o == services) {
                            if (!isRemovePath(e)) {
                                log.finer("Clearing selection assertions palette");
                                assertionPalette.clearSelection();
                            }
                        } else {
                            log.warning("Received unexpected selection path from " + o.getClass());
                        }
                    }

                    private boolean isRemovePath(TreeSelectionEvent e) {
                        TreePath[] paths = e.getPaths();
                        for (int i = 0; i < paths.length; i++) {
                            if (!e.isAddedPath(i)) {
                                return true;
                            }
                        }
                        return false;
                    }

                };
        getServicesAndPoliciesTree().addTreeSelectionListener(treeSelectionListener);
        assertionPaletteTree.addTreeSelectionListener(treeSelectionListener);

        getMainSplitPaneRight().removeAll();
        addComponentToGridBagContainer(getMainSplitPaneRight(), getWorkSpacePanel());

        Registry.getDefault().getLicenseManager().addLicenseListener(paletteTreeLicenseListener);

        updateCustomGlobalActionsMenu();
    }

    private void updateCustomGlobalActionsMenu() {
        JMenu menu = getCustomGlobalActionsMenu();
        menu.removeAll();

        List<Action> menuActions = new ArrayList<>();

        boolean added = false;
        Set<Assertion> assertions = TopComponents.getInstance().getAssertionRegistry().getAssertions();
        for (Assertion assertion : assertions) {
            if (Registry.getDefault().getLicenseManager().isAssertionEnabled(assertion)) {
                try {
                    Action[] actions = assertion.meta().get(AssertionMetadata.GLOBAL_ACTIONS);
                    if (actions != null) {
                        menuActions.addAll(Arrays.asList(actions));
                    }
                } catch (Exception e) {
                    log.log(Level.WARNING, "Exception while initializing global actions for assertion " + assertion.toString() + ": " + ExceptionUtils.getMessage(e), e);
                }
            }
        }

        // Add actions from custom assertions.
        //
        menuActions.addAll(this.getCustomAssertionActions());

        // Expose policy backed services if enabled
        if ( haveAnyMultiMethodPolicyBackedServices() ) {
            menuActions.add( getManagePolicyBackedServicesAction() );
        }

        // sort actions before sticking them into Additional Actions menu
        // so they'll appear in the same order
        Collections.sort(menuActions, new ActionComparator());
        //now add actions to the menu
        for (Action action : menuActions) {
            menu.add(action);
            added = true;
        }

        menu.setEnabled(added);
    }

    private boolean haveAnyMultiMethodPolicyBackedServices() {
        try {
            if ( Registry.getDefault().isAdminContextPresent() ) {
                return Registry.getDefault().getPolicyBackedServiceAdmin().isAnyMultiMethodPolicyBackedServiceRegistered();
            }
        } catch ( Exception e ) {
            log.log( Level.INFO, "Unable to check for policy backed services: " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
        }
        return false;
    }

    private List<Action> getCustomAssertionActions() {
        // This method is called twice on login by this.updateCustomGlobalActionsMenu(...):
        //  - once by LicenseListener.licenseChanged(...) before ConsoleAssertionRegistry.updateCustomAssertions()
        //  - then again by this.initializeWorkspace(...) after updateCustomAssertions.updateCustomAssertions().

        List<Action> customAssertionActions = new ArrayList<>();
        ConsoleLicenseManager consoleLicenseManager = Registry.getDefault().getLicenseManager();
        CustomAssertionsRegistrar registrar = Registry.getDefault().getCustomAssertionsRegistrar();
        for (CustomAssertionHolder customAssertionHolder : TopComponents.getInstance().getAssertionRegistry().getCustomAssertions()) {
            if (consoleLicenseManager.isAssertionEnabled(customAssertionHolder)) {
                // over the wire call to Gateway (there's possible performance improvement here)
                CustomTaskActionUI taskActionUI = registrar.getTaskActionUI(customAssertionHolder.getCustomAssertion().getClass().getName());
                if (taskActionUI != null) {
                    customAssertionActions.add(new CustomAssertionHolderAction(taskActionUI));
                }
            }
        }

        return customAssertionActions;
    }

    /**
     * Compares actions based on the classname. Used in sort function.
     */
    private static class ActionComparator implements Comparator<Action> {
        @Override
        public int compare(Action a1, Action a2) {
            return a1.getClass().getName().compareTo(a2.getClass().getName());
        }
    }
    /**
     * Return the MainJMenuBar property value.
     *
     * @return JMenuBar
     */
    private JMenuBar getMainJMenuBar() {
        if (mainJMenuBar == null) {
            mainJMenuBar = new JMenuBar();
            //mainJMenuBar.setFocusable(false);
            mainJMenuBar.add(getFileMenu());
            mainJMenuBar.add(getEditMenu());
            mainJMenuBar.add(getTasksMenu());
            mainJMenuBar.add(getViewMenu());
            //           mainJMenuBar.add(getWindowMenu());
            mainJMenuBar.add(getHelpMenu());
            Utilities.removeToolTipsFromMenuItems(mainJMenuBar);
        }
        return mainJMenuBar;
    }


    private Action getManageJmsEndpointsAction() {
        if (manageJmsEndpointsAction != null)
            return manageJmsEndpointsAction;


        manageJmsEndpointsAction = new ManageJmsEndpointsAction();
        disableUntilLogin(manageJmsEndpointsAction);
        return manageJmsEndpointsAction;
    }

    private Action getManageKerberosAction() {
        if (manageKerberosAction != null)
            return manageKerberosAction;


        manageKerberosAction = new ManageKerberosAction();
        disableUntilLogin(manageKerberosAction);
        return manageKerberosAction;
    }

    private Action getManageCertificatesAction() {
        if (manageCertificatesAction != null)
            return manageCertificatesAction;

        manageCertificatesAction = new ManageCertificatesAction();
        disableUntilLogin(manageCertificatesAction);
        return manageCertificatesAction;
    }

    private Action getManageSsgConnectorsAction() {
        if (manageSsgConnectorsAction != null)
            return manageSsgConnectorsAction;

        manageSsgConnectorsAction = new ManageSsgConnectorsAction();
        disableUntilLogin(manageSsgConnectorsAction);
        return manageSsgConnectorsAction;
    }

    private Action getManageEncapsulatedAssertionsAction() {
        if (manageEncapsulatedAssertionsAction == null) {
            manageEncapsulatedAssertionsAction = new ManageEncapsulatedAssertionsAction();
            disableUntilLogin(manageEncapsulatedAssertionsAction);
        }
        return manageEncapsulatedAssertionsAction;
    }

    private Action getManagePolicyBackedServicesAction() {
        if ( managePolicyBackedServicesAction == null ) {
            managePolicyBackedServicesAction = new ManagePolicyBackedServicesAction();
            disableUntilLogin( managePolicyBackedServicesAction );
        }
        return managePolicyBackedServicesAction;
    }

    private Action getManageServerModuleFilesAction() {
        if ( manageServerModuleFilesAction == null ) {
            manageServerModuleFilesAction = new ManageServerModuleFilesAction();
            disableUntilLogin( manageServerModuleFilesAction );
        }
        return manageServerModuleFilesAction;
    }

    private Action getManageWorkQueuesAction() {
        if (manageWorkQueuesAction == null) {
            manageWorkQueuesAction = new ManageWorkQueuesAction();
            disableUntilLogin(manageWorkQueuesAction);
        }
        return manageWorkQueuesAction;
    }

    private Action getManageSecurityZonesAction() {
        if (manageSecurityZonesAction == null) {
            manageSecurityZonesAction = new ManageSecurityZonesAction();
            disableUntilLogin(manageSecurityZonesAction);
        }
        return manageSecurityZonesAction;
    }

    private Action getManageUDDIRegistriesAction() {
        if (manageUDDIRegistriesAction != null) return manageUDDIRegistriesAction;

        manageUDDIRegistriesAction = new ManageUDDIRegistriesAction();
        disableUntilLogin(manageUDDIRegistriesAction);
        return manageUDDIRegistriesAction;
    }

    private Action getManageHttpConfigurationAction() {
        if (manageHttpConfigurationAction != null) return manageHttpConfigurationAction;

        manageHttpConfigurationAction = new ManageHttpConfigurationAction();
        disableUntilLogin(manageHttpConfigurationAction);
        return manageHttpConfigurationAction;
    }

    private Action getManageScheduledTasksAction() {
        if (manageScheduledTasksAction != null)
            return manageScheduledTasksAction;

        manageScheduledTasksAction = new ManageScheduledTasksAction();
        disableUntilLogin(manageScheduledTasksAction);
        return manageScheduledTasksAction;
    }

    private Action getManageJdbcConnectionsAction() {
        if (manageJdbcConnectionsAction != null)
            return manageJdbcConnectionsAction;

        manageJdbcConnectionsAction = new ManageJdbcConnectionsAction();
        disableUntilLogin(manageJdbcConnectionsAction);
        return manageJdbcConnectionsAction;
    }

    private Action getManageCassandraConnectionsAction() {
        if(manageCassandraConnectionAction != null) {
            return manageCassandraConnectionAction;
        }

        manageCassandraConnectionAction = new ManageCassandraConnectionAction();
        disableUntilLogin(manageCassandraConnectionAction);
        return manageCassandraConnectionAction;
    }

    private Action getSiteMinderConfigurationAction() {
        if (manageSiteMinderConfigurationAction != null)
            return manageSiteMinderConfigurationAction;

        manageSiteMinderConfigurationAction = new ManageSiteMinderConfigurationAction();
        disableUntilLogin(manageSiteMinderConfigurationAction);
        return manageSiteMinderConfigurationAction;
    }

    private Action getManageTrustedEsmUsersAction() {
        if (manageTrustedEsmUsersAction != null)
            return manageTrustedEsmUsersAction;

        SecureAction action = new ManageTrustedEsmUsersAction();
        disableUntilLogin(action);
        return manageTrustedEsmUsersAction = (ManageTrustedEsmUsersAction) action;
    }

    private Action getManageSolutionKitsAction() {
        if (manageSolutionKitsAction != null)
            return manageSolutionKitsAction;

        manageSolutionKitsAction = new ManageSolutionKitsAction();
        disableUntilLogin(manageSolutionKitsAction);
        return manageSolutionKitsAction;
    }

    private JMenu getCustomGlobalActionsMenu() {
        if (customGlobalActionsMenu != null)
            return customGlobalActionsMenu;

        final JMenu menu = new JMenu("Additional Actions"){
            private final LicenseListener listener = new LicenseListener() {
                    @Override
                    public void licenseChanged( final ConsoleLicenseManager licenseManager ) {
                        if ( licenseManager.getLicense() != null )
                            updateCustomGlobalActionsMenu();
                    }
                };
            {
                Registry.getDefault().getLicenseManager().addLicenseListener( listener );
            }
        };

        return customGlobalActionsMenu = menu;
    }

    private void disableUntilLogin(SecureAction action) {
        action.setEnabled(false);
        this.addLogonListener(action);
        addPermissionRefreshListener(action);
    }

    private Action getManageLogSinksAction() {
        if (manageLogSinksAction != null)
            return manageLogSinksAction;

        manageLogSinksAction = new ManageLogSinksAction();
        disableUntilLogin(manageLogSinksAction);
        return manageLogSinksAction;
    }

    private Action getManageEmailListenersAction() {
        if (manageEmailListenersAction != null)
            return manageEmailListenersAction;

        manageEmailListenersAction = new ManageEmailListenersAction();
        disableUntilLogin(manageEmailListenersAction);
        return manageEmailListenersAction;
    }

    private Action getConfigureFtpAuditArchiverAction() {
        if (configureFtpAuditArchiver != null)
            return configureFtpAuditArchiver;

        configureFtpAuditArchiver = new ConfigureFtpAuditArchiverAction();
        disableUntilLogin(configureFtpAuditArchiver);
        return configureFtpAuditArchiver;
    }

    private Action getManagePrivateKeysAction() {
        if (managePrivateKeysAction != null)
            return managePrivateKeysAction;

        managePrivateKeysAction = new ManagePrivateKeysAction();
        disableUntilLogin(managePrivateKeysAction);
        return managePrivateKeysAction;
    }

    private Action getManageSecurePasswordsAction() {
        if (manageSecurePasswordsAction != null)
            return manageSecurePasswordsAction;

        manageSecurePasswordsAction = new ManageSecurePasswordsAction();
        disableUntilLogin(manageSecurePasswordsAction);
        return manageSecurePasswordsAction;
    }

    private Action getRevokeCertificatesAction() {
        if (revokeCertificatesAction != null)
            return revokeCertificatesAction;

        revokeCertificatesAction = new RevokeCertificatesAction();
        disableUntilLogin(revokeCertificatesAction);
        return revokeCertificatesAction;
    }

    private Action getManageGlobalResourcesAction() {
        if (manageGlobalResourcesAction != null) return manageGlobalResourcesAction;
        manageGlobalResourcesAction = new ManageGlobalResourcesAction();
        disableUntilLogin(manageGlobalResourcesAction);
        return manageGlobalResourcesAction;
    }

    private Action getManageClusterPropertiesAction() {
        if (manageClusterPropertiesAction != null) return manageClusterPropertiesAction;
        manageClusterPropertiesAction = new ManageClusterPropertiesAction();
        disableUntilLogin(manageClusterPropertiesAction);
        return manageClusterPropertiesAction;
    }

    private Action getShowDashboardAction() {
        if (showDashboardAction != null) return showDashboardAction;
        showDashboardAction = new ShowDashboardAction();
        disableUntilLogin(showDashboardAction);
        return showDashboardAction;
    }

    private Action getManagerClusterLicensesAction() {
        if (manageClusterLicensesAction != null) return manageClusterLicensesAction;
        manageClusterLicensesAction = new ManageClusterLicensesAction();
        disableUntilLogin(manageClusterLicensesAction);
        return manageClusterLicensesAction;
    }

    private Action getGatewayAuditWindowAction() {
        if (viewGatewayAuditsWindowAction != null) return viewGatewayAuditsWindowAction;
        viewGatewayAuditsWindowAction = new ViewGatewayAuditsAction() {
            @Override
            protected void performAction() {
                AuditAlertsNotificationPanel auditAlert = getAuditAlertBar();
                if (auditAlert != null)
                    auditAlert.auditsViewed();

                super.performAction();
            }
        };
        disableUntilLogin(viewGatewayAuditsWindowAction);
        return viewGatewayAuditsWindowAction;
    }

    private Action getViewLogsAction() {
        if (viewLogsAction != null) return viewLogsAction;
        viewLogsAction = new ViewLogsAction();
        disableUntilLogin(viewLogsAction);
        return viewLogsAction;
    }

    private ViewAuditsFromFileAction getAuditOrLogsFromFileAction() {
        if (auditOrLogFromFileAction != null) return auditOrLogFromFileAction;
        auditOrLogFromFileAction = new ViewAuditsFromFileAction();
        return auditOrLogFromFileAction;
    }

    /**
     * Return the mainJSplitPaneTop property value.
     *
     * @return JSplitPane
     */
    private JSplitPane getMainSplitPane() {
        if (mainSplitPane != null)
            return mainSplitPane;


        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        mainSplitPane.add(getMainSplitPaneRight(), "right");
        mainSplitPane.add(getMainLeftPanel(), "left");
        mainSplitPane.setDividerSize(4);
        mainSplitPane.setBorder(null);
        getMainLeftPanel().addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                preferredHorizontalSplitLocation = mainSplitPane.getDividerLocation() / (double) (mainSplitPane.getWidth() - mainSplitPane.getDividerSize());
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                preferredHorizontalSplitLocation =
                        setSplitLocation("main.split.divider.location",
                                preferredHorizontalSplitLocation,
                                mainSplitPane);
            }
        });

        return mainSplitPane;
    }

    /**
     * Return the MainPane property value.
     *
     * @return JPanel
     */
    private JPanel getMainPane() {
        if (mainPane == null) {
            mainPane = new JPanel();
            mainPane.setBorder(null);
            mainPane.setLayout(new BorderLayout());
            getMainPane().add(getMainSplitPane(), "Center");
        }
        return mainPane;
    }

    /**
     * Return the StatusBarPane property value.
     *
     * @return JPanel
     */
    private JPanel getStatusBarPane() {
        if (statusBarPane == null) {
            statusBarPane = new JPanel();
            statusBarPane.setLayout(new BorderLayout());
            statusBarPane.setDoubleBuffered(true);
            Border border =
                    BorderFactory.
                            createCompoundBorder(getStatusMsgLeft().getBorder(),
                                    BorderFactory.createEmptyBorder(2, 2, 2, 2));

            getStatusMsgLeft().setBorder(border);

            getStatusBarPane().add(getStatusMsgLeft(), BorderLayout.WEST);
            JPanel rightPanel = new JPanel();
            rightPanel.setLayout(new BorderLayout());

            getStatusMsgRight().setBorder(border);
            rightPanel.add(getStatusMsgRight(), BorderLayout.WEST);
            ProgressBar progressBar = (ProgressBar) TopComponents.getInstance().getComponent(ProgressBar.NAME);
            if (progressBar == null) {
                progressBar = new ProgressBar(0, 100, 20);
                TopComponents.getInstance().registerComponent(ProgressBar.NAME, progressBar);
            }

            // a bit of a hack here , set the size to the size of "disconnected" label
            progressBar.setPreferredSize(getStatusMsgLeft().getPreferredSize());
            progressBar.setMaximumSize(getStatusMsgLeft().getMaximumSize());

            progressBar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

            // rightPanel.add(progressBar, BorderLayout.EAST);
            getStatusBarPane().add(rightPanel, BorderLayout.EAST);
            SsmPreferences p = preferences;
            statusBarPane.setVisible(p.isStatusBarBarVisible());
        }
        return statusBarPane;
    }

    /**
     * Return the StatusMsgLeft property value.
     *
     * @return JLabel
     */
    private JLabel getStatusMsgLeft() {
        if (statusMsgLeft == null) {
            statusMsgLeft = new JLabel();
            statusMsgLeft.setText("Disconnected");
        }
        return statusMsgLeft;
    }

    /**
     * Return the StatusMsgRight property value.
     *
     * @return JLabel
     */
    private JLabel getStatusMsgRight() {
        if (statusMsgRight == null) {
            statusMsgRight = new JLabel();
        }
        return statusMsgRight;
    }

    /**
     * Add an action button to a toolbar.
     *
     * @param tb the toolbar
     * @param a  the action
     * @return the new button
     */
    private JButton tbadd(JToolBar tb, Action a) {
        JButton b = tb.add(a);
        b.setFont(new Font("Dialog", 1, 10));
        b.setText((String) a.getValue(Action.NAME));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setHorizontalTextPosition(SwingConstants.RIGHT);
        b.setFocusable(false);
        return b;
    }

    /**
     * Add a popup menu button to a toolbar.
     *
     * @param tb           the toolbar
     * @param menu         the popup menu
     * @param iconResource resource path of icon for menu button
     */
    private void tbadd(JToolBar tb, final JPopupMenu menu, String iconResource) {
        final JButton[] but = new JButton[]{null};
        final Action showAction = new AbstractAction(menu.getLabel()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                menu.show(but[0], 0, but[0].getHeight());
            }
        };
        if (iconResource != null) {
            URL loc = cl.getResource(iconResource);
            if (loc != null) showAction.putValue(Action.SMALL_ICON, new ImageIcon(loc));
        }
        but[0] = tbadd(tb, showAction);
    }

    /**
     * Return the ToolBarPane property value.
     *
     * @return JToolBar
     */
    private JToolBar getToolBarPane() {
        if (toolBarPane != null) return toolBarPane;

        toolBarPane = new JToolBar();
        toolBarPane.setFloatable(false);
        toolBarPane.setFocusable(false);
        toolBarPane.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
        toolBarPane.setFloatable(false);

        tbadd(toolBarPane, getConnectAction());
        tbadd(toolBarPane, getDisconnectAction());
        tbadd(toolBarPane, getRefreshAction());
        tbadd(toolBarPane, getHomeAction());

        if (isApplet()) {
            // Ensure that clipboard actions get initialized properly, even though we won't display this menu
            // (side effects, hack hack)
            getEditMenu();

            if (appletManagePopUpMenu == null) {
                JPopupMenu manageMenu = new JPopupMenu("Manage...");
                manageMenu.add(getManageAdminUsersSubMenu());
                manageMenu.addSeparator();

                manageMenu.add(getMenuItemPreferences(false));
                manageMenu.add(getManageCertificatesMenuItem());
                manageMenu.add(getManagePrivateKeysMenuItem());
                manageMenu.add(getManageSecurePasswordsMenuItem());
                manageMenu.add(getManageGlobalResourcesMenuItem());
                manageMenu.add(getManageClusterPropertiesActionMenuItem());
                manageMenu.add(getManageSsgConnectorsAction());
                manageMenu.add(getManageDataSourcesAction());
                manageMenu.add(getManageJmsEndpointsMenuItem());
                manageMenu.add(getManageKerberosMenuItem());
                manageMenu.add(getManageRolesMenuItem());
                manageMenu.add(getManageScheduledTasksAction());
                manageMenu.add(getManageSecurityZonesAction());
                manageMenu.add(getManageAuditAlertOptionsMenuItem());
                manageMenu.add(getManageClusterLicensesMenuItem());
                manageMenu.add(getMyAccountMenuItem(false));
                manageMenu.add(getManageLogSinksAction());
                manageMenu.add(getManageEmailListenersAction());
                manageMenu.add(getConfigureFtpAuditArchiverAction());
                manageMenu.add(getManageTrustedEsmUsersAction());
                manageMenu.add(getManageUDDIRegistriesAction());
                manageMenu.add(getManageHttpConfigurationAction());
                manageMenu.add(getManageServiceResolutionMenuItem());
                manageMenu.add(getManageEncapsulatedAssertionsAction());
                manageMenu.add(getSiteMinderConfigurationAction());
                manageMenu.add(getManageServerModuleFilesAction());
                manageMenu.add(getManageWorkQueuesAction());
                manageMenu.add(getManageSolutionKitsAction());

                manageMenu.add(getCustomGlobalActionsMenu());
                appletManagePopUpMenu = manageMenu;
            }

            updateTopMenu(appletManagePopUpMenu);
            Utilities.removeToolTipsFromMenuItems(appletManagePopUpMenu);
            tbadd(toolBarPane, appletManagePopUpMenu, RESOURCE_PATH + "/Properties16.gif");

            JPopupMenu menu = new JPopupMenu("Monitor...");
            menu.add(getDashboardMenuItem());
            menu.add(getAuditMenuItem());
            menu.add(getViewLogMenuItem());
            menu.add(getFromFileMenuItem());
            Utilities.removeToolTipsFromMenuItems(menu);
            tbadd(toolBarPane, menu, RESOURCE_PATH + "/AnalyzeGatewayLog16x16.gif");

            menu = new JPopupMenu("Edit");
            menu.add(getGoToMenuItem());
            menu.add(getFindMenuItem());
            menu.add(getF3MenuItem());
            menu.add(getShiftF3MenuItem());
            menu.add(getMigrateNamespacesMenuItem());

            Utilities.removeToolTipsFromMenuItems(menu);
            tbadd(toolBarPane, menu, null);

            menu = new JPopupMenu("Help...");
            menu.add(getHelpTopicsMenuItem());
            JCheckBoxMenuItem jcm = new JCheckBoxMenuItem(getPolicyMessageAreaToggle());
            jcm.setSelected(getPreferences().isPolicyMessageAreaVisible());
            menu.add(jcm);
            menu.add(new AboutAction());
            Utilities.removeToolTipsFromMenuItems(menu);
            tbadd(toolBarPane, menu, RESOURCE_PATH + "/About16.gif");
        } else {
            tbadd(toolBarPane, getPreferencesAction());
        }

        toolBarPane.add(Box.createHorizontalGlue());
        toolBarPane.add(getAuditAlertBar());

        return toolBarPane;
    }

    /**
     * @return true if we are running as an Applet
     */
    public boolean isApplet() {
        return ssmApplication.isApplet();
    }

    /**
     * @return true if we are running trusted
     */
    public boolean isTrusted() {
        return ssmApplication.isTrusted();
    }

    /**
     * Return the ToolBarPane property value.
     *
     * @return JToolBar
     */
    public PolicyToolBar getPolicyToolBar() {
        if (policyToolBar != null) return policyToolBar;
        policyToolBar = new PolicyToolBar();
        policyToolBar.setFloatable(false);
        policyToolBar.registerPaletteTree(getAssertionPaletteTree());
        addLogonListener(policyToolBar);
        return policyToolBar;
    }

    public void setPolicyToolBar(PolicyToolBar policyToolBar) {
        this.policyToolBar = policyToolBar;
    }

    private void configureScrollPane(JScrollPane js) {
        // Not sure what this increment stuff is for, so removing for now.

        // default increment
        //int mInc = js.getVerticalScrollBar().getUnitIncrement();

        // some arbitrary text to set the unit increment to the
        // height of one line instead of default value
        //int vInc = (int)getStatusMsgLeft().getPreferredSize().getHeight();
        //js.getVerticalScrollBar().setUnitIncrement(Math.max(mInc, vInc));

        //int hInc = (int)getStatusMsgLeft().getPreferredSize().getWidth();
        //js.getHorizontalScrollBar().setUnitIncrement(Math.max(mInc, hInc));

        js.setBorder(null);
    }

    private void resetSplitLocations() {
        preferredHorizontalSplitLocation =
                setSplitLocation("main.split.divider.location",
                        preferredHorizontalSplitLocation,
                        mainSplitPane);

        preferredVerticalSplitLocation =
                setSplitLocation("tree.split.divider.location",
                        preferredVerticalSplitLocation,
                        verticalSplitPane);
    }

    private double setSplitLocation(String propertyName, double splitLocation, JSplitPane splitPane) {
        SsmPreferences prefs = preferences;
        String s = prefs.getString(propertyName);

        if (s != null) {
            try {
                double fromFile = Double.parseDouble(s);
                if (fromFile >= 0 && fromFile <= 1.0)
                    splitLocation = fromFile;
                else
                    log.log(Level.WARNING, "Invalid divider location '" + fromFile + "'.");
            } catch (NumberFormatException nfe) {
                log.log(Level.WARNING, "Unable to parse divider location '" + s + "'.");
            }
        }
        if (splitLocation >= 0 && splitLocation <= 1.0) {
            splitPane.setDividerLocation(splitLocation);
        } else {
            log.warning("Ignoring invalid divider location '" + splitLocation + "'.");
        }
        return splitLocation;
    }

    private void showOrHideIdentityProvidersTab() {
        JTabbedPane treePanel = getPaletteTabbedPane();
        ConsoleLicenseManager licenseManager = Registry.getDefault().getLicenseManager();
        if (licenseManager.isAuthenticationEnabled()) {
            if (treePanel.getTabCount() < 2) {
                // Add missing Identity Providers tab
                JScrollPane identityScroller = new JScrollPane(getIdentitiesTree());
                configureScrollPane(identityScroller);
                treePanel.addTab("Identity Providers", identityScroller);
                treePanel.setSelectedIndex(0);
            } else {
                treePanel.setEnabledAt(1, true);
                treePanel.setSelectedIndex(0);
            }
        } else {
            if (treePanel.getTabCount() > 1) {
                // Remove unwanted Identity Providers tab
                //treePanel.remove(1);
                //bug 5357 - instead of removing the tab, we'll just disable the tab
                treePanel.setEnabledAt(1, false);
                treePanel.setSelectedIndex(0);
            }
        }
    }

    private JTabbedPane getPaletteTabbedPane() {
        if (paletteTabbedPane != null) return paletteTabbedPane;
        paletteTabbedPane = new JTabbedPane();
        paletteTabbedPane.setPreferredSize(new Dimension(140, 280));
        JScrollPane assertionScroller = new JScrollPane(getAssertionPaletteTree());
        configureScrollPane(assertionScroller);

        JPanel searchPanel = new JPanel(new BorderLayout(3, 1));
        searchPanel.add(assertionSearchLabel,BorderLayout.WEST);
        searchPanel.add(getAssertionSearchComboBox(), BorderLayout.CENTER);

        JPanel assertionsPane = new JPanel(new BorderLayout());
        assertionsPane.add(searchPanel, BorderLayout.NORTH);
        assertionsPane.add(assertionScroller, BorderLayout.CENTER);

        final Action findAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getAssertionSearchComboBox().requestFocusInWindow();
            }
        };

        getAssertionPaletteTree().getActionMap().put(MainWindow.L7_FIND, findAction);

        paletteTabbedPane.addTab("Assertions", assertionsPane);
        JScrollPane identityScroller = new JScrollPane(getIdentitiesTree());
        configureScrollPane(identityScroller);
        paletteTabbedPane.addTab("Identity Providers", identityScroller);
        Registry.getDefault().getLicenseManager().addLicenseListener(paletteTabbedPaneLicenseListener);
        return paletteTabbedPane;
    }

    /**
     * Return the TreeJPanel property value.
     *
     * @return JPanel
     */
    private JPanel getMainLeftPanel() {
        if (mainLeftPanel != null)
            return mainLeftPanel;

        //setup the tabs for the palette tree and identity pane
        JTabbedPane treePanel = getPaletteTabbedPane();
        showOrHideIdentityProvidersTab();

        Component descriptionPane = getAssertionDescriptionPane();
        JScrollPane descriptionScrollPane = new JScrollPane(descriptionPane);
        descriptionScrollPane.setPreferredSize(new Dimension(140, 100));
        descriptionScrollPane.setBorder(null);

        final JSplitPane paletteSections = new JSplitPane(JSplitPane.VERTICAL_SPLIT, treePanel, descriptionScrollPane);
        paletteSections.setOneTouchExpandable(true);
        paletteSections.setDividerLocation(-1);
        paletteSections.setResizeWeight(1);

        verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        verticalSplitPane.setBorder(null);
        verticalSplitPane.setDividerSize(10);
        verticalSplitPane.setResizeWeight(0.6);

        paletteSections.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                preferredVerticalSplitLocation = verticalSplitPane.getDividerLocation() / (double) (verticalSplitPane.getHeight() - verticalSplitPane.getDividerSize());
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                preferredVerticalSplitLocation =
                        setSplitLocation("tree.split.divider.location",
                                preferredVerticalSplitLocation,
                                verticalSplitPane);
            }
        });

        JScrollPane serviceScroller = new JScrollPane(getServicesAndPoliciesTree());
        configureScrollPane(serviceScroller);

        verticalSplitPane.setTopComponent(paletteSections);

        JPanel servicesAndPoliciesTreePanel = new JPanel();
        servicesAndPoliciesTreePanel.setLayout(new BorderLayout());
        getFilterStatusLabel().setText(FILTER_STATUS_NONE);

        //searching components
        JPanel searchPanel = new JPanel(new BorderLayout(3, 1));
        //searchPanel.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));
        searchPanel.add(searchLabel, BorderLayout.WEST);
        searchPanel.add(getSearchComboBox(), BorderLayout.CENTER);

        servicesAndPoliciesTreePanel.add(searchPanel, BorderLayout.NORTH);
        servicesAndPoliciesTreePanel.add(serviceScroller, BorderLayout.CENTER);
        servicesAndPoliciesTreePanel.add(getFilterStatusLabel(), BorderLayout.SOUTH);

        final Action findAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                getSearchComboBox().requestFocusInWindow();
            }
        };

        servicesAndPoliciesTreePanel.getActionMap().put(MainWindow.L7_FIND, findAction);

        verticalSplitPane.setBottomComponent(servicesAndPoliciesTreePanel);

        mainLeftPanel = new JPanel(new BorderLayout());
        mainLeftPanel.add(verticalSplitPane, BorderLayout.CENTER);
        mainLeftPanel.add(getPolicyToolBar(), BorderLayout.EAST);
        mainLeftPanel.setBorder(null);
        return mainLeftPanel;
    }

    /**
     * Create an editable search combo box for service and policy tree panel.
     *
     * @return The initialized editable search combo box for service and policy tree panel.
     */
    private EditableSearchComboBox<AbstractTreeNode> getSearchComboBox() {
        if (searchComboBox == null) {
            searchComboBox = new EditableSearchComboBox<AbstractTreeNode>(new EditableSearchComboBox.Filter() {
                @Override
                public boolean accept(Object obj) {
                    if (obj == null) return false; //this should not happen

                    //look for EntityHeaderNode and FolderNode, include Folder node as search result if the
                    //Folder name match with the search text
                    if (!(obj instanceof EntityHeaderNode) && !(obj instanceof FolderNode)) return false;
                    AbstractTreeNode node = (AbstractTreeNode) obj;

                    //match display names
                    final String searchText = this.getFilterText().toLowerCase();
                    //getName contains the URI for service nodes
                    boolean matches = node.getName().toLowerCase().contains(searchText);

                    //match uri as well
                    if (obj instanceof ServiceNode) {
                        ServiceNode serviceNode = (ServiceNode) obj;
                        final String routingUri = serviceNode.getEntityHeader().getRoutingUri();
                        if (routingUri != null) {
                            if (searchText.startsWith("/")) {
                                matches = routingUri.toLowerCase().contains(searchText.substring(1));
                            }
                        }
                    }

                    return matches;
                }
            }) {
            };

            searchComboBox.setEnabled(false);
            searchLabel.setEnabled(false);

            final Functions.Unary<String, AbstractTreeNode> accessorFunction = new Functions.Unary<String, AbstractTreeNode>() {
                @Override
                public String call(AbstractTreeNode abstractTreeNode) {
                    StringBuilder pathPrefix = new StringBuilder();

                    TreeNode nodePath[] = abstractTreeNode.getPath();

                    if (nodePath.length <= 4) {
                        for (int i = 1; i < nodePath.length - 1 ; i++) {
                            AbstractTreeNode n = (AbstractTreeNode) nodePath[i];
                            pathPrefix.append(n.getName());
                            pathPrefix.append(PATH_SEPARATOR);
                        }
                    } else if(nodePath.length > 4) {
                        AbstractTreeNode n = (AbstractTreeNode) nodePath[1];
                        pathPrefix.append(n.getName());
                        pathPrefix.append(PATH_SEPARATOR).append(ELLIPSIS).append(PATH_SEPARATOR);
                        n = (AbstractTreeNode) nodePath[nodePath.length - 2];
                        pathPrefix.append(n.getName());
                        pathPrefix.append(PATH_SEPARATOR);
                    }

                    return pathPrefix + abstractTreeNode.getName();
                }
            };

            final Functions.Unary<Icon, AbstractTreeNode> iconAccessorFunction = new Functions.Unary<Icon, AbstractTreeNode>() {

                @Override
                public Icon call(AbstractTreeNode abstractTreeNode) {
                    return new ImageIcon(abstractTreeNode.getIcon());
                }
            };

            //Create a renderer and configure it to clip. Text which is too large will automatically get '...' added to it
            //and the JLabel will not grow to accommodate it, if it is larger than the size of the combo box component
            TextListCellRenderer<EntityHeaderNode> comboBoxRenderer =
                    new TextListCellRenderer<EntityHeaderNode>(accessorFunction, null, iconAccessorFunction, false);
            comboBoxRenderer.setRenderClipped(true);

            searchComboBox.setRenderer(comboBoxRenderer);

            //create comparator to sort the filtered items
            searchComboBox.setComparator(new Comparator<AbstractTreeNode>() {
                @Override
                public int compare(AbstractTreeNode o1, AbstractTreeNode o2) {
                    return (o1.toString().compareToIgnoreCase(o2.toString()));
                }
            });

            //monitor the action if selection was made by mouse or keyboard.  We need to filter scrolling actions
            searchComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    invokeSelection();
                }
            });

            searchComboBox.addTextFieldKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                        getServicesAndPoliciesTree().requestFocus();
                }

            });
        }

        return searchComboBox;
    }

    /**
     * @return  The initialized editable search combo box for assertion tree panel.
     */
    private EditableSearchComboBox<AbstractLeafPaletteNode> getAssertionSearchComboBox() {
        if (assertionSearchComboBox == null) {
            assertionSearchComboBox = new EditableSearchComboBox<AbstractLeafPaletteNode>(new EditableSearchComboBox.Filter() {
                @Override
                public boolean accept(Object obj) {
                    if(obj == null) return false; //this should not happen

                    if (!(obj instanceof AbstractLeafPaletteNode)) return false;
                    AbstractLeafPaletteNode paletteNode = (AbstractLeafPaletteNode) obj;

                    //match display names
                    final String searchText = this.getFilterText().toLowerCase();
                    //getName contains the URI for service nodes
                    return paletteNode.getName().toLowerCase().contains(searchText);
                }
            }){};

            assertionSearchComboBox.setEnabled(false);
            assertionSearchLabel.setEnabled(false);

            final Functions.Unary<String, AbstractLeafPaletteNode> accessorFunction = new Functions.Unary<String, AbstractLeafPaletteNode>() {
                @Override
                public String call(AbstractLeafPaletteNode abstractTreeNode) {
                    return  abstractTreeNode.getName();

                }
            };

            final Functions.Unary<Icon, AbstractLeafPaletteNode> iconAccessorFunction = new Functions.Unary<Icon, AbstractLeafPaletteNode>() {
                @Override
                public Icon call(AbstractLeafPaletteNode abstractTreeNode) {
                    Image icon = abstractTreeNode.getIcon();
                    return icon == null ? null : new ImageIcon(icon);
                }
            };

            //Create a renderer and configure it to clip. Text which is too large will automatically get '...' added to it
            //and the JLabel will not grow to accommodate it, if it is larger than the size of the combo box component
            TextListCellRenderer<AbstractLeafPaletteNode> comboBoxRenderer =
                    new TextListCellRenderer<AbstractLeafPaletteNode>(accessorFunction, null, iconAccessorFunction, false);
            comboBoxRenderer.setRenderClipped(true);

            assertionSearchComboBox.setRenderer(comboBoxRenderer);

            //create comparator to sort the filtered items
            assertionSearchComboBox.setComparator(new Comparator<AbstractLeafPaletteNode>() {
                @Override
                public int compare(AbstractLeafPaletteNode o1, AbstractLeafPaletteNode o2) {
                    return (o1.toString().compareToIgnoreCase(o2.toString()));
                }
            });

            //monitor the action if selection was made by mouse or keyboard.  We need to filter scrolling actions
            assertionSearchComboBox.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    invokePaletteSelection();
                }
            });

            assertionSearchComboBox.addTextFieldKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
                        getAssertionPaletteTree().requestFocus();
                }
            });
        }

        return assertionSearchComboBox;
    }

    /**
     * Expands the tree accordingly based on the service or policy selection.  It will also invoke the editing
     * action so that the user can readily edit the service or policy.
     */
    private void invokeSelection() {
        JTree tree = getServicesAndPoliciesTree();
        final AbstractTreeNode node = (AbstractTreeNode) searchComboBox.getSelectedItem();
        if (node == null) return;

        //make the node visible by scroll to it
        TreePath path = new TreePath(node.getPath());
        tree.scrollPathToVisible(path);
        tree.setSelectionPath(path);
        tree.requestFocus();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //invoke the edit policy action
                for (Action action : node.getActions()) {
                    if (action instanceof EditPolicyAction) {
                        ((EditPolicyAction) action).invoke();
                    }
                }
            }
        });
    }

    /**
     * Expands the tree accordingly based on the assertion selection.
     */
    private void invokePaletteSelection() {
        JTree tree = getAssertionPaletteTree();
        final AbstractTreeNode node = (AbstractTreeNode) assertionSearchComboBox.getSelectedItem();
        if (node == null) return;
        final TreePath treePath = new TreePath(node.getPath());
        tree.setSelectionPath(treePath);
        tree.makeVisible(treePath);
        tree.scrollPathToVisible(treePath);
        getAssertionPaletteTree().requestFocus();
    }

    /**
     * @return  The list of searchable service and policy nodes based on the filtering selection.
     */
    private List<AbstractTreeNode> getAllSearchableServiceAndPolicyNodes() {
        JTree tree = getServicesAndPoliciesTree();
        RootNode rootNode = (RootNode) tree.getModel().getRoot();
        NodeFilter filter = ((FilteredTreeModel) tree.getModel()).getFilter();

        return (List<AbstractTreeNode>) rootNode.collectSearchableChildren(new Class[] {EntityHeaderNode.class, FolderNode.class}, filter);
    }

    /**
     * @return  The list of searchable assertion palette nodes based on the filtering selection.
     */
    private List<AbstractLeafPaletteNode> getAllSearchablePaletteNodes() {
        JTree tree = getAssertionPaletteTree();
        AssertionsPaletteRootNode rootNode = (AssertionsPaletteRootNode) tree.getModel().getRoot();
        NodeFilter filter = ((FilteredTreeModel) tree.getModel()).getFilter();

        return (List<AbstractLeafPaletteNode>) rootNode.collectSearchableChildren(new Class[]{AbstractLeafPaletteNode.class}, filter);
    }

    private JLabel getFilterStatusLabel(){
        if(filterStatusLabel != null){
            return filterStatusLabel;
        }
        filterStatusLabel = new JLabel();
        return filterStatusLabel;
    }

    private Component getAssertionDescriptionPane() {
        if (descriptionText == null) {
            descriptionText = new JTextPane();
            descriptionText.setContentType("text/html");
            descriptionText.setEditable(false);
            descriptionText.setMaximumSize(new Dimension(descriptionText.getMaximumSize().width, 100));
            descriptionText.setBorder(null);
            Utilities.attachDefaultContextMenu(descriptionText);
        }
        return descriptionText;
    }

    // --- Event listeners ---------------------------------------

    /**
     * Disconnect from the SSG.
     */
    public void disconnectFromGateway() {
        getWorkSpacePanel().clearWorkspaceUnvetoable();
        getStatusMsgLeft().setText("Disconnected");
        getStatusMsgRight().setText("");
        getAssertionPaletteTree().setModel(null);
        getMainSplitPaneRight().removeAll();
        addComponentToGridBagContainer(getMainSplitPaneRight(), getDropComponent());
        getMainSplitPaneRight().validate();
        getMainSplitPaneRight().repaint();
        getServicesAndPoliciesTree().setModel(null);
        getIdentitiesTree().setModel(null);
        updateActions(null);
        fireDisconnected();

        // if inactivityTimer is running stop
        if (inactivityTimer.isRunning()) {
            inactivityTimer.stop();
        }
        getFilterStatusLabel().setText(FILTER_STATUS_NONE);
        PolicyDiffContext.setLeftDiffPolicyInfo(null);
    }

    private void addComponentToGridBagContainer(JComponent container, JComponent component) {
        GridBagConstraints constraints
                = new GridBagConstraints(0, // gridx
                0, // gridy
                1, // widthx
                1, // widthy
                1.0, // weightx
                1.0, // weigthy
                GridBagConstraints.NORTH, // anchor
                GridBagConstraints.BOTH, //fill
                new Insets(0, 0, 0, 0), // inses
                0, // padx
                0); // pady
        container.add(component, constraints);
    }

    /**
     * update the actions, menus, buttons for the selected node.
     *
     * @param node currently selected node
     */
    private void updateActions(AbstractTreeNode node) {
        if (node == null) {
            toggleConnectedMenus(false);
            getRemoveNodeAction().setEnabled(false);
            getFindIdentityAction().setEnabled(false);
            return;
        }
        getRemoveNodeAction().setEnabled(node.canDelete());
        getFindIdentityAction().setEnabled(false);
        getRefreshAction().setEnabled(false);
    }

    /**
     * @see ActionEvent for details
     */
    private void exitMenuEventHandler() {
        if (isConnected()) {
            try {
                getWorkSpacePanel().updatePolicyTabsProperties();
                getWorkSpacePanel().clearWorkspace(); // vetoable
                disconnectFromGateway();
            } catch (ActionVetoException e) {
                return;
            }

            SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
            if (securityProvider != null) securityProvider.logoff();
        }

        String maximized = Boolean.toString(getExtendedState() == Frame.MAXIMIZED_BOTH);
        this.setVisible(false);
        try {
            SsmPreferences prefs = preferences;
            prefs.putProperty("last.window.maximized", maximized);
            prefs.putProperty("tree.split.divider.location", Double.toString(preferredVerticalSplitLocation));
            prefs.putProperty("main.split.divider.location", Double.toString(preferredHorizontalSplitLocation));
            prefs.store();
        } catch (Exception e) {
            log.log(Level.WARNING, "Unable to save divider location.", e);
        }
        System.exit(0);
    }

    /**
     * Initializes listeners for the form
     */
    private void initListeners() {


        // exitMenuItem listener
        getExitMenuItem().
                addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        exitMenuEventHandler();
                    }
                });

        // HelpTopics listener
        getHelpTopicsMenuItem().
                addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        showHelpTopicsRoot();
                    }
                });

        // look and feel listener
        PropertyChangeListener l =
                new PropertyChangeListener() {
                    /**
                     * This method gets called when a property is changed.
                     */
                    @Override
                    public void propertyChange(final PropertyChangeEvent evt) {
                        if ("lookAndFeel".equals(evt.getPropertyName())) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    SwingUtilities.updateComponentTreeUI(TopComponents.getInstance().getTopParent());
                                }
                            });
                        }
                    }
                };
        UIManager.addPropertyChangeListener(l);

        //focus listener to track the component which currently has focus
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.addPropertyChangeListener("permanentFocusOwner", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                Object fo = evt.getNewValue();
                if (fo instanceof JComponent) {
                    focusOwner = (JComponent) fo;
                    //all logic for when a component is focused should go in the following method and not here
                    updateActionsDependentOnFocusedComponent();
                }
            }
        });

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //all logic for when a component is focused should go in the following method and not here
                updateActionsDependentOnFocusedComponent();
            }
        });
    }


    // --- End Event listeners ---------------------------------------

    /**
     * Initialize the class.
     */
    private void initialize() {

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // exit routine, do not remove
                MainWindow.this.exitMenuEventHandler();
            }
        });

        // Adds listener to save window size and location under normal state
        // (not under maximized state).
        // But JDK 1.5 currently does not distinguish between the two, so we
        // have to test for window state below.
        // @see http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6256547
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                if (getExtendedState() == Frame.NORMAL) {
                    preferences.setLastWindowSize(getSize());
                }
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                super.componentMoved(e);
                final Point p = getLocation();
                if (getExtendedState() == Frame.NORMAL && p.x >= 0 && p.y >= 0) {
                    // Currently JDK 1.5 fires componentMoved before state is
                    // set to maximized. So we have to additionally test for
                    // positive coordinate values. It's not perfect since user
                    // can drag a normal state window slightly beyond the top
                    // left screen corner. But that's the best we can do now.
                    preferences.setLastWindowLocation(getLocation());
                }
            }
        });

        setName("MainWindow");
        setJMenuBar(isApplet() ? null : getMainJMenuBar());
        setTitle( resapplication.getString("SSG") + " " + BuildInfo.getProductVersion() );

        String imagePath = null;
        final ClassLoader classLoader = getClass().getClassLoader();
        ImageIcon smallIcon =
                new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/CA_Logo_Black_16x16.png"));
        ImageIcon largeIcon =
                new ImageIcon(ImageCache.getInstance().getIcon(RESOURCE_PATH + "/CA_Logo_Black_32x32.png"));
        setIconImages(Arrays.asList(smallIcon.getImage(), largeIcon.getImage()));

        DialogDisplayer.setDefaultFrameIcon(smallIcon);
        DialogDisplayer.setDefaultWindowImages(Arrays.asList(largeIcon.getImage(), smallIcon.getImage()));

        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(getFrameContentPane(), BorderLayout.CENTER);
        contentPane.add(getStatusBarPane(), BorderLayout.SOUTH);
        //getRootPane().setFocusable(false);

        initListeners();


        toggleConnectedMenus(false);
        /* Pack frame on the screen */
        //pack();
        validate();
        /* restore window position */
        initializeWindowPosition();
        initializeHTMLRenderingKit();
        installInactivityTimerEventListener();
        installCascadingErrorHandler();
        installClosingWindowHandler();

        installTopMenuRefresh();
    }

    /**
     * Get the small logo image, suitable for use as a frame icon.
     *
     * @return the small logo image.  Never null
     * @throws RuntimeException if the icon resource can't be found
     */
    public static Image getSmallLogoImage() {
        String path = RESOURCE_PATH + "/CA_Logo_Black_16x16.png";
        Image icon = ImageCache.getInstance().getIcon(path);
        if (icon == null) throw new RuntimeException("Missing resource: " + path);
        return icon;
    }

    private void initializeHTMLRenderingKit() {
        //setup the default font for html rendering (unless overridden by the Document or component itself)
        //we'll use the default font for a label since it seems nice
        JLabel label = new JLabel();
        final HTMLEditorKit htmlEditorKit = new HTMLEditorKit();
        StyleSheet ss = htmlEditorKit.getStyleSheet();
        Style style = ss.getStyle("body");
        if (style != null) {
            style.removeAttribute(StyleConstants.FontFamily);
            final Font font = label.getFont();
            style.addAttribute(StyleConstants.FontFamily, font.getFamily());
            style.removeAttribute(StyleConstants.FontSize);
            style.addAttribute(StyleConstants.FontSize, Integer.toString(font.getSize()));
        }
    }

    /**
     * Restore any saved window position.  Declines to restore the position
     * if the screen resolution has decreased since then.
     */
    private void initializeWindowPosition() {
        boolean posWasSet = false;
        Dimension curScreenSize = Toolkit.getDefaultToolkit().getScreenSize();
        try {
            SsmPreferences prefs = preferences;
            Boolean maximized = Boolean.valueOf(prefs.getString("last.window.maximized", "false"));
            if (maximized) {
                maximizeOnStart = true;
            }
            Dimension lastScreenSize = prefs.getLastScreenSize();
            Dimension lastWindowSize = prefs.getLastWindowSize();
            Point lastWindowLocation = prefs.getLastWindowLocation();
            if (lastScreenSize != null &&
                    lastScreenSize.width >= curScreenSize.width &&
                    lastScreenSize.height >= curScreenSize.height) {
                if (lastWindowLocation != null) {
                    this.setLocation(lastWindowLocation);
                    posWasSet = true;
                }
                if (lastWindowSize != null) {
                    this.setSize(lastWindowSize);
                    posWasSet = true;
                }
            }

            prefs.setLastScreenSize(curScreenSize);
            prefs.store();
            prefs.updateSystemProperties();
        } catch (IOException e) {
            log.log(Level.WARNING, "unable to fetch or set window position prefs: ", e);
        }

        if (!posWasSet) {
            if (curScreenSize.height > 768 &&
                    curScreenSize.width > 1024) {
                curScreenSize = new Dimension(1024, 768);
            }
            this.setSize(curScreenSize);
            Utilities.centerOnScreen(this);
        }
    }


    // -------------- inactivitiy timeout (close your eyes) -------------------
    private volatile long inactivityTimeout = TimeUnit.MINUTES.toMillis(30L); // AbstractSsmPreferences.DEFAULT_INACTIVITY_TIMEOUT
    private volatile long lastActivityTime = System.currentTimeMillis();
    private volatile long lastRemoteActivityTime = System.currentTimeMillis();

    private void installInactivityTimerEventListener() {
        if (ssmApplication.isApplet()) return; // no inactivity timer on applet

        // AWT event listener
        final
        AWTEventListener listener =
                new AWTEventListener() {
                    @Override
                    public void eventDispatched(AWTEvent e) {
                        lastActivityTime = System.currentTimeMillis();
                    }
                };
        // all events that should reset the idle timout, omitting events which might fire while dashboard and audits are updating unattended (Bug #4142)
        long mask =
                AWTEvent.FOCUS_EVENT_MASK |
                        AWTEvent.KEY_EVENT_MASK |
                        AWTEvent.MOUSE_EVENT_MASK |
                        AWTEvent.MOUSE_MOTION_EVENT_MASK |
                        AWTEvent.WINDOW_EVENT_MASK |
                        AWTEvent.ACTION_EVENT_MASK |
                        AWTEvent.INPUT_METHOD_EVENT_MASK;

        // dynamic initializer, register listener
        {
            MainWindow.this.getToolkit().
                    addAWTEventListener(listener, mask);
        }
    }

    private void onInactivityTimerTick() {
        if (ssmApplication.isApplet()) return;

        final long now = System.currentTimeMillis();
        if (inactivityTimeout > 0L &&
                (now - lastActivityTime) > inactivityTimeout) { // match
            inactivityTimer.stop(); // stop timer
            // make sure it is invoked on event dispatching thread
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    MainWindow.this.getStatusMsgRight().
                            setText("inactivity timeout expired; disconnecting...");

                    getWorkSpacePanel().updatePolicyTabsProperties();
                    TopComponents.getInstance().setConnectionLost(true);
                    MainWindow.this.disconnectFromGateway();
                    SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
                    if (securityProvider != null) securityProvider.logoff();

                    // ensure redraw after any errors
                    validate();

                    // add a top level dlg that indicates the connection was closed
                    DialogDisplayer.showMessageDialog(MainWindow.this,
                            "The Policy Manager connection has been closed due\n" +
                                    "to timeout. Any unsaved work will be lost.",
                            "Connection Timeout", JOptionPane.WARNING_MESSAGE, null);
                }
            });
        } else if (Registry.getDefault().isAdminContextPresent() &&
                (now - lastRemoteActivityTime) > PING_INTERVAL && lastActivityTime > lastRemoteActivityTime) {
            // If the user is interacting with the Manager then ping the Gateway periodically
            // to keep the remote session alive if there are no other remote calls.
            //
            // This is in the background so it does not block the swing thread if slow.
            Background.scheduleOneShot(new TimerTask() {
                @Override
                public void run() {
                    try {
                        Registry.getDefault().getAdminLogin().ping();
                        updateRemoteLastActivityTime(); // should also get updated by pinging
                    } catch (IllegalStateException ise) {
                        // admin context not available
                    } catch (Exception e) {
                        ErrorManager.getDefault().notify(Level.INFO, e, ErrorManager.DEFAULT_ERROR_MESSAGE);
                    }
                }
            }, 1L);
        }
    }

    private final Timer inactivityTimer =
            new Timer((int) TimeUnit.SECONDS.toMillis(10L),
                    new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            onInactivityTimerTick();
                        }
                    });

    public void updateLastActivityTime() {
        lastActivityTime = System.currentTimeMillis();
    }

    public void updateRemoteLastActivityTime() {
        lastRemoteActivityTime = System.currentTimeMillis();
    }

    // -------------- inactivitiy timeout end (open your eyes) -------------------


    /**
     * set the inactivity timeout value
     *
     * @param newTimeout new inactivity timeout
     */
    public void setInactivitiyTimeout(int newTimeout) {
        inactivityTimeout = TimeUnit.MINUTES.toMillis((long) newTimeout);
        if (!isConnected()) return;

        if (inactivityTimeout >= 0) {
            log.log(Level.INFO, "Inactivity timeout updated (timeout = " + inactivityTimeout + ")");
            if (!inactivityTimer.isRunning()) inactivityTimer.start();
        } else {
            log.log(Level.WARNING, "Incorrect timeout value " + inactivityTimeout);
            setInactivitiyTimeout(0);
        }
    }

    private void installCascadingErrorHandler() {
        ErrorManager errorManager = ErrorManager.getDefault();

        CascadingErrorHandler handler = new CascadingErrorHandler();
        addLogonListener(handler);

        errorManager.pushHandler(handler);
    }

    /**
     * Install a handler to close all opened windows when ssg is disconnected.
     */
    private void installClosingWindowHandler() {
        closeWindowListener = new LogonListener() {
            @Override
            public void onLogon(LogonEvent e) {
            }

            @Override
            public void onLogoff(LogonEvent e) {
                closeAllWindows();
            }
        };

        addLogonListener(closeWindowListener);
    }

    /**
     * Installs listener to refresh top menu so that it will enable/disable menu items upon start of SSM, logon, and logoff
     */
    private void installTopMenuRefresh() {
        updateTopMenu();
        topMenuLogonListener = new LogonListener() {
            @Override
            public void onLogoff(LogonEvent e) {
                updateTopMenu();
            }

            @Override
            public void onLogon(LogonEvent e) {
                updateTopMenu();
            }
        };
        addLogonListener(topMenuLogonListener);

        topMenuPermissionRefreshListener = new PermissionRefreshListener() {
            @Override
            public void onPermissionRefresh() {
                updateTopMenu();
            }
        };
        addPermissionRefreshListener(topMenuPermissionRefreshListener);

        topMenuLicenseListener = new LicenseListener() {
            @Override
            public void licenseChanged(ConsoleLicenseManager licenseManager) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        updateTopMenu();
                    }
                });
            }
        };
        Registry.getDefault().getLicenseManager().addLicenseListener(topMenuLicenseListener);
    }

    /**
     * Perform configuration validation
     */
    void checkConfiguration() {
        String trustStore = SyspropUtil.getString("javax.net.ssl.trustStore", null);
        if (trustStore != null) {
            File storeFile = new File(trustStore);
            if (!storeFile.exists()) {
                if (storeFile.getParentFile() == null || !storeFile.getParentFile().exists()) {
                    DialogDisplayer.showMessageDialog(this, null, "Invalid Trust Store configuration.\n File '" + trustStore + "'.", null);
                }
            } else if (!storeFile.canWrite()) {
                log.warning("Trust store file is not writable '" + storeFile.getAbsolutePath() + "'.");
            }
        }
    }

    /**
     * invoke logon dialog
     */
    void activateLogonDialog() {
        getConnectAction().actionPerformed(null);
    }

    /**
     * is this console connected?
     *
     * @return true if connected,false otherwise
     */
    public boolean isConnected() {
        // the menu item is enabled if connected
        return getDisconnectMenuItem().isEnabled();
    }

    public JMenuItem getAuditMenuItem() {
        if (auditMenuItem != null) return auditMenuItem;
        auditMenuItem = new JMenuItem(getGatewayAuditWindowAction());

        return auditMenuItem;
    }

    public JMenuItem getViewLogMenuItem() {
        if (viewLogMenuItem != null) return viewLogMenuItem;
        viewLogMenuItem = new JMenuItem(getViewLogsAction());

        return viewLogMenuItem;
    }

    public JMenuItem getFromFileMenuItem() {
        if (fromFileMenuItem != null) return fromFileMenuItem;
        fromFileMenuItem = new JMenuItem(getAuditOrLogsFromFileAction());

        return fromFileMenuItem;
    }

    public JMenuItem getManageJmsEndpointsMenuItem() {
        if (manageJmsEndpointsMenuItem != null)
            return manageJmsEndpointsMenuItem;
        manageJmsEndpointsMenuItem = new JMenuItem(getManageJmsEndpointsAction());

        return manageJmsEndpointsMenuItem;
    }

    public JMenuItem getManageKerberosMenuItem() {
        if (manageKerberosMenuItem != null)
            return manageKerberosMenuItem;
        manageKerberosMenuItem = new JMenuItem(getManageKerberosAction());

        return manageKerberosMenuItem;
    }

    public JMenuItem getManageRolesMenuItem() {

        if (manageRolesMenuItem == null)
            manageRolesMenuItem = new JMenuItem(getManageRolesAction());

        return manageRolesMenuItem;
    }

    public JMenuItem getManageServiceResolutionMenuItem() {
        if (manageServiceResolutionMenuItem == null) {
            manageServiceResolutionMenuItem = new JMenuItem(getManageServiceResolutionAction());
            manageServiceResolutionMenuItem.setText("Manage Service Resolution");
            manageServiceResolutionMenuItem.setMnemonic(0);
        }
        return manageServiceResolutionMenuItem;
    }

    private Action getManageRolesAction() {
        if (manageRolesAction == null) {
            manageRolesAction = new ManageRolesAction();
            disableUntilLogin(manageRolesAction);
        }
        return manageRolesAction;
    }

    private Action getManageServiceResolutionAction() {
        if (manageServiceResolutionAction == null) {
            manageServiceResolutionAction = new ManageResolutionConfigurationAction();
            disableUntilLogin(manageServiceResolutionAction);
        }
        return manageServiceResolutionAction;
    }

    public JMenuItem getManageCertificatesMenuItem() {
        if (manageCertificatesMenuItem != null)
            return manageCertificatesMenuItem;
        manageCertificatesMenuItem = new JMenuItem(getManageCertificatesAction());

        return manageCertificatesMenuItem;
    }

    public JMenuItem getManagePrivateKeysMenuItem() {
        if (managePrivateKeysMenuItem != null)
            return managePrivateKeysMenuItem;
        return managePrivateKeysMenuItem = new JMenuItem(getManagePrivateKeysAction());
    }

    public JMenuItem getManageSecurePasswordsMenuItem() {
        if (manageSecurePasswordsMenuItem != null)
            return manageSecurePasswordsMenuItem;
        return manageSecurePasswordsMenuItem = new JMenuItem(getManageSecurePasswordsAction());
    }

    public JMenuItem getRevokeCertificatesMenuItem() {
        if (revokeCertificatesMenuItem != null)
            return revokeCertificatesMenuItem;
        revokeCertificatesMenuItem = new JMenuItem(getRevokeCertificatesAction());

        return revokeCertificatesMenuItem;
    }

    public JMenuItem getManageGlobalResourcesMenuItem() {
        if (manageGlobalResourcesMenuItem != null) return manageGlobalResourcesMenuItem;
        manageGlobalResourcesMenuItem = new JMenuItem(getManageGlobalResourcesAction());
        return manageGlobalResourcesMenuItem;
    }

    public JMenuItem getManageClusterPropertiesActionMenuItem() {
        if (manageClusterPropertiesMenuItem != null) return manageClusterPropertiesMenuItem;
        manageClusterPropertiesMenuItem = new JMenuItem(getManageClusterPropertiesAction());
        return manageClusterPropertiesMenuItem;
    }

    private JMenuItem getDashboardMenuItem() {
        if (dashboardMenuItem != null) return dashboardMenuItem;
        dashboardMenuItem = new JMenuItem(getShowDashboardAction());
        return dashboardMenuItem;
    }

    private JMenuItem getManageClusterLicensesMenuItem() {
        if (manageClusterLicensesMenuItem != null) return manageClusterLicensesMenuItem;
        manageClusterLicensesMenuItem = new JMenuItem(getManagerClusterLicensesAction());
        return manageClusterLicensesMenuItem;
    }

    public void updateNodeNameInStatusMessage(String oldName, String newName) {
        // extract the node name from the status message
        int startIndex = getStatusMsgLeft().getText().indexOf(CONNECTION_PREFIX);
        if (startIndex > 0) {
            String nodeName = getStatusMsgLeft().getText().substring(startIndex + CONNECTION_PREFIX.length(), getStatusMsgLeft().getText().length() - 1);

            if (nodeName.equals(oldName)) {
                // update the node name only when the nodeName mataches with the oldName
                String newStatus = connectionID + connectionContext + getNodeNameMsg(newName);
                getStatusMsgLeft().setText(newStatus);
            }
        } else {
            // this should never happen
            log.severe("Internal error: cannot update the node name on the status bar.");
        }
    }

    private String getNodeNameMsg(String nodeName) {

        String nodeNameMsg = "";
        if (nodeName != null) {
            nodeNameMsg = CONNECTION_PREFIX + nodeName + "]";
        }
        return nodeNameMsg;
    }

    private final LicenseListener paletteTreeLicenseListener = new LicenseListener() {
        @Override
        public void licenseChanged(ConsoleLicenseManager licenseManager) {
            JTree tree = getAssertionPaletteTree();
            if (tree == null || tree.getModel() == null) return; // not constructed yet
            TopComponents.getInstance().getPaletteFolderRegistry().refreshPaletteFolders();
        }
    };

    private final LicenseListener paletteTabbedPaneLicenseListener = new LicenseListener() {
        @Override
        public void licenseChanged(ConsoleLicenseManager licenseManager) {
            showOrHideIdentityProvidersTab();
        }
    };

    private LogonDialog.LogonListener logonListener = new LogonDialog.LogonListener() {
        /* invoked on authentication success */
        @Override
        public void onAuthSuccess(final String id, final boolean usedCertificate) {
            ConsoleGoidUpgradeMapper.updatePrefixesFromGateway();

            final boolean isApplet = isApplet();

            if (isApplet) {
                User user = Registry.getDefault().getSecurityProvider().getUser();
                if(null != user) {
                    connectionID = user.getName();
                }
            } else {
                connectionID = id;
            }

            String statusMessage = connectionID;
            connectionContext = "";

            /* clear cached server cert */
            serverSslCert = null;
            auditSigningCert = null;

            /* init rmi cl */
            if (!isApplet)
                RMIClassLoader.getDefaultProviderInstance();

            /* set the preferences */
            try {
                SsmPreferences prefs = preferences;
                connectionContext = " @ " + getServiceUrl();
                /**
                 * At anytime, save the last login id.
                 * Note: showing the login id at the logon dialog is dependent on if the property, SAVE_LAST_LOGIN_ID
                 * is true or false.  Also see {@link AbstractSsmPreferences#rememberLoginId}
                 */
                prefs.putProperty(SsmPreferences.LAST_LOGIN_ID, id);
                prefs.putProperty(SsmPreferences.LAST_LOGIN_TYPE, usedCertificate ? "certificate" : "password");
                prefs.store();
            } catch (IOException e) {
                log.log(Level.WARNING, "onAuthSuccess()", e);
            }

            ClusterStatusAdmin clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();

            // Gather and cache the cluster license early, since components like the assertion palette will need it
            CompositeLicense compositeLicense = null; // if null, no license installed

            try {
                compositeLicense = clusterStatusAdmin.getCompositeLicense();
            } finally {
                // Cache it
                Registry.getDefault().getLicenseManager().setLicense(compositeLicense);
            }

            if (compositeLicense != null && compositeLicense.isFeatureEnabled(ConsoleLicenseManager.SERVICE_ADMIN)) {
                // Gather any modular assertions offered by this gateway early on as well, for the assertion palette
                try {
                    ServicesAndPoliciesTree spt = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                    if ( null != spt )
                        spt.removeAllModularAssertionListeners();
                    TopComponents.getInstance().getAssertionRegistry().updateModularAssertions();
                    TopComponents.getInstance().getAssertionRegistry().updateCustomAssertions();
                    TopComponents.getInstance().getEncapsulatedAssertionRegistry().updateEncapsulatedAssertions();
                } catch (RuntimeException e) {
                    log.log(Level.WARNING, "Unable to update modular assertions: " + ExceptionUtils.getMessage(e) + ".",
                        ExceptionUtils.getDebugException(e));
                } catch (FindException e) {
                    log.log(Level.WARNING, "Unable to update encapsulated assertions: " + ExceptionUtils.getMessage(e) + ".",
                        ExceptionUtils.getDebugException(e));
                }
            } else {
                log.log(Level.INFO, "Unable to update assertions because " + ConsoleLicenseManager.SERVICE_ADMIN + " is not licensed.");
            }
            SecurityZoneUtil.flushCachedSecurityZones();

            String nodeName = "";

            try {
                nodeName = clusterStatusAdmin.getSelfNodeName();
            } catch (RuntimeException e) {
                log.log(Level.WARNING, "Cannot get the node name", e);
            }

            if (nodeName == null) {
                nodeName = "unknown";
            }

            statusMessage += connectionContext;
            statusMessage += getNodeNameMsg(nodeName);

            final String message = statusMessage;
            final int timeout = preferences.getInactivityTimeout();

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    getStatusMsgLeft().setText(message);
                    boolean success = false;
                    try {
                        initializeWorkspace();
                        toggleConnectedMenus(true);
                        loadPolicyTabsIntoWorkspace();
                        MainWindow.this.
                                setInactivitiyTimeout(timeout);
                        MainWindow.this.fireConnected();
                        ssmApplication.updateHelpUrl();
                        success = true;
                    } finally {
                        if (!success) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    MainWindow.this.fireDisconnected();
                                    enableOrDisableConnectionComponents(true);
                                }
                            });
                        }
                    }
                }
            });

            Authorizer auth = Registry.getDefault().getSecurityProvider();

            if (auth.hasPermission(new AttemptedDeleteAll(EntityType.ANY))) {
                if (null == compositeLicense) {
                    showManageLicensesDialog();
                } else {
                    long warningPeriod = clusterStatusAdmin.getLicenseExpiryWarningPeriod();

                    // check for any license issues we should warn the user of
                    String licenseWarnings = collectLicenseWarnings(compositeLicense, warningPeriod);

                    if (licenseWarnings.length() > 0) {
                        showLicenseWarnings(licenseWarnings);
                    } else {
                        X509Certificate[] sslCertificates = getServerSslCertChain();

                        if (sslCertificates != null && sslCertificates.length > 0) {
                            Date sslExpiryDate = sslCertificates[0].getNotAfter();

                            if (sslExpiryDate != null && (sslExpiryDate.getTime() - warningPeriod) < System.currentTimeMillis()) {
                                showSSLWarning(sslExpiryDate);
                            }
                        }
                    }
                }
            }

            try {
                showWarningBanner();
            } catch (RuntimeException re) {
                log.log(Level.WARNING, "Unable to show warning banner: " + ExceptionUtils.getMessage(re) + ".",
                        ExceptionUtils.getDebugException(re));
            }
        }

        /* invoked on authentication failure */
        @Override
        public void onAuthFailure() {
        }
    };

    /**
     * Load all previously last opened policies into the workspace.  One tab is created for one policy version.
     * Note: any non-existing or invalid policy entity node goid, policy goid, and policy version number will
     * discard that policy tab to load into the workspace.  The warning message will be logged.
     */
    private void loadPolicyTabsIntoWorkspace() {
        final String tabsProp = preferences.getString(WorkSpacePanel.PROPERTY_LAST_OPENED_POLICY_TABS);
        // If no such property exist in preferences, open a Home Page
        if (tabsProp == null) {
            getHomeAction().actionPerformed(null);
            return;
        }

        final RootNode rootNode = ((ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME)).getRootNode();
        final String tabTokens[] = TextUtils.CSV_STRING_SPLIT_PATTERN.split(tabsProp);

        // In the property, the last selected policy tab is always saved into the last position of the property value.
        // So when the last tabToken is processed, the corresponding component is the right one and will be selected via setComponent(...) in PolicyEditorPanel.
        boolean someTabOpened = false;
        for (String tabToken: tabTokens) {
            if (tabToken.equals(HomePagePanel.HOME_PAGE_NAME)) {
                getHomeAction().actionPerformed(null);
                someTabOpened = true;
            } else {
                String[] tabInfo = Pattern.compile("\\s*#\\s*").split(tabToken);
                if (tabInfo.length < 3) throw new IllegalArgumentException("The format of " + WorkSpacePanel.PROPERTY_LAST_OPENED_POLICY_TABS + " is invalid.");

                // Find the policy entity node, which policy versions are associated with.
                // But check if the policy entity node exists or not first.
                String policyNodeEntityGoid = tabInfo[0];
                EntityWithPolicyNode entityWithPolicyNode;
                try {
                    entityWithPolicyNode = (EntityWithPolicyNode) rootNode.getNodeForEntity(Goid.parseGoid(policyNodeEntityGoid));
                } catch (Exception e) {
                    log.info("The policy entity node (goid=" + policyNodeEntityGoid + ") cannot be found, so the policy editor workspace cannot open a tab for it.");
                    continue;
                }

                // Check if the policy goid is valid to match a policy or not.
                String policyGoid = tabInfo[1];
                try {
                    Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(Goid.parseGoid(policyGoid));
                    if (policy == null) {
                        log.info("The policy (goid=" + policyGoid + ") does not exist, so the policy editor workspace cannot open a tab for it.");
                        continue;
                    }
                } catch (FindException e) {
                    log.info("The policy (goid=" + policyGoid + ") cannot be found, so the policy editor workspace cannot open a tab for it.");
                    continue;
                } catch (Exception e1) {
                    log.info("Cannot open a tab for the policy (goid=" + policyGoid + "): " + ExceptionUtils.getMessage(e1));
                    continue;
                }

                // Find the policy version and open it
                for (int i = 2; i < tabInfo.length; i++) {
                    String versionOrdinal = tabInfo[i];

                    // Find the policy version
                    PolicyVersion fullPolicyVersion;
                    try {
                        fullPolicyVersion = Registry.getDefault().getPolicyAdmin().findPolicyVersionForPolicy(Goid.parseGoid(policyGoid), Long.parseLong(versionOrdinal));
                        if (fullPolicyVersion == null) {
                            log.info("The policy version (goid=" + policyGoid + ", version=" + versionOrdinal + ") does not exist, so the policy editor workspace cannot open a tab for it.");
                            continue;
                        }
                    } catch (FindException e) {
                        log.info("The policy version (goid=" + policyGoid + ", version=" + versionOrdinal + ") cannot be found, so the policy editor workspace cannot open a tab for it.");
                        continue;
                    } catch (Exception e1) {
                        log.info("Cannot open a tab for the policy version (goid=" + policyGoid + ", version=" + versionOrdinal + "): " + ExceptionUtils.getMessage(e1));
                        continue;
                    }

                    // Open the policy version into the workspace
                    new EditPolicyAction(entityWithPolicyNode, true, fullPolicyVersion).invoke();
                    someTabOpened = true;
                }
            }
        }

        // If there is no any tab opened yet, then open a Home page
        if (! someTabOpened) {
            getHomeAction().actionPerformed(null);
        }
    }

    /**
     * Looks for missing primary license, license invalidity, and license expiration. Creates warning
     * messages for each issue the user should be warned about.
     *
     * @param compositeLicense the CompositeLicense to analyse and show any warnings for
     * @param expiryWarningPeriod the period of time before a license expiry in which the user should be warned of it
     * @return a String containing all warning messages
     */
    private String collectLicenseWarnings(@NotNull final CompositeLicense compositeLicense, final long expiryWarningPeriod) {
        final StringBuilder message = new StringBuilder();

        if (!Registry.getDefault().getLicenseManager().isPrimaryLicenseInstalled()) {
            message.append("There is no valid Primary License installed on this gateway.\n");
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");

        // check for FeatureLicenses expiring soon
        if (compositeLicense.hasValid()) {
            final Map<Long, FeatureLicense> validFeatureLicenses = compositeLicense.getValidFeatureLicenses();

            ArrayList<Long> expiringKeys = new ArrayList<>();

            final long warningPeriodEnd = expiryWarningPeriod + System.currentTimeMillis();

            for (Long idKey : validFeatureLicenses.keySet()) {
                if (!validFeatureLicenses.get(idKey).isLicensePeriodExpiryAfter(warningPeriodEnd)) {
                    expiringKeys.add(idKey);
                }
            }

            if(expiringKeys.size() > 0) {
                if (expiringKeys.size() == 1) {
                    message.append("One of the");
                } else {
                    message.append(validFeatureLicenses.size());
                }

                message.append(" licenses installed on this gateway will expire soon:\n");

                for (Long idKey : expiringKeys) {
                    message.append("- License ")
                            .append(idKey)
                            .append(" expires ")
                            .append(sdf.format(validFeatureLicenses.get(idKey).getExpiryDate()))
                            .append(".\n");
                }
            }
        }

        // check for expired FeatureLicenses
        if (compositeLicense.hasExpired()) {
            final Map<Long, FeatureLicense> expiredFeatureLicenses = compositeLicense.getExpiredFeatureLicenses();

            if (expiredFeatureLicenses.size() == 1) {
                message.append("A license installed on this gateway has expired:\n");
            } else {
                message.append(expiredFeatureLicenses.size())
                        .append(" of the licenses installed on this gateway have expired:\n");
            }

            for (Long idKey : expiredFeatureLicenses.keySet()) {
                message.append("- License ")
                        .append(idKey)
                        .append(" expired ")
                        .append(sdf.format(expiredFeatureLicenses.get(idKey).getExpiryDate()))
                        .append(".\n");
            }
        }

        // check for invalid FeatureLicenses (including start date not reached yet) and LicenseDocuments
        if (compositeLicense.hasInvalidLicenseDocuments() || compositeLicense.hasInvalidFeatureLicenses()) {
            final Map<Long, FeatureLicense> invalidFeatureLicenses = compositeLicense.getInvalidFeatureLicenses();

            int invalidCount = compositeLicense.getInvalidFeatureLicenses().size() +
                            compositeLicense.getInvalidLicenseDocuments().size();
            if (invalidCount == 1) {

                message.append("A license installed on this gateway is invalid:\n");
            } else {
                message.append(invalidCount)
                        .append(" of the licenses installed on this gateway are invalid:\n");
            }

            for (Long idKey : invalidFeatureLicenses.keySet()) {
                FeatureLicense l = invalidFeatureLicenses.get(idKey);

                message.append("- License ")
                        .append(idKey);

                if (!l.hasTrustedIssuer()) {
                    message.append(" was not signed by a trusted issuer.");
                } else if (!l.isProductEnabled(BuildInfo.getProductName()) ||
                        !l.isVersionEnabled(BuildInfo.getProductVersionMajor(), BuildInfo.getProductVersionMinor())) {
                    message.append(" does not grant access to this version of this product.");
                } else if (!l.isLicensePeriodStartBefore(System.currentTimeMillis())) {
                    message.append(" is not yet valid.");
                }

                message.append("\n");
            }

            int invalidDocsCount = compositeLicense.getInvalidLicenseDocuments().size();

            if (invalidDocsCount == 1) {
                message.append("- One license is malformed.\n");
            } else if (invalidDocsCount > 1) {
                message.append("- ")
                        .append(invalidDocsCount)
                        .append(" licenses are malformed.\n");
            }
        }

        return message.toString();
    }

    private void showLicenseWarnings(final String warningMessage) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
            DialogDisplayer.OptionListener callback = new DialogDisplayer.OptionListener() {
                @Override
                public void reportResult(int retval) {
                    if (retval == JOptionPane.YES_OPTION) {
                        showManageLicensesDialog();
                    }
                }
            };

            DialogDisplayer.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                    warningMessage + "\nWould you like to view the license manager now?",
                    "Gateway License Warning",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    callback);
            }
        });
    }

    private void showManageLicensesDialog() {
        ManageLicensesDialog dlg = new ManageLicensesDialog(TopComponents.getInstance().getTopParent());
        dlg.pack();
        Utilities.centerOnParentWindow(dlg);
        dlg.setModal(true);
        DialogDisplayer.display(dlg);
    }

    private void showWarningBanner() {
        //determine if the warning banner is configured to be displayed
        String warningBannerMessage = TopComponents.getInstance().getLogonWarningBanner();

        if (warningBannerMessage != null && !warningBannerMessage.trim().isEmpty()) {
            //warning banner is configured, need to display warning banner
            final Frame parent = TopComponents.getInstance().getTopParent();
            final WarningBanner warningBanner = new WarningBanner(parent, warningBannerMessage);
            warningBanner.pack();
            Utilities.centerOnScreen(warningBanner);
            DialogDisplayer.suppressSheetDisplay(warningBanner);
            DialogDisplayer.display(warningBanner, parent, new Runnable() {
                @Override
                public void run() {
                    if (!warningBanner.isOkClicked() && !warningBanner.isCancelClicked()) {
                        TopComponents.getInstance().disconnectFromGateway();
                    }
                }
            });
        }
    }

    /**
     * called when the policy currently edited gets deleted
     */
    public void firePolicyEditDone() {
        getValidateMenuItem().setEnabled(false);
        getSaveAndActivateMenuItem().setEnabled(false);
        getSaveOnlyMenuItem().setEnabled(false);
        getExportMenuItem().setEnabled(false);
        getImportMenuItem().setEnabled(false);
        getMigrateNamespacesMenuItem().setEnabled(false);
    }

    public void firePolicyEdit(PolicyEditorPanel policyPanel) {
        // enable the items that make sense to show when a policy is being edited
        getValidateMenuItem().setAction(policyPanel.getValidateAction());
        getSaveAndActivateMenuItem().setAction(policyPanel.getSaveAndActivateAction());
        getSaveOnlyMenuItem().setAction(policyPanel.getSaveOnlyAction());
        getExportMenuItem().setAction(policyPanel.getExportAction());
        getImportMenuItem().setAction(policyPanel.getImportAction());
        getShowAssertionCommentsMenuItem().setAction(policyPanel.getHideShowCommentAction(getShowAssertionCommentsMenuItem()));
        getShowAssertionLineNumbersMenuItem().setAction(policyPanel.getShowAssertionLineNumbersAction(getShowAssertionLineNumbersMenuItem()));
        getMigrateNamespacesMenuItem().setAction(policyPanel.getMigrateNamespacesAction());
    }

    public void fireGlobalAction(final String actionName, final Component source) {
        if (actionName.equals(L7_F3)) {
            f3Action.actionPerformed(new ActionEvent(source, ActionEvent.ACTION_PERFORMED, L7_F3));
        } else if (actionName.equals(L7_SHIFT_F3)) {
            shiftF3Action.actionPerformed(new ActionEvent(source, ActionEvent.ACTION_PERFORMED, L7_SHIFT_F3));
        }
    }

    public void showSSLWarning(Date expiry) {
        final String title = "Gateway SSL Warning";
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        String dateStr = sdf.format(expiry);
        final StringBuffer message = new StringBuffer();

        message.append("The currently installed SSL certificate for this gateway ");
        if (new Date().before(expiry)) {
            message.append("expires ");
        } else {
            message.append("expired ");
        }

        message.append(dateStr);
        message.append(".");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                        message.toString(),
                        title,
                        JOptionPane.OK_OPTION,
                        null);
            }
        });
    }

    /**
     * called by the applet after the content has been stolen out of the MainWindow.
     */
    public void notifyRootPaneStolen() {
        setRootPane(new JRootPane());
        setLayeredPane(getLayeredPane());
        setGlassPane(getGlassPane());
        setContentPane(getContentPane());
        setJMenuBar(null);
        validate();
    }

    public void showHelpTopicsRoot() {
        ssmApplication.showHelpTopicsRoot();
    }

    public void showNoPrivilegesErrorMessage() {
        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
            "The requested action could not be performed because the applet is running\n" +
                "in untrusted mode.  If you wish to enable this feature, and are willing to\n" +
                "run the applet in trusted mode, adjust your Java plug-in settings to trust\n" +
                "this signed applet and then reload the page.",
            "Disallowed by browser settings",
            JOptionPane.WARNING_MESSAGE, null);
    }

    @Override
    public void showSheet(JInternalFrame sheet) {
        if (isApplet()) {
            AppletMain applet = (AppletMain) TopComponents.getInstance().getComponent(AppletMain.COMPONENT_NAME);
            if (applet == null)
                throw new IllegalStateException("Running as applet but there's no applet");
            DialogDisplayer.showSheet(applet, sheet);
            return;
        }

        Frame topParent = TopComponents.getInstance().getTopParent();
        if (topParent != this && topParent instanceof RootPaneContainer) {
            DialogDisplayer.showSheet((RootPaneContainer) topParent, sheet);
            return;
        }

        DialogDisplayer.showSheet(this, sheet);
    }

    public void unregisterComponents() {
        TopComponents.getInstance().unregisterComponent(AssertionsTree.NAME);
        TopComponents.getInstance().unregisterComponent(IdentityProvidersTree.NAME);
        TopComponents.getInstance().unregisterComponent(ServicesAndPoliciesTree.NAME);
        TopComponents.getInstance().unregisterComponent(ProgressBar.NAME);
        TopComponents.getInstance().unregisterComponent("mainWindow");
    }

    public AbstractTreeNode getServicesFolderNode() {
        return rootNode;
    }

    public AbstractTreeNode getPoliciesFolderNode() {
        return rootNode;
    }

    private void closeAllWindows() {
        // Find and destroy dialogs
        Window[] owned = TopComponents.getInstance().getTopParent().getOwnedWindows();
        if (owned != null) {
            for ( final Window window : owned ) {
                if ( window.isDisplayable() ) {
                    window.dispose();
                }
            }
        }

        // Find and dispose windows (gets windows our applet owns)
        Frame topFrame = TopComponents.getInstance().getTopParent();
        Frame[] frames = Frame.getFrames();
        if (frames != null) {
            for (Frame frame : frames) {
                if (frame != topFrame) {
                    frame.dispose();
                }
            }
        }
    }

    /**
     * @return the SSG SSL cert, or null if not connected or we can't look it up due to an error.
     */
    public X509Certificate[] getServerSslCertChain() {
        if (serverSslCert == null) {
            try {
                serverSslCert = Registry.getDefault().getTrustedCertManager().getSSGSslCert();
            } catch (IOException e) {
                log.log(Level.WARNING, "Unable to look up Gateway SSL cert: " + ExceptionUtils.getMessage(e), e);
            } catch (CertificateException e) {
                log.log(Level.WARNING, "Unable to look up Gateway SSL cert: " + ExceptionUtils.getMessage(e), e);
            }
        }
        return new X509Certificate[]{serverSslCert};
    }

    /**
     * @return the SSG audit signing cert, or null if not connected or we can't look it up due to an error.
     */
    public X509Certificate getSsgAuditSigningCert() {
        if (auditSigningCert == null) {
            try {
                auditSigningCert = Registry.getDefault().getTrustedCertManager().getSSGAuditSigningCert();
            } catch (IOException e) {
                log.log(Level.WARNING, "Unable to look up Gateway audit signing cert: " + ExceptionUtils.getMessage(e), e);
            } catch (CertificateException e) {
                log.log(Level.WARNING, "Unable to look up Gateway audit signing cert: " + ExceptionUtils.getMessage(e), e);
            }
        }
        return auditSigningCert;
    }

    public Action getPublishInternalServiceAction() {
        if (publishInternalServiceAction != null) {
            return publishInternalServiceAction;
        }
        publishInternalServiceAction = new PublishInternalServiceAction();
        disableUntilLogin(publishInternalServiceAction);
        return publishInternalServiceAction;
    }

    public Action getPublishReverseWebProxyAction() {
        if (publishReverseWebProxyAction != null) {
            return publishReverseWebProxyAction;
        }
        publishReverseWebProxyAction = new PublishReverseWebProxyAction();
        disableUntilLogin(publishReverseWebProxyAction);
        return publishReverseWebProxyAction;
    }

    /**
     * A recursive method which will parse out the menu to see if it can determine if the menu should be enabled/disabled
     * based on the children of the given menu.
     *
     * @param menu the menu to determine to enable/disable
     * @return TRUE if menu should be enabled, otherwise FALSE to make menu disabled.
     */
    private boolean isMenuItemActive(JMenu menu) {
        if (menu.getMenuComponentCount() == 0) {    //no more children
            if (menu.getAction() != null) {
                return menu.getAction().isEnabled();
            } else {
                return false;
            }
        } else {
            final Component[] components = menu.getMenuComponents();
            //allow all sub components to be asked whether they have an enabled component or not.
            boolean foundEnabledSubComponent = false;
            for (Component component : components) {
                if (component instanceof JMenu) {
                    if (isMenuItemActive((JMenu) component)) {
                        foundEnabledSubComponent = true;
                    }
                } else if (component instanceof JMenuItem) { //a menu item
                    if (((JMenuItem) component).getAction() != null) {
                        final boolean isEnabled = ((JMenuItem) component).getAction().isEnabled();
                        if (isEnabled) {
                            foundEnabledSubComponent = true;
                        }
                    }
                }
            }
            return foundEnabledSubComponent;
        }
    }

    /**
     * Helper method to enable/disable the menu items for the given root menu.
     *
     * @param menu The root of the menu.
     */
    private void updateTopMenu(JMenu menu) {
        updateTopMenu(menu.getPopupMenu());
    }

    private void updateTopMenu(JPopupMenu menu) {
        final Component[] components = menu.getComponents();
        for (Component component : components) {
            if (component instanceof JMenu) {
                component.setEnabled(isMenuItemActive((JMenu) component));
            }
        }
    }

    /**
     * Updates the top menu to enable/disable the menu items
     */
    private void updateTopMenu() {
        if (isApplet()) {
            updateTopMenu(appletManagePopUpMenu);
        } else {
            updateTopMenu(fileMenu);
            updateTopMenu(editMenu);
            updateTopMenu(tasksMenu);
            updateTopMenu(viewMenu);
            updateTopMenu(helpMenu);
        }
    }

    /**
     * Forces to set the status of the filter and sort menu to the specified status.
     *
     * @param status TRUE to enable the filter and sort menu.
     */
    private void setFilterAndSortMenuEnabled(boolean status) {
        getFilterServiceAndPolicyTreeMenu().setEnabled(status);
        getSortServiceAndPolicyTreeMenu().setEnabled(status);
    }

    public static final class RemoteActivityListener implements Functions.Nullary<Void> {
        @Override
        public Void call() {
            TopComponents.getInstance().updateLastRemoteActivityTime();
            return null;
        }
    }
}