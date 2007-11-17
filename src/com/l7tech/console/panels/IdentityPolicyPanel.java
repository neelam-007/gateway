/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.panels;

import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.console.action.Actions;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.JmsUtilities;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.Identity;
import com.l7tech.policy.PolicyPathBuilderFactory;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.composite.AllAssertion;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.List;

/**
 * The <code>IdentityPolicyPanel</code> is the policy panel that allows
 * editing identity policy.
 * The policy allows editing only the elements that are specific to the
 * identity. For exmaple if assertions are shared with other identites
 * then they cannot be edited.
 */
public class IdentityPolicyPanel extends JPanel {

    private JPanel mainPanel;
    private JButton cancelButton;
    private JButton okButton;
    private JButton helpButton;
    private JButton defaultUrlButton;
    private JComboBox authMethodComboBox;
    private JCheckBox sslCheckBox;
    private JTextField routeToUrlField;
    private JTextField userRouteField;
    private JPasswordField passwordRouteField;
    private JTextField realmRouteField;
    private JRadioButton httpRoutingRadioButton;
    private JRadioButton bridgeRoutingRadioButton;
    private JRadioButton jmsRoutingRadioButton;
    private JComboBox jmsQueueComboBox;
    private JButton configureBridgeRoutingButton;

    private final Identity identity;
    private IdentityPath principalAssertionPaths;
    private Set<IdentityPath> otherPaths;
    private final Assertion rootAssertion;
    private final PublishedService service;
    private final AssertionTreeNode identityAssertionNode;

    private SslAssertion sslAssertion = null;
    private Assertion existingCredAssertion = null;

    private RoutingAssertion existingRoutingAssertion = null;
    private BridgeRoutingAssertion guiBraConfig = new BridgeRoutingAssertion();
    private boolean routeEdited;
    private boolean routeModifiable = true;
    private boolean initializing = false;

    private final PolicyTreeModel policyTreeModel;
    private final AssertionTreeNode rootAssertionTreeNode;
    private final Map<String, Assertion> credentialsLocationMap = CredentialsLocation.newCredentialsLocationMap(true);
    private final PolicyPathBuilderFactory pathBuilderFactory;

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
        this.identity = IdentityPath.extractIdentity(identityAssertionNode.asAssertion());
        this.rootAssertionTreeNode = (AssertionTreeNode) identityAssertionNode.getRoot();
        this.rootAssertion = rootAssertionTreeNode.asAssertion();
        this.pathBuilderFactory = Registry.getDefault().getPolicyPathBuilderFactory();

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
        routingProtocolButtonGroup.add(bridgeRoutingRadioButton);
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
        bridgeRoutingRadioButton.addActionListener(routingRadioButtonListener);
        jmsQueueComboBox.setModel(new DefaultComboBoxModel(JmsUtilities.loadQueueItems()));
        jmsQueueComboBox.addActionListener(routingRadioButtonListener);
        Utilities.enableGrayOnDisabled(configureBridgeRoutingButton);

        configureBridgeRoutingButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Wsdl wsdl;
                try {
                    wsdl = service.parsedWsdl();
                } catch (WSDLException e1) {
                    throw new RuntimeException("Couldn't parse WSDL", e1);
                }
                BridgeRoutingAssertionPropertiesDialog d =
                        new BridgeRoutingAssertionPropertiesDialog(TopComponents.getInstance().getTopParent(),
                                                                   guiBraConfig,
                                service.getPolicy(), wsdl);
                d.setModal(true);
                d.pack();
                Utilities.centerOnScreen(d);
                DialogDisplayer.display(d);
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                exitHandler();
            }
        });
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(SwingUtilities.windowForComponent(IdentityPolicyPanel.this));
            }
        });
        okButton.addActionListener(updateIdentityPolicy);
        Utilities.equalizeButtonSizes(new JButton[]{cancelButton, okButton, helpButton});
        authMethodComboBox.setModel(CredentialsLocation.getCredentialsLocationComboBoxModelNonAnonymous(service.isSoap()));

        defaultUrlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    final URL url = service.serviceUrl();
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
        try {
            principalAssertionPaths = IdentityPath.forIdentity(identity, rootAssertion, pathBuilderFactory);
            otherPaths = IdentityPath.getPaths(rootAssertion, pathBuilderFactory);
        } catch (InterruptedException e) {
            throw new RuntimeException(e); // can't happen here
        } catch (PolicyAssertionException e) {
            throw new RuntimeException(e); // TODO find some better way to handle this
        }
        Collection<IdentityPath> remove = new ArrayList<IdentityPath>();
        for (IdentityPath ip : otherPaths) {
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

    private void exitHandler() {
        Utilities.dispose(Utilities.getRootPaneContainerAncestor(this));
    }

    /**
     * update the ssl assertion control for the identity
     * This updates the assertion elements by analyzing
     * the policy to find out if the assertion can modified
     * and what is the value of it.
     */
    private void updateSslAssertion() {
        boolean canmod = true;
        boolean selected;

        Set<SslAssertion> othersSslAssertions = new HashSet<SslAssertion>();
        for (IdentityPath ip : otherPaths) {
            othersSslAssertions.addAll(ip.getEqualAssertions(SslAssertion.class));
        }

        Set principalSslAssertions = principalAssertionPaths.getEqualAssertions(SslAssertion.class);
        selected = !principalSslAssertions.isEmpty();
        for (Object principalSslAssertion : principalSslAssertions) {
            sslAssertion = (SslAssertion) principalSslAssertion;
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

        Set<Assertion> othersCredAssertions = new HashSet<Assertion>();
        for (IdentityPath ip : otherPaths) {
            othersCredAssertions.addAll(ip.getAssertions(IdentityPath.CREDENTIAL_SOURCE));
        }

        Set<Assertion> principalCredAssertions = principalAssertionPaths.getAssertions(IdentityPath.CREDENTIAL_SOURCE);
        for (Assertion principalCredAssertion : principalCredAssertions) {
            existingCredAssertion = principalCredAssertion;

            selectAuthMethodComboItem(existingCredAssertion);
            if (othersCredAssertions.contains(existingCredAssertion))
                canmod = false;
        }

        authMethodComboBox.setEnabled(canmod);
    }

    private void selectAuthMethodComboItem(Assertion cas) {
        Set<Map.Entry<String, Assertion>> entrySet = credentialsLocationMap.entrySet();
        for (Map.Entry<String, Assertion> entry : entrySet) {
            if (cas.getClass().equals(entry.getValue().getClass())) {
                authMethodComboBox.setSelectedItem(entry.getKey());
                break;
            }
        }
    }

    private void updateRouting() {
        routeModifiable = true;

        Set<Assertion> othersRouteAssertions = new HashSet<Assertion>();
        for (IdentityPath ip : otherPaths) {
            othersRouteAssertions.addAll(ip.getAssignableAssertions(RoutingAssertion.class));
        }

        Set<RoutingAssertion> principalRouteAssertions = principalAssertionPaths.getAssignableAssertions(RoutingAssertion.class);
        for (RoutingAssertion principalRouteAssertion : principalRouteAssertions) {
            existingRoutingAssertion = principalRouteAssertion;
            if (existingRoutingAssertion instanceof BridgeRoutingAssertion) {
                guiBraConfig.copyFrom((BridgeRoutingAssertion) existingRoutingAssertion);
            } else if (existingRoutingAssertion instanceof HttpRoutingAssertion) {
                HttpRoutingAssertion hra = (HttpRoutingAssertion) existingRoutingAssertion;
                routeToUrlField.setText(hra.getProtectedServiceUrl());
                userRouteField.setText(hra.getLogin());
                passwordRouteField.setText(hra.getPassword());
                realmRouteField.setText(hra.getRealm());
                guiBraConfig.setProtectedServiceUrl(hra.getProtectedServiceUrl());
            } else if (existingRoutingAssertion instanceof JmsRoutingAssertion) {
                JmsRoutingAssertion jra = (JmsRoutingAssertion) existingRoutingAssertion;
                JmsUtilities.selectEndpoint(jmsQueueComboBox, jra.getEndpointOid());
            } else {
                // Some routing assertion that either isn't modifiable or didn't exist yet in
                // this version of the code.
                routeModifiable = false;
            }
            if (othersRouteAssertions.contains(existingRoutingAssertion)) {
                routeModifiable = false;
                break;
            }
        }

        boolean isJms = existingRoutingAssertion instanceof JmsRoutingAssertion;
        if (!isJms)
            jmsQueueComboBox.setSelectedIndex(-1);
        boolean isBridge = existingRoutingAssertion instanceof BridgeRoutingAssertion;
        boolean isHttp = existingRoutingAssertion instanceof HttpRoutingAssertion && !isBridge;
        jmsRoutingRadioButton.setSelected(isJms);
        jmsRoutingRadioButton.setEnabled(routeModifiable);
        httpRoutingRadioButton.setSelected(isHttp);
        httpRoutingRadioButton.setEnabled(routeModifiable);
        bridgeRoutingRadioButton.setSelected(isBridge);
        bridgeRoutingRadioButton.setEnabled(routeModifiable);
        enableOrDisableRoutingControls();
    }

    private void enableOrDisableRoutingControls() {
        boolean isJms = jmsRoutingRadioButton.isSelected();
        boolean isHttp = httpRoutingRadioButton.isSelected();
        boolean isBridge = bridgeRoutingRadioButton.isSelected();

        boolean enableHttp = routeModifiable && isHttp;
        boolean enableJms = routeModifiable && isJms;
        boolean enableBridge = routeModifiable && isBridge;

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

        jmsQueueComboBox.setEnabled(enableJms);
        configureBridgeRoutingButton.setEnabled(enableBridge);
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
            List<Assertion[]> replaceAssertions = new ArrayList<Assertion[]>();
            List<Assertion> removeAssertions = new ArrayList<Assertion>();
            List<Assertion> addAssertions = new ArrayList<Assertion>();

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

            Assertion newCredAssertion = collectCredentialsAssertion();
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

            for (Assertion[] assertions : replaceAssertions) {
                replaceAssertion(assertions[0], assertions[1]);
            }
            final Assertion[] aa = addAssertions.toArray(new Assertion[]{});
            final Assertion[] ar = removeAssertions.toArray(new Assertion[]{});
            addAssertionTreeNodes(aa);
            removeAssertionTreeNodes(ar);
            scheduleValidate();
            exitHandler();

        }

        private void removeAssertionTreeNodes(Assertion[] assertions) {
            List<Assertion> nodes = Arrays.asList(assertions);
            List<AssertionTreeNode> deadNodes = new ArrayList<AssertionTreeNode>();

            Enumeration e = rootAssertionTreeNode.preorderEnumeration();
            while (e.hasMoreElements()) {
                AssertionTreeNode an = (AssertionTreeNode) e.nextElement();
                if (nodes.contains(an.asAssertion())) {
                    deadNodes.add(an);
                }
            }

            for (AssertionTreeNode dead : deadNodes) {
                policyTreeModel.removeNodeFromParent(dead);
            }
        }

        private void addAssertionTreeNodes(Assertion[] aa) {
            if (aa.length == 0) return;
            final AssertionTreeNode parent = (AssertionTreeNode) identityAssertionNode.getParent();
            int index = policyTreeModel.getIndexOfChild(parent, identityAssertionNode);

            if (parent.asAssertion() instanceof AllAssertion) { // just add
                for (Assertion a : aa) {
                    policyTreeModel.rawInsertNodeInto(AssertionTreeNodeFactory.asTreeNode(a), parent, index);
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

    private Assertion collectCredentialsAssertion() {
        if (!authMethodComboBox.isEnabled()) return null;

        String key = (String) authMethodComboBox.getSelectedItem();
        // TODO verify support for XML signing/encryption
        return credentialsLocationMap.get(key);
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
        } else if (bridgeRoutingRadioButton.isSelected()) {
            BridgeRoutingAssertion bra = new BridgeRoutingAssertion();
            bra.copyFrom(guiBraConfig);
            final String psurl = guiBraConfig.getProtectedServiceUrl();
            if (psurl == null || psurl.length() < 1) {
                try {
                    bra.setProtectedServiceUrl(service.serviceUrl().toExternalForm());
                } catch (WSDLException e) {
                    // todo warn user
                } catch (MalformedURLException e) {
                    // todo warn user
                }
            }
            return bra;
        } else
            throw new IllegalStateException("Unknown type of routing assertion is selected");
    }


}
