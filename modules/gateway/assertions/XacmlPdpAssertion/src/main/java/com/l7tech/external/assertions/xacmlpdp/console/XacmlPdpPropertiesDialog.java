package com.l7tech.external.assertions.xacmlpdp.console;

import com.l7tech.console.panels.AssertionPropertiesEditorSupport;
import com.l7tech.console.SsmApplication;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.xacmlpdp.XacmlPdpAssertion;
import com.l7tech.external.assertions.xacmlpdp.XacmlAssertionEnums;
import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.FileChooserUtil;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.widgets.OkCancelDialog;
import com.l7tech.console.panels.UrlPanel;
import com.l7tech.common.io.ByteOrderMarkInputStream;
import com.l7tech.util.IOUtils;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.ResourceUtils;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.SingleUrlResourceInfo;
import com.l7tech.policy.variable.PolicyVariableUtils;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.sun.xacml.Policy;
import com.japisoft.xmlpad.XMLContainer;
import com.japisoft.xmlpad.PopupModel;
import com.japisoft.xmlpad.UIAccessibility;
import com.japisoft.xmlpad.editor.XMLEditor;
import com.japisoft.xmlpad.action.ActionModel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.net.URLConnection;
import java.net.URL;
import java.security.AccessControlException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Copyright (C) 2009, Layer 7 Technologies Inc.
 * User: njordan
 * Date: 13-Mar-2009
 * Time: 5:29:48 PM
 */
public class XacmlPdpPropertiesDialog extends AssertionPropertiesEditorSupport<XacmlPdpAssertion> {
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

        public String toString() {
            if(messageSource != XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE){
                return messageSource.getLocationName();
            }else{
                return "Context Variable: " +
                        Syntax.SYNTAX_PREFIX + messageSource.getLocationName() + Syntax.SYNTAX_SUFFIX;    
            }
        }
    }

    private static final Logger log = Logger.getLogger(XacmlPdpPropertiesDialog.class.getName());

    private static final String CONFIGURED_IN_ADVANCE = "Configured in advance";
    private static final String MONITOR_URL = "Monitor URL for latest value";

    private JComboBox messageSourceComboBox;
    private JComboBox messageOutputComboBox;
    private JTextField outputMessageVariableNameField;
    private JPanel mainPanel;
    private JPanel policyPanel;
    private JButton fetchFileButton;
    private JButton fetchUrlButton;
    private JCheckBox failCheckBox;
    private JButton okButton;
    private JButton cancelButton;
    private JComboBox messageFormatComboBox;
    private JTextField urlToMonitorField;
    private JPanel monitorUrlPolicyPanel;
    private JPanel preconfiguredPolicyPanel;
    private JComboBox policyLocationComboBox;
    private JPanel xacmlPolicyPanel;

    private UIAccessibility uiAccessibility;

    private XacmlPdpAssertion assertion;
    private boolean confirmed;

    public XacmlPdpPropertiesDialog(Frame owner, XacmlPdpAssertion a) {
        super(owner, "XACML PDP Properties");
        assertion = a;
        initComponents();
        enableDisableComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
        comboBoxModel.addElement(
                new MessageSourceEntry(
                        XacmlAssertionEnums.MessageLocation.DEFAULT_REQUEST, null));
        comboBoxModel.addElement(
                new MessageSourceEntry(
                        XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE, null));

        Map<String, VariableMetadata> predecessorVariables = PolicyVariableUtils.getVariablesSetByPredecessors(assertion);
        SortedSet<String> predecessorVariableNames = new TreeSet<String>(predecessorVariables.keySet());
        for (String variableName: predecessorVariableNames) {
            if (predecessorVariables.get(variableName).getType() == DataType.MESSAGE) {
                final MessageSourceEntry item =
                        new MessageSourceEntry(
                                XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE,
                                variableName);
                comboBoxModel.addElement(item);
            }
        }
        messageSourceComboBox.setModel(comboBoxModel);

        comboBoxModel = new DefaultComboBoxModel();
        comboBoxModel.addElement(XacmlAssertionEnums.MessageLocation.DEFAULT_RESPONSE);
        comboBoxModel.addElement(XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE);
        messageOutputComboBox.setModel(comboBoxModel);

        outputMessageVariableNameField.setEnabled(false);

        messageFormatComboBox.setModel(
                new DefaultComboBoxModel(XacmlPdpAssertion.SoapEncapsulationType.allTypesAsStrings()));

        policyLocationComboBox.setModel(new DefaultComboBoxModel(new String[] {CONFIGURED_IN_ADVANCE, "Monitor URL for latest value"}));
        monitorUrlPolicyPanel.setVisible(false);
        ((TitledBorder)xacmlPolicyPanel.getBorder()).setTitle("XACML Policy - " + CONFIGURED_IN_ADVANCE);

        policyLocationComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                if(CONFIGURED_IN_ADVANCE.equals(policyLocationComboBox.getSelectedItem())) {
                    monitorUrlPolicyPanel.setVisible(false);
                    preconfiguredPolicyPanel.setVisible(true);
                    ((TitledBorder)xacmlPolicyPanel.getBorder()).setTitle("XACML Policy - " + CONFIGURED_IN_ADVANCE);
                } else if(MONITOR_URL.equals(policyLocationComboBox.getSelectedItem())) {
                    monitorUrlPolicyPanel.setVisible(true);
                    preconfiguredPolicyPanel.setVisible(false);
                    ((TitledBorder)xacmlPolicyPanel.getBorder()).setTitle("XACML Policy - " + MONITOR_URL);
                }
            }
        });

        XMLContainer xmlContainer = new XMLContainer(true);
        final JComponent xmlEditor = xmlContainer.getView();

        uiAccessibility = xmlContainer.getUIAccessibility();
        uiAccessibility.setTreeAvailable(false);
        uiAccessibility.setToolBarAvailable(false);
        xmlContainer.setStatusBarAvailable(false);
        PopupModel popupModel = xmlContainer.getPopupModel();
        // remove the unwanted actions
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.LOAD_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVE_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.SAVEAS_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.NEW_ACTION));
        popupModel.removeAction(ActionModel.getActionByName(ActionModel.PARSE_ACTION));

        if (TopComponents.getInstance().isApplet()) {
            // Search action tries to get the class loader
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.INSERT_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.SEARCH_ACTION));
            popupModel.removeAction(ActionModel.getActionByName(ActionModel.COMMENT_ACTION));
        }

        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_SELECTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_COMMENTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_COPYNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_CUTNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_EDITNODE_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_CLEANHISTORY_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_ADDHISTORY_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_PREVIOUS_ACTION));
        xmlContainer.getTreePopupModel().removeAction(ActionModel.getActionByName(ActionModel.TREE_NEXT_ACTION));

        boolean lastWasSeparator = true; // remove trailing separator
        for (int i=popupModel.size()-1; i>=0; i--) {
            boolean isSeparator = popupModel.isSeparator(i);
            if (isSeparator && (i==0 || lastWasSeparator)) {
                popupModel.removeSeparator(i);
            } else {
                lastWasSeparator = isSeparator;
            }
        }

        Utilities.equalizeButtonSizes(new JButton[] {okButton, cancelButton});

        fetchFileButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                readFromFile();
            }
        });

        fetchUrlButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final OkCancelDialog dlg = new OkCancelDialog(XacmlPdpPropertiesDialog.this, "Enter the URL to load the XACML policy from:", true, new UrlPanel("XACML Policy URL:", null));
                dlg.pack();
                Utilities.centerOnScreen(dlg);
                DialogDisplayer.display(dlg, new Runnable() {
                    public void run() {
                        String url = (String)dlg.getValue();
                        if (url != null) {
                            readFromUrl(url);
                        }
                    }
                });
            }
        });

        DocumentListener documentListener = new DocumentListener() {
            public void changedUpdate(DocumentEvent evt) {
                enableDisableComponents();
            }

            public void insertUpdate(DocumentEvent evt) {
                enableDisableComponents();
            }

            public void removeUpdate(DocumentEvent evt) {
                enableDisableComponents();
            }
        };
        uiAccessibility.getEditor().getDocument().addDocumentListener(documentListener);
        outputMessageVariableNameField.getDocument().addDocumentListener(documentListener);
        urlToMonitorField.getDocument().addDocumentListener(documentListener);

        messageOutputComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                enableDisableComponents();
            }
        });

        failCheckBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                enableDisableComponents();
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                getData(assertion);
                confirmed = true;
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                dispose();
            }
        });

        // Initialize the XML editor to the XACML policy from the assertion, if any, else to an identity transform
        SwingUtilities.invokeLater(new Runnable() {
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

        policyPanel.setLayout(new BorderLayout());
        policyPanel.add(xmlEditor, BorderLayout.CENTER);

        setContentPane(mainPanel);

        //pack();
    }

    private void readFromFile() {
        SsmApplication.doWithJFileChooser(new FileChooserUtil.FileChooserUser() {
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
        byte[] bytes;
        String encoding;
        ByteOrderMarkInputStream bomis = null;
        InputStream httpStream = null;
        try {
            URLConnection conn = new URL(urlstr).openConnection();
            httpStream = conn.getInputStream();
            bytes = IOUtils.slurpStream(httpStream);
            bomis = new ByteOrderMarkInputStream(new ByteArrayInputStream(bytes));
        } catch (AccessControlException ace) {
            TopComponents.getInstance().showNoPrivilegesErrorMessage();
            return;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "No content could be retrieved at that URL. " + urlstr, "XACML Policy Error", JOptionPane.ERROR_MESSAGE);
            return;
        } finally {
            ResourceUtils.closeQuietly(httpStream);
            ResourceUtils.closeQuietly(bomis);
        }

        Document doc;
        try {
            doc = XmlUtil.parse(new ByteArrayInputStream(bytes));

        } catch (SAXException e) {
            JOptionPane.showMessageDialog(this,
                    "Cannot parse the XML from URL '" + urlstr+ "' due to error: " + e.getMessage(),
                    "XACML Policy Error",
                    JOptionPane.ERROR_MESSAGE);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return;
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "Cannot parse the XML from URL '" + urlstr+"'",
                    "XACML Policy Error",
                    JOptionPane.ERROR_MESSAGE);
            log.log(Level.FINE, "cannot parse " + urlstr, e);
            return;
        }

        try {
            docIsXacmlPolicy(doc);

            uiAccessibility.getEditor().setText(new String(bytes));
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
            outputMessageVariableNameField.setEnabled(true);

            if(outputMessageVariableNameField.getText().trim().length() == 0){
                okButton.setEnabled(false);
                return;
            }
        }else{
            outputMessageVariableNameField.setEnabled(false);
        }

        //Check the PDP policy locations
        boolean enableOkButton;
        //is a remote url being used?
        if(monitorUrlPolicyPanel.isVisible()){
            //we need the url value
            enableOkButton = urlToMonitorField.getText().trim().length() != 0;
        }else{
            //otherwise we need the policy in the xml editor
            enableOkButton = uiAccessibility.getEditor().getText().trim().length() != 0;            
        }
        okButton.setEnabled(enableOkButton);
        
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public void setData(XacmlPdpAssertion assertion) {
        this.assertion = assertion;

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
        if(assertion.getOutputMessageTarget() == XacmlAssertionEnums.MessageLocation.CONTEXT_VARIABLE) {
            outputMessageVariableNameField.setText(assertion.getOutputMessageVariableName());
        }

        messageFormatComboBox.setSelectedItem(assertion.getSoapEncapsulation().toString());

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

    public XacmlPdpAssertion getData(XacmlPdpAssertion assertion) {
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
            assertion.setOutputMessageVariableName(outputMessageVariableNameField.getText().trim());
        }

        String type = (String) messageFormatComboBox.getSelectedItem();
        assertion.setSoapEncapsulation(XacmlPdpAssertion.SoapEncapsulationType.findEncapsulationType(type));

        if(CONFIGURED_IN_ADVANCE.equals(policyLocationComboBox.getSelectedItem())) {
            String policyText = uiAccessibility.getEditor().getText();
            StaticResourceInfo sri = new StaticResourceInfo(policyText);
            assertion.setResourceInfo(sri);
        } else if(MONITOR_URL.equals(policyLocationComboBox.getSelectedItem())) {
            assertion.setResourceInfo(new SingleUrlResourceInfo(urlToMonitorField.getText().trim()));
        }
        
        assertion.setFailIfNotPermit(failCheckBox.isSelected());
        return assertion;
    }
}
