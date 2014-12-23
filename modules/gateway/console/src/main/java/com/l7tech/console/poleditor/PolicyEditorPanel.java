package com.l7tech.console.poleditor;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.MainWindow;
import com.l7tech.console.action.*;
import com.l7tech.console.event.ContainerVetoException;
import com.l7tech.console.event.VetoableContainerListener;
import com.l7tech.console.panels.CancelableOperationDialog;
import com.l7tech.console.panels.ImportPolicyFromUDDIWizard;
import com.l7tech.console.panels.InformationDialog;
import com.l7tech.console.tree.*;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.*;
import com.l7tech.gateway.common.AsyncAdminMethods;
import com.l7tech.gateway.common.admin.PolicyAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gui.util.*;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.OrganizationHeader;
import com.l7tech.policy.*;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.validator.PolicyValidationContext;
import com.l7tech.policy.wsp.*;
import com.l7tech.util.*;
import com.l7tech.wsdl.SerializableWSDLLocator;
import com.l7tech.xml.NamespaceMigratable;
import com.l7tech.xml.soap.SoapVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.*;
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
import java.lang.reflect.Method;
import java.net.URI;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class PolicyEditorPanel extends JPanel implements VetoableContainerListener {
    private static final Logger log = Logger.getLogger(PolicyEditorPanel.class.getName());
    private static final SsmPreferences preferences = TopComponents.getInstance().getPreferences();

    public static final String POLICYNAME_PROPERTY = "policy.name";
    public static final String TAB_TITLE_CHANGE_PROPERTY = "tabTitle.change";
    @Deprecated // used in pre-Icefish
    public static final String SHOW_COMMENTS = "COMMENTS.SHOWSTATE";
    @Deprecated // used in pre-Icefish
    private static final String PREF_PREFIX = "policy.editor.";
    @Deprecated // used in pre-Icefish
    private static final String SHOW_ASSERTION_NUMBERS = PREF_PREFIX + "showAssertionNumbers";
    @Deprecated // used in pre-Icefish
    private static final String MESSAGE_AREA_DIVIDER_KEY =  PREF_PREFIX + JSplitPane.DIVIDER_LOCATION_PROPERTY;

    private static final long TIME_BEFORE_OFFERING_CANCEL_DIALOG = 500L;

    private static final String POLICY_TAB_PROPERTY_PREFIX = "policy.tabs.";
    private static final String ASSERTION_PROPERTY_PREFIX = "assertion.";
    private static final String SEARCH_PROPERTY_PREFIX = "search.";

    private static final String POLICY_TAB_PROPERTY_ASSERTION_SHOW_NUMBERS = POLICY_TAB_PROPERTY_PREFIX + ASSERTION_PROPERTY_PREFIX + "showNumbers";
    public static final String POLICY_TAB_PROPERTY_ASSERTION_SHOW_COMMENTS = POLICY_TAB_PROPERTY_PREFIX + ASSERTION_PROPERTY_PREFIX + "showComments";
    public static final String POLICY_TAB_PROPERTY_SEARCH_SHOW = POLICY_TAB_PROPERTY_PREFIX + SEARCH_PROPERTY_PREFIX + "show";
    public static final String POLICY_TAB_PROPERTY_SEARCH_CASE_SENSITIVE = POLICY_TAB_PROPERTY_PREFIX + SEARCH_PROPERTY_PREFIX + "caseSensitive";
    public static final String POLICY_TAB_PROPERTY_SEARCH_SHOW_DISABLED = POLICY_TAB_PROPERTY_PREFIX + SEARCH_PROPERTY_PREFIX + "showDisabled";
    public static final String POLICY_TAB_PROPERTY_SEARCH_INCLUDE_PROPERTIES = POLICY_TAB_PROPERTY_PREFIX + SEARCH_PROPERTY_PREFIX + "includeProperties";
    public static final String POLICY_TAB_PROPERTY_MESSAGE_AREA_DIVIDER_LOCATION = POLICY_TAB_PROPERTY_PREFIX + "policy.editor." + JSplitPane.DIVIDER_LOCATION_PROPERTY;

    public static final String[] ALL_POLICY_TAB_PROPERTIES = new String[] {
        POLICY_TAB_PROPERTY_ASSERTION_SHOW_NUMBERS,
        POLICY_TAB_PROPERTY_ASSERTION_SHOW_COMMENTS,
        POLICY_TAB_PROPERTY_SEARCH_SHOW,
        POLICY_TAB_PROPERTY_SEARCH_CASE_SENSITIVE,
        POLICY_TAB_PROPERTY_SEARCH_SHOW_DISABLED,
        POLICY_TAB_PROPERTY_SEARCH_INCLUDE_PROPERTIES,
        POLICY_TAB_PROPERTY_MESSAGE_AREA_DIVIDER_LOCATION
    };

    private static final String PROP_PREFIX = "com.l7tech.console";
    private static final long DELAY_INITIAL = ConfigFactory.getLongProperty( PROP_PREFIX + ".policyValidator.serverSideDelay.initial", 71L );
    private static final long DELAY_CAP = ConfigFactory.getLongProperty( PROP_PREFIX + ".policyValidator.serverSideDelay.maximum", 15000L );
    private static final double DELAY_MULTIPLIER = SyspropUtil.getDouble(PROP_PREFIX + ".policyValidator.serverSideDelay.multiplier", 1.6);

    private JTextPane messagesTextPane;
    private AssertionTreeNode rootAssertion;
    private PolicyTree policyTree;
    private PolicyEditToolBar policyEditorToolbar;
    private JSplitPane splitPane;
    private final TopComponents topComponents = TopComponents.getInstance();
    private NumberedPolicyTreePane policyTreePane;
    private boolean messageAreaVisible = false;
    private JTabbedPane messagesTab;
    private boolean validating = false;
    private SecureAction saveAndActivateAction;
    private SecureAction saveOnlyAction;
    private SecureAction showAssertionLineNumbersAction;
    private List<AbstractButton> showCmtsButtons = new ArrayList<AbstractButton>();
    private List<AbstractButton> showLnNumsButtons = new ArrayList<AbstractButton>();
    private ValidatePolicyAction validateAction;
    private ValidatePolicyAction serverValidateAction;
    private ExportPolicyToFileAction exportPolicyAction;
    private ExportPolicyToFileAction simpleExportPolicyAction;
    private ImportPolicyFromFileAction importPolicyAction;
    private PolicyEditorSubject subject;
    private String subjectName;
    private final boolean enableUddi;
    private final PolicyValidator policyValidator;
    private final PolicyAdmin policyAdmin;
    private final Map<String, String> registeredCustomAssertionFeatureSets = new HashMap<>();
    private Long overrideVersionNumber = null;
    private Boolean overrideVersionActive = null;
    private SearchForm searchForm;
    private SecureAction hideShowCommentsAction;
    private MigrateNamespacesAction migrateNamespacesAction;

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
    public PolicyEditorPanel(PolicyEditorSubject subject, PolicyTree pt, boolean enableUddi) {
        if (subject == null || pt == null) {
            throw new IllegalArgumentException();
        }
        this.enableUddi = enableUddi;
        this.subject = subject;
        this.subjectName = subject.getName();
        this.policyTree = pt;
        this.policyTree.setWriteAccess(subject.hasWriteAccess());
        this.policyValidator = Registry.getDefault().getPolicyValidator();
        this.policyAdmin = Registry.getDefault().getPolicyAdmin();

        for (CustomAssertionHolder customAssertionHolder : TopComponents.getInstance().getAssertionRegistry().getCustomAssertions()) {
            registeredCustomAssertionFeatureSets.put(customAssertionHolder.getCustomAssertion().getClass().getName(), customAssertionHolder.getRegisteredCustomFeatureSetName());
        }

        layoutComponents();

        renderPolicy();
        setEditorListeners();
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
     * Get the GOID of the PublishedService whose policy is open for editing, if this panel is editing the
     * policy of a PublishedService.
     *
     * @return the GOID of the PublishedService whose policy is open for editing, or null if it's not a PublishedService policy.
     */
    public Goid getPublishedServiceGoid() {
        final PublishedService ps = getPublishedService();
        return ps == null ? null : ps.getGoid();
    }

    /**
     * Get the GOID of the Policy that is open for editing in this editor panel.
     *
     * @return the policy GOID.  Never null.
     */
    public Goid getPolicyGoid() {
        try {
            return getPolicyNode().getPolicy().getGoid();
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
            output.addWarning(new PolicyValidatorResult.Warning( Collections.<Integer>emptyList(), -1,
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
        final SerializableWSDLLocator wsdlLocator;
        final SoapVersion soapVersion;
        try {
            final PublishedService service = getPublishedService();
            if (service == null) {
                wsdlLocator = null;
                soap = getPolicyNode().getPolicy().isSoap();
                soapVersion = null;
            } else {
                wsdlLocator = service.wsdlLocator();
                soap = service.isSoap();
                soapVersion = service.getSoapVersion();
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
                    	r.addError(new PolicyValidatorResult.Error(Collections.<Integer>emptyList(), -1, "Policy could not be loaded", null));
                    	return r;
                	}
                    final PolicyValidationContext pvc = new PolicyValidationContext(policy.getType(), policy.getInternalTag(), policy.getInternalSubTag(), wsdlLocator, soap, soapVersion);
                    pvc.setPermittedAssertionClasses(new HashSet<>(TopComponents.getInstance().getAssertionRegistry().getPermittedAssertionClasses()));
                    pvc.getRegisteredCustomAssertionFeatureSets().putAll(registeredCustomAssertionFeatureSets);
                    PolicyValidatorResult r = policyValidator.validate(assertion, pvc, licenseManager);
                    policyValidator.checkForCircularIncludes(policy.getGuid(), policy.getName(), assertion, r);
                    return r;
                }
            };
        } else {
            callable = NO_VAL_CALLBACK;
        }
        return validateAndDisplay(callable, null, displayResult);
    }

    private PolicyValidatorResult validateAndDisplay(Callable<PolicyValidatorResult> callable, Set<PolicyValidatorResult.Warning> extraWarnings, boolean displayResult) {
        final JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        final CancelableOperationDialog cancelDlg =
                new CancelableOperationDialog(topComponents.getTopParent(), "Validating", "        Validating policy...        ", bar);

        final PolicyValidatorResult result;
        try {
            result = Utilities.doWithDelayedCancelDialog(callable, cancelDlg, TIME_BEFORE_OFFERING_CANCEL_DIALOG);
            if (result != null && displayResult)
                displayPolicyValidateResult(mergeExtrasAndPruneDuplicates(result, extraWarnings));
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

        searchForm = new SearchForm(this);

        final MouseListener listener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                searchForm.hidePanel();
            }
        };

        searchForm.addCloseLabelListener(listener);
        boolean showSearch = Boolean.parseBoolean(getTabSettingFromPolicyTabProperty(POLICY_TAB_PROPERTY_SEARCH_SHOW,
            SearchForm.SHOW, "true", getPolicyGoid(), getVersionNumber()));
        if (!showSearch) {
            searchForm.hidePanel();
        }
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
                    if (e.getKeyCode() == KeyEvent.VK_F && e.getModifiers() == KeyEvent.CTRL_MASK) {
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
                    } else if (e.getKeyCode() == KeyEvent.VK_G && e.getModifiers() == KeyEvent.CTRL_MASK) {
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
                  int dividerLocation = splitPane.getDividerLocation();
                  updatePolicyTabProperty(POLICY_TAB_PROPERTY_MESSAGE_AREA_DIVIDER_LOCATION, Integer.toString(dividerLocation));
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
                    String s = getTabSettingFromPolicyTabProperty(POLICY_TAB_PROPERTY_MESSAGE_AREA_DIVIDER_LOCATION,
                        MESSAGE_AREA_DIVIDER_KEY, null, getPolicyGoid(), getVersionNumber());
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
        policyTree.getSelectionModel().addTreeSelectionListener(ClipboardActions.getTreeUpdateListener());
        policyTreePane = new NumberedPolicyTreePane(policyTree);
        policyTreePane.setNumbersVisible(Boolean.parseBoolean(getTabSettingFromPolicyTabProperty(POLICY_TAB_PROPERTY_ASSERTION_SHOW_NUMBERS,
            SHOW_ASSERTION_NUMBERS, "false", getPolicyGoid(), getVersionNumber())));
        return policyTreePane;
    }

    public void changeSubjectName(String newName) {
        subjectName = newName;
    }

    public long getVersionNumber() {
        if (overrideVersionNumber != null) return overrideVersionNumber;
        return subject.getVersionNumber();
    }

    @Nullable
    private Long getLatestVersionNumber() {
        final Goid policyGoid;
        try {
            policyGoid = subject.getPolicyNode().getPolicy().getGoid();
        } catch (final FindException e) {
            throw new RuntimeException(e);
        }
        try {
            final PolicyVersion latest = policyAdmin.findLatestRevisionForPolicy( policyGoid );
            return latest.getOrdinal();
        } catch ( RuntimeException e ) {
            log.log( Level.WARNING, "Unable to look up latest policy revision number: " + ExceptionUtils.getMessage( e ), ExceptionUtils.getDebugException( e ) );
            return null;
        }
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
        return PolicyRevisionUtils.getDisplayName(subjectName, getVersionNumber(), getLatestVersionNumber(), isVersionActive());
    }

    /** updates the policy name, tab name etc */
    public void updateHeadings() {
        String displayName = getDisplayName();
        setName(displayName);  // Keep this line since the component name will be used in TabTitleComponentPanel to initialize the name of a tabTitleLabel.
        topComponents.getCurrentWorkspace().updateTabTitle(this, displayName);
    }

    public void updateAssertions( final AssertionTreeNode atn ) {
        PolicyTreeUtils.updateAssertions(atn, new HashMap<Goid, String>());
    }

    public AssertionTreeNode getCurrentRoot() {
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
     * Refresh the policy editor panel including the policy xml.
     * Note: (1) This method is very similar with the above renderPolicy() method.
     *       (2) If there are unsaved changes, save these changes first, then reload
     *       the original policy version instead of the saved policy version.
     *
     * @throws FindException thrown when policy version cannot be found.
     * @throws IOException thrown when WspReader cannot parse policy xml.
     */
    public void refreshPolicyEditorPanel() throws FindException, IOException {
        // Before refresh, preserve version number and active status
        long versionNum = getVersionNumber();
        boolean isActive = isVersionActive();

        // Before refresh, check if the policy editor panel has any unsaved changes and save the unsaved changes.
        try {
            saveUnsavedPolicyChanges(null);
            policyEditorToolbar.setSaveButtonsEnabled(false);
            TopComponents.getInstance().firePolicyEditDone();
        } catch (ContainerVetoException e) {
            // Won't happen here at this case!
        }

        // Get the refreshed policy assertion
        final PolicyVersion fullPolicyVersion = Registry.getDefault().getPolicyAdmin().findPolicyVersionForPolicy(getPolicyGoid(), versionNum);
        final Assertion newPolicy = WspReader.getDefault().parsePermissively(fullPolicyVersion.getXml(), WspReader.INCLUDE_DISABLED);

        // Get the refresh entity node (ServiceNode or PolicyNode) in the ServicesAndPoliciesTree, associated with the policy editor panel.
        final String entityNodeGoidString = ((OrganizationHeader) getPolicyNode().getUserObject()).getStrId();
        final RootNode rootNode = ((ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME)).getRootNode();
        final EntityWithPolicyNode refreshedEntityNode = (EntityWithPolicyNode) rootNode.getNodeForEntity(Goid.parseGoid(entityNodeGoidString));
        refreshedEntityNode.addPropertyChangeListener(policyPropertyChangeListener); // Since the policy entity node is changed after refresh

        // Create a new policy tree mode based on a new policy assertion
        PolicyTreeModel policyTreeModel = PolicyTreeModel.policyModel(newPolicy);
        policyTreeModel.addTreeModelListener(policyTreeModellistener);               // Since the policy tree model is changed after refresh
        policyTreeModel.addTreeModelListener(treeModelListener);                     // Since the policy tree model is changed after refresh
        policyTree.setModel(policyTreeModel);

        AssertionTreeNode newRootAssertion = (AssertionTreeNode)policyTreeModel.getRoot();
        rootAssertion = newRootAssertion;
        ((AbstractTreeNode) newRootAssertion.getRoot()).addCookie(new AbstractTreeNode.NodeCookie(refreshedEntityNode));
        updateAssertions(newRootAssertion);

        // Restore version number and active status
        overrideVersionNumber = versionNum;
        overrideVersionActive = isActive;
        updateHeadings();
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
        private ClearMessageAreaAction() {
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
    public PolicyEditToolBar getToolBar() {
        if (policyEditorToolbar != null) return policyEditorToolbar;
        policyEditorToolbar = new PolicyEditToolBar(enableUddi);
        policyEditorToolbar.setFloatable(false);
        policyEditorToolbar.setBorder(null);
        return policyEditorToolbar;
    }

    /**
     */
    public final class PolicyEditToolBar extends JToolBar {
        private JButton buttonSaveAndActivate;
        private JButton buttonSaveOnly;
        private JButton buttonValidate;
        private final boolean enableUddi;

        PolicyEditToolBar(boolean enableUddi) {
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
                    validateRoot();
                    appendToMessageArea("<i>Policy saved and made active.</i>");
                    ba.setEnabled(false);
                    buttonSaveAndActivate.setEnabled(false);
                    bsaa.setEnabled(false);
                    buttonSaveOnly.setEnabled(false);

                    // Update the tab title by adding an asterisk if unsaved changes occur.
                    PolicyEditorPanel.this.updateHeadings();

                    // Copy all policy tab properties for a new policy version
                    copyAllPolicyTabPropertiesBasedOnUI();

                    // If a policy is saved and activated, then update all other opened policies containing the policy fragment.
                    final EntityWithPolicyNode entityWithPolicyNode = PolicyEditorPanel.this.getPolicyNode();
                    if (entityWithPolicyNode instanceof PolicyEntityNode) {
                        try {
                            TopComponents.getInstance().getCurrentWorkspace().refreshPoliciesContainingIncludedFragment(entityWithPolicyNode.getPolicy().getGuid());
                        } catch (FindException e1) {
                            DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), "Refresh Error",
                                "Cannot find the policy, " + entityWithPolicyNode.getName(), null);
                        }
                    }
                }
            });

            add(buttonSaveOnly);
            bsaa.setEnabled(false);
            bsaa.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    validateRoot();
                    appendToMessageArea("<i>Policy saved but not activated.</i>");
                    bsaa.setEnabled(false);
                    buttonSaveOnly.setEnabled(false);

                    // Update the tab title by adding an asterisk if unsaved changes occur.
                    PolicyEditorPanel.this.updateHeadings();

                    // Copy all policy tab properties for a new policy version
                    copyAllPolicyTabPropertiesBasedOnUI();
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

        public void setSaveButtonsEnabled(boolean enabled) {
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
        validateAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.ALT_MASK | ActionEvent.CTRL_MASK));
        return validateAction;
    }


    /**
     * @return the policy xml that was validated, or null if validation canceled
     */
    private String fullValidate() {
        final Assertion assertion = getCurrentRoot().asAssertion();
        final String policyXml;
        final SerializableWSDLLocator wsdlLocator;
        final boolean soap;
        final SoapVersion soapVersion;
        final PolicyType type;
        final String internalTag;
        final String internalSubTag;
        final PublishedService service;
        final HashMap<String, Policy> includedFragments = new HashMap<String, Policy>();
        final Set<PolicyValidatorResult.Warning> extraWarnings = new LinkedHashSet<PolicyValidatorResult.Warning>();
        try {
            WspWriter wspWriter = new WspWriter() {
                @Override
                protected void freezeBeanProperty(TypeMapping tm, TypedReference tr, Element element, Method getter, Object targetObject) {
                    super.freezeBeanProperty(tm, tr, element, getter, targetObject);
                    WspSensitive sensitive = getter.getAnnotation(WspSensitive.class);
                    if (sensitive != null) {
                        Object target = tr.target;
                        if (target instanceof CharSequence) {
                            final CharSequence cs = (CharSequence) target;
                            if (cs.length() > 0 && !PasswordGuiUtils.SECPASS_VAR_PATTERN.matcher(cs).matches()) {
                                String getterName = getter.getName();
                                if (getterName.startsWith("get") && getterName.length() > 3)
                                    getterName = getterName.substring(3);
                                if (targetObject instanceof Assertion) {
                                    Assertion assertion = (Assertion) targetObject;
                                    extraWarnings.add(new PolicyValidatorResult.Warning(assertion, "Sensitive field " + getterName + " being saved as plaintext.  Consider writing as ${secpass.*.plaintext} reference instead", null));
                                } else {
                                    log.log(Level.WARNING, "Sensitive data appears to be serialized to policy XML without using a ${secpass.*.plaintext} reference for field " + getterName + " of instance of non-Assertion type " + targetObject.getClass().getName());
                                }
                            }
                        }
                    }
                }
            };
            wspWriter.setPolicy(assertion);
            policyXml = wspWriter.getPolicyXmlAsString();

            service = getPublishedService();
            if (service == null) {
                wsdlLocator = null;
                soap = getPolicyNode().getPolicy().isSoap();
                soapVersion = null;
                type = getPolicyNode().getPolicy().getType();
                internalTag = getPolicyNode().getPolicy().getInternalTag();
                internalSubTag = getPolicyNode().getPolicy().getInternalSubTag();
            } else {
                wsdlLocator = service.wsdlLocator();
                soap = service.isSoap();
                soapVersion = service.getSoapVersion();
                type = PolicyType.PRIVATE_SERVICE;
                internalTag = null;
                internalSubTag = null;
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
                    final PolicyValidationContext pvc = new PolicyValidationContext(type, internalTag, internalSubTag, wsdlLocator, soap, soapVersion);
                    pvc.setPermittedAssertionClasses(new HashSet<>(TopComponents.getInstance().getAssertionRegistry().getPermittedAssertionClasses()));
                    pvc.getRegisteredCustomAssertionFeatureSets().putAll(registeredCustomAssertionFeatureSets);
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

        PolicyValidatorResult result = validateAndDisplay(callable, extraWarnings, true);
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
        ((DefaultTreeModel)policyTree.getModel()).nodeChanged(getCurrentRoot());
        overWriteMessageArea("");
        if (rootAssertion != null && containDisabledAssertion(rootAssertion.asAssertion())) {
            appendToMessageArea("<i>Some assertion(s) disabled.</i>");
        }
        for (PolicyValidatorResult.Error pe : sort(r.getErrors())) {
            appendToMessageArea(MessageFormat.format("{0}</a> Error: {1}{2}", getValidateMessageIntro(pe), pe.getMessage(), makeFixitLink(pe)));
        }
        for (PolicyValidatorResult.Warning pw : sort(r.getWarnings())) {
            appendToMessageArea(MessageFormat.format("{0} Warning: {1}{2}", getValidateMessageIntro(pw), pw.getMessage(), makeFixitLink(pw)));
        }
        if (r.getErrors().isEmpty() && r.getWarnings().isEmpty()) {
            appendToMessageArea("<i>Policy validated ok.</i>");
        }
    }

    private <MT extends PolicyValidatorResult.Message> List<MT> sort( final List<MT> messages ) {
        final List<MT> sortedMessages = new ArrayList<MT>();
        sortedMessages.addAll( messages );
        Collections.sort( sortedMessages, new Comparator<PolicyValidatorResult.Message>(){
            @Override
            public int compare( final PolicyValidatorResult.Message message1,
                                final PolicyValidatorResult.Message message2 ) {
                final List<Integer> path1 = message1.getAssertionIndexPath();
                final List<Integer> path2 = message2.getAssertionIndexPath();

                int result = 0;
                int index = 0;
                while ( result == 0 && index < path1.size() && index < path2.size() ) {
                    result = path1.get(index).compareTo( path2.get(index) );
                    index++;
                }
                if ( result == 0 ) {
                    result = Integer.valueOf( path2.size() ).compareTo( path1.size() );
                }
                if ( result == 0 && message1.getMessage()!=null && message2.getMessage()!=null) {
                    result = String.CASE_INSENSITIVE_ORDER.compare( message1.getMessage(), message2.getMessage() );
                }
                return result;
            }
        } );
        return sortedMessages;
    }

    private String makeFixitLink(PolicyValidatorResult.Message message) {
        if (message.getRemedialActionClassname() == null)
            return "";

        return MessageFormat.format( " <a href=\"file://fixit/{1}?assertion=assertion#{0}\">Fix It</a>",
                pathString(message.getAssertionOrdinal(), message.getAssertionIndexPath()),
                message.getRemedialActionClassname());
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

    public Action[] getActions (){
        if(policyTree.getSelectionCount() == 0){
            return new Action[0];
        }
        else if(policyTree.getSelectionCount() == 1){
            return ((AssertionTreeNode)policyTree.getLastSelectedPathComponent()).getActions();
        }
        else{
            return getActionsMultipleNodes();
        }
    }

    private class ActionWrapped{
        private Action action;
        ActionWrapped (Action action) {
            this.action = action;
        }

        @Override
        public boolean equals(Object obj){
            if(obj.getClass() != ActionWrapped.class)
                return false;
            return ((ActionWrapped)(obj)).action.getClass().getName().equals(this.action.getClass().getName());
        }
    }

    public Action[] getActionsMultipleNodes() {
        // merge actions
        final TreePath[] paths = policyTree.getSelectionPaths();
        final AssertionTreeNode[] assertionTreeNodes = toAssertionTreeNodeArray(paths);
        List <ActionWrapped> mergedActions = new ArrayList<ActionWrapped>();
        Action[] firstActions = assertionTreeNodes[0].getActions();
        for(Action action: firstActions ){
            mergedActions.add(new ActionWrapped(action));
        }


        for(int i = 1; i < assertionTreeNodes.length; ++i){
            List<ActionWrapped> prevActions = new ArrayList<ActionWrapped>(mergedActions);
            Action[] actions = assertionTreeNodes[i].getActions();
            for(Action action : actions){
                ActionWrapped wrapped = new ActionWrapped(action);
                if( !mergedActions.contains(wrapped))
                    mergedActions.remove(wrapped);
                else if (action instanceof BaseAction &&!((BaseAction) action).supportMultipleSelection())
                    mergedActions.remove(wrapped);                    
                else
                    prevActions.remove(wrapped);
            }
            mergedActions.removeAll(prevActions);
        }

        List <Action> actionList = new ArrayList <Action>();

        for (ActionWrapped wrapped : mergedActions) {
            Action action = wrapped.action;
            if (action instanceof SavePolicyAction) {
                if (((SavePolicyAction)action).isActivateAsWell()) {
                    action = policyEditorToolbar.buttonSaveAndActivate.getAction();
                    action.setEnabled(policyEditorToolbar.buttonSaveAndActivate.isEnabled());
                } else {
                    action = policyEditorToolbar.buttonSaveOnly.getAction();
                    action.setEnabled(policyEditorToolbar.buttonSaveOnly.isEnabled());
                }
            } else if (action instanceof ValidatePolicyAction) {
                action = policyEditorToolbar.buttonValidate.getAction();
                action.setEnabled(policyEditorToolbar.buttonValidate.isEnabled());
            } else if (action instanceof AssertionMoveUpAction) {
                action = getMultipleSelectedAssertionMoveUpAction( action.isEnabled());
            } else if (action instanceof AssertionMoveDownAction) {
                action = getMultipleSelectedAssertionMoveDownAction( action.isEnabled());
            } else if (action instanceof DeleteAssertionAction) {
                action = getMultipleSelectedDeleteAssertionAction( action.isEnabled());
            } else if (action instanceof SelectMessageTargetAction) {
                action = getMultipleSelectedSelectMessageTargetAction( (SelectMessageTargetAction)action);
            } else if (action instanceof SelectIdentityTargetAction) {
                action = getMultipleSelectedSelectIdentityTargetAction( (SelectIdentityTargetAction)action);
            } else if (action instanceof SelectIdentityTagAction) {
                action = getMultipleSelectedSelectIdentityTagAction( (SelectIdentityTagAction)action);
            }
            actionList.add(action);
        }
        return actionList.toArray(new Action[actionList.size()]);
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
     * @param isEnabled     Previous enabled action value
     * @return  A new action fitted based on the node selection.
     */
    private DeleteAssertionAction getMultipleSelectedDeleteAssertionAction(boolean isEnabled) {
        final TreePath[] paths = policyTree.getSelectionPaths();
        final AssertionTreeNode[] assertionTreeNodes = toAssertionTreeNodeArray(paths);
        DeleteAssertionAction action;
        action = new DeleteAssertionAction(null, assertionTreeNodes);

        action.setEnabled(isEnabled && canEnableAction(null, assertionTreeNodes, action));
        return action;
    }

    /**
     * Support multiple selection (edit) for message target.
     *
     * <p>Multiple edit is only permitted if all selected items have the same setting.</p>
     */
    private SelectMessageTargetAction getMultipleSelectedSelectMessageTargetAction( final SelectMessageTargetAction selectMessageTargetAction) {
        SelectMessageTargetAction action = selectMessageTargetAction;
        if ( action.isEnabled() ) {
            final TreePath[] paths = policyTree.getSelectionPaths();
            boolean sameTarget = true;

            final AssertionTreeNode[] assertionTreeNodes = toAssertionTreeNodeArray(paths);
            final Assertion target = assertionTreeNodes[0].asAssertion();
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

        return action;
    }

    /**
     * Support multiple selection (edit) for identity target.
     *
     * <p>Multiple edit is only permitted if all selected items have the same
     * setting and target the same message.</p>
     */
    private SelectIdentityTargetAction getMultipleSelectedSelectIdentityTargetAction( final SelectIdentityTargetAction selectIdentityTargetAction ) {
        SelectIdentityTargetAction action = selectIdentityTargetAction;
        if ( action.isEnabled() ) {
            final TreePath[] paths = policyTree.getSelectionPaths();
            boolean sameTarget = true;

            final AssertionTreeNode[] assertionTreeNodes = toAssertionTreeNodeArray(paths);
            final Assertion targetAssertion = assertionTreeNodes[0].asAssertion();
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

        return action;    }

    /**
     * Support multiple selection (edit) for identity tags.
     *
     * <p>Multiple edit is only permitted if all selected items have the same
     * setting.</p>
     */
    private SelectIdentityTagAction getMultipleSelectedSelectIdentityTagAction( final SelectIdentityTagAction selectIdentityTagAction ) {
        SelectIdentityTagAction action = selectIdentityTagAction;
        if ( action.isEnabled() ) {
            final TreePath[] paths = policyTree.getSelectionPaths();
            boolean sameTag = true;

            final AssertionTreeNode[] assertionTreeNodes = toAssertionTreeNodeArray(paths);
            final String idTag = ((IdentityTagable) assertionTreeNodes[0].asAssertion()).getIdentityTag();
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

        return action;
    }

    /**
     * @param isEnabled     Previous enabled action value
     * @return  A new action fitted based on the node selection.
     */
    private AssertionMoveUpAction getMultipleSelectedAssertionMoveUpAction(boolean isEnabled) {
        final TreePath[] paths = policyTree.getSelectionPaths();
        final AssertionTreeNode[] assertionTreeNodes = toAssertionTreeNodeArray(paths);
        AssertionMoveUpAction action = new AssertionMoveUpAction(null) {
                @Override
                protected void performAction() {
                    nodes = assertionTreeNodes;
                    super.performAction();
                    final JTree tree = policyTree;
                    if (tree != null) {
                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                tree.setSelectionPaths(paths);
                            }
                        });
                    }
                }
            };

        action.setEnabled(isEnabled && canEnableAction(null, assertionTreeNodes, action));
        return action;
    }

    /**
     * @param isEnabled     Previous enabled action value
     * @return  A new action fitted based on the node selection.
     */
    private AssertionMoveDownAction getMultipleSelectedAssertionMoveDownAction(boolean isEnabled) {
        final TreePath[] paths = policyTree.getSelectionPaths();
        final AssertionTreeNode[] assertionTreeNodes = toAssertionTreeNodeArray(paths);
        AssertionMoveDownAction action = new AssertionMoveDownAction(null) {
            @Override
            protected void performAction() {
                nodes = assertionTreeNodes;
                super.performAction();
                final JTree tree = policyTree;
                if (tree != null) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            tree.setSelectionPaths(paths);
                        }
                    });
                }
            }
        };

        action.setEnabled(isEnabled && canEnableAction(null, assertionTreeNodes, action));
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
            //noinspection ThrowableResultOfMethodCallIgnored
            log.log(Level.INFO, "Unable to append message: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
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
                        assertionNode.clearIcons();
                    }
                }
            }

            updatePolicyEditorPanel();

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

        @Override
        public void treeNodesInserted(final TreeModelEvent e) {
            updatePolicyEditorPanel();
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e) {
            updatePolicyEditorPanel();
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e) {
            updatePolicyEditorPanel();
        }

        /**
         * Update the policy editor panel such as enabling save buttons and updating tab titles.
         */
        private void updatePolicyEditorPanel() {
            enableButtonSave();

            // If the policy tree node is changed, it means the policy is changed.
            // Update the tab title by adding an asterisk if unsaved changes occur.
            PolicyEditorPanel.this.updateHeadings();
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

            // If the policy tree node is changed, it means the policy is changed.
            // Update the tab title by adding an asterisk if unsaved changes occur.
            PolicyEditorPanel.this.updateHeadings();
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
              } else if (TAB_TITLE_CHANGE_PROPERTY.equals(evt.getPropertyName())) {
                  subjectName = (String) evt.getNewValue();
                  updateHeadings();
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
              final String uriStr = e.getURL().toString();
              URI uri = URI.create(uriStr);
              String f = uri.getFragment();
              if (f == null) return;
              java.util.List<Integer> path = new ArrayList<Integer>();
              StringTokenizer strtok = new StringTokenizer(f, ",");
              try {
                  while (strtok.hasMoreTokens()) {
                      path.add( Integer.parseInt( strtok.nextToken() ) );
                  }
              } catch (NumberFormatException ex) {
                  //noinspection ThrowableResultOfMethodCallIgnored
                  log.log(Level.WARNING, "Unable to parse validator hyperlink: " + ExceptionUtils.getMessage(ex), ExceptionUtils.getDebugException(ex));
                  return;
              }

              AssertionTreeNode an = getCurrentRoot().getAssertionByIndexPath( path.subList( 1, path.size() ) );
              if ( an != null ) {
                  TreePath p = new TreePath(an.getPath());
                  if (!policyTree.hasBeenExpanded(p) || !policyTree.isExpanded(p)) {
                      policyTree.expandPath(p);
                  }
                  policyTree.setSelectionPath(p);

                  if ("fixit".equals(uri.getHost())) {
                      String classname = uri.getPath();
                      if (classname.startsWith("/") && classname.length() > 1)
                          classname = classname.substring(1);

                      try {
                          executeRemedialAction(classname, an);
                      } catch (Exception ex) {
                          //noinspection ThrowableResultOfMethodCallIgnored
                          log.log(Level.WARNING, "Unable to execute remedial action: " + ExceptionUtils.getMessage(ex), ex);
                      }
                  }
              }
          }
    };

    private void executeRemedialAction(String classname, AssertionTreeNode an) throws Exception {
        final Assertion ass = an.asAssertion();
        ClassLoader cl = ass == null ? Thread.currentThread().getContextClassLoader() : ass.getClass().getClassLoader();

        Class actionClass = cl.loadClass(classname);
        if (Runnable.class.isAssignableFrom(actionClass)) {
            ((Runnable)actionClass.newInstance()).run();
        } else if (Callable.class.isAssignableFrom(actionClass)) {
            ((Callable)actionClass.newInstance()).call();
        } else if (Functions.UnaryVoid.class.isAssignableFrom(actionClass)) {
            // Assume it is UnaryVoid<AssertionTreeNode>
            //noinspection unchecked
            ((Functions.UnaryVoid<AssertionTreeNode>)actionClass.newInstance()).call(an);
        } else {
            throw new IllegalArgumentException("Don't know how to invoke remedial action of type " + actionClass.getName());
        }
    }

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
        if (this != e.getChild()) {
            // For some unknown reason, the non-deleted tab calls this method too.
            // In this case, do not reset the policy editor panel.  Also See componentWillRemove(...)
            return;
        }

        log.fine("Resetting the policy editor panel");
        subject.removePropertyChangeListener(policyPropertyChangeListener);
        policyTree.setPolicyEditor(null);
        policyTree.setModel(null);
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
            saveUnsavedPolicyChanges(e);
            final PolicyToolBar pt = topComponents.getPolicyToolBar();
            pt.disableAll();
            pt.unregisterPolicyTree(PolicyEditorPanel.this.getPolicyTree());
            TopComponents.getInstance().firePolicyEditDone();
        }
    }

    private void saveUnsavedPolicyChanges(ContainerEvent e) throws ContainerVetoException {
        if (! isUnsavedChanges()) return;


        boolean connectionLost = TopComponents.getInstance().isConnectionLost();
        if ( !connectionLost && getLatestVersionNumber() == null ) {
            String saveOption = "Save Policy";
            String discardOption = "Discard Policy";
            Object[] options = new String[] { saveOption, discardOption };

            int answer = JOptionPane.showOptionDialog(TopComponents.getInstance().getTopParent(),
                    "<html><center><b>Policy Deleted on Server.  Do you want to save changes for" +
                            "<br>'" + HtmlUtil.escapeHtmlCharacters(getDisplayName()) + "'<br>" +
                            "to service policy file?</b></center></html>",
                    "Save Service Policy",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.WARNING_MESSAGE,
                    null,
                    options,
                    saveOption);
            if (answer != 1)
                getSimpleExportAction().actionPerformed(null);
        } else if (!connectionLost ) {
            int answer = (JOptionPane.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                "<html><center><b>Do you want to save changes to service policy " +
                    "for<br> '" + HtmlUtil.escapeHtmlCharacters(getDisplayName()) + "' ?</b><br>The changed policy will not be activated.</center></html>",
                "Save Service Policy",
                JOptionPane.YES_NO_CANCEL_OPTION));
            if (answer == JOptionPane.YES_OPTION) {
                policyEditorToolbar.buttonSaveOnly.getAction().actionPerformed(null);
            } else if ((answer == JOptionPane.CANCEL_OPTION)) {
                if (e != null) throw new ContainerVetoException(e, "User aborted");
            }
        } else {
            String saveOption = "Save Policy";
            String discardOption = "Discard Policy";
            Object[] options = new String[] { saveOption, discardOption };

            int answer = JOptionPane.showOptionDialog(TopComponents.getInstance().getTopParent(),
                "<html><center><b>Connection Lost.  Do you want to save changes for" +
                        "<br>'" + HtmlUtil.escapeHtmlCharacters(getDisplayName()) + "'<br>" +
                        "to service policy file?</b></center></html>",
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

    /**
     * prune duplicate messages
     *
     * @param result the validation result
     * @param extraWarnings extra warnings to mix in before we prune
     * @return the result containing unique messages
     */
    private PolicyValidatorResult mergeExtrasAndPruneDuplicates(PolicyValidatorResult result, Set<PolicyValidatorResult.Warning> extraWarnings) {
        PolicyValidatorResult pr = new PolicyValidatorResult();

        Set<PolicyValidatorResult.Error> errors = new LinkedHashSet<PolicyValidatorResult.Error>();
        errors.addAll(result.getErrors());
        for (PolicyValidatorResult.Error error : errors) {
            pr.addError(error);
        }

        Set<PolicyValidatorResult.Warning> warnings = new LinkedHashSet<PolicyValidatorResult.Warning>();
        warnings.addAll(result.getWarnings());
        if (extraWarnings != null)
            warnings.addAll(extraWarnings);
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
                final boolean prevActiveStatus = PolicyEditorPanel.this.isVersionActive();

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
                            validateRoot();

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
                            if (activateAsWell) {
                                ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                                ((DefaultTreeModel)tree.getModel()).nodeChanged(subject.getPolicyNode());
                            }
                        }
                    } finally {
                        validating = false;
                    }
                }
                // Update the titles of other tabs associated to the same service/policy entity node
                TopComponents.getInstance().getCurrentWorkspace().updateTabs(prevActiveStatus, activateAsWell);

                // Update the list of the last opened policies
                TopComponents.getInstance().getCurrentWorkspace().saveLastOpenedPolicyTabs();
            }
        };
        ret.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_S);
        int mask = activateAsWell
                   ? ActionEvent.ALT_MASK | ActionEvent.CTRL_MASK
                   : ActionEvent.ALT_MASK;
        ret.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_S, mask));
        return ret;
    }

    private void validateRoot() {
        if (rootAssertion == null) throw new NullPointerException("Root assertion must not be null.");
        if (!(rootAssertion.asAssertion() instanceof AllAssertion)) throw new RuntimeException("Root assertion must be an AllAssertion.");
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

    private void extractFragmentsFromAssertion(@Nullable Assertion rootAssertion, HashMap<String, Policy> fragments) {
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
        // Set the mnemonic
        exportPolicyAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_R);
        exportPolicyAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_R, ActionEvent.ALT_MASK));
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

    /**
     * Visit each node that is currently open in the PolicyEditorPanel.
     *
     * @param visitor the function to call on each node.
     */
    public void visitCurrentlyOpenPolicyTreeNodes(@NotNull final Functions.UnaryVoid<AssertionTreeNode> visitor) {
        if (rootAssertion != null) {
            recurseVisitTreeNodes(rootAssertion, visitor);
        }
    }

    /**
     * Recursively calls the visitor function on the given node and its children.
     */
    private void recurseVisitTreeNodes(final @NotNull AssertionTreeNode node, final @NotNull Functions.UnaryVoid<AssertionTreeNode> visitor) {
        visitor.call(node);
        final Enumeration kidsEnum = node.children();
        while (kidsEnum.hasMoreElements()) {
            final Object child = kidsEnum.nextElement();
            if (child instanceof AssertionTreeNode) {
                final AssertionTreeNode abstractTreeNode = (AssertionTreeNode) child;
                recurseVisitTreeNodes(abstractTreeNode, visitor);
            }
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

    public Action getHideShowCommentAction(final AbstractButton button){
        if (button != null && !showCmtsButtons.contains(button)) {
            showCmtsButtons.add(button);
        }

        if (hideShowCommentsAction == null) {
            hideShowCommentsAction = new SecureAction(null) {
                @Override
                protected void performAction() {
                    //shown = true means the button should say 'Hide'

                    updatePolicyTabProperty(POLICY_TAB_PROPERTY_ASSERTION_SHOW_COMMENTS, String.valueOf(!isShowing()));

                    for (AbstractButton butn: showCmtsButtons) {
                        butn.setText(getName());
                        butn.setToolTipText(getName());
                        butn.setIcon(new ImageIcon(ImageCache.getInstance().getIcon(iconResource())));
                        updateNodesWithComments();
                    }
                }

                @Override
                public String getName() {
                    if (isShowing()) {
                        return "Hide Comments";
                    } else {
                        return "Show Comments";
                    }
                }

                @Override
                protected String iconResource() {
                    return (isShowing()) ? "com/l7tech/console/resources/About16Crossed.gif" : "com/l7tech/console/resources/About16.gif";
                }

                private boolean isShowing(){
                    final String showState = getTabSettingFromPolicyTabProperty(POLICY_TAB_PROPERTY_ASSERTION_SHOW_COMMENTS,
                        SHOW_COMMENTS, null, getPolicyGoid(), getVersionNumber());
                    return Boolean.parseBoolean(showState);
                }
            };

            // Set the mnemonic and accelerator key
            hideShowCommentsAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_C);
            hideShowCommentsAction.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.ALT_MASK));
        }

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

    public MigrateNamespacesAction getMigrateNamespacesAction() {
        if (migrateNamespacesAction != null)
            return migrateNamespacesAction;

        migrateNamespacesAction = new MigrateNamespacesAction();
        migrateNamespacesAction.putValue(Action.MNEMONIC_KEY, KeyEvent.VK_M);

        getPolicyTree().getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                migrateNamespacesAction.setEnabled(isAtLeastOneNamespaceMigratableAssertionSelected());
            }
        });

        return migrateNamespacesAction;
    }

    private boolean isAtLeastOneNamespaceMigratableAssertionSelected() {
        if (getPolicyTree().getSelectionCount() > 0) {
            TreePath[] paths = getPolicyTree().getSelectionModel().getSelectionPaths();
            for (TreePath path : paths) {
                Object obj = path.getLastPathComponent();
                if (obj instanceof AssertionTreeNode) {
                    AssertionTreeNode node = (AssertionTreeNode) obj;
                    Assertion ass = node.asAssertion();
                    if (ass instanceof NamespaceMigratable) {
                        return true;
                    }
                }
            }
        }

        return false;
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
            showAssertionLineNumbersAction = new SecureAction(null) {
                @Override
                protected void performAction() {
                    // Update the status of assertion line numbers shown/hidden in the policy editor panel.
                    boolean toShowNumbers = !policyTreePane.isNumbersVisible();
                    policyTreePane.setNumbersVisible(toShowNumbers);
                    updatePolicyTabProperty(POLICY_TAB_PROPERTY_ASSERTION_SHOW_NUMBERS, String.valueOf(toShowNumbers));

                    // Update the buttons such as policy edit button or menuItem.
                    for (AbstractButton button: showLnNumsButtons) {
                        button.setText(getName());
                        button.setToolTipText(getName());
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

    /**
     * Copy all policy tab properties based on the status of current UI components as showCmtsButtons, policyTreePane, splitPane, and searchForm
     */
    private void copyAllPolicyTabPropertiesBasedOnUI() {
        updatePolicyTabProperty(POLICY_TAB_PROPERTY_ASSERTION_SHOW_COMMENTS, String.valueOf(showCmtsButtons.get(0).getText().equals("Hide Comments")));
        updatePolicyTabProperty(POLICY_TAB_PROPERTY_ASSERTION_SHOW_NUMBERS, String.valueOf(policyTreePane.isNumbersVisible()));
        updatePolicyTabProperty(POLICY_TAB_PROPERTY_MESSAGE_AREA_DIVIDER_LOCATION, Integer.toString(splitPane.getDividerLocation()));

        if (searchForm != null) {
            searchForm.copyAllSearchPropertiesBasedOnUI();
        }
    }

    /**
     * Remove the settings of the current policy tab from policy tab properties. The settings of other policy tabs remains unchanged.
     */
    public void removePolicyTabSettingsFromPolicyTabProperties() {
        final String policyGoid = getPolicyGoid().toString();

        String propValue;
        int goidIdx;
        int goidIdxOffset;
        int leftDelimiterIdx;
        int rightDelimiterIdx;
        int rightDelimiterLength;

        for (String propName: ALL_POLICY_TAB_PROPERTIES) {
            // If no such property found, then check a next property
            propValue = preferences.getString(propName);
            if (propValue == null) {
                continue;
            }

            goidIdx = propValue.indexOf(policyGoid);
            // If no such policy goid found, then check a next property
            if (goidIdx == -1) {
                continue;
            }
            leftDelimiterIdx = propValue.indexOf("(", goidIdx);

            rightDelimiterIdx = propValue.indexOf("), ", goidIdx);
            if (rightDelimiterIdx == -1) {
                rightDelimiterIdx = propValue.indexOf("),", goidIdx);
                if (rightDelimiterIdx == -1) {
                    rightDelimiterIdx = propValue.indexOf(")", goidIdx);
                    rightDelimiterLength = 1;
                } else {
                    rightDelimiterLength = 2;
                }
            } else {
                rightDelimiterLength = 3;
            }

            if (leftDelimiterIdx == -1 || rightDelimiterIdx == -1) {
                throw new RuntimeException("The property '" + propName + "' has an invalid formatted property value.");
            }

            goidIdxOffset = 0;
            if (goidIdx > 2 && rightDelimiterLength == 1) { // rightDelimiterLength == 1 means the policy goid is the last one in the property value.
                if (propValue.substring(goidIdx - 2, goidIdx).equals(", ")) {
                    goidIdxOffset = 2;
                } else if (propValue.substring(goidIdx - 1, goidIdx).equals(",")) {
                    goidIdxOffset = 1;
                }
            }

            propValue = propValue.substring(0, goidIdx - goidIdxOffset) + propValue.substring(rightDelimiterIdx + rightDelimiterLength);

            if (propValue.trim().isEmpty()) {
                preferences.remove(propName);
            } else {
                preferences.putProperty(propName, propValue);
            }
        }

        try {
            preferences.store();
        } catch ( IOException e ) {
            log.warning( "Unable to store preferences " + ExceptionUtils.getMessage(e));
        }
    }

    /**
     * A policy tab property consists of settings of all individual policy tabs in a format:
     * policy_tab_prop_name=policy_oid(policy_version#value, ...), ...etc.
     *
     * This method is to extract the tab setting of the current policy from the property value retrieved by "propName".
     *
     * @param propName: the name of a policy tab property
     * @param deprecatedPropName: the name of a deprecated property, which was used in pre-Icefish versions).  Since
     *                    new property and deprecated property use different value format, to keep backward compatibility,
     *                    somehow we need to retrieve property values of deprecated properties from ssg.properties.
     *                    Set it as null if a deprecated property is not applicable.
     * @param policyGoid: the GOID of a policy, with policyVerNum together to find the property value
     * @param policyVerNum: the ordinal number of a policy, with policyGoid together to find the property value
     *
     * @return a tab setting extracted from the property value of the policy tab property with property name, propName.
     *
     */
    public static String getTabSettingFromPolicyTabProperty(String propName, String deprecatedPropName, String defaultValue,
                                                            Goid policyGoid, long policyVerNum) {
        if (propName == null) {
            if (deprecatedPropName == null) {
                throw new IllegalArgumentException("No property name provided.");
            } else {
                return preferences.getString(deprecatedPropName, defaultValue);
            }
        } else {
            if (! propName.startsWith(POLICY_TAB_PROPERTY_PREFIX)) {
                throw new IllegalArgumentException("The name of the policy tab property '" + propName + "' does not start with 'policy.tabs.'");
            }

            String propValue = preferences.getString(propName);
            if (propValue != null) {
                // The policy goid found means there is a tab setting for the particular policy version
                final int goidIdx = propValue.indexOf(policyGoid.toString());
                if (goidIdx >= 0) {
                    final int leftDelimiter = propValue.indexOf("(", goidIdx);
                    final int rightDelimiter = propValue.indexOf(")", goidIdx);
                    if (leftDelimiter == -1 || rightDelimiter == -1) {
                        throw new RuntimeException("The property " + propName + " has an invalid formatted property value.");
                    }

                    // The property value has a format: (number1#value1, number2#value2, ... ect).
                    // Split the property value by a comma with optional preceding and trailing whitespace
                    final String tabSettings = propValue.substring(leftDelimiter + 1, rightDelimiter);
                    List<String> values = Arrays.asList(TextUtils.CSV_STRING_SPLIT_PATTERN.split(tabSettings));

                    for (String value: values) {
                        // Split each item by a '#' with optional preceding and trailing whitespace
                        String[] result = Pattern.compile("\\s*#\\s*").split(value);
                        if (result == null || result.length != 2) {
                            throw new RuntimeException("The property '" + propName + "' has an invalid formatted property value.");
                        }
                        if (result[0].equals(String.valueOf(policyVerNum))) {
                            return result[1];
                        }
                    }
                    return defaultValue;
                } else {
                    // If the policy goid not found, then return the default value
                    return defaultValue;
                }
            } else if (deprecatedPropName != null) {
                return preferences.getString(deprecatedPropName, defaultValue);
            } else {
                return defaultValue;
            }
        }
    }

    /**
     * Update the policy tab property, whose property value will be extracted and then add a new tab setting or replace an old setting with the new tab setting.
     *
     * @param propName: the property name, which must be in the format: "policy.tabs.XXX".
     */
    public void updatePolicyTabProperty(String propName, String tabSetting) {
        if (propName == null || !propName.startsWith(POLICY_TAB_PROPERTY_PREFIX)) {
            throw new IllegalArgumentException("Invalid policy tab property: " + propName);
        }

        final String policyGoid = getPolicyGoid().toString();
        final String policyVerNum = String.valueOf(getVersionNumber());
        String propValue = preferences.getString(propName);

        // The property value has a format: (number1#value1, number2#value2, ... ect).
        if (propValue == null) {
            propValue = policyGoid + "(" + policyVerNum + "#" + tabSetting + ")";
        } else {
            // The policy goid found means there is a tab setting for the particular policy version
            final int goidIdx = propValue.indexOf(policyGoid);
            if (goidIdx >= 0) {
                final int leftDelimiter = propValue.indexOf("(", goidIdx);
                final int rightDelimiter = propValue.indexOf(")", goidIdx);
                if (leftDelimiter == -1 || rightDelimiter == -1) {
                    throw new RuntimeException("The property '" + propName + "' has an invalid formatted property value.");
                }

                // Split the property value by a comma with optional preceding and trailing whitespace
                final String tabSettings = propValue.substring(leftDelimiter + 1, rightDelimiter);
                List<String> values = Arrays.asList(TextUtils.CSV_STRING_SPLIT_PATTERN.split(tabSettings));

                boolean found = false;
                StringBuilder sb = new StringBuilder();
                for (String value: values) {
                    // Split each item by a '#' with optional preceding and trailing whitespace
                    String[] result = Pattern.compile("\\s*#\\s*").split(value);
                    if (result == null || result.length != 2) {
                        throw new RuntimeException("The property '" + propName + "' has an invalid formatted property value.");
                    }

                    // Check if adding a delimiter ','
                    sb.append(sb.length() > 0? ", " : "");

                    if (result[0].equals(policyVerNum)) {
                        sb.append(result[0]).append("#").append(tabSetting);
                        found = true;
                    } else {
                        sb.append(value);
                    }
                }
                if (!found) {
                    sb.append(sb.length() > 0? ", " : "").append(policyVerNum).append("#").append(tabSetting);
                }

                propValue = propValue.substring(0, leftDelimiter + 1) + sb.toString() + propValue.substring(rightDelimiter);
            } else {
                propValue += ", " + policyGoid + "(" + policyVerNum + "#" + tabSetting + ")";
            }
        }
        preferences.putProperty(propName, propValue);

        try {
            preferences.store();
        } catch ( IOException e ) {
            log.warning( "Unable to store preferences " + ExceptionUtils.getMessage(e));
        }
    }
}
