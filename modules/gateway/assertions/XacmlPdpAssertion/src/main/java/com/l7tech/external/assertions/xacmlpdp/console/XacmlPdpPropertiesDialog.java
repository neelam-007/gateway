package com.l7tech.external.assertions.xacmlpdp.console;

import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.l7tech.common.io.ByteOrderMarkInputStream;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.action.ManageHttpConfigurationAction;
import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.panels.TargetVariablePanel;
import com.l7tech.console.panels.UrlPanel;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.util.AdminGuiUtils;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.VariablePrefixUtil;
import com.l7tech.console.util.XMLContainerFactory;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.external.assertions.xacmlpdp.XacmlPdpAssertion;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.RunOnChangeListener;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.util.*;
import com.sun.xacml.Policy;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * User: njordan
 * Date: 13-Mar-2009
 * Time: 5:29:48 PM
 */
public class XacmlPdpPropertiesDialog extends AssertionPropertiesEditorSupport<XacmlPdpAssertion> {
    private static final ResourceBundle resources = ResourceBundle.getBundle( XacmlPdpPropertiesDialog.class.getName() );
    public static class MessageSourceEntry {
        private XacmlAssertionEnums.MessageLocation messageSource;
        private String messageVariableName;

        public MessageSourceEntry(XacmlAssertionEnums.MessageLocation messageSource,
                                  String messageVariableName) {
            this.messageSource = messageSource;
            this.messageVariableName = messageVariableName;
        }

        public XacmlAssertionEnums.MessageLocation getMessageSource() {
            return messageSource;
        }

        public String getMessageVariableName() {
            return messageVariableName;
        }

        @Override
        public String toString() {
            if(messageSource != XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE){
                return messageSource.getLocationName();
            }else{
                return "Context Variable: " +
                        Syntax.SYNTAX_PREFIX + messageVariableName + Syntax.SYNTAX_SUFFIX;
            }
        }
    }

    private static final Logger log = Logger.getLogger(XacmlPdpPropertiesDialog.class.getName());

    private static final String CONFIGURED_IN_ADVANCE = "Configured in advance";
    private static final String MONITOR_URL = "Monitor URL for latest value";

    private JComboBox messageSourceComboBox;
    private JComboBox messageOutputComboBox;
    private JPanel mainPanel;
    private JPanel policyPanel;
    private JButton fetchFileButton;
    private JButton fetchUrlButton;
    private JCheckBox failCheckBox;
    private JButton okButton;
    private JButton cancelButton;
    private JTextField urlToMonitorField;
    private JButton manageHttpOptionsButton;
    private JPanel monitorUrlPolicyPanel;
    private JPanel preconfiguredPolicyPanel;
    private JComboBox policyLocationComboBox;
    private JPanel xacmlPolicyPanel;
    private JCheckBox sourceSOAPEncapsulatedCheckBox;
    private JCheckBox targetSOAPEncapsulatedCheckBox;
    private JLabel messageVariableLabel;
    private JPanel outputMessageVariableNamePanel;
    private TargetVariablePanel outputMessageVariableNameField;

    private UIAccessibility uiAccessibility;
    private String policyXml; // uiAccessibility editor cannot be used after dialog disposal

    private XacmlPdpAssertion assertion;
    private boolean confirmed;

    public XacmlPdpPropertiesDialog( final Frame owner, final XacmlPdpAssertion a ) {
        super(owner, a );
        assertion = a;
        initComponents();
        enableDisableComponents();
        DialogDisplayer.suppressSheetDisplay(this); // incompatible with xmlpad
    }

    private void initComponents() {
        Utilities.setEscKeyStrokeDisposes( this );
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        messageSourceComboBox.setModel(buildMessageSourceComboBoxModel(assertion));
        messageSourceComboBox.setRenderer(TextListCellRenderer.basicComboBoxRenderer());

        DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
        comboBoxModel.addElement(XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST);
        comboBoxModel.addElement(XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE);
        comboBoxModel.addElement(XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE);
        messageOutputComboBox.setModel(comboBoxModel);
        messageOutputComboBox.setRenderer( new TextListCellRenderer<XacmlAssertionEnums.MessageLocation>( new Functions.Unary<String,XacmlAssertionEnums.MessageLocation>(){
            @Override
            public String call( final XacmlAssertionEnums.MessageLocation messageLocation ) {
                return messageLocation.getLocationName();
            }
        }) );

        messageVariableLabel.setEnabled(false);


        outputMessageVariableNameField = new TargetVariablePanel();
        outputMessageVariableNamePanel.setLayout(new BorderLayout());
        outputMessageVariableNamePanel.add(outputMessageVariableNameField, BorderLayout.CENTER);
        outputMessageVariableNameField.setEnabled(false);

        policyLocationComboBox.setModel(new DefaultComboBoxModel(new String[] {CONFIGURED_IN_ADVANCE, resources.getString( "monitor.url.label" ) }));
        monitorUrlPolicyPanel.setVisible(false);
        ((TitledBorder)xacmlPolicyPanel.getBorder()).setTitle( resources.getString( "xacml.policy" ) + CONFIGURED_IN_ADVANCE);

        policyLocationComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                if(CONFIGURED_IN_ADVANCE.equals(policyLocationComboBox.getSelectedItem())) {
                    monitorUrlPolicyPanel.setVisible(false);
                    preconfiguredPolicyPanel.setVisible(true);
                    ((TitledBorder)xacmlPolicyPanel.getBorder()).setTitle(resources.getString( "xacml.policy" ) + CONFIGURED_IN_ADVANCE);
                } else if(MONITOR_URL.equals(policyLocationComboBox.getSelectedItem())) {
                    monitorUrlPolicyPanel.setVisible(true);
                    preconfiguredPolicyPanel.setVisible(false);
                    ((TitledBorder)xacmlPolicyPanel.getBorder()).setTitle(resources.getString( "xacml.policy" ) + MONITOR_URL);
                }
                enableDisableComponents();
            }
        });
        XMLContainer xmlContainer = XMLContainerFactory.createXmlContainer(true);
        final JComponent xmlEditor = xmlContainer.getView();
        uiAccessibility = xmlContainer.getUIAccessibility();

        Utilities.equalizeButtonSizes(new JButton[] {okButton, cancelButton});

        fetchFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                readFromFile();
            }
        });

        fetchUrlButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final OkCancelDialog<String> dlg = new OkCancelDialog<String>(XacmlPdpPropertiesDialog.this, resources.getString( "xacml.url.label" ), true, new UrlPanel( resources.getString( "xacml.policy.url" ), null));
                dlg.pack();
                Utilities.centerOnScreen(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    @Override
                    public void run() {
                        String url = dlg.getValue();
                        if (url != null) {
                            readFromUrl(url);
                        }
                    }
                });
            }
        });

        manageHttpOptionsButton.setAction( new ManageHttpConfigurationAction( this ) );
        manageHttpOptionsButton.setText(resources.getString("manageHttpOptionsButton.label"));
        manageHttpOptionsButton.setIcon(null);

        RunOnChangeListener validationListener = new RunOnChangeListener(new Runnable(){
            @Override
            public void run() {
                enableDisableComponents();
            }
        });

        uiAccessibility.getEditor().getDocument().addDocumentListener(validationListener);
        outputMessageVariableNameField.addChangeListener(validationListener);
        urlToMonitorField.getDocument().addDocumentListener(validationListener);
        messageOutputComboBox.addActionListener(validationListener);
        failCheckBox.addActionListener(validationListener);

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if ( validProperties() ) {
                    getData(assertion);
                    policyXml = uiAccessibility.getEditor().getText();
                    confirmed = true;
                    dispose();
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                dispose();
            }
        });

        // Initialize the XML editor to the XACML policy from the assertion, if any, else to an identity transform
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if(assertion.getResourceInfo() instanceof StaticResourceInfo) {
                    StaticResourceInfo sri = (StaticResourceInfo)assertion.getResourceInfo();
                    String policyString = sri.getDocument();

                    if(policyString == null || policyString.trim().length() < 1) {
                        policyString = "";
                    }

                    XMLEditor editor = uiAccessibility.getEditor();
                    try {
                        editor.setText(policyString);
                    } catch(Exception e) {
                        log.log(Level.WARNING, "Couldn't parse initial XACML policy", e);
                        editor.setText(policyString);
                    }
                    editor.setLineNumber(1);
                }
            }
        });

        Utilities.equalizeComponentSizes( messageSourceComboBox, messageOutputComboBox );

        policyPanel.setLayout(new BorderLayout());
        policyPanel.add(xmlEditor, BorderLayout.CENTER);

        setContentPane(mainPanel);
        Utilities.setRequestFocusOnOpen( this );
    }

    private ComboBoxModel buildMessageSourceComboBoxModel( final XacmlPdpAssertion assertion ) {
        final DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();

        comboBoxModel.addElement(
                new MessageSourceEntry(
                        XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST, null));

        comboBoxModel.addElement(
                new MessageSourceEntry(
                        XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE, null));

        final Map<String, VariableMetadata> predecessorVariables =
                (assertion.getParent() != null) ? SsmPolicyVariableUtils.getVariablesSetByPredecessors( assertion ) :
                (getPreviousAssertion() != null)? SsmPolicyVariableUtils.getVariablesSetByPredecessorsAndSelf( getPreviousAssertion() ) :
                Collections.<String, VariableMetadata>emptyMap();

        final SortedSet<String> predecessorVariableNames = new TreeSet<String>(predecessorVariables.keySet());
        for (String variableName: predecessorVariableNames) {
            if (predecessorVariables.get(variableName).getType() == DataType.MESSAGE) {
                final MessageSourceEntry item =
                        new MessageSourceEntry(
                                XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE,
                                variableName);
                comboBoxModel.addElement(item);
            }
        }

        return comboBoxModel;
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

        ByteOrderMarkInputStream bomis = null;
        String filename = dlg.getSelectedFile().getAbsolutePath();
        try {
            try {
                bomis = new ByteOrderMarkInputStream(new FileInputStream(dlg.getSelectedFile()));
            } catch (FileNotFoundException e) {
                log.log(Level.FINE, "cannot open file" + filename, e);
                return;
            } catch (IOException e) {
                log.log(Level.FINE, "cannot parse" + filename, e);
                return;
            }

            // try to get document
            Document doc;
            byte[] bytes;
            try {
                bytes = IOUtils.slurpStream(bomis);
                doc = XmlUtil.parse(new ByteArrayInputStream(bytes));
            } catch (SAXException e) {
                JOptionPane.showMessageDialog(this,
                        "Cannot parse the XML from file '" + filename+"'",
                        "XACML Policy Error",
                        JOptionPane.ERROR_MESSAGE);
                log.log(Level.FINE, "cannot parse " + filename, e);
                return;
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this,
                        "Cannot parse the XML from file '" + filename+"'",
                        "XACML Policy Error",
                        JOptionPane.ERROR_MESSAGE);
                log.log(Level.FINE, "cannot parse " + filename, e);
                return;
            }

            try {
                docIsXacmlPolicy(doc);

                uiAccessibility.getEditor().setText(new String(bytes));
                uiAccessibility.getEditor().setCaretPosition(0);

                enableDisableComponents();
            } catch (SAXException e) {
                JOptionPane.showMessageDialog(this,
                        "The file '" + filename + "' is not a valid XACML Policy: " + ExceptionUtils.getMessage(e),
                        "XACML Policy Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        } finally {
            ResourceUtils.closeQuietly(bomis);
        }
    }

    private void readFromUrl(String urlstr) {
        // try to get document
        final ServiceAdmin serviceAdmin;
        final Registry reg = Registry.getDefault();
        if (reg == null || reg.getServiceManager() == null) {
            throw new RuntimeException("No access to registry. Cannot download document.");
        } else {
            serviceAdmin = reg.getServiceManager();
        }

        Either<String, String> policyXml;
        try {
            policyXml = AdminGuiUtils.doAsyncAdmin(
                    serviceAdmin,
                    XacmlPdpPropertiesDialog.this,
                    MessageFormat.format(resources.getString("urlLoadingDialog.title"), urlstr),
                    resources.getString("urlLoadingDialog.message"),
                    serviceAdmin.resolveUrlTargetAsync(urlstr, XacmlPdpAssertion.XACML_PDP_MAX_DOWNLOAD_SIZE));
        } catch (InterruptedException e) {
            //do nothing the user cancelled
            return;
        } catch (InvocationTargetException e) {
            policyXml = Either.left(ExceptionUtils.getMessage(e));
        }
        if (policyXml.isLeft()) {
            //An error occurred retrieving the document
            JOptionPane.showMessageDialog(this, "No content could be retrieved at URL '" + urlstr + "'. " +
                    "Due to: " + policyXml.left(), "XACML Policy Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final Document doc;
        try {
            doc = XmlUtil.parse(policyXml.right());
        } catch (SAXException e) {
            JOptionPane.showMessageDialog(this,
                    "Cannot parse the XML from URL '" + urlstr+ "' due to error: " + e.getMessage(),
                    "XACML Policy Error",
                    JOptionPane.ERROR_MESSAGE);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return;
        } 

        try {
            docIsXacmlPolicy(doc);

            uiAccessibility.getEditor().setText(policyXml.right());
            uiAccessibility.getEditor().setCaretPosition(0);

            enableDisableComponents();
        } catch (SAXException e) {
            JOptionPane.showMessageDialog(this, "The document at URL is not a valid XACML policy. " + urlstr + ": " + ExceptionUtils.getMessage(e), "XACML Policy Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void docIsXacmlPolicy(Document doc) throws SAXException {
        try {
            Policy.getInstance(doc.getDocumentElement());
        } catch (Exception e) {
            throw new SAXException("Document is not a valid XACML policy: " + ExceptionUtils.getMessage(e), e);
        }
    }

    private void enableDisableComponents() {
        XacmlAssertionEnums.MessageLocation outputEntry =
                (XacmlAssertionEnums.MessageLocation)messageOutputComboBox.getSelectedItem();


        //first check the message variable, and manage setting the context variable text field to be enabled or not
        if(outputEntry == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE){
            messageVariableLabel.setEnabled(true);
            outputMessageVariableNameField.setEnabled(true);

            if(!outputMessageVariableNameField.isEntryValid()){
                okButton.setEnabled(false);
                return;
            }
        }else{
            messageVariableLabel.setEnabled(false);
            outputMessageVariableNameField.setEnabled(false);
        }

        //Check the PDP policy locations if a remote url being used, if not the
        //policy XML is validated on OK
        boolean enableOkButton =
                isProvidedPolicyXML() ||
                Syntax.getReferencedNames( urlToMonitorField.getText(), false ).length > 0 ||
                ValidationUtils.isValidUrl( urlToMonitorField.getText().trim(), false, CollectionUtils.caseInsensitiveSet( "http", "https" ) );

        okButton.setEnabled(enableOkButton);        
    }

    private boolean isProvidedPolicyXML() {
        return !monitorUrlPolicyPanel.isVisible();
    }

    private boolean validProperties() {
        boolean valid = false;

        if ( isProvidedPolicyXML() ) {
            String policyXml = uiAccessibility.getEditor().getText();
            if ( policyXml==null || policyXml.trim().isEmpty() ) {
                JOptionPane.showMessageDialog(this,
                    "A Policy is required when \"Configured in advance\" is selected.\nPlease supply a Policy, or select an alternative \"Policy Location\".",
                    "Missing Policy", JOptionPane.ERROR_MESSAGE, null );
            } else if ( !isXacmlPolicy( policyXml ) ) {
                JOptionPane.showMessageDialog(this,
                    "The Policy is not a valid XACML Policy.\nPlease update the Policy, or select an alternative \"Policy Location\".",
                    "Invalid Policy", JOptionPane.ERROR_MESSAGE, null );
            } else {
                valid = true;
            }
        } else {
            valid = true;
        }

        return valid;
    }

    private boolean isXacmlPolicy( final String text ) {
        boolean isXacmlPolicy = false;

        if ( Syntax.getReferencedNames( text, false ).length > 0 ) {
            isXacmlPolicy = true;
        } else {
            try {
                Document policyDoc = XmlUtil.parse( text );
                isXacmlPolicy =
                        policyDoc.getDocumentElement().getNamespaceURI() != null &&
                        XacmlConstants.XACML_POLICY_NAMESPACES.contains( policyDoc.getDocumentElement().getNamespaceURI() ) &&
                        XacmlConstants.XACML_POLICY_ELEMENT.equals( policyDoc.getDocumentElement().getLocalName() );
            } catch ( SAXException e ) {
                // invalid doc
            }
        }

        return isXacmlPolicy;
    }

    @Override
    public boolean isConfirmed() {
        return confirmed;
    }

    @Override
    public void setData( final XacmlPdpAssertion assertion ) {
        this.assertion = assertion;

        messageSourceComboBox.setModel(buildMessageSourceComboBoxModel(assertion));
        Utilities.equalizeComponentSizes( messageSourceComboBox, messageOutputComboBox );

        boolean foundItem = false;
        for(int i = 0;i < messageSourceComboBox.getItemCount();i++) {
            MessageSourceEntry entry = (MessageSourceEntry)messageSourceComboBox.getItemAt(i);
            if(entry.getMessageSource() != XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
                if(assertion.getInputMessageSource() == entry.getMessageSource()) {
                    messageSourceComboBox.setSelectedItem(entry);
                    foundItem = true;
                    break;
                }
            } else if(entry.getMessageVariableName().equals(assertion.getInputMessageVariableName())) {
                messageSourceComboBox.setSelectedItem(entry);
                foundItem = true;
                break;
            }
        }

        if(!foundItem && assertion.getInputMessageSource() == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
            int position = -1;
            for(int i = 0;i < messageSourceComboBox.getItemCount();i++) {
                MessageSourceEntry entry = (MessageSourceEntry)messageSourceComboBox.getItemAt(i);
                if(entry.getMessageSource() ==
                        XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE
                        && assertion.getInputMessageVariableName().compareTo(entry.getMessageVariableName()) < 0) {
                    position = i;
                    break;
                }
            }

            MessageSourceEntry newEntry =
                    new MessageSourceEntry(
                            XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE,
                            assertion.getInputMessageVariableName());
            if(position == -1) {
                messageSourceComboBox.addItem(newEntry);
            } else {
                messageSourceComboBox.insertItemAt(newEntry, position);
            }
        }

        for(int i = 0;i < messageOutputComboBox.getItemCount();i++) {
            XacmlAssertionEnums.MessageLocation entry =
                    (XacmlAssertionEnums.MessageLocation)messageOutputComboBox.getItemAt(i);
            if(entry == assertion.getOutputMessageTarget()) {
                messageOutputComboBox.setSelectedItem(entry);
                break;
            }
        }

        outputMessageVariableNameField.setAssertion(assertion,getPreviousAssertion());
        if(assertion.getOutputMessageTarget() == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
            outputMessageVariableNameField.setVariable(assertion.getOutputMessageVariableName());
        }

        switch ( assertion.getSoapEncapsulation() ) {
            case NONE:
                break;
            case REQUEST:
                sourceSOAPEncapsulatedCheckBox.setSelected( true );
                break;
            case REQUEST_AND_RESPONSE:
                sourceSOAPEncapsulatedCheckBox.setSelected( true );
                targetSOAPEncapsulatedCheckBox.setSelected( true );
                break;
            case RESPONSE:
                targetSOAPEncapsulatedCheckBox.setSelected( true );
                break;
        }

        if(assertion.getResourceInfo() instanceof StaticResourceInfo) {
            StaticResourceInfo sri = (StaticResourceInfo)assertion.getResourceInfo();
            uiAccessibility.getEditor().setText(sri.getDocument() == null ? "" : sri.getDocument());

            policyLocationComboBox.setSelectedItem(CONFIGURED_IN_ADVANCE);
            monitorUrlPolicyPanel.setVisible(false);
            preconfiguredPolicyPanel.setVisible(true);
        } else if(assertion.getResourceInfo() instanceof SingleUrlResourceInfo) {
            SingleUrlResourceInfo suri = (SingleUrlResourceInfo)assertion.getResourceInfo();
            urlToMonitorField.setText(suri.getUrl());

            policyLocationComboBox.setSelectedItem(MONITOR_URL);
            monitorUrlPolicyPanel.setVisible(true);
            preconfiguredPolicyPanel.setVisible(false);
        }

        failCheckBox.setSelected(assertion.getFailIfNotPermit());
    }

    @Override
    public XacmlPdpAssertion getData( final XacmlPdpAssertion assertion ) {
        MessageSourceEntry messageSourceEntry = (MessageSourceEntry)messageSourceComboBox.getSelectedItem();
        assertion.setInputMessageSource(messageSourceEntry.getMessageSource());
        if(messageSourceEntry.getMessageSource() == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
            assertion.setInputMessageVariableName(messageSourceEntry.getMessageVariableName());
        } else {
            assertion.setInputMessageVariableName(null);
        }

        XacmlAssertionEnums.MessageLocation outputEntry =
                (XacmlAssertionEnums.MessageLocation)messageOutputComboBox.getSelectedItem();
        assertion.setOutputMessageTarget(outputEntry);
        if(outputEntry == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
            assertion.setOutputMessageVariableName( VariablePrefixUtil.fixVariableName(outputMessageVariableNameField.getVariable()) );
        }

        if ( sourceSOAPEncapsulatedCheckBox.isSelected() &&
             targetSOAPEncapsulatedCheckBox.isSelected()) {
            assertion.setSoapEncapsulation(XacmlPdpAssertion.SoapEncapsulationType.REQUEST_AND_RESPONSE);
        } else if ( sourceSOAPEncapsulatedCheckBox.isSelected() ) {
            assertion.setSoapEncapsulation(XacmlPdpAssertion.SoapEncapsulationType.REQUEST);
        } else if ( targetSOAPEncapsulatedCheckBox.isSelected() ) {
            assertion.setSoapEncapsulation(XacmlPdpAssertion.SoapEncapsulationType.RESPONSE);
        } else {
            assertion.setSoapEncapsulation(XacmlPdpAssertion.SoapEncapsulationType.NONE);
        }

        if(CONFIGURED_IN_ADVANCE.equals(policyLocationComboBox.getSelectedItem())) {
            String policyText;
            if ( this.isVisible() ) {
               policyText = uiAccessibility.getEditor().getText();                
            } else {
               policyText = policyXml;
            }
            StaticResourceInfo sri = new StaticResourceInfo(policyText);
            assertion.setResourceInfo(sri);
        } else if(MONITOR_URL.equals(policyLocationComboBox.getSelectedItem())) {
            assertion.setResourceInfo(new SingleUrlResourceInfo(urlToMonitorField.getText().trim()));
        }
        
        assertion.setFailIfNotPermit(failCheckBox.isSelected());
        return assertion;
    }
}
