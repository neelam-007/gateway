package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.console.action.Actions;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.JmsUtilities;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssX509Cert;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.wsdl.WSDLException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.Principal;
import java.util.*;
import java.util.List;

/**
 * The <code>IdentityPolicyPanel</code> is the policy panel that allows
 * editing identity policy.
 * The policy allows editing only the elements that are specific to the
 * identity. For exmaple if assertions are shared with other identites
 * then they cannot be edited.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class IdentityPolicyPanel extends JPanel {

    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JButton helpButton;
    private JButton defaultUrlButton;
    private JComboBox authMethodComboBox;
    private JComboBox xmlSecOptions;
    private JCheckBox sslCheckBox;
    private JTextField routeToUrlField;
    private JTextField userRouteField;
    private JPasswordField passwordRouteField;
    private JTextField realmRouteField;
    private JRadioButton httpRoutingRadioButton;
    private JRadioButton jmsRoutingRadioButton;
    private JComboBox jmsQueueComboBox;

    private Principal principal;
    private IdentityPath principalAssertionPaths;
    private Set otherPaths;
    private Assertion rootAssertion;
    private PublishedService service;
    private AssertionTreeNode identityAssertionNode;

    private SslAssertion sslAssertion = null;
    private CredentialSourceAssertion existingCredAssertion = null;

    private RoutingAssertion existingRoutingAssertion = null;
    private boolean routeEdited;
    private boolean routeModifiable = true;
    private boolean initializing = false;

    private PolicyTreeModel policyTreeModel;
    private AssertionTreeNode rootAssertionTreeNode;
    private static final String[] XML_SEC_OPTIONS = new String[]{"sign only", "sign and encrypt"};
    private Map credentialsLocationMap = CredentialsLocation.newCredentialsLocationMap(true);

    /**
     * Create the identity policy panel for a given identity and service
     *
     * @param service               the service
     * @param model                 the policy tree model
     * @param identityAssertionNode the identity assertion node
     */
    public IdentityPolicyPanel(PublishedService service,
                               DefaultTreeModel model,
                               IdentityAssertionTreeNode identityAssertionNode) {
        super();
        if (service == null ||
          identityAssertionNode == null ||
          model == null) {
            throw new IllegalArgumentException();
        }
        this.service = service;
        this.policyTreeModel = (PolicyTreeModel) model;
        this.identityAssertionNode = identityAssertionNode;
        this.principal = IdentityPath.extractIdentity(identityAssertionNode.asAssertion());
        rootAssertionTreeNode = (AssertionTreeNode) identityAssertionNode.getRoot();
        this.rootAssertion = rootAssertionTreeNode.asAssertion();

        try {
            initializing = true;
            this.initialize();
        } finally {
            initializing = false;
        }
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
    }

    private void initialize() {
        ButtonGroup routingProtocolButtonGroup = new ButtonGroup();
        routingProtocolButtonGroup.add(jmsRoutingRadioButton);
        routingProtocolButtonGroup.add(httpRoutingRadioButton);
        ActionListener routingRadioButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (!initializing) {
                    routeEdited = true;
                }
                enableOrDisableRoutingControls();
            }
        };
        httpRoutingRadioButton.addActionListener(routingRadioButtonListener);
        jmsRoutingRadioButton.addActionListener(routingRadioButtonListener);
        jmsQueueComboBox.setModel(new DefaultComboBoxModel(JmsUtilities.loadQueueItems()));
        jmsQueueComboBox.addActionListener(routingRadioButtonListener);

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exitHandler(e);
            }
        });
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(SwingUtilities.windowForComponent(IdentityPolicyPanel.this));
            }
        });
        okButton.addActionListener(updateIdentityPolicy);
        Utilities.equalizeButtonSizes(new JButton[]{cancelButton, okButton, helpButton});
        authMethodComboBox.setModel(CredentialsLocation.getCredentialsLocationComboBoxModelNonAnonymous(true));
        authMethodComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Object key = e.getItem();
                CredentialSourceAssertion ca =
                  (CredentialSourceAssertion) credentialsLocationMap.get(key);
                xmlSecOptions.setEnabled(ca instanceof RequestWssX509Cert);
            }
        });
        
        // Bugzilla #821 - the default is disabled since the default setting of Authentication Method is HTTP Basic
        xmlSecOptions.setEnabled(false);

        defaultUrlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    final URL url = service.serviceUrl(null);
                    if (url != null) {
                        routeToUrlField.setText(url.toString());
                    }
                } catch (WSDLException e1) {
                    // say to the user
                } catch (MalformedURLException e1) {
                    // say something to the user
                }
            }
        });
        ComboBoxModel cm = new DefaultComboBoxModel(XML_SEC_OPTIONS);
        xmlSecOptions.setModel(cm);
        principalAssertionPaths = IdentityPath.forIdentity(principal, rootAssertion);
        otherPaths = IdentityPath.getPaths(rootAssertion);
        Collection remove = new ArrayList();
        for (Iterator iterator = otherPaths.iterator(); iterator.hasNext();) {
            IdentityPath ip = (IdentityPath) iterator.next();
            if (ip.getPrincipal().equals(principalAssertionPaths.getPrincipal())) {
                remove.add(ip);
            }
        }
        otherPaths.removeAll(remove);

        routeToUrlField.getDocument().addDocumentListener(wasRouteEditedListener);
        userRouteField.getDocument().addDocumentListener(wasRouteEditedListener);
        passwordRouteField.getDocument().addDocumentListener(wasRouteEditedListener);
        realmRouteField.getDocument().addDocumentListener(wasRouteEditedListener);
        passwordRouteField.setBorder(userRouteField.getBorder());
        updateSslAssertion();
        updateAuthMethod();
        updateRouting();
    }

    private void exitHandler(ActionEvent e) {
        Window w = SwingUtilities.windowForComponent(this);
        w.dispose();
    }

    /**
     * update the ssl assertion control for the identity
     * This updates the assertion elements by analyzing
     * the policy to find out if the assertion can modified
     * and what is the value of it.
     */
    private void updateSslAssertion() {
        boolean canmod = true;
        boolean selected = false;

        Set othersSslAssertions = new HashSet();
        for (Iterator iterator = otherPaths.iterator(); iterator.hasNext();) {
            IdentityPath ip = (IdentityPath) iterator.next();
            othersSslAssertions.addAll(ip.getEqualAssertions(SslAssertion.class));
        }

        Set principalSslAssertions = principalAssertionPaths.getEqualAssertions(SslAssertion.class);
        selected = !principalSslAssertions.isEmpty();
        for (Iterator iterator = principalSslAssertions.iterator(); iterator.hasNext();) {
            sslAssertion = (SslAssertion) iterator.next();
            if (othersSslAssertions.contains(sslAssertion)) {
                canmod = false;
                break;
            }
        }
        sslCheckBox.setSelected(selected);
        sslCheckBox.setEnabled(canmod);
    }

    private void updateAuthMethod() {
        boolean canmod = true;

        Set othersCredAssertions = new HashSet();
        for (Iterator iterator = otherPaths.iterator(); iterator.hasNext();) {
            IdentityPath ip = (IdentityPath) iterator.next();
            othersCredAssertions.addAll(ip.getAssignableAssertions(CredentialSourceAssertion.class));
        }

        Set principalCredAssertions = principalAssertionPaths.getAssignableAssertions(CredentialSourceAssertion.class);
        for (Iterator it = principalCredAssertions.iterator(); it.hasNext();) {
            CredentialSourceAssertion credAssertion = (CredentialSourceAssertion)it.next();

            existingCredAssertion = credAssertion;
            RequestWssX509Cert xmlSec = null;
            if (credAssertion instanceof RequestWssX509Cert)
                xmlSec = (RequestWssX509Cert)credAssertion;

            selectAuthMethodComboItem(existingCredAssertion);
            if (othersCredAssertions.contains(existingCredAssertion))
                canmod = false;

            if (xmlSec != null) {
                xmlSecOptions.setEnabled(canmod);
                boolean isXmlEncrypted = false; // TODO this no longer makes sense
                xmlSecOptions.setSelectedIndex(isXmlEncrypted ? 1 : 0);
            } else
                xmlSecOptions.setEnabled(false);
        }

        authMethodComboBox.setEnabled(canmod);
    }

    private void selectAuthMethodComboItem(Assertion cas) {
        Set entrySet = credentialsLocationMap.entrySet();
        for (Iterator iterator = entrySet.iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry) iterator.next();
            if (cas.getClass().equals(entry.getValue().getClass())) {
                authMethodComboBox.setSelectedItem(entry.getKey());
                break;
            }
        }
    }

    private void updateRouting() {
        routeModifiable = true;

        Set othersRouteAssertions = new HashSet();
        for (Iterator iterator = otherPaths.iterator(); iterator.hasNext();) {
            IdentityPath ip = (IdentityPath) iterator.next();
            othersRouteAssertions.addAll(ip.getAssignableAssertions(RoutingAssertion.class));
        }

        Set principalRouteAssertions = principalAssertionPaths.getAssignableAssertions(RoutingAssertion.class);
        for (Iterator it = principalRouteAssertions.iterator(); it.hasNext();) {
            existingRoutingAssertion = (RoutingAssertion) it.next();
            if (existingRoutingAssertion instanceof HttpRoutingAssertion) {
                HttpRoutingAssertion hra = (HttpRoutingAssertion) existingRoutingAssertion;
                routeToUrlField.setText(hra.getProtectedServiceUrl());
                userRouteField.setText(hra.getLogin());
                passwordRouteField.setText(hra.getPassword());
                realmRouteField.setText(hra.getRealm());
            } else if (existingRoutingAssertion instanceof JmsRoutingAssertion) {
                JmsRoutingAssertion jra = (JmsRoutingAssertion) existingRoutingAssertion;
                JmsUtilities.selectEndpoint(jmsQueueComboBox, jra.getEndpointOid());
            } else {
                throw new RuntimeException("Unrecognized type of routing assertion: " +
                  existingRoutingAssertion.getClass().getName());
            }
            if (othersRouteAssertions.contains(existingRoutingAssertion)) {
                routeModifiable = false;
                break;
            }
        }

        boolean isJms = existingRoutingAssertion instanceof JmsRoutingAssertion;
        if (!isJms)
            jmsQueueComboBox.setSelectedIndex(-1);
        jmsRoutingRadioButton.setSelected(isJms);
        jmsRoutingRadioButton.setEnabled(routeModifiable);
        httpRoutingRadioButton.setSelected(!isJms);
        httpRoutingRadioButton.setEnabled(routeModifiable);
        enableOrDisableRoutingControls();
    }

    private Boolean wasJms = null;

    private void enableOrDisableRoutingControls() {
        boolean isJms = jmsRoutingRadioButton.isSelected();
        if (wasJms != null && wasJms.booleanValue() == isJms)
        // no change
            return;
        wasJms = Boolean.valueOf(isJms);

        boolean enableHttp = routeModifiable && !isJms;
        routeToUrlField.setEditable(enableHttp);
        userRouteField.setEditable(enableHttp);
        passwordRouteField.setEditable(enableHttp);
        realmRouteField.setEditable(enableHttp);
        defaultUrlButton.setEnabled(enableHttp);

        if (enableHttp) {
            routeToUrlField.setForeground(Color.BLACK);
            userRouteField.setForeground(Color.BLACK);         
            passwordRouteField.setForeground(Color.BLACK);
            realmRouteField.setForeground(Color.BLACK);
        } else {
            routeToUrlField.setForeground(Color.GRAY);
            userRouteField.setForeground(Color.GRAY);
            passwordRouteField.setForeground(Color.GRAY);
            realmRouteField.setForeground(Color.GRAY);
        }

        boolean enableJms = routeModifiable && isJms;
        jmsQueueComboBox.setEnabled(enableJms);
    }

    private DocumentListener wasRouteEditedListener = new DocumentListener() {
        public void changedUpdate(DocumentEvent e) {
            routeEdited = true;
        }

        public void insertUpdate(DocumentEvent e) {
            routeEdited = true;
        }

        public void removeUpdate(DocumentEvent e) {
            routeEdited = true;
        }
    };


    private ActionListener updateIdentityPolicy = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            List replaceAssertions = new ArrayList();
            List removeAssertions = new ArrayList();
            List addAssertions = new ArrayList();

            // Bugzilla #725 - the handler updates the policy with the assertions in reverse order.
            // Here we must add the assertions to the list in the following order
            // a. routing assertion, b. credential assertion, c. SSL Transport assertion
            RoutingAssertion newRoutingAssertion = collectRoutingAssertion();
            if (newRoutingAssertion != null) {
                if (existingRoutingAssertion != null) {
                    replaceAssertions.add(new Assertion[]{existingRoutingAssertion, newRoutingAssertion});
                } else {
                    addAssertions.add(newRoutingAssertion);
                }
            }

            CredentialSourceAssertion newCredAssertion = collectCredentialsAssertion();
            if (newCredAssertion != null) {
                if (existingCredAssertion != null) {
                    replaceAssertions.add(new Assertion[]{existingCredAssertion, newCredAssertion});
                } else {
                    addAssertions.add(newCredAssertion);
                }
            }

            if (sslCheckBox.isEnabled()) { // edit allowed
                if (sslCheckBox.isSelected()) { // selected
                    if (sslAssertion == null) {
                        addAssertions.add(new SslAssertion());
                    }
                } else {
                    if (sslAssertion != null) {
                        removeAssertions.add(sslAssertion);
                    }
                }
            }

            for (Iterator iterator = replaceAssertions.iterator(); iterator.hasNext();) {
                Assertion[] assertions = (Assertion[]) iterator.next();
                replaceAssertion(assertions[0], assertions[1]);
            }
            final Assertion[] aa = (Assertion[]) addAssertions.toArray(new Assertion[]{});
            final Assertion[] ar = (Assertion[]) removeAssertions.toArray(new Assertion[]{});
            addAssertionTreeNodes(aa);
            removeAssertionTreeNodes(ar);
            scheduleValidate();
            exitHandler(e);

        }

        private void removeAssertionTreeNodes(Assertion[] assertions) {
            List nodes = Arrays.asList(assertions);
            List deadNodes = new ArrayList();

            Enumeration e = rootAssertionTreeNode.preorderEnumeration();
            while (e.hasMoreElements()) {
                AssertionTreeNode an = (AssertionTreeNode) e.nextElement();
                if (nodes.contains(an.asAssertion())) {
                    deadNodes.add(an);
                }
            }

            for (Iterator iterator = deadNodes.iterator(); iterator.hasNext();) {
                AssertionTreeNode dead = (AssertionTreeNode) iterator.next();
                policyTreeModel.removeNodeFromParent(dead);
            }
        }

        private void addAssertionTreeNodes(Assertion[] aa) {
            if (aa.length == 0) return;
            final AssertionTreeNode parent = (AssertionTreeNode) identityAssertionNode.getParent();
            int index = policyTreeModel.getIndexOfChild(parent, identityAssertionNode);

            if (parent.asAssertion() instanceof AllAssertion) { // just add
                for (int i = 0; i < aa.length; i++) {
                    policyTreeModel.rawInsertNodeInto(AssertionTreeNodeFactory.asTreeNode(aa[i]), parent, index);
                }
            } else {
                final AssertionTreeNode newParent = AssertionTreeNodeFactory.asTreeNode(new AllAssertion());
                policyTreeModel.rawInsertNodeInto(newParent, parent, index);
                policyTreeModel.removeNodeFromParent(identityAssertionNode);
                policyTreeModel.rawInsertNodeInto(identityAssertionNode, newParent, 0);
                addAssertionTreeNodes(aa);
            }
        }

        private void replaceAssertion(Assertion o, Assertion n) {
            AssertionTreeNode outAssertion = null;

            Enumeration e = rootAssertionTreeNode.preorderEnumeration();

            while (e.hasMoreElements()) {
                AssertionTreeNode an = (AssertionTreeNode) e.nextElement();
                if (an.asAssertion().equals(o)) {
                    outAssertion = an;
                    break;
                }
            }
            if (outAssertion == null) {
                throw new IllegalArgumentException("Cannot find assertion " + o.getClass() + " (bug).");
            }
            final MutableTreeNode parent = (MutableTreeNode) outAssertion.getParent();
            int pos = parent.getIndex(outAssertion);
            policyTreeModel.rawInsertNodeInto(AssertionTreeNodeFactory.asTreeNode(n), parent, pos);
            policyTreeModel.removeNodeFromParent(outAssertion);
        }
    };

    private void scheduleValidate() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                final WorkSpacePanel cw = TopComponents.getInstance().getCurrentWorkspace();
                final JComponent c = cw.getComponent();
                if (c != null && c instanceof PolicyEditorPanel) {
                    PolicyEditorPanel pp = (PolicyEditorPanel) c;
                    pp.validatePolicy();
                }
            }
        });
    }

    private CredentialSourceAssertion collectCredentialsAssertion() {
        if (!authMethodComboBox.isEnabled()) return null;

        Object key = authMethodComboBox.getSelectedItem();
        CredentialSourceAssertion newCredAssertion = (CredentialSourceAssertion) credentialsLocationMap.get(key);
        // TODO verify support for XML signing/encryption
        return newCredAssertion;
    }

    private RoutingAssertion collectRoutingAssertion() {
        if (!routeModifiable || !routeEdited)
            return null;
        if (jmsRoutingRadioButton.isSelected()) {
            JmsRoutingAssertion jmsRa = new JmsRoutingAssertion();
            JmsUtilities.QueueItem selected = (JmsUtilities.QueueItem) jmsQueueComboBox.getSelectedItem();
            if (selected != null) {
                final JmsEndpoint endpoint = selected.getQueue().getEndpoint();
                jmsRa.setEndpointOid(new Long(endpoint.getOid()));
                jmsRa.setEndpointName(endpoint.getName());
            }
            return jmsRa;
        } else if (httpRoutingRadioButton.isSelected()) {
            HttpRoutingAssertion httpRa = new HttpRoutingAssertion();
            httpRa.setLogin(userRouteField.getText());
            httpRa.setProtectedServiceUrl(routeToUrlField.getText());
            httpRa.setPassword(new String(passwordRouteField.getPassword()));
            httpRa.setRealm(realmRouteField.getText());
            return httpRa;
        } else
            throw new IllegalStateException("Neither type of routin assertion is selected");
    }


    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(4, 1, new Insets(5, 5, 5, 5), -1, -1));
        final JTabbedPane tabbedPane1 = new JTabbedPane();
        mainPanel.add(tabbedPane1, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, new Dimension(200, 200), null));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 2, new Insets(10, 0, 0, 0), -1, -1));
        tabbedPane1.addTab("Authentication", panel1);
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(3, 5, new Insets(5, 10, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("XML Security options:");
        panel2.add(label1, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        sslCheckBox = new JCheckBox();
        sslCheckBox.setContentAreaFilled(true);
        sslCheckBox.setHorizontalTextPosition(10);
        sslCheckBox.setMargin(new Insets(2, 2, 2, 0));
        sslCheckBox.setText("Require SSL/TLS encryption");
        panel2.add(sslCheckBox, new GridConstraints(0, 0, 1, 2, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label2 = new JLabel();
        label2.setText("Authentication method");
        panel2.add(label2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        xmlSecOptions = new JComboBox();
        panel2.add(xmlSecOptions, new GridConstraints(1, 3, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        authMethodComboBox = new JComboBox();
        panel2.add(authMethodComboBox, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer2 = new Spacer();
        panel2.add(spacer2, new GridConstraints(2, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(10, -1), null, null));
        final Spacer spacer3 = new Spacer();
        panel2.add(spacer3, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
        final Spacer spacer4 = new Spacer();
        panel2.add(spacer4, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(50, -1), null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(7, 6, new Insets(10, 10, 0, 0), -1, -1));
        tabbedPane1.addTab("Routing", panel3);
        final JLabel label3 = new JLabel();
        label3.setText("Service URL");
        panel3.add(label3, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        routeToUrlField = new JTextField();
        panel3.add(routeToUrlField, new GridConstraints(1, 2, 1, 2, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, new Dimension(200, -1), new Dimension(150, -1), null));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(3, 2, new Insets(5, 0, 5, 0), -1, -1));
        panel3.add(panel4, new GridConstraints(3, 1, 1, 2, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        panel4.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Protected service authentication"));
        final JLabel label4 = new JLabel();
        label4.setText("Identity");
        panel4.add(label4, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label5 = new JLabel();
        label5.setText("Password");
        panel4.add(label5, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label6 = new JLabel();
        label6.setText("Realm (optional)");
        panel4.add(label6, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        userRouteField = new JTextField();
        panel4.add(userRouteField, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        realmRouteField = new JTextField();
        panel4.add(realmRouteField, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        passwordRouteField = new JPasswordField();
        panel4.add(passwordRouteField, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(150, -1), null));
        final Spacer spacer5 = new Spacer();
        panel3.add(spacer5, new GridConstraints(3, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
        defaultUrlButton = new JButton();
        defaultUrlButton.setLabel("Default");
        defaultUrlButton.setText("Default");
        panel3.add(defaultUrlButton, new GridConstraints(1, 4, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer6 = new Spacer();
        panel3.add(spacer6, new GridConstraints(1, 5, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(5, -1), null, null));
        final Spacer spacer7 = new Spacer();
        panel3.add(spacer7, new GridConstraints(2, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        httpRoutingRadioButton = new JRadioButton();
        httpRoutingRadioButton.setText("Route outgoing using HTTP:");
        panel3.add(httpRoutingRadioButton, new GridConstraints(0, 0, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        jmsRoutingRadioButton = new JRadioButton();
        jmsRoutingRadioButton.setText("Route outgoing using JMS:");
        panel3.add(jmsRoutingRadioButton, new GridConstraints(4, 0, 1, 5, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer8 = new Spacer();
        panel3.add(spacer8, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, new Dimension(40, -1), null));
        jmsQueueComboBox = new JComboBox();
        panel3.add(jmsQueueComboBox, new GridConstraints(5, 2, 1, 3, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label7 = new JLabel();
        label7.setText("JMS Queue:");
        panel3.add(label7, new GridConstraints(5, 1, 1, 1, GridConstraints.ANCHOR_EAST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer9 = new Spacer();
        panel3.add(spacer9, new GridConstraints(6, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(-1, 5), null, null));
        final Spacer spacer10 = new Spacer();
        mainPanel.add(spacer10, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(1, 4, new Insets(5, 0, 0, 0), -1, -1));
        mainPanel.add(panel5, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        helpButton = new JButton();
        helpButton.setText("Help");
        panel5.add(helpButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        panel5.add(cancelButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        okButton = new JButton();
        okButton.setText("OK");
        panel5.add(okButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer11 = new Spacer();
        panel5.add(spacer11, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
    }
}
