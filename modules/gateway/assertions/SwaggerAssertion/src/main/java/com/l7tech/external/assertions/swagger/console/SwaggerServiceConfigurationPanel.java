package com.l7tech.external.assertions.swagger.console;

import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.SecurityZoneWidget;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.ValidatorUtils;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.util.InetAddressUtil;
import com.l7tech.util.Pair;
import com.l7tech.util.ValidationUtils;
import org.apache.commons.lang.StringUtils;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.ResourceBundle;

import static com.l7tech.external.assertions.swagger.console.PublishSwaggerServiceWizard.SwaggerServiceConfig;

public class SwaggerServiceConfigurationPanel extends WizardStepPanel<SwaggerServiceConfig> {
    private static final ResourceBundle resources =
            ResourceBundle.getBundle(SwaggerServiceConfigurationPanel.class.getName());

    private static final int SERVICE_NAME_MAX_LENGTH = 235;

    private JPanel contentPanel;
    private JTextField serviceNameTextField;
    private JTextField apiHostTextField;
    private JTextField apiBasePathTextField;
    private JTextField routingUriTextField;
    private JLabel gatewayUrlPrefixLabel;
    private SecurityZoneWidget securityZoneWidget;
    private JCheckBox validatePathCheckBox;
    private JCheckBox validateMethodCheckBox;
    private JCheckBox validateSchemeCheckBox;
    private JCheckBox requireSecurityCredentialsToCheckBox;
    private JLabel credentialsWarningLabel;

    public SwaggerServiceConfigurationPanel(WizardStepPanel<SwaggerServiceConfig> next) {
        super(next);

        initComponents();
    }

    private void initComponents() {
        securityZoneWidget.configure(Arrays.asList(new EntityType[]{EntityType.SERVICE, EntityType.POLICY}),
                OperationType.CREATE, null);

        setLayout(new BorderLayout());

        add(contentPanel);

        // add a bold, red warning label that is visible when "Require Security Credentials to be Present" is checked
        credentialsWarningLabel.setVisible(false);
        credentialsWarningLabel.setForeground(Color.RED);
        Font warningFont = credentialsWarningLabel.getFont()
                .deriveFont(Font.BOLD, credentialsWarningLabel.getFont().getSize() - 1);
        credentialsWarningLabel.setFont(warningFont);
        credentialsWarningLabel.setText(resources.getString("credentialsWarning"));

        requireSecurityCredentialsToCheckBox.addChangeListener(new RunOnChangeListener() {
            @Override
            protected void run() {
                credentialsWarningLabel.setVisible(requireSecurityCredentialsToCheckBox.isSelected());
            }
        });

        gatewayUrlPrefixLabel.setText("http(s)://" + TopComponents.getInstance().ssgURL().getHost() + ":[port]");

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
                } else if (serviceName.length() > SERVICE_NAME_MAX_LENGTH) {
                    return MessageFormat.format(resources.getString("serviceNameTooLongError"), SERVICE_NAME_MAX_LENGTH);
                }

                return null;
            }
        });

        // must have an API Host including host and (optionally) port
        validationRules.add(new InputValidator.ComponentValidationRule(apiHostTextField) {
            @Override
            public String getValidationError() {
                String apiHost = apiHostTextField.getText().trim();

                if (StringUtils.isBlank(apiHost)) {
                    return resources.getString("apiHostNotSpecifiedError");
                }

                // parse from "host[:port]" using placeholder for default port
                Pair<String, String> hostAndPort = InetAddressUtil.getHostAndPort(apiHost, "X");

                if (!InetAddressUtil.isValidHostName(hostAndPort.left) &&
                        !InetAddressUtil.isValidIpv4Address(hostAndPort.left) &&
                        !InetAddressUtil.isValidIpv6Address(hostAndPort.left)) {
                    return resources.getString("invalidApiHostError");
                }

                // if the parsed port is not the placeholder default, validate it
                if (!"X".equals(hostAndPort.right)) {
                    try {
                        final Integer port = Integer.valueOf(hostAndPort.right);

                        if (port < 1 || port > 65535) {
                            return resources.getString("invalidApiHostError");
                        }
                    } catch (NumberFormatException e) {
                        return resources.getString("invalidApiHostError");
                    }
                }

                return null;
            }
        });

        // must have a valid API Base Path with a leading slash
        validationRules.add(new InputValidator.ComponentValidationRule(apiBasePathTextField) {
            @Override
            public String getValidationError() {
                String apiBasePath = apiBasePathTextField.getText().trim();

                String errorMessage = null;

                if (StringUtils.isBlank(apiBasePath)) {
                    errorMessage = resources.getString("apiBasePathNotSpecifiedError");
                } else if (!apiBasePath.startsWith("/")) {
                    errorMessage = resources.getString("noLeadingSlashApiBasePathError");
                } else {
                    try {
                        URI uri = new URI(apiBasePath);

                        if (!uri.getPath().equals(apiBasePath)) {   // should be the path only - no file or parameters
                            errorMessage = resources.getString("invalidApiBasePathError");
                        }
                    } catch (URISyntaxException e) {
                        errorMessage = resources.getString("invalidApiBasePathError");
                    }
                }

                return errorMessage;
            }
        });

        // must have a valid routing uri
        validationRules.add(new InputValidator.ComponentValidationRule(routingUriTextField) {
            @Override
            public String getValidationError() {
                String routingUri = routingUriTextField.getText().trim();

                if (StringUtils.isBlank(routingUri)) {
                    return resources.getString("routingUriNotSpecifiedError");
                }

                if (!routingUri.startsWith("/")) {
                    return resources.getString("routingUriMissingLeadingSlashError");
                }

                String errorMsg = ValidationUtils.isValidUriString(routingUri);

                return null == errorMsg
                        ? ValidatorUtils.validateResolutionPath(routingUri, true, true)
                        : errorMsg;
            }
        });

        InputValidator inputValidator = new InputValidator(this, "Wizard Warning");

        inputValidator.validate();

        RunOnChangeListener enableDisableListener = new RunOnChangeListener() {
            @Override
            public void run() {
                enableDisableComponents();
            }
        };

        validatePathCheckBox.addChangeListener(enableDisableListener);
        validateMethodCheckBox.addChangeListener(enableDisableListener);
    }

    @Override
    public void storeSettings(SwaggerServiceConfig settings) throws IllegalArgumentException {
        settings.setServiceName(serviceNameTextField.getText().trim());
        settings.setApiHost(apiHostTextField.getText().trim());
        settings.setApiBasePath(apiBasePathTextField.getText().trim());
        settings.setRoutingUri(routingUriTextField.getText().trim());
        settings.setValidatePath(validatePathCheckBox.isSelected());
        settings.setValidateMethod(validateMethodCheckBox.isSelected());
        settings.setValidateScheme(validateSchemeCheckBox.isSelected());
        settings.setRequireSecurityCredentials(requireSecurityCredentialsToCheckBox.isSelected());
        settings.setSecurityZone(securityZoneWidget.getSelectedZone());
    }

    @Override
    public void readSettings(SwaggerServiceConfig settings) throws IllegalArgumentException {
        if (null == settings.getServiceName() || settings.getServiceName().isEmpty()) {
            serviceNameTextField.setText(settings.getApiTitle());
        } else {
            serviceNameTextField.setText(settings.getServiceName());
        }

        apiHostTextField.setText(settings.getApiHost());
        apiBasePathTextField.setText(settings.getApiBasePath());

        if ((null != settings.getApiBasePath() && !settings.getApiBasePath().isEmpty()) &&
                (null == settings.getRoutingUri() || settings.getRoutingUri().isEmpty())) {
            routingUriTextField.setText(settings.getApiBasePath() + "/*");
        } else {
            routingUriTextField.setText(settings.getRoutingUri());
        }

        validatePathCheckBox.setSelected(settings.isValidatePath());
        validateMethodCheckBox.setSelected(settings.isValidateMethod());
        validateSchemeCheckBox.setSelected(settings.isValidateScheme());
        requireSecurityCredentialsToCheckBox.setSelected(settings.isRequireSecurityCredentials());

        securityZoneWidget.setSelectedZone(settings.getSecurityZone());

        updateUrlPreview();
        enableDisableComponents();
    }

    private void updateUrlPreview() {
        String routingUri = routingUriTextField.getText().trim();

        if (routingUri.startsWith("/") && ValidationUtils.isValidUri(routingUri)) {
            gatewayUrlPrefixLabel.setForeground(Color.BLACK);
            routingUriTextField.setForeground(Color.BLACK);
        } else {
            gatewayUrlPrefixLabel.setForeground(Color.RED);
            routingUriTextField.setForeground(Color.RED);
        }
    }

    @Override
    public boolean canFinish() {
        return true;
    }

    @Override
    public boolean canAdvance() {
        return false;
    }

    @Override
    public String getStepLabel() {
        return resources.getString("stepLabel");
    }

    @Override
    public String getDescription() {
        return resources.getString("stepDescription");
    }

    private void enableDisableComponents() {
        validateMethodCheckBox.setEnabled(validatePathCheckBox.isSelected());
        validateSchemeCheckBox.setEnabled(validateMethodCheckBox.isSelected());
        requireSecurityCredentialsToCheckBox.setEnabled(validateMethodCheckBox.isSelected());

        if (!validatePathCheckBox.isSelected()) {
            validateMethodCheckBox.setSelected(false);
        }

        if (!validateMethodCheckBox.isSelected()) {
            validateSchemeCheckBox.setSelected(false);
            requireSecurityCredentialsToCheckBox.setSelected(false);
        }
    }
}
