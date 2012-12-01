package com.l7tech.console.panels;

import com.l7tech.policy.assertion.MessageBufferingAssertion;

import javax.swing.*;
import java.awt.*;

public class MessageBufferingAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<MessageBufferingAssertion> {
    private JPanel contentPane;
    private JRadioButton bufferImmediatelyRadioButton;
    private JRadioButton disallowBufferingRadioButton;

    public MessageBufferingAssertionPropertiesDialog(Frame owner, MessageBufferingAssertion assertion) {
        super(MessageBufferingAssertion.class, owner, assertion, true);
        initComponents();
    }

    @Override
    public void setData(MessageBufferingAssertion assertion) {
        bufferImmediatelyRadioButton.setSelected(assertion.isAlwaysBuffer());
        disallowBufferingRadioButton.setSelected(assertion.isNeverBuffer());
    }

    @Override
    public MessageBufferingAssertion getData(MessageBufferingAssertion assertion) throws ValidationException {
        assertion.setAlwaysBuffer(bufferImmediatelyRadioButton.isSelected());
        assertion.setNeverBuffer(disallowBufferingRadioButton.isSelected());
        return assertion;
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }
}
