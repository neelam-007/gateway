package com.l7tech.console.panels;

import com.l7tech.service.PublishedService;

import javax.swing.*;
import java.security.Principal;

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
    private PublishedService service;
    private Principal principal;

    /**
     * Create the identity policy panel for a given principal and serviec
     *
     * @param service the service
     * @param principal the principal that the identity policy panel is
     *        created for
     */
    public IdentityPolicyPanel(PublishedService service, Principal principal) {
        super();
        if (service == null || principal == null) {
            throw new IllegalArgumentException();
        }
        this.service = service;
        this.principal = principal;
    }

    {
// do not edit this generated initializer!!! do not add your code here!!!
        $$$setupUI$$$();
    }

    /** generated code, do not edit or call this method manually !!! */
    private void $$$setupUI$$$() {
        JPanel _1;
        _1 = new JPanel();
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 1, new java.awt.Insets(0, 0, 0, 0), -1, -1));
        JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new java.awt.Insets(10, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 1, 1, 3, 3, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        _2.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        JLabel _3;
        _3 = new JLabel();
        _3.setText("Identity Policy");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JTabbedPane _4;
        _4 = new JTabbedPane();
        _1.add(_4, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, new java.awt.Dimension(-1, -1), new java.awt.Dimension(200, 200), new java.awt.Dimension(-1, -1)));
        JPanel _5;
        _5 = new JPanel();
        _5.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 2, new java.awt.Insets(0, 0, 0, 0), -1, -1));
        _4.addTab("Authentication", _5);
        JPanel _6;
        _6 = new JPanel();
        _6.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 2, new java.awt.Insets(0, 10, 0, 0), -1, -1));
        _5.add(_6, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 2, 1, 0, 3, 3, 3, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JCheckBox _7;
        _7 = new JCheckBox();
        _7.setHorizontalTextPosition(10);
        _7.setText("Require SSL/TLS encryption");
        _6.add(_7, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 2, 8, 0, 3, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JLabel _8;
        _8 = new JLabel();
        _8.setText("Authentication method:");
        _6.add(_8, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 0, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JComboBox _9;
        _9 = new JComboBox();
        _6.add(_9, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 2, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _10;
        _10 = new com.intellij.uiDesigner.core.Spacer();
        _6.add(_10, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 2, 1, 6, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JPanel _11;
        _11 = new JPanel();
        _11.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 2, new java.awt.Insets(0, 10, 0, 0), -1, -1));
        _5.add(_11, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 3, 3, 3, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JLabel _12;
        _12 = new JLabel();
        _12.setText("Please Select the XML Security options:");
        _11.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JComboBox _13;
        _13 = new JComboBox();
        _11.add(_13, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 1, 2, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _14;
        _14 = new com.intellij.uiDesigner.core.Spacer();
        _11.add(_14, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 2, 1, 6, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _15;
        _15 = new com.intellij.uiDesigner.core.Spacer();
        _11.add(_15, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, 0, 1, 6, 1, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JPanel _16;
        _16 = new JPanel();
        _16.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 4, new java.awt.Insets(10, 10, 0, 0), -1, -1));
        _4.addTab("Routing", _16);
        JLabel _17;
        _17 = new JLabel();
        _17.setText("Protected service authentication");
        _16.add(_17, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 2, 8, 0, 0, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JLabel _18;
        _18 = new JLabel();
        _18.setText("Service URL:");
        _16.add(_18, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JTextField _19;
        _19 = new JTextField();
        _16.add(_19, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 2, 8, 1, 6, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(150, -1), new java.awt.Dimension(-1, -1)));
        JPanel _20;
        _20 = new JPanel();
        _20.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 2, new java.awt.Insets(0, 0, 0, 0), -1, -1));
        _16.add(_20, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 2, 0, 3, 3, 3, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JLabel _21;
        _21 = new JLabel();
        _21.setText("Identity");
        _20.add(_21, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 4, 0, 0, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JLabel _22;
        _22 = new JLabel();
        _22.setText("Password");
        _20.add(_22, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 4, 0, 0, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JLabel _23;
        _23 = new JLabel();
        _23.setText("Realm");
        _20.add(_23, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 4, 0, 0, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JTextField _24;
        _24 = new JTextField();
        _20.add(_24, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 8, 1, 6, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(150, -1), new java.awt.Dimension(-1, -1)));
        JTextField _25;
        _25 = new JTextField();
        _20.add(_25, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 1, 6, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(150, -1), new java.awt.Dimension(-1, -1)));
        JTextField _26;
        _26 = new JTextField();
        _20.add(_26, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, 8, 1, 6, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(150, -1), new java.awt.Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _27;
        _27 = new com.intellij.uiDesigner.core.Spacer();
        _16.add(_27, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 0, 1, 6, 1, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _28;
        _28 = new com.intellij.uiDesigner.core.Spacer();
        _16.add(_28, new com.intellij.uiDesigner.core.GridConstraints(2, 2, 1, 1, 0, 1, 6, 1, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _29;
        _29 = new com.intellij.uiDesigner.core.Spacer();
        _1.add(_29, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 2, 1, 6, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JPanel _30;
        _30 = new JPanel();
        _30.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 4, new java.awt.Insets(0, 0, 0, 0), -1, -1));
        _1.add(_30, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 3, 3, 3, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JButton _31;
        _31 = new JButton();
        _31.setText("Help");
        _30.add(_31, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 0, 1, 3, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JButton _32;
        _32 = new JButton();
        _32.setText("Cancel");
        _30.add(_32, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        JButton _33;
        _33 = new JButton();
        _33.setText("Ok");
        _30.add(_33, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 3, 0, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
        com.intellij.uiDesigner.core.Spacer _34;
        _34 = new com.intellij.uiDesigner.core.Spacer();
        _30.add(_34, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 6, 1, new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1), new java.awt.Dimension(-1, -1)));
    }

}
