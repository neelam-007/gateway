package com.l7tech.external.assertions.swagger.console;

import com.l7tech.console.action.ManageHttpConfigurationAction;
import com.l7tech.console.panels.WizardStepPanel;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.external.assertions.swagger.SwaggerAssertion;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gui.util.InputValidator;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import io.swagger.models.Swagger;
import io.swagger.models.auth.AuthorizationValue;
import io.swagger.parser.SwaggerParser;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;
import java.util.List;
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
    private Swagger parsedDocumentModel = null;

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

                final ServiceAdmin serviceAdmin = getServiceAdmin();

                Either<String, String> resolveResult;

                try {
                    resolveResult = AdminGuiUtils.doAsyncAdmin(
                            serviceAdmin,
                            SwaggerDocumentPanel.this.getOwner(),
                            resources.getString("documentLoadingDialog.title"),
                            MessageFormat.format(resources.getString("documentLoadingDialog.message"), location),
                            serviceAdmin.resolveUrlTargetAsync(location,
                                    SwaggerAssertion.CPROP_SWAGGER_DOC_MAX_DOWNLOAD_SIZE));
                } catch (InterruptedException e) {
                    // cancelled by the user - counts as validation failure
                    return resources.getString("documentDownloadInterruptedError");
                } catch (InvocationTargetException e) {
                    resolveResult = Either.left(ExceptionUtils.getMessage(e));
                }

                if (resolveResult.isLeft()) {
                    // an error occurred retrieving the document
                    final String errorMsg =
                            MessageFormat.format(resources.getString("documentDownloadFailedWithReasonError"),
                            resolveResult.left());
                    logger.log(Level.FINE, errorMsg);
                    return errorMsg;
                } else if (resolveResult.right().isEmpty()) {
                    final String errorMsg = resources.getString("documentDownloadFailedError");
                    logger.log(Level.FINE, errorMsg);
                    return errorMsg;
                }

                String swaggerDocument = resolveResult.right();

                Swagger model;

                try {
                    SwaggerParser parser = new SwaggerParser();
                    List<AuthorizationValue> authorizationValues = new ArrayList<>();
                    authorizationValues.add(new AuthorizationValue());
                    model = parser.parse(swaggerDocument, authorizationValues);
                } catch (Exception e) {
                    logger.log(Level.FINE, e.getMessage());
                    return resources.getString("parseDocumentFailed");
                }

                // the parser returns null if it could not parse the document
                if (null == model) {
                    return resources.getString("parseDocumentFailed");
                }

                lastParsedDocumentLocation = location;
                parsedDocumentModel = model;

                return null;
            }
        });
    }

    private ServiceAdmin getServiceAdmin() {
        final Registry reg = Registry.getDefault();

        final ServiceAdmin serviceAdmin = reg.getServiceManager();

        if (null == serviceAdmin) {
            throw new RuntimeException("No access to registry. Cannot download document.");
        }

        return serviceAdmin;
    }

    @Override
    public void storeSettings(SwaggerServiceConfig settings) throws IllegalArgumentException {
        settings.setDocumentUrl(lastParsedDocumentLocation);

        if (null == parsedDocumentModel.getHost() || parsedDocumentModel.getHost().isEmpty()) {
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
            settings.setApiHost(parsedDocumentModel.getHost());
        }

        settings.setApiBasePath(parsedDocumentModel.getBasePath());

        if (null != parsedDocumentModel.getInfo()) {
            settings.setApiTitle(parsedDocumentModel.getInfo().getTitle());
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
