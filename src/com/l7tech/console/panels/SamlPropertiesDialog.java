package com.l7tech.console.panels;

import com.l7tech.console.tree.policy.SamlTreeNode;
import com.l7tech.console.action.Actions;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

/**
 * Class <code>SamlPropertiesPanel</code> edits the SAML properties.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SamlPropertiesDialog extends JDialog {
    private JPanel mainPanel;
    private JCheckBox requireTimeValidity;
    private JCheckBox requireSignature;
    private JCheckBox requireEncryption;

    private JButton helpButton;
    private JButton okButton;
    private JButton cancelButton;
    private SamlTreeNode samlTreeNode;
    private SamlSecurity samlSecurity;

    public SamlPropertiesDialog(Frame owner, SamlTreeNode n) {
        super(owner, false);
        setTitle("SAML properties");
        samlTreeNode = n;
        samlSecurity = (SamlSecurity)samlTreeNode.asAssertion();
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        requireSignature.setSelected(samlSecurity.isValidateSignature());
        requireEncryption.setEnabled(false);
        requireSignature.setEnabled(false);

        requireTimeValidity.setSelected(samlSecurity.isValidateValidityPeriod());

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                samlSecurity.setValidateSignature(requireSignature.isSelected());
                samlSecurity.setValidateValidityPeriod(requireTimeValidity.isSelected());
                SamlPropertiesDialog.this.dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SamlPropertiesDialog.this.dispose();

            }
        });

        helpButton.addActionListener( new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(SamlPropertiesDialog.this);
            }
        });

    }

    {
// do not edit this generated initializer!!! do not add your code here!!!
        $$$setupUI$$$();
    }

    /**
     * generated code, do not edit or call this method manually !!!
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
        _3.setText("SAML constraints");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final JPanel _4;
        _4 = new JPanel();
        _4.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 4, new Insets(5, 0, 0, 0), -1, -1));
        _1.add(_4, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JButton _5;
        _5 = new JButton();
        helpButton = _5;
        _5.setText("Help");
        _4.add(_5, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _6;
        _6 = new JButton();
        cancelButton = _6;
        _6.setText("Cancel");
        _4.add(_6, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _7;
        _7 = new JButton();
        okButton = _7;
        _7.setText("Ok");
        _4.add(_7, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _8;
        _8 = new com.intellij.uiDesigner.core.Spacer();
        _4.add(_8, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 6, 1, null, null, null));
        final JPanel _9;
        _9 = new JPanel();
        _9.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        _1.add(_9, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 3, 3, 3, null, null, null));
        final JCheckBox _10;
        _10 = new JCheckBox();
        requireTimeValidity = _10;
        _10.setHorizontalTextPosition(11);
        _10.setHorizontalAlignment(10);
        _10.setText("Check time and date on ticket");
        _9.add(_10, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 8, 0, 3, 0, null, null, null));
        final JCheckBox _11;
        _11 = new JCheckBox();
        requireEncryption = _11;
        _11.setHorizontalTextPosition(11);
        _11.setHorizontalAlignment(10);
        _11.setText("Require encrypted ticket");
        _9.add(_11, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, 8, 0, 3, 0, null, null, null));
        final JCheckBox _12;
        _12 = new JCheckBox();
        requireSignature = _12;
        _12.setHorizontalTextPosition(11);
        _12.setText("Require signed ticket");
        _9.add(_12, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, 8, 0, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _13;
        _13 = new com.intellij.uiDesigner.core.Spacer();
        _9.add(_13, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 0, 1, 6, 1, new Dimension(100, -1), new Dimension(100, -1), null));
        final com.intellij.uiDesigner.core.Spacer _14;
        _14 = new com.intellij.uiDesigner.core.Spacer();
        _1.add(_14, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 0, 2, 1, 6, null, null, null));
    }


}
