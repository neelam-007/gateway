package com.l7tech.external.assertions.swagger.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.ValidatorUtils;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.objectmodel.EntityType;
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
import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.swagger.console.PublishSwaggerServiceWizard.SwaggerServiceConfig;

public class SwaggerServiceConfigurationPanel extends WizardStepPanel<SwaggerServiceConfig> {
    private static final Logger logger = Logger.getLogger(SwaggerServiceConfigurationPanel.class.getName());
    private static final ResourceBundle resources =
            ResourceBundle.getBundle(SwaggerServiceConfigurationPanel.class.getName());

    private static final int SERVICE_NAME_MAX_LENGTH = 235;
    private static final int ROUTING_URI_MAX_LENGTH = 127;

    private JPanel contentPanel;
    private JTextField serviceNameTextField;
    private JTextField routingUriTextField;
    private JTextField urlPreviewTextField;
    private SecurityZoneWidget securityZoneWidget;

    private String urlPrefix;

    public SwaggerServiceConfigurationPanel(WizardStepPanel<SwaggerServiceConfig> next) {
        super(next);

        initComponents();
    }

    private void initComponents() {
        securityZoneWidget.configure(Arrays.asList(new EntityType[]{EntityType.SERVICE, EntityType.POLICY}),
                OperationType.CREATE, null);

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

        // service must have a name and be unique
        validationRules.add(new InputValidator.ComponentValidationRule(serviceNameTextField) {
            @Override
            public String getValidationError() {
                String serviceName = serviceNameTextField.getText().trim();

                if (StringUtils.isBlank(serviceName)) {
                    return resources.getString("serviceNameNotSpecifiedError");
                } else if (serviceName.length() > SERVICE_NAME_MAX_LENGTH) {
                    return MessageFormat.format(resources.getString("serviceNameTooLongError"), SERVICE_NAME_MAX_LENGTH);
                }

                try {
                    if (!isPolicyNameUnique(serviceName)) {
                        return MessageFormat.format(resources.getString("policyNameNotUniqueError"), serviceName);
                    }
                } catch (FindException e) {
                    logger.log(Level.WARNING, e.getMessage(), ExceptionUtils.getDebugException(e));
                    return "Failed to validate service name " + serviceNameTextField.getText().trim();
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
    }

    private boolean isPolicyNameUnique(String policyName) throws FindException {
        final Policy found = Registry.getDefault().getPolicyAdmin().findPolicyByUniqueName(policyName);

        return null == found;
    }

    @Override
    public void storeSettings(SwaggerServiceConfig settings) throws IllegalArgumentException {
        settings.setServiceName(serviceNameTextField.getText().trim());
        settings.setRoutingUri(routingUriTextField.getText().trim());
        settings.setSecurityZone(securityZoneWidget.getSelectedZone());
    }

    @Override
    public void readSettings(SwaggerServiceConfig settings) throws IllegalArgumentException {
        serviceNameTextField.setText(settings.getServiceName());
        routingUriTextField.setText(settings.getRoutingUri());
        securityZoneWidget.setSelectedZone(settings.getSecurityZone());

        updateUrlPreview();
    }

    private void updateUrlPreview() {
        String routingUri = routingUriTextField.getText().trim();

        urlPreviewTextField.setText(urlPrefix + routingUri);

        if (!routingUri.isEmpty() && ValidationUtils.isValidUri(routingUri) && routingUri.startsWith("/")) {
            urlPreviewTextField.setForeground(Color.BLACK);
        } else {
            urlPreviewTextField.setForeground(Color.RED);
        }
    }

    @Override
    public String getStepLabel() {
        return resources.getString("stepLabel");
    }

    @Override
    public String getDescription() {
        return resources.getString("stepDescription");
    }
}
