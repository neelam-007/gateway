package com.l7tech.external.assertions.retrieveservicewsdl.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.ValidatorUtils;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.Policy;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.*;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.retrieveservicewsdl.console.PublishWsdlQueryHandlerWizard.WsdlQueryHandlerConfig;

public class WsdlQueryHandlerConfigurationPanel extends WizardStepPanel<WsdlQueryHandlerConfig> {
    private static final Logger logger = Logger.getLogger(WsdlQueryHandlerConfigurationPanel.class.getName());
    private static final ResourceBundle resources = ResourceBundle.getBundle(WsdlQueryHandlerConfigurationPanel.class.getName());

    private static final int SERVICE_NAME_MAX_LENGTH = 255;
    private static final int ROUTING_URI_MAX_LENGTH = 127;

    private JPanel contentPanel;
    private JTextField serviceNameTextField;
    private JTextField routingUriTextField;
    private JLabel urlPreviewLabel;

    private String urlPrefix;

    public WsdlQueryHandlerConfigurationPanel(WizardStepPanel<WsdlQueryHandlerConfig> next) {
        super(next);

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        add(contentPanel);

        urlPrefix = "http(s)://" + TopComponents.getInstance().ssgURL().getHost() + ":[port]";

        // listener updates service URL preview label as routing URI is updated
        routingUriTextField.getDocument().addDocumentListener(new DocumentListener() {
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
        });

        // service must have a name
        validationRules.add(new InputValidator.ComponentValidationRule(serviceNameTextField) {
            @Override
            public String getValidationError() {
                String serviceName = serviceNameTextField.getText().trim();

                if (StringUtils.isBlank(serviceName)) {
                    return resources.getString("serviceNameNotSpecifiedError");
                } else if (serviceName.length() > 255) {
                    return MessageFormat.format(resources.getString("serviceNameTooLongError"), SERVICE_NAME_MAX_LENGTH);
                }

                return null;
            }
        });

        // must have a valid routing uri
        validationRules.add(new InputValidator.ComponentValidationRule(routingUriTextField) {
            @Override
            public String getValidationError() {
                String errorMsg;

                String routingUri = routingUriTextField.getText().trim();

                if (StringUtils.isBlank(routingUri)) {
                    return resources.getString("routingUriNotSpecifiedError");
                } else if (!StringUtils.startsWith(routingUri, "/")) {
                    return resources.getString("routingUriMissingLeadingSlashError");
                } else if (routingUri.length() > ROUTING_URI_MAX_LENGTH) {
                    return MessageFormat.format(resources.getString("routingUriTooLongError"), ROUTING_URI_MAX_LENGTH);
                }

                errorMsg = ValidationUtils.isValidUriString(routingUri);

                return null == errorMsg
                        ? ValidatorUtils.validateResolutionPath(routingUri, true, true)
                        : errorMsg;
            }
        });

        // policy names must be unique
        validationRules.add(new InputValidator.ComponentValidationRule(serviceNameTextField) {
            @Override
            public String getValidationError() {
                try {
                    String serviceName = serviceNameTextField.getText().trim();

                    // redirection fragment name
                    String fragmentName = serviceName + PublishWsdlQueryHandlerWizard.REDIRECTION_FRAGMENT_NAME_SUFFIX;

                    if (!isPolicyNameUnique(fragmentName)) {
                        return MessageFormat.format(resources.getString("policyNameNotUniqueError"), fragmentName);
                    }

                    // authentication fragment name
                    fragmentName = serviceName + PublishWsdlQueryHandlerWizard.AUTHENTICATION_FRAGMENT_NAME_SUFFIX;

                    if (!isPolicyNameUnique(fragmentName)) {
                        return MessageFormat.format(resources.getString("policyNameNotUniqueError"), fragmentName);
                    }
                } catch (FindException e) {
                    logger.log(Level.WARNING, e.getMessage(), ExceptionUtils.getDebugException(e));
                    return "Failed to validate service name " + serviceNameTextField.getText().trim();
                }

                return null;
            }
        });
    }

    private boolean isPolicyNameUnique(String policyName) throws FindException {
        final Policy found = Registry.getDefault().getPolicyAdmin().findPolicyByUniqueName(policyName);

        return null == found;
    }

    @Override
    public void storeSettings(WsdlQueryHandlerConfig settings) throws IllegalArgumentException {
        settings.setServiceName(serviceNameTextField.getText().trim());
        settings.setRoutingUri(routingUriTextField.getText().trim());
    }

    @Override
    public void readSettings(WsdlQueryHandlerConfig settings) throws IllegalArgumentException {
        serviceNameTextField.setText(settings.getServiceName());
        routingUriTextField.setText(settings.getRoutingUri());

        updateUrlPreview();
    }

    private void updateUrlPreview() {
        String routingUri = routingUriTextField.getText().trim();

        urlPreviewLabel.setText(urlPrefix + routingUri);

        if (!routingUri.isEmpty() && ValidationUtils.isValidUri(routingUri) && routingUri.startsWith("/")) {
            urlPreviewLabel.setForeground(Color.BLACK);
        } else {
            urlPreviewLabel.setForeground(Color.RED);
        }
    }

    @Override
    public String getStepLabel() {
        return resources.getString("stepLabel");
    }
}
