package com.l7tech.console.panels;

import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.WsspAssertion;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 *
 */
public class WsspAssertionPropertiesDialog extends AssertionPropertiesOkCancelSupport<WsspAssertion> {
    private JPanel mainPanel;
    private JCheckBox cbGenBasePolicy;
    private JTextArea basePolicyTextArea;
    private JCheckBox cbGenInputPolicy;
    private JTextArea inputPolicyTextArea;
    private JCheckBox cbGenOutputPolicy;
    private JTextArea outputPolicyTextArea;

    public WsspAssertionPropertiesDialog(Frame parent, WsspAssertion assertion) {
        super(WsspAssertion.class, parent, "WS-Security Policy Properties", true);
        initComponents();
        setData(assertion);
    }

    protected void initComponents() {
        super.initComponents();
        Utilities.enableGrayOnDisabled(basePolicyTextArea);
        Utilities.enableGrayOnDisabled(inputPolicyTextArea);
        Utilities.enableGrayOnDisabled(outputPolicyTextArea);
        final ActionListener fieldEnabler = new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                updateTextAreaEnableStates();
            }
        };
        cbGenBasePolicy.addActionListener(fieldEnabler);
        cbGenInputPolicy.addActionListener(fieldEnabler);
        cbGenOutputPolicy.addActionListener(fieldEnabler);
    }

    private void updateTextAreaEnableStates() {
        basePolicyTextArea.setEnabled(!cbGenBasePolicy.isSelected());
        inputPolicyTextArea.setEnabled(!cbGenInputPolicy.isSelected());
        outputPolicyTextArea.setEnabled(!cbGenOutputPolicy.isSelected());
    }

    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    public void setData(WsspAssertion assertion) {
        final String basePolicy = assertion.getBasePolicyXml();
        final String inputPolicy = assertion.getInputPolicyXml();
        final String outputPolicy = assertion.getOutputPolicyXml();
        cbGenBasePolicy.setSelected(basePolicy == null);
        cbGenInputPolicy.setSelected(inputPolicy == null);
        cbGenOutputPolicy.setSelected(outputPolicy == null);
        basePolicyTextArea.setText(basePolicy == null ? "" : basePolicy);
        inputPolicyTextArea.setText(inputPolicy == null ? "" : inputPolicy);
        outputPolicyTextArea.setText(outputPolicy == null ? "" : outputPolicy);
        updateTextAreaEnableStates();
    }

    public WsspAssertion getData(WsspAssertion assertion) throws ValidationException {
        assertion.setBasePolicyXml(cbGenBasePolicy.isSelected() ? null : basePolicyTextArea.getText());
        assertion.setInputPolicyXml(cbGenInputPolicy.isSelected() ? null : inputPolicyTextArea.getText());
        assertion.setOutputPolicyXml(cbGenOutputPolicy.isSelected() ? null : outputPolicyTextArea.getText());
        return assertion;
    }
}
