package com.l7tech.external.assertions.swagger.console;

import com.l7tech.console.action.ManageHttpConfigurationAction;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.swagger.SwaggerAdmin;
import com.l7tech.external.assertions.swagger.SwaggerApiMetadata;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.external.assertions.swagger.console.PublishSwaggerServiceWizard.SwaggerServiceConfig;

public class SwaggerDocumentPanel extends WizardStepPanel<SwaggerServiceConfig> {
    private static final Logger logger = Logger.getLogger(SwaggerDocumentPanel.class.getName());

    private static final ResourceBundle resources =
            ResourceBundle.getBundle(SwaggerDocumentPanel.class.getName());

    private JPanel contentPanel;
    private JTextField documentLocationTextField;
    private JButton manageHttpOptionsButton;

    private String lastParsedDocumentLocation = null;
    private SwaggerApiMetadata parsedApiMetadata = null;

    public SwaggerDocumentPanel(WizardStepPanel<SwaggerServiceConfig> next) {
        super(next);

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        add(contentPanel);

        // add manage HTTP configuration action
        manageHttpOptionsButton.setAction(new ManageHttpConfigurationAction(this));
        manageHttpOptionsButton.setIcon(null);
        manageHttpOptionsButton.setText("HTTP Options");

        // document location must be a valid url with HTTP or HTTPS protocol
        validationRules.add(new InputValidator.ComponentValidationRule(documentLocationTextField) {
            @Override
            public String getValidationError() {
                String location = documentLocationTextField.getText().trim();

                if (location.equals(lastParsedDocumentLocation)) {
                    return null;    // no need to download and validate - it was valid last time
                }

                URL url;
                boolean validUrl = false;

                try {
                    url = new URL(location);

                    validUrl = !url.getHost().isEmpty()
                            && (url.getProtocol().equals("http") || url.getProtocol().equals("https"));
                } catch (MalformedURLException e) {
                    // ignore - validUrl will be false
                }

                if (!validUrl) {
                    return MessageFormat.format(resources.getString("invalidDocumentLocationUrlError"), location);
                }

                final SwaggerAdmin swaggerAdmin = getSwaggerAdmin();

                Either<String, SwaggerApiMetadata> resolveResult;

                try {
                    resolveResult = AdminGuiUtils.doAsyncAdmin(
                            swaggerAdmin,
                            SwaggerDocumentPanel.this.getOwner(),
                            resources.getString("documentLoadingDialog.title"),
                            MessageFormat.format(resources.getString("documentLoadingDialog.message"), location),
                            swaggerAdmin.retrieveApiMetadataAsync(location)
                    );
                } catch (InterruptedException e) {
                    // cancelled by the user - counts as validation failure
                    return resources.getString("documentDownloadInterruptedError");
                } catch (InvocationTargetException e) {
                    resolveResult = Either.left(ExceptionUtils.getMessage(e));
                }

                if (resolveResult.isLeft()) {
                    // an error occurred retrieving the metadata
                    final String errorMsg = resolveResult.left();
                    logger.log(Level.FINE, errorMsg);
                    return errorMsg;
                } else if (null == resolveResult.right()) {
                    final String errorMsg = resources.getString("documentDownloadFailedError");
                    logger.log(Level.FINE, errorMsg);
                    return errorMsg;
                }

                lastParsedDocumentLocation = location;
                parsedApiMetadata = resolveResult.right();

                return null;
            }
        });
    }

    private SwaggerAdmin getSwaggerAdmin() {
        final Registry reg = Registry.getDefault();

        final SwaggerAdmin swaggerAdmin = reg.getExtensionInterface(SwaggerAdmin.class, null);

        if (null == swaggerAdmin) {
            throw new RuntimeException("No access to registry. Cannot parse document.");
        }

        return swaggerAdmin;
    }

    @Override
    public void storeSettings(SwaggerServiceConfig settings) throws IllegalArgumentException {
        settings.setDocumentUrl(lastParsedDocumentLocation);

        if (null == parsedApiMetadata ||
                null == parsedApiMetadata.getHost() ||
                parsedApiMetadata.getHost().isEmpty()) {
            String apiHost = null;

            try {
                URL url = new URL(lastParsedDocumentLocation);

                if (url.getPort() != -1) {
                    apiHost = url.getHost() + ":" + url.getPort();
                } else {
                    apiHost = url.getHost();
                }
            } catch (MalformedURLException e) {
                logger.warning("Could not determine API Host from Swagger document URL: " + lastParsedDocumentLocation);
            }

            settings.setApiHost(apiHost);
        } else {
            settings.setApiHost(parsedApiMetadata.getHost());
        }

        if (null != parsedApiMetadata) {
            settings.setApiTitle(parsedApiMetadata.getTitle());
            settings.setApiBasePath(parsedApiMetadata.getBasePath());
        }
    }

    @Override
    public void readSettings(SwaggerServiceConfig settings) throws IllegalArgumentException {
        documentLocationTextField.setText(settings.getDocumentUrl());
    }

    @Override
    public boolean canFinish() {
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
}
