package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.l7tech.console.action.Actions;
import com.l7tech.console.tree.policy.SamlTreeNode;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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
        setTitle("SAML Properties");
        samlTreeNode = n;
        samlSecurity = (SamlSecurity)samlTreeNode.asAssertion();
        initialize();
    }

    private void initialize() {
        setContentPane(mainPanel);
        requireSignature.setSelected(samlSecurity.isValidateSignature());
        requireEncryption.setEnabled(false);
        requireSignature.setEnabled(false);
        Actions.setEscKeyStrokeDisposes(this);
        
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

        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(SamlPropertiesDialog.this);
            }
        });

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
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_NORTH, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        panel1.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        final JLabel label1 = new JLabel();
        label1.setText("SAML constraints");
        panel1.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 4, new Insets(5, 0, 0, 0), -1, -1));
        mainPanel.add(panel2, new GridConstraints(3, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        helpButton = new JButton();
        helpButton.setText("Help");
        panel2.add(helpButton, new GridConstraints(0, 3, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        cancelButton = new JButton();
        cancelButton.setText("Cancel");
        panel2.add(cancelButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        okButton = new JButton();
        okButton.setText("OK");
        panel2.add(okButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer1 = new Spacer();
        panel2.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(4, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel3, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        requireTimeValidity = new JCheckBox();
        requireTimeValidity.setHorizontalAlignment(10);
        requireTimeValidity.setHorizontalTextPosition(11);
        requireTimeValidity.setText("Check time and date on ticket");
        panel3.add(requireTimeValidity, new GridConstraints(1, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        requireEncryption = new JCheckBox();
        requireEncryption.setHorizontalAlignment(10);
        requireEncryption.setHorizontalTextPosition(11);
        requireEncryption.setText("Require encrypted ticket");
        panel3.add(requireEncryption, new GridConstraints(2, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        requireSignature = new JCheckBox();
        requireSignature.setHorizontalTextPosition(11);
        requireSignature.setText("Require signed ticket");
        panel3.add(requireSignature, new GridConstraints(3, 1, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer2 = new Spacer();
        panel3.add(spacer2, new GridConstraints(1, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, new Dimension(100, -1), new Dimension(100, -1), null));
        final Spacer spacer3 = new Spacer();
        mainPanel.add(spacer3, new GridConstraints(2, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_VERTICAL, 1, GridConstraints.SIZEPOLICY_WANT_GROW, null, null, null));
    }
}
