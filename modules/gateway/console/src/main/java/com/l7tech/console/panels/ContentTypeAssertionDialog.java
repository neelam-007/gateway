package com.l7tech.console.panels;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.ContentTypeAssertion;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

/**
 * Properties editor for ContentTypeAssertion.
 */
public class ContentTypeAssertionDialog extends AssertionPropertiesOkCancelSupport<ContentTypeAssertion> {
    private JPanel mainPanel;
    private JCheckBox messagePartCheckBox;
    private JTextField messagePartField;
    private JRadioButton validateRadioButton;
    private JRadioButton changeRadioButton;
    private JTextField contentTypeField;
    private JCheckBox reInitializeMessageCheckBox;

    private RunOnChangeListener changeListener = new RunOnChangeListener(new Runnable() {
        @Override
        public void run() {
            enableDisable();
        }
    });

    public ContentTypeAssertionDialog(Frame owner, ContentTypeAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
    }

    @Override
    public void setData(ContentTypeAssertion assertion) {
        messagePartCheckBox.setSelected(assertion.isMessagePart());
        messagePartField.setText(String.valueOf(assertion.getMessagePartNum()));
        changeRadioButton.setSelected(assertion.isChangeContentType());
        validateRadioButton.setSelected(!assertion.isChangeContentType());
        contentTypeField.setText(assertion.getNewContentTypeValue());
        reInitializeMessageCheckBox.setSelected(assertion.isReinitializeMessage());
        enableDisable();
    }

    @Override
    public ContentTypeAssertion getData(ContentTypeAssertion assertion) throws ValidationException {
        assertion.setMessagePart(messagePartCheckBox.isSelected());
        assertion.setMessagePartNum(toPartNum(messagePartField.getText()));
        final boolean usesContentType = changeRadioButton.isSelected();
        assertion.setChangeContentType(usesContentType);
        final String contentType = contentTypeField.getText();
        assertion.setNewContentTypeValue(contentType);
        if (usesContentType && Syntax.getReferencedNames(contentType).length < 1) {
            try {
                ContentTypeHeader.create(contentType).validate();
            } catch (IOException e) {
                throw new ValidationException(ExceptionUtils.getMessage(e));
            }
        }
        assertion.setReinitializeMessage(reInitializeMessageCheckBox.isSelected());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        Utilities.enableGrayOnDisabled(contentTypeField);
        Utilities.enableGrayOnDisabled(messagePartField);
        Utilities.enableGrayOnDisabled(reInitializeMessageCheckBox);
        messagePartCheckBox.addActionListener(changeListener);
        changeRadioButton.addActionListener(changeListener);
        validateRadioButton.addActionListener(changeListener);
        reInitializeMessageCheckBox.addActionListener(changeListener);
        return mainPanel;
    }

    private void enableDisable() {
        contentTypeField.setEnabled(changeRadioButton.isSelected());
        reInitializeMessageCheckBox.setEnabled(changeRadioButton.isSelected());
        messagePartField.setEnabled(messagePartCheckBox.isSelected());
    }

    private String toPartNum(String s) {
        if (Syntax.getReferencedNames(s).length > 0)
            return s;

        try {
            int part = Integer.parseInt(s);
            if (part < 1)
                throw new ValidationException("Message part number must be positive if it doesn't use variables");
            return String.valueOf(part);
        } catch (NumberFormatException e) {
            throw new ValidationException("Message part must be a valid number if it doesn't use variables");
        }
    }
}
