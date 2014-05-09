package com.l7tech.external.assertions.jsonschema.console;

import com.l7tech.console.SsmApplication;
import com.l7tech.console.panels.*;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.jsonschema.JSONSchemaAssertion;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.json.JSONData;
import com.l7tech.json.JSONFactory;
import com.l7tech.policy.AssertionResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.AssertionResourceType;
import com.l7tech.policy.assertion.MessageTargetableAssertion;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.util.Either;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.IOUtils;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

public class JSONSchemaPropertiesDialog extends AssertionPropertiesOkCancelSupport<JSONSchemaAssertion> {

    public JSONSchemaPropertiesDialog(final Window parent, final JSONSchemaAssertion assertion) {
        super(JSONSchemaAssertion.class, parent, assertion, true);
        initComponents(assertion);
    }

    @Override
    public JSONSchemaAssertion getData(JSONSchemaAssertion assertion) throws ValidationException {

        if ( !targetMessagePanel.isValidTarget() ) {
            throw new ValidationException("Invalid Target Message: " + targetMessagePanel.check());
        }

        final Object selectedItem = cbSchemaLocation.getSelectedItem();
        if(CONFIGURED_IN_ADVANCE.equals(selectedItem)){
            final String jsonSchemaText = jsonTextArea.getText();

            if(jsonSchemaText.trim().isEmpty()){
                final String message = "A JSON Schema must be supplied when assertion is configured in advance.";
                logger.log(Level.FINE, message);
                throw new ValidationException(message);
            }

            final boolean referencesVariables = Syntax.getReferencedNames(jsonSchemaText, false).length > 0;
            if(!referencesVariables){
                //only validate the json if no variables are referenced
                validateJsonSchema(jsonSchemaText);
            }

            final String jsonSchema = jsonTextArea.getText();
            StaticResourceInfo sri = new StaticResourceInfo(jsonSchema);
            assertion.setResourceInfo(sri);
        } else if(MONITOR_URL.equals(selectedItem)){
            final String err = urlPanel.check();
            if (err != null) throw new ValidationException("Monitor URL Error: " + err);
            urlPanel.updateModel(assertion);
        } else if(MODE_HEADER.equals(selectedItem)){
            final String err = fetchPanel.check();
            if( err != null ) throw new ValidationException("Retrieve URL Error: " + err);
            fetchPanel.updateModel(assertion);
        } else {
            throw new IllegalStateException("Unknown mode for JSON Schema selected");
        }

        targetMessagePanel.updateModel(assertion);
        return assertion;
    }

    private void validateJsonSchema(String jsonSchemaText) throws ValidationException{
        JSONFactory jsonFactory = JSONFactory.getInstance();

        final JSONData jsonNode = jsonFactory.newJsonData(jsonSchemaText);
        try {
            jsonFactory.newJsonSchema(jsonNode);
        } catch (Exception ex) {
            final String msg = "Invalid JSON Schema.";
            logger.log(Level.FINE, msg, ExceptionUtils.getDebugException(ex));
            throw new ValidationException(msg, "JSON Schema Error", ExceptionUtils.getDebugException(ex));
        }
    }

    @Override
    public void setData(JSONSchemaAssertion assertion) {
        updateTargetModel(assertion);
        final AssertionResourceInfo resourceInfo = assertion.getResourceInfo();
        if(resourceInfo instanceof StaticResourceInfo){
            StaticResourceInfo sri = (StaticResourceInfo) resourceInfo;
            jsonTextArea.setText(sri.getDocument());
            jsonTextArea.setCaretPosition(0);
        } 
    }

    //- PROTECTED

    @Override
    protected JPanel createPropertyPanel() {
        return mainPanel;
    }
    
    protected void initComponents(final JSONSchemaAssertion assertion) {

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

        cbSchemaLocation.setModel(new DefaultComboBoxModel(MODES));

        cbSchemaLocation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateModeComponents();
            }
        });

        AssertionResourceInfo ri = assertion.getResourceInfo();
        AssertionResourceType rit = ri.getType();
        if (AssertionResourceType.MESSAGE_URL.equals(rit)) {
            cbSchemaLocation.setSelectedItem(MODE_HEADER);
        } else if (AssertionResourceType.SINGLE_URL.equals(rit)) {
            cbSchemaLocation.setSelectedItem(MONITOR_URL);
        } else if (AssertionResourceType.STATIC.equals(rit)) {
            cbSchemaLocation.setSelectedItem(CONFIGURED_IN_ADVANCE);
        } else {
            final String msg = "Unknown assertion resource found in assertion";
            logger.log(Level.WARNING, msg);
            throw new RuntimeException(msg);
        }

        targetMessagePanelHolder.add(targetMessagePanel);

        updateTargetModel(assertion);

        readFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                readFromFile();
            }
        });

        readURLButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final OkCancelDialog dlg = new OkCancelDialog<String>(JSONSchemaPropertiesDialog.this,
                                                        resources.getString("urlDialog.title"),
                                                        true,
                                                        new UrlPanel(resources.getString("urlDialog.prompt"), null));
                dlg.pack();
                Utilities.centerOnScreen(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    @Override
                    public void run() {
                        String url = (String)dlg.getValue();
                        if (url != null) {
                            try {
                                readFromUrl(url);
                            } catch (AccessControlException ace) {
                                TopComponents.getInstance().showNoPrivilegesErrorMessage();
                            }
                        }
                    }
                });
            }
        });
        
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                updateModeComponents();
            }
        });

        super.initComponents();        
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

    private void readFromUrl(String url){
        if (url == null || url.length() < 1) {
            displayError(resources.getString("error.nourl"), null);
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
            validateJsonSchema(jsonSchema.right());
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

    private void readFromFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
            @Override
            public void useFileChooser(JFileChooser fc) {
                doRead(fc);
            }
        });
    }

    private void doRead(JFileChooser dlg) {
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
            validateJsonSchema(jsonData);
        } catch (ValidationException e) {
            //this is likely to be a GenericHttpException
            final String errorMsg = "Invalid JSON Schema: " + ExceptionUtils.getMessage(e);
            displayError(errorMsg, "Invalid JSON Schema");
            logger.log(Level.FINE, errorMsg, e);
            return;
        }

        jsonTextArea.setText(jsonData);
    }

    private void displayError(String msg, String title) {
        if (title == null) title = resources.getString("error.window.title");
        final FontMetrics fontMetrics = this.getFontMetrics(this.getFont());
        final int width = Utilities.computeStringWidth(fontMetrics, msg);
        final Object object;
        if(width > 600){
            object = Utilities.getTextDisplayComponent( msg, 600, 100, -1, -1 );
        }else{
            object = msg;
        }
        JOptionPane.showMessageDialog(this, object, title, JOptionPane.ERROR_MESSAGE);
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

    private static final ResourceBundle resources = ResourceBundle.getBundle( JSONSchemaPropertiesDialog.class.getName() );

    private final String BORDER_TITLE_PREFIX = resources.getString("modeBorderTitlePrefix.text");
    private final String CONFIGURED_IN_ADVANCE = resources.getString("specifyItem.label");
    private final String MONITOR_URL = resources.getString("specifyUrlItem.label");
    private final String MODE_HEADER = resources.getString("contentTypeOrLineHeader.label");

    private final String[] MODES = new String[] {
            CONFIGURED_IN_ADVANCE,
            MONITOR_URL,
            MODE_HEADER
    };

    private JPanel mainPanel;
    private JComboBox cbSchemaLocation;
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

    private MonitorUrlPanel urlPanel;
    private RegexWhiteListPanel fetchPanel;
    private TargetMessagePanel targetMessagePanel = new TargetMessagePanel();

    private static final Logger logger = Logger.getLogger(JSONSchemaPropertiesDialog.class.getName());
}
