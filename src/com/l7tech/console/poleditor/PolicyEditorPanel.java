package com.l7tech.console.poleditor;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.security.rbac.AttemptedUpdate;
import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.console.action.*;
import com.l7tech.console.event.ContainerVetoException;
import com.l7tech.console.event.VetoableContainerListener;
import com.l7tech.console.panels.CancelableOperationDialog;
import com.l7tech.console.panels.ImportPolicyFromUDDIWizard;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.FilteredTreeModel;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.ServicesTree;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.ConsoleLicenseManager;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SsmPreferences;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.PolicyValidator;
import com.l7tech.policy.PolicyValidatorResult;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.PublishedService;

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
import java.io.StringReader;
import java.io.IOException;
import java.io.FileOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;

/**
 * The class represents the policy editor
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class PolicyEditorPanel extends JPanel implements VetoableContainerListener {
    static Logger log = Logger.getLogger(PolicyEditorPanel.class.getName());
    private static final String MESSAGE_AREA_DIVIDER_KEY = "policy.editor." + JSplitPane.DIVIDER_LOCATION_PROPERTY;
    public static final String SERVICENAME_PROPERTY = "service.name";
    private static final long TIME_BEFORE_OFFERING_CANCEL_DIALOG = 500L;
    private JTextPane messagesTextPane;
    private AssertionTreeNode rootAssertion;
    private PolicyTree policyTree;
    private PolicyEditToolBar policyEditorToolbar;
    private JSplitPane splitPane;
    private final TopComponents topComponents = TopComponents.getInstance();
    private JScrollPane policyTreePane;
    private boolean initialValidate = false;
    private boolean messageAreaVisible = false;
    private JTabbedPane messagesTab;
    private boolean validating = false;
    private SecureAction saveAction;
    private ValidatePolicyAction validateAction;
    private ValidatePolicyAction serverValidateAction;
    private ExportPolicyToFileAction exportPolicyAction;
    private ExportPolicyToFileAction simpleExportPolicyAction;
    private ImportPolicyFromFileAction importPolicyAction;
    private PolicyEditorSubject subject;
    private String subjectName;
    private final SsmPreferences preferences = TopComponents.getInstance().getPreferences();
    private final boolean enableUddi;

    public interface PolicyEditorSubject {
        /**
         *
         * @return the service node if the policy is attached to a published service, null otherwise
         */
        ServiceNode getServiceNode();

        /**
         * get the root assertion of the policy to edit
         * @return the root of the policy
         */
        Assertion getRootAssertion();

        /**
         * @return the name of either the published service or the policy file. never null
         */
        String getName();

        void addPropertyChangeListener(PropertyChangeListener servicePropertyChangeListener);

        void removePropertyChangeListener(PropertyChangeListener servicePropertyChangeListener);

        boolean hasWriteAccess();
    }

    public PolicyEditorPanel(PolicyEditorSubject subject, PolicyTree pt, boolean validateOnOpen, boolean enableUddi) {
        if (subject == null || pt == null) {
            throw new IllegalArgumentException();
        }
        this.enableUddi = enableUddi;
        this.subject = subject;
        subjectName = subject.getName();
        this.policyTree = pt;
        policyTree.setWriteAccess(subject.hasWriteAccess());
        layoutComponents();

        renderPolicy(false);
        setEditorListeners();
        if (validateOnOpen) {
            SwingUtilities.invokeLater(new Runnable() {
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
    public void setName(String name) {
        String oldName = getName();
        super.setName(name);
        this.firePropertyChange("name", oldName, name);
    }

    public PolicyTree getPolicyTree() {
        return policyTree;
    }

    /**
     * Get the service node that this panel is editing
     * <p/>
     * Note that it does not return the copy, therefore any
     * changes made through this method are not visible to
     * the policy editor.
     * 
     * @return the noe that the panel is editing
     */
    public ServiceNode getServiceNode() {
        return subject.getServiceNode();
    }

    protected boolean isPolicyValidationOn() {
        String maybe = preferences.asProperties().getProperty(SsmPreferences.ENABLE_POLICY_VALIDATION_ID);
        boolean output = Boolean.valueOf(maybe).booleanValue();
        if (maybe == null || maybe.length() < 1) {
            output = true;
        }
        return output;
    }

    protected PublishedService getPublishedService() {
        PublishedService service = null;
        if (subject.getServiceNode() != null) {
            try {
                service = subject.getServiceNode().getPublishedService();
            } catch (FindException e) {
                log.log(Level.SEVERE, "cannot get service", e);
            }
        }
        return service;
    }

    /**
     * validate the service policy and display the result.
     */
    public void validatePolicy() {
        validatePolicy(true);
    }

    protected static final Callable<PolicyValidatorResult> NO_VAL_CALLBACK = new Callable<PolicyValidatorResult>() {
        public PolicyValidatorResult call() throws Exception {
            PolicyValidatorResult output = new PolicyValidatorResult();
            output.addWarning(new PolicyValidatorResult.Warning(0, 0, "Policy validation feedback has " +
                                                                      "been disabled in the Preferences", null));
            return output;
        }
    };

    /**
     * validate the service policy.
     * @param displayResult if true, will call displayResult before returning.  otherwise, just returns the results
     * @return result, or null if it was canceled
     */
    private PolicyValidatorResult validatePolicy(boolean displayResult) {
        final Assertion assertion = rootAssertion.asAssertion();
        final PublishedService service = getPublishedService();
        final ConsoleLicenseManager licenseManager = Registry.getDefault().getLicenseManager();
        Callable<PolicyValidatorResult> callable;
        if (isPolicyValidationOn()) {
            callable = new Callable<PolicyValidatorResult>() {
                public PolicyValidatorResult call() throws Exception {
                    return PolicyValidator.getDefault().validate(assertion, service, licenseManager);
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
            throw new RuntimeException(e);
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

    private JSplitPane getSplitPane() {
        if (splitPane != null) return splitPane;
        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setBorder(null);
        splitPane.add(getPolicyTreePane(), "top");

        final JComponent messagePane = getMessagePane();

        splitPane.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY,
          new PropertyChangeListener() {
              public void propertyChange(PropertyChangeEvent evt) {
                  SsmPreferences prefs = preferences;
                  int l = splitPane.getDividerLocation();
                  prefs.putProperty(MESSAGE_AREA_DIVIDER_KEY, Integer.toString(l));
              }
          });
        messagePane.addHierarchyListener(new HierarchyListener() {
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
        policyTree.setRowHeight((int)(policyTree.getRowHeight() * 1.3));
        policyTreePane = new JScrollPane(policyTree);
        policyTreePane.setBorder(null);
        return policyTreePane;
    }

    public void changeSubjectName(String newName) {
        subjectName = newName;
    }

    /** updates the service name, tab name etc */
    public void updateHeadings() {
        setName(subjectName);
        getSplitPane().setName(subjectName);
    }

    /**
     * Render the service policy into the editor
     * 
     * @param identityView
     */
    private void renderPolicy(boolean identityView)
      {
        updateHeadings();

        TreeModel policyTreeModel;
        final PolicyToolBar pt = topComponents.getPolicyToolBar();


        if (identityView) {
            PolicyTreeModel model;
            try {
                model = PolicyTreeModel.identityModel(subject.getRootAssertion());
            } catch (InterruptedException e) {
                throw new RuntimeException(e); // can't happen here
            }
            policyTreeModel = new FilteredTreeModel((TreeNode)model.getRoot());
            ((FilteredTreeModel)policyTreeModel).setFilter(new PolicyTreeModel.IdentityNodeFilter());

        } else {
            PolicyTreeModel model;
            if (rootAssertion != null) {
                model = PolicyTreeModel.policyModel(rootAssertion.asAssertion());
            } else {
                model = PolicyTreeModel.make(subject.getRootAssertion());
            }
            policyTreeModel = model;
        }

        rootAssertion = (AssertionTreeNode)policyTreeModel.getRoot();
        ((AbstractTreeNode)rootAssertion.getRoot()).addCookie(new AbstractTreeNode.NodeCookie(subject.getServiceNode()));
        //rootAssertion.addCookie(new AbstractTreeNode.NodeCookie(subject.getServiceNode()));

        policyTree.setModel(policyTreeModel);
        if (identityView) {
            pt.unregisterPolicyTree(policyTree);
        } else {
            pt.registerPolicyTree(policyTree);
        }


        policyTreeModel.addTreeModelListener(policyTreeModellistener);
        final TreeNode root = (TreeNode)policyTreeModel.getRoot();
        final TreePath rootPath = new TreePath(((DefaultMutableTreeNode)root).getPath());
        policyTree.expandPath(rootPath);

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (root.getChildCount() > 0) {
                    final TreeNode selNode = root.getChildAt(0);
                    final TreePath path = new TreePath(((DefaultMutableTreeNode)selNode).getPath());
                    policyTree.setSelectionPath(path);
                    policyTree.requestFocusInWindow();
                }
            }
        });
    }

    /**
     * set various listeners that this panel uses
     */
    private void setEditorListeners() {
        subject.addPropertyChangeListener(servicePropertyChangeListener);
        JTree tree = (JTree)topComponents.getComponent(ServicesTree.NAME);
        if (tree == null) {
            throw new IllegalStateException("Internal error - (could not get services tree component)");
        }
        tree.getModel().addTreeModelListener(servicesTreeModelListener);
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
        policyEditorToolbar = new PolicyEditToolBar();
        policyEditorToolbar.setFloatable(false);
        policyEditorToolbar.setBorder(null);
        return policyEditorToolbar;
    }

    /**
     */
    final class PolicyEditToolBar extends JToolBar {
        private JButton buttonSave;
        private JButton buttonSaveTemplate;
        private JButton buttonImport;
        private JButton buttonUDDIImport;
        private JButton buttonValidate;
        private JToggleButton identityViewButton;
        private JToggleButton policyViewButton;

        public PolicyEditToolBar() {
            super();
            this.putClientProperty("JToolBar.isRollover", Boolean.TRUE);
            initComponents();
        }

        private void initComponents() {
            buttonSave = new JButton(getSaveAction());

            this.add(buttonSave);
            final BaseAction ba = (BaseAction)buttonSave.getAction();
            ba.setEnabled(false);
            ba.addActionListener(new ActionListener() {
                /**
                 * Invoked when an action occurs.
                 */
                public void actionPerformed(ActionEvent e) {
                    appendToMessageArea("<i>Policy saved.</i>");
                    ba.setEnabled(false);
                    buttonSave.setEnabled(false);
                }
            });

            buttonValidate = new JButton(getServerValidateAction());
            this.add(buttonValidate);

            buttonSaveTemplate = new JButton(getExportAction());
            this.add(buttonSaveTemplate);

            buttonImport = new JButton(getImportAction());
            this.add(buttonImport);

            if (!TopComponents.getInstance().isApplet()) {
                buttonUDDIImport = new JButton(getUDDIImportAction());
                this.add(buttonUDDIImport);
            }

            policyViewButton = new JToggleButton(new PolicyViewAction());
            policyViewButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    renderPolicyView(false);
                }
            });
            this.add(policyViewButton);
            policyViewButton.setSelected(true);

            identityViewButton = new JToggleButton(new PolicyIdentityViewAction());
            identityViewButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    renderPolicyView(true);
                }
            });
            this.add(identityViewButton);
            ButtonGroup bg = new ButtonGroup();
            bg.add(identityViewButton);
            bg.add(policyViewButton);
            this.add(Box.createHorizontalGlue());
        }

        private void renderPolicyView(boolean identityView) {
            policyTree.getModel().removeTreeModelListener(policyTreeModellistener);
            renderPolicy(identityView);
            validatePolicy();
            policyTree.getModel().addTreeModelListener(policyTreeModellistener);
        }
    }

    public Action getValidateAction() {
        if (validateAction == null) {
            validateAction = new ValidatePolicyAction() {
                protected void performAction() {
                    validatePolicy();
                }
            };
        }
        return validateAction;
    }

    /**
     * @return the policy xml that was validated, or null if validation canceled
     */
    private String fullValidate() {
        final Assertion assertion = rootAssertion.asAssertion();
        final String policyXml = WspWriter.getPolicyXml(assertion);
        final PublishedService service = getPublishedService();
        final ConsoleLicenseManager licenseManager = Registry.getDefault().getLicenseManager();
        Callable<PolicyValidatorResult> callable;
        if (isPolicyValidationOn()) {
            callable = new Callable<PolicyValidatorResult>() {
                public PolicyValidatorResult call() throws Exception {
                    PolicyValidatorResult result = PolicyValidator.getDefault().validate(assertion, service, licenseManager);
                    if (getPublishedService() != null) {
                        PolicyValidatorResult result2 = Registry.getDefault().getServiceManager().
                                validatePolicy(policyXml, getPublishedService().getOid());
                        if (result2.getErrorCount() > 0) {
                            for (Object o : result2.getErrors()) {
                                result.addError((PolicyValidatorResult.Error)o);
                            }
                        }
                        if (result2.getWarningCount() > 0) {
                            for (Object o : result2.getWarnings()) {
                                result.addWarning((PolicyValidatorResult.Warning)o);
                            }
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

        ((DefaultTreeModel)policyTree.getModel()).nodeChanged(rootAssertion);
        return policyXml;
    }

    public Action getServerValidateAction() {
        if (serverValidateAction == null) {
            serverValidateAction = new ValidatePolicyAction() {
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
        for (Enumeration en = rootAssertion.preorderEnumeration(); en.hasMoreElements();) {
            AssertionTreeNode an = (AssertionTreeNode)en.nextElement();
            an.setValidatorMessages(r.messages(an.asAssertion()));
        }
        if (!validating) {
               ((DefaultTreeModel)policyTree.getModel()).nodeChanged(rootAssertion);
           }
           overWriteMessageArea("");
        for (Iterator iterator = r.getErrors().iterator();
             iterator.hasNext();) {
            PolicyValidatorResult.Error pe =
              (PolicyValidatorResult.Error)iterator.next();
            appendToMessageArea(getValidateMessageIntro(pe)
              +
              "</a>" + " Error: " + pe.getMessage() + "");
        }
        for (Iterator iterator = r.getWarnings().iterator();
             iterator.hasNext();) {
            PolicyValidatorResult.Warning pe =
              (PolicyValidatorResult.Warning)iterator.next();
            appendToMessageArea(getValidateMessageIntro(pe)
              +
              " Warning: " + pe.getMessage() + "");
        }
        if (r.getErrors().isEmpty() && r.getWarnings().isEmpty()) {
            appendToMessageArea("<i>Policy validated ok.</i>");
        }
    }

    public void updateActions(Action[] actions) {
        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            if (action instanceof SavePolicyAction) {
                actions[i] = policyEditorToolbar.buttonSave.getAction();
                actions[i].setEnabled(policyEditorToolbar.buttonSave.isEnabled());
            } else if (action instanceof ValidatePolicyAction) {
                actions[i] = policyEditorToolbar.buttonValidate.getAction();
                actions[i].setEnabled(policyEditorToolbar.buttonValidate.isEnabled());
            }
        }
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
     * @param pe 
     * @return the string as a message
     */
    private String getValidateMessageIntro(PolicyValidatorResult.Message pe) {
        String msg = null;
        Assertion assertion = rootAssertion.asAssertion().getAssertionWithOrdinal(pe.getAssertionOrdinal());
        if (assertion != null) {
            msg = "Assertion: " +
              "<a href=\"file://assertion#" +
              assertion.hashCode() + "\">" +
              Descriptions.getDescription(assertion).getShortDescription() + "</a>";
        } else {
            msg = ""; // supplied message (non single assertion related)
        }
        return msg;
    }


    // listener for policy tree changes
    TreeModelListener policyTreeModellistener = new TreeModelListener() {
        public void treeNodesChanged(TreeModelEvent e) {
            if (!(e instanceof PolicyTreeModelEvent)) return; // do not validate this
            if (!initialValidate) {
                enableButtonSave();
                SwingUtilities.invokeLater(new Runnable() {
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

        public void treeNodesInserted(final TreeModelEvent e) {
            enableButtonSave();
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            enableButtonSave();
        }

        public void treeStructureChanged(TreeModelEvent e) {
            enableButtonSave();
        }

        private void enableButtonSave() {
            overWriteMessageArea("");
            policyEditorToolbar.buttonSave.getAction().setEnabled(true);
        }
    };


// listener for policy tree changes
    TreeModelListener servicesTreeModelListener = new TreeModelListener() {
        public void treeNodesChanged(TreeModelEvent e) {
        }

        public void treeNodesInserted(TreeModelEvent e) {
        }

        public void treeNodesRemoved(TreeModelEvent e) {
            Object[] children = e.getChildren();
            for (int i = 0; i < children.length; i++) {
                Object child = children[i];
                if (child == subject.getServiceNode()) {
                    log.fine("Service node deleted, disabling save controls");
                    policyEditorToolbar.buttonSave.setEnabled(false);
                    policyEditorToolbar.buttonSave.getAction().setEnabled(false);
                }
            }
        }

        public void treeStructureChanged(TreeModelEvent e) {
        }

    };


    private final PropertyChangeListener
      servicePropertyChangeListener = new PropertyChangeListener() {
          /**
           * This method gets called when a bound property is changed.
           * 
           * @param evt A PropertyChangeEvent object describing the event source
           *            and the property that has changed.
           */
          public void propertyChange(PropertyChangeEvent evt) {
              log.info(evt.getPropertyName() + "changed");
              if (SERVICENAME_PROPERTY.equals(evt.getPropertyName())) {
                  updateHeadings();
                  renderPolicy(false);
              } else if ("policy".equals(evt.getPropertyName())) {
                  rootAssertion = null;
                  renderPolicy(false);
                  policyEditorToolbar.buttonSave.setEnabled(true);
                  policyEditorToolbar.buttonSave.getAction().setEnabled(true);
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
          public void hyperlinkUpdate(HyperlinkEvent e) {
              if (HyperlinkEvent.EventType.ACTIVATED != e.getEventType())
                  return;
              URI uri = URI.create(e.getURL().toString());
              String f = uri.getFragment();
              if (f == null) return;
              try {
                  int hashcode = Integer.parseInt(f);
                  for (Enumeration en = rootAssertion.preorderEnumeration();
                       en.hasMoreElements();) {
                      AssertionTreeNode an = (AssertionTreeNode)en.nextElement();
                      if (an.asAssertion().hashCode() == hashcode) {
                          TreePath p = new TreePath(an.getPath());
                          if (!policyTree.hasBeenExpanded(p) || !policyTree.isExpanded(p)) {
                              policyTree.expandPath(p);
                          }
                          policyTree.setSelectionPath(p);
                      }
                  }
              } catch (NumberFormatException ex) {
                  ex.printStackTrace();
              }
          }
      };

    /**
     * Invoked when a component has been added to the container.
     */
    public void componentAdded(ContainerEvent e) {
    }

    /**
     * Invoked when a component has been removed from the container.
     */
    public void componentRemoved(ContainerEvent e) {
        log.fine("Resetting the policy editor panel");
        subject.removePropertyChangeListener(servicePropertyChangeListener);
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
    public void componentWillRemove(ContainerEvent e)
      throws ContainerVetoException {
        if (e.getChild() == this) {
            if (policyEditorToolbar.buttonSave.isEnabled()) {
                if (!TopComponents.getInstance().isConnectionLost()) {
                    int answer = (JOptionPane.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                      "<html><center><b>Do you want to save changes to service policy " +
                      "for<br> '" + subjectName + "' ?</b></center></html>",
                      "Save Service policy",
                      JOptionPane.YES_NO_CANCEL_OPTION));
                    if (answer == JOptionPane.YES_OPTION) {
                        policyEditorToolbar.buttonSave.getAction().actionPerformed(null);
                    } else if ((answer == JOptionPane.CANCEL_OPTION)) {
                        throw new ContainerVetoException(e, "User aborted");
                    }
                } else {
                    int answer = (JOptionPane.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                      "<html><center><b>Connection Lost. Do you want to save changes to service policy " +
                      "file?</b></center></html>",
                      "Save Service policy",
                      JOptionPane.YES_NO_CANCEL_OPTION));
                    if (answer == JOptionPane.YES_OPTION) {
                        getSimpleExportAction().actionPerformed(null);
                    }
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

        Set errors = new LinkedHashSet();
        errors.addAll(result.getErrors());
        for (Iterator iterator = errors.iterator(); iterator.hasNext();) {
            pr.addError((PolicyValidatorResult.Error)iterator.next());
        }
        Set warnings = new LinkedHashSet();
        warnings.addAll(result.getWarnings());

        for (Iterator iterator = warnings.iterator(); iterator.hasNext();) {
            pr.addWarning((PolicyValidatorResult.Warning)iterator.next());

        }
        return pr;
    }

    private String getHomePath() {
        return TopComponents.getInstance().isApplet() ? null :  preferences.getHomePath();
    }

    public Action getSaveAction() {
        if (saveAction == null) {
            if (subject.getServiceNode() == null) {
                saveAction = new ExportPolicyToFileAction(getHomePath()) {
                    public String getName() {
                        return "Save Policy";
                    }
                    public String getDescription() {
                        return "Save the policy to a file along with external references.";
                    }
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
            } else {
                try {
                    saveAction = new SavePolicyAction() {
                        protected void performAction() {
                            // fla, bugzilla 1094. all saves are now preceeded by a validation action
                            if (!validating) {
                                try {
                                    validating = true;
                                    String xml = fullValidate();
                                    if (xml != null) {
                                        this.node = rootAssertion;
                                        super.performAction(xml);
                                    }
                                } finally {
                                    validating = false;
                                }
                            }
                        }
                    };
                } catch (Exception e) {
                    throw new RuntimeException("Couldn't get current PublishedService", e);
                }
            }
        }
        return saveAction;
    }

    public Action getExportAction() {
        if (exportPolicyAction == null) {
            exportPolicyAction = new ExportPolicyToFileAction(getHomePath()) {
                protected void performAction() {
                    Assertion assertion = rootAssertion.asAssertion();
                    exportPolicy(getName(), assertion);
                }
            };
        }
        return exportPolicyAction;
    }

    public Action getSimpleExportAction() {
        if (simpleExportPolicyAction == null) {
            simpleExportPolicyAction = new ExportPolicyToFileAction(getHomePath()) {
                protected void performAction() {
                    Assertion assertion = rootAssertion.asAssertion();
                    exportPolicy(getName(), assertion);
                }
                protected void serializeToFile(Assertion rootAssertion, File policyFile) throws IOException, SAXException {
                    // do policy to xml
                    Document policydoc = XmlUtil.stringToDocument(WspWriter.getPolicyXml(rootAssertion));
                    // write xml to file
                    FileOutputStream fos = new FileOutputStream(policyFile);
                    XmlUtil.nodeToFormattedOutputStream(policydoc, fos);
                    fos.flush();
                }
            };
        }
        return simpleExportPolicyAction;
    }

    public Action getUDDIImportAction() {
        return new SecureAction(null, SecureAction.LIC_AUTH_ASSERTIONS) {
            @Override
            public boolean isAuthorized() {
                PublishedService svc = getPublishedService();
                return svc != null && canAttemptOperation(new AttemptedUpdate(EntityType.SERVICE, svc)) && enableUddi;
            }

            public String getName() {
                return "Import From UDDI";
            }

            protected String iconResource() {
                return "com/l7tech/console/resources/server16.gif";
            }

            protected void performAction() {
                ImportPolicyFromUDDIWizard wiz = ImportPolicyFromUDDIWizard.getInstance(TopComponents.getInstance().getTopParent());
                wiz.pack();
                Utilities.centerOnScreen(wiz);
                wiz.setVisible(true);
                if (wiz.importedPolicy() != null) {
                    getPublishedService().setPolicyXml(wiz.importedPolicy());
                    rootAssertion = null;
                    renderPolicy(false);
                    policyEditorToolbar.buttonSave.setEnabled(true);
                    policyEditorToolbar.buttonSave.getAction().setEnabled(true);
                    validatePolicy();
                }
            }
        };
    }

    public Action getImportAction() {
        if (importPolicyAction == null) {
            if (subject.getServiceNode() != null) {
                importPolicyAction = new ImportPolicyFromFileAction(getHomePath()) {

                    protected OperationType getOperation() {
                        return OperationType.UPDATE;
                    }

                    protected void performAction() {
                        try {
                            PublishedService service = subject.getServiceNode().getPublishedService();
                            if (importPolicy(service)) {
                                rootAssertion = null;
                                renderPolicy(false);
                                policyEditorToolbar.buttonSave.setEnabled(true);
                                policyEditorToolbar.buttonSave.getAction().setEnabled(true);
                                validatePolicy();
                            }
                        } catch (FindException e) {
                            log.log(Level.SEVERE, "cannot get back service", e);
                        }
                    }
                };
            } else {
                // TODO import in not connected mode
            }
        }
        return importPolicyAction;
    }
}
