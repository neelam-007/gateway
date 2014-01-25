package com.l7tech.console.panels;

import com.l7tech.console.MainWindow;
import com.l7tech.console.action.ActionVetoException;
import com.l7tech.console.action.EditPolicyAction;
import com.l7tech.console.action.HomeAction;
import com.l7tech.console.event.ContainerVetoException;
import com.l7tech.console.event.VetoableContainerListener;
import com.l7tech.console.event.WeakEventListenerList;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyToolBar;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.*;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.util.Functions;
import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EventListener;
import java.util.List;
import java.util.logging.Logger;

/**
 * <CODE>WorkSpacePanel</CODE> represents the main editing panel
 * for elements such as policies.
 */
public class WorkSpacePanel extends JPanel {
    static public final String NAME = "workspace.panel";
    static final Logger log = Logger.getLogger(WorkSpacePanel.class.getName());
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
        final int maxNumOfTabsAllowed = getMaxNumOfTabsPropertyFromPreferences();
        final int numOfAllTabs = tabbedPane.getTabCount();
        final List<JComponent> foundTabsAllowToBeClosed = findTabsWithoutUnsavedChanges();
        if (numOfAllTabs >= maxNumOfTabsAllowed) {
            if ((!openedTabs.isEmpty() && foundTabsAllowToBeClosed.isEmpty()) ||           // Case: all tabs are changes-unsaved tabs.
                (numOfAllTabs - maxNumOfTabsAllowed) >= foundTabsAllowToBeClosed.size()) { // Case: there are no enough number of without-unsaved-changes tabs to be automatically closed.
                // At these cases, we don't automatically close some without-unsaved-changes tabs and display a warning
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                    "You have reached the maximum number of tabs allowed (" + maxNumOfTabsAllowed + ").\n" +
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
                for (int i = 0; i <= numOfAllTabs - maxNumOfTabsAllowed; i++) {
                    JComponent component = foundTabsAllowToBeClosed.get(i);
                    tabbedPane.remove(component);
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

        final int index = tabbedPane.indexOfComponent(jc);
        final TabTitleComponentPanel newTabCompPanel = new TabTitleComponentPanel(jc);
        tabbedPane.setTabComponentAt(index, newTabCompPanel); // Add a table title render object
        tabbedPane.setToolTipTextAt(index, jc.getName());
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (tabbedPane.indexOfComponent(jc) < 0) {
                    return;
                }

                tabbedPane.setSelectedComponent(jc);

                // Maybe the opened service/policy has a few other versions that have been added into the policy editor panel,
                // so other tabs should keep their versions number unchanged and update active status as inactive.
                if (jc instanceof PolicyEditorPanel) {
                    try {
                        final boolean currentActiveStatus = ((PolicyEditorPanel) jc).getPolicyNode().getPolicy().isVersionActive();
                        updateTabsVersionNumAndActiveStatus(false, currentActiveStatus);
                    } catch (FindException e) {
                        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                            "Cannot find the policy for the policy editor panel, '" + ((PolicyEditorPanel) jc).getDisplayName() + "'.",
                            "Open Policy Tab Error", JOptionPane.ERROR_MESSAGE, null);
                    }
                }
            }
        });

        // If foundAndRemoved object is not null, it means that the associated PolicyEditorPanel has been added back to
        // the workspace (see the above lines), so closedTabs should remove the foundAndRemoved object because of it added back (i.e., reopened).
        if (foundAndRemoved != null) {
            closedTabs.remove(foundAndRemoved);
        }

        // Add newTabCompPanel into the least-recently-used list
        openedTabs.add(newTabCompPanel);

        jc.addPropertyChangeListener(new PropertyChangeListener() {
            /**
             * This method gets called when a bound property is changed.
             *
             * @param evt A PropertyChangeEvent object describing the event source
             *            and the property that has changed.
             */
            public void propertyChange(PropertyChangeEvent evt) {
                if ("name".equals(evt.getPropertyName())) {
                    tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), (String) evt.getNewValue());
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
        private JLabel tabTitleLabel;   // tab display name
        private JButton tabCloseButton; // button to close the tab
        private JComponent component;   // a PolicyEditorPanel or HomePanel object
        private long version;           // to preserve policy/service version number for this tab
        private boolean active;         // to preserve policy active status

        TabTitleComponentPanel(final JComponent component) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 2));
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

        long getVersion() {
            return version;
        }

        void setVersion(long version) {
            this.version = version;
        }

        public boolean isActive() {
            return active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        private void initializeComponents() {
            // Add a tab label to display tab title
            tabTitleLabel = new JLabel(component.getName());
            final Font font = tabTitleLabel.getFont();
            tabTitleLabel.setFont(new Font(font.getName(), Font.BOLD, 12));
            tabTitleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
            add(tabTitleLabel);

            // Add a close tab button
            final Icon closeIcon = new ImageIcon(ImageCache.getInstance().getIcon(MainWindow.RESOURCE_PATH + "/tabClose16.png"));
            tabCloseButton = new JButton(closeIcon);
            tabCloseButton.setToolTipText("Close this tab");
            tabCloseButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tabbedPane.remove(component);
                }
            });
            tabCloseButton.setBorder(BorderFactory.createEmptyBorder());
            add(tabCloseButton);

            // This method should be called after tabTitleLabel and tabCloseButton are initialized.
            validateTabTitleLength(component.getName());

            if (component instanceof PolicyEditorPanel) {
                version = ((PolicyEditorPanel) component).getVersionNumber();
                active = ((PolicyEditorPanel) component).isVersionActive();
            }
        }

        /**
         * Check if a title is too long.  If so, truncate the middle part and replace the truncated part with "..."
         * to fit the width of the policy editor panel as possible.
         *
         * Then use the truncated title to set the policy tab title.
         */
        private void validateTabTitleLength(String title) {
            final int workspaceWidth = ((MainWindow) TopComponents.getInstance().getComponent("mainWindow")).getMainSplitPaneRight().getSize().width;
            int labelAndButtonWidth = tabTitleLabel.getPreferredSize().width + tabCloseButton.getPreferredSize().width;
            int versionIdx;
            int middleIdx;
            String policyName;
            String versionAndRest;

            while (labelAndButtonWidth > workspaceWidth && workspaceWidth > 0 && title.length() > 4) {
                versionIdx = title.lastIndexOf(" (v");
                policyName = versionIdx == -1? title : title.substring(0, versionIdx);
                versionAndRest = versionIdx == -1? "": title.substring(versionIdx);

                if (policyName.length() > 13)  {
                    // Every time truncate 10 characters in the middle of the policy name.  Note: version and active status remain the same.
                    middleIdx = policyName.length() / 2;
                    title = policyName.substring(0, middleIdx - 5) + "..." + policyName.substring(middleIdx + 5) + versionAndRest;
                } else if (title.length() > 13) {
                    if (title.contains("...")) {
                        int dotsIdx = title.indexOf("...");
                        title = title.substring(0, dotsIdx + 3) + title.substring(dotsIdx + 10);
                    } else {
                        middleIdx = title.length() / 2;
                        title = title.substring(0, middleIdx - 5) + "..." + title.substring(middleIdx + 5);
                    }
                }

                tabTitleLabel.setText(title);
                labelAndButtonWidth = tabTitleLabel.getPreferredSize().width + tabCloseButton.getPreferredSize().width;
            }
        }
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
        JComponent selectedComponent = getComponent();

        for (int i = tabbedPane.getTabCount() - 1; i >= 0; i--) {
            final Component component = tabbedPane.getComponentAt(i);
            final TabTitleComponentPanel tabComponent = (TabTitleComponentPanel) tabbedPane.getTabComponentAt(i);

            if (component instanceof PolicyEditorPanel) {
                // Remove the out-dated tab first
                tabbedPane.removeTabAt(i);
                // Add the tab back
                try {
                    reopenPolicyEditorPanel.call((PolicyEditorPanel) component, tabComponent.getVersion());
                } catch (FindException e) {
                    // Report error, but still continue other tabs refresh
                    DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                        "Cannot retrieve the policy, '" + tabComponent.getTabTitle() + "'.",
                        "Refresh Error", JOptionPane.WARNING_MESSAGE, null);
                    continue;
                }

                // Get a refreshed policy tree and add it into the list for return
                PolicyTree policyTree = (PolicyTree) TopComponents.getInstance().getComponent(PolicyTree.NAME);
                alreadyRefreshed.add(policyTree);

                if (component == selectedComponent) {
                    selectedComponent = getComponent();
                }
            }
        }

        // Set the previous selected panel to be selected
        if (selectedComponent instanceof HomePagePanel) {
            tabbedPane.setSelectedIndex(0);
        } else {
            tabbedPane.setSelectedComponent(selectedComponent);
        }

        return alreadyRefreshed;
    }

    private Functions.BinaryVoidThrows<PolicyEditorPanel, Long, FindException> reopenPolicyEditorPanel = new Functions.BinaryVoidThrows<PolicyEditorPanel, Long, FindException>() {
        /**
         * Reopen a PolicyEditorPanel for a particular policy version
         *
         * @param pep: the policy editor panel to be refreshed
         * @param versionOrdinal: the policy version ordinal
         *
         * @throws FindException: thrown if the policy version or the refreshed policy cannot be retrieved.
         */
        @Override
        public void call(final PolicyEditorPanel pep, final Long versionOrdinal) throws FindException {
            // Get the full policy version, which contains policy xml for the version specified by "versionOrdinal".
            final Goid policyGoid = pep.getPolicyGoid();
            final PolicyVersion fullPolicyVersion = Registry.getDefault().getPolicyAdmin().findPolicyVersionForPolicy(policyGoid, versionOrdinal);

            if (fullPolicyVersion == null) {
                throw new FindException("Cannot find the policy '" + pep.getDisplayName() + "'");
            }

            // Get the refresh entity node (ServiceNode or PolicyNode) in the ServicesAndPoliciesTree, associated with the policy editor panel.
            final String entityNodeGoidString = ((OrganizationHeader) pep.getPolicyNode().getUserObject()).getStrId();
            final RootNode rootNode = ((ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME)).getRootNode();
            final EntityWithPolicyNode refreshedEntityNode = (EntityWithPolicyNode) rootNode.getNodeForEntity(Goid.parseGoid(entityNodeGoidString));

            // Reset the policy xml, i.e. update the policy
            refreshedEntityNode.getPolicy().setXml(fullPolicyVersion.getXml());

            // Invoke EditPolicyAction to update the policy editor panel.
            new EditPolicyAction(refreshedEntityNode, true, fullPolicyVersion).invoke();
        }
    };

    public void setReopenPolicyEditorPanel(final Functions.BinaryVoidThrows<PolicyEditorPanel, Long, FindException> reopenPolicyEditorPanel) {
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

            final Goid policyNodeGoid = ((PolicyEditorPanel) jc).getPolicyGoid();
            final long version = ((PolicyEditorPanel) jc).getVersionNumber();

            for (int i = 0; i < getTabCount(); i++) {
                final Component component = getComponentAt(i);

                if (component instanceof PolicyEditorPanel) {
                    final Goid tempPolicyGoid = ((PolicyEditorPanel) component).getPolicyGoid();
                    final long tempVersion = ((TabTitleComponentPanel)getTabComponentAt(i)).getVersion();

                    if (Goid.equals(policyNodeGoid, tempPolicyGoid)) {
                        if (version == tempVersion) {
                            TabTitleComponentPanel tabTitleCompPanel = (TabTitleComponentPanel) getTabComponentAt(i);
                            closedTabs.remove(tabTitleCompPanel);
                            openedTabs.remove(tabTitleCompPanel);

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
    public class MouseTabListener extends PopUpMouseListener {
        private JTabbedPane tabPane;

        MouseTabListener (JTabbedPane tabbedPane) {
            tabPane = tabbedPane;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            popUpMenuHandler(e);
        }

        @Override
        public void popUpMenuHandler(MouseEvent e) {
            final int index = tabPane.getUI().tabForCoordinate(tabPane, e.getX(), e.getY());
            if (index != -1) {
                // Handel Mouse Left Click
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (tabPane.getSelectedIndex () != index) {
                        tabPane.setSelectedIndex(index);
                    } else if (tabPane.isRequestFocusEnabled()) {
                        tabPane.requestFocusInWindow();
                    }
                }
                // Handel Mouse Right Click
                else if (SwingUtilities.isRightMouseButton(e)) {
                    final JPopupMenu popupMenu = new JPopupMenu ();

                    // Add "Close Tab" Action
                    final JMenuItem closeTab = new JMenuItem("Close Tab");
                    closeTab.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            doCloseTab(index);
                        }
                    });
                    popupMenu.add(closeTab);

                    // Add "Close Others" Action
                    final JMenuItem closeOthers = new JMenuItem("Close Others");
                    closeOthers.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            doCloseOthers(index);
                        }
                    });
                    popupMenu.add(closeOthers);
                    closeOthers.setEnabled(tabbedPane.getTabCount() > 1);

                    // Add "Close All" Action
                    final JMenuItem closeAll = new JMenuItem("Close All");
                    closeAll.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            doCloseAll();
                        }
                    });
                    popupMenu.add(closeAll);

                    // Add "Close Unmodified" Action
                    final JMenuItem closeUnmodified = new JMenuItem("Close Unmodified");
                    closeUnmodified.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            doCloseUnmodified();
                        }
                    });
                    popupMenu.add(closeUnmodified);
                    closeUnmodified.setEnabled(hasUnmodified());

                    // Add "Reopen Closed Tab" Action
                    final JMenuItem reopenClosedTab = new JMenuItem("Reopen Closed Tab");
                    reopenClosedTab.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            doReopenClosedTab();
                        }
                    });
                    popupMenu.add(reopenClosedTab);
                    reopenClosedTab.setEnabled(! closedTabs.isEmpty());

                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
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
                        reopenPolicyEditorPanel.call((PolicyEditorPanel) component, lastClosedTabTitleComponentPanel.getVersion());
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

        private boolean hasUnmodified() {
            for (Component component: tabPane.getComponents()) {
                if (component instanceof HomePagePanel ||
                    (component instanceof PolicyEditorPanel && !((PolicyEditorPanel) component).isUnsavedChanges())) {
                    return true;
                }
            }
            return false;
        }
    };

    /**
     * When user adds a service/policy into the policy editor panel, the service/policy maybe has a few other versions,
     * which have been added into the policy editor panel, so other tabs should keep their versions number unchanged and
     * update active status as inactive depending on the previous and new active status of the selected component.
     *
     * The method should be called in one other case, where a version of the opened service/policy is edited and saved,
     * then all other tabs associated with the service/policy should keep their versions number unchanged and update
     * active status as inactive depending on the previous and new active status of the selected component.
     *
     * @param prevActiveStatus: previous active status of the selected component: true means "active" and false means "inactive"
     * @param newActiveStatus: new active status of the selected component: true means "active" and false means "inactive".
     */
    public void updateTabsVersionNumAndActiveStatus(boolean prevActiveStatus, boolean newActiveStatus) {
        final JComponent selectedComponent = getComponent();

        if (selectedComponent instanceof PolicyEditorPanel) {
            final EntityWithPolicyNode policyNode = ((PolicyEditorPanel) selectedComponent).getPolicyNode();
            final long version = ((PolicyEditorPanel) selectedComponent).getVersionNumber();
            final boolean active = ((PolicyEditorPanel) selectedComponent).isVersionActive();

            for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                final Component component = tabbedPane.getComponentAt(i);
                if (component instanceof PolicyEditorPanel) {
                    final EntityWithPolicyNode tempEntityNode = ((PolicyEditorPanel) component).getPolicyNode();
                    final long tempVersion = ((TabTitleComponentPanel)tabbedPane.getTabComponentAt(i)).getVersion();
                    final boolean tempActive = ((TabTitleComponentPanel)tabbedPane.getTabComponentAt(i)).isActive();

                    if (selectedComponent == component) {
                        // If it is the selected component, update "version" and "active" in its TabTitleComponentPanel object
                        // just in case its version has been changed.
                        ((TabTitleComponentPanel)tabbedPane.getTabComponentAt(i)).setVersion(version);
                        ((TabTitleComponentPanel)tabbedPane.getTabComponentAt(i)).setActive(active);
                    } else if  (policyNode == tempEntityNode && version != tempVersion) {
                        // Update the active status to be inactive, depending on the flags, prevActiveStatus and newActiveStatus
                        if ((prevActiveStatus != newActiveStatus) && newActiveStatus) {
                            ((PolicyEditorPanel) component).setOverrideVersionActive(false);
                            ((TabTitleComponentPanel)tabbedPane.getTabComponentAt(i)).setActive(false);
                        } else {
                            // Otherwise, keep other tabs' active status unchanged (since selectedComponent and component share the same latest PolicyEditorSubject object).
                            ((PolicyEditorPanel) component).setOverrideVersionActive(tempActive);
                        }
                        // Keep other tabs' version unchanged
                        ((PolicyEditorPanel) component).setOverrideVersionNumber(tempVersion);

                        // Redraw tab title
                        ((PolicyEditorPanel) component).updateHeadings();
                    }
                }
            }
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
     * @param policyNode: the policy node to find all related tabs.
     */
    public void closeTabsRelatedToPolicyNode(EntityWithPolicyNode policyNode) {
        java.util.List<Component> matchedComponents = new ArrayList<>();
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            final Component component = tabbedPane.getComponentAt(i);
            if (component instanceof PolicyEditorPanel) {
                EntityWithPolicyNode node = ((PolicyEditorPanel) component).getPolicyNode();
                if (policyNode == node) {
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
                ((PolicyEditorPanel) component).deletePolicyTabSettingsFromAllPolicyTabProperties();
            }
        }
    }
}