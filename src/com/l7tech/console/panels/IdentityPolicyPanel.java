package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.console.action.Actions;
import com.l7tech.console.poleditor.PolicyEditorPanel;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.util.JmsUtilities;
import com.l7tech.console.util.TopComponents;
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
    private Assertion existingCredAssertion = null;

    private RoutingAssertion existingRoutingAssertion = null;
    private boolean routeEdited;
    private boolean routeModifiable = true;
    private boolean initializing = false;

    private PolicyTreeModel policyTreeModel;
    private AssertionTreeNode rootAssertionTreeNode;
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
            othersCredAssertions.addAll(ip.getAssertions(IdentityPath.CREDENTIAL_SOURCE));
        }

        Set principalCredAssertions = principalAssertionPaths.getAssertions(IdentityPath.CREDENTIAL_SOURCE);
        for (Iterator it = principalCredAssertions.iterator(); it.hasNext();) {
            Assertion credAssertion = (Assertion)it.next();

            existingCredAssertion = credAssertion;

            selectAuthMethodComboItem(existingCredAssertion);
            if (othersCredAssertions.contains(existingCredAssertion))
                canmod = false;

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

    private Assertion collectCredentialsAssertion() {
        if (!authMethodComboBox.isEnabled()) return null;

        Object key = authMethodComboBox.getSelectedItem();
        Assertion newCredAssertion = (Assertion) credentialsLocationMap.get(key);
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


}
