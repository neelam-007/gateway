package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.console.action.Actions;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.JmsUtilities;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.policy.assertion.xmlsec.XmlRequestSecurity;
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
 * identity. For exmaple if asseritons are shared with other identites
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

    private PolicyTreeModel policyTreeModel;
    private AssertionTreeNode rootAssertionTreeNode;
    private static final String[] XML_SEC_OPTIONS = new String[]{"sign only", "sign and encrypt"};

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
        this.policyTreeModel = (PolicyTreeModel)model;
        this.identityAssertionNode = identityAssertionNode;
        this.principal = IdentityPath.extractIdentity(identityAssertionNode.asAssertion());
        rootAssertionTreeNode = (AssertionTreeNode)identityAssertionNode.getRoot();
        this.rootAssertion = rootAssertionTreeNode.asAssertion();
        this.initialize();
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
    }

    private void initialize() {
        {
            ButtonGroup routingProtocolButtonGroup = new ButtonGroup();
            routingProtocolButtonGroup.add(jmsRoutingRadioButton);
            routingProtocolButtonGroup.add(httpRoutingRadioButton);
            ActionListener routingRadioButtonListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    routeEdited = true;
                    enableOrDisableRoutingControls();
                }
            };
            httpRoutingRadioButton.addActionListener(routingRadioButtonListener);
            jmsRoutingRadioButton.addActionListener(routingRadioButtonListener);
            jmsQueueComboBox.setModel(new DefaultComboBoxModel(JmsUtilities.loadQueueItems()));
            jmsQueueComboBox.addActionListener(routingRadioButtonListener);
        }
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exitHandler(e);
            }
        });
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(IdentityPolicyPanel.this);
            }
        });
        okButton.addActionListener(updateIdentityPolicy);
        Utilities.equalizeButtonSizes(new JButton[]{cancelButton, okButton, helpButton});
        authMethodComboBox.setModel(Components.getCredentialsLocationComboBoxModelNonAnonymous());
        authMethodComboBox.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                Object key = e.getItem();
                CredentialSourceAssertion ca =
                        (CredentialSourceAssertion)Components.getCredentialsLocationMap().get(key);
                xmlSecOptions.setEnabled(ca instanceof XmlRequestSecurity);
            }
        });
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
            IdentityPath ip = (IdentityPath)iterator.next();
            if (ip.getPrincipal().equals(principalAssertionPaths.getPrincipal())) {
                remove.add(ip);
            }
        }
        otherPaths.removeAll(remove);

        routeToUrlField.getDocument().addDocumentListener(wasRouteEditedListener);
        userRouteField.getDocument().addDocumentListener(wasRouteEditedListener);
        passwordRouteField.getDocument().addDocumentListener(wasRouteEditedListener);
        realmRouteField.getDocument().addDocumentListener(wasRouteEditedListener);
        Utilities.enableGrayOnDisabled(passwordRouteField);

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
            IdentityPath ip = (IdentityPath)iterator.next();
            othersSslAssertions.addAll(ip.getEqualAssertions(SslAssertion.class));
        }

        Set principalSslAssertions = principalAssertionPaths.getEqualAssertions(SslAssertion.class);
        selected = !principalSslAssertions.isEmpty();
        for (Iterator iterator = principalSslAssertions.iterator(); iterator.hasNext();) {
            sslAssertion = (SslAssertion)iterator.next();
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
            IdentityPath ip = (IdentityPath)iterator.next();
            othersCredAssertions.addAll(ip.getAssignableAssertions(CredentialSourceAssertion.class));
        }

        Set principalCredAssertions =
                principalAssertionPaths.getAssignableAssertions(CredentialSourceAssertion.class);
        for (Iterator it = principalCredAssertions.iterator(); it.hasNext();) {
            existingCredAssertion = (CredentialSourceAssertion)it.next();
            selectAuthMethodComboItem(existingCredAssertion);
            if (othersCredAssertions.contains(existingCredAssertion)) {
                canmod = false;
            }
        }
        authMethodComboBox.setEnabled(canmod);
    }

    private void selectAuthMethodComboItem(Assertion cas) {
        Set entrySet = Components.getCredentialsLocationMap().entrySet();
        for (Iterator iterator = entrySet.iterator(); iterator.hasNext();) {
            Map.Entry entry = (Map.Entry)iterator.next();
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
            IdentityPath ip = (IdentityPath)iterator.next();
            othersRouteAssertions.addAll(ip.getAssignableAssertions(RoutingAssertion.class));
        }

        Set principalRouteAssertions = principalAssertionPaths.getAssignableAssertions(RoutingAssertion.class);
        for (Iterator it = principalRouteAssertions.iterator(); it.hasNext();) {
            existingRoutingAssertion = (RoutingAssertion)it.next();
            if (existingRoutingAssertion instanceof HttpRoutingAssertion) {
                HttpRoutingAssertion hra = (HttpRoutingAssertion)existingRoutingAssertion;
                routeToUrlField.setText(hra.getProtectedServiceUrl());
                userRouteField.setText(hra.getLogin());
                passwordRouteField.setText(hra.getPassword());
                realmRouteField.setText(hra.getRealm());
            } else if (existingRoutingAssertion instanceof JmsRoutingAssertion) {
                JmsRoutingAssertion jra = (JmsRoutingAssertion)existingRoutingAssertion;
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
        routeToUrlField.setEnabled(enableHttp);
        userRouteField.setEditable(enableHttp);
        passwordRouteField.setEnabled(enableHttp);
        realmRouteField.setEnabled(enableHttp);
        defaultUrlButton.setEnabled(enableHttp);

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
                Assertion[] assertions = (Assertion[])iterator.next();
                replaceAssertion(assertions[0], assertions[1]);
            }
            final Assertion[] aa = (Assertion[])addAssertions.toArray(new Assertion[]{});
            final Assertion[] ar = (Assertion[])removeAssertions.toArray(new Assertion[]{});
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
                AssertionTreeNode an = (AssertionTreeNode)e.nextElement();
                if (nodes.contains(an.asAssertion())) {
                    deadNodes.add(an);
                }
            }

            for (Iterator iterator = deadNodes.iterator(); iterator.hasNext();) {
                AssertionTreeNode dead = (AssertionTreeNode)iterator.next();
                policyTreeModel.removeNodeFromParent(dead);
            }
        }

        private void addAssertionTreeNodes(Assertion[] aa) {
            if (aa.length == 0) return;
            final AssertionTreeNode parent = (AssertionTreeNode)identityAssertionNode.getParent();
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
                AssertionTreeNode an = (AssertionTreeNode)e.nextElement();
                if (an.asAssertion().equals(o)) {
                    outAssertion = an;
                    break;
                }
            }
            if (outAssertion == null) {
                throw new IllegalArgumentException("Cannot find assertion " + o.getClass() + " (bug).");
            }
            final MutableTreeNode parent = (MutableTreeNode)outAssertion.getParent();
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
                    PolicyEditorPanel pp = (PolicyEditorPanel)c;
                    pp.validatePolicy();
                }
            }
        });
    }

    private CredentialSourceAssertion collectCredentialsAssertion() {
        if (!authMethodComboBox.isEnabled()) return null;

        Object key = authMethodComboBox.getSelectedItem();
        CredentialSourceAssertion newCredAssertion = (CredentialSourceAssertion)Components.getCredentialsLocationMap().get(key);
        if (newCredAssertion instanceof XmlRequestSecurity) {
            boolean encrypt = xmlSecOptions.getSelectedItem().equals(XML_SEC_OPTIONS[1]);
            //((XmlRequestSecurity)newCredAssertion).setEncryption(encrypt);
        }
        return newCredAssertion;
    }

    private RoutingAssertion collectRoutingAssertion() {
        if (!routeModifiable || !routeEdited)
            return null;
        if (jmsRoutingRadioButton.isSelected()) {
            JmsRoutingAssertion jmsRa = new JmsRoutingAssertion();
            JmsUtilities.QueueItem selected = (JmsUtilities.QueueItem)jmsQueueComboBox.getSelectedItem();
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
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        final JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 1, new Insets(5, 5, 5, 5), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 1, 1, 3, 3, null, null, null));
        _2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        final JLabel _3;
        _3 = new JLabel();
        _3.setText("Identity Policy");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JTabbedPane _4;
        _4 = new JTabbedPane();
        _1.add(_4, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, new Dimension(200, 200), null));
        final JPanel _5;
        _5 = new JPanel();
        _5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(10, 0, 0, 0), -1, -1));
        _4.addTab("Authentication", _5);
        final JPanel _6;
        _6 = new JPanel();
        _6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 5, new Insets(5, 10, 0, 0), -1, -1));
        _5.add(_6, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 3, 3, 3, null, null, null));
        final JLabel _7;
        _7 = new JLabel();
        _7.setText("XML Security options:");
        _6.add(_7, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 8, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _8;
        _8 = new com.intellij.uiDesigner.core.Spacer();
        _6.add(_8, new com.intellij.uiDesigner.core.GridConstraints(2, 3, 1, 1, 0, 2, 1, 6, null, null, null));
        final JCheckBox _9;
        _9 = new JCheckBox();
        sslCheckBox = _9;
        _9.setHorizontalTextPosition(10);
        _9.setText("Require SSL/TLS encryption");
        _9.setMargin(new Insets(2, 2, 2, 0));
        _9.setContentAreaFilled(true);
        _6.add(_9, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, 4, 0, 3, 0, null, null, null));
        final JLabel _10;
        _10 = new JLabel();
        _10.setText("Authentication method");
        _6.add(_10, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 4, 0, 0, 0, null, null, null));
        final JComboBox _11;
        _11 = new JComboBox();
        xmlSecOptions = _11;
        _6.add(_11, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, 8, 1, 2, 0, null, null, null));
        final JComboBox _12;
        _12 = new JComboBox();
        authMethodComboBox = _12;
        _6.add(_12, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 2, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _13;
        _13 = new com.intellij.uiDesigner.core.Spacer();
        _6.add(_13, new com.intellij.uiDesigner.core.GridConstraints(2, 4, 1, 1, 0, 1, 6, 1, new Dimension(10, -1), null, null));
        final com.intellij.uiDesigner.core.Spacer _14;
        _14 = new com.intellij.uiDesigner.core.Spacer();
        _6.add(_14, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, 0, 1, 6, 1, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _15;
        _15 = new com.intellij.uiDesigner.core.Spacer();
        _6.add(_15, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 0, 1, 6, 1, new Dimension(50, -1), null, null));
        final JPanel _16;
        _16 = new JPanel();
        _16.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(7, 6, new Insets(10, 10, 0, 0), -1, -1));
        _4.addTab("Routing", _16);
        final JLabel _17;
        _17 = new JLabel();
        _17.setText("Service URL");
        _16.add(_17, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 4, 0, 0, 0, null, null, null));
        final JTextField _18;
        _18 = new JTextField();
        routeToUrlField = _18;
        _16.add(_18, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 2, 8, 1, 6, 0, new Dimension(200, -1), new Dimension(150, -1), null));
        final JPanel _19;
        _19 = new JPanel();
        _19.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 2, new Insets(5, 0, 5, 0), -1, -1));
        _16.add(_19, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 2, 0, 3, 3, 3, null, null, null));
        _19.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Protected service authentication"));
        final JLabel _20;
        _20 = new JLabel();
        _20.setText("Identity");
        _19.add(_20, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 4, 0, 0, 0, null, null, null));
        final JLabel _21;
        _21 = new JLabel();
        _21.setText("Password");
        _19.add(_21, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 4, 0, 0, 0, null, null, null));
        final JLabel _22;
        _22 = new JLabel();
        _22.setText("Realm (optional)");
        _19.add(_22, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 4, 0, 0, 0, null, null, null));
        final JTextField _23;
        _23 = new JTextField();
        userRouteField = _23;
        _19.add(_23, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JTextField _24;
        _24 = new JTextField();
        realmRouteField = _24;
        _19.add(_24, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final JPasswordField _25;
        _25 = new JPasswordField();
        passwordRouteField = _25;
        _19.add(_25, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 6, 0, null, new Dimension(150, -1), null));
        final com.intellij.uiDesigner.core.Spacer _26;
        _26 = new com.intellij.uiDesigner.core.Spacer();
        _16.add(_26, new com.intellij.uiDesigner.core.GridConstraints(3, 3, 1, 1, 0, 1, 6, 1, null, null, null));
        final JButton _27;
        _27 = new JButton();
        defaultUrlButton = _27;
        _27.setLabel("Default");
        _27.setText("Default");
        _16.add(_27, new com.intellij.uiDesigner.core.GridConstraints(1, 4, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _28;
        _28 = new com.intellij.uiDesigner.core.Spacer();
        _16.add(_28, new com.intellij.uiDesigner.core.GridConstraints(1, 5, 1, 1, 0, 1, 6, 1, new Dimension(5, -1), null, null));
        final com.intellij.uiDesigner.core.Spacer _29;
        _29 = new com.intellij.uiDesigner.core.Spacer();
        _16.add(_29, new com.intellij.uiDesigner.core.GridConstraints(2, 3, 1, 1, 0, 2, 1, 6, null, null, null));
        final JRadioButton _30;
        _30 = new JRadioButton();
        httpRoutingRadioButton = _30;
        _30.setText("Route outgoing using HTTP:");
        _16.add(_30, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 5, 8, 0, 3, 0, null, null, null));
        final JRadioButton _31;
        _31 = new JRadioButton();
        jmsRoutingRadioButton = _31;
        _31.setText("Route outgoing using JMS:");
        _16.add(_31, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 5, 8, 0, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _32;
        _32 = new com.intellij.uiDesigner.core.Spacer();
        _16.add(_32, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 1, 6, 1, null, new Dimension(40, -1), null));
        final JComboBox _33;
        _33 = new JComboBox();
        jmsQueueComboBox = _33;
        _16.add(_33, new com.intellij.uiDesigner.core.GridConstraints(5, 2, 1, 3, 8, 1, 2, 0, null, null, null));
        final JLabel _34;
        _34 = new JLabel();
        _34.setText("JMS Queue:");
        _16.add(_34, new com.intellij.uiDesigner.core.GridConstraints(5, 1, 1, 1, 4, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _35;
        _35 = new com.intellij.uiDesigner.core.Spacer();
        _16.add(_35, new com.intellij.uiDesigner.core.GridConstraints(6, 2, 1, 1, 0, 2, 1, 6, new Dimension(-1, 5), null, null));
        final com.intellij.uiDesigner.core.Spacer _36;
        _36 = new com.intellij.uiDesigner.core.Spacer();
        _1.add(_36, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JPanel _37;
        _37 = new JPanel();
        _37.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 4, new Insets(5, 0, 0, 0), -1, -1));
        _1.add(_37, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _38;
        _38 = new JButton();
        helpButton = _38;
        _38.setText("Help");
        _37.add(_38, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _39;
        _39 = new JButton();
        cancelButton = _39;
        _39.setText("Cancel");
        _37.add(_39, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _40;
        _40 = new JButton();
        okButton = _40;
        _40.setText("Ok");
        _37.add(_40, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _41;
        _41 = new com.intellij.uiDesigner.core.Spacer();
        _37.add(_41, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 6, 1, null, null, null));
    }


}
