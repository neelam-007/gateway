package com.l7tech.external.assertions.jsondocumentstructure.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.external.assertions.jsondocumentstructure.JsonDocumentStructureAssertion;
import com.l7tech.gui.util.InputValidator;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.ResourceBundle;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class JsonDocumentStructurePropertiesDialog extends AssertionPropertiesOkCancelSupport<JsonDocumentStructureAssertion> {
    private static final ResourceBundle resources =
            ResourceBundle.getBundle(JsonDocumentStructurePropertiesDialog.class.getName());

    private static final String COMPONENT_LABEL_SUFFIX = ":";

    private JPanel contentPane;

    private JCheckBox containerDepthCheckBox;
    private JCheckBox objectEntryCheckBox;
    private JCheckBox arrayEntryCountCheckBox;
    private JCheckBox entryNameLengthCheckBox;
    private JCheckBox stringValueLengthCheckBox;

    private JTextField containerDepthTextField;
    private JTextField objectEntryCountTextField;
    private JTextField arrayEntryCountTextField;
    private JTextField entryNameLengthTextField;
    private JTextField stringValueLengthTextField;

    private InputValidator inputValidator;

    public JsonDocumentStructurePropertiesDialog(final Window owner, final JsonDocumentStructureAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
        updateEnableState();
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        final ChangeListener enablementListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateEnableState();
            }
        };

        containerDepthCheckBox.addChangeListener(enablementListener);
        objectEntryCheckBox.addChangeListener(enablementListener);
        arrayEntryCountCheckBox.addChangeListener(enablementListener);
        entryNameLengthCheckBox.addChangeListener(enablementListener);
        stringValueLengthCheckBox.addChangeListener(enablementListener);

        inputValidator = new InputValidator(this, getTitle());

        inputValidator.constrainTextFieldToNumberRange(getResourceString("containerDepthLabel"),
                containerDepthTextField, 0L, Long.MAX_VALUE);

        inputValidator.constrainTextFieldToNumberRange(getResourceString("objectEntryCountLabel"),
                objectEntryCountTextField, 0L, Long.MAX_VALUE);

        inputValidator.constrainTextFieldToNumberRange(getResourceString("arrayEntryCountLabel"),
                arrayEntryCountTextField, 0L, Long.MAX_VALUE);

        inputValidator.constrainTextFieldToNumberRange(getResourceString("entryNameLengthLabel"),
                entryNameLengthTextField, 0L, Long.MAX_VALUE);

        inputValidator.constrainTextFieldToNumberRange(getResourceString("stringValueLengthLabel"),
                stringValueLengthTextField, 0L, Long.MAX_VALUE);
    }

    @Override
    public void setData(JsonDocumentStructureAssertion assertion) {
        containerDepthCheckBox.setSelected(assertion.isCheckContainerDepth());
        objectEntryCheckBox.setSelected(assertion.isCheckObjectEntryCount());
        arrayEntryCountCheckBox.setSelected(assertion.isCheckArrayEntryCount());
        entryNameLengthCheckBox.setSelected(assertion.isCheckEntryNameLength());
        stringValueLengthCheckBox.setSelected(assertion.isCheckStringValueLength());

        containerDepthTextField.setText(Long.toString(assertion.getMaxContainerDepth()));
        objectEntryCountTextField.setText(Long.toString(assertion.getMaxObjectEntryCount()));
        arrayEntryCountTextField.setText(Long.toString(assertion.getMaxArrayEntryCount()));
        entryNameLengthTextField.setText(Long.toString(assertion.getMaxEntryNameLength()));
        stringValueLengthTextField.setText(Long.toString(assertion.getMaxStringValueLength()));
    }

    @Override
    public JsonDocumentStructureAssertion getData(JsonDocumentStructureAssertion assertion) throws ValidationException {
        final String error = inputValidator.validate();

        if (null != error) {
            throw new ValidationException(error);
        }

        assertion.setCheckContainerDepth(containerDepthCheckBox.isSelected());
        assertion.setCheckObjectEntryCount(objectEntryCheckBox.isSelected());
        assertion.setCheckArrayEntryCount(arrayEntryCountCheckBox.isSelected());
        assertion.setCheckEntryNameLength(entryNameLengthCheckBox.isSelected());
        assertion.setCheckStringValueLength(stringValueLengthCheckBox.isSelected());

        assertion.setMaxContainerDepth(Long.parseLong(containerDepthTextField.getText()));
        assertion.setMaxObjectEntryCount(Long.parseLong(objectEntryCountTextField.getText()));
        assertion.setMaxArrayEntryCount(Long.parseLong(arrayEntryCountTextField.getText()));
        assertion.setMaxEntryNameLength(Long.parseLong(entryNameLengthTextField.getText()));
        assertion.setMaxStringValueLength(Long.parseLong(stringValueLengthTextField.getText()));

        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    private void updateEnableState() {
        containerDepthTextField.setEnabled(containerDepthCheckBox.isSelected());
        objectEntryCountTextField.setEnabled(objectEntryCheckBox.isSelected());
        arrayEntryCountTextField.setEnabled(arrayEntryCountCheckBox.isSelected());
        entryNameLengthTextField.setEnabled(entryNameLengthCheckBox.isSelected());
        stringValueLengthTextField.setEnabled(stringValueLengthCheckBox.isSelected());
    }

    /**
     * Returns the value of the specified resource string. If the string has a label suffix, e.g. a colon,
     * it is removed.
     * @param key the key of the resource
     * @return the resource string
     */
    private static String getResourceString(String key) {
        final String value = resources.getString(key);

        if (value.endsWith(COMPONENT_LABEL_SUFFIX)) {
            return value.substring(0, value.lastIndexOf(COMPONENT_LABEL_SUFFIX));
        }

        return value;
    }
}
