package com.l7tech.external.assertions.jsonschema.console;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.panels.*;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.jsonschema.JSONSchemaAssertion;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.json.JSONData;
import com.l7tech.json.JSONFactory;
import com.l7tech.json.JsonSchemaVersion;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.AssertionResourceType;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;
import com.networknt.schema.JsonSchemaException;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessControlException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.json.JSONSchema.ATTRIBUTE_SCHEMA_VERSION;
import static com.l7tech.json.JsonSchemaVersion.DRAFT_V2;
import static com.l7tech.json.JsonSchemaVersion.DRAFT_V4;
import static java.util.Arrays.stream;

public class JSONSchemaPropertiesDialog extends AssertionPropertiesOkCancelSupport<JSONSchemaAssertion> {

    private static final ResourceBundle resources = ResourceBundle.getBundle(JSONSchemaPropertiesDialog.class.getName());

    private static final String BORDER_TITLE_PREFIX = resources.getString("modeBorderTitlePrefix.text");
    private static final String CONFIGURED_IN_ADVANCE = resources.getString("specifyItem.label");
    private static final String MONITOR_URL = resources.getString("specifyUrlItem.label");
    private static final String MODE_HEADER = resources.getString("contentTypeOrLineHeader.label");
    private static final String JSON_SCHEMA_NOT_SUPPLIED = "A JSON Schema must be supplied when assertion is configured in advance.";
    private static final String UNKNOWN_MODE = "Unknown mode for JSON Schema selected";
    private static final String UNKNOWN_ASSERTION_RESOURCE = "Unknown assertion resource found in assertion";
    private static final String MESSAGE_ERROR_PARSING_JSON = "Error parsing the JSON document";
    private static final String MESSAGE_VERSION_MISMATCH = "JSON Schema %s chosen, but it does not agree with the " +
            "specified '$schema' property. Please fix the uri for '$schema' to be the same version as %s " +
            "or remove the '$schema' property.";
    private static final String MESSAGE_INVALID_SCHEMA = "Invalid JSON Schema.";
    private static final String MESSAGE_SCHEMA_ERROR = "JSON Schema Error";

    private static final String[] MODES = new String[] {CONFIGURED_IN_ADVANCE, MONITOR_URL, MODE_HEADER};

    private static final String[] VERSIONS = stream(JsonSchemaVersion.values())
            .map(JsonSchemaVersion::getDisplayName)
            .toArray(String[]::new);

    private JPanel mainPanel;
    private JComboBox<String> cbSchemaLocation;
    private JPanel borderPanel;
    private JTabbedPane innerTabHolder;
    private JButton readURLButton;
    private JButton readFileButton;
    private JTextArea jsonTextArea;
    private JPanel innerPanel;
    private JPanel targetMessagePanelHolder;
    private JPanel configuredInAdvancePanel;
    private JPanel monitorUrlPanel;
    private JPanel retrieveUrlPanel;
    private JComboBox<String> cbSchemaVersion;

    private MonitorUrlPanel urlPanel;
    private RegexWhiteListPanel fetchPanel;

    private TargetMessagePanel targetMessagePanel = new TargetMessagePanel();
    private static final Logger logger = Logger.getLogger(JSONSchemaPropertiesDialog.class.getName());

    public JSONSchemaPropertiesDialog(final Window parent, final JSONSchemaAssertion assertion) {
        super(JSONSchemaAssertion.class, parent, assertion, true);
        initComponents(assertion);
    }

    @Override
    public JSONSchemaAssertion getData(@NotNull final JSONSchemaAssertion assertion) throws ValidationException {

        if ( !targetMessagePanel.isValidTarget() ) {
            throw new ValidationException("Invalid Target Message: " + targetMessagePanel.check());
        }

        final Object selectedMode = cbSchemaLocation.getSelectedItem();
        if (CONFIGURED_IN_ADVANCE.equals(selectedMode)) {

            final String jsonSchemaText = jsonTextArea.getText();

            if(jsonSchemaText.trim().isEmpty()){
                logger.log(Level.FINE, JSON_SCHEMA_NOT_SUPPLIED);
                throw new ValidationException(JSON_SCHEMA_NOT_SUPPLIED);
            }

            assertion.setJsonSchemaVersion(getCurrentFetchVersion());

            //only validate the json if no variables are referenced
            final boolean referencesVariables = Syntax.getReferencedNames(jsonSchemaText, false).length > 0;
            if(!referencesVariables){
                updateVersionModel(assertion, jsonSchemaText);
                validateJsonSchema(jsonSchemaText, assertion.getJsonSchemaVersion());
            }

            final String jsonSchema = jsonTextArea.getText();
            final StaticResourceInfo sri = new StaticResourceInfo(jsonSchema);
            assertion.setResourceInfo(sri);

        } else if (MONITOR_URL.equals(selectedMode)) {
            final String err = urlPanel.check();
            if (err != null) {
                throw new ValidationException("Monitor URL Error: " + err);
            }
            assertion.setJsonSchemaVersion(getCurrentFetchVersion());
            urlPanel.updateModel(assertion);

        } else if (MODE_HEADER.equals(selectedMode)) {
            final String err = fetchPanel.check();
            if (err != null) {
                throw new ValidationException("Retrieve URL Error: " + err);
            }
            assertion.setJsonSchemaVersion(getCurrentFetchVersion());
            fetchPanel.updateModel(assertion);

        } else {
            throw new IllegalStateException(UNKNOWN_MODE);
        }

        targetMessagePanel.updateModel(assertion);
        return assertion;
    }

    @Override
    public void setData(final JSONSchemaAssertion assertion) {
        updateTargetModel(assertion);
        final AssertionResourceInfo resourceInfo = assertion.getResourceInfo();
        if (resourceInfo instanceof StaticResourceInfo) {
            StaticResourceInfo sri = (StaticResourceInfo) resourceInfo;
            jsonTextArea.setText(sri.getDocument());
            jsonTextArea.setCaretPosition(0);
        }

        cbSchemaVersion.setSelectedItem(assertion.getJsonSchemaVersion().getDisplayName());
    }

    private static JsonSchemaVersion scanSchemaVersion(final String jsonSchemaText,
                                                       final JsonSchemaVersion versionToSet)
            throws IllegalArgumentException, JsonSchemaException {
        try {
            final JsonNode root = new ObjectMapper().readTree(jsonSchemaText);
            final JsonNode nodeWithVersion = root.get(ATTRIBUTE_SCHEMA_VERSION);
            switch (versionToSet) {
                case DRAFT_V2:
                    if (nodeWithVersion == null || DRAFT_V2.matchesSchemaUri(nodeWithVersion.textValue())) {
                        return DRAFT_V2;
                    }
                    throw new JsonSchemaException(
                            String.format(MESSAGE_VERSION_MISMATCH, versionToSet.name(), versionToSet.name()));

                case DRAFT_V4:
                    if (nodeWithVersion == null || DRAFT_V4.matchesSchemaUri(nodeWithVersion.textValue())) {
                        return DRAFT_V4;
                    }
                    throw new JsonSchemaException(
                            String.format(MESSAGE_VERSION_MISMATCH, versionToSet.name(), versionToSet.name()));

                default:
                    throw new IllegalArgumentException("Unknown version: " + versionToSet);
            }
        } catch (JsonProcessingException e) {
            final String errMessage = e.getOriginalMessage();
            final StringBuilder stringBuilder = new StringBuilder(MESSAGE_ERROR_PARSING_JSON);

            final String errorReason = stringBuilder.append(".\n")
                    .append(errMessage)
                    .toString();

            throw new JsonSchemaException(errorReason);
        } catch (IOException e) {
            throw new JsonSchemaException(MESSAGE_ERROR_PARSING_JSON);
        }
    }

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }

    /**
     * Gets current selected item in the Version combo box
     * @return The version as a string
     */
    private JsonSchemaVersion getCurrentFetchVersion() {
        return JsonSchemaVersion.fromDisplayName((String) cbSchemaVersion.getSelectedItem());
    }

    /**
     * Triggered when a user selects a new version on the combo box
     */
    private void updateVersionModel(final JSONSchemaAssertion assertion, final String jsonSchemaText) {
        final JsonSchemaVersion versionToSet = getCurrentFetchVersion();

        try {
            assertion.setJsonSchemaVersion(scanSchemaVersion(jsonSchemaText, versionToSet));

        } catch (JsonSchemaException | IllegalArgumentException e) {
            logger.log(Level.FINE, e.getMessage());
            throw new ValidationException(e.getMessage());
        }
    }

    private void validateJsonSchema(final String jsonSchemaText, final JsonSchemaVersion version)
            throws ValidationException{
        try {
            final JSONData jsonNode = JSONFactory.INSTANCE.newJsonData(jsonSchemaText, version);
            JSONFactory.INSTANCE.newJsonSchema(jsonNode);

        } catch (Exception ex) {
            logger.log(Level.FINE, MESSAGE_INVALID_SCHEMA, ex);
            throw new ValidationException(MESSAGE_INVALID_SCHEMA + "\n" + ex.getMessage(), MESSAGE_SCHEMA_ERROR, ex);
        }
    }

    private void initComponents(final JSONSchemaAssertion assertion) {
        innerTabHolder.remove(configuredInAdvancePanel);
        innerTabHolder.remove(monitorUrlPanel);
        innerTabHolder.remove(retrieveUrlPanel);

        innerPanel.removeAll();
        innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.Y_AXIS));
        innerPanel.add(configuredInAdvancePanel);
        innerPanel.add(monitorUrlPanel);
        innerPanel.add(retrieveUrlPanel);

        fetchPanel = new RegexWhiteListPanel(this, assertion, resources);
        retrieveUrlPanel.add(fetchPanel, BorderLayout.CENTER);

        urlPanel = new MonitorUrlPanel(assertion, resources);
        monitorUrlPanel.add(urlPanel, BorderLayout.CENTER);

        cbSchemaLocation.setModel(new DefaultComboBoxModel<>(MODES));
        cbSchemaLocation.addActionListener((ActionEvent e) -> updateModeComponents());

        final AssertionResourceInfo ri = assertion.getResourceInfo();
        final AssertionResourceType rit = ri.getType();
        if (AssertionResourceType.MESSAGE_URL.equals(rit)) {
            cbSchemaLocation.setSelectedItem(MODE_HEADER);

        } else if (AssertionResourceType.SINGLE_URL.equals(rit)) {
            cbSchemaLocation.setSelectedItem(MONITOR_URL);

        } else if (AssertionResourceType.STATIC.equals(rit)) {
            cbSchemaLocation.setSelectedItem(CONFIGURED_IN_ADVANCE);

        } else {
            logger.log(Level.WARNING, UNKNOWN_ASSERTION_RESOURCE);
            throw new RuntimeException(UNKNOWN_ASSERTION_RESOURCE);
        }

        cbSchemaVersion.setModel(new DefaultComboBoxModel<>(VERSIONS));
        cbSchemaVersion.setSelectedItem(assertion.getJsonSchemaVersion().getDisplayName());

        targetMessagePanelHolder.add(targetMessagePanel);

        updateTargetModel(assertion);

        readFileButton.addActionListener(e -> readFromFile(assertion.getJsonSchemaVersion()));
        readURLButton.addActionListener(e -> handleReadUrlButtonClick(assertion));

        SwingUtilities.invokeLater(this::updateModeComponents);

        super.initComponents();
    }

    private void handleReadUrlButtonClick(final JSONSchemaAssertion assertion) {
        final OkCancelDialog<String> dlg = new OkCancelDialog<>(
                JSONSchemaPropertiesDialog.this,
                resources.getString("urlDialog.title"),
                true,
                new UrlPanel(resources.getString("urlDialog.prompt"), null));

        dlg.pack();
        Utilities.centerOnScreen(dlg);

        DialogDisplayer.display(dlg, () -> {
            String url = dlg.getValue();
            if (url != null) {
                try {
                    readFromUrl(url, assertion.getJsonSchemaVersion());

                } catch (AccessControlException ace) {
                    TopComponents.getInstance().showNoPrivilegesErrorMessage();
                }
            }
        });
    }

    private void updateTargetModel(final JSONSchemaAssertion assertion) {
        targetMessagePanel.setModel(new MessageTargetableAssertion() {{
            TargetMessageType targetMessageType = assertion.getTarget();
            if ( targetMessageType != null ) {
                setTarget(targetMessageType);
            } else {
                clearTarget();
            }
            setOtherTargetMessageVariable(assertion.getOtherTargetMessageVariable());
        }},getPreviousAssertion());
    }

    private void readFromUrl(final String url, final JsonSchemaVersion version){
        if (url == null || url.length() < 1) {
            displayError(resources.getString("error.nourl"), null);
            return;
        }

        //validate the URL
        try {
            new URL(url);
        } catch (MalformedURLException e) {
            final String errorMsg = url + " " + resources.getString("error.badurl");
            logger.log(Level.FINE, errorMsg, e);
        }

        final ServiceAdmin schemaAdmin = getServiceAdmin();
        Either<String, String> jsonSchema;
        try {
            jsonSchema = AdminGuiUtils.doAsyncAdmin(
                    schemaAdmin,
                    JSONSchemaPropertiesDialog.this,
                    resources.getString("urlLoadingDialog.title"),
                    MessageFormat.format(resources.getString("urlLoadingDialog.message"), url),
                    schemaAdmin.resolveUrlTargetAsync(url, JSONSchemaAssertion.CPROP_JSON_SCHEMA_MAX_DOWNLOAD_SIZE));

        } catch (InterruptedException e) {
            //do nothing the user cancelled
            return;

        } catch (InvocationTargetException e) {
            jsonSchema = Either.left(ExceptionUtils.getMessage(e));
        }

        if (jsonSchema.isLeft()) {
            //An error occurred retrieving the document
            final String errorMsg = "Cannot download document: " + jsonSchema.left();
            displayError(errorMsg, "Errors downloading file");
            logger.log(Level.FINE, errorMsg);
            return;
        }

        try {
            validateJsonSchema(jsonSchema.right(), version);

        } catch (ValidationException e) {
            //this is likely to be a GenericHttpException
            final String errorMsg = "Invalid JSON Schema: " + ExceptionUtils.getMessage(e);
            displayError(errorMsg, "Invalid JSON Schema");
            logger.log(Level.FINE, errorMsg, e);
            return;
        }

        jsonTextArea.setText(jsonSchema.right());
    }

    private ServiceAdmin getServiceAdmin() {
        final ServiceAdmin serviceAdmin;
        final Registry reg = Registry.getDefault();
        if (reg == null || reg.getServiceManager() == null) {
            throw new RuntimeException("No access to registry. Cannot download document.");
        } else {
            serviceAdmin = reg.getServiceManager();
        }

        return serviceAdmin;
    }

    private void readFromFile(final JsonSchemaVersion version) {
        SsmApplication.doWithJFileChooser(fc -> doRead(fc, version));
    }

    private void doRead(final JFileChooser dlg, final JsonSchemaVersion version) {
        if (JFileChooser.APPROVE_OPTION != dlg.showOpenDialog(this)) {
            return;
        }
        final FileInputStream fis;
        final File selectedFile = dlg.getSelectedFile();
        try {
            fis = new FileInputStream(selectedFile);
        } catch (FileNotFoundException e) {
            final String msg = "Cannot open file " + selectedFile.getAbsolutePath()+". Error: " + ExceptionUtils.getMessage(e);
            displayError(msg, "Cannot open file");
            logger.log(Level.FINE, msg, e);
            return;
        }

        final String jsonData;
        try {
            jsonData = new String(IOUtils.slurpStream(fis));
        } catch (IOException e) {
            final String msg = "Cannot read file " + selectedFile.getAbsolutePath()+". Error: " + ExceptionUtils.getMessage(e);
            displayError(msg, "Cannot read file");
            logger.log(Level.FINE, msg, e);
            return;
        }

        try {
            validateJsonSchema(jsonData, version);
        } catch (ValidationException e) {
            //this is likely to be a GenericHttpException
            final String errorMsg = "Invalid JSON Schema: " + ExceptionUtils.getMessage(e);
            displayError(errorMsg, "Invalid JSON Schema");
            logger.log(Level.FINE, errorMsg, e);
            return;
        }

        jsonTextArea.setText(jsonData);
    }

    private void displayError(final String msg, final String title) {
        final FontMetrics fontMetrics = this.getFontMetrics(this.getFont());
        final int width = Utilities.computeStringWidth(fontMetrics, msg);

        final Object object;
        if (width > 600) {
            object = Utilities.getTextDisplayComponent( msg, 600, 100, -1, -1 );
        } else {
            object = msg;
        }

        final String titleOrError = title != null ? title : resources.getString("error.window.title");
        JOptionPane.showMessageDialog(this, object, titleOrError, JOptionPane.ERROR_MESSAGE);
    }

    //- PRIVATE

    private String getCurrentFetchMode() {
        return (String)cbSchemaLocation.getSelectedItem();
    }

    private void updateModeComponents() {
        String mode = getCurrentFetchMode();
        if (MONITOR_URL.equals(mode)) {
            monitorUrlPanel.setVisible(true);
            configuredInAdvancePanel.setVisible(false);
            retrieveUrlPanel.setVisible(false);

        } else if (CONFIGURED_IN_ADVANCE.equals(mode)) {
            configuredInAdvancePanel.setVisible(true);
            monitorUrlPanel.setVisible(false);
            retrieveUrlPanel.setVisible(false);

        } else if (MODE_HEADER.equals(mode)) {
            retrieveUrlPanel.setVisible(true);
            monitorUrlPanel.setVisible(false);
            configuredInAdvancePanel.setVisible(false);

        } else {
            throw new RuntimeException("unhandled schema validation mode: " + mode);
        }

        Border border = borderPanel.getBorder();
        if (border instanceof TitledBorder) {
            TitledBorder tb = (TitledBorder)border;
            tb.setTitle(BORDER_TITLE_PREFIX + " " + mode);
        }
        innerPanel.revalidate();
    }

}
