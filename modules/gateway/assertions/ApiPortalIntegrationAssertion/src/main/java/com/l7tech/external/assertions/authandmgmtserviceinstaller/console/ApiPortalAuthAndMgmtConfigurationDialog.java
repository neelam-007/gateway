package com.l7tech.external.assertions.authandmgmtserviceinstaller.console;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.service.*;
import com.l7tech.gui.NumberField;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import com.l7tech.util.*;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.authandmgmtserviceinstaller.console.ApiPortalAuthenticationConfiguration.*;
import static com.l7tech.external.assertions.authandmgmtserviceinstaller.ApiPortalAuthAndMgmtServiceInstallerConstants.*;
import static com.l7tech.external.assertions.authandmgmtserviceinstaller.ApiPortalAuthAndMgmtServicePolicyResolverUtils.*;

/**
 * The main GUI class for API Portal Authentication and Management Service installer.
 *
 * Briefly this installer will ask user to configure LDAP Providers for Authentication and Management, publish a new service
 * based on three given policy templates (main policy, msad-ldap auth section policy, and open-ldap auth section policy)
 * with a prefix resolution URI, save the published service in a selected folder into the Services and Policies tree in
 * the Policy Manager.
 *
 * For more detail, please visit the func spec of this installer at
 * https://wiki.l7tech.com/mediawiki/index.php/API_Portal_Authentication_And_Management_Service_Installer.
 *
 * @author ghuang
 */
public class ApiPortalAuthAndMgmtConfigurationDialog extends JDialog {

    private final Logger logger = Logger.getLogger(ApiPortalAuthAndMgmtConfigurationDialog.class.getName());

    private JButton cancelButton;
    private JButton okButton;
    private JLabel folderLabel;
    private JPanel mainPanel;
    private JCheckBox enableAuth2CheckBox;
    private JTextField prefixResolutionUrlTextField;
    private JLabel userAccountControlLabel;
    private JTextField userAccountControlTextField;
    private JTextField groupNameTextField;
    private JTextField groupNameForOrgAdminTextField;
    private JTextField userDnCreateFormatTextField;
    private JTextField userDnMemberFormatTextField;
    private JTextField groupDnTextField;
    private JTextField userSearchFilterTextField;
    private JTextField groupAttributeTextField;
    private JTextField usernameAttributeTextField;
    private JTextField passwordAttributeTextField;
    private JTextField objectClassTextField;
    private JComboBox mgmtLdapComboBox;
    private JPanel ldap1Panel;
    private JPanel ldap2Panel;
    private JPanel mgmtAttributesPanel;
    private JCheckBox prefixResolutionURICheckBox;
    private JLabel examplePrefixedUrlLabel;
    private JLabel installerVersionLabel;
    private JCheckBox enableManagementCheckBox;
    private JPanel mgmtLdapSelectionPanel;
    private JLabel positiveIntegerLabel;

    private ApiPortalAuthenticationConfiguration auth1Config;
    private ApiPortalAuthenticationConfiguration auth2Config;
    private long lastSelectedLdapId;
    private Pair<String, FolderNode> selectedFolder;

    public ApiPortalAuthAndMgmtConfigurationDialog(Frame owner) {
        super(owner, INSTALLER_NAME, true);
        setContentPane(mainPanel);
        getRootPane().setDefaultButton(okButton);

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        mainPanel.registerKeyboardAction(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        initialize();
    }

    private void initialize() {
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    onOk();
                } catch (Exception e1) {
                    logger.warning(e1.getMessage());
                    reportError(e1.getMessage());
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                onCancel();
            }
        });

        try {
            installerVersionLabel.setText(getInstallerVersion());
        } catch (Exception e) {
            reportError(e.getMessage());
            return;
        }

        selectedFolder = getSelectedFolder();
        folderLabel.setText(selectedFolder.left);

        auth1Config = new ApiPortalAuthenticationConfiguration(this, true);  // "true" means the "ldap provider 1" config ui is enabled
        auth2Config = new ApiPortalAuthenticationConfiguration(this, false); // "false" means the "ldap provider 2" config ui is disabled

        ldap1Panel.setLayout(new BoxLayout(ldap1Panel, BoxLayout.Y_AXIS));
        ldap1Panel.add(auth1Config.getContentPane());

        ldap2Panel.setLayout(new BoxLayout(ldap2Panel, BoxLayout.Y_AXIS));
        ldap2Panel.add(auth2Config.getContentPane());

        enableAuth2CheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                auth2Config.setEnabled(enableAuth2CheckBox.isSelected());
            }
        });

        enableManagementCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateComponents();
            }
        });

        mgmtLdapComboBox.setModel(new DefaultComboBoxModel(populateLdapProviders()));

        mgmtLdapComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateComponents();
            }
        });

        int maxLen = Math.max(Long.toString(USER_ACCOUNT_CONTROL_MIN).length(), Long.toString(USER_ACCOUNT_CONTROL_MAX).length());
        userAccountControlTextField.setDocument(new NumberField(maxLen + 1));


        prefixResolutionURICheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateComponents();
            }
        });

        prefixResolutionUrlTextField.getDocument().addDocumentListener(new RunOnChangeListener(new Runnable() {
            @Override
            public void run() {
                updateComponents();
            }
        }));

        updateComponents();
    }

    /**
     * Update all UI components if there are any changes from CheckBox and ComboBox.
     */
    private void updateComponents() {
        // If there is no any LDAP chosen under the Management tab, then don't display all attributes.
        boolean visible = mgmtLdapComboBox.getSelectedIndex() != 0;
        if (visible) {
            mgmtAttributesPanel.setVisible(true);

            // If the chosen LDAP is a MSAD LDAP, then diaplay User Account Control components.
            boolean msadLdapAttrVisible = isMsadLdapSelectedFromComboBox(mgmtLdapComboBox);
            userAccountControlLabel.setVisible(msadLdapAttrVisible);
            userAccountControlTextField.setVisible(msadLdapAttrVisible);
            positiveIntegerLabel.setVisible(msadLdapAttrVisible);
        } else {
            mgmtAttributesPanel.setVisible(false);
        }

        // If the management configure is not enabled, then disable the LDAP list and all attributes.
        boolean mgmtConfigEnabled = enableManagementCheckBox.isSelected();
        for (Component c: mgmtLdapSelectionPanel.getComponents()) {
            c.setEnabled(mgmtConfigEnabled);
        }
        for (Component c: mgmtAttributesPanel.getComponents()) {
            c.setEnabled(mgmtConfigEnabled);
        }
        userAccountControlTextField.setEnabled(mgmtConfigEnabled);
        positiveIntegerLabel.setEnabled(mgmtConfigEnabled);

        // When loading a different LDAP from the list, then initialize the attribute values for the chosen LDAP.
        long selectedLdapId = getSelectedLdapId(mgmtLdapComboBox);
        if (isMsadLdapSelectedFromComboBox(mgmtLdapComboBox) && selectedLdapId != lastSelectedLdapId) {
            initializeMgmtMSADLdapAttributes();
        } else if (isOpenLdapSelectedFromComboBox(mgmtLdapComboBox) && selectedLdapId != lastSelectedLdapId) {
            initializemMgmtOpenLdapAttributes();
        }
        lastSelectedLdapId = selectedLdapId;

        // Enable or disable Prefix components
        boolean prefixEnabled = prefixResolutionURICheckBox.isSelected();
        prefixResolutionUrlTextField.setEnabled(prefixEnabled);
        examplePrefixedUrlLabel.setEnabled(prefixEnabled);
        examplePrefixedUrlLabel.setText(getExamplePrefixedUrlLabel());

        DialogDisplayer.pack(this);
    }

    /**
     * Initialize attribute values when a different MSAD LDAP is selected from the LDAP list.
     */
    private void initializeMgmtMSADLdapAttributes() {
        userAccountControlTextField.setText(MGMT_MSAD_DEFAULT_USER_ACCOUNT_CONTROL);
        groupNameTextField.setText(MGMT_MSAD_DEFAULT_GROUP_NAME);
        groupNameForOrgAdminTextField.setText(MGMT_MSAD_DEFAULT_GROUP_NAME_FOR_ORGADMIN);

        LdapIdentityProviderConfig lipc = ((LdapRowInfo) mgmtLdapComboBox.getSelectedItem()).lipc;
        userDnCreateFormatTextField.setText(MessageFormat.format(MGMT_MSAD_DEFAULT_USER_DN_CREATE_FORMAT, lipc.getSearchBase()));
        userDnMemberFormatTextField.setText(MessageFormat.format(MGMT_MSAD_DEFAULT_USER_DN_MEMBER_FORMAT, lipc.getSearchBase()));
        groupDnTextField.setText(MessageFormat.format(MGMT_MSAD_DEFAULT_GROUP_DN, lipc.getSearchBase()));

        userSearchFilterTextField.setText(MGMT_MSAD_DEFAULT_USER_SEARCH_FILTER);
        groupAttributeTextField.setText(MGMT_MSAD_DEFAULT_GROUP_ATTRIBUTE);
        usernameAttributeTextField.setText(MGMT_MSAD_DEFAULT_USERNAME_ATTRIBUTE);
        passwordAttributeTextField.setText(MGMT_MSAD_DEFAULT_PASSWORD_ATTRIBUTE);
        objectClassTextField.setText(MGMT_MSAD_DEFAULT_OBJECTCLASS);
    }

    /**
     * Initialize attribute values when a different Open LDAP is selected from the LDAP list.
     */
    private void initializemMgmtOpenLdapAttributes() {
        groupNameTextField.setText(MGMT_OPENLDAP_DEFAULT_GROUP_NAME);
        groupNameForOrgAdminTextField.setText(MGMT_OPENLDAP_DEFAULT_GROUP_NAME_FOR_ORGADMIN);

        LdapIdentityProviderConfig lipc = ((LdapRowInfo) mgmtLdapComboBox.getSelectedItem()).lipc;
        userDnCreateFormatTextField.setText(MessageFormat.format(MGMT_OPENLDAP_DEFAULT_USER_DN_CREATE_FORMAT, lipc.getSearchBase()));
        userDnMemberFormatTextField.setText(MessageFormat.format(MGMT_OPENLDAP_DEFAULT_USER_DN_MEMBER_FORMAT, lipc.getSearchBase()));
        groupDnTextField.setText(MessageFormat.format(MGMT_OPENLDAP_DEFAULT_GROUP_DN, lipc.getSearchBase()));

        userSearchFilterTextField.setText(MGMT_OPENLDAP_DEFAULT_USER_SEARCH_FILTER);
        groupAttributeTextField.setText(MGMT_OPENLDAP_DEFAULT_GROUP_ATTRIBUTE);
        usernameAttributeTextField.setText(MGMT_OPENLDAP_DEFAULT_USERNAME_ATTRIBUTE);
        passwordAttributeTextField.setText(MGMT_OPENLDAP_DEFAULT_PASSWORD_ATTRIBUTE);
        objectClassTextField.setText(MGMT_OPENLDAP_DEFAULT_OBJECTCLASS);
    }

    /**
     * Publish a new service based on user's configuration inputs
     *
     * @throws Exception: thrown when cannot find the policy files, cannot parse the policy files into a Document, cannot save a published service, etc.
     */
    private void onOk() throws Exception {
        // Create a Service Template based on three given policy files (main policy, msad ldap auth section policy, and open ldap auth section policy)
        final String prefix = prefixResolutionUrlTextField.getText().trim();
        final String resolutionUri = (prefix.isEmpty()? "" : "/" + prefix) + DEFAULT_RESOLUTION_URI;
        final ServiceTemplate toSave = createServiceTemplate(
            POLICY_RESOURCE_BASE_NAME + AUTHENTICATION_MAIN_POLICY_FILE_NAME,
            POLICY_RESOURCE_BASE_NAME + MSAD_LDAP_AUTH_SECTION_POLICY_FILE_NAME,
            POLICY_RESOURCE_BASE_NAME + OPEN_LDAP_AUTH_SECTION_POLICY_FILE_NAME,
            INSTALLER_PUBLISHED_SERVICE_NAME,
            resolutionUri
        );

        // Create a new PublishedService object based on the ServiceTemplate object just created
        PublishedService service = new PublishedService();
        service.setFolder(selectedFolder.right.getFolder());
        service.setName(toSave.getName());
        service.getPolicy().setXml(toSave.getDefaultPolicyXml());
        service.setRoutingUri(toSave.getDefaultUriPrefix());
        service.setSoap(toSave.isSoap());
        service.setInternal(true);
        service.parseWsdlStrategy( new ServiceDocumentWsdlStrategy(toSave.getServiceDocuments()) );
        service.setWsdlUrl(toSave.getServiceDescriptorUrl());
        service.setWsdlXml(toSave.getServiceDescriptorXml());
        service.setDisabled(false);
        service.setHttpMethods(EnumSet.of(HttpMethod.POST, HttpMethod.GET, HttpMethod.PUT, HttpMethod.DELETE));

        // Perform validation such as checking service naming conflict, ldap provider selection,
        // attribute with non-empty value, and valid resolution uri prefix.
        final List<String> problems = validateComponents();

        final ServiceAdmin.ResolutionReport report = Registry.getDefault().getServiceManager().generateResolutionReport(service, null);
        // Check service resolution uri conflict
        if (!report.isSuccess()) {
            problems.add(0, "Service Resolution Conflict: " + service.displayName() + " already exists.");
        }

        // If there are any problems, report them.
        if (! problems.isEmpty()) {
            final StringBuilder sb = new StringBuilder("<html>");
            sb.append("<p>Installation not completed due to the following problem(s):</p>");
            sb.append("<ul");
            for (String problem: problems)
                sb.append("<li>").append(problem).append("</li>");
            sb.append("</ul>");
            sb.append("</html");

            DialogDisplayer.showMessageDialog(this, sb.toString(), "Installation Problem(s)", JOptionPane.WARNING_MESSAGE, null);
            return;
        }

        // If validation is passed, then publish a new service
        Goid goid = Registry.getDefault().getServiceManager().savePublishedService(service);
        Registry.getDefault().getSecurityProvider().refreshPermissionCache();
        service.setGoid(goid);
        Thread.sleep(1000);

        EntityHeader entityHeader = new ServiceHeader(service);
        final JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        AbstractTreeNode parent = getSelectedFolder().right;

        //Remove any filter before insert
        TopComponents.getInstance().clearFilter();

        // Set the selection on the newly published service
        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
        final AbstractTreeNode sn = TreeNodeFactory.asTreeNode(entityHeader, null);
        model.insertNodeInto(sn, parent, parent.getInsertPosition(sn, RootNode.getComparator()));
        RootNode rootNode = (RootNode) model.getRoot();
        rootNode.addEntity(entityHeader.getGoid(), sn);
        tree.setSelectionPath(new TreePath(sn.getPath()));

        // If everything is good, close the installation dialog
        dispose();
    }

    private void onCancel() {
        dispose();
    }

    /**
     * validate all components before the installer processes installation.
     *
     * @return a list of problems after validation such as checking Service Resolution conflict, LDAP Provider selection,
     * Attribute value format, prefix for Resolution URI.
     */
    private List<String> validateComponents() {
        final List<String> problemsList = new ArrayList<String>();

        // Check LDAP provider selection
        if (auth1Config.getLdapComboBox().getSelectedIndex() == 0) {
            problemsList.add("LDAP Provider 1 for Authentication is not specified.");
        }
        if (enableAuth2CheckBox.isSelected() && auth2Config.getLdapComboBox().getSelectedIndex() == 0) {
            problemsList.add("LDAP Provider 2 for Authentication is enabled but not specified.");
        }
        if (enableManagementCheckBox.isSelected() && mgmtLdapComboBox.getSelectedIndex() == 0) {
            problemsList.add("LDAP Provider for Management is enabled but not specified.");
        }
        // Check all attributes
        // Firstly check LDAP Provider 1 for Authentication
        validateAuthenticationAttributes(problemsList, auth1Config, "Ldap Provider 1");

        // Secondly check LDAP Provider 2 for Authentication
        if (enableAuth2CheckBox.isSelected()) {
            validateAuthenticationAttributes(problemsList, auth2Config, "Ldap Provider 2");
        }
        // Finally check LDAP Provider for Management
        if (enableManagementCheckBox.isSelected()) {
            if (isMsadLdapSelectedFromComboBox(mgmtLdapComboBox)) {
                try {
                    int num = Integer.parseInt(userAccountControlTextField.getText().trim());
                    if (num < USER_ACCOUNT_CONTROL_MIN || num > USER_ACCOUNT_CONTROL_MAX) {
                        problemsList.add("'User Account Control' is not in a valid range: [" + USER_ACCOUNT_CONTROL_MIN + ", " + USER_ACCOUNT_CONTROL_MAX + "].");
                    }
                } catch (NumberFormatException e) {
                    problemsList.add("'User Account Control' is not a number.");
                }
            }
            if (mgmtLdapComboBox.getSelectedIndex() > 0) {
                checkNullOrEmpty(problemsList, "'Group Name'", groupNameTextField.getText().trim());
                checkNullOrEmpty(problemsList, "'Group Name for OrgAdmin'", groupNameForOrgAdminTextField.getText().trim());

                checkNullOrEmpty(problemsList, "'User DN Create Format'", userDnCreateFormatTextField.getText().trim());
                checkNullOrEmpty(problemsList, "'User DN Member Format'", userDnMemberFormatTextField.getText().trim());
                checkNullOrEmpty(problemsList, "'Group DN'", groupDnTextField.getText().trim());

                String attributeInfo = "'User Search Filter' for Management";
                String userSearchFilterValue = userSearchFilterTextField.getText().trim();
                checkNullOrEmpty(problemsList, attributeInfo, userSearchFilterValue);
                if (isOpenLdapSelectedFromComboBox(mgmtLdapComboBox)) {
                    if ((userSearchFilterValue.startsWith("(") && !userSearchFilterValue.endsWith(")")) ||
                        (!userSearchFilterValue.startsWith("(") && userSearchFilterValue.endsWith(")"))) {
                        problemsList.add(attributeInfo + " has invalid form.");
                    }
                }

                checkNullOrEmpty(problemsList, "'Group Attribute'", groupAttributeTextField.getText().trim());
                checkNullOrEmpty(problemsList, "'Username Attribute'", usernameAttributeTextField.getText().trim());
                checkNullOrEmpty(problemsList, "'Password Attribute'", passwordAttributeTextField.getText().trim());

                checkNullOrEmpty(problemsList, "'ObjectClass'", objectClassTextField.getText().trim());
            }
        }

        // Check the resolution URI prefix
        if (prefixResolutionURICheckBox.isSelected()) {
            checkNullOrEmpty(problemsList, "'Prefix resolution URI'", prefixResolutionUrlTextField.getText());

            String prefixError = getPrefixedUrlErrorMsg(prefixResolutionUrlTextField.getText());
            if (prefixError != null) problemsList.add(prefixError);
        }

        return problemsList;
    }

    /**
     * Validate all authentication attributes such as non-empty string required and parenthese must be paired if they present.
     *
     * @param problemsList: the list of problems occur during validation.  This list will be returned to the caller.
     * @param authConfig: the Authentication configuration UI component, which is for either LDAP Provider 1 or LDAP Provider 2.
     * @param ldapProviderName: the name of the chosen LDAP Provider, such as either LDAP Provider 1 or LDAP Provider 2.
     */
    private void validateAuthenticationAttributes(final List<String> problemsList, final ApiPortalAuthenticationConfiguration authConfig, final String ldapProviderName) {
        if (isMsadLdapSelectedFromComboBox(authConfig.getLdapComboBox())) {
            String attributeInfo = "'Search Filter' for " + ldapProviderName;
            checkNullOrEmpty(problemsList, attributeInfo, authConfig.getSearchFilter());
        } else if (isOpenLdapSelectedFromComboBox(authConfig.getLdapComboBox())) {
            String attributeInfo = "'User Search Filter' for " + ldapProviderName;
            String userSearchFilterValue = authConfig.getUserSearchFilter();
            checkNullOrEmpty(problemsList, attributeInfo, userSearchFilterValue);

            if ((userSearchFilterValue.startsWith("(") && !userSearchFilterValue.endsWith(")")) ||
                (!userSearchFilterValue.startsWith("(") && userSearchFilterValue.endsWith(")"))) {
                problemsList.add(attributeInfo + " has invalid form.");
            }

            attributeInfo = "'Group Search Filter' for " + ldapProviderName;
            String groupSearchFilterValue = authConfig.getGroupSearchFilter();
            checkNullOrEmpty(problemsList, attributeInfo, groupSearchFilterValue);

            if ((groupSearchFilterValue.startsWith("(") && !groupSearchFilterValue.endsWith(")")) ||
                (!groupSearchFilterValue.startsWith("(") && groupSearchFilterValue.endsWith(")"))) {
                problemsList.add(attributeInfo + " has invalid form.");
            }
        }
    }

    /**
     * Check if an input is null or empty (emtpy string or white space string)
     *
     * @param problemsList: the list of problems occur during validation.  This list will be returned to the caller.
     * @param fieldInfo: the name of attribute with some extra information.
     * @param input: the value of attribute
     */
    private void checkNullOrEmpty(List<String> problemsList, String fieldInfo, String input) {
        if (input == null || input.trim().isEmpty()) problemsList.add(fieldInfo + " is empty.");
    }

    /**
     * Create a Service Template based on user's inputs of Authentication and Management configuration.
     *
     * @param mainPolicyFile: the policy xml file for the authentication and management main policy.  The main policy xml
     *                      content will contain the below two policy xml content depending on user choosing LDAP providers.
     * @param msadLdapAuthPolicyFile: the policy xml file for the authentication section with a MSAD LDAP provider selected.
     * @param openLdapAuthPolicyFile: the policy xml file for the authentication section with a Open LDAP provider selected.
     * @param serviceName: the name of the published service
     * @param uriPrefix: the prefix of the service resolution URI
     * @return a ServiceTemplate object with service name, uri prefix, policy xml initialized.
     * @throws Exception: throw exceptions when policy files cannot be read and parsed.
     */
    private ServiceTemplate createServiceTemplate(final String mainPolicyFile, final String msadLdapAuthPolicyFile, final String openLdapAuthPolicyFile,
                                                  final String serviceName, final String uriPrefix) throws Exception {
        ServiceTemplate template;
        try {
            final String policyXml = generatePolicyXmlContent(mainPolicyFile, msadLdapAuthPolicyFile, openLdapAuthPolicyFile);
            template = new ServiceTemplate(
                serviceName,
                uriPrefix,
                policyXml,
                ServiceType.OTHER_INTERNAL_SERVICE,
                null);
        } catch (final IOException e) {
            logger.log(Level.WARNING, "Error creating service template. " + serviceName + " will not be available.", ExceptionUtils.getDebugException(e));
            throw new Exception("Cannot read policy files: " + mainPolicyFile + ", " + msadLdapAuthPolicyFile + ", or " + openLdapAuthPolicyFile);
        } catch (SAXException e) {
            throw new Exception("Cannot parse policy template file(s) correctly.");
        }
        return template;
    }

    /**
     * Generate a policy xml content for the published service based on user's inputs for Authentication and Management configuration.
     *
     * @param mainPolicyFile: the policy xml file for the authentication and management main policy.  The main policy xml
     *                      content will contain the below two policy xml content depending on user choosing LDAP providers.
     * @param msadLdapAuthPolicyFile: the policy xml file for the authentication section with a MSAD LDAP provider selected.
     * @param openLdapAuthPolicyFile: the policy xml file for the authentication section with a Open LDAP provider selected.
     * @return an xml content for the policy of the published service.
     * @throws Exception: throw exceptions when policy files cannot be read and parsed.
     */
    private String generatePolicyXmlContent(final String mainPolicyFile, final String msadLdapAuthPolicyFile, final String openLdapAuthPolicyFile) throws Exception {
        Document mainPolicyDoc = readPolicyFile(mainPolicyFile);

        // (1) Update the Authentication part in the main policy with the following two blocks of code:
        // Firstly, check if the ldap provider 2 is enabled or not
        boolean isLdapProvider2Enabled = enableAuth2CheckBox.isSelected();
        if (isLdapProvider2Enabled) {
            Document auth2PolicyDoc = null;
            if (isMsadLdapSelectedFromComboBox(auth2Config.getLdapComboBox())) {
                auth2PolicyDoc = readPolicyFile(msadLdapAuthPolicyFile);
                updateAttributeValueByLeftCommentName(auth2PolicyDoc, LEFT_COMMENT_NAME_AUTH_MSAD_LDAP_SEARCH_FILTER, auth2Config.getSearchFilter());
            } else if (isOpenLdapSelectedFromComboBox(auth2Config.getLdapComboBox())) {
                auth2PolicyDoc = readPolicyFile(openLdapAuthPolicyFile);
                updateAttributeValueByLeftCommentName(auth2PolicyDoc, LEFT_COMMENT_NAME_AUTH_OPEN_LDAP_USER_SEARCH_FILTER, auth2Config.getUserSearchFilter());
                updateAttributeValueByLeftCommentName(auth2PolicyDoc, LEFT_COMMENT_NAME_AUTH_OPEN_LDAP_GROUP_SEARCH_FILTER, auth2Config.getGroupSearchFilter());
            }
            if (auth2PolicyDoc != null) {
                LdapIdentityProviderConfig lipc = ((LdapRowInfo) auth2Config.getLdapComboBox().getSelectedItem()).lipc;
                updateLdapProviderIdByElementName(auth2PolicyDoc, "IdentityProviderOid", lipc.getId());
                updateLdapProviderIdByElementName(auth2PolicyDoc, "LdapProviderOid", lipc.getId());

                insertAuthenticationPartIntoPolicy(mainPolicyDoc, auth2PolicyDoc, LEFT_COMMENT_NAME_AUTHENTICATION_PROVIDER);
            }
        }

        // Secondly, process the ldap provider 1, which is must.
        Document auth1PolicyDoc = null;
        if (isMsadLdapSelectedFromComboBox(auth1Config.getLdapComboBox())) {
            auth1PolicyDoc = readPolicyFile(msadLdapAuthPolicyFile);

            updateAttributeValueByLeftCommentName(auth1PolicyDoc, LEFT_COMMENT_NAME_AUTH_MSAD_LDAP_SEARCH_FILTER, auth1Config.getSearchFilter());
        } else if (isOpenLdapSelectedFromComboBox(auth1Config.getLdapComboBox())) {
            auth1PolicyDoc = readPolicyFile(openLdapAuthPolicyFile);

            updateAttributeValueByLeftCommentName(auth1PolicyDoc, LEFT_COMMENT_NAME_AUTH_OPEN_LDAP_USER_SEARCH_FILTER, auth1Config.getUserSearchFilter());
            updateAttributeValueByLeftCommentName(auth1PolicyDoc, LEFT_COMMENT_NAME_AUTH_OPEN_LDAP_GROUP_SEARCH_FILTER, auth1Config.getGroupSearchFilter());
        }
        if (auth1PolicyDoc != null) {
            LdapIdentityProviderConfig lipc = ((LdapRowInfo) auth1Config.getLdapComboBox().getSelectedItem()).lipc;
            updateLdapProviderIdByElementName(auth1PolicyDoc, "IdentityProviderOid", lipc.getId());
            updateLdapProviderIdByElementName(auth1PolicyDoc, "LdapProviderOid", lipc.getId());

            insertAuthenticationPartIntoPolicy(mainPolicyDoc, auth1PolicyDoc, LEFT_COMMENT_NAME_AUTHENTICATION_PROVIDER);
        }

        // (2) Set the prefix context variable in the main policy
        if (prefixResolutionURICheckBox.isSelected()) {
            String resolutionUriPrefix = prefixResolutionUrlTextField.getText().trim();
            if (! resolutionUriPrefix.isEmpty()) {
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_PREFIX, resolutionUriPrefix);
            }
        }

        // (3) Update Management part in the main policy
        boolean managementEnabled = enableManagementCheckBox.isSelected();
        if (managementEnabled) {
            LdapIdentityProviderConfig lipc = ((LdapRowInfo)mgmtLdapComboBox.getSelectedItem()).lipc;
            if (lipc != null) {
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_SSG_LDAP_PROVIDER_NAME, lipc.getName());
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_BASE_DN, lipc.getSearchBase());

                boolean isMsadLdap = isMsadLdapSelectedFromComboBox(mgmtLdapComboBox);
                if (isMsadLdap) {
                    updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_USER_ACCOUNT_CONTROL, userAccountControlTextField.getText().trim());
                }
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_BASE_DN, lipc.getSearchBase());
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_GROUP_NAME, groupNameTextField.getText().trim());
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_GROUP_NAME_FOR_ORGADMIN, groupNameForOrgAdminTextField.getText().trim());
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_USER_DN_CREATE_FORMAT, userDnCreateFormatTextField.getText().trim());
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_USER_DN_MEMBER_FORMAT, userDnMemberFormatTextField.getText().trim());
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_GROUP_DN, groupDnTextField.getText().trim());
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_USER_SEARCH_FILTER, userSearchFilterTextField.getText().trim());
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_GROUP_ATTRIBUTE, groupAttributeTextField.getText().trim());
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_USERNAME_ATTRIBUTE, usernameAttributeTextField.getText().trim());
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_PASSWORD_ATTRIBUTE, passwordAttributeTextField.getText().trim());
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_OBJECTCLASS, objectClassTextField.getText().trim());
                updateAttributeValueByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_HAS_UID_ATTRIBUTES, isMsadLdap ? "false" : "true");

                updateLdapProviderIdByLeftCommentName(mainPolicyDoc, LEFT_COMMENT_NAME_MGMT_QUERY_LDAP, lipc.getId());
            }
        } else {
            removeManagementPartFromPolicy(mainPolicyDoc, LEFT_COMMENT_NAME_MANAGEMENT_HANDLER);
        }

        try {
            return XmlUtil.nodeToString(mainPolicyDoc);
        } catch (IOException e) {
            throw new Exception("Cannot convert a Document object to a string for the file " + mainPolicyFile);
        }
    }

    /**
     * Get the installer version number from the bundle info file, ApiPortalAuthServiceInstallerBundleInfo.xml.
     * The installer version indicates the current version of API Portal Authentication and Management Service Installer.
     *
     * @return a version string, for example, "1.0"
     * @throws Exception: throw exceptions when the bundle file is not found, is not well-formatted, has invalid content inside.
     */
    private String getInstallerVersion() throws Exception {
        final URL installerBundleInfoUrl = getClass().getResource(INSTALLER_BUNDLE_INFO_FILE_PATH);
        if (installerBundleInfoUrl == null) {
            throw new IllegalArgumentException("Could not find " + INSTALLER_BUNDLE_INFO_FILE_PATH);
        }
        final byte[] bundleBytes;
        try {
            bundleBytes = IOUtils.slurpUrl(installerBundleInfoUrl);
        } catch (IOException e) {
            throw new Exception("The bundle information URL '" + installerBundleInfoUrl + "' is invalid.");
        }
        final Document installerBundleInfoDoc;
        try {
            installerBundleInfoDoc = XmlUtil.parse(new ByteArrayInputStream(bundleBytes));
        } catch (Exception e) {
            throw new Exception("The bundle information file is invalid");
        }
        final Element versionElm;
        try {
            versionElm = XmlUtil.findExactlyOneChildElementByName(installerBundleInfoDoc.getDocumentElement(), NS_INSTALLER_VERSION, "Version");
        } catch (Exception e) {
            throw new Exception("The bundle information file is not well-formatted");
        }

        final String installerVersion = DomUtils.getTextValue(versionElm, true);
        if (installerVersion.isEmpty()) {
            throw new Exception("Could not get version information for Portal Auth Installer");
        }

        return installerVersion;
    }

    /**
     * Report an error by displaying an error dialog with a given error information message.
     * @param errorMessage: the message to show what the error is.
     */
    private void reportError(String errorMessage) {
        DialogDisplayer.showMessageDialog(this, errorMessage, "Pre-installation Error", JOptionPane.WARNING_MESSAGE, new Runnable() {
            @Override
            public void run() {
                ApiPortalAuthAndMgmtConfigurationDialog.this.dispose();
            }
        });
    }

    /**
     * Generate an example for a prefixed resolution URL, based on the prefix setup.
     * @return a resolution URI string
     */
    @Nullable
    private String getExamplePrefixedUrlLabel() {
        String urlLabel = "Example prefixed URL:";
        String prefix = prefixResolutionUrlTextField.getText().trim();

        if (prefix.isEmpty()) return urlLabel;
        else return urlLabel + " https://yourgateway.com:8443/" + prefix + DEFAULT_RESOLUTION_URI;
    }

    /**
     * Validate the prefixed resolution URL and generate an error message if the URL is invalid.
     *
     * @param prefix: the prefix of the resolution URI
     * @return null if no validation error exists and an error message if the prefixed URL is invalid.
     */
    @Nullable
    private String getPrefixedUrlErrorMsg(String prefix){
        // validate for XML chars
        String [] invalidChars = new String[]{"\"", "&", "'", "<", ">"};
        for (String invalidChar : invalidChars) {
            if (prefix.contains(invalidChar)) {
                return "Invalid character '" + TextUtils.escapeHtmlSpecialCharacters(invalidChar) + "' is not allowed in the resolution prefix.";
            }
        }

        String testUri = "http://ssg.com:8080/" + prefix + "/query";
        if (!ValidationUtils.isValidUrl(testUri)) {
            return "It must be possible to construct a valid routing URI using the prefix.";
        }

        return null;
    }

    /**
     * Get the selected folder from the Services and Policies tree.
     * @return a pair containing the path of the selected folder and a Folder object.
     */
    public static Pair<String, FolderNode> getSelectedFolder(){
        ServicesAndPoliciesTree tree = (ServicesAndPoliciesTree) TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);

        String folderPath = null;
        FolderNode parentFolderNode = null;

        if (tree != null) {
            final TreePath selectionPath = tree.getSelectionPath();
            if (selectionPath != null) {
                final Object[] path = selectionPath.getPath();

                if (path.length > 0) {
                    StringBuilder builder = new StringBuilder("");

                    // skip the root node, it is captured as /
                    RootNode rootFolder = (RootNode) path[0];
                    FolderNode lastParentFolderNode = rootFolder;
                    for (int i = 1, pathLength = path.length; i < pathLength; i++) {
                        Object o = path[i];
                        if (o instanceof FolderNode) {
                            FolderNode folderNode = (FolderNode) o;
                            builder.append("/");
                            builder.append(folderNode.getName());
                            lastParentFolderNode = folderNode;
                        }
                    }
                    builder.append("/");  // if only root node then this captures that with a single /
                    folderPath = builder.toString();
                    parentFolderNode = lastParentFolderNode;
                }
            }

            if (parentFolderNode == null) {
                final RootNode rootNode = tree.getRootNode();
                parentFolderNode = rootNode;
                folderPath = "/";
            }
        }
        return new Pair<String, FolderNode>(folderPath, parentFolderNode);
    }
}