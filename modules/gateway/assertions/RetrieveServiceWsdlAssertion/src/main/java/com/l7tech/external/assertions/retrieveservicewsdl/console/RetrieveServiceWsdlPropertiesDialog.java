package com.l7tech.external.assertions.retrieveservicewsdl.console;

import com.l7tech.console.panels.AssertionPropertiesOkCancelSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.external.assertions.retrieveservicewsdl.RetrieveServiceWsdlAssertion;
import com.l7tech.gui.MaxLengthDocument;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class RetrieveServiceWsdlPropertiesDialog extends AssertionPropertiesOkCancelSupport<RetrieveServiceWsdlAssertion> {
    private static final ResourceBundle resources =
            ResourceBundle.getBundle(RetrieveServiceWsdlPropertiesDialog.class.getName());

    private static final String HTTP_OPTION = "HTTP";
    private static final String HTTPS_OPTION = "HTTPS";

    private static final int PROTOCOL_FROM_VARIABLE_OPTION_INDEX = 0;
    private static final String PROTOCOL_FROM_VARIABLE_OPTION = getResourceString("protocolFromVariable");

    private static final String COMPONENT_LABEL_SUFFIX = ":";

    private JPanel contentPane;
    private JTextField serviceIdTextField;
    private JTextField hostTextField;
    private JComboBox<MessageTargetableSupport> targetMessageComboBox;
    private JPanel targetVariablePanelHolder;
    private JTextField portTextField;
    private JLabel urlPreviewLabel;
    private JComboBox<String> protocolComboBox;
    private JPanel protocolVariablePanelHolder;
    private TargetVariablePanel protocolVariablePanel;
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

        DocumentListener urlComponentDocumentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateUrlPreview();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateUrlPreview();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateUrlPreview();
            }
        };

        // protocol
        DefaultComboBoxModel<String> protocolComboBoxModel = new DefaultComboBoxModel<>();

        protocolComboBoxModel.insertElementAt(PROTOCOL_FROM_VARIABLE_OPTION, PROTOCOL_FROM_VARIABLE_OPTION_INDEX);
        protocolComboBoxModel.addElement(HTTP_OPTION);
        protocolComboBoxModel.addElement(HTTPS_OPTION);

        protocolComboBox.setModel(protocolComboBoxModel);
        protocolComboBox.setSelectedIndex(PROTOCOL_FROM_VARIABLE_OPTION_INDEX);
        protocolComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateProtocolVariablePanelEnablementState();
                updateUrlPreview();
            }
        });

        // protocol variable
        protocolVariablePanel = new TargetVariablePanel();
        protocolVariablePanel.setValueWillBeRead(true);
        protocolVariablePanel.setValueWillBeWritten(false);

        protocolVariablePanel.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                updateUrlPreview();
            }
        });

        protocolVariablePanelHolder.setLayout(new BorderLayout());
        protocolVariablePanelHolder.add(protocolVariablePanel, BorderLayout.CENTER);

        // host name
        hostTextField.setDocument(new MaxLengthDocument(255));
        hostTextField.getDocument().addDocumentListener(urlComponentDocumentListener);

        // port
        portTextField.getDocument().addDocumentListener(urlComponentDocumentListener);

        // message target combo box
        targetMessageComboBox.setModel(buildMessageTargetComboBoxModel(false));

        targetMessageComboBox.setRenderer(
                new TextListCellRenderer<>(getMessageNameFunction("Default", "Message Variable"), null, true));

        targetMessageComboBox.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                updateTargetVariablePanelEnablementState();
            }
        });

        // message target variable
        targetVariablePanel = new TargetVariablePanel();
        targetVariablePanel.setValueWillBeRead(false);
        targetVariablePanel.setValueWillBeWritten(true);

        targetVariablePanelHolder.setLayout(new BorderLayout());
        targetVariablePanelHolder.add(targetVariablePanel, BorderLayout.CENTER);

        /* --- Validation --- */

        inputValidator = new InputValidator(this, getResourceString("validationDialogTitle"));

        // service ID is required, must be a single context variable or a valid GOID
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String serviceId = serviceIdTextField.getText().trim();

                if (serviceId.isEmpty()) {
                    return resources.getString("serviceIdEmptyErrMsg");
                }

                // see if a context variable is present
                if (Syntax.getReferencedNames(serviceId).length > 0) {
                    if (!Syntax.isOnlyASingleVariableReferenced(serviceId)) {
                        return resources.getString("serviceIdNotOnlyOneVariableErrMsg");
                    }
                } else if (!ValidationUtils.isValidGoid(serviceId, false)) {  // if not a variable, must be a GOID
                    return MessageFormat.format(resources.getString("serviceIdInvalidErrMsg"), serviceId);
                }

                return null;
            }
        });

        // protocol option is required
        inputValidator.ensureComboBoxSelection(getResourceString("protocolLabel"), protocolComboBox);

        // validate protocol variable
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                return protocolVariablePanel.getErrorMessage();
            }
        });

        // host is required
        inputValidator.constrainTextFieldToBeNonEmpty(getResourceString("hostLabel"), hostTextField, null);

        // port must be integer or context variable
        inputValidator.addRule(new InputValidator.ComponentValidationRule(portTextField) {
            @Override
            public String getValidationError() {
                if (!isPortValid()) {
                    return getResourceString("portError");
                }

                return null;
            }
        });

        // protocol, hostname and port must form valid URL, or be composed of one or more context variables
        inputValidator.addRule(new InputValidator.ValidationRule() {
            @Override
            public String getValidationError() {
                String endpointUrl = getEndpointUrl();

                if (!isValidUrlOrContainsContextVariables(endpointUrl)) {
                    return MessageFormat.format(getResourceString("urlError"), endpointUrl);
                }

                return null;
            }
        });

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

        // endpoint fields
        String protocolVariable = assertion.getProtocolVariable();

        if (null != protocolVariable) {
            protocolComboBox.setSelectedIndex(PROTOCOL_FROM_VARIABLE_OPTION_INDEX);
            protocolVariablePanel.setVariable(protocolVariable);
        } else {
            String protocol = assertion.getProtocol();

            if (protocol.equals("http")) {
                protocolComboBox.setSelectedItem(HTTP_OPTION);
            } else if (protocol.equals("https")) {
                protocolComboBox.setSelectedItem(HTTPS_OPTION);
            }
        }

        hostTextField.setText(assertion.getHost());
        portTextField.setText(assertion.getPort());

        updateUrlPreview();

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
        assertion.setServiceId(serviceIdTextField.getText().trim());

        // endpoint fields
        if (protocolComboBox.getSelectedIndex() == PROTOCOL_FROM_VARIABLE_OPTION_INDEX) {
            assertion.setProtocol(null);
            assertion.setProtocolVariable(protocolVariablePanel.getVariable());
        } else {
            if (protocolComboBox.getSelectedItem().equals(HTTP_OPTION)) {
                assertion.setProtocol("http");
            } else if (protocolComboBox.getSelectedItem().equals(HTTPS_OPTION)) {
                assertion.setProtocol("https");
            }

            assertion.setProtocolVariable(null);
        }

        assertion.setHost(hostTextField.getText().trim());
        assertion.setPort(portTextField.getText().trim());

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

    private void updateProtocolVariablePanelEnablementState() {
        protocolVariablePanel.setEnabled(protocolComboBox.getSelectedIndex() == PROTOCOL_FROM_VARIABLE_OPTION_INDEX);
    }

    private void updateTargetVariablePanelEnablementState() {
        Object selectedTarget = targetMessageComboBox.getSelectedItem();

        targetVariablePanel.setEnabled(targetMessageComboBox.isEnabled() && selectedTarget != null
                && ((MessageTargetable) selectedTarget).getTarget() == TargetMessageType.OTHER);
    }

    private void updateUrlPreview() {
        String endpointUrl = getEndpointUrl();

        urlPreviewLabel.setText(endpointUrl);

        if (isProtocolValid() && isHostValid() && isPortValid()
                && isValidUrlOrContainsContextVariables(endpointUrl)) {
            urlPreviewLabel.setForeground(Color.BLACK);
        } else {
            urlPreviewLabel.setForeground(Color.RED);
        }
    }

    // valid if a combo box item is selected, and the protocol variable panel is either disabled or has no errors
    private boolean isProtocolValid() {
        return null != getProtocol() &&
                (!protocolVariablePanel.isEnabled() || null == protocolVariablePanel.getErrorMessage());
    }

    private boolean isHostValid() {
        String host = hostTextField.getText().trim();

        return !host.isEmpty() || Syntax.getReferencedNames(host).length > 0;
    }

    private boolean isPortValid() {
        String portStr = portTextField.getText().trim();

        try {
            int port = Integer.parseInt(portStr);
            return port >= RetrieveServiceWsdlAssertion.PORT_RANGE_START && port <= RetrieveServiceWsdlAssertion.PORT_RANGE_END;
        } catch (NumberFormatException e) {
            // must be using context variable
            return Syntax.getReferencedNames(portStr).length > 0;
        }
    }

    private boolean isValidUrlOrContainsContextVariables(String endpointUrl) {
        return ValidationUtils.isValidUrl(endpointUrl, false) ||
                Syntax.getReferencedNames(endpointUrl).length > 0;
    }

    private String getEndpointUrl() {
        return getProtocol() + "://" + hostTextField.getText().trim() + ":" + portTextField.getText().trim();
    }

    private String getProtocol() {
        String protocol = null;

        if (protocolComboBox.getSelectedIndex() == PROTOCOL_FROM_VARIABLE_OPTION_INDEX) {
            protocol = Syntax.getVariableExpression(protocolVariablePanel.getVariable());
        } else if (protocolComboBox.getSelectedItem().equals(HTTP_OPTION)) {
            protocol = "http";
        } else if (protocolComboBox.getSelectedItem().equals(HTTPS_OPTION)) {
            protocol = "https";
        }

        return protocol;
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
