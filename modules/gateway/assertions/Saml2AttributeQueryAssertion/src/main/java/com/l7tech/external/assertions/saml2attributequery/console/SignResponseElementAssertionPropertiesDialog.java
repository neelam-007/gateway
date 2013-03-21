package com.l7tech.external.assertions.saml2attributequery.console;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.action.Actions;
import com.l7tech.console.panels.NamespaceMapEditor;
import com.l7tech.console.panels.SampleMessageDialog;
import com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.wsdl.BindingOperationTreeNode;
import com.l7tech.console.tree.wsdl.BindingTreeNode;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.XmlViewer;
import com.l7tech.external.assertions.saml2attributequery.SignResponseElementAssertion;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.service.SampleMessage;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.SpeedIndicator;
import com.l7tech.gui.widgets.SquigglyField;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.InvalidDocumentFormatException;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.soap.SoapMessageGenerator;
import com.l7tech.xml.soap.SoapMessageGenerator.Message;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.tarari.util.TarariXpathConverter;
import com.l7tech.xml.xpath.*;
import org.dom4j.DocumentException;
import org.jaxen.XPathSyntaxException;
import org.jaxen.saxpath.SAXPathException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.*;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class SignResponseElementAssertionPropertiesDialog extends JDialog {
    static final Logger log = Logger.getLogger(SignResponseElementAssertionPropertiesDialog.class.getName());
    private JPanel mainPanel;
    private JPanel messageViewerPanel;
    private JPanel messageViewerToolbarPanel;
    private JTree operationsTree;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JLabel descriptionLabel;
    private JPanel signingCertificatePanel;
    private JTextField signingCertificateDnVariableField;
    private AssertionTreeNode node;
    private SignResponseElementAssertion assertion;
    private EntityWithPolicyNode policyNode;
    private Wsdl serviceWsdl;
    private Message[] soapMessages;
    private String blankMessage = "<empty />";
    private Map<String, String> namespaces = new HashMap<String, String>();
    private Map<String, String> requiredNamespaces = new HashMap<String, String>();
    private XmlViewer messageViewer;
    private XpathBasedAssertionPropertiesDialog.XpathToolBar messageViewerToolBar;
    private ActionListener okActionListener;
    private boolean haveTarari;
    private org.w3c.dom.Document testEvaluator;
    private JButton namespaceButton;
    private JLabel hardwareAccelStatusLabel;
    private JPanel speedIndicatorPanel;
    private JPanel signatureResponseConfigPanel;
    private JRadioButton embeddedCertificateRadioButton;
    private JRadioButton skiReferenceRadioButton;
    private SpeedIndicator speedIndicator;
    private JComboBox sampleMessagesCombo;
    private DefaultComboBoxModel sampleMessagesComboModel;
    private JButton addSampleButton;
    private JButton removeSampleButton;
    private static final SampleMessageComboEntry USE_AUTOGEN = new SampleMessageComboEntry(null) {
        @Override
        public String toString() {
            return "<use automatically generated message above>";
        }
    };
    private BindingOperation currentOperation;
    private JButton editSampleButton;
    private JComboBox messageSourceComboBox;
    private JTextField messageSourceContextVarField;
    private final boolean showHardwareAccelStatus;

    private static final String NON_SOAP_NAME = "<Non-SOAP service>";
    private static final WsdlTreeNode NON_SOAP_NODE = new WsdlTreeNode(NON_SOAP_NAME, new WsdlTreeNode.Options()) {
        @Override
        protected String iconResource(boolean open) {
            return "com/l7tech/console/resources/methodPublic.gif";
        }

        @Override
        public boolean getAllowsChildren() {
            return false;
        }

        @Override
        public String toString() {
            return NON_SOAP_NAME;
        }
    };
    private static final String GENERIC_SOAP_NAME = "<Generic SOAP service>";
    private static final WsdlTreeNode GENERIC_SOAP_NODE = new WsdlTreeNode(GENERIC_SOAP_NAME, new WsdlTreeNode.Options()) {
        @Override
        protected String iconResource(boolean open) {
            return "com/l7tech/console/resources/methodPublic.gif";
        }

        @Override
        public String toString() {
            return GENERIC_SOAP_NAME;
        }
    };

    private static final String DEFAULT_SOAP_MESSAGE = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org" +
            "/soap/envelope/\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/" +
            "XMLSchema-instance\"><soapenv:Header></soapenv:Header><soapenv:Body></soapenv:Body></soapenv:Envelope>";

    private static class PrivateKeyEntry {
        public String alias;
        public long keyStoreId;
        public String subjectDN;

        public PrivateKeyEntry(String alias, long keyStoreId, String subjectDN) {
            this.alias = alias;
            this.keyStoreId = keyStoreId;
            this.subjectDN = subjectDN;
        }

        public String toString() {
            return subjectDN;
        }
    }

    private static class MessageSourceEntry {
        public int messageSource;
        public String displayName;

        public MessageSourceEntry(int messageSource, String displayName) {
            this.messageSource = messageSource;
            this.displayName = displayName;
        }

        public String toString() {
            return displayName;
        }
    }

    /**
     * @param owner this panel owner
     * @param modal is this modal dialog or not
     * @param n     the xml security node
     */
    public SignResponseElementAssertionPropertiesDialog(final Frame owner,
                                               final boolean modal,
                                               final AssertionTreeNode n,
                                               final SignResponseElementAssertion assertion,
                                               final ActionListener okListener,
                                               final boolean showHardwareAccelStatus,
                                               final boolean readOnly) {
        super(owner, modal);
        if (n == null) {
            throw new IllegalArgumentException();
        }
        this.showHardwareAccelStatus = showHardwareAccelStatus;
        construct(n, assertion, okListener, readOnly);
    }


    private void construct(final AssertionTreeNode n, final SignResponseElementAssertion assertion, final ActionListener okListener, final boolean readOnly) {
        node = n;
        okActionListener = okListener;

        this.assertion = assertion;
        if (assertion.getXpathExpression() != null) {
            //noinspection unchecked
            namespaces = assertion.getXpathExpression().getNamespaces();
        } else {
            namespaces = new HashMap<String,String>();
        }
        policyNode = AssertionTreeNode.getServiceNode(node);
        if (policyNode == null) {
            policyNode = AssertionTreeNode.getPolicyNode(node);
            if (policyNode == null) {
                throw new IllegalStateException("Unable to determine the PolicyEntityNode for " + assertion);
            }
        }

        signingCertificatePanel.setVisible(true);

        if(assertion.getPrivateKeyDnVariable() != null) {
            this.signingCertificateDnVariableField.setText(assertion.getPrivateKeyDnVariable());
        }

        DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel(new Object[] {
                new MessageSourceEntry(SignResponseElementAssertion.REQUEST_MESSAGE, "Default Request"),
                new MessageSourceEntry(SignResponseElementAssertion.RESPONSE_MESSAGE, "Default Response"),
                new MessageSourceEntry(SignResponseElementAssertion.MESSAGE_VARIABLE, "Context Variable")
        });
        messageSourceComboBox.setModel(comboBoxModel);
        messageSourceComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                MessageSourceEntry entry = (MessageSourceEntry)messageSourceComboBox.getSelectedItem();
                messageSourceContextVarField.setEnabled(entry.messageSource == SignResponseElementAssertion.MESSAGE_VARIABLE);
            }
        });

        if(assertion.getInputMessageSource() == SignResponseElementAssertion.REQUEST_MESSAGE) {
            messageSourceComboBox.setSelectedIndex(0);
            messageSourceContextVarField.setEnabled(false);
        } else if(assertion.getInputMessageSource() == SignResponseElementAssertion.RESPONSE_MESSAGE) {
            messageSourceComboBox.setSelectedIndex(1);
            messageSourceContextVarField.setEnabled(false);
        } else if(assertion.getInputMessageSource() == SignResponseElementAssertion.MESSAGE_VARIABLE) {
            messageSourceComboBox.setSelectedIndex(2);
            messageSourceContextVarField.setEnabled(true);
            messageSourceContextVarField.setText((assertion.getInputMessageVariableName() == null) ? "" : assertion.getInputMessageVariableName());
        }

        signatureResponseConfigPanel.setVisible(true);
        if (policyNode instanceof ServiceNode) {
            try {
                serviceWsdl = ((ServiceNode)policyNode).getEntity().parsedWsdl();
                if (serviceWsdl != null) {
                    serviceWsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
                    SoapMessageGenerator sg = new SoapMessageGenerator();
                    if (isEditingRequest()) {
                        soapMessages = sg.generateRequests(serviceWsdl);
                    } else {
                        soapMessages = sg.generateResponses(serviceWsdl);
                    }
                    if (soapMessages.length > 0)
                        initializeBlankMessage(soapMessages[0]);
                    for (Message soapRequest : soapMessages) {
                        //noinspection unchecked
                        requiredNamespaces.putAll(XpathUtil.getNamespaces(soapRequest.getSOAPMessage()));
                    }
                    requiredNamespaces.put("L7p",WspConstants.L7_POLICY_NS);
                    requiredNamespaces.put("wsp",WspConstants.WSP_POLICY_NS);
                }
            } catch (Exception e) {
                throw new RuntimeException("Unable to parse the service WSDL " + policyNode.getName(), e);
            }
        } else {
            try {
                // policyNode is gurranteed not to be null, since it's been set up in the method construct(...).
                Policy policy = policyNode.getPolicy();
                if (policy.isSoap()) {
                    blankMessage = DEFAULT_SOAP_MESSAGE;
                }
            } catch (FindException e) {
                throw new RuntimeException("Couldn't find the policy", e);
            }
        }

        if (namespaces.size() < requiredNamespaces.size()) {
            namespaces.putAll(requiredNamespaces);
        }

        initialize(readOnly);

        // display the existing xpath expression
        String initialvalue = null;
        if (assertion.getXpathExpression() != null) {
            initialvalue = assertion.getXpathExpression().getExpression();
        }
        messageViewerToolBar.getxpathField().setText(initialvalue);

        populateSampleMessages(null, 0);
        enableSampleButtons();

        addSampleButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final SampleMessage sm;
                try {
                    String xml = "";
                    try {
                        org.w3c.dom.Document doc = messageViewer.getDocument();
                        xml = XmlUtil.nodeToFormattedString(doc);
                    } catch (Exception e1) {
                        log.log(Level.WARNING, "Invalid XML", e1);
                    }
                    String name = currentOperation == null ? null : currentOperation.getName();
                    long objectId = (policyNode instanceof ServiceNode)? policyNode.getEntityOid() : -1;
                    sm = new SampleMessage(objectId, name, name, xml);
                } catch (Exception ex) {
                    throw new RuntimeException("Couldn't find PublishedService", ex);
                }

                showSampleMessageDialog(sm, new Functions.UnaryVoid<SampleMessageDialog>() {
                    public void call(SampleMessageDialog smd) {
                        if (smd.isOk()) {
                            try {
                                long oid = Registry.getDefault().getServiceManager().saveSampleMessage(sm);
                                populateSampleMessages(currentOperation == null ? null : currentOperation.getName(), oid);
                            } catch (SaveException ex) {
                                throw new RuntimeException("Couldn't save SampleMessage", ex);
                            }
                        }
                    }
                });
            }
        });

        editSampleButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final SampleMessageComboEntry entry = (SampleMessageComboEntry)sampleMessagesCombo.getSelectedItem();
                if (entry == USE_AUTOGEN) return;
                showSampleMessageDialog(entry.message, new Functions.UnaryVoid<SampleMessageDialog>() {
                    public void call(SampleMessageDialog smd) {
                        if (smd.isOk()) {
                            try {
                                Registry.getDefault().getServiceManager().saveSampleMessage(entry.message);
                            } catch (SaveException ex) {
                                throw new RuntimeException("Couldn't save SampleMessage", ex);
                            }
                            sampleMessagesCombo.repaint();
                            displayMessage(smd.getMessage().getXml());
                        }
                    }
                });
            }
        });

        removeSampleButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SampleMessageComboEntry entry = (SampleMessageComboEntry)sampleMessagesCombo.getSelectedItem();
                if (entry == USE_AUTOGEN) return;
                int resp = JOptionPane.showConfirmDialog(SignResponseElementAssertionPropertiesDialog.this,
                        "Are you sure you want to remove '" + entry.toString() + "'?",
                        "Remove Sample Message",
                        JOptionPane.OK_CANCEL_OPTION);

                if (resp == JOptionPane.OK_OPTION) {
                    try {
                        Registry.getDefault().getServiceManager().deleteSampleMessage(entry.message);
                        sampleMessagesCombo.removeItem(entry);
                    } catch (DeleteException e1) {
                        throw new RuntimeException("Couldn't delete SampleMessage", e1);
                    }
                }
            }
        });

        sampleMessagesCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SampleMessageComboEntry entry = (SampleMessageComboEntry)sampleMessagesCombo.getSelectedItem();
                try {
                    if (entry == USE_AUTOGEN) {
                        if (currentOperation == null) {
                            displayMessage(blankMessage);
                        } else {
                            displayMessage(forOperation(currentOperation).getSOAPMessage());
                        }
                    } else {
                        displayMessage(entry.message.getXml());
                    }
                } catch (Exception e1) {
                    throw new RuntimeException("Can't use sample message XML", e1);
                }
            }
        });

        haveTarari = checkForTarari();
        speedIndicator = new SpeedIndicator(0);
        speedIndicatorPanel.setLayout(new BorderLayout());
        speedIndicatorPanel.add(speedIndicator, BorderLayout.CENTER);
    }

    private boolean checkForTarari() {
        try {
            final ClusterStatusAdmin clusterStatusAdmin = Registry.getDefault().getClusterStatusAdmin();
            String accel = clusterStatusAdmin.getHardwareCapability(ClusterStatusAdmin.CAPABILITY_HWXPATH);
            return ClusterStatusAdmin.CAPABILITY_VALUE_HWXPATH_TARARI.equals(accel);
        } catch (Exception e) {
            // Oh well, it's cosmetic anyway
            return false;
        }
    }

    private void showSampleMessageDialog(SampleMessage sm, final Functions.UnaryVoid<SampleMessageDialog> result) {
        final SampleMessageDialog smd = new SampleMessageDialog(this, sm, false, namespaces);
        smd.pack();
        Utilities.centerOnScreen(smd);
        DialogDisplayer.display(smd, new Runnable() {
            public void run() {
                result.call(smd);
            }
        });
    }

    private void populateSampleMessages(String operationName, long whichToSelect) {
        EntityHeader[] sampleMessages;
        ArrayList<SampleMessageComboEntry> messageEntries = new ArrayList<SampleMessageComboEntry>();
        messageEntries.add(USE_AUTOGEN);
        SampleMessageComboEntry whichEntryToSelect = null;
        try {
            ServiceAdmin serviceManager = Registry.getDefault().getServiceManager();
            long objectId = (policyNode instanceof ServiceNode)? policyNode.getEntityOid() : -1;
            sampleMessages = serviceManager.findSampleMessageHeaders(objectId, operationName);
            for (EntityHeader sampleMessage : sampleMessages) {
                long thisOid = sampleMessage.getOid();
                SampleMessageComboEntry entry = new SampleMessageComboEntry(serviceManager.findSampleMessageById(thisOid));
                if (thisOid == whichToSelect) whichEntryToSelect = entry;
                messageEntries.add(entry);
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get sample messages", e);
        }

        sampleMessagesComboModel = new DefaultComboBoxModel(messageEntries.toArray(new SampleMessageComboEntry[messageEntries.size()]));
        sampleMessagesCombo.setModel(sampleMessagesComboModel);

        if (whichEntryToSelect != null)
            sampleMessagesCombo.setSelectedItem(whichEntryToSelect);

        sampleMessagesCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableSampleButtons();
            }
        });

        enableSampleButtons();
    }

    private void enableSampleButtons() {
        boolean gotOne = sampleMessagesComboModel.getSelectedItem() != USE_AUTOGEN;
        removeSampleButton.setEnabled(gotOne);
        editSampleButton.setEnabled(gotOne);
    }

    private static class SampleMessageComboEntry {
        public SampleMessageComboEntry(SampleMessage message) {
            this.message = message;
            if (message == null) return;
            try {
                org.w3c.dom.Document doc = XmlUtil.stringToDocument(message.getXml());
                XmlUtil.findAllNamespaces(doc.getDocumentElement());
            } catch (Exception e) {
                log.log(Level.WARNING, "Sample message is not XML", e);
            }
        }

        @Override
        public String toString() {
            return message.getName();
        }

        private final SampleMessage message;
    }


    private void initialize(final boolean readOnly) {
        Utilities.setEscKeyStrokeDisposes(this);

        initializeResponseSignatureConfig();
        setModal(true);
        namespaceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final NamespaceMapEditor nseditor = new NamespaceMapEditor(SignResponseElementAssertionPropertiesDialog.this,
                        namespaces,
                        requiredNamespaces, null);
                nseditor.pack();
                Utilities.centerOnScreen(nseditor);
                DialogDisplayer.display(nseditor, new Runnable() {
                    public void run() {
                        Map newMap = nseditor.newNSMap();
                        if (newMap != null) {
                            namespaces = newMap;

                            // update feedback for new namespaces
                            JTextField xpathTextField = messageViewerToolBar.getxpathField();
                            xpathFieldPauseListener.textEntryPaused(xpathTextField, 0);
                        }
                    }
                });
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SignResponseElementAssertionPropertiesDialog.this.dispose();
            }
        });

        okButton.setEnabled( !readOnly );
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // get xpath from control and the namespace map
                // then save it in assertion
                JTextField xpathTextField = messageViewerToolBar.getxpathField();
                String xpath = xpathTextField.getText();
                XpathFeedBack res = getFeedBackMessage(namespaces, xpathTextField);
                if (res != null && !res.valid()) {
                    String xpathmsg = xpath;
                    if (xpathmsg == null || xpathmsg.equals("")) {
                        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null,
                                "The empty XPath is invalid. Please specify it.", null);
                        return;
                    }
                    int rs2 = JOptionPane.showConfirmDialog(okButton, "The path " + xpathmsg + " is not valid (" +
                      res.getShortMessage() + ").\nAre you sure " +
                      "you want to save?");
                    if (rs2 != JOptionPane.YES_OPTION) {
                        return;
                    }
                }

                if(signingCertificateDnVariableField.getText().trim().length() == 0 ||
                        !VariableMetadata.isNameValid(signingCertificateDnVariableField.getText()))
                {
                    DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null,
                            "The signing certificate DN variable name is invalid. Please specify it.", null);
                    return;
                }

                MessageSourceEntry entry = (MessageSourceEntry)messageSourceComboBox.getSelectedItem();
                if(entry.messageSource == SignResponseElementAssertion.MESSAGE_VARIABLE) {
                    if(messageSourceContextVarField.getText().trim().length() == 0) {
                        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null,
                                "The message source context variable was invalid. Please specify it.", null);
                        return;
                    }
                    assertion.setInputMessageVariableName(messageSourceContextVarField.getText());
                }
                assertion.setInputMessageSource(entry.messageSource);

                if (xpath == null || "".equals(xpath.trim())) {
                    assertion.setXpathExpression(null);
                } else {
                    assertion.setXpathExpression(new XpathExpression(xpath, namespaces));
                }

                assertion.setPrivateKeyDnVariable(signingCertificateDnVariableField.getText());

                collectResponseSignatureConfig();
                SignResponseElementAssertionPropertiesDialog.this.dispose();
                if (okActionListener != null) okActionListener.actionPerformed(e);
            }
        });

        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(SignResponseElementAssertionPropertiesDialog.this);
            }
        });
        try {
            final MutableTreeNode root = new DefaultMutableTreeNode();
            final DefaultTreeModel operationsTreeModel = new DefaultTreeModel(root);

            if (policyNode instanceof ServiceNode) {
                final Wsdl wsdl = ((ServiceNode)policyNode).getEntity().parsedWsdl();
                if (wsdl != null) {
                    populateOperations(wsdl, operationsTreeModel, root);
                } else {
                    root.insert(NON_SOAP_NODE, 0);
                }
            } else {
                try {
                    // policyNode is gurranteed not to be null, since it's been set up in the method construct(...).
                    Policy policy = policyNode.getPolicy();
                    if (policy.isSoap()) {
                        root.insert(GENERIC_SOAP_NODE, 0);
                    } else {
                        root.insert(NON_SOAP_NODE, 0);
                    }
                } catch (FindException e) {
                    throw new RuntimeException("Couldn't find the policy", e);
                }
            }

            operationsTree.setModel(operationsTreeModel);
            operationsTree.setRootVisible(false);
            operationsTree.setShowsRootHandles(true);
            operationsTree.setCellRenderer(wsdlTreeRenderer);
            operationsTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            operationsTree.getSelectionModel().addTreeSelectionListener(operationsSelectionListener);

            initializeSoapMessageViewer(blankMessage);

            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(mainPanel);
        } catch (WSDLException e) {
            throw new RuntimeException(e);
        } catch (FindException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXParseException e) {
            throw new RuntimeException(e);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }

        // initialize the test evaluator
        testEvaluator = XmlUtil.stringAsDocument("<blah xmlns=\"http://bzzt.com\"/>");
        TextComponentPauseListenerManager.registerPauseListener(messageViewerToolBar.getxpathField(), xpathFieldPauseListener, 700);

        String description = "Select a response element to sign:";
        String title = "Sign Response Element Properties";

        descriptionLabel.setText(description);
        setTitle(title);
    }

    private void initializeResponseSignatureConfig() {
        ButtonGroup bg = new ButtonGroup();
        bg.add(embeddedCertificateRadioButton);
        bg.add(skiReferenceRadioButton);
        if (KeyReference.BST.getName().equals(assertion.getKeyReference())) {
            embeddedCertificateRadioButton.setSelected(true);
        } else {
            skiReferenceRadioButton.setSelected(true);
        }
    }

    private void collectResponseSignatureConfig() {
        if (embeddedCertificateRadioButton.isSelected()) {
            assertion.setKeyReference(KeyReference.BST.getName());
        } else if (skiReferenceRadioButton.isSelected()) {
            assertion.setKeyReference(KeyReference.SKI.getName());
        }
    }

    /**
     * initialize the blank message tha is displayed on whole message
     * selection. The blank message is created from the first message,
     * without body nodes.
     */
    private void initializeBlankMessage(Message soapMessage) throws IOException, SOAPException, SAXException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        soapMessage.getSOAPMessage().writeTo(bo);
        String s = bo.toString();
        org.w3c.dom.Document document = XmlUtil.stringToDocument(s);
        Element body;
        try {
            body = SoapUtil.getBodyElement(document);
        } catch (InvalidDocumentFormatException e) {
            log.warning("Unable to create the blank message from " + s + ": " + e.getMessage());
            return;
        }
        if (body == null) {
            log.warning("Unable to create the blank message from " + s);
            return;
        }
        NodeList nl = body.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            Node item = nl.item(i);
            if (item == null) continue;
            if (item.getNodeType() == Element.ELEMENT_NODE) {
                item.getParentNode().removeChild(item);
            }
        }
        blankMessage = XmlUtil.nodeToString(document);
    }

    /**
     * Populate/initialize the operations tree
     *
     * @param wsdl      the wsdl
     * @param treeModel the treemodel to populate
     * @param root      the tree root
     */
    private void populateOperations(final Wsdl wsdl, final DefaultTreeModel treeModel, final MutableTreeNode root) {
        Collection collection = wsdl.getBindings();
        boolean showBindings = wsdl.getBindings().size() > 1;
        WsdlTreeNode.Options wo = new WsdlTreeNode.Options();

        MutableTreeNode parentNode = root;
        int bindingsCounter = 0;
        for (Object o : collection) {
            Binding b = (Binding) o;
            if (showBindings) {
                final BindingTreeNode bindingTreeNode = new BindingTreeNode(b, wo);
                treeModel.insertNodeInto(bindingTreeNode, root, bindingsCounter++);
                parentNode = bindingTreeNode;
            }

            List operations = b.getBindingOperations();
            int index = 0;
            for (Object operation : operations) {
                BindingOperation bo = (BindingOperation) operation;
                treeModel.insertNodeInto(new BindingOperationTreeNode(bo, wo), parentNode, index++);
            }
        }
    }

    private void initializeSoapMessageViewer(String msg)
        throws IOException, SAXParseException, DocumentException {
        messageViewer = new XmlViewer();
        setMessageViewerText(msg);
        messageViewerToolBar = new XpathBasedAssertionPropertiesDialog.XpathToolBar(messageViewer, new Functions.Nullary<Map<String, String>>() {
            @Override
            public Map<String, String> call() {
                return namespaces;
            }
        });
        messageViewerToolbarPanel.setLayout(new BorderLayout());
        messageViewerToolbarPanel.add(messageViewerToolBar, BorderLayout.CENTER);
        com.intellij.uiDesigner.core.GridConstraints gridConstraints2 = new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 7, 7, null, null, null);
        messageViewerPanel.add(messageViewer, gridConstraints2);
    }

    private void setMessageViewerText(String xml) {
        try {
            messageViewer.setDocument(XmlUtil.stringToDocument(xml));
        } catch (SAXException e) {
            log.warning("unable to parse message XML: " + ExceptionUtils.getMessage(e));
        }
    }

    /**
     * @return whether we are editing the request
     */
    private boolean isEditingRequest() {
        return assertion.getInputMessageSource() == SignResponseElementAssertion.REQUEST_MESSAGE;
    }

    private final
    TreeCellRenderer wsdlTreeRenderer = new DefaultTreeCellRenderer() {
        /**
         * Sets the value of the current tree cell to <code>value</code>.
         *
         * @return the <code>Component</code> that the renderer uses to draw the value
         */
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value,
                                                      boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            this.setBackgroundNonSelectionColor(tree.getBackground());
            if (!(value instanceof WsdlTreeNode)) return this;

            WsdlTreeNode node = (WsdlTreeNode)value;
            setText(node.toString());
            Image image = expanded ? node.getOpenedIcon() : node.getIcon();
            Icon icon = null;
            Icon disabledIcon = null;
            if (image != null) {
                ImageIcon ii = new ImageIcon(image);
                icon = ii;
                disabledIcon = new ImageIcon(GrayFilter.createDisabledImage(ii.getImage()));
            }
            setIcon(icon);
            setDisabledIcon(disabledIcon);

            return this;
        }
    };

    private final TreeSelectionListener
      operationsSelectionListener = new TreeSelectionListener() {
        public void valueChanged(TreeSelectionEvent e) {
            TreePath path = e.getNewLeadSelectionPath();
            if (path == null) {
                populateSampleMessages(null, 0);
                displayMessage(blankMessage);
                currentOperation = null;
            } else {
                final Object lpc = path.getLastPathComponent();
                if (!((lpc instanceof BindingOperationTreeNode) || (lpc instanceof BindingTreeNode) || (lpc == NON_SOAP_NODE) || (lpc == GENERIC_SOAP_NODE))) {
                    messageViewerToolBar.setToolbarEnabled(false);
                    return;
                }
                if (lpc instanceof BindingTreeNode) {
                    messageViewerToolBar.setToolbarEnabled(false);
                    try {
                        setMessageViewerText("<all/>");

                        return;
                    } catch (Exception e1) {
                        throw new RuntimeException(e1);
                    }
                }
                final JTextField xpf = messageViewerToolBar.getxpathField();

                if (lpc instanceof BindingOperationTreeNode) {
                    BindingOperationTreeNode boperation = (BindingOperationTreeNode) lpc;
                    currentOperation = boperation.getOperation();
                    populateSampleMessages(currentOperation.getName(), 0);
                    Message sreq = forOperation(boperation.getOperation());
                    if (sreq != null) {
                        try {
                            SOAPMessage soapMessage = sreq.getSOAPMessage();
                            displayMessage(soapMessage);
                            if (e.getSource() == operationsTree.getSelectionModel()) {
                                xpf.setText("");
                            }
                        } catch (Exception e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                } else {
                    populateSampleMessages(null, 0);
                    displayMessage(blankMessage);
                    currentOperation = null;
                }
                messageViewerToolBar.setToolbarEnabled(true);
                xpathFieldPauseListener.textEntryResumed(xpf);
            }
        }
    };

    /**
     * Display soap message into the message viewer
     *
     * @throws RuntimeException wrapping the originasl exception
     */
    private void displayMessage(SOAPMessage soapMessage)
      throws RuntimeException {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            soapMessage.writeTo(bos);
            displayMessage(bos.toString());
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * display the string soap message onto the viewer
     *
     * @throws RuntimeException wrapping the originasl exception
     */
    private void displayMessage(String soapMessage)
      throws RuntimeException {
        try {
            org.w3c.dom.Document doc;
            try {
                doc = XmlUtil.stringToDocument(soapMessage);
                Map docns = XmlUtil.findAllNamespaces(doc.getDocumentElement());
                for (Object o : docns.keySet()) {
                    String prefix = (String) o;
                    String uri = (String) docns.get(prefix);
                    if (!namespaces.containsValue(uri)) {
                        namespaces.put(prefix, uri);
                    }
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Couldn't get namespaces from non-XML document", e);
            }
            setMessageViewerText(soapMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Message forOperation(BindingOperation bop) {
        String opName = bop.getOperation().getName();
        Binding binding = serviceWsdl.getBinding(bop);
        if (binding == null) {
            throw new IllegalArgumentException("Bindiong operation without binding " + opName);
        }
        String bindingName = binding.getQName().getLocalPart();

        for( Message soapRequest : soapMessages ) {
            if( opName.equals( soapRequest.getOperation() ) &&
                    bindingName.equals( soapRequest.getBinding() ) ) {
                return soapRequest;
            }
        }
        return null;
    }


    final PauseListener xpathFieldPauseListener = new PauseListenerAdapter() {
        public void textEntryPaused(JTextComponent component, long msecs) {
            final JTextField xpathField = (JTextField)component;
            XpathFeedBack feedBack = getFeedBackMessage(namespaces, xpathField);
            processFeedBack(feedBack, xpathField);
        }

        private void processHardwareFeedBack(XpathFeedBack hardwareFeedBack, JTextField xpathField) {
            if (!showHardwareAccelStatus) {
                hardwareAccelStatusLabel.setVisible(false);
                speedIndicator.setVisible(false);
                return;
            }

            if (!haveTarari) {
                hardwareAccelStatusLabel.setText("");
                hardwareAccelStatusLabel.setToolTipText(null);
                speedIndicator.setSpeed(SpeedIndicator.SPEED_FAST);
                String n = hardwareFeedBack == null ? "" : " be too complex to";
                speedIndicator.setToolTipText("Hardware accelerated XPath not present on Gateway, but if it were, this expression would" + n + " run in parallel at full speed");
                return;
            }

            if (hardwareFeedBack == null) {
                hardwareAccelStatusLabel.setText("");
                hardwareAccelStatusLabel.setToolTipText(null);
                speedIndicator.setSpeed(SpeedIndicator.SPEED_FASTEST);
                speedIndicator.setToolTipText("Expression will be hardware accelerated in parallel at full speed");
            } else {
                hardwareAccelStatusLabel.setText("");
                hardwareAccelStatusLabel.setToolTipText(null);
                speedIndicator.setSpeed(SpeedIndicator.SPEED_FASTER);
                speedIndicator.setToolTipText("Expression will be hardware accelerated, but is too complex to run in parallel at full speed");

                // Squiggles and detailed parse error messages are disabled for now
                if (false && xpathField instanceof SquigglyField) {
                    SquigglyField squigglyField = (SquigglyField)xpathField;
                    int pos = hardwareFeedBack.errorPosition;
                    if (pos >= 0)
                        squigglyField.setRange(pos - 1, pos + 1);
                    else
                        squigglyField.setAll();
                    squigglyField.setStraight();
                    squigglyField.setColor(Color.BLUE);
                }
            }
        }

        private void processFeedBack(XpathFeedBack feedBack, JTextField xpathField) {
            if (feedBack == null) feedBack = new XpathFeedBack(-1, null, null, null); // NPE guard

            if (feedBack.valid() || feedBack.isEmpty()) {
                if (xpathField instanceof SquigglyField) {
                    SquigglyField squigglyField = (SquigglyField)xpathField;
                    squigglyField.setNone();
                }
                xpathField.setToolTipText(null);
                processHardwareFeedBack(feedBack.hardwareAccelFeedback, xpathField);
                return;
            }

            processHardwareFeedBack(feedBack.hardwareAccelFeedback, xpathField);
            speedIndicator.setSpeed(0);
            speedIndicator.setToolTipText(null);
            StringBuffer tooltip = new StringBuffer();
            boolean htmlOpenAdded = false;
            if (feedBack.getErrorPosition() != -1) {
                tooltip.append("<html><b>Position : ").append(feedBack.getErrorPosition()).append(", ");
                htmlOpenAdded = true;
            }
            String msg = feedBack.getShortMessage();
            if (msg != null) {
                if (!htmlOpenAdded) {
                    msg = "<html><b>" + msg;
                }
                tooltip.append(msg).append("</b></html>");
            }

            xpathField.setToolTipText(tooltip.toString());

            if (xpathField instanceof SquigglyField) {
                SquigglyField squigglyField = (SquigglyField)xpathField;
                squigglyField.setAll();
                squigglyField.setSquiggly();
                squigglyField.setColor(Color.RED);
            }
        }

    };

    private XpathVersion getXpathVersion() {
        // For now we will not expose XPath 2.0 for this assertion.
        // TODO update GUI to expose XPath version selection
        return XpathVersion.XPATH_1_0;
    }

    private XpathFeedBack getFeedBackMessage(Map nsMap, JTextField xpathField) {
        final XpathVersion xpathVersion = getXpathVersion();

        String xpath = xpathField.getText();
        if (xpath == null) return new XpathFeedBack(-1, null, XpathFeedBack.EMPTY_MSG, XpathFeedBack.EMPTY_MSG);
        xpath = xpath.trim();
        if (xpath.length() < 1) return new XpathFeedBack(-1, null, XpathFeedBack.EMPTY_MSG, XpathFeedBack.EMPTY_MSG);
        try {
            final Set<String> variables = SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet();
            XpathUtil.testXpathExpression(testEvaluator, xpath, xpathVersion, namespaces, buildXpathVariableFinder(variables));
            XpathFeedBack feedback = new XpathFeedBack(-1, xpath, null, null);
            feedback.hardwareAccelFeedback = getHardwareAccelFeedBack(nsMap, xpath);
            return feedback;
        } catch (InvalidXpathException e) {
            return getXpathFeedBackForInvalidXpathException(xpath, e);
        } catch (RuntimeException e) { // sometimes NPE, sometimes NFE
            log.log(Level.WARNING, e.getMessage(), e);
            return new XpathFeedBack(-1, xpath, "XPath expression error '" + xpath + "'", null);
        }
    }

    private static XpathFeedBack getXpathFeedBackForInvalidXpathException(String xpath, InvalidXpathException e) {
        Exception c;

        //noinspection ThrowableResultOfMethodCallIgnored
        if ((c = ExceptionUtils.getCauseIfCausedBy(e, XPathSyntaxException.class)) != null) {
            XPathSyntaxException xpe = (XPathSyntaxException)c;
            log.log(Level.FINE, xpe.getMessage(), xpe);
            return new XpathFeedBack(xpe.getPosition(), xpath, xpe.getMessage(), xpe.getMultilineMessage());
        }

        //noinspection ThrowableResultOfMethodCallIgnored
        if ((c = ExceptionUtils.getCauseIfCausedBy(e, SAXPathException.class)) != null) {
            SAXPathException spe = (SAXPathException)c;
            log.log(Level.FINE, spe.getMessage(), spe);
            return new XpathFeedBack(-1, xpath, ExceptionUtils.getMessage( spe ), ExceptionUtils.getMessage(spe));
        }

        log.log(Level.WARNING, e.getMessage(), e);
        return new XpathFeedBack(-1, xpath, "XPath expression error '" + xpath + "'", null);
    }

    private XpathVariableFinder buildXpathVariableFinder( final Set<String> variables ) {
        return new XpathVariableFinder(){
            @Override
            public Object getVariableValue( final String namespaceUri,
                                            final String variableName ) throws NoSuchXpathVariableException {
                if ( namespaceUri != null )
                    throw new NoSuchXpathVariableException("Unsupported XPath variable namespace '"+namespaceUri+"'.");

                if ( !variables.contains(variableName) )
                    throw new NoSuchXpathVariableException("Unsupported XPath variable name '"+variableName+"'.");

                return "";
            }
        };
    }

    /**
     * @return feedback for hardware accel problems, or null if no hardware accel problems detected.
     */
    private XpathFeedBack getHardwareAccelFeedBack(Map nsMap, String xpath) {
        final XpathVersion xpathVersion = getXpathVersion();
        if (XpathVersion.XPATH_2_0.equals(xpathVersion)) {
            return new XpathFeedBack(-1, xpath, "Parallel XPath processing not supported for XPath 2.0", null);
        }

        XpathFeedBack hardwareFeedback;
        // Check if hardware accel is known not to work with this xpath
        String convertedXpath = xpath;
        try {
            final Set<String> variables = SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet();
            FastXpath fastXpath = TarariXpathConverter.convertToFastXpath(nsMap, xpath);
            convertedXpath = fastXpath.getExpression();
            XpathUtil.testXpathExpression(testEvaluator, convertedXpath, xpathVersion, namespaces, buildXpathVariableFinder(variables));
            hardwareFeedback = null;
        } catch (ParseException e) {
            hardwareFeedback = new XpathFeedBack(e.getErrorOffset(), convertedXpath, e.getMessage(), e.getMessage());
        } catch (InvalidXpathException e) {
            return getXpathFeedBackForInvalidXpathException(convertedXpath, e);
        } catch (RuntimeException e) { // sometimes NPE, sometimes NFE
            hardwareFeedback = new XpathFeedBack(-1, convertedXpath, "XPath expression error '" + convertedXpath + "'", null);
        }
        return hardwareFeedback;
    }

    private static class XpathFeedBack {
        private static final String EMPTY_MSG = "Empty XPath expression";
        int errorPosition = -1;
        String shortMessage = null;
        String detailedMessage = null;
        String xpathExpression = null;
        XpathFeedBack hardwareAccelFeedback = null;

        public XpathFeedBack(int errorPosition, String expression, String sm, String lm) {
            this.errorPosition = errorPosition;
            this.xpathExpression = expression;
            this.detailedMessage = lm;
            this.shortMessage = sm;
        }

        boolean valid() {
            return errorPosition == -1 && shortMessage == null;
        }

        public String getXpathExpression() {
            return xpathExpression;
        }

        public int getErrorPosition() {
            return errorPosition;
        }

        public String getDetailedMessage() {
            return detailedMessage;
        }

        public String getShortMessage() {
            return shortMessage;
        }

        public boolean isEmpty() {
            return EMPTY_MSG.equals(shortMessage);
        }
    }
}