/*
 * Copyright (C) 2003-2008 Layer 7 Technologies Inc.
 */
package com.l7tech.console.poleditor;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.MainWindow;
import com.l7tech.console.action.*;
import com.l7tech.console.event.ContainerVetoException;
import com.l7tech.console.event.VetoableContainerListener;
import com.l7tech.console.panels.CancelableOperationDialog;
import com.l7tech.console.panels.ImportPolicyFromUDDIWizard;
import com.l7tech.console.panels.InformationDialog;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.HtmlUtil;
import com.l7tech.gui.util.ImageCache;
import com.l7tech.gui.util.Utilities;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.SyspropUtil;
import com.l7tech.wsdl.Wsdl;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.plaf.SplitPaneUI;
import javax.swing.plaf.basic.BasicSplitPaneUI;
import javax.swing.text.EditorKit;
import javax.swing.text.JTextComponent;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PolicyEditorPanel extends JPanel implements VetoableContainerListener {
    static Logger log = Logger.getLogger(PolicyEditorPanel.class.getName());

    public static final String POLICYNAME_PROPERTY = "policy.name";
    public static final String SHOW_COMMENTS = "COMMENTS.SHOWSTATE";

    private static final String PREF_PREFIX = "policy.editor.";
    private static final String SHOW_ASSERTION_NUMBERS = PREF_PREFIX + "showAssertionNumbers";
    private static final String MESSAGE_AREA_DIVIDER_KEY =  PREF_PREFIX + JSplitPane.DIVIDER_LOCATION_PROPERTY;
    private static final long TIME_BEFORE_OFFERING_CANCEL_DIALOG = 500L;

    private static final String PROP_PREFIX = "com.l7tech.console";
    private static final long DELAY_INITIAL = SyspropUtil.getLong(PROP_PREFIX + ".policyValidator.serverSideDelay.initial", 71L);
    private static final long DELAY_CAP = SyspropUtil.getLong(PROP_PREFIX + ".policyValidator.serverSideDelay.maximum", 15000L);
    private static final double DELAY_MULTIPLIER = SyspropUtil.getDouble(PROP_PREFIX + ".policyValidator.serverSideDelay.multiplier", 1.6);

    private JTextPane messagesTextPane;
    private AssertionTreeNode rootAssertion;
    private PolicyTree policyTree;
    private PolicyEditToolBar policyEditorToolbar;
    private JSplitPane splitPane;
    private final TopComponents topComponents = TopComponents.getInstance();
    private NumberedPolicyTreePane policyTreePane;
    private boolean initialValidate = false;
    private boolean messageAreaVisible = false;
    private JTabbedPane messagesTab;
    private boolean validating = false;
    private SecureAction saveAndActivateAction;
    private SecureAction saveOnlyAction;
    private SecureAction showAssertionLineNumbersAction;
    private List<AbstractButton> showLnNumsButtons = new ArrayList<AbstractButton>();
    private ValidatePolicyAction validateAction;
    private ValidatePolicyAction serverValidateAction;
    private ExportPolicyToFileAction exportPolicyAction;
    private ExportPolicyToFileAction simpleExportPolicyAction;
    private ImportPolicyFromFileAction importPolicyAction;
    private PolicyEditorSubject subject;
    private String subjectName;
    private final SsmPreferences preferences = TopComponents.getInstance().getPreferences();
    private final boolean enableUddi;
    private final PolicyValidator policyValidator;
    private Long overrideVersionNumber = null;
    private Boolean overrideVersionActive = null;
    private SearchForm searchForm;
    private SecureAction hideShowCommentsAction;

    public interface PolicyEditorSubject {
        /**
         *
         * @return the policy node if the policy is attached to an entity, null otherwise
         */
        EntityWithPolicyNode getPolicyNode();

        /**
         * get the root assertion of the policy to edit
         * @return the root of the policy
         */
        Assertion getRootAssertion();

        /**
         * @return the name of either the published service or the policy. never null
         */
        String getName();

        /** @return the last checkpointed version number for the policy XML. */
        long getVersionNumber();

        /** @return true if the current version number is the active version of the policy XML. */
        boolean isActive();

        void addPropertyChangeListener(PropertyChangeListener policyPropertyChangeListener);

        void removePropertyChangeListener(PropertyChangeListener policyPropertyChangeListener);

        boolean hasWriteAccess();
    }

    /**
     * Post Bondo (5.2) enableUddi should only be false when were in untrusted Applet mode
     * @param enableUddi if true, then the 'Import from UDDI' button in the policy window will be shown
     */
    public PolicyEditorPanel(PolicyEditorSubject subject, PolicyTree pt, boolean validateOnOpen, boolean enableUddi) {
        if (subject == null || pt == null) {
            throw new IllegalArgumentException();
        }
        this.enableUddi = enableUddi;
        this.subject = subject;
        this.subjectName = subject.getName();
        this.policyTree = pt;
        this.policyTree.setWriteAccess(subject.hasWriteAccess());
        this.policyValidator = Registry.getDefault().getPolicyValidator();
        layoutComponents();

        renderPolicy();
        setEditorListeners();
        if (validateOnOpen) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    try {
                        initialValidate = true;
                        validatePolicy();
                    } finally {
                        initialValidate = false;
                    }
                }
            });
        }
    }

    /**
     * Sets the name of the component to the specified string. The
     * method is overriden to support/fire property events
     * 
     * @param name the string that is to be this
     *             component's name
     * @see #getName()
     */
    @Override
    public void setName(String name) {
        String oldName = getName();
        super.setName(name);
        this.firePropertyChange("name", oldName, name);
    }

    public PolicyTree getPolicyTree() {
        return policyTree;
    }

    /**
     * Get the Policy node that this panel is editing
     * <p/>
     * Note that it does not return the copy, therefore any
     * changes made through this method are not visible to
     * the policy editor.
     *
     * @return the noe that the panel is editing
     */
    public EntityWithPolicyNode getPolicyNode() {
        return subject.getPolicyNode();
    }

    protected boolean isPolicyValidationOn() {
        String maybe = preferences.asProperties().getProperty(SsmPreferences.ENABLE_POLICY_VALIDATION_ID);
        boolean output = Boolean.valueOf(maybe);
        if (maybe == null || maybe.length() < 1) {
            output = true;
        }
        return output;
    }

    /**
     * Check if this policy editor panel has unsaved changes.
     *
     * @return true iff. there are unsaved changes in this panel.
     */
    public boolean isUnsavedChanges() {
        return policyEditorToolbar.buttonSaveOnly.isEnabled();
    }

    protected PublishedService getPublishedService() {
        PublishedService service = null;
        EntityWithPolicyNode pn = subject.getPolicyNode();
        if (pn instanceof ServiceNode) {
            try {
                service = ((ServiceNode)pn).getEntity();
            } catch (FindException e) {
                log.log(Level.SEVERE, "cannot get service", e);
            }
        }
        return service;
    }

    /**
     * Check if this policy editor panel is editing a published service policy or a non-published service policy.
     *
     * @return true if this panel currently has a PublishedService node's policy open for editing.
     *         false if the policy being edited does not belong to a PublishedService (is an includable policy, say)
     */
    public boolean isEditingPublishedService() {
        return getPublishedService() != null;
    }

    /**
     * Get the OID of the PublishedService whose policy is open for editing, if this panel is editing the
     * policy of a PublishedService.
     *
     * @return the OID of the PublishedService whose policy is open for editing, or null if it's not a PublishedService policy.
     */
    public Long getPublishedServiceOid() {
        final PublishedService ps = getPublishedService();
        return ps == null ? null : ps.getOid();
    }

    /**
     * Get the OID of the Policy that is open for editing in this editor panel.
     *
     * @return the policy OID.  Never null.
     */
    public long getPolicyOid() {
        try {
            return getPolicyNode().getPolicy().getOid();
        } catch (FindException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * validate the policy and display the result.
     */
    public void validatePolicy() {
        validatePolicy(true);
    }

    protected static final Callable<PolicyValidatorResult> NO_VAL_CALLBACK = new Callable<PolicyValidatorResult>() {
        @Override
        public PolicyValidatorResult call() throws Exception {
            PolicyValidatorResult output = new PolicyValidatorResult();
            output.addWarning(new PolicyValidatorResult.Warning( Collections.<Integer>emptyList(), -1, 0,
                    "Policy validation feedback has been disabled in the Preferences", null));
            return output;
        }
    };

    /**
     * validate the policy.
     * @param displayResult if true, will call displayResult before returning.  otherwise, just returns the results
     * @return result, or null if it was canceled
     */
    private PolicyValidatorResult validatePolicy(boolean displayResult) {
        final Assertion assertion = getCurrentRoot().asAssertion();
        final boolean soap;
        final Wsdl wsdl;
        try {
            final PublishedService service = getPublishedService();
            if (service == null) {
                wsdl = null;
                soap = getPolicyNode().getPolicy().isSoap();
            } else {
                wsdl = service.parsedWsdl();
                soap = service.isSoap();
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to find policy", e);
        }

        Callable<PolicyValidatorResult> callable;
        if (isPolicyValidationOn()) {
            final ConsoleLicenseManager licenseManager = Registry.getDefault().getLicenseManager();
            callable = new Callable<PolicyValidatorResult>() {
                @Override
                public PolicyValidatorResult call() throws Exception {
                	final Policy policy = getPolicyNode().getPolicy();
	                if (policy == null) {
    	                PolicyValidatorResult r = new PolicyValidatorResult();
                    	r.addError(new PolicyValidatorResult.Error(Collections.<Integer>emptyList(), -1, -1, "Policy could not be loaded", null));
                    	return r;
                	}
                    PolicyValidatorResult r = policyValidator.validate(assertion, new PolicyValidationContext(policy.getType(), policy.getInternalTag(), wsdl, soap), licenseManager);
                    policyValidator.checkForCircularIncludes(policy.getGuid(), policy.getName(), assertion, r);
                    return r;
                }
            };
        } else {
            callable = NO_VAL_CALLBACK;
        }
        return validateAndDisplay(callable, displayResult);
    }

    private PolicyValidatorResult validateAndDisplay(Callable<PolicyValidatorResult> callable, boolean displayResult) {
        final JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        final CancelableOperationDialog cancelDlg =
                new CancelableOperationDialog(topComponents.getTopParent(), "Validating", "        Validating policy...        ", bar);

        final PolicyValidatorResult result;
        try {
            result = Utilities.doWithDelayedCancelDialog(callable, cancelDlg, TIME_BEFORE_OFFERING_CANCEL_DIALOG);
            if (result != null && displayResult)
                displayPolicyValidateResult(pruneDuplicates(result));
            return result;
        } catch (InterruptedException e) {
            return null;
        } catch (InvocationTargetException e) {
            throw ExceptionUtils.wrap(e.getTargetException());
        }
    }


    public void setMessageAreaVisible(boolean b) {
        final JComponent messagePane = getMessagePane();
        messageAreaVisible = b;
        if (b) {
            splitPane.add(messagePane, "bottom");
            splitPane.setDividerSize(10);
        } else {
            splitPane.remove(messagePane);
            splitPane.setDividerSize(0);

        }
        SplitPaneUI sui = splitPane.getUI();
        if (sui instanceof BasicSplitPaneUI) {
            BasicSplitPaneUI bsui = (BasicSplitPaneUI)sui;
            bsui.getDivider().setVisible(b);
        }
    }

    public boolean isMessageAreaVisible() {
        return messageAreaVisible;
    }

    private void layoutComponents() {
        setBorder(null);
        setLayout(new BorderLayout());

        // This component init has side-effects that need to occur before
        // the toolbar is created (configures the pep with the policy tree)
        //
        // If you don't do this the buttons may not be correctly enabled.
        JSplitPane pane = getSplitPane();

        add(getToolBar(), BorderLayout.NORTH);
        add(pane, BorderLayout.CENTER);
        setMessageAreaVisible(preferences.isPolicyMessageAreaVisible());
    }

    private JPanel getFindPanel(){
        if(searchForm != null) return searchForm.getSearchPanel();

        searchForm = new SearchForm();

        final MouseListener listener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                searchForm.hidePanel();
            }
        };

        searchForm.addCloseLabelListener(listener);
        searchForm.hidePanel();
        return searchForm.getSearchPanel();
    }

    private JSplitPane getSplitPane() {
        if (splitPane != null) return splitPane;
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(null);

        JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new BorderLayout());
        containerPanel.add(getFindPanel(), BorderLayout.NORTH);
        containerPanel.add(getPolicyTreePane(), BorderLayout.CENTER);

        final Action findAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (searchForm != null) searchForm.showPanel(policyTree);
            }
        };

        containerPanel.getActionMap().put(MainWindow.L7_FIND, findAction);
        
        final Action goToAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                DialogDisplayer.showInputDialog(policyTree,
                        "Enter Assertion Number: ",
                        "Go to Assertion",
                        JOptionPane.INFORMATION_MESSAGE,
                        null, null, null,
                        new DialogDisplayer.InputListener() {
                    @Override
                    public void reportResult(Object option) {
                        if (option == null)
                            return;

                        try {
                            policyTree.goToAssertionTreeNode(option.toString());
                        } catch (NumberFormatException e1) {
                            //show a warning dialog
                            DialogDisplayer.showMessageDialog(policyTree,
                                    "Invalid Assertion Number (Must be an integer >= "
                                            + Assertion.MIN_DISPLAYED_ORDINAL + ")",
                                    "Invalid Assertion Number", JOptionPane.WARNING_MESSAGE, null);
                        }
                    }
                });
            }
        };

        containerPanel.getActionMap().put(MainWindow.L7_GO_TO, goToAction);

        final Action f3Action = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(!searchForm.isSearchVisible()){
                    searchForm.showPanel(policyTree);
                    return;
                }

                //check that the some search text has been entered
                if(!searchForm.hasSearchResults()) {
                    final InformationDialog iDialog = new InformationDialog("No search results.");
                    MainWindow.showInformationDialog(iDialog, null);
                    return;
                }

                final String ordinal;
                final boolean isF3;
                if(e.getActionCommand().equals(MainWindow.L7_F3)){
                    ordinal = searchForm.getNextAssertionOrdinal();
                    isF3 = true;
                } else if(e.getActionCommand().equals(MainWindow.L7_SHIFT_F3)){
                    ordinal = searchForm.getPreviousAssertionOrdinal();
                    isF3 = false;
                }else {
                    ordinal = null;
                    isF3 = false;
                }

                if(ordinal == null){
                    if(searchForm.hasSearchResults()){
                        final InformationDialog iDialog;
                        if(isF3){
                            iDialog = new InformationDialog("No more results. Press F3 to search from the top.", KeyEvent.VK_F3);
                        }else{
                            iDialog = new InformationDialog("No more results. Press Shift + F3 to search from the bottom.",
                                    KeyStroke.getKeyStroke(KeyEvent.VK_F3,  KeyEvent.VK_SHIFT).getKeyCode(), true);
                        }

                        MainWindow.showInformationDialog(iDialog, new Runnable() {
                            @Override
                            public void run() {
                                //this runs on the swing thread
                                if (iDialog.isSpecialKeyDisposed()) {
                                    final String goToOrdinal;
                                    if (isF3) {
                                        goToOrdinal = searchForm.getNextAssertionOrdinal();
                                    } else {
                                        goToOrdinal = searchForm.getPreviousAssertionOrdinal();
                                    }

                                    if (goToOrdinal != null) {
                                        policyTree.goToAssertionTreeNode(goToOrdinal);
                                    }
                                }
                            }
                        });
                    }else{
                        final InformationDialog iDialog = new InformationDialog("No search results.");
                        MainWindow.showInformationDialog(iDialog, null);
                    }
                }else {
                    policyTree.goToAssertionTreeNode(ordinal);
                }
            }
        };

        containerPanel.getActionMap().put(MainWindow.L7_F3, f3Action);
        containerPanel.getActionMap().put(MainWindow.L7_SHIFT_F3, f3Action);

        if(topComponents.isApplet()){
            //The applet does not have any JMenus so accelerators will not work out of the box. (Copy, paste are
            //special as they work due to the JComponents transferHandler). 
            final KeyAdapter adapter = new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyStroke.getKeyStroke(KeyEvent.VK_F, KeyEvent.CTRL_MASK).getKeyCode()) {
                        findAction.actionPerformed(new ActionEvent(this,
                                ActionEvent.ACTION_PERFORMED,
                                MainWindow.L7_FIND));
                    } else if (e.getKeyCode() == KeyEvent.VK_F3 && e.getModifiers() == KeyEvent.SHIFT_MASK) {
                        f3Action.actionPerformed(new ActionEvent(this,
                                ActionEvent.ACTION_PERFORMED,
                                MainWindow.L7_SHIFT_F3));
                    } else if (e.getKeyCode() == KeyEvent.VK_F3) {
                        f3Action.actionPerformed(new ActionEvent(this,
                                ActionEvent.ACTION_PERFORMED,
                                MainWindow.L7_F3));
                    } else if (e.getKeyCode() == KeyStroke.getKeyStroke(KeyEvent.VK_G, KeyEvent.CTRL_MASK).getKeyCode()) {
                        goToAction.actionPerformed(new ActionEvent(this,
                                ActionEvent.ACTION_PERFORMED,
                                MainWindow.L7_GO_TO));
                    }

                }
            };
            policyTree.addKeyListener(adapter);
            searchForm.addKeyListeners(adapter);
            
        }

        policyTree.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
                    if (searchForm != null) searchForm.hidePanel();
                }
            }
        });

        splitPane.add(containerPanel, "top");

        final JComponent messagePane = getMessagePane();

        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
          new PropertyChangeListener() {
              @Override
              public void propertyChange(PropertyChangeEvent evt) {
                  SsmPreferences prefs = preferences;
                  int l = splitPane.getDividerLocation();
                  prefs.putProperty(MESSAGE_AREA_DIVIDER_KEY, Integer.toString(l));
              }
          });
        
        messagePane.addHierarchyListener(new HierarchyListener() {
            @Override
            public void hierarchyChanged(HierarchyEvent e) {
                long flags = e.getChangeFlags();
                if ((flags & HierarchyEvent.SHOWING_CHANGED) == HierarchyEvent.SHOWING_CHANGED) {
                    if (!messagePane.isShowing()) return;
                }
                try {
                    SsmPreferences prefs = preferences;
                    String s = prefs.getString(MESSAGE_AREA_DIVIDER_KEY);
                    if (s != null) {
                        int l = Integer.parseInt(s);
                        splitPane.setDividerLocation(l);
                    }
                } catch (NumberFormatException e1) {
                    log.log(Level.SEVERE, "should not happen", e);
                }
            }
        });
        
        return splitPane;
    }

    private JComponent getPolicyTreePane() {
        if (policyTreePane != null) return policyTreePane;
        policyTree.setPolicyEditor(this);
        policyTree.setRootVisible(false);
        policyTree.setShowsRootHandles(true);
        policyTree.setAlignmentX(Component.LEFT_ALIGNMENT);
        policyTree.setAlignmentY(Component.TOP_ALIGNMENT);
        policyTreePane = new NumberedPolicyTreePane(policyTree);
        policyTreePane.setNumbersVisible( Boolean.valueOf(preferences.getString( SHOW_ASSERTION_NUMBERS, "false" )) );
        return policyTreePane;
    }

    public void changeSubjectName(String newName) {
        subjectName = newName;
    }

    public long getVersionNumber() {
        if (overrideVersionNumber != null) return overrideVersionNumber;
        return subject.getVersionNumber();
    }

    public boolean isVersionActive() {
        if (overrideVersionActive != null) return overrideVersionActive;
        return subject.isActive();
    }

    public void setOverrideVersionActive(boolean overrideVersionActive) {
        this.overrideVersionActive = overrideVersionActive;
    }

    public void setOverrideVersionNumber(long overrideVersionNumber) {
        this.overrideVersionNumber = overrideVersionNumber;
    }

    public String getDisplayName() {
        long versionNum = getVersionNumber();
        String activeStr = isVersionActive() ? "active" : "inactive";
        if (versionNum < 1)
            return subjectName + " (" + activeStr + ')';
        else
            return subjectName + " (v" + versionNum + ", " + activeStr + ')';
    }

    /** updates the policy name, tab name etc */
    public void updateHeadings() {
        String displayName = getDisplayName();
        setName(displayName);
        getSplitPane().setName(displayName);
    }

    public void updateAssertions( final AssertionTreeNode atn ) {
        PolicyTreeUtils.updateAssertions( atn, new HashMap<Long,String>() );
    }

    private AssertionTreeNode getCurrentRoot() {
        return rootAssertion;
    }

    /**
     * Render the policy into the editor
     */
    private void renderPolicy() {
        updateHeadings();

        TreeModel policyTreeModel;
        final PolicyToolBar pt = topComponents.getPolicyToolBar();


        PolicyTreeModel model;
        if (rootAssertion != null) {
            model = PolicyTreeModel.policyModel(rootAssertion.asAssertion());
        } else {
            model = PolicyTreeModel.make(subject.getRootAssertion());
        }
        policyTreeModel = model;

        AssertionTreeNode newRootAssertion = (AssertionTreeNode)policyTreeModel.getRoot();
        ((AbstractTreeNode) newRootAssertion.getRoot()).addCookie(new AbstractTreeNode.NodeCookie(subject.getPolicyNode()));
        //rootAssertion.addCookie(new AbstractTreeNode.NodeCookie(subject.getEntityWithPolicyNode()));
        updateAssertions(newRootAssertion);

        policyTree.setModel(policyTreeModel);

        rootAssertion = newRootAssertion;
        pt.registerPolicyTree(policyTree);

        policyTreeModel.addTreeModelListener(policyTreeModellistener);
        final TreeNode root = (TreeNode)policyTreeModel.getRoot();
        final TreePath rootPath = new TreePath(((DefaultMutableTreeNode)root).getPath());
        policyTree.expandPath(rootPath);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (root.getChildCount() > 0) {
                    final TreeNode selNode = root.getChildAt(0);
                    final TreePath path = new TreePath(((DefaultMutableTreeNode)selNode).getPath());
                    policyTree.setSelectionPath(path);
                    policyTree.requestFocusInWindow();
                    searchForm.setPolicyTree(policyTree);
                }
            }
        });
    }

    /**
     * set various listeners that this panel uses
     */
    private void setEditorListeners() {
        subject.addPropertyChangeListener(policyPropertyChangeListener);
        addListener(ServicesAndPoliciesTree.NAME, treeModelListener);
        addListener(PolicyTree.NAME, treeModelListener);
        addListener(PolicyTree.NAME, policyTreeModelListener);
    }

    private void addListener(final String treeComponentName, final TreeModelListener treeModelListernToAdd) {
        JTree tree = (JTree)topComponents.getComponent(treeComponentName);
        if (tree == null) throw new IllegalStateException("Internal error - (could not get tree component)");
        tree.getModel().addTreeModelListener(treeModelListernToAdd);
    }

    private JComponent getMessagePane() {
        if (messagesTab != null) return messagesTab;
        messagesTextPane = new JTextPane();
        //make sure it's using an HTMLEditorKit. NOTE, this will share the style with ALL HTMLEditorKits (by default) and there is one set in the MainWindow.
        messagesTextPane.setEditorKit(new HTMLEditorKit());
        // messagesTextPane.setText("");
        messagesTextPane.addHyperlinkListener(hlinkListener);
        messagesTextPane.setEditable(false);
        messagesTab = new JTabbedPane();
        messagesTab.setBorder(null);
        JScrollPane scrollPane = new JScrollPane(messagesTextPane);
        scrollPane.setBorder(null);
        messagesTab.addTab("Policy Validation Messages", scrollPane);
        messagesTab.setTabPlacement(JTabbedPane.TOP);
        messagesTextPane.addMouseListener(Utilities.createContextMenuMouseListener(messagesTextPane, new Utilities.DefaultContextMenuFactory() {
            @Override
            public JPopupMenu createContextMenu(final JTextComponent tc) {
                JPopupMenu menu = super.createContextMenu(tc);
                menu.add(new ClearMessageAreaAction());
                Utilities.removeToolTipsFromMenuItems(menu);
                return menu;
            }
        }));
        return messagesTab;
    }

    private class ClearMessageAreaAction extends AbstractAction {
        public ClearMessageAreaAction() {
            putValue(Action.NAME, "Clear All");
            putValue(Action.SHORT_DESCRIPTION, "Clear message area");
        }

        /**
         * Invoked when an action occurs.
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            overWriteMessageArea("");
        }
    }

    /**
     * Return the ToolBarForTable instance for a given node or null.
     * 
     * @return ToolBarForTable
     */
    private PolicyEditToolBar getToolBar() {
        if (policyEditorToolbar != null) return policyEditorToolbar;
        policyEditorToolbar = new PolicyEditToolBar(enableUddi);
        policyEditorToolbar.setFloatable(false);
        policyEditorToolbar.setBorder(null);
        return policyEditorToolbar;
    }

    /**
     */
    final class PolicyEditToolBar extends JToolBar {
        private JButton buttonSaveAndActivate;
        private JButton buttonSaveOnly;
        private JButton buttonValidate;
        private final boolean enableUddi;

        public PolicyEditToolBar(boolean enableUddi) {
            super();
            this.enableUddi = enableUddi;
            this.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
            initComponents();
        }

        private void initComponents() {
            buttonSaveOnly = new JButton(getSaveOnlyAction());
            final BaseAction bsaa = (BaseAction)buttonSaveOnly.getAction();
            buttonSaveAndActivate = new JButton(getSaveAndActivateAction());
            add(buttonSaveAndActivate);
            final BaseAction ba = (BaseAction)buttonSaveAndActivate.getAction();
            ba.setEnabled(false);
            ba.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (PolicyEditorPanel.this.hasEmptyRoot()) {
                        ba.setEnabled(true);
                        buttonSaveAndActivate.setEnabled(true);
                        bsaa.setEnabled(true);
                        buttonSaveOnly.setEnabled(true);
                        return;
                    }
                    appendToMessageArea("<i>Policy saved and made active.</i>");
                    ba.setEnabled(false);
                    buttonSaveAndActivate.setEnabled(false);
                    bsaa.setEnabled(false);
                    buttonSaveOnly.setEnabled(false);
                }
            });

            add(buttonSaveOnly);
            bsaa.setEnabled(false);
            bsaa.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    if (PolicyEditorPanel.this.hasEmptyRoot()) {
                        bsaa.setEnabled(true);
                        buttonSaveOnly.setEnabled(true);
                        return;
                    }
                    appendToMessageArea("<i>Policy saved but not activated.</i>");
                    bsaa.setEnabled(false);
                    buttonSaveOnly.setEnabled(false);
                }
            });

            add(buttonValidate = new JButton(getServerValidateAction()));
            add(new JButton(getExportAction()));
            add(new JButton(getImportAction()));

            if (enableUddi) {
                JButton buttonUDDIImport = new JButton(getUDDIImportAction());
                this.add(buttonUDDIImport);
            }

            JButton showComments = new JButton();
            showComments.setAction(getHideShowCommentAction(showComments));
            this.add(showComments);

            JButton displayAssertionNumsBtn = new JButton();
            displayAssertionNumsBtn.setAction(getShowAssertionLineNumbersAction(displayAssertionNumsBtn));
            displayAssertionNumsBtn.setDisplayedMnemonicIndex(15);
            add(displayAssertionNumsBtn);
        }

        private void setSaveButtonsEnabled(boolean enabled) {
            buttonSaveAndActivate.setEnabled(enabled);
            buttonSaveAndActivate.getAction().setEnabled(enabled);
            buttonSaveOnly.setEnabled(enabled);
            buttonSaveOnly.getAction().setEnabled(enabled);
        }
    }

    public Action getValidateAction() {
        if (validateAction == null) {
            validateAction = new ValidatePolicyAction() {
                @Override
                protected void performAction() {
                    validatePolicy();
                }
            };
        }
        // Set the mnemonic and accelerator key
        validateAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_V);
        validateAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK));
        return validateAction;
    }


    /**
     * @return the policy xml that was validated, or null if validation canceled
     */
    private String fullValidate() {
        final Assertion assertion = getCurrentRoot().asAssertion();
        final String policyXml = WspWriter.getPolicyXml(assertion);
        final Wsdl wsdl;
        final boolean soap;
        final PolicyType type;
        final String internalTag;
        final PublishedService service;
        final HashMap<String, Policy> includedFragments = new HashMap<String, Policy>();
        try {
            service = getPublishedService();
            if (service == null) {
                wsdl = null;
                soap = getPolicyNode().getPolicy().isSoap();
                type = getPolicyNode().getPolicy().getType();
                internalTag = getPolicyNode().getPolicy().getInternalTag();
            } else {
                wsdl = service.parsedWsdl();
                soap = service.isSoap();
                type = PolicyType.PRIVATE_SERVICE;
                internalTag = null;
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't parse policy or WSDL", e);
        }

        extractFragmentsFromAssertion(getCurrentRoot().asAssertion(), includedFragments);

        final ConsoleLicenseManager licenseManager = Registry.getDefault().getLicenseManager();
        Callable<PolicyValidatorResult> callable;
        if (isPolicyValidationOn()) {
            callable = new Callable<PolicyValidatorResult>() {
                @Override
                public PolicyValidatorResult call() throws Exception {
                    final Policy policy = getPolicyNode().getPolicy();
                    final PolicyValidationContext pvc = new PolicyValidationContext(type, internalTag, wsdl, soap);
                    final PolicyValidatorResult result = policyValidator.validate(assertion, pvc, licenseManager);
                    policyValidator.checkForCircularIncludes(policy.getGuid(), policy.getName(), assertion, result);
                    final ServiceAdmin serviceAdmin = Registry.getDefault().getServiceManager();
                    final AsyncAdminMethods.JobId<PolicyValidatorResult> serverValidationJobId =
                            serviceAdmin.validatePolicy(policyXml, pvc, includedFragments);
                    double delay = DELAY_INITIAL;
                    Thread.sleep((long)delay);
                    PolicyValidatorResult serverValidationResult = null;
                    while ( serverValidationResult == null ) {
                        final String status = serviceAdmin.getJobStatus(serverValidationJobId);
                        if ( status == null ) {
                            log.warning("Server could not find our policy validation job ID");
                            break;
                        }
                        if (status.startsWith("i")) {
                            serverValidationResult = serviceAdmin.getJobResult(serverValidationJobId).result;
                            if ( serverValidationResult == null ) {
                                log.warning("Server returned a null job result");
                                break;
                            }
                        }
                        delay = delay >= DELAY_CAP ? DELAY_CAP : delay * DELAY_MULTIPLIER;
                        Thread.sleep((long)delay);
                    }

                    if ( serverValidationResult != null ) {
                        for ( final PolicyValidatorResult.Error error : serverValidationResult.getErrors() ) {
                            result.addError( error );
                        }
                        for ( final PolicyValidatorResult.Warning warning : serverValidationResult.getWarnings() ) {
                            result.addWarning( warning );
                        }
                    }

                    return result;
                }
            };
        } else {
            callable = NO_VAL_CALLBACK;
        }

        PolicyValidatorResult result = validateAndDisplay(callable, true);
        if (result == null)
            return null;

        ((DefaultTreeModel)policyTree.getModel()).nodeChanged(getCurrentRoot());
        return policyXml;
    }

    public Action getServerValidateAction() {
        if (serverValidateAction == null) {
            serverValidateAction = new ValidatePolicyAction() {
                @Override
                protected void performAction() {
                    if (!validating) {
                        try {
                            validating = true;
                            fullValidate();
                        } finally {
                            validating = false;
                        }
                    }
                }
            };
        }
        return serverValidateAction;
    }

    /**
     * display the policy validation result
     * 
     * @param r the policy validation result
     */
    void displayPolicyValidateResult(PolicyValidatorResult r) {
        for (Enumeration en = getCurrentRoot().preorderEnumeration(); en.hasMoreElements();) {
            AssertionTreeNode an = (AssertionTreeNode)en.nextElement();
            an.setValidatorMessages(r.messages(an.asAssertionIndexPath()));
        }
        if (!validating) {
            ((DefaultTreeModel)policyTree.getModel()).nodeChanged(getCurrentRoot());
        }
        overWriteMessageArea("");
        if (rootAssertion != null && containDisabledAssertion(rootAssertion.asAssertion())) {
            appendToMessageArea("<i>Some assertion(s) disabled.</i>");
        }
        for (PolicyValidatorResult.Error pe : r.getErrors()) {
            appendToMessageArea(MessageFormat.format("{0}</a> Error: {1}", getValidateMessageIntro(pe), pe.getMessage()));
        }
        for (PolicyValidatorResult.Warning pw : r.getWarnings()) {
            appendToMessageArea(MessageFormat.format("{0} Warning: {1}", getValidateMessageIntro(pw), pw.getMessage()));
        }
        if (r.getErrors().isEmpty() && r.getWarnings().isEmpty()) {
            appendToMessageArea("<i>Policy validated ok.</i>");
        }
    }

    private boolean containDisabledAssertion(Assertion assertion) {
        if (assertion == null) {
            return false;
        }
        if (! assertion.isEnabled()) {
            return true;
        }

        if (assertion instanceof CompositeAssertion) {
            CompositeAssertion parent = (CompositeAssertion)assertion;
            for (Object child: parent.getChildren()) {
                if (containDisabledAssertion((Assertion)child)) return true;
            }
        }
        return false;
    }

    public void updateActions(final AssertionTreeNode node, Action[] actions) {
        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            if (action instanceof SavePolicyAction) {
                if (((SavePolicyAction)action).isActivateAsWell()) {
                    actions[i] = policyEditorToolbar.buttonSaveAndActivate.getAction();
                    actions[i].setEnabled(policyEditorToolbar.buttonSaveAndActivate.isEnabled());
                } else {
                    actions[i] = policyEditorToolbar.buttonSaveOnly.getAction();
                    actions[i].setEnabled(policyEditorToolbar.buttonSaveOnly.isEnabled());
                }
            } else if (action instanceof ValidatePolicyAction) {
                actions[i] = policyEditorToolbar.buttonValidate.getAction();
                actions[i].setEnabled(policyEditorToolbar.buttonValidate.isEnabled());
            } else if (action instanceof AssertionMoveUpAction) {
                actions[i] = getAssertionMoveUpAction(node, action.isEnabled());
            } else if (action instanceof AssertionMoveDownAction) {
                actions[i] = getAssertionMoveDownAction(node, action.isEnabled());
            } else if (action instanceof DeleteAssertionAction) {
                actions[i] = getDeleteAssertionAction(node, action.isEnabled());
            } else if (action instanceof SelectMessageTargetAction) {
                actions[i] = getSelectMessageTargetAction(node, (SelectMessageTargetAction)action);
            } else if (action instanceof SelectIdentityTargetAction) {
                actions[i] = getSelectIdentityTargetAction(node, (SelectIdentityTargetAction)action);
            } else if (action instanceof SelectIdentityTagAction) {
                actions[i] = getSelectIdentityTagAction(node, (SelectIdentityTagAction)action);
            }
        }
    }

    /**
     * Based on the node action enum type, it will determine if the action should be enabled or disabled.
     * This basically determines whether the selected the node(s) can perform the selected node action in the
     * tree.  For example, if the top node is selected, then it shouldnt be able to move up in the tree any more.
     *
     * @param node      Last selected node
     * @param nodes     Selected list of nodes, if any
     * @param action    Action to be performed on the node
     * @return  TRUE if the action should be enabled, otherwise FALSE.
     */
    private boolean canEnableAction(AssertionTreeNode node, AssertionTreeNode[] nodes, Action action) {
        boolean canEnableAction = false;
        if (nodes == null) {
            if (action instanceof AssertionMoveUpAction)
                canEnableAction = node.canMoveUp();
            if (action instanceof AssertionMoveDownAction)
                canEnableAction = node.canMoveDown();
            if (action instanceof DeleteAssertionAction)
                canEnableAction = node.canDelete();
        } else if (nodes.length > 0) {
            boolean canEnableActionFromAll = true;
            for (AssertionTreeNode current : nodes) {
                if ( (current == null)
                        || (action instanceof AssertionMoveUpAction && !current.canMoveUp())
                        || (action instanceof AssertionMoveDownAction && !current.canMoveDown())
                        || (action instanceof DeleteAssertionAction && !current.canDelete()))
                    canEnableActionFromAll = false;
            }
            canEnableAction = canEnableActionFromAll;
        }
        return canEnableAction;
    }

    /**
     * @param node          Last selected node
     * @param isEnabled     Previous enabled action value
     * @return  A new action fitted based on the node selection.
     */
    private DeleteAssertionAction getDeleteAssertionAction(final AssertionTreeNode node, boolean isEnabled) {
        final TreePath[] paths = policyTree.getSelectionPaths();
        final AssertionTreeNode[] assertionTreeNodes = toAssertionTreeNodeArray(paths);
        DeleteAssertionAction action;
        if (paths != null && paths.length > 1) {
            action = new DeleteAssertionAction(node, assertionTreeNodes);
        } else {
            action = new DeleteAssertionAction(node);
        }
        action.setEnabled(isEnabled && canEnableAction(node, assertionTreeNodes, action));
        return action;
    }

    /**
     * Support multiple selection (edit) for message target.
     *
     * <p>Multiple edit is only permitted if all selected items have the same setting.</p>
     */
    private SelectMessageTargetAction getSelectMessageTargetAction( final AssertionTreeNode node,
                                                                    final SelectMessageTargetAction selectMessageTargetAction) {
        SelectMessageTargetAction action = selectMessageTargetAction;
        if ( action.isEnabled() ) {
            final TreePath[] paths = policyTree.getSelectionPaths();
            if ( paths != null && paths.length > 1 ) {
                boolean sameTarget = true;

                final AssertionTreeNode[] assertionTreeNodes = toAssertionTreeNodeArray(paths);
                final Assertion target = node.asAssertion();
                final List<MessageTargetable> list = new ArrayList<MessageTargetable>();
                for ( AssertionTreeNode assertionTreeNode : assertionTreeNodes ) {
                    Assertion assertion = assertionTreeNode.asAssertion();
                    if ( assertion instanceof MessageTargetable ) {
                        MessageTargetable mt = (MessageTargetable) assertion;
                        list.add(mt);
                        sameTarget = AssertionUtils.isSameTargetMessage( target, assertion );
                    } else {
                        sameTarget = false;
                    }
                    if ( !sameTarget ) break;
                }

                if ( sameTarget ) {
                    action = new SelectMessageTargetAction( assertionTreeNodes, list.toArray(new MessageTargetable[list.size()]) );
                } else {
                    action.setEnabled( false ); // disable, since we cannot edit all at once
                }
            }
        }

        return action;
    }

    /**
     * Support multiple selection (edit) for identity target.
     *
     * <p>Multiple edit is only permitted if all selected items have the same
     * setting and target the same message.</p>
     */
    private SelectIdentityTargetAction getSelectIdentityTargetAction( final AssertionTreeNode node,
                                                                      final SelectIdentityTargetAction selectIdentityTargetAction ) {
        SelectIdentityTargetAction action = selectIdentityTargetAction;
        if ( action.isEnabled() ) {
            final TreePath[] paths = policyTree.getSelectionPaths();
            if ( paths != null && paths.length > 1 ) {
                boolean sameTarget = true;

                final AssertionTreeNode[] assertionTreeNodes = toAssertionTreeNodeArray(paths);
                final Assertion targetAssertion = node.asAssertion();
                final IdentityTarget target = new IdentityTarget(((IdentityTargetable) targetAssertion).getIdentityTarget());
                final List<IdentityTargetable> list = new ArrayList<IdentityTargetable>();
                for ( AssertionTreeNode assertionTreeNode : assertionTreeNodes ) {
                    Assertion assertion = assertionTreeNode.asAssertion();
                    if ( assertion instanceof IdentityTargetable ) {
                        IdentityTargetable it = (IdentityTargetable) assertion;
                        list.add(it);
                        sameTarget = new IdentityTarget(it.getIdentityTarget()).equals(target) &&
                                     AssertionUtils.isSameTargetMessage( targetAssertion, assertion );
                    } else {
                        sameTarget = false;
                    }
                    if ( !sameTarget ) break;
                }

                if ( sameTarget ) {
                    action = new SelectIdentityTargetAction( assertionTreeNodes, list.toArray(new IdentityTargetable[list.size()]) );
                } else {
                    action.setEnabled( false ); // disable, since we cannot edit all at once
                }
            }
        }

        return action;    }

    /**
     * Support multiple selection (edit) for identity tags.
     *
     * <p>Multiple edit is only permitted if all selected items have the same
     * setting.</p>
     */
    private SelectIdentityTagAction getSelectIdentityTagAction( final AssertionTreeNode node,
                                                                final SelectIdentityTagAction selectIdentityTagAction ) {
        SelectIdentityTagAction action = selectIdentityTagAction;
        if ( action.isEnabled() ) {
            final TreePath[] paths = policyTree.getSelectionPaths();
            if ( paths != null && paths.length > 1 ) {
                boolean sameTag = true;

                final AssertionTreeNode[] assertionTreeNodes = toAssertionTreeNodeArray(paths);
                final String idTag = ((IdentityTagable) node.asAssertion()).getIdentityTag();
                final List<IdentityTagable> list = new ArrayList<IdentityTagable>();
                for ( AssertionTreeNode assertionTreeNode : assertionTreeNodes ) {
                    Assertion assertion = assertionTreeNode.asAssertion();
                    if ( assertion instanceof IdentityTagable ) {
                        IdentityTagable it = (IdentityTagable) assertion;
                        list.add(it);
                        sameTag = idTag==null && it.getIdentityTag()==null || (idTag != null && idTag.equalsIgnoreCase(it.getIdentityTag()));
                    } else {
                        sameTag = false;
                    }
                    if ( !sameTag ) break;
                }

                if ( sameTag ) {
                    action = new SelectIdentityTagAction( assertionTreeNodes, list.toArray(new IdentityTagable[list.size()]) );
                } else {
                    action.setEnabled( false ); // disable, since we cannot edit all at once
                }
            }
        }

        return action;
    }

    /**
     * @param node          Last selected node
     * @param isEnabled     Previous enabled action value
     * @return  A new action fitted based on the node selection.
     */
    private AssertionMoveUpAction getAssertionMoveUpAction(final AssertionTreeNode node, boolean isEnabled) {
        final TreePath[] paths = policyTree.getSelectionPaths();
        final AssertionTreeNode[] assertionTreeNodes = toAssertionTreeNodeArray(paths);
        AssertionMoveUpAction action;
        if (paths != null && paths.length > 1) {
            action = new AssertionMoveUpAction(node) {
                @Override
                protected void performAction() {
                    nodes = assertionTreeNodes;
                    super.performAction();
                    final JTree tree = policyTree;
                    if (paths != null && tree != null) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                tree.setSelectionPaths(paths);
                            }
                        });
                    }
                }
            };
        } else {
            action = new AssertionMoveUpAction(node);
        }
        action.setEnabled(isEnabled && canEnableAction(node, assertionTreeNodes, action));
        return action;
    }

    /**
     * @param node          Last selected node
     * @param isEnabled     Previous enabled action value
     * @return  A new action fitted based on the node selection.
     */
    private AssertionMoveDownAction getAssertionMoveDownAction(final AssertionTreeNode node, boolean isEnabled) {
        final TreePath[] paths = policyTree.getSelectionPaths();
        final AssertionTreeNode[] assertionTreeNodes = toAssertionTreeNodeArray(paths);
        AssertionMoveDownAction action;
        if (paths != null && paths.length > 1) {
            action = new AssertionMoveDownAction(node) {
                @Override
                protected void performAction() {
                    nodes = assertionTreeNodes;
                    super.performAction();
                    final JTree tree = policyTree;
                    if (paths != null && tree != null) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                tree.setSelectionPaths(paths);
                            }
                        });
                    }
                }
            };
        } else {
            action = new AssertionMoveDownAction(node);
        }
        action.setEnabled(isEnabled && canEnableAction(node, assertionTreeNodes, action));
        return action;
    }

    /**
     * Coverts the given tree paths into an array of assertion tree nodes.
     *
     * @param paths The tree path to convert
     * @return  A list of assertion tree nodes converted, will not return null.
     */
    private AssertionTreeNode[] toAssertionTreeNodeArray(TreePath[] paths) {
        java.util.List<AssertionTreeNode> assertionTreeNodes = new ArrayList<AssertionTreeNode>();

        if (paths != null) {
            for (TreePath path : paths) {
                assertionTreeNodes.add((AssertionTreeNode)path.getLastPathComponent());
            }
        }
        return assertionTreeNodes.toArray(new AssertionTreeNode[assertionTreeNodes.size()]);
    }

    private void appendToMessageArea(String s) {
        if (messagesTextPane == null) return;
        try {
            int pos = messagesTextPane.getDocument().getLength();
            //if (pos > 0) s = "\n" + s;
            StringReader sr = new StringReader(s);
            EditorKit editorKit = messagesTextPane.getEditorKit();
            editorKit.read(sr, messagesTextPane.getDocument(), pos);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void overWriteMessageArea(String s) {
        if (messagesTextPane == null) return;
        messagesTextPane.setText(s);
    }

    /**
     * get the validator result message intro
     *
     * @return the string as a message
     */
    private String getValidateMessageIntro(PolicyValidatorResult.Message pe) {
        String msg;
        Assertion assertion = null;

        AssertionTreeNode atn = getCurrentRoot().getAssertionByIndexPath(pe.getAssertionIndexPath());
        if ( atn != null ) {
            assertion = atn.asAssertion();
        }

        if (assertion != null) {
            msg = MessageFormat.format( "Assertion: <a href=\"file://assertion#{0}\">{1}</a>",
                    pathString(pe.getAssertionOrdinal(), pe.getAssertionIndexPath()),
                    atn.getName(false));
        } else {
            msg = ""; // supplied message (non single assertion related)
        }
        return msg;
    }

    /**
     * Build a csv string starting with the ordinal and ending with the index path
     */
    private String pathString( final int ordinal, final java.util.List<Integer> path ) {
        StringBuilder builder = new StringBuilder();

        builder.append( ordinal );
        for ( Integer index : path ) {
            builder.append(',');
            builder.append(index);
        }

        return builder.toString();
    }


    // listener for policy tree changes
    TreeModelListener policyTreeModellistener = new TreeModelListener() {
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            if (!(e instanceof PolicyTreeModelEvent)) return; // do not validate this

            final Object[] path = e.getPath();

            if(path[path.length - 1] instanceof AssertionTreeNode){
                AssertionTreeNode parentNode = (AssertionTreeNode) path[path.length - 1];
                final int[] ints = e.getChildIndices();
                for (int anInt : ints) {
                    final TreeNode at = parentNode.getChildAt(anInt);
                    if(at instanceof AssertionTreeNode){
                        AssertionTreeNode assertionNode = (AssertionTreeNode) at;
                        assertionNode.clearPropsString();
                    }
                }
            }

            if (!initialValidate) {
                enableButtonSave();
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (!validating) {
                                try {
                                    validating = true;
                                    validatePolicy();
                                } finally {
                                    validating = false;
                                }
                            }
                        } catch (Throwable t) {
                            log.severe(t.getMessage());
                        }
                    }
                });
            }
        }

        @Override
        public void treeNodesInserted(final TreeModelEvent e) {
            enableButtonSave();
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            enableButtonSave();
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            enableButtonSave();
        }

        private void enableButtonSave() {
            overWriteMessageArea("");
            SecureAction saveButtonAction = (SecureAction) PolicyEditorPanel.this.getSaveOnlyAction();
            if (saveButtonAction.isAuthorized()) {
                policyEditorToolbar.setSaveButtonsEnabled(true);
            } else {
                policyEditorToolbar.setSaveButtonsEnabled(false);
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null,
                "You do not have permission to update this service/policy. In order to keep your changes, you may export it.", null);
            }
        }
    };

    // listener for policy tree changes
    final TreeModelListener policyTreeModelListener = new TreeModelListener() {
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            searchForm.setPolicyTree(policyTree);
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            searchForm.setPolicyTree(policyTree);
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            searchForm.setPolicyTree(policyTree);
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            searchForm.setPolicyTree(policyTree);
        }
    };


    // listener for policy tree changes
    final TreeModelListener treeModelListener = new TreeModelListener() {
        @Override
        public void treeNodesChanged(TreeModelEvent e) {
            searchForm.setPolicyTree(policyTree);
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e) {
            searchForm.setPolicyTree(policyTree);
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            Object[] children = e.getChildren();
            for (Object child : children) {
                if (child == subject.getPolicyNode()) {
                    log.fine("Service or Policy node deleted, disabling save controls");
                    policyEditorToolbar.setSaveButtonsEnabled(false);
                }
            }
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
        }

    };

    private final PropertyChangeListener
            policyPropertyChangeListener = new PropertyChangeListener() {
          /**
           * This method gets called when a bound property is changed.
           *
           * @param evt A PropertyChangeEvent object describing the event source
           *            and the property that has changed.
           */
          @Override
          public void propertyChange(PropertyChangeEvent evt) {
              log.info(evt.getPropertyName() + "changed");
              if (POLICYNAME_PROPERTY.equals(evt.getPropertyName())) {
                  renderPolicy();
              } else if ("policy".equals(evt.getPropertyName())) {
                  rootAssertion = null;
                  renderPolicy();
                  policyEditorToolbar.setSaveButtonsEnabled(true);
                  validatePolicy();
              }
          }
      };

    private final HyperlinkListener
      hlinkListener = new HyperlinkListener() {
          /**
           * Called when a hypertext link is updated.
           *
           * @param e the event responsible for the update
           */
          @Override
          public void hyperlinkUpdate(HyperlinkEvent e) {
              if (HyperlinkEvent.EventType.ACTIVATED != e.getEventType())
                  return;
              URI uri = URI.create(e.getURL().toString());
              String f = uri.getFragment();
              if (f == null) return;
              try {
                  java.util.List<Integer> path = new ArrayList<Integer>();
                  StringTokenizer strtok = new StringTokenizer(f, ",");
                  while (strtok.hasMoreTokens()) {
                      path.add( Integer.parseInt( strtok.nextToken() ) );
                  }

                  AssertionTreeNode an = getCurrentRoot().getAssertionByIndexPath( path.subList( 1, path.size() ) );
                  if ( an != null ) {
                      TreePath p = new TreePath(an.getPath());
                      if (!policyTree.hasBeenExpanded(p) || !policyTree.isExpanded(p)) {
                          policyTree.expandPath(p);
                      }
                      policyTree.setSelectionPath(p);
                  }
              } catch (NumberFormatException ex) {
                  ex.printStackTrace();
              }
          }
      };

    /**
     * Invoked when a component has been added to the container.
     */
    @Override
    public void componentAdded(ContainerEvent e) {
    }

    /**
     * Invoked when a component has been removed from the container.
     */
    @Override
    public void componentRemoved(ContainerEvent e) {
        log.fine("Resetting the policy editor panel");
        subject.removePropertyChangeListener(policyPropertyChangeListener);
        policyTree.setPolicyEditor(null);
        policyTree.setModel(null);
        final PolicyToolBar pt = topComponents.getPolicyToolBar();
        if (pt != null) {
            pt.unregisterPolicyTree(policyTree);
        }
    }

    /**
     * Invoked when a component has to be added to the container.
     *
     * @param e the container event
     * @throws com.l7tech.console.event.ContainerVetoException
     *          if the recipient wishes to stop
     *          (not perform) the action.
     */
    @Override
    public void componentWillAdd(ContainerEvent e)
      throws ContainerVetoException {
    }

    /**
     * Invoked when a component has to be removed from to the container.
     *
     * @param e the container event
     * @throws ContainerVetoException if the recipient wishes to stop
     *                                (not perform) the action.
     */
    @Override
    public void componentWillRemove(ContainerEvent e)
      throws ContainerVetoException {
        if (e.getChild() == this) {
            if (isUnsavedChanges()) {
                if (!TopComponents.getInstance().isConnectionLost()) {
                    int answer = (JOptionPane.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                      "<html><center><b>Do you want to save changes to service policy " +
                      "for<br> '" + HtmlUtil.escapeHtmlCharacters(subjectName) + "' ?</b><br>The changed policy will not be activated.</center></html>",
                      "Save Service Policy",
                      JOptionPane.YES_NO_CANCEL_OPTION));
                    if (answer == JOptionPane.YES_OPTION) {
                        policyEditorToolbar.buttonSaveOnly.getAction().actionPerformed(null);
                    } else if ((answer == JOptionPane.CANCEL_OPTION)) {
                        throw new ContainerVetoException(e, "User aborted");
                    }
                } else {
                    String saveOption = "Save Policy";
                    String discardOption = "Discard Policy";
                    Object[] options = new String[] { saveOption, discardOption };

                    int answer = JOptionPane.showOptionDialog(TopComponents.getInstance().getTopParent(),
                            "<html><center><b>Connection Lost.  Do you want to save changes to service policy " +
                            "file?</b></center></html>",
                            "Save Service Policy",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            options,
                            saveOption);
                    if (answer != 1)
                        getSimpleExportAction().actionPerformed(null);
                }
            }
            final PolicyToolBar pt = topComponents.getPolicyToolBar();
            pt.disableAll();
            TopComponents.getInstance().firePolicyEditDone();
        }
    }

    /**
     * prune duplicate messsages
     *
     * @param result the validation result
     * @return the result containing unique messages
     */
    private PolicyValidatorResult pruneDuplicates(PolicyValidatorResult result) {
        PolicyValidatorResult pr = new PolicyValidatorResult();

        Set<PolicyValidatorResult.Error> errors = new LinkedHashSet<PolicyValidatorResult.Error>();
        errors.addAll(result.getErrors());
        for (PolicyValidatorResult.Error error : errors) {
            pr.addError(error);
        }

        Set<PolicyValidatorResult.Warning> warnings = new LinkedHashSet<PolicyValidatorResult.Warning>();
        warnings.addAll(result.getWarnings());
        for (PolicyValidatorResult.Warning warning : warnings) {
            pr.addWarning(warning);
        }

        return pr;
    }

    private String getHomePath() {
        return TopComponents.getInstance().isApplet() ? null :  preferences.getHomePath();
    }

    public Action getSaveAndActivateAction() {
        if (saveAndActivateAction != null) return saveAndActivateAction;
        if (subject.getPolicyNode() == null) {
            saveAndActivateAction = makeExportAction();
            return saveAndActivateAction;
        }
        try {
            saveAndActivateAction = makeSavePolicyAction(true);
            return saveAndActivateAction;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get current PublishedService", e);
        }
    }

    public Action getSaveOnlyAction() {
        if (saveOnlyAction != null) return saveOnlyAction;
        if (subject.getPolicyNode() == null) {
            saveOnlyAction = makeExportAction();
            return saveOnlyAction;
        }
        try {
            saveOnlyAction = makeSavePolicyAction(false);
            return saveOnlyAction;
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get current PublishedService", e);
        }
    }

    private SecureAction makeSavePolicyAction(final boolean activateAsWell) {
        SecureAction ret = new SavePolicyAction(activateAsWell) {
            @Override
            protected void performAction() {
                // fla, bugzilla 1094. all saves are now preceeded by a validation action
                if (!validating) {
                    try {
                        validating = true;
                        boolean canValidate = TopComponents.getInstance().isTrusted();
                        String xml = canValidate ?
                                fullValidate() :
                                WspWriter.getPolicyXml(getCurrentRoot().asAssertion());
                        if (xml != null) {
                            this.node = rootAssertion;

                            if (hasEmptyRoot()) {
                                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(),
                                    "Cannot save an empty policy or published service.  Please add assertion(s) into it or delete it.", "Save Warning",
                                    JOptionPane.WARNING_MESSAGE, null);
                                return;
                            }

                            HashMap<String, Policy> includedFragments = new HashMap<String, Policy>();
                            extractFragmentsFromAssertion(rootAssertion.asAssertion(), includedFragments);
                            if(PolicyEditorPanel.this.removeNameFromIncludeAssertions(rootAssertion.asAssertion())) {
                                xml = WspWriter.getPolicyXml(rootAssertion.asAssertion());
                            }
                            super.performAction(xml, includedFragments);

                            if(getFragmentNameGuidMap() != null && !getFragmentNameGuidMap().isEmpty()) {
                                try {
                                    updateIncludeAssertions(rootAssertion.asAssertion(), getFragmentNameGuidMap());
                                    PolicyEditorPanel.this.renderPolicy();
                                    PolicyEditorPanel.this.topComponents.refreshPoliciesFolderNode();
                                } catch(Exception e) {
                                    log.log(Level.SEVERE, "Cannot update policy with new fragment OIDs", e);
                                }
                            }
                        }
                    } finally {
                        validating = false;
                    }
                }
            }
        };
        ret.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
        int mask = activateAsWell
                   ? ActionEvent.ALT_MASK | ActionEvent.CTRL_MASK
                   : ActionEvent.ALT_MASK;
        ret.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, mask));
        return ret;
    }

    private boolean hasEmptyRoot() {
        if (rootAssertion == null) throw new NullPointerException("Root assertion must not be null.");
        if (!(rootAssertion.asAssertion() instanceof AllAssertion)) throw new RuntimeException("Root assertion must be an AllAssertion.");

        AllAssertion assertion = (AllAssertion)rootAssertion.asAssertion();
        return assertion.isRoot() && assertion.isEmpty();
    }

    private boolean removeNameFromIncludeAssertions(Assertion rootAssertion) {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            boolean retVal = false;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                retVal = retVal | removeNameFromIncludeAssertions(child);
            }
            return retVal;
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            includeAssertion.setPolicyName(null);
            if(includeAssertion.retrieveFragmentPolicy() != null) {
                try {
                    if(removeNameFromIncludeAssertions(includeAssertion.retrieveFragmentPolicy().getAssertion())) {
                        includeAssertion.retrieveFragmentPolicy().setXml(WspWriter.getPolicyXml(includeAssertion.retrieveFragmentPolicy().getAssertion()));
                    }
                } catch(IOException e) {
                    // Ignore. If there was a real error with the include, it should have been exposed during the import
                }
            }

            return true;
        }

        return false;
    }

    private void extractFragmentsFromAssertion(Assertion rootAssertion, HashMap<String, Policy> fragments) {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                extractFragmentsFromAssertion(child, fragments);
            }
        } else if(rootAssertion instanceof PolicyReference) {
            PolicyReference policyReference = (PolicyReference)rootAssertion;
            if(policyReference.retrieveFragmentPolicy() != null) {
                fragments.put(policyReference.retrievePolicyGuid(), policyReference.retrieveFragmentPolicy());
                try {
                    extractFragmentsFromAssertion(policyReference.retrieveFragmentPolicy().getAssertion(), fragments);
                } catch(IOException e) {
                    // Ignore. If there was a real error with the include, it should have been exposed during the import
                }
            }
        }
    }

    private void updateIncludeAssertions(Assertion rootAssertion, Map<String, String> fragmentNameGuidMap) {
        if(rootAssertion instanceof CompositeAssertion) {
            CompositeAssertion compAssertion = (CompositeAssertion)rootAssertion;
            for(Iterator it = compAssertion.children();it.hasNext();) {
                Assertion child = (Assertion)it.next();
                updateIncludeAssertions(child, fragmentNameGuidMap);
            }
        } else if(rootAssertion instanceof Include) {
            Include includeAssertion = (Include)rootAssertion;
            if(fragmentNameGuidMap.containsKey(includeAssertion.getPolicyName())) {
                includeAssertion.setPolicyGuid(fragmentNameGuidMap.get(includeAssertion.getPolicyName()));
                includeAssertion.replaceFragmentPolicy(null);
            }
        } else if(rootAssertion instanceof PolicyReference) {
            PolicyReference policyReference = (PolicyReference)rootAssertion;
            if(policyReference.retrieveFragmentPolicy() != null && fragmentNameGuidMap.containsKey(policyReference.retrieveFragmentPolicy().getName())) {
                policyReference.replaceFragmentPolicy(null);
            }
        }
    }

    private SecureAction makeExportAction() {
        return new ExportPolicyToFileAction(getHomePath()) {
            @Override
            public String getName() {
                return "Save Policy";
            }
            @Override
            public String getDescription() {
                return "Save the policy to a file along with external references.";
            }
            @Override
            protected void performAction() {
                Assertion assertion = rootAssertion.asAssertion();
                String dialogTitle = "Save Policy to File";
                File policyFile = exportPolicy(dialogTitle, assertion);
                if (policyFile != null) {
                    subjectName = policyFile.getName();
                    updateHeadings();
                }
            }
        };
    }

    public Action getExportAction() {
        if (exportPolicyAction == null) {
            exportPolicyAction = new ExportPolicyToFileAction(getHomePath()) {
                @Override
                protected void performAction() {
                    Assertion assertion = rootAssertion.asAssertion();
                    exportPolicy(getName(), assertion);
                }
            };
        }
        // Set the mnemonic and accelerator key
        exportPolicyAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_E);
        exportPolicyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_E, ActionEvent.ALT_MASK));
        return exportPolicyAction;
    }

    public Action getSimpleExportAction() {
        if (simpleExportPolicyAction == null) {
            simpleExportPolicyAction = new ExportPolicyToFileAction(getHomePath()) {
                @Override
                protected void performAction() {
                    Assertion assertion = rootAssertion.asAssertion();
                    exportPolicy(getName(), assertion);
                }
                @Override
                protected void serializeToFile(Assertion rootAssertion, File policyFile) throws IOException, SAXException {
                    // do policy to xml
                    Document policydoc = XmlUtil.stringToDocument(WspWriter.getPolicyXml(rootAssertion));
                    // write xml to file
                    FileOutputStream fos = null;
                    try {
                        //noinspection IOResourceOpenedButNotSafelyClosed
                        fos = new FileOutputStream(policyFile);
                        XmlUtil.nodeToFormattedOutputStream(policydoc, fos);
                        fos.flush();
                    } finally {
                        ResourceUtils.closeQuietly(fos);
                    }
                }
            };
        }
        return simpleExportPolicyAction;
    }

    /**
     * Get the tree to redraw itself without telling causing the 'Save and Activate' and 'Save' buttons from
     * becoming activated. This is needed for comments which can cause the displayed value for a node to change,
     * which normally only happens when it's internal values have changed due to the user editing the properties dialog.
     */
    public void updateNodesWithComments(){
        //update the tree so that the comment is displayed correctly
        //this should not cause the 'Save and Activate' or 'Save' buttons to become activated as there is no
        //actual change to the assertions, we are just updating the display of comments
        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel) tree.getModel();
            updateNode((AssertionTreeNode) model.getRoot(), model);

        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }

    }

    private void updateNode(AssertionTreeNode node, PolicyTreeModel model) {
        final Assertion assertion = node.asAssertion();
        if (assertion.getAssertionComment() != null) {
            model.nodeChanged(node);
        }

        final int childCount = node.getChildCount();
        if (childCount < 1) return;
        for (int i = 0; i < childCount; i++) {
            updateNode((AssertionTreeNode) node.getChildAt(i), model);
        }
    }

    public Action getHideShowCommentAction(final JButton button){

        if(hideShowCommentsAction == null){
            hideShowCommentsAction = new SecureAction(null) {
                        @Override
                        protected void performAction() {
                            final String showState = preferences.getString(SHOW_COMMENTS);
                            final boolean shown = Boolean.parseBoolean(showState);
                            //shown = true means the button should say 'Hide'

                            if (shown) preferences.putProperty(SHOW_COMMENTS, "false");
                            else preferences.putProperty(SHOW_COMMENTS, "true");

                            button.setText(getName());
                            updateNodesWithComments();
                        }

                        @Override
                        public String getName() {

                            final SsmPreferences preferences = TopComponents.getInstance().getPreferences();
                            final String showState = preferences.getString(SHOW_COMMENTS);
                            final boolean shown = Boolean.parseBoolean(showState);

                            if (shown) {
                                return "Hide Comments";
                            } else {
                                return "Show Comments";
                            }
                        }

                        @Override
                        protected String iconResource() {
                            return "com/l7tech/console/resources/About16.gif";
                        }
                    };
        }
        // Set the mnemonic and accelerator key
        hideShowCommentsAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
        hideShowCommentsAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));

        return hideShowCommentsAction;

    }

    public Action getUDDIImportAction() {
        return new SecureAction(null, "set:core") {
            @Override
            public boolean isAuthorized() {
                PublishedService svc = getPublishedService();
                return svc != null && canAttemptOperation(new AttemptedUpdate(EntityType.SERVICE, svc)) && enableUddi;
            }

            @Override
            public String getName() {
                return "Import From UDDI";
            }

            @Override
            protected String iconResource() {
                return "com/l7tech/console/resources/server16.gif";
            }

            @Override
            protected void performAction() {
                ImportPolicyFromUDDIWizard wiz = ImportPolicyFromUDDIWizard.getInstance(TopComponents.getInstance().getTopParent());
                wiz.pack();
                Utilities.centerOnScreen(wiz);
                wiz.setVisible(true);
                if (wiz.importedPolicy() != null) {
                    getPublishedService().getPolicy().setXml(wiz.importedPolicy());
                    rootAssertion = null;
                    renderPolicy();
                    policyEditorToolbar.setSaveButtonsEnabled(true);
                    validatePolicy();
                }
            }
        };
    }

    public Action getImportAction() {
        if (importPolicyAction == null) {
            if (subject.getPolicyNode() != null) {
                importPolicyAction = new ImportPolicyFromFileAction(getHomePath()) {

                    @Override
                    protected OperationType getOperation() {
                        return OperationType.UPDATE;
                    }

                    @Override
                    protected void performAction() {
                        try {
                            Policy policy = subject.getPolicyNode().getPolicy();
                            if (importPolicy(policy)) {
                                rootAssertion = null;
                                renderPolicy();
                                policyEditorToolbar.setSaveButtonsEnabled(true);
                                validatePolicy();
                            }
                        } catch (FindException e) {
                            log.log(Level.SEVERE, "cannot get back policy", e);
                        }
                    }
                };
            } else {
                // TODO import in not connected mode
            }
        }
        // Set the mnemonic and accelerator key
        importPolicyAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_I);
        importPolicyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_I, ActionEvent.ALT_MASK));
        return importPolicyAction;
    }

    /**
     * Create/get an action showing/hiding assertion line numbers in the policy editor panel.
     *
     * @param targetButton could be either JButton (used in PolicyEditorPanel) or JMenuItem (used in MainWindow)
     * @return an action showing/hiding assertion line numbers.
     */
    public Action getShowAssertionLineNumbersAction(final AbstractButton targetButton) {
        if (targetButton != null && !showLnNumsButtons.contains(targetButton)) {
            showLnNumsButtons.add(targetButton);
        }

        if (showAssertionLineNumbersAction == null) {
            showAssertionLineNumbersAction = new SecureAction(null) { //todo: access control for assertions
                @Override
                protected void performAction() {
                    // Update the status of assertion line numbers shown/hidden in the policy editor panel.
                    boolean lineNumsShown = !policyTreePane.isNumbersVisible();
                    policyTreePane.setNumbersVisible( lineNumsShown );
                    preferences.putProperty( SHOW_ASSERTION_NUMBERS, Boolean.toString(lineNumsShown));
                    try {
                        preferences.store();
                    } catch ( IOException e ) {
                        log.warning( "Unable to store preferences " + ExceptionUtils.getMessage(e));
                    }

                    // Update the buttons such as policy edit button or menuItem.
                    for (AbstractButton button: showLnNumsButtons) {
                        button.setText(getName());
                        button.setDisplayedMnemonicIndex(15);
                        button.setIcon(new ImageIcon(ImageCache.getInstance().getIcon(iconResource())));
                    }
                }

                @Override
                public String getName() {
                    return policyTreePane.isNumbersVisible() ? "Hide Assertion Numbers" : "Show Assertion Numbers";
                }

                @Override
                protected String iconResource() {
                    return policyTreePane.isNumbersVisible() ?
                        "com/l7tech/console/resources/HideLineNumbers16.png" :
                        "com/l7tech/console/resources/ShowLineNumbers16.png";
                }
            };

            // Set the mnemonic and accelerator key
            showAssertionLineNumbersAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_N);
            showAssertionLineNumbersAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.ALT_MASK));
        }

        return showAssertionLineNumbersAction;
    }
}
