package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.action.Actions;
import com.l7tech.console.tree.policy.IdentityPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RoutingAssertion;
import com.l7tech.policy.assertion.SslAssertion;
import com.l7tech.policy.assertion.credential.CredentialSourceAssertion;
import com.l7tech.service.PublishedService;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.security.Principal;
import java.util.*;

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

    private Principal principal;
    private IdentityPath principalAssertionPaths;
    private Set otherPaths;
    private Assertion rootAssertion;
    private PublishedService service;

    /**
     * Create the identity policy panel for a given principal and serviec
     *
     * @param service the service
     * @param principal the principal that the identity policy panel is
     *        created for
     */
    public IdentityPolicyPanel(PublishedService service, Assertion rootAssertion, Principal principal) {
        super();
        if (service == null || principal == null) {
            throw new IllegalArgumentException();
        }
        this.service = service;
        this.principal = principal;
        this.rootAssertion = rootAssertion;
        initialize();
        setLayout(new BorderLayout());
        /** Set content pane */
        add(mainPanel, BorderLayout.CENTER);
    }

    private void initialize() {
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Window w =
                  SwingUtilities.windowForComponent(IdentityPolicyPanel.this);
                w.dispose();
            }
        });
        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(IdentityPolicyPanel.this);
            }
        });
        Utilities.equalizeButtonSizes(new JButton[]{cancelButton, okButton, helpButton});
        authMethodComboBox.setModel(Components.getCredentialsLocationComboBoxModelNonAnonymous());

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

        updateSslAssertion();
        updateAuthMethod();
        updateRouting();
    }

    /**
     * update the ssl assertion control for the identity
     * This updates the asserion elements by analyzing
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
            Assertion assertion = (Assertion)iterator.next();
            if (othersSslAssertions.contains(assertion)) {
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
            Assertion cas = (Assertion)it.next();
            selectAuthMethodComboItem(cas);
            if (othersCredAssertions.contains(cas)) {
                canmod = false;
            }
        }

        authMethodComboBox.setEnabled(canmod);
        xmlSecOptions.setEnabled(canmod);
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
        boolean routeModifiable = true;

        Set othersRouteAssertions = new HashSet();
        for (Iterator iterator = otherPaths.iterator(); iterator.hasNext();) {
            IdentityPath ip = (IdentityPath)iterator.next();
            othersRouteAssertions.addAll(ip.getEqualAssertions(RoutingAssertion.class));
        }

        Set principalRouteAssertions =
          principalAssertionPaths.getEqualAssertions(RoutingAssertion.class);
        for (Iterator it = principalRouteAssertions.iterator(); it.hasNext();) {
            RoutingAssertion ra = (RoutingAssertion)it.next();
            routeToUrlField.setText(ra.getProtectedServiceUrl());
            userRouteField.setText(ra.getLogin());
            passwordRouteField.setText(ra.getPassword());
            realmRouteField.setText(ra.getRealm());
            if (othersRouteAssertions.contains(ra)) {
                routeModifiable = false;
                break;
            }
        }

        routeToUrlField.setEnabled(routeModifiable);
        userRouteField.setEditable(routeModifiable);
        passwordRouteField.setEnabled(routeModifiable);
        realmRouteField.setEnabled(routeModifiable);
        defaultUrlButton.setEnabled(routeModifiable);
    }


    {
// do not edit this generated initializer!!! do not add your code here!!!
        $$$setupUI$$$();
    }

    /** generated code, do not edit or call this method manually !!! */
    private void $$$setupUI$$$() {
        JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 1, new Insets(5, 5, 5, 5), -1, -1));
        JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 1, 1, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        _2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        JLabel _3;
        _3 = new JLabel();
        _3.setText("Identity Policy");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTabbedPane _4;
        _4 = new JTabbedPane();
        _1.add(_4, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(200, 200), new Dimension(-1, -1)));
        JPanel _5;
        _5 = new JPanel();
        _5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new Insets(10, 0, 0, 0), -1, -1));
        _4.addTab("Authentication", _5);
        JPanel _6;
        _6 = new JPanel();
        _6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 5, new Insets(5, 10, 0, 0), -1, -1));
        _5.add(_6, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _7;
        _7 = new JLabel();
        _7.setText("XML Security options:");
        _6.add(_7, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 8, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _8;
        _8 = new com.intellij.uiDesigner.core.Spacer();
        _6.add(_8, new com.intellij.uiDesigner.core.GridConstraints(2, 3, 1, 1, 0, 2, 1, 6, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JCheckBox _9;
        _9 = new JCheckBox();
        sslCheckBox = _9;
        _9.setContentAreaFilled(true);
        _9.setHorizontalTextPosition(10);
        _9.setMargin(new Insets(2, 2, 2, 0));
        _9.setText("Require SSL/TLS encryption");
        _6.add(_9, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, 4, 0, 3, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _10;
        _10 = new JLabel();
        _10.setText("Authentication method");
        _6.add(_10, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 4, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JComboBox _11;
        _11 = new JComboBox();
        xmlSecOptions = _11;
        _6.add(_11, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, 8, 1, 2, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JComboBox _12;
        _12 = new JComboBox();
        authMethodComboBox = _12;
        _6.add(_12, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 2, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _13;
        _13 = new com.intellij.uiDesigner.core.Spacer();
        _6.add(_13, new com.intellij.uiDesigner.core.GridConstraints(2, 4, 1, 1, 0, 1, 6, 1, new Dimension(10, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _14;
        _14 = new com.intellij.uiDesigner.core.Spacer();
        _6.add(_14, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, 0, 1, 6, 1, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _15;
        _15 = new com.intellij.uiDesigner.core.Spacer();
        _6.add(_15, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 0, 1, 6, 1, new Dimension(50, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JPanel _16;
        _16 = new JPanel();
        _16.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 5, new Insets(10, 10, 0, 0), -1, -1));
        _4.addTab("Routing", _16);
        JLabel _17;
        _17 = new JLabel();
        _17.setText("Service URL");
        _16.add(_17, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 4, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _18;
        _18 = new JTextField();
        routeToUrlField = _18;
        _16.add(_18, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
        JPanel _19;
        _19 = new JPanel();
        _19.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 2, new Insets(5, 0, 5, 0), -1, -1));
        _16.add(_19, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 2, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        _19.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Protected service authentication"));
        JLabel _20;
        _20 = new JLabel();
        _20.setText("Identity");
        _19.add(_20, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 4, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _21;
        _21 = new JLabel();
        _21.setText("Password");
        _19.add(_21, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 4, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JLabel _22;
        _22 = new JLabel();
        _22.setText("Realm (optional)");
        _19.add(_22, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 4, 0, 0, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JTextField _23;
        _23 = new JTextField();
        userRouteField = _23;
        _19.add(_23, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
        JTextField _24;
        _24 = new JTextField();
        realmRouteField = _24;
        _19.add(_24, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
        JPasswordField _25;
        _25 = new JPasswordField();
        passwordRouteField = _25;
        _19.add(_25, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 6, 0, new Dimension(-1, -1), new Dimension(150, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _26;
        _26 = new com.intellij.uiDesigner.core.Spacer();
        _16.add(_26, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, 0, 1, 6, 1, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JButton _27;
        _27 = new JButton();
        defaultUrlButton = _27;
        _27.setText("Default");
        _27.setLabel("Default");
        _16.add(_27, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 0, 1, 3, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _28;
        _28 = new com.intellij.uiDesigner.core.Spacer();
        _16.add(_28, new com.intellij.uiDesigner.core.GridConstraints(0, 4, 1, 1, 0, 1, 6, 1, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _29;
        _29 = new com.intellij.uiDesigner.core.Spacer();
        _16.add(_29, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 0, 2, 1, 6, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _30;
        _30 = new com.intellij.uiDesigner.core.Spacer();
        _1.add(_30, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 2, 1, 6, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JPanel _31;
        _31 = new JPanel();
        _31.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 4, new Insets(5, 0, 0, 0), -1, -1));
        _1.add(_31, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 3, 3, 3, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JButton _32;
        _32 = new JButton();
        helpButton = _32;
        _32.setText("Help");
        _31.add(_32, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 0, 1, 3, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JButton _33;
        _33 = new JButton();
        cancelButton = _33;
        _33.setText("Cancel");
        _31.add(_33, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        JButton _34;
        _34 = new JButton();
        okButton = _34;
        _34.setText("Ok");
        _31.add(_34, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 3, 0, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _35;
        _35 = new com.intellij.uiDesigner.core.Spacer();
        _31.add(_35, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 6, 1, new Dimension(-1, -1), new Dimension(-1, -1), new Dimension(-1, -1)));
    }


}
