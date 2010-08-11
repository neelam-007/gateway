package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.external.assertions.xmlsec.NonSoapSignElementAssertion;
import com.l7tech.gui.util.Utilities;
import com.l7tech.util.DomUtils;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.FullQName;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.ParseException;

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

    private String validQname(String text) {
        final String msg = "Attribute name should be in one of the formats: NAME, PREFIX:NAME, {URI}NAME, or {URI}PREFIX:NAME";

        if (text == null || text.length() < 1)
            throw new ValidationException("Attribute name is empty.\n\n" + msg);

        try {
            FullQName parsedQname = FullQName.valueOf(text);
            String nsUri = parsedQname.getNsUri();
            String prefix = parsedQname.getPrefix();
            String local = parsedQname.getLocal();

            // Check URI
            if (nsUri != null && nsUri.length() > 0)
                new URI(nsUri);

            // Check prefix
            if (prefix != null && prefix.length() > 0 && !DomUtils.isValidXmlNcName(prefix))
                throw new ValidationException("Attribute name has an invalid namespace prefix: " + prefix + "\n\n" + msg);

            // Check local name
            if (local == null || local.length() < 1)
                throw new ValidationException("Attribute name is missing a local part.\n\n" + msg);
            if (!DomUtils.isValidXmlNcName(local))
                throw new ValidationException("Attribute name local part is not valid.\n\n" + msg);

            return text;
        } catch (URISyntaxException e) {
            throw new ValidationException("Invalid attribute namespace URI: " + ExceptionUtils.getMessage(e), e);
        } catch (ParseException e) {
            throw new ValidationException(ExceptionUtils.getMessage(e) + "\n\n" + msg, e);
        }
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
