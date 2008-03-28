package com.l7tech.console;

import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.common.Authorizer;
import com.l7tech.common.InvalidLicenseException;
import com.l7tech.common.License;
import com.l7tech.common.audit.LogonEvent;
import com.l7tech.common.gui.util.*;
import com.l7tech.common.security.rbac.AttemptedDeleteAll;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.console.action.*;
import com.l7tech.console.auditalerts.AuditAlertChecker;
import com.l7tech.console.auditalerts.AuditAlertConfigBean;
import com.l7tech.console.auditalerts.AuditAlertOptionsAction;
import com.l7tech.console.auditalerts.AuditAlertsNotificationPanel;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.console.panels.LicenseDialog;
import com.l7tech.console.panels.LogonDialog;
import com.l7tech.console.panels.PreferencesDialog;
import com.l7tech.console.panels.WorkSpacePanel;
import com.l7tech.console.panels.identity.finder.Options;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.security.LogonListener;
import com.l7tech.console.security.PermissionRefreshListener;
import com.l7tech.console.security.SecurityProvider;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.identity.IdentitiesRootNode;
import com.l7tech.console.tree.identity.IdentityProvidersTree;
import com.l7tech.console.tree.policy.PolicyToolBar;
import com.l7tech.console.util.*;
import com.l7tech.console.logging.ErrorManager;
import com.l7tech.console.logging.CascadingErrorHandler;
import com.l7tech.identity.User;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
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
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


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
    /**
     * the resource bundle name
     */
    private static
    ResourceBundle resapplication = ResourceBundle.getBundle("com.l7tech.console.resources.console");

    /* this class classloader */
    private final ClassLoader cl = MainWindow.class.getClassLoader();

    private JMenuBar mainJMenuBar = null;
    private JMenu fileMenu = null;
    private JMenu editMenu = null;
    private JMenu tasksMenu = null;
    private JMenu viewMenu = null;
    private JMenu helpMenu = null;
    private JMenu newProviderSubMenu = null;

    private JMenuItem connectMenuItem = null;
    private JMenuItem disconnectMenuItem = null;
    private JMenuItem changePasswordMenuItem = null;
    private JMenuItem exitMenuItem = null;
    private JMenuItem menuItemPref = null;
    private JMenuItem auditMenuItem = null;
    private JMenuItem fromFileMenuItem = null;
    private JMenuItem manageJmsEndpointsMenuItem = null;
    private JMenuItem manageKerberosMenuItem = null;
    private JMenuItem manageCertificatesMenuItem = null;
    private JMenuItem managePrivateKeysMenuItem = null;
    private JMenuItem manageSsgConnectorsMenuItem = null;
    private JMenuItem revokeCertificatesMenuItem = null;
    private JMenuItem manageGlobalSchemasMenuItem = null;
    private JMenuItem manageClusterPropertiesMenuItem = null;
    private JMenuItem manageRolesMenuItem = null;
    private JMenuItem dashboardMenuItem;
    private JMenuItem manageClusterLicensesMenuItem = null;
    private JMenuItem helpTopicsMenuItem = null;
    private JMenuItem manageAuditAlertsMenuItem;
    private JMenuItem editPolicyMenuItem = null;
    private JMenuItem servicePropertiesMenuItem = null;
    private JMenuItem publishToUDDIMenuItem = null;
    private JMenuItem deleteServiceMenuItem = null;

    // actions
    private Action refreshAction = null;
    private FindIdentityAction findAction = null;
    private Action prefsAction = null;
    private Action removeNodeAction = null;
    private Action connectAction = null;
    private Action disconnectAction = null;
    private Action toggleStatusBarAction = null;
    private Action togglePolicyMessageArea = null;
    private ChangePasswordAction changePasswordAction = null;
    private PublishServiceAction publishServiceAction = null;
    private PublishNonSoapServiceAction publishNonSoapServiceAction = null;
    private CreateServiceWsdlAction createServiceAction = null;
    private CreatePolicyAction createPolicyAction;
    private ViewGatewayAuditsAction viewGatewayAuditsWindowAction;
    private ViewAuditsOrLogsFromFileAction auditOrLogFromFileAction;
    private ManageJmsEndpointsAction manageJmsEndpointsAction = null;
    private ManageKerberosAction manageKerberosAction = null;
    private ManageRolesAction manageRolesAction = null;
    private HomeAction homeAction = new HomeAction();
    private NewGroupAction newInernalGroupAction;
    private NewLdapProviderAction newLDAPProviderAction;
    private NewFederatedIdentityProviderAction newPKIProviderAction;
    private ManageCertificatesAction manageCertificatesAction = null;
    private ManagePrivateKeysAction managePrivateKeysAction = null;
    private ManageSsgConnectorsAction manageSsgConnectorsAction = null;
    private RevokeCertificatesAction revokeCertificatesAction = null;
    private ManageGlobalSchemasAction manageGlobalSchemasAction = null;
    private ManageClusterPropertiesAction manageClusterPropertiesAction = null;
    private ShowDashboardAction showDashboardAction = null;
    private ManageClusterLicensesAction manageClusterLicensesAction = null;
    private NewInternalUserAction newInernalUserAction;
    private AuditAlertOptionsAction manageAuditAlertsAction;
    private ManageLogSinksAction manageLogSinksAction = null;


    private JPanel frameContentPane = null;
    private JPanel mainPane = null;
    private JPanel statusBarPane = null;
    private JLabel statusMsgLeft = null;
    private JLabel statusMsgRight = null;


    private JToolBar toolBarPane = null;
    private PolicyToolBar policyToolBar = null;
    private JSplitPane mainSplitPane = null;
    private JPanel mainLeftPanel = null;
    private JPanel mainSplitPaneRight = null;
    private JTabbedPane paletteTabbedPane;

    public static final String TITLE = "SSG Management Console";
    public static final String NAME = "main.window"; // registered
    private EventListenerList listenerList = new WeakEventListenerList();
    private LogonListener closeWindowListener;
    // cached credential manager
    private String connectionContext = "";
    private String connectionID = "";
    private ServicesAndPoliciesTree servicesAndPoliciesTree;
    private IdentityProvidersTree identityProvidersTree;
    private JMenuItem validateMenuItem;
    private JMenuItem importMenuItem;
    private JMenuItem exportMenuItem;
    private JMenuItem saveAndActivateMenuItem;
    private JMenuItem saveOnlyMenuItem;
    private boolean disconnected = false;
    private String ssgURL;
    private SsmApplication ssmApplication;
    private IdentitiesRootNode identitiesRootNode;
    private final DefaultMutableTreeNode SERVICE_POLICY_ROOT = new DefaultMutableTreeNode("NOT SHOWN Services and Policies");
    private ServicesFolderNode servicesFolderNode;
    private PoliciesFolderNode policiesFolderNode;
    private JTextPane descriptionText;
    private JSplitPane verticalSplitPane;
    private double preferredVerticalSplitLocation = 0.57;
    private double preferredHorizontalSplitLocation = 0.27;
    private boolean maximizeOnStart = false;

    private final SsmPreferences preferences;

    private AuditAlertsNotificationPanel auditAlertBar;
    private AuditAlertChecker auditAlertChecker;
    private X509Certificate serverSslCert;


    /**
     * MainWindow constructor comment.
     * @param app  the application bean
     */
    public MainWindow(SsmApplication app) {
        super(TITLE);
        ssmApplication = app;
        this.preferences = (SsmPreferences)app.getApplicationContext().getBean("preferences");
        if (preferences == null) throw new IllegalStateException("Internal error: no preferences bean");
        initialize();
    }

    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if(visible) {
            toFront();
            if(maximizeOnStart) {
                setExtendedState(MAXIMIZED_BOTH);
                resetSplitLocations();
            }
        }
    }

    /**
     * add the ConnectionListener
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
            ((LogonListener) listener).onLogoff(event);
        }
        disconnected = true;
        descriptionText.setText("");
    }

    public boolean isDisconnected() {
        return disconnected;
    }

    /**
     * The SSG URL this ssm is connected to (excluding the /ssg suffix).
     *
     * @return the url of the SSG we are currently connected to.
     */
    public String ssgURL() {
        return ssgURL;
    }

    /** @return the SsmApplication for which this MainWindow was created. */
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
     * Return the ChangePasswordMenuItem property value.
     *
     * @param accel if true, an accelerator key will be set on the menu item if it is created.
     * @return JMenuItem
     */
    private JMenuItem getChangePasswordMenuItem(final boolean accel) {
        if (changePasswordMenuItem != null)
            return changePasswordMenuItem;

        changePasswordMenuItem = new JMenuItem(getChangePasswordAction());

        if (accel) {
            int mnemonic = changePasswordMenuItem.getText().toCharArray()[2];
            changePasswordMenuItem.setMnemonic(mnemonic);
            changePasswordMenuItem.setAccelerator(
                    KeyStroke.getKeyStroke(Character.toUpperCase(mnemonic), ActionEvent.ALT_MASK));
        }

        return changePasswordMenuItem;
    }


    /**
     * Return the menuItemPref property value.
     *
     * @return JMenuItem
     */
    private JMenuItem getMenuItemPreferences() {
        if (menuItemPref == null) {
            menuItemPref = new JMenuItem(getPreferencesAction());
            //menuItemPref.setFocusable(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/preferences.gif"));
            menuItemPref.setIcon(icon);
            int mnemonic = menuItemPref.getText().toCharArray()[0];
            menuItemPref.setMnemonic(mnemonic);
            menuItemPref.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
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
            menu.add(getPublishToUDDIMenuItem());
            menu.add(getDeleteServiceMenuItem());

            menu.addSeparator();

            menu.add(getConnectMenuItem());
            menu.add(getDisconnectMenuItem());
            menu.add(getChangePasswordMenuItem(true));
            if (!isApplet()) {
                menu.add(getMenuItemPreferences());
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

    private JMenuItem getPublishToUDDIMenuItem() {
        if (publishToUDDIMenuItem == null) {
            publishToUDDIMenuItem = new JMenuItem();
            publishToUDDIMenuItem.setEnabled(false);
            Icon icon = new ImageIcon(cl.getResource(RESOURCE_PATH + "/xmlObject16.gif"));
            publishToUDDIMenuItem.setIcon(icon);
            publishToUDDIMenuItem.setText("Publish Policy to UDDI");
            //int mnemonic = publishToUDDIMenuItem.getText().toCharArray()[0];
            //publishToUDDIMenuItem.setMnemonic(mnemonic);
            //publishToUDDIMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return publishToUDDIMenuItem;
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
            int mnemonic = exportMenuItem.getText().toCharArray()[0];
            exportMenuItem.setMnemonic(mnemonic);
            exportMenuItem.setAccelerator(KeyStroke.getKeyStroke(mnemonic, ActionEvent.ALT_MASK));
        }
        return exportMenuItem;
    }

    /** @return the preferences bean. never null. */
    public SsmPreferences getPreferences() {
        return preferences;
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

            editMenu = menu;
        }
        return editMenu;
    }

    /**
     * Return the tasksMenu property value.
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
            menu.add(getFindAction());

            menu.addSeparator();

            menu.add(getPublishServiceAction());
            menu.add(getCreateServiceAction());
            menu.add(getCreatePolicyAction());
            menu.add(getPublishNonSoapServiceAction());
            menu.addSeparator();

            menu.add(getManageCertificatesMenuItem());
            menu.add(getManagePrivateKeysMenuItem());
            menu.add(getRevokeCertificatesMenuItem());
            menu.add(getManageGlobalSchemasMenuItem());
            menu.add(getManageClusterPropertiesActionMenuItem());
            menu.add(getManageSsgConnectorsAction());
            menu.add(getManageJmsEndpointsMenuItem());
            menu.add(getManageKerberosMenuItem());
            menu.add(getManageRolesMenuItem());
            menu.add(getManageAuditAlertOptionsMenuItem());
            menu.add(getManageLogSinksAction());


            int mnemonic = menu.getText().toCharArray()[0];
            menu.setMnemonic(mnemonic);

            tasksMenu = menu;
        }
        return tasksMenu;
    }

    private JMenuItem getManageAuditAlertOptionsMenuItem() {
        if (manageAuditAlertsMenuItem == null) manageAuditAlertsMenuItem = new JMenuItem(getManageAuditAlertsAction());
        return manageAuditAlertsMenuItem;
    }

    private Action getManageAuditAlertsAction() {
        if (manageAuditAlertsAction == null) {
            manageAuditAlertsAction = AuditAlertOptionsAction.getInstance();
            manageAuditAlertsAction.addAuditWatcher(getAuditAlertBar());
            manageAuditAlertsAction.setEnabled(false);
            this.addLogonListener(manageAuditAlertsAction);
            addPermissionRefreshListener(manageAuditAlertsAction);
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
            newProviderSubMenu.add(getNewFederatedIdentityProviderAction());
        }
        return newProviderSubMenu;
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

        if (!isApplet()) {
            jcm = new JCheckBoxMenuItem(getToggleStatusBarToggleAction());
            jcm.setSelected(getPreferences().isStatusBarBarVisible());
            menu.add(jcm);
        }

        menu.addSeparator();

        JMenuItem item = new JMenuItem(getRefreshAction());
        item.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0));
        menu.add(item);

        menu.addSeparator();

        menu.add(getDashboardMenuItem());
        menu.add(getAuditMenuItem());
        menu.add(getFromFileMenuItem());

        int mnemonic = menu.getText().toCharArray()[0];
        menu.setMnemonic(mnemonic);

        viewMenu = menu;

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
              public void actionPerformed(ActionEvent event) {

                  SwingUtilities.invokeLater(new Runnable() { public void run() {
                      if (isApplet() && !LogonDialog.isValidSessionID()) { // due to click "Disconnect" or reload browser tabs
                          AppletMain applet = (AppletMain)TopComponents.getInstance().getComponent(AppletMain.COMPONENT_NAME);
                          applet.redirectToServlet();
                      } else {
                          LogonDialog.logon(TopComponents.getInstance().getTopParent(), logonListenr);
                      }
                  }});
              }
          };
        connectAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        return connectAction;
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
               * @param event the event that occured
               */
              public void actionPerformed(ActionEvent event) {
                  try {
                      getWorkSpacePanel().clearWorkspace(); // vetoable

                      // Must disable actions first, since doing so may attempt to make admin calls
                      disconnectFromGateway();

                      SecurityProvider securityProvider = Registry.getDefault().getSecurityProvider();
                      if (securityProvider != null) securityProvider.logoff();
                      LogonDialog.setPreconfiguredSessionId(null);
                  } catch (ActionVetoException e) {
                      // action vetoed
                  }
              }
          };
        disconnectAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        return disconnectAction;
    }

    private ChangePasswordAction getChangePasswordAction() {
        if (changePasswordAction != null) {
            return changePasswordAction;
        }
        changePasswordAction = new ChangePasswordAction();
        changePasswordAction.setEnabled(false);
        this.addLogonListener(changePasswordAction);
        addPermissionRefreshListener(changePasswordAction);
        return changePasswordAction;
    }

    /**
     * @return the <code>Action</code> for the publish service
     */
    private PublishServiceAction getPublishServiceAction() {
        if (publishServiceAction != null) {
            return publishServiceAction;
        }
        publishServiceAction = new PublishServiceAction();
        publishServiceAction.setEnabled(false);
        this.addLogonListener(publishServiceAction);
        addPermissionRefreshListener(publishServiceAction);
        return publishServiceAction;
    }

    private PublishNonSoapServiceAction getPublishNonSoapServiceAction() {
        if (publishNonSoapServiceAction != null) {
            return publishNonSoapServiceAction;
        }
        publishNonSoapServiceAction = new PublishNonSoapServiceAction();
        publishNonSoapServiceAction.setEnabled(false);
        this.addLogonListener(publishNonSoapServiceAction);
        addPermissionRefreshListener(publishNonSoapServiceAction);
        return publishNonSoapServiceAction;
    }


    /**
     * @return the <code>Action</code> for the create service
     */
    private CreateServiceWsdlAction getCreateServiceAction() {
        if (createServiceAction != null) {
            return createServiceAction;
        }
        createServiceAction = new CreateServiceWsdlAction();
        createServiceAction.setEnabled(false);
        this.addLogonListener(createServiceAction);
        addPermissionRefreshListener(createServiceAction);
        return createServiceAction;
    }


    private NewInternalUserAction getNewInternalUserAction() {
        if (newInernalUserAction != null) return newInernalUserAction;
        newInernalUserAction = new NewInternalUserAction(null) {
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
            public void onLogon(LogonEvent e) {
                super.onLogon(e);
                final DefaultMutableTreeNode root =
                  (DefaultMutableTreeNode)getIdentitiesTree().getModel().getRoot();
                node = (AbstractTreeNode)root;
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
            public void onLogon(LogonEvent e) {
                super.onLogon(e);
                final DefaultMutableTreeNode root =
                  (DefaultMutableTreeNode)getIdentitiesTree().getModel().getRoot();
                node = (AbstractTreeNode)root;
            }
        };
        MainWindow.this.addLogonListener(newLDAPProviderAction);
        newLDAPProviderAction.setEnabled(false);
        addPermissionRefreshListener(newLDAPProviderAction);
        return newLDAPProviderAction;
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
                  public void onLogon(LogonEvent e) {
                      refreshAction.setEnabled(true);
                  }
                  public void onLogoff(LogonEvent e) {
                      refreshAction.setEnabled(false);
                  }
              };
              {
                addLogonListener(ll);
              }

              public void actionPerformed(ActionEvent event) {
                  Collection<Refreshable> alreadyRefreshed = new ArrayList<Refreshable>();
                  // no matter what, if id provider tree exists, always refresh it
                  if (identityProvidersTree != null) {
                      identityProvidersTree.refresh(identitiesRootNode);
                      alreadyRefreshed.add(identityProvidersTree);
                  }
                  // no matter what, if service tree exists, always refresh it
                  if (servicesAndPoliciesTree != null) {
                      servicesAndPoliciesTree.refresh(servicesFolderNode);
                      servicesAndPoliciesTree.refresh(policiesFolderNode);
                      alreadyRefreshed.add(servicesAndPoliciesTree);
                  }
                  // no matter what, always refresh the policy editor panel if it is showing
                  final WorkSpacePanel cw = TopComponents.getInstance().getCurrentWorkspace();
                  final JComponent jc = cw.getComponent();
                  if (jc != null && jc instanceof PolicyEditorPanel) {
                      PolicyEditorPanel pep = (PolicyEditorPanel)jc;
                      if (pep.getPolicyTree() != null) {
                          alreadyRefreshed.add(pep.getPolicyTree());
                          pep.getPolicyTree().refresh();
                          alreadyRefreshed.add(pep.getPolicyTree());
                      }
                  }
                  final KeyboardFocusManager kbm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                  final Component c = kbm.getFocusOwner();
                  log.finest("the focus owner is " + (c==null ? "NULL" : c.getClass()));
                  if (c instanceof Refreshable) {

                      try {
                          Refreshable r = (Refreshable)c;
                          if (!alreadyRefreshed.contains(r)) {
                              if (r.canRefresh()) {
                                  log.finest("Invoke refresh on " + c.getClass());
                                  r.refresh();
                              }
                          }
                      } finally {
                          SwingUtilities.invokeLater(new Runnable() {
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
        servicesAndPoliciesTree.refresh(policiesFolderNode);
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
               * @see Action#removePropertyChangeListener
               */
              public void actionPerformed(ActionEvent event) {
                  JCheckBoxMenuItem item = (JCheckBoxMenuItem)event.getSource();
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
               * @see Action#removePropertyChangeListener
               */
              public void actionPerformed(ActionEvent event) {
                  JCheckBoxMenuItem item = (JCheckBoxMenuItem)event.getSource();
                  final boolean selected = item.isSelected();
                  final WorkSpacePanel cw = TopComponents.getInstance().getCurrentWorkspace();
                  final JComponent c = cw.getComponent();
                  if (c != null && c instanceof PolicyEditorPanel) {
                      PolicyEditorPanel pe = (PolicyEditorPanel)c;
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
    private Action getFindAction() {
        if (findAction != null) return findAction;

        Options options = new Options();
        options.setEnableDeleteAction(true);
        options.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        findAction = new FindIdentityAction(options);
        String aDesc = resapplication.getString("Find_MenuItem_text_description");
        findAction.putValue(Action.SHORT_DESCRIPTION, aDesc);
        addPermissionRefreshListener(findAction);

        return findAction;
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
              public void actionPerformed(ActionEvent event) {
                  SwingUtilities.invokeLater(new Runnable() { public void run() {
                      PreferencesDialog dialog = new PreferencesDialog(MainWindow.this, true, isConnected());
                      dialog.pack();
                      Utilities.centerOnScreen(dialog);
                      dialog.setResizable(false);
                      DialogDisplayer.display(dialog);
                  }});
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
        getFindAction().setEnabled(connected);
        getDisconnectAction().setEnabled(connected);
        getCreatePolicyAction().setEnabled(connected);
        getConnectAction().setEnabled(!connected);
        // these are enabled if connected AND a service is selected in the tree
        if (!connected) {
            getEditPolicyMenuItem().setEnabled(connected);
            getServicePropertiesMenuItem().setEnabled(connected);
            getPublishToUDDIMenuItem().setEnabled(connected);
            getDeleteServiceMenuItem().setEnabled(connected);
        }
        homeAction.setEnabled(connected);
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

        panel.setTransferHandler(new FileDropTransferHandler(new FileDropTransferHandler.FileDropListener(){
            public boolean acceptFiles(File[] files) {
                boolean accepted = false;
                for (File file : files) {
                    accepted = accepted | getAuditOrLogsFromFileAction().openFile(file);
                }

                return accepted;
            }

            public boolean isDropEnabled() {
                return true;
            }
        }, new FilenameFilter(){
            public boolean accept(File dir, String name) {
                boolean accept = false;
                if(name != null && (name.endsWith(".ssga") || name.endsWith(".ssgl"))) {
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
        JTree tree = (JTree)TopComponents.getInstance().getComponent(AssertionsTree.NAME);
        if (tree != null)
            return tree;
        tree = new AssertionsTree();
        tree.setShowsRootHandles(true);
        tree.setBorder(null);
        final JTree finalTree = tree;
        tree.addTreeSelectionListener(new TreeSelectionListener() {
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

                if (description == null) {
                    description = ""; // clear currently displayed description
                }
                descriptionText.setText(description);
                descriptionText.getCaret().setDot(0);
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
        IdentityProvidersTree tree = (IdentityProvidersTree)TopComponents.getInstance().getComponent(IdentityProvidersTree.NAME);
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
        JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        if (tree != null)
            return tree;

        servicesAndPoliciesTree = new ServicesAndPoliciesTree();
        servicesAndPoliciesTree.setShowsRootHandles(false);
        servicesAndPoliciesTree.setRootVisible(false);
        TopComponents.getInstance().registerComponent(ServicesAndPoliciesTree.NAME, servicesAndPoliciesTree);
		servicesAndPoliciesTree.addTreeSelectionListener(new TreeSelectionListener() {
            public void valueChanged(TreeSelectionEvent treeSelectionEvent) {
                boolean enable = servicesAndPoliciesTree.getSelectionCount() > 0;
                if (enable) {
                    if (!(servicesAndPoliciesTree.getSelectionModel().getSelectionPaths()[0].getLastPathComponent() instanceof ServiceNode)) {
                        enable = false;
                    }
                }
                getEditPolicyMenuItem().setEnabled(enable);
                getServicePropertiesMenuItem().setEnabled(enable);
                getPublishToUDDIMenuItem().setEnabled(enable);
                getDeleteServiceMenuItem().setEnabled(enable);
                if (enable) {
                    // go get the actions from the node
                    ServiceNode node = (ServiceNode)(servicesAndPoliciesTree.getSelectionModel().getSelectionPaths()[0].getLastPathComponent());
                    getEditPolicyMenuItem().setAction(new EditPolicyAction(node));
                    getServicePropertiesMenuItem().setAction(new EditServiceProperties(node));
                    getPublishToUDDIMenuItem().setAction(new PublishPolicyToUDDIRegistry(node));
                    getDeleteServiceMenuItem().setAction(new DeleteServiceAction(node) {
                        public String getName() {
                            return "Delete Service";
                        }
                    });
                }
            }
        });
        return servicesAndPoliciesTree;
    }

    /**
     * Initialize the workspace. Invoked on successfull login.
     */
    private void initalizeWorkspace() {
        DefaultTreeModel treeModel = new FilteredTreeModel(null);
        final AbstractTreeNode paletteRootNode = new AssertionsPaletteRootNode("Policy Assertions");
        treeModel.setRoot(paletteRootNode);
        final JTree assertionPaletteTree = getAssertionPaletteTree();
        assertionPaletteTree.setRootVisible(true);
        assertionPaletteTree.setModel(treeModel);
        TreePath path = new TreePath(paletteRootNode.getPath());
        assertionPaletteTree.setSelectionPath(path);

        identitiesRootNode = new IdentitiesRootNode("Identity Providers");
        treeModel = new FilteredTreeModel(null);
        treeModel.setRoot(identitiesRootNode);
        getIdentitiesTree().setRootNode(identitiesRootNode);

        final JTree identitiesTree = getIdentitiesTree();
        identitiesTree.setRootVisible(true);
        identitiesTree.setModel(treeModel);

        final String url = preferences.getString(SsmPreferences.SERVICE_URL);
        servicesFolderNode = new ServicesFolderNode(Registry.getDefault().getServiceManager(), "Services @ " + url);
        policiesFolderNode = new PoliciesFolderNode(Registry.getDefault().getPolicyAdmin(), "Policies @ " + url);
        SERVICE_POLICY_ROOT.removeAllChildren();
        SERVICE_POLICY_ROOT.insert(servicesFolderNode, 0);
        SERVICE_POLICY_ROOT.insert(policiesFolderNode, 1);

        DefaultTreeModel servicesTreeModel = new FilteredTreeModel(null);
        servicesTreeModel.setRoot(SERVICE_POLICY_ROOT);
        getServicesAndPoliciesTree().setModel(servicesTreeModel);
        getServicesAndPoliciesTree().setShowsRootHandles(true);
        getServicesAndPoliciesTree().setRootVisible(false);

        TreeSelectionListener treeSelectionListener =
          new TreeSelectionListener() {
              private final JTree assertionPalette =
                assertionPaletteTree;
              private final JTree services = getServicesAndPoliciesTree();

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
        manageJmsEndpointsAction.setEnabled(false);
        this.addLogonListener(manageJmsEndpointsAction);
        addPermissionRefreshListener(manageJmsEndpointsAction);
        return manageJmsEndpointsAction;
    }

    private Action getManageKerberosAction() {
        if (manageKerberosAction != null)
            return manageKerberosAction;


        manageKerberosAction = new ManageKerberosAction();
        manageKerberosAction.setEnabled(false);
        this.addLogonListener(manageKerberosAction);
        addPermissionRefreshListener(manageKerberosAction);
        return manageKerberosAction;
    }

    private Action getManageCertificatesAction() {
        if (manageCertificatesAction != null)
            return manageCertificatesAction;

        manageCertificatesAction = new ManageCertificatesAction();
        manageCertificatesAction.setEnabled(false);
        this.addLogonListener(manageCertificatesAction);
        addPermissionRefreshListener(manageCertificatesAction);
        return manageCertificatesAction;
    }

    private Action getManageSsgConnectorsAction() {
        if (manageSsgConnectorsAction != null)
            return manageSsgConnectorsAction;

        manageSsgConnectorsAction = new ManageSsgConnectorsAction();
        manageSsgConnectorsAction.setEnabled(false);
        this.addLogonListener(manageSsgConnectorsAction);
        addPermissionRefreshListener(manageSsgConnectorsAction);
        return manageSsgConnectorsAction;
    }

    private Action getManageLogSinksAction() {
        if (manageLogSinksAction != null)
            return manageLogSinksAction;

        manageLogSinksAction = new ManageLogSinksAction();
        manageLogSinksAction.setEnabled(false);
        this.addLogonListener(manageLogSinksAction);
        addPermissionRefreshListener(manageLogSinksAction);
        return manageLogSinksAction;
    }

    private Action getManagePrivateKeysAction() {
        if (managePrivateKeysAction != null)
            return managePrivateKeysAction;

        managePrivateKeysAction = new ManagePrivateKeysAction();
        managePrivateKeysAction.setEnabled(false);
        this.addLogonListener(managePrivateKeysAction);
        addPermissionRefreshListener(managePrivateKeysAction);
        return managePrivateKeysAction;
    }

    private Action getRevokeCertificatesAction() {
        if (revokeCertificatesAction != null)
            return revokeCertificatesAction;

        revokeCertificatesAction = new RevokeCertificatesAction();
        revokeCertificatesAction.setEnabled(false);
        this.addLogonListener(revokeCertificatesAction);
        addPermissionRefreshListener(revokeCertificatesAction);
        return revokeCertificatesAction;
    }

    private Action getManageGlobalSchemasAction() {
        if (manageGlobalSchemasAction != null) return manageGlobalSchemasAction;
        manageGlobalSchemasAction = new ManageGlobalSchemasAction();
        manageGlobalSchemasAction.setEnabled(false);
        this.addLogonListener(manageGlobalSchemasAction);
        addPermissionRefreshListener(manageGlobalSchemasAction);
        return manageGlobalSchemasAction;
    }

    private Action getManageClusterPropertiesAction() {
        if (manageClusterPropertiesAction != null) return manageClusterPropertiesAction;
        manageClusterPropertiesAction = new ManageClusterPropertiesAction();
        manageClusterPropertiesAction.setEnabled(false);
        this.addLogonListener(manageClusterPropertiesAction);
        addPermissionRefreshListener(manageClusterPropertiesAction);
        return manageClusterPropertiesAction;
    }

    private Action getShowDashboardAction() {
        if (showDashboardAction != null) return showDashboardAction;
        showDashboardAction = new ShowDashboardAction();
        showDashboardAction.setEnabled(false);
        this.addLogonListener(showDashboardAction);
        addPermissionRefreshListener(showDashboardAction);
        return showDashboardAction;
    }

    private Action getManagerClusterLicensesAction() {
        if (manageClusterLicensesAction != null) return manageClusterLicensesAction;
        manageClusterLicensesAction = new ManageClusterLicensesAction();
        manageClusterLicensesAction.setEnabled(false);
        this.addLogonListener(manageClusterLicensesAction);
        addPermissionRefreshListener(manageClusterLicensesAction);
        return manageClusterLicensesAction;
    }

    private Action getGatewayAuditWindowAction() {
        if (viewGatewayAuditsWindowAction != null) return viewGatewayAuditsWindowAction;
        viewGatewayAuditsWindowAction = new ViewGatewayAuditsAction(){
            protected void performAction() {
                AuditAlertsNotificationPanel auditAlert = getAuditAlertBar();
                if( auditAlert != null )
                    auditAlert.auditsViewed();

                super.performAction();
            }
        };
        viewGatewayAuditsWindowAction.setEnabled(false);
        this.addLogonListener(viewGatewayAuditsWindowAction);
        addPermissionRefreshListener(viewGatewayAuditsWindowAction);
        return viewGatewayAuditsWindowAction;
    }

    private ViewAuditsOrLogsFromFileAction getAuditOrLogsFromFileAction() {
        if (auditOrLogFromFileAction != null) return auditOrLogFromFileAction;
        auditOrLogFromFileAction = new ViewAuditsOrLogsFromFileAction();
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
        getMainLeftPanel().addComponentListener(new ComponentAdapter(){
            public void componentResized(ComponentEvent e) {
                preferredHorizontalSplitLocation = mainSplitPane.getDividerLocation() / (double)(mainSplitPane.getWidth() - mainSplitPane.getDividerSize());
            }
        });
        addWindowListener(new WindowAdapter() {
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
     * @param tb  the toolbar
     * @param a   the action
     * @return  the new button
     */
    private JButton tbadd(JToolBar tb, Action a) {
        JButton b = tb.add(a);
        b.setFont(new Font("Dialog", 1, 10));
        b.setText((String)a.getValue(Action.NAME));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setHorizontalTextPosition(SwingConstants.RIGHT);
        b.setFocusable(false);
        return b;
    }

    /**
     * Add a popup menu button to a toolbar.
     *
     * @param tb    the toolbar
     * @param menu  the popup menu
     * @param iconResource  resource path of icon for menu button
     */
    private void tbadd(JToolBar tb, final JPopupMenu menu, String iconResource) {
        final JButton[] but = new JButton[] { null };
        final Action showAction = new AbstractAction(menu.getLabel()) {
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
        tbadd(toolBarPane, homeAction);

        if (isApplet()) {
            // Ensure that clipboard actions get initialized properly, even though we won't display this menu
            // (side effects, hack hack)
            getEditMenu();

            JPopupMenu menu = new JPopupMenu("Manage...");
            menu.add(getManageCertificatesMenuItem());
            menu.add(getManagePrivateKeysMenuItem());
            menu.add(getManageGlobalSchemasMenuItem());
            menu.add(getManageClusterPropertiesActionMenuItem());
            menu.add(getManageSsgConnectorsAction());
            menu.add(getManageJmsEndpointsMenuItem());
            menu.add(getManageKerberosMenuItem());
            menu.add(getManageRolesMenuItem());
            menu.add(getManageAuditAlertOptionsMenuItem());
            menu.add(getManageClusterLicensesMenuItem());
            menu.add(getChangePasswordMenuItem(false));
            menu.add(getManageLogSinksAction());
            Utilities.removeToolTipsFromMenuItems(menu);
            tbadd(toolBarPane, menu, RESOURCE_PATH + "/Properties16.gif");

            menu = new JPopupMenu("Monitor...");
            menu.add(getDashboardMenuItem());
            menu.add(getAuditMenuItem());
            menu.add(getFromFileMenuItem());
            Utilities.removeToolTipsFromMenuItems(menu);
            tbadd(toolBarPane, menu, RESOURCE_PATH + "/AnalyzeGatewayLog16x16.gif");

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

    /** @return true if we are running as an Applet */
    public boolean isApplet() {
        return ssmApplication.isApplet();
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
                if(fromFile>=0 && fromFile<=1.0)
                    splitLocation = fromFile;
                else
                    log.log(Level.WARNING, "Invalid divider location '"+fromFile+"'.");
            } catch (NumberFormatException nfe) {
                log.log(Level.WARNING, "Unable to parse divider location '"+s+"'.");
            }
        }
        if(splitLocation>=0 && splitLocation<=1.0) {
            splitPane.setDividerLocation(splitLocation);
        }
        else {
            log.warning("Ignoring invalid divider location '"+splitLocation+"'.");
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
            }
        } else {
            if (treePanel.getTabCount() > 1) {
                // Remove unwanted Identity Providers tab
                treePanel.remove(1);
            }
        }
    }

    private JTabbedPane getPaletteTabbedPane() {
        if (paletteTabbedPane != null) return paletteTabbedPane;
        paletteTabbedPane = new JTabbedPane();
        paletteTabbedPane.setPreferredSize(new Dimension(140, 280));
        JScrollPane assertionScroller = new JScrollPane(getAssertionPaletteTree());
        configureScrollPane(assertionScroller);
        paletteTabbedPane.addTab("Assertions", assertionScroller);
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

        paletteSections.addComponentListener(new ComponentAdapter(){
            public void componentResized(ComponentEvent e) {
                preferredVerticalSplitLocation = verticalSplitPane.getDividerLocation() / (double)(verticalSplitPane.getHeight() - verticalSplitPane.getDividerSize());
            }
        });
        addWindowListener(new WindowAdapter(){
            public void windowOpened(WindowEvent e) {
                preferredVerticalSplitLocation =
                        setSplitLocation("tree.split.divider.location",
                                preferredVerticalSplitLocation,
                                verticalSplitPane);
            }
        });

        JScrollPane serviceScroller = new JScrollPane(getServicesAndPoliciesTree());
        configureScrollPane(serviceScroller);
        treePanel = new JTabbedPane();
        treePanel.addTab("Services", serviceScroller);

        verticalSplitPane.setTopComponent(paletteSections);
        verticalSplitPane.setBottomComponent(treePanel);

        mainLeftPanel = new JPanel(new BorderLayout());
        mainLeftPanel.add(verticalSplitPane, BorderLayout.CENTER);
        mainLeftPanel.add(getPolicyToolBar(), BorderLayout.EAST);
        mainLeftPanel.setBorder(null);
        return mainLeftPanel;
    }

    private Component getAssertionDescriptionPane() {
        if (descriptionText == null) {
            descriptionText= new JTextPane();
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
            getFindAction().setEnabled(false);
            return;
        }
        getRemoveNodeAction().setEnabled(node.canDelete());
        getFindAction().setEnabled(false);
        getRefreshAction().setEnabled(false);
    }

    /**
     * @see ActionEvent for details
     */
    private void exitMenuEventHandler() {
        if (isConnected()) {
            try {
                getWorkSpacePanel().clearWorkspace(); // vetoable
                disconnectFromGateway();
            } catch (ActionVetoException e) {
                return;
            }
        }
        String maximized = Boolean.toString(getExtendedState()==Frame.MAXIMIZED_BOTH);
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
              public void actionPerformed(ActionEvent e) {
                  exitMenuEventHandler();
              }
          });

        // HelpTopics listener
        getHelpTopicsMenuItem().
          addActionListener(new ActionListener() {
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
              public void propertyChange(final PropertyChangeEvent evt) {
                  if ("lookAndFeel".equals(evt.getPropertyName())) {
                      SwingUtilities.invokeLater(new Runnable() {
                          public void run() {
                              SwingUtilities.updateComponentTreeUI(TopComponents.getInstance().getTopParent());
                          }
                      });
                  }
              }
          };
        UIManager.addPropertyChangeListener(l);
    }


    // --- End Event listeners ---------------------------------------

    /**
     * Initialize the class.
     */
    private void initialize() {

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
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
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                if (getExtendedState() == Frame.NORMAL) {
                    preferences.setLastWindowSize(getSize());
                }
            }

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
        setTitle(resapplication.getString("SSG"));
        Image icon = getSmallLogoImage();
        ImageIcon imageIcon = new ImageIcon(icon);
        setIconImage(imageIcon.getImage());
        DialogDisplayer.setDefaultFrameIcon(new ImageIcon(icon));
        DialogDisplayer.setDefaultWindowImages(Arrays.asList(icon));
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
    }

    /**
     * Get the small logo image, suitable for use as a frame icon.
     *
     * @return the small logo image.  Never null
     * @throws RuntimeException if the icon resource can't be found
     */
    public static Image getSmallLogoImage() {
        String path = RESOURCE_PATH + "/layer7_logo_small_32x32.png";
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
            if(maximized) {
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
    private long lastActivityTime = System.currentTimeMillis();

    private void installInactivityTimerEventListener() {
        if (ssmApplication.isApplet()) return; // no inactivity timer on applet

        // AWT event listener
        final
        AWTEventListener listener =
          new AWTEventListener() {
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
        if (ssmApplication.isApplet()) return;  // timer disabled on applet

        long now = System.currentTimeMillis();
        double inactive = (now - lastActivityTime);
        if (Math.round(inactive / inactivityTimer.getDelay()) >= 1) { // match
            inactivityTimer.stop(); // stop timer
            MainWindow.this.getStatusMsgRight().
              setText("inactivity timeout expired; disconnecting...");
            // make sure it is invoked on event dispatching thread
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        getWorkSpacePanel().clearWorkspace();  // vetoable
                        MainWindow.this.disconnectFromGateway();
                        // add a top level dlg that indicates the connection was closed
                        DialogDisplayer.showMessageDialog(MainWindow.this,
                                                      "The SecureSpan Manager connection has been closed due\n" +
                                                      "to timeout. Any unsaved work will be lost.",
                                                      "Connection Timeout", JOptionPane.WARNING_MESSAGE, null);
                    } catch (ActionVetoException e1) {
                        // swallow, cannot happen from here
                    }
                }
            });
        }
    }

    final Timer
      inactivityTimer =
      new Timer(60 * 1000 * 20,
        new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                onInactivityTimerTick();
            }
        });

    public void updateLastActivityTime() {
        lastActivityTime = System.currentTimeMillis();
    }

    // -------------- inactivitiy timeout end (open your eyes) -------------------


    /**
     * set the inactivity timeout value
     *
     * @param newTimeout new inactivity timeout
     */
    public void setInactivitiyTimeout(int newTimeout) {
        int inactivityTimeout = newTimeout * 60 * 1000;
        if (!isConnected()) return;

        if (ssmApplication.isApplet()) {
            inactivityTimer.stop();
            log.log(Level.INFO, "inactivity timeout disabled for applet");
            return;
        }

        if (inactivityTimeout == 0) {
            if (inactivityTimer.isRunning()) {
                inactivityTimer.stop();
                log.log(Level.WARNING, "inactivity timeout disabled (timeout = 0)");
            }
        } else if (inactivityTimeout > 0) {
            //  substract 1 secs (tollerance)
            inactivityTimer.setDelay(inactivityTimeout);
            inactivityTimer.setInitialDelay(inactivityTimeout);
            if (inactivityTimer.isRunning()) {
                inactivityTimer.stop();
            }
            inactivityTimer.start();
            log.log(Level.INFO, "inactivity timeout enabled (timeout = " + inactivityTimeout + ")");
        } else {
            log.log(Level.WARNING, "incorrect timeout value " + inactivityTimeout);
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
    private void installClosingWindowHandler () {
        closeWindowListener = new LogonListener(){
            public void onLogon(LogonEvent e) {}

            public void onLogoff(LogonEvent e) {
                closeAllWindows();
            }
        };

       addLogonListener(closeWindowListener);
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

    private Action getManageRolesAction() {
        if (manageRolesAction == null) {
            manageRolesAction = new ManageRolesAction();
            manageRolesAction.setEnabled(false);
            this.addLogonListener(manageRolesAction);
            addPermissionRefreshListener(manageRolesAction);
        }
        return manageRolesAction;
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
        managePrivateKeysMenuItem = new JMenuItem(getManagePrivateKeysAction());

        return managePrivateKeysMenuItem;
    }

    public JMenuItem getManageSsgConnectorsMenuItem() {
        if (manageSsgConnectorsMenuItem != null)
            return manageSsgConnectorsMenuItem;
        return manageSsgConnectorsMenuItem = new JMenuItem(getManageSsgConnectorsAction());
    }

    public JMenuItem getRevokeCertificatesMenuItem() {
        if (revokeCertificatesMenuItem != null)
            return revokeCertificatesMenuItem;
        revokeCertificatesMenuItem = new JMenuItem(getRevokeCertificatesAction());

        return revokeCertificatesMenuItem;
    }

    public JMenuItem getManageGlobalSchemasMenuItem(){
        if (manageGlobalSchemasMenuItem != null) return manageGlobalSchemasMenuItem;
        manageGlobalSchemasMenuItem = new JMenuItem(getManageGlobalSchemasAction());
        return manageGlobalSchemasMenuItem;
    }

    public JMenuItem getManageClusterPropertiesActionMenuItem(){
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
        public void licenseChanged(ConsoleLicenseManager licenseManager) {
            JTree tree = getAssertionPaletteTree();
            if (tree == null || tree.getModel() == null) return; // not constructed yet
            AbstractTreeNode root = (AbstractTreeNode)tree.getModel().getRoot();
            Utilities.collapseTree(tree);
            root.removeAllChildren();
            root.reloadChildren();
            ((DefaultTreeModel)(tree.getModel())).nodeStructureChanged(root);
            tree.validate();
            tree.repaint();
        }
    };

    private final LicenseListener paletteTabbedPaneLicenseListener = new LicenseListener() {
        public void licenseChanged(ConsoleLicenseManager licenseManager) {
            showOrHideIdentityProvidersTab();
        }
    };

    private
    LogonDialog.LogonListener logonListenr =
      new LogonDialog.LogonListener() {
          /* invoked on authentication success */
          public void onAuthSuccess(String id, String serverURL) {
              ssgURL = serverURL;
              connectionID = id;
              String statusMessage = connectionID;
              connectionContext = "";

              /* clear cached server cert */
              serverSslCert = null;

              /* init rmi cl */
              if (!isApplet())
                RMIClassLoader.getDefaultProviderInstance();

              /* set the preferences */
              try {
                  SsmPreferences prefs = preferences;
                  connectionContext = " @ " + prefs.getString(SsmPreferences.SERVICE_URL);
                  if (prefs.rememberLoginId()) {
                      prefs.putProperty(SsmPreferences.LAST_LOGIN_ID, id);
                      prefs.store();
                  }
              } catch (IOException e) {
                  log.log(Level.WARNING, "onAuthSuccess()", e);
              }

              ClusterStatusAdmin clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();

              // Gather and cache the cluster license early, since components like the assertion palette will need it
              Registry reg = Registry.getDefault();
              License lic = null;         // if null, license is missing or invalid
              boolean licInvalid = false; // if true, license is invalid
              long licenseExpiryWarningPeriod = 0;
              try {
                  licenseExpiryWarningPeriod = clusterStatusAdmin.getLicenseExpiryWarningPeriod();
                  lic = clusterStatusAdmin.getCurrentLicense();
              } catch (InvalidLicenseException e1) {
                  licInvalid = true;
              } finally {
                  // Cache it
                  reg.getLicenseManager().setLicense(lic);
              }

              // Gather any modular assertions offered by this gateway early on as well, for the assertion palette
              try {
                  TopComponents.getInstance().getAssertionRegistry().updateModularAssertions();
              } catch (RuntimeException e) {
                  log.log(Level.WARNING, "Unable to update modular assertions: " + ExceptionUtils.getMessage(e) + ".",
                          ExceptionUtils.getDebugException(e));
              }

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
                  public void run() {
                      getStatusMsgLeft().setText(message);
                      initalizeWorkspace();
                      toggleConnectedMenus(true);
                      homeAction.actionPerformed(null);
                      MainWindow.this.
                        setInactivitiyTimeout(timeout);
                      MainWindow.this.fireConnected();
                  }
              });

              if (lic == null) {
                  showLicenseWarning(licInvalid, false, null);
              } else {
                  Authorizer auth = Registry.getDefault().getSecurityProvider();
                  if ( auth.hasPermission(new AttemptedDeleteAll(EntityType.ANY)) ) {
                      Date expiryDate = lic.getExpiryDate();
                      if (expiryDate!=null && (expiryDate.getTime()-licenseExpiryWarningPeriod) < System.currentTimeMillis()) {
                          showLicenseWarning(false, true, expiryDate);
                      } else {
                          X509Certificate[] sslCertificates = getServerSslCertChain();
                          if ( sslCertificates != null && sslCertificates.length > 0 ) {
                              Date sslExpiryDate = sslCertificates[0].getNotAfter();
                              if (sslExpiryDate!=null && (sslExpiryDate.getTime()-licenseExpiryWarningPeriod) < System.currentTimeMillis()) {
                                  showSSLWarning(sslExpiryDate);
                              }
                          }
                      }
                  }
              }
          }

          /* invoked on authentication failure */
          public void onAuthFailure() { }
      };

    /**
     * called when the policy currently edited gets deleted
     */
    public void firePolicyEditDone() {
        getValidateMenuItem().setEnabled(false);
        getSaveAndActivateMenuItem().setEnabled(false);
        getSaveOnlyMenuItem().setEnabled(false);
        getExportMenuItem().setEnabled(false);
        getImportMenuItem().setEnabled(false);
    }

    public void firePolicyEdit(PolicyEditorPanel policyPanel) {
        // enable the items that make sense to show when a policy is being edited
        getValidateMenuItem().setAction(policyPanel.getValidateAction());
        getSaveAndActivateMenuItem().setAction(policyPanel.getSaveAndActivateAction());
        getSaveOnlyMenuItem().setAction(policyPanel.getSaveOnlyAction());
        getExportMenuItem().setAction(policyPanel.getExportAction());
        getImportMenuItem().setAction(policyPanel.getImportAction());
    }

    public void showLicenseWarning(boolean invalidLicense, boolean expiresSoon, Date expiry) {
        final String title;
        final StringBuffer message;
        if (invalidLicense) {
            title = "Gateway Not Licensed";
            message = new StringBuffer("The currently installed license for this gateway is invalid.");
        } else if (expiresSoon) {
            title = "Gateway License Warning";
            SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
            String dateStr = sdf.format(expiry);
            message = new StringBuffer("The currently installed license for this gateway expires "+dateStr+".");
        } else {
            title = "Gateway Not Licensed";
            message = new StringBuffer("There is no license currently installed for this gateway.");
        }
        message.append("\n Would you like to view the license manager now?");

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                DialogDisplayer.OptionListener callback = new DialogDisplayer.OptionListener() {
                    public void reportResult(int retval) {
                        if (retval == JOptionPane.YES_OPTION) {
                            LicenseDialog dlg = new LicenseDialog(TopComponents.getInstance().getTopParent(),
                                                                  preferences.getString(SsmPreferences.SERVICE_URL));
                            dlg.pack();
                            Utilities.centerOnScreen(dlg);
                            dlg.setModal(true);
                            DialogDisplayer.display(dlg);
                        }
                    }
                };
                DialogDisplayer.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                                                  message.toString(), title,
                                                  JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE,
                                                  callback);
            }
        });
    }

    public void showSSLWarning(Date expiry) {
        final String title = "Gateway SSL Warning";
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z");
        String dateStr = sdf.format(expiry);
        final StringBuffer message = new StringBuffer();

        message.append("The currently installed SSL certificate for this gateway ");
        if (new Date().before(expiry))  {
            message.append("expires ");
        } else {
            message.append("expired ");
        }

        message.append(dateStr);
        message.append(".");

        SwingUtilities.invokeLater(new Runnable() {
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

    public void showSheet(JInternalFrame sheet) {
        if (isApplet()) {
            AppletMain applet = (AppletMain)TopComponents.getInstance().getComponent(AppletMain.COMPONENT_NAME);
            if (applet == null)
                throw new IllegalStateException("Running as applet but there's no applet");
            DialogDisplayer.showSheet(applet, sheet);
            return;
        }

        Frame topParent = TopComponents.getInstance().getTopParent();
        if (topParent != this && topParent instanceof RootPaneContainer) {
            DialogDisplayer.showSheet((RootPaneContainer)topParent, sheet);
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

    public ServicesFolderNode getServicesFolderNode() {
        return servicesFolderNode;
    }

    public PoliciesFolderNode getPoliciesFolderNode() {
        return policiesFolderNode;
    }

    private void closeAllWindows() {
        // Find and destroy dialogs
        Window[] owned = TopComponents.getInstance().getTopParent().getOwnedWindows();
        if (owned != null) {
            for (Window window : owned) {
                window.dispose();
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
                log.log(Level.WARNING, "Unable to look up SSG SSL cert: " + ExceptionUtils.getMessage(e), e);
            } catch (CertificateException e) {
                log.log(Level.WARNING, "Unable to look up SSG SSL cert: " + ExceptionUtils.getMessage(e), e);
            }
        }
        return new X509Certificate[] { serverSslCert };
    }
}