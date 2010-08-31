package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.external.assertions.xmlsec.NonSoapSignElementAssertion;
import com.l7tech.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class NonSoapSignElementAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<NonSoapSignElementAssertion> {
    // For now, we have nothing to configure except the XPath, so we just let our superclass handle everything
    @SuppressWarnings({"UnusedDeclaration"})
    private JPanel contentPane;
    private JRadioButton autoIdAttrButton;
    private JRadioButton useSpecificIdAttrButton;
    private JTextField idAttributeNameField;
    private JComboBox signatureLocationComboBox;


    public NonSoapSignElementAssertionPropertiesDialog(Window owner, NonSoapSignElementAssertion assertion) {
        super(owner, assertion);
        initComponents();
        setData(assertion);
        getControlsBelowXpath().setLayout(new BorderLayout());
        getControlsBelowXpath().add(createExtraPanel(), BorderLayout.CENTER);
    }

    private JPanel createExtraPanel() {
        signatureLocationComboBox.setModel(new DefaultComboBoxModel(NonSoapSignElementAssertion.SignatureLocation.values()));

        ActionListener enableOrDisableListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                enableOrDisableComponents();
            }
        };
        autoIdAttrButton.addActionListener(enableOrDisableListener);
        useSpecificIdAttrButton.addActionListener(enableOrDisableListener);

        Utilities.enableGrayOnDisabled(idAttributeNameField);
        Utilities.attachDefaultContextMenu(idAttributeNameField);

        return contentPane;
    }

    private void enableOrDisableComponents() {
        idAttributeNameField.setEnabled(useSpecificIdAttrButton.isSelected());
    }

    private String validQname(String text) throws ValidationException {
        QnameValidator.validateQname(text);
        return text;
    }

    @Override
    public void setData(NonSoapSignElementAssertion assertion) {
        super.setData(assertion);

        String qname = assertion.getCustomIdAttributeQname();
        if (qname == null) {
            idAttributeNameField.setText("");
            useSpecificIdAttrButton.setSelected(false);
            autoIdAttrButton.setSelected(true);
        } else {
            idAttributeNameField.setText(qname);
            autoIdAttrButton.setSelected(false);
            useSpecificIdAttrButton.setSelected(true);
        }

        signatureLocationComboBox.setSelectedItem(assertion.getSignatureLocation());

        enableOrDisableComponents();
    }

    @Override
    public NonSoapSignElementAssertion getData(NonSoapSignElementAssertion assertion) throws ValidationException {
        NonSoapSignElementAssertion ass = super.getData(assertion);

        String qname = useSpecificIdAttrButton.isSelected() ? validQname(idAttributeNameField.getText()) : null;
        ass.setCustomIdAttributeQname(qname);
        ass.setSignatureLocation((NonSoapSignElementAssertion.SignatureLocation) signatureLocationComboBox.getSelectedItem());

        return ass;
    }
}
