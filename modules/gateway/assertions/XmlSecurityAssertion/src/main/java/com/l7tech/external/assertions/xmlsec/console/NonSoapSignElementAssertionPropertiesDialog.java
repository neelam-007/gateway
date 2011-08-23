package com.l7tech.external.assertions.xmlsec.console;

import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.xmlsec.NonSoapSignElementAssertion;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.security.xml.SupportedDigestMethods;
import com.l7tech.util.ArrayUtils;

import javax.swing.*;
import java.awt.*;

public class NonSoapSignElementAssertionPropertiesDialog extends NonSoapSecurityAssertionDialog<NonSoapSignElementAssertion> {
    private JPanel contentPane;
    private JRadioButton autoIdAttrButton;
    private JRadioButton useSpecificIdAttrButton;
    private JTextField idAttributeNameField;
    private JComboBox signatureLocationComboBox;
    private JRadioButton rbAddSigToEachTarget;
    private JRadioButton rbCreateDetached;
    private JPanel detachedVariablePanel;
    private TargetVariablePanel detachedVariableField;
    private JCheckBox envelopedCheckBox;
    private JComboBox signatureDigestComboBox;
    private JComboBox referenceDigestComboBox;

    private static final String DEFAULT = "Default";

    public NonSoapSignElementAssertionPropertiesDialog(Window owner, NonSoapSignElementAssertion assertion) {
        super(owner, assertion);
        detachedVariableField = new TargetVariablePanel();
        initComponents();
        setData(assertion);
        Utilities.setSingleChild(getControlsBelowXpath(), createExtraPanel());
    }

    private JPanel createExtraPanel() {
        signatureLocationComboBox.setModel(new DefaultComboBoxModel(NonSoapSignElementAssertion.SignatureLocation.values()));

        RunOnChangeListener enableOrDisableListener = new RunOnChangeListener() {
            @Override
            public void run() {
                enableOrDisableComponents();
            }
        };

        Utilities.setSingleChild(detachedVariablePanel, detachedVariableField);
        detachedVariableField.addChangeListener(enableOrDisableListener);
        detachedVariableField.setValueWillBeWritten(true);
        detachedVariableField.setValueWillBeRead(false);

        autoIdAttrButton.addActionListener(enableOrDisableListener);
        useSpecificIdAttrButton.addActionListener(enableOrDisableListener);
        rbCreateDetached.addActionListener(enableOrDisableListener);
        rbAddSigToEachTarget.addActionListener(enableOrDisableListener);

        idAttributeNameField.setEnabled(true);
        Utilities.enableGrayOnDisabled(idAttributeNameField);
        Utilities.attachDefaultContextMenu(idAttributeNameField);

        detachedVariableField.setEnabled(true);
        Utilities.enableGrayOnDisabled(detachedVariableField);

        Utilities.enableGrayOnDisabled(signatureLocationComboBox);
        Utilities.enableGrayOnDisabled(envelopedCheckBox);

        signatureDigestComboBox.setModel(new DefaultComboBoxModel(prependDefault(SupportedDigestMethods.getDigestNames())));
        referenceDigestComboBox.setModel(new DefaultComboBoxModel(prependDefault(SupportedDigestMethods.getDigestNames())));

        return contentPane;
    }

    private void enableOrDisableComponents() {
        idAttributeNameField.setEnabled(useSpecificIdAttrButton.isSelected());
        signatureLocationComboBox.setEnabled(rbAddSigToEachTarget.isSelected());
        envelopedCheckBox.setEnabled(rbCreateDetached.isSelected());
        detachedVariableField.setEnabled(rbCreateDetached.isSelected());
    }

    private String validQname(String text) throws ValidationException {
        if ("".equals(text))
            return text;
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

        String detachedVar = assertion.getDetachedSignatureVariableName();
        boolean detached = detachedVar != null;
        rbAddSigToEachTarget.setSelected(!detached);
        rbCreateDetached.setSelected(detached);
        detachedVariableField.setAssertion(assertion, getPreviousAssertion());
        detachedVariableField.setVariable(detachedVar == null ? "" : detachedVar);
        envelopedCheckBox.setSelected(assertion.isForceEnvelopedTransform());
        setSelectedItemOrDefaultIfNull(signatureDigestComboBox, assertion.getDigestAlgName());
        setSelectedItemOrDefaultIfNull(referenceDigestComboBox, assertion.getRefDigestAlgName());

        enableOrDisableComponents();
    }

    @Override
    public NonSoapSignElementAssertion getData(NonSoapSignElementAssertion assertion) throws ValidationException {
        NonSoapSignElementAssertion ass = super.getData(assertion);

        String qname = useSpecificIdAttrButton.isSelected() ? validQname(idAttributeNameField.getText()) : null;
        ass.setCustomIdAttributeQname(qname);
        ass.setSignatureLocation((NonSoapSignElementAssertion.SignatureLocation) signatureLocationComboBox.getSelectedItem());
        final boolean detached = rbCreateDetached.isSelected();
        final String detachedVar = detachedVariableField.getVariable();
        if (detached && detachedVar.trim().length() < 1)
            throw new ValidationException("A variable name must be provided in order to create a detached signature.");
        ass.setDetachedSignatureVariableName(detached ? detachedVar : null);
        ass.setForceEnvelopedTransform(envelopedCheckBox.isSelected());
        ass.setDigestAlgName((String)getSelectedItemOrNullIfDefault(signatureDigestComboBox));
        ass.setRefDigestAlgName((String)getSelectedItemOrNullIfDefault(referenceDigestComboBox));

        return ass;
    }

    private static Object[] prependDefault(Object[] things) {
        return ArrayUtils.unshift(things, DEFAULT);
    }

    private static void setSelectedItemOrDefaultIfNull(JComboBox comboBox, Object objOrUnchanged) {
        if (null == objOrUnchanged) {
            comboBox.setSelectedItem(DEFAULT);
        } else {
            comboBox.setSelectedItem(objOrUnchanged);
        }
    }

    private static Object getSelectedItemOrNullIfDefault(JComboBox comboBox) {
        Object ret = comboBox.getSelectedItem();
        return DEFAULT == ret || !comboBox.isEnabled() ? null : ret;
    }
}
