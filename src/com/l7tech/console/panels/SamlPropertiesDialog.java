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
        requireSignature.setSelected(true);
        requireSignature.setEnabled(false);
        requireTimeValidity.setSelected(true);
        requireTimeValidity.setEnabled(false);
        Actions.setEscKeyStrokeDisposes(this);
        
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
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

}
