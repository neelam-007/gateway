package com.l7tech.external.assertions.retrieveservicewsdl.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.retrieveservicewsdl.RetrieveServiceWsdlAssertion;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ResourceBundle;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class RetrieveServiceWsdlPropertiesDialog extends AssertionPropertiesOkCancelSupport<RetrieveServiceWsdlAssertion> {
    private static final ResourceBundle resources =
            ResourceBundle.getBundle(RetrieveServiceWsdlPropertiesDialog.class.getName());

    private static final String COMPONENT_LABEL_SUFFIX = ":";

    private JPanel contentPane;
    private JTextField serviceIdTextField;
    private JTextField hostNameTextField;
    private JComboBox<MessageTargetableSupport> targetMessageComboBox;
    private JPanel targetVariablePanelHolder;
    private TargetVariablePanel targetVariablePanel;

    private InputValidator inputValidator;

    public RetrieveServiceWsdlPropertiesDialog(final Window owner, final RetrieveServiceWsdlAssertion assertion) {
        super(assertion.getClass(), owner, assertion, true);
        initComponents();
        setData(assertion);
        updateTargetVariablePanelEnablementState();
    }

    @Override
    protected void initComponents() {
        super.initComponents();

        // Message target combo box
        targetMessageComboBox.setModel(buildMessageTargetComboBoxModel(false));

        targetMessageComboBox.setRenderer(
                new TextListCellRenderer<>(getMessageNameFunction("Default", "Message Variable"), null, true));

        targetMessageComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateTargetVariablePanelEnablementState();
            }
        });

        // Message target variable
        targetVariablePanel = new TargetVariablePanel();
        targetVariablePanelHolder.setLayout(new BorderLayout());
        targetVariablePanelHolder.add(targetVariablePanel, BorderLayout.CENTER);

        /* --- validation --- */

        inputValidator = new InputValidator(this, getResourceString("validationDialogTitle"));

        // service ID is required
        inputValidator.constrainTextFieldToBeNonEmpty(getResourceString("serviceIdLabel"), serviceIdTextField, null);

        // host is required
        inputValidator.constrainTextFieldToBeNonEmpty(getResourceString("hostNameLabel"), hostNameTextField, null);

        // validate target message variable
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return targetVariablePanel.getErrorMessage();
            }
        });
    }

    @Override
    protected JPanel createPropertyPanel() {
        return contentPane;
    }

    @Override
    public void setData(RetrieveServiceWsdlAssertion assertion) {
        // service ID
        serviceIdTextField.setText(assertion.getServiceId());

        // host name
        hostNameTextField.setText(assertion.getHostname());

        // message target
        final MessageTargetableSupport responseTarget = assertion.getMessageTarget();
        targetMessageComboBox.setSelectedItem(new MessageTargetableSupport(responseTarget.getTarget()));

        // message target variable
        targetVariablePanel.setAssertion(assertion, getPreviousAssertion());

        if (responseTarget.getTarget() == TargetMessageType.OTHER) {
            targetVariablePanel.setVariable(responseTarget.getOtherTargetMessageVariable());
        } else {
            targetVariablePanel.setVariable(StringUtils.EMPTY);
        }
    }

    @Override
    public RetrieveServiceWsdlAssertion getData(RetrieveServiceWsdlAssertion assertion) throws ValidationException {
        // perform validation
        final String error = inputValidator.validate();

        if (error != null) {
            throw new ValidationException(error);
        }

        // service ID
        assertion.setServiceId(serviceIdTextField.getText());

        // host name
        assertion.setHostname(hostNameTextField.getText());

        // message target
        final MessageTargetableSupport responseTarget =
                new MessageTargetableSupport((MessageTargetable) targetMessageComboBox.getSelectedItem());

        if (responseTarget.getTarget() == TargetMessageType.OTHER) {
            responseTarget.setOtherTargetMessageVariable(targetVariablePanel.getVariable());
            responseTarget.setSourceUsedByGateway(false);
            responseTarget.setTargetModifiedByGateway(true);
        }

        assertion.setMessageTarget(responseTarget);

        return assertion;
    }

    private void updateTargetVariablePanelEnablementState() {
        Object selectedTarget = targetMessageComboBox.getSelectedItem();

        targetVariablePanel.setEnabled(targetMessageComboBox.isEnabled() && selectedTarget != null
                && ((MessageTargetable) selectedTarget).getTarget() == TargetMessageType.OTHER);
    }

    /**
     * Returns the value of the specified resource string.
     * If the string has a label suffix, e.g. a colon, it is removed.
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
