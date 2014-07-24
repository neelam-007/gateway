package com.l7tech.console.panels;

import com.l7tech.console.MainWindow;
import com.l7tech.console.action.ActionVetoException;
import com.l7tech.console.action.EditPolicyAction;
import com.l7tech.console.action.HomeAction;
import com.l7tech.console.event.ContainerVetoException;
import com.l7tech.console.event.VetoableContainerListener;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.console.panels.policydiff.PolicyDiffContext;
import com.l7tech.console.panels.policydiff.PolicyDiffWindow;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyToolBar;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Refreshable;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.TextUtils;
import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.plaf.TabbedPaneUI;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * <CODE>WorkSpacePanel</CODE> represents the main editing panel
 * for elements such as policies.
 */
public class WorkSpacePanel extends JPanel {
    public static final String NAME = "workspace.panel";
    public static final String PROPERTY_LAST_OPENED_POLICY_TABS = "last.opened.policy.tabs";

    private final static Logger log = Logger.getLogger(WorkSpacePanel.class.getName());
    private final static Icon closeTabIcon = new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/tabClose16.png"));
    private final static int CLOSE_TAB_ICON_RIGHT_GAP_WIDTH = 7;

    private final List<TabTitleComponentPanel> closedTabs = new ArrayList<>();  // Track all closed policy tabs in their closing order
    private final List<TabTitleComponentPanel> openedTabs = new ArrayList<>();  // Track all opened policy tabs in their opening order
    private final TabbedPane tabbedPane = new TabbedPane();

    /** TLS helper for container veto exceptions */
    private static ThreadLocal containerVetoException = new ThreadLocal() {
        protected synchronized Object initialValue() {
            return null;
        }
    };

    /**
     * default constructor
     */
    public WorkSpacePanel() {
        layoutComponents();
        initializePropertiesListener();
    }

    /**
     * Set the component in the work space and update other tabs' active status if applicable
     *
     * @param jc the new component to host
     */
    public void setComponent(final JComponent jc) throws ActionVetoException {
        // Check if HomePage tab exists or not.  If it does exist, set it as selected.
        // Note: HomePage index is always zero, since we always put HomePage at the first position.
        if (jc instanceof HomePagePanel && tabbedPane.getTabCount() > 0 && tabbedPane.getComponentAt(0) instanceof HomePagePanel) {
            tabbedPane.setSelectedIndex(0);
            return;
        }

        // Remove an existing component, which is associated with a same service/policy entity node with "jc" component.
        // The reason to remove the existing component is that EditPolicyAction.performAction() always creates a new
        // PolicyEditorPanel object with new context, so remove the existing one and add a new one, "jc" back to WorkSpacePanel.
        TabTitleComponentPanel foundAndRemoved = tabbedPane.removeExistingComponent(jc);

        // Remove some least-recently-used tab(s) without unsaved changes if the number of tabs reaches the "Maximum Num Of Policy Tabs" defined in Preferences.
        final int numOfTabsAllowed = getMaxNumOfTabsPropertyFromPreferences();
        final int numOfTabsOpened = tabbedPane.getTabCount();
        final List<JComponent> tabsAbleToBeClosed = findTabsWithoutUnsavedChanges();
        if (numOfTabsOpened >= numOfTabsAllowed) {
            if ((!openedTabs.isEmpty() && tabsAbleToBeClosed.isEmpty()) ||           // Case: all tabs are changes-unsaved tabs.
                (numOfTabsOpened - numOfTabsAllowed) >= tabsAbleToBeClosed.size()) { // Case: there are no enough number of without-unsaved-changes tabs to be automatically closed.
                // At these cases, we don't automatically close some without-unsaved-changes tabs and display a warning
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                    "You have reached the maximum number of tabs allowed (" + numOfTabsAllowed + ").\n" +
                    "Either close some tabs or increase the maximum in Preferences.",
                    "Open New Policy Tab Warning", JOptionPane.WARNING_MESSAGE, null);

                // If not found any existing component, it implies that user attempts to add a new tab, then return immediately to let user manually close some tabs.
                // Otherwise, add back the tab just removed by the above line, tabbedPane.removeExistingComponent(jc)
                if (foundAndRemoved == null) {
                    return;
                }
            }
            // Otherwise, automatically close some tabs without any unsaved changes
            else {
                for (int i = 0; i <= numOfTabsOpened - numOfTabsAllowed; i++) {
                    JComponent component = tabsAbleToBeClosed.get(i);
                    tabbedPane.remove(component);

                    // Move the last element of closedTabs to the first position due to automatic deletion.
                    // This is to avoid a tab recursive reopen.
                    if (!closedTabs.isEmpty()) {
                        TabTitleComponentPanel lastEl = closedTabs.remove(closedTabs.size() - 1);
                        closedTabs.add(0, lastEl);
                    }
                }
            }
        }

        if (containerVetoException.get() != null) {
            ContainerVetoException cve = (ContainerVetoException)containerVetoException.get();
            containerVetoException.set(null);
            throw new ActionVetoException(null, "workspace change vetoed", cve);
        }

        // Add the component (e.g., HomePagePanel or PolicyEditorPanel)
        if (jc instanceof HomePagePanel) {
            // Always set HomePagePanel at the index zero
            tabbedPane.add(jc, 0);
        } else {
            // addTab method sets title as null, since the next line tabPane.setTabComponentAt(...) will set a tab
            // component (a private class TabTitleComponentPanel object) to render the tab with a title plus a close button.
            tabbedPane.addTab(null, jc);
        }

        // Update the list of the last opened policies
        saveLastOpenedPolicyTabs();

        final int index = tabbedPane.indexOfComponent(jc);
        final TabTitleComponentPanel newTabCompPanel = new TabTitleComponentPanel(jc);
        tabbedPane.setTabComponentAt(index, newTabCompPanel); // Add a table title render object
        tabbedPane.setToolTipTextAt(index, jc.getName());

        if (jc instanceof PolicyEditorPanel) {
            // Save the copies of active and version for this policy editor panel, because active and version got
            // from subject are not guaranteed to be correct due to multiple policy versions opened.
            ((PolicyEditorPanel) jc).setOverrideVersionActive(((PolicyEditorPanel) jc).isVersionActive());
            ((PolicyEditorPanel) jc).setOverrideVersionNumber(((PolicyEditorPanel) jc).getVersionNumber());
        }

        // If foundAndRemoved object is not null, it means that the associated PolicyEditorPanel has been added back to
        // the workspace (see the above lines), so closedTabs should remove the foundAndRemoved object because it was
        // added back by the above line: tabbedPane.removeExistingComponent(jc).
        if (foundAndRemoved != null) {
            closedTabs.remove(foundAndRemoved);
        }

        // Add newTabCompPanel into the least-recently-used list
        openedTabs.add(newTabCompPanel);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (tabbedPane.indexOfComponent(jc) < 0)  return;

                tabbedPane.setSelectedComponent(jc);

                // Maybe the opened service/policy has a few other versions that have been added into the policy editor panel,
                // so other tabs should keep their versions number unchanged and update active status as inactive.
                if (jc instanceof PolicyEditorPanel) {
                    final boolean currentActiveStatus = ((PolicyEditorPanel) jc).isVersionActive();  // This method will load overrideVersionActive value first.
                    updateTabs(false, currentActiveStatus);
                }
            }
        });
    }

    /**
     * Get the property of "Maximum Number of Policy Tabs" saved in the policy manager preference.
     * @return an integer for the maximum number of policy tabs allowed to be opened.
     */
    private int getMaxNumOfTabsPropertyFromPreferences() {
        final SsmPreferences preferences = TopComponents.getInstance().getPreferences();
        if (preferences == null) return SsmPreferences.DEFAULT_MAX_NUM_POLICY_TABS;

        final String maxNumOfTabsProp = preferences.asProperties().getProperty(SsmPreferences.MAX_NUM_POLICY_TABS);
        int maxNumOfTabs;
        if (maxNumOfTabsProp == null) {
            maxNumOfTabs = SsmPreferences.DEFAULT_MAX_NUM_POLICY_TABS;
        } else {
            try {
                maxNumOfTabs = Integer.parseInt(maxNumOfTabsProp);
            } catch (NumberFormatException e) {
                maxNumOfTabs = SsmPreferences.DEFAULT_MAX_NUM_POLICY_TABS;
            }
        }
        return maxNumOfTabs;
    }

    /**
     * Get the property of "Policy Tabs Layout" saved in the policy manager preference.
     */
    private int getPolicyTabsLayoutFromPreferences() {
        final SsmPreferences preferences = TopComponents.getInstance().getPreferences();
        if (preferences == null) return SsmPreferences.DEFAULT_POLICY_TABS_LAYOUT;

        final String optionProp = preferences.asProperties().getProperty(SsmPreferences.POLICY_TABS_LAYOUT);
        int option;
        if (optionProp == null) {
            option = SsmPreferences.DEFAULT_POLICY_TABS_LAYOUT;
        } else {
            try {
                option = Integer.parseInt(optionProp);

                // The tab layout option must be either 0 or 1, since WRAP_TAB_LAYOUT = 0 and SCROLL_TAB_LAYOUT = 1.
                if (option != 0 && option != 1) {
                    option = SsmPreferences.DEFAULT_POLICY_TABS_LAYOUT;
                }
            } catch (NumberFormatException e) {
                option = SsmPreferences.DEFAULT_POLICY_TABS_LAYOUT;
            }
        }
        return option;
    }

    /**
     * Find all tabs associated with policies without unsaved changes
     * @return a HomePagePanel or PolicyEditorPanel without unsaved changes
     */
    private List<JComponent> findTabsWithoutUnsavedChanges() {
        List<JComponent> componentList = new ArrayList<>();
        JComponent component;
        for (TabTitleComponentPanel tabTitleComponentPanel: openedTabs) {
            component = tabTitleComponentPanel.getComponent();
            if (component instanceof HomePagePanel ||
                (component instanceof PolicyEditorPanel && !((PolicyEditorPanel) component).isUnsavedChanges())) {
                componentList.add(component);
            }
        }
        return componentList;
    }

    /**
     * This class is a Tab Title Renderer class.
     */
    private class TabTitleComponentPanel extends JPanel {
        private final static String EXTRA_SPACES = "    ";  // the spaces for drawing the close-tab icon
        private JLabel tabTitleLabel;   // holds the tab title
        private JComponent component;   // a PolicyEditorPanel or HomePanel object

        TabTitleComponentPanel(final JComponent component) {
            super(new FlowLayout(FlowLayout.CENTER));
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));

            if (component == null) {
                throw new IllegalArgumentException("The component must be specified.");
            }
            this.component = component;
            initializeComponents();
        }

        String getTabTitle() {
            return tabTitleLabel.getText();
        }

        void setTabTitle(String title) {
            tabTitleLabel.setText(title);

            // Check if the new title has a valid width for displaying
            validateTabTitleLength(title);
        }

        JComponent getComponent() {
            return component;
        }

        private void initializeComponents() {
            // Add a tab label to display tab title
            final String title = component.getName();
            tabTitleLabel = new JLabel(title);

            final Font font = tabTitleLabel.getFont();
            tabTitleLabel.setFont(new Font(font.getName(), Font.BOLD, 12));
            tabTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

            add(tabTitleLabel);
            add(new JLabel(EXTRA_SPACES)); // Add one other label holding some extra spaces, where the close-tab icon will be painted if the policy tab is very short.

            // This method should be called after tabTitleLabel is initialized.
            validateTabTitleLength(title);
        }

        /**
         * Check if a title is too long.  If so, truncate the middle part and replace the truncated part with "..."
         * to fit the width of the policy editor panel as possible.
         *
         * Then use the truncated title to set the policy tab title.
         */
        private void validateTabTitleLength(String title) {
            final int workspaceWidth = getWorkspaceWidth();
            final FontMetrics metrics = tabTitleLabel.getFontMetrics(tabTitleLabel.getFont());
            int totalWidth = SwingUtilities.computeStringWidth(metrics, title + EXTRA_SPACES);
            boolean changed = false;

            while (totalWidth > workspaceWidth && workspaceWidth > 0 && title.length() >= 11) {
                title = TextUtils.truncStringMiddle(title, title.length() - 6); // Truncate 6 characters every time
                totalWidth = SwingUtilities.computeStringWidth(metrics, title + EXTRA_SPACES);
                changed = true;
            }

            if (changed) {
                tabTitleLabel.setText(title);
            }
        }
    }

    /**
     * Get the width of the policy editor workspace
     * @return an integer for the workspace width.  Return a non-positive number if any errors occur.
     */
    private int getWorkspaceWidth() {
        int workspaceWidth = 0;

        final int tabLayout = getPolicyTabsLayoutFromPreferences();
        if (tabLayout == JTabbedPane.WRAP_TAB_LAYOUT) {
            for (Component component: tabbedPane.getComponents()) {
                // Check if the component is a BasicTabbedPaneUI.TabContainer
                if (component instanceof JPanel && component instanceof UIResource) {
                    workspaceWidth = component.getWidth();
                    break;
                }
            }
        } else if (tabLayout == JTabbedPane.SCROLL_TAB_LAYOUT) {
            int viewportWidth = 0;
            int scrollButtonsWidth = 0;

            for (Component component: tabbedPane.getComponents()) {
                // Check if the component is a BasicTabbedPaneUI.ScrollableTabViewport
                if (component instanceof JViewport) {
                    viewportWidth = component.getWidth();
                } else if (component.isVisible() && component instanceof  BasicArrowButton) {
                    scrollButtonsWidth += component.getWidth();
                }
            }

            workspaceWidth = viewportWidth - scrollButtonsWidth;
        }

        return workspaceWidth;
    }

    /**
     * get the component that the workspace panel is currently
     * hosting or null.
     * 
     * @return the workspace panel component or null
     */
    public JComponent getComponent() {
        return (JComponent)tabbedPane.getSelectedComponent();
    }

    public TabbedPane getTabbedPane() {
        return tabbedPane;
    }

    /**
     * Update policy tabs information such as saving last opened tabs and validating policy tab properties.
     */
    public void updatePolicyTabsProperties() {
        // If the gateway connection is lost, then do not update anything.
        if (TopComponents.getInstance().isConnectionLost()) {
            return;
        }

        validatePolicyTabProperties();
        saveLastOpenedPolicyTabs();
    }

    /**
     * Save policy goid and policy version number of all last opened policies into the property, "last.opened.policy.tabs".
     * So when the policy manager starts next time, these recorded policies will be loaded into the workspace.
     */
    public void saveLastOpenedPolicyTabs() {
        final SsmPreferences preferences = TopComponents.getInstance().getPreferences();
        if (preferences == null) return;

        final JComponent selectedComponent = getComponent();
        final Map<String, String> tokenVersionMap = new Hashtable<>();
        Component component;
        String goidToken, version;
        boolean homePageExisting = false;

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            component = tabbedPane.getComponentAt(i);

            // The selected component will be processed at last
            if (component == selectedComponent) continue;

            if (component instanceof HomePagePanel) {
                homePageExisting = true;
            } else if (component instanceof PolicyEditorPanel) {
                goidToken = ((OrganizationHeader) ((PolicyEditorPanel)component).getPolicyNode().getUserObject()).getStrId() + "#" + ((PolicyEditorPanel) component).getPolicyGoid().toString();
                version = String.valueOf(((PolicyEditorPanel) component).getVersionNumber());

                if (tokenVersionMap.keySet().contains(goidToken)) {
                    version = tokenVersionMap.get(goidToken) + "#" + version;
                }

                tokenVersionMap.put(goidToken, version);
            }
        }

        final StringBuilder sb = new StringBuilder();

        if (homePageExisting) {
            sb.append(HomePagePanel.HOME_PAGE_NAME);
        }

        String versionsToBeAdded = null;

        if (! tokenVersionMap.isEmpty()) {
            for (String token: tokenVersionMap.keySet()) {
                // Don't add these tabs, which have the same policy goid as the select component's policy goid,
                // but record their version numbers and process them later on.
                if (selectedComponent != null) {
                    String selectedComptGoidToken = null;
                    if (selectedComponent instanceof HomePagePanel) {
                        selectedComptGoidToken = HomePagePanel.HOME_PAGE_NAME;
                    } else if (selectedComponent instanceof PolicyEditorPanel) {
                        selectedComptGoidToken = ((OrganizationHeader) ((PolicyEditorPanel)selectedComponent).getPolicyNode().getUserObject()).getStrId() + "#" + ((PolicyEditorPanel) selectedComponent).getPolicyGoid().toString();
                    }
                    if (token.equals(selectedComptGoidToken)) {
                        versionsToBeAdded = tokenVersionMap.get(token);
                        continue;
                    }
                }

                if (sb.length() > 0) sb.append(", ");
                sb.append(token).append('#').append(tokenVersionMap.get(token));
            }
        }

        // Finally process the selected component
        if (selectedComponent != null) {
            if (sb.length() > 0) sb.append(", ");

            if (selectedComponent instanceof HomePagePanel) {
                sb.append(HomePagePanel.HOME_PAGE_NAME);
            } else {
                String selectedComptGoidToken = ((OrganizationHeader) ((PolicyEditorPanel)selectedComponent).getPolicyNode().getUserObject()).getStrId() + "#" + ((PolicyEditorPanel) selectedComponent).getPolicyGoid().toString();
                String selectedComptVersion = String.valueOf(((PolicyEditorPanel) selectedComponent).getVersionNumber());

                sb.append(selectedComptGoidToken).append('#').append(versionsToBeAdded == null? "" : versionsToBeAdded + "#").append(selectedComptVersion);
            }
        }

        if (sb.length() == 0) {
            preferences.remove(PROPERTY_LAST_OPENED_POLICY_TABS);
        } else {
            preferences.putProperty(PROPERTY_LAST_OPENED_POLICY_TABS, sb.toString());
        }

        try {
            preferences.store();
        } catch ( IOException e ) {
            log.warning("Unable to store preferences " + ExceptionUtils.getMessage(e));
        }
    }

    /**
     * Validate all policy tab properties, since some policy versions are stale, do not exist, or have invalid format.
     * If these cases occur, remove these policy tab settings from the policy tab properties.
     */
    private void validatePolicyTabProperties() {
        final SsmPreferences preferences = TopComponents.getInstance().getPreferences();
        if (preferences == null) return;

        final Map<String, Boolean> validationCache = new Hashtable<>();

        for (String propName: PolicyEditorPanel.ALL_POLICY_TAB_PROPERTIES) {
            // If no such property found, then check a next property
            String propValue = preferences.getString(propName);
            if (propValue == null) {
                continue;
            }

            StringBuilder updatedPropValueSB = new StringBuilder();
            String[] valueTokens = Pattern.compile("\\s*\\),\\s*").split(propValue);

            for (String token: valueTokens) {
                int delimiterIdx = token.indexOf('(');
                if (delimiterIdx == -1 || token.length() == delimiterIdx + 1) {
                    log.fine("The property '" + propName + "' has an invalid formatted property value, so its policy tab settings is removed from '" + propName + "'.");
                    continue;
                }

                String policyGoid = token.substring(0, delimiterIdx);

                // Validate a policy goid.  Before doing that, check if the policy goid has been validated or not.
                boolean validPolicy;
                if (validationCache.containsKey(policyGoid)) {
                    validPolicy = validationCache.get(policyGoid);
                } else {
                    validPolicy = true;
                    try {
                        Policy policy = Registry.getDefault().getPolicyAdmin().findPolicyByPrimaryKey(Goid.parseGoid(policyGoid));
                        if (policy == null) {
                            validPolicy = false;
                            log.fine("The policy (goid=" + policyGoid + ") does not exist, its policy tab setting is removed from '" + propName + "'.");
                        }
                    } catch (FindException e) {
                        validPolicy = false;
                        log.fine("The policy (goid=" + policyGoid + ") cannot be found, its policy tab setting is removed from '" + propName + "'.");
                    } catch (Throwable t) {
                        // For other exceptions such as PermissionDeniedException, RemoteInvocationFailureException, etc,
                        // we still treat these cases as a valid policy
                        log.fine(ExceptionUtils.getMessage(t));
                    }
                    validationCache.put(policyGoid, validPolicy);
                }
                if (! validPolicy) continue;

                String restPart = token.substring(delimiterIdx + 1);
                StringBuilder updatedVersionAndValueSB = new StringBuilder();
                String[] versionAndValueTokens = TextUtils.CSV_STRING_SPLIT_PATTERN.split(restPart);

                for (String versionAndValueToken: versionAndValueTokens) {
                    String[] versionAndValue = Pattern.compile("\\s*#\\s*").split(versionAndValueToken);
                    if (versionAndValue.length != 2) {
                        log.fine("The property '" + propName + "' has an invalid formatted property value, so its policy tab settings is removed from '" + propName + "'.");
                        continue;
                    }

                    String versionOrdinal = versionAndValue[0];
                    String settingValue = versionAndValue[1];
                    // Validate policy version number
                    try {
                        Long.parseLong(versionOrdinal);
                    } catch (NumberFormatException e) {
                        log.fine("The property '" + propName + "' has an invalid policy version number, '" + versionOrdinal + "', so its policy tab settings is removed from '" + propName + "'.");
                        continue;
                    }

                    // Validate policy versions (identified by policy goid and policy version ordinal).
                    // However, first check if the policy version has been validated already.
                    String versionUniqueKey = policyGoid + '#' + versionOrdinal;
                    boolean validPolicyVersion;
                    if (validationCache.containsKey(versionUniqueKey)) {
                        validPolicyVersion = validationCache.get(versionUniqueKey);
                    } else {
                        validPolicyVersion = true;
                        try {
                            PolicyVersion policyVersion = Registry.getDefault().getPolicyAdmin().findPolicyVersionForPolicy(Goid.parseGoid(policyGoid), Long.parseLong(versionOrdinal));
                            if (policyVersion == null) {
                                log.fine("The policy version (goid=" + policyGoid + ", version=" + versionOrdinal + ") does not exist, its policy tab setting is removed from '" + propName + "'.");
                                validPolicyVersion = false;
                            }
                        } catch (FindException e) {
                            validPolicyVersion = false;
                            log.fine("The policy version (goid=" + policyGoid + ", version=" + versionOrdinal + ") cannot be found, its policy tab setting is removed from '" + propName + "'.");
                        } catch (Throwable t) {
                            // For other exceptions such as PermissionDeniedException, RemoteInvocationFailureException, etc,
                            // we still treat these cases as a valid policy
                            log.fine(ExceptionUtils.getMessage(t));
                        }
                        validationCache.put(versionUniqueKey, validPolicyVersion);
                    }
                    if (! validPolicyVersion) continue;

                    // At this moment, policyGoid and verionOrdinal are good, then add the version and version back to the string builder
                    if (updatedVersionAndValueSB.length() > 0) updatedVersionAndValueSB.append(", ");

                    // Remove a redundant character ')'.
                    if (settingValue.endsWith(")")) settingValue = settingValue.substring(0, settingValue.length() - 1);

                    updatedVersionAndValueSB.append(versionOrdinal).append('#').append(settingValue);
                }
                if (updatedVersionAndValueSB.length() == 0) continue;

                if (updatedPropValueSB.length() > 0) updatedPropValueSB.append(", ");
                updatedPropValueSB.append(policyGoid).append('(').append(updatedVersionAndValueSB.toString()).append(')');
            }

            if (updatedPropValueSB.length() == 0) {
                preferences.remove(propName);
            } else if (! updatedPropValueSB.equals(propValue)) {
                preferences.putProperty(propName, updatedPropValueSB.toString());
            }
        }
        // Update the policy manager preferences in ssg.properties
        try {
            preferences.store();
        } catch ( IOException e ) {
            log.warning( "Unable to store preferences " + ExceptionUtils.getMessage(e));
        }
    }

    /**
     * Remove the active component that the workspace.
     * The {@link JComponent#getName() } sets the tab name.
     */
    public void clearWorkspace() throws ActionVetoException {
        tabbedPane.removeAll();
        if (containerVetoException.get() != null) {
            ContainerVetoException cve = (ContainerVetoException)containerVetoException.get();
            containerVetoException.set(null);
            throw new ActionVetoException(null, "workspace change vetoed", cve);
        }
        closedTabs.clear();
        openedTabs.clear();
    }

    /**
     * Remove the active component that the workspace.  This version cannot be vetoed by the user -- use if
     * there is a communications error talking to the SSG, for example.
     * <p/>
     * TODO There's no obvious reason why disconnecting from the SSG should necessarily have to require 
     *      clearing the workspace, other than the current problem that many workspace components cannot
     *      repaint without a working RMI connection.
     *
     * The {@link JComponent#getName() } sets the tab name.
     */
    public void clearWorkspaceUnvetoable() {
        tabbedPane.removeAll();
        closedTabs.clear();
        openedTabs.clear();
    }

    /**
     * Refresh all policy editor panels
     *
     * @return a list of refreshed Refreshable objects
     */
    public Collection<Refreshable> refreshWorkspace() {
        final Collection<Refreshable> alreadyRefreshed = new ArrayList<>();
        final Component selectedComponent = tabbedPane.getSelectedComponent();

        for  (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getComponentAt(i);

            if (component instanceof PolicyEditorPanel) {
                try {
                    tabbedPane.setSelectedComponent(component);
                    ((PolicyEditorPanel) component).refreshPolicyEditorPanel();

                    // After refresh policy, the policy tree is refreshed too, so add it into the refresh list
                    alreadyRefreshed.add(((PolicyEditorPanel) component).getPolicyTree());
                } catch (FindException e) {
                    // Report error, but still continue other tabs refresh
                    DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                        "Cannot retrieve the policy, '" + component.getName() + "'.",
                        "Refresh Error", JOptionPane.WARNING_MESSAGE, null);
                } catch (IOException e) {
                    // Report error, but still continue other tabs refresh
                    DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                        "Cannot parse the policy, '" + component.getName() + "'.",
                        "Refresh Error", JOptionPane.WARNING_MESSAGE, null);
                }
            }
        }

        if (tabbedPane.getTabCount() > 0 && selectedComponent != null) {
            tabbedPane.setSelectedComponent(selectedComponent);
        }

        return alreadyRefreshed;
    }

    /**
     * Refresh all opened polices containing a policy fragment with a specific policy fragment GUID.
     *
     * @param policyFragmentGuid: the policy fragment GUId used to match the opened policy, which contains the policy fragment.
     */
    public void refreshPoliciesContainingIncludedFragment(String policyFragmentGuid) {
        final Component selectedComponent = tabbedPane.getSelectedComponent();

        for  (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getComponentAt(i);

            if (component instanceof PolicyEditorPanel) {
                try {
                    String policyXml = WspWriter.getPolicyXml(((PolicyEditorPanel) component).getCurrentRoot().asAssertion());
                    if (policyXml != null && policyXml.contains(policyFragmentGuid)) {
                        ((PolicyEditorPanel) component).refreshPolicyEditorPanel();
                    }
                } catch (FindException e) {
                    // Report error, but still continue other tabs refresh
                    DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                        "Cannot retrieve the policy, '" + component.getName() + "'.",
                        "Refresh Error", JOptionPane.WARNING_MESSAGE, null);
                } catch (IOException e) {
                    // Report error, but still continue other tabs refresh
                    DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                        "Cannot parse the policy, '" + component.getName() + "'.",
                        "Refresh Error", JOptionPane.WARNING_MESSAGE, null);
                }
            }
        }

        if (tabbedPane.getTabCount() > 0 && selectedComponent != null) {
            tabbedPane.setSelectedComponent(selectedComponent);
        }
    }

    private Functions.UnaryVoidThrows<PolicyEditorPanel, FindException> reopenPolicyEditorPanel = new Functions.UnaryVoidThrows<PolicyEditorPanel, FindException>() {
        /**
         * Reopen a PolicyEditorPanel for a particular policy version, which may be updated by other manners (e.g., other policy managers)
         *
         * @param pep: the policy editor panel to be refreshed
         * @throws FindException: thrown if the policy version or the refreshed policy cannot be retrieved.
         */
        @Override
        public void call(final PolicyEditorPanel pep) throws FindException {
            // Get the full policy version, which contains policy xml for the version specified by "versionOrdinal".
            final Goid policyGoid = pep.getPolicyGoid();
            final Long versionOrdinal = pep.getVersionNumber();
            final PolicyVersion fullPolicyVersion = Registry.getDefault().getPolicyAdmin().findPolicyVersionForPolicy(policyGoid, versionOrdinal);

            if (fullPolicyVersion == null) {
                throw new FindException("Cannot find the policy '" + pep.getDisplayName() + "'");
            }

            // Get the refresh entity node (ServiceNode or PolicyNode) in the ServicesAndPoliciesTree, associated with the policy editor panel.
            final String entityNodeGoidString = ((OrganizationHeader) pep.getPolicyNode().getUserObject()).getStrId();
            final RootNode rootNode = ((ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME)).getRootNode();
            final EntityWithPolicyNode refreshedEntityNode = (EntityWithPolicyNode) rootNode.getNodeForEntity(Goid.parseGoid(entityNodeGoidString));

            // Invoke EditPolicyAction to update the policy editor panel.
            new EditPolicyAction(refreshedEntityNode, true, fullPolicyVersion).invoke();
        }
    };

    public void setReopenPolicyEditorPanel(final Functions.UnaryVoidThrows<PolicyEditorPanel, FindException> reopenPolicyEditorPanel) {
        this.reopenPolicyEditorPanel = reopenPolicyEditorPanel;
    }

    /**
     * Adds the specified container listener to receive container events
     * from this container.
     * If l is null, no exception is thrown and no action is performed.
     * This is a specialized version of the container listener, and it is
     * delegated to the Container that hosts the <i>workspace</i> component.
     * 
     * @param l the container listener
     */
    public synchronized void addWorkspaceContainerListener(ContainerListener l) {
        tabbedPane.addContainerListener(l);
    }

    /**
     * Removes the specified container listener so it no longer receives
     * container events from this container.
     * If l is null, no exception is thrown and no action is performed.
     * This is a specialized version of the container listener and it is
     * delegated to the Container that hosts the <i>workspace</i> component.
     * 
     * @param l the container listener
     */
    public synchronized void removeWorkspaceContainerListener(ContainerListener l) {
        tabbedPane.removeContainerListener(l);
    }


    /**
     * layout components on this panel
     */
    private void layoutComponents() {
        setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        addHierarchyListener(hierarchyListener);
        setLayout(new BorderLayout());
        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * initialize properties listener
     */
    private void initializePropertiesListener() {
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
                              SwingUtilities.updateComponentTreeUI(WorkSpacePanel.this);

                          }
                      });
                  }
              }
          };

        UIManager.addPropertyChangeListener(l);

        // Add a listener on workspace size, so redraw all tab titles if the workspace is resized.
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                    final Component component = tabbedPane.getComponentAt(i);
                    if (component instanceof PolicyEditorPanel) {
                        ((PolicyEditorPanel) component).updateHeadings();
                    }
                }
            }
        });

        tabbedPane.addContainerListener(new ContainerAdapter() {
            @Override
            public void componentRemoved(ContainerEvent e) {
                final Component c = e.getChild();
                if (c instanceof ContainerListener) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            log.fine("Removing container listener of type " + c.getClass());
                            tabbedPane.removeContainerListener((ContainerListener) c);
                        }
                    });
                }
            }
        });

        // Add a listener to listen tabs switching, so TopComponents and PolicyToolBar will re-register a different policy tree.
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                final Component component = tabbedPane.getSelectedComponent();
                if (component instanceof PolicyEditorPanel) {
                    final TopComponents topComponents = TopComponents.getInstance();
                    final PolicyTree prevPolicyTree = (PolicyTree) topComponents.getPolicyTree();
                    final PolicyToolBar policyToolBar = topComponents.getPolicyToolBar();

                    // Un-register the old PolicyTree
                    topComponents.unregisterComponent(PolicyTree.NAME);
                    policyToolBar.unregisterPolicyTree(prevPolicyTree);

                    // Re-register the current PolicyTree
                    final PolicyTree currPolicyTree = ((PolicyEditorPanel) component).getPolicyTree();
                    topComponents.registerComponent(PolicyTree.NAME, currPolicyTree);
                    policyToolBar.registerPolicyTree(currPolicyTree);

                    // Re-highlight previous selected node in the policy tree
                    // If no selected node, return immediately.
                    final TreeNode selectedNode = (AssertionTreeNode)currPolicyTree.getLastSelectedPathComponent();
                    if (selectedNode == null) return;

                    // Otherwise, clear all selections first
                    currPolicyTree.clearSelection();

                    // Then select the previous selected node
                    final TreePath path = new TreePath(((DefaultMutableTreeNode)selectedNode).getPath());
                    currPolicyTree.setSelectionPath(path);
                    currPolicyTree.requestFocusInWindow();
                } else if (component instanceof HomePagePanel) {
                    // If it is HomePage, then disable all buttons on PolicyToolBar
                    final PolicyToolBar policyToolBar = TopComponents.getInstance().getPolicyToolBar();
                    policyToolBar.disableAll();
                }
            }
        });
    }

    // hierarchy listener
    private final
    HierarchyListener hierarchyListener =
      new HierarchyListener() {
          /**
           * Called when the hierarchy has been changed.
           */
          public void hierarchyChanged(HierarchyEvent e) {
              long flags = e.getChangeFlags();
              if ((flags & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                  if (WorkSpacePanel.this.isShowing()) {
                  } else {
                  }
              }
          }
      };

    /**
     * The tabbed pane with veto support (for removing/adding tabs)
     */
    public class TabbedPane extends JTabbedPane {
        private final EventListenerList listenerList = new WeakEventListenerList();
        private final MouseTabListener mouseTabListener = new MouseTabListener(TabbedPane.this);

        public TabbedPane() {
            setUI(new WindowsTabbedPaneUI() {
                @Override
                protected MouseListener createMouseListener() {
                    return mouseTabListener;
                }

                // Paint the close icon (similar to a close button) used to close a policy tab
                @Override
                protected void paintTab(Graphics g, int tabPlacement, Rectangle[] rects, int tabIndex, Rectangle iconRect, Rectangle textRect) {
                    super.paintTab(g, tabPlacement, rects, tabIndex, iconRect, textRect);

                    final Rectangle tabRect = rects[tabIndex];
                    closeTabIcon.paintIcon(
                        tabPane, g,
                        (tabRect.x + tabRect.width - closeTabIcon.getIconWidth() - CLOSE_TAB_ICON_RIGHT_GAP_WIDTH), // X coordinate
                        (tabRect.y + (tabRect.height - closeTabIcon.getIconHeight()) / 2)                           // Y coordinate
                    );
                }
            });

            final int tabLayout = getPolicyTabsLayoutFromPreferences();
            setTabLayoutPolicy(tabLayout);
        }

        public MouseTabListener getMouseTabListener() {
            return mouseTabListener;
        }

        /**
         * Adds the specified container listener to receive container events
         * from this container.
         * If l is null, no exception is thrown and no action is performed.
         * This is a specialized version of the container listener, that
         * support the VetoableContainerListener
         * 
         * @param l the container listener
         */
        public synchronized void addContainerListener(ContainerListener l) {
            if (l instanceof VetoableContainerListener) {
                listenerList.add(VetoableContainerListener.class, (VetoableContainerListener)l);
            }
            super.addContainerListener(l);
        }

        /**
         * Removes the specified container listener so it no longer receives
         * container events from this container.
         * If l is null, no exception is thrown and no action is performed.
         * This is a specialized version of the container listener, that
         * supports the VetoableContainerListener
         * 
         * @param l the container listener
         */
        public synchronized void removeContainerListener(ContainerListener l) {
            if (l instanceof VetoableContainerListener) {
                listenerList.remove(VetoableContainerListener.class, (VetoableContainerListener)l);
            }
            super.removeContainerListener(l);
        }

        /**
         * Removes the tab at <code>index</code>. This method is overriden
         * for veto support.
         * 
         * @see #addTab(String, Component)
         * @see #insertTab
         */
        public void removeTabAt(int index) {
            containerVetoException.set(null);
            EventListener[] listeners = listenerList.getListeners(VetoableContainerListener.class);
            ContainerEvent e = new ContainerEvent(this, ContainerEvent.COMPONENT_REMOVED, getComponentAt(index));
            try {
                for (int i = 0; i < listeners.length; i++) {
                    EventListener listener = listeners[i];
                    ((VetoableContainerListener)listener).componentWillRemove(e);
                }

                // Before remove the tab, preserve ite tab component, just in case user wants to reopen it.
                // Also remove it from the least-recently-used tab list.
                final TabTitleComponentPanel tabTitleComponentPanel = (TabTitleComponentPanel) getTabComponentAt(index);
                closedTabs.add(tabTitleComponentPanel);
                openedTabs.remove(tabTitleComponentPanel);

                // Do real removal
                super.removeTabAt(index);
            } catch (ContainerVetoException e1) {
                containerVetoException.set(e1);
            }
        }

        /**
         * Inserts a <code>component</code>, at <code>index</code>. This method
         * is overridden for veto support.
         */
        public void insertTab(String title, Icon icon, Component component, String tip, int index) {
            // Only add HomePage once
            if (component instanceof HomePagePanel && getTabCount() > 0 && getTabComponentAt(0) instanceof HomePagePanel) {
                return;
            }
            containerVetoException.set(null);
            EventListener[] listeners = listenerList.getListeners(VetoableContainerListener.class);
            ContainerEvent e = new ContainerEvent(this, ContainerEvent.COMPONENT_ADDED, component);
            try {
                for (int i = 0; i < listeners.length; i++) {
                    EventListener listener = listeners[i];
                    ((VetoableContainerListener)listener).componentWillAdd(e);
                }
                super.insertTab(title, icon, component, tip, index);
            } catch (ContainerVetoException e1) {
                containerVetoException.set(e1);
            }
        }

        /**
         * Remove an existing component (PolicyEditorPanel or HomePanel), which is associated with a same service/policy
         * entity node with jc and has the same policy version with jc.
         *
         * Note: this method never removes the HomePage
         *
         * @param jc: the new panel will be added back to the work space.  It is used to identify the policy/service node
         *          and other related information.
         * @return a TabTitleComponentPanel object matched to jc (e.g., same Service/Policy node and same policy version)
         */
        private TabTitleComponentPanel removeExistingComponent(JComponent jc) {
            if (! (jc instanceof PolicyEditorPanel)) {
                return null; // nothing removed
            }

            final Goid policyGoid = ((PolicyEditorPanel) jc).getPolicyGoid();
            final long version = ((PolicyEditorPanel) jc).getVersionNumber();

            for (int i = 0; i < getTabCount(); i++) {
                final Component component = getComponentAt(i);

                if (component instanceof PolicyEditorPanel) {
                    final Goid tempPolicyGoid = ((PolicyEditorPanel) component).getPolicyGoid();
                    final long tempVersion = ((PolicyEditorPanel) component).getVersionNumber();

                    if (Goid.equals(policyGoid, tempPolicyGoid)) {
                        if (version == tempVersion) {
                            TabTitleComponentPanel tabTitleCompPanel = (TabTitleComponentPanel) getTabComponentAt(i);
                            removeTabAt(i);
                            return tabTitleCompPanel;
                        }
                    }
                }
            }

            return null; // nothing found and removed
        }

        /**
         * Removes all the tabs and their corresponding components
         * from the <code>tabbedpane</code>.
         *
         * @see #addTab(String, Component)
         * @see #removeTabAt
         */
        public void removeAll() {
            //setSelectedIndexImpl(-1);

            int tabCount = getTabCount();
            // We invoke removeTabAt for each tab, otherwise we may end up
            // removing CredentialsLocation added by the UI.
            while (tabCount-- > 0) {
                removeTabAt(tabCount);
            }
        }
    }

    /**
     * The mouse listener class handles  policy tab actions (Close Tab, Close Others, Close All, Close Unmodified, and
     * Reopen Closed Tab) triggered by mouse click on a policy tab.
     */
    public class MouseTabListener extends MouseAdapter {
        private JTabbedPane tabPane;

        MouseTabListener (JTabbedPane tabbedPane) {
            tabPane = tabbedPane;
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            mouseActionHandler(e);
        }

        private void mouseActionHandler(MouseEvent e) {
            final TabbedPaneUI tabbedPaneUI = tabPane.getUI();
            final int index = tabbedPaneUI.tabForCoordinate(tabPane, e.getX(), e.getY());

            if (index != -1) {
                // Handel Mouse Left Click
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (tabPane.getSelectedIndex () != index) {
                        tabPane.setSelectedIndex(index);
                    } else if (tabPane.isRequestFocusEnabled()) {
                        tabPane.requestFocusInWindow();
                    }

                    // If the mouse clicks on the area where the close-tab icon is nearby, then close the tab.
                    final Rectangle tabRectangle = tabbedPaneUI.getTabBounds(tabPane, index);
                    if (tabRectangle != null) {
                        final int mouseX = e.getX();
                        if (mouseX >= (tabRectangle.x + tabRectangle.width - closeTabIcon.getIconWidth() - CLOSE_TAB_ICON_RIGHT_GAP_WIDTH)) {
                            doCloseTab(index);

                            // Update the list of the last opened policies
                            saveLastOpenedPolicyTabs();
                        }
                    }
                }
                // Handel Mouse Right Click
                else if (SwingUtilities.isRightMouseButton(e)) {
                    final JPopupMenu popupMenu = new JPopupMenu ();

                    // Add "Close Tab" Action
                    final JMenuItem closeTab = new JMenuItem("Close Tab");
                    closeTab.addActionListener(new TabMenuActionListener() {
                        @Override
                        protected void performTabAction() {
                            doCloseTab(index);
                        }
                    });
                    popupMenu.add(closeTab);

                    // Add "Close Others" Action
                    final JMenuItem closeOthers = new JMenuItem("Close Others");
                    closeOthers.addActionListener(new TabMenuActionListener() {
                        @Override
                        protected void performTabAction() {
                            doCloseOthers(index);
                        }
                    });
                    popupMenu.add(closeOthers);
                    closeOthers.setEnabled(tabbedPane.getTabCount() > 1);

                    // Add "Close All" Action
                    final JMenuItem closeAll = new JMenuItem("Close All");
                    closeAll.addActionListener(new TabMenuActionListener() {
                        @Override
                        protected void performTabAction() {
                            doCloseAll();
                        }
                    });
                    popupMenu.add(closeAll);

                    // Add "Close Unmodified" Action
                    final JMenuItem closeUnmodified = new JMenuItem("Close Unmodified");
                    closeUnmodified.addActionListener(new TabMenuActionListener() {
                        @Override
                        protected void performTabAction() {
                            doCloseUnmodified();
                        }
                    });
                    popupMenu.add(closeUnmodified);
                    closeUnmodified.setEnabled(hasUnmodified());

                    // Add "Reopen Closed Tab" Action
                    final JMenuItem reopenClosedTab = new JMenuItem("Reopen Closed Tab");
                    reopenClosedTab.addActionListener(new TabMenuActionListener() {
                        @Override
                        protected void performTabAction() {
                            doReopenClosedTab();
                        }
                    });
                    popupMenu.add(reopenClosedTab);
                    reopenClosedTab.setEnabled(! closedTabs.isEmpty());

                    // Policy Diff Action
                    final JMenuItem diffPolicy = new JMenuItem("Compare Policy: " + (PolicyDiffContext.hasLeftDiffPolicy()? "Right" : "Left"));
                    diffPolicy.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            diffPolicy(index);
                        }
                    });
                    popupMenu.add(diffPolicy);
                    diffPolicy.setEnabled(tabPane.getComponentAt(index) instanceof PolicyEditorPanel);

                    // Show the popup menu now
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }

        private abstract class TabMenuActionListener implements ActionListener {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Perform a particular tab action, such as close tab, close all tabs, etc.
                performTabAction();

                // Update the list of the last opened policies after the tab(s) changed
                saveLastOpenedPolicyTabs();
            }

            protected abstract void performTabAction();
        }

        public void doCloseTab(int index) {
            tabPane.removeTabAt(index);
        }

        public void doCloseOthers(int index) {
            final Component currentComponent = tabPane.getComponentAt(index);

            for (Component component: tabPane.getComponents()) {
                if (component != currentComponent && (component instanceof HomePagePanel || component instanceof PolicyEditorPanel)) {
                    tabPane.remove(component);
                }
            }
        }

        public void doCloseAll() {
            tabPane.removeAll();
        }

        public void doCloseUnmodified() {
            for (Component component: tabPane.getComponents()) {
                if (component instanceof HomePagePanel ||
                    (component instanceof PolicyEditorPanel && !((PolicyEditorPanel) component).isUnsavedChanges())) {
                    tabPane.remove(component);
                }
            }
        }

        public void doReopenClosedTab() {
            if (! closedTabs.isEmpty()) {
                TabTitleComponentPanel lastClosedTabTitleComponentPanel = closedTabs.remove(closedTabs.size() - 1);
                JComponent component = lastClosedTabTitleComponentPanel.getComponent();
                try {
                    if (component instanceof HomePagePanel) {
                        new HomeAction().actionPerformed(null);
                    } else if (component instanceof PolicyEditorPanel) {
                        reopenPolicyEditorPanel.call((PolicyEditorPanel) component);
                    }  else {
                        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                            "The reopened policy tab type is not recognized.",
                            "Reopen Policy Error", JOptionPane.WARNING_MESSAGE, null);
                    }
                } catch (FindException e1) {
                    DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                        "Cannot retrieve the policy, '" + lastClosedTabTitleComponentPanel.getTabTitle() + "'.",
                        "Reopen Policy Error", JOptionPane.WARNING_MESSAGE, null);
                }
            }
        }

        public void diffPolicy(final int index) {
            if (PolicyDiffContext.hasLeftDiffPolicy()) {
                new PolicyDiffWindow(PolicyDiffContext.getLeftDiffPolicyInfo(), getPolicyInfo(index)).setVisible(true);
            } else {
                PolicyDiffContext.setLeftDiffPolicyInfo(getPolicyInfo(index));
            }
        }

        /**
         * Obtain policy information such as policy full name and policy xml
         *
         * @param index: the index of the tab which mouse clicks on.
         *
         * @return a pair of two strings: policy full name (policy name, resolution, version, and active status) and policy xml.
         */
        private Pair<String, PolicyTreeModel> getPolicyInfo(final int index) {
            final Component currentComponent = tabPane.getComponentAt(index);
            if (! (currentComponent instanceof PolicyEditorPanel)) {
                return null;
            }

            final PolicyEditorPanel pep = (PolicyEditorPanel) currentComponent;
            String policyFullName = pep.getDisplayName();
            if (pep.isUnsavedChanges()) policyFullName = "* " + policyFullName;

            // Get a fresh policy xml, since the policy might be changed and unsaved.
            final String policyXml = WspWriter.getPolicyXml(((PolicyEditorPanel) currentComponent).getCurrentRoot().asAssertion());

            PolicyTreeModel policyTreeModel;
            try {
                policyTreeModel = new PolicyTreeModel(WspReader.getDefault().parsePermissively(policyXml, WspReader.Visibility.includeDisabled));
            } catch (IOException e) {
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                        "Cannot parse the policy XML", "Policy Comparison Error", JOptionPane.WARNING_MESSAGE, null);
                return null;
            }

            return new Pair<>(policyFullName, policyTreeModel);
        }

        private boolean hasUnmodified() {
            for (Component component: tabPane.getComponents()) {
                if (component instanceof HomePagePanel ||
                    (component instanceof PolicyEditorPanel && !((PolicyEditorPanel) component).isUnsavedChanges())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * When user adds a service/policy into the policy editor panel, the service/policy maybe has a few other versions,
     * which have been added into the policy editor panel, so other tabs should keep their versions number unchanged and
     * update active status as inactive depending on the previous and new active status of the selected component.
     *
     * The method should be called in one other case, where a version of the opened service/policy is edited and saved,
     * then all other tabs associated with the service/policy should keep their versions number unchanged and update
     * active status as inactive depending on the previous and new active status of the selected component.
     *
     * When saving a policy version, if other policy versions are the same as the saved policy versions, then need to
     * remove these duplicates from the workspace.
     *
     * @param prevActiveStatus: previous active status of the selected component: true means "active" and false means "inactive"
     * @param newActiveStatus: new active status of the selected component: true means "active" and false means "inactive".
     */
    public void updateTabs(boolean prevActiveStatus, boolean newActiveStatus) {
        final JComponent selectedComponent = getComponent();
        if (! (selectedComponent instanceof PolicyEditorPanel)) return;

        final Goid selectedPolicyGoid = ((PolicyEditorPanel) selectedComponent).getPolicyGoid();
        final long selectedVersion = ((PolicyEditorPanel) selectedComponent).getVersionNumber();
        final List<Component> duplicates = new ArrayList<>();

        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            final Component component = tabbedPane.getComponentAt(i);
            if (! (component instanceof PolicyEditorPanel)) continue;

            final Goid policyGoid = ((PolicyEditorPanel) component).getPolicyGoid();
            final long version = ((PolicyEditorPanel) component).getVersionNumber();
            final boolean isActive = ((PolicyEditorPanel) component).isVersionActive();

            if (selectedComponent == component) {
                // If it is the selected component, update "version" and "active" in its TabTitleComponentPanel object
                // just in case its version has been changed.
                ((PolicyEditorPanel) component).setOverrideVersionActive(isActive);
                ((PolicyEditorPanel) component).setOverrideVersionNumber(version);
            } else if (Goid.equals(selectedPolicyGoid, policyGoid)) {
                if (selectedVersion != version) {
                    // Update the active status to be inactive, depending on the flags, prevActiveStatus and newActiveStatus
                    if ((prevActiveStatus != newActiveStatus) && newActiveStatus) {
                        ((PolicyEditorPanel) component).setOverrideVersionActive(false);
                    } else {
                        // Otherwise, keep other tabs' active status unchanged (since selectedComponent and component share the same latest PolicyEditorSubject object).
                        ((PolicyEditorPanel) component).setOverrideVersionActive(isActive);
                    }
                    // Keep other tabs' version unchanged
                    ((PolicyEditorPanel) component).setOverrideVersionNumber(version);
                } else {
                    duplicates.add(component);
                }
            }
            // Redraw tab title
            ((PolicyEditorPanel) component).updateHeadings();
        }

        // At last, remove those duplicates tabs
        for (Component component: duplicates) {
            tabbedPane.remove(component);
        }
    }

    /**
     * Update the tab title by using the given title, "newTitle" for a particular PolicyEditorPanel, "policyEditorPanel".
     *
     * @param policyEditorPanel: the policy editor panel whose tab title will be changed.
     * @param newTitle: a new title will replace the old tab title.
     */
    public void updateTabTitle(final PolicyEditorPanel policyEditorPanel, final String newTitle) {
        final int index = tabbedPane.indexOfComponent(policyEditorPanel);
        if (index == -1) return;

        // Update the tab title
        final TabTitleComponentPanel tabComponentsPanel = (TabTitleComponentPanel) tabbedPane.getTabComponentAt(index);
        final String decoratedNewTitle = policyEditorPanel.isUnsavedChanges()? "* " + newTitle: newTitle;
        tabComponentsPanel.setTabTitle(decoratedNewTitle);

        // Update the tab tooltip
        tabbedPane.setToolTipTextAt(index, newTitle);
    }

    /**
     * Close all tabs (each tab title has a different policy version) associated with a same policy node.
     * @param policyGoid: the policy node's policy GOID, which is used to match other policy versions
     */
    public void closeTabsRelatedToPolicyNode(Goid policyGoid) {
        java.util.List<Component> matchedComponents = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            final Component component = tabbedPane.getComponentAt(i);
            if (component instanceof PolicyEditorPanel) {
                if (Goid.equals(policyGoid, ((PolicyEditorPanel) component).getPolicyGoid())) {
                    matchedComponents.add(component);
                }
            }
        }

        for (Component component: matchedComponents) {
            tabbedPane.remove(component);
        }
    }

    /**
     * Delete policy tab settings identified by the policy goid from all policy tab properties.
     *
     * @param policyGoid: the policy goid is used to identify policy tab settings in a policy tab property.
     */
    public void deletePolicyTabSettingsByPolicyGoid(final Goid policyGoid) {
        Component component;
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            component = tabbedPane.getComponentAt(i);

            if (component instanceof PolicyEditorPanel && ((PolicyEditorPanel) component).getPolicyGoid().equals(policyGoid)) {
                ((PolicyEditorPanel) component).removePolicyTabSettingsFromPolicyTabProperties();
            }
        }
    }
}