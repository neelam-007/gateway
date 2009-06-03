package com.l7tech.console.panels;

import com.l7tech.security.xml.KeyReference;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.gui.widgets.ValidatedPanel;
import com.l7tech.policy.assertion.xmlsec.AddWssSecurityToken;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author alex
 */
public class AddWssSecurityTokenPanel extends ValidatedPanel<AddWssSecurityToken> {
    private JPanel mainPanel;
    private JComboBox tokenTypeCombo;
    private JRadioButton bstRadio;
    private JRadioButton strRadio;
    private JCheckBox includePasswordCheckBox;

    private final AddWssSecurityToken assertion;

    private final ActionListener updater = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            checkSyntax();
        }
    };

    public AddWssSecurityTokenPanel(AddWssSecurityToken model) {
        super("assertion");
        this.assertion = model;
        init();
    }

    protected void initComponents() {
        tokenTypeCombo.setModel(new DefaultComboBoxModel(AddWssSecurityToken.SUPPORTED_TOKEN_TYPES));
        tokenTypeCombo.setSelectedItem(assertion.getTokenType());
        tokenTypeCombo.addActionListener(updater);

        if (assertion.getTokenType() == SecurityTokenType.WSS_USERNAME) {
            includePasswordCheckBox.setEnabled(true);
            includePasswordCheckBox.setSelected(assertion.isIncludePassword());
        } else {
            includePasswordCheckBox.setEnabled(false);
        }
        includePasswordCheckBox.addActionListener(updater);

        ButtonGroup bg = new ButtonGroup();
        bg.add(bstRadio);
        bg.add(strRadio);

        boolean bst = KeyReference.BST.getName().equals(assertion.getKeyReference());
        bstRadio.setSelected(bst);
        strRadio.setSelected(!bst);
        bstRadio.addActionListener(updater);
        strRadio.addActionListener(updater);

        add(mainPanel, BorderLayout.CENTER);
    }

    protected void doUpdateModel() {
        SecurityTokenType type = (SecurityTokenType)tokenTypeCombo.getSelectedItem();
        includePasswordCheckBox.setEnabled(type == SecurityTokenType.WSS_USERNAME);
        assertion.setIncludePassword(includePasswordCheckBox.isSelected());
        assertion.setTokenType(type);
        if (bstRadio.isSelected()) {
            assertion.setKeyReference(KeyReference.BST.getName());
        } else if (strRadio.isSelected()) {
            assertion.setKeyReference(KeyReference.SKI.getName());
        } else {
            throw new IllegalStateException("Neither BST nor SKI selected");
        }
    }

    protected AddWssSecurityToken getModel() {
        return assertion;
    }

    public void focusFirstComponent() {
    }
}
