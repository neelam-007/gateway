package com.l7tech.console.panels;

import com.l7tech.common.gui.util.PauseListener;
import com.l7tech.common.gui.util.TextComponentPauseListenerManager;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.SquigglyTextField;
import com.l7tech.common.security.xml.KeyReference;
import com.l7tech.common.security.xml.XencAlgorithm;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.*;
import com.l7tech.common.xml.SoapMessageGenerator.Message;
import com.l7tech.common.xml.tarari.util.TarariXpathConverter;
import com.l7tech.console.action.Actions;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.*;
import com.l7tech.console.tree.wsdl.BindingOperationTreeNode;
import com.l7tech.console.tree.wsdl.BindingTreeNode;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.xmlviewer.ExchangerDocument;
import com.l7tech.console.xmlviewer.Viewer;
import com.l7tech.console.xmlviewer.ViewerToolBar;
import com.l7tech.console.xmlviewer.properties.ConfigurationProperties;
import com.l7tech.console.xmlviewer.util.DocumentUtilities;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.ResponseXpathAssertion;
import com.l7tech.policy.assertion.SimpleXpathAssertion;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
import com.l7tech.service.PublishedService;
import com.l7tech.service.SampleMessage;
import com.l7tech.service.ServiceAdmin;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.jaxen.JaxenException;
import org.jaxen.XPathSyntaxException;
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
import java.io.*;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class XpathBasedAssertionPropertiesDialog extends JDialog {
    static final Logger log = Logger.getLogger(XpathBasedAssertionPropertiesDialog.class.getName());
    private JPanel mainPanel;
    private JPanel messageViewerPanel;
    private JPanel messageViewerToolbarPanel;
    private JLabel operationsLabel;
    private JTree operationsTree;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JLabel descriptionLabel;
    private XpathBasedAssertionTreeNode node;
    private XpathBasedAssertion assertion;
    private ServiceNode serviceNode;
    private Wsdl serviceWsdl;
    private JScrollPane treeScrollPane;
    private Message[] soapMessages;
    private String blankMessage = "<empty />";
    private Map namespaces = new HashMap();
    private Map requiredNamespaces = new HashMap();
    private Viewer messageViewer;
    private ViewerToolBar messageViewerToolBar;
    private ExchangerDocument exchangerDocument;
    private ActionListener okActionListener;
    private boolean isEncryption;
    private XpathEvaluator testEvaluator;
    private JButton namespaceButton;
    private JLabel hardwareAccelStatusLabel;
    private JPanel securityConfigPanel;
    private JPanel encryptionConfigPanel;
    private JPanel signatureResponseConfigPanel;
    private JRadioButton aes128radioButton;
    private JRadioButton aes192radioButton;
    private JRadioButton aes256radioButton;
    private JRadioButton tripleDESradioButton;
    private JRadioButton bstReferenceRadioButton;
    private JRadioButton skiReferenceRadioButton;
    private JTextField varPrefixField;
    private JLabel varPrefixLabel;

    private JComboBox sampleMessagesCombo;
    private DefaultComboBoxModel sampleMessagesComboModel;
    private JButton addSampleButton;
    private JButton removeSampleButton;
    private static final SampleMessageComboEntry USE_AUTOGEN = new SampleMessageComboEntry(null) {
        public String toString() {
            return "<use automatically generated message above>";
        }
    };
    private BindingOperation currentOperation;
    private JButton editSampleButton;

    /**
     * @param owner this panel owner
     * @param modal is this modal dialog or not
     * @param sn    the ServiceNode
     * @param n     the xml security node
     */
    public XpathBasedAssertionPropertiesDialog(JFrame owner, boolean modal, ServiceNode sn, XpathBasedAssertionTreeNode n, ActionListener okListener)
    {
        super(owner, modal);
        if (n == null) {
            throw new IllegalArgumentException();
        }
        construct(sn, n, okListener);
    }


    /**
     * @param owner this panel owner
     * @param modal is this modal dialog or not
     * @param n     the xml security node
     */
    public XpathBasedAssertionPropertiesDialog(JFrame owner, boolean modal, XpathBasedAssertionTreeNode n, ActionListener okListener)
    {
        super(owner, modal);
        if (n == null) {
            throw new IllegalArgumentException();
        }
        construct(null, n, okListener);
    }

    private void construct(ServiceNode sn, XpathBasedAssertionTreeNode n, ActionListener okListener) {
        node = n;
        okActionListener = okListener;

        assertion = (XpathBasedAssertion)node.asAssertion();
        if (assertion.getXpathExpression() != null) {
            namespaces = assertion.getXpathExpression().getNamespaces();
        } else {
            namespaces = new HashMap();
        }
        if (assertion instanceof RequestWssConfidentiality ||
          assertion instanceof ResponseWssConfidentiality) {
            isEncryption = true;
        } else
            isEncryption = false;
        if (sn != null)
            serviceNode = sn;
        else
            serviceNode = AssertionTreeNode.getServiceNode(node);
        if (serviceNode == null) {
            throw new IllegalStateException("Unable to determine the service node for " + assertion);
        }

        signatureResponseConfigPanel.setVisible(assertion instanceof ResponseWssIntegrity);
        try {
            serviceWsdl = serviceNode.getPublishedService().parsedWsdl();
            serviceWsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
            SoapMessageGenerator sg = new SoapMessageGenerator();
            if (isEditingRequest()) {
                soapMessages = sg.generateRequests(serviceWsdl);
            } else {
                soapMessages = sg.generateResponses(serviceWsdl);
            }
            initializeBlankMessage(soapMessages[0]);
            for (int i = 0; i < soapMessages.length; i++) {
                SoapMessageGenerator.Message soapRequest = soapMessages[i];
                requiredNamespaces.putAll(XpathEvaluator.getNamespaces(soapRequest.getSOAPMessage()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse the service WSDL " + serviceNode.getName(), e);
        }

        if (namespaces.size() < requiredNamespaces.size()) {
            namespaces.putAll(requiredNamespaces);
        }

        initialize();

        // display the existing xpath expression
        String initialvalue = null;
        if (assertion.getXpathExpression() != null) {
            initialvalue = assertion.getXpathExpression().getExpression();
        }
        messageViewerToolBar.getxpathField().setText(initialvalue);

        populateSampleMessages(null);
        enableSampleButtons();

        addSampleButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SampleMessage sm;
                try {
                    String xml = messageViewer.getContent();
                    try {
                        org.w3c.dom.Document doc = XmlUtil.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")));
                        xml = XmlUtil.nodeToFormattedString(doc);
                    } catch (Exception e1) {
                        log.log(Level.WARNING, "Invalid XML", e1);
                    }
                    PublishedService service = serviceNode.getPublishedService();
                    String name = currentOperation == null ? null : currentOperation.getName();
                    sm = new SampleMessage(service.getOid(), name, name, xml);
                } catch (Exception ex) {
                    throw new RuntimeException("Couldn't find PublishedService", ex);
                }

                SampleMessageDialog smd = showSampleMessageDialog(sm);
                if (smd.isOk()) {
                    try {
                        Registry.getDefault().getServiceManager().saveSampleMessage(sm);
                        SampleMessageComboEntry entry = new SampleMessageComboEntry(sm);
                        sampleMessagesComboModel.addElement(entry);
                        sampleMessagesComboModel.setSelectedItem(entry);
                    } catch (SaveException ex) {
                        throw new RuntimeException("Couldn't save SampleMessage", ex);
                    }
                }
            }
        });

        editSampleButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SampleMessageComboEntry entry = (SampleMessageComboEntry)sampleMessagesCombo.getSelectedItem();
                if (entry == USE_AUTOGEN) return;
                try {
                    SampleMessageDialog smd = showSampleMessageDialog(entry.message);
                    if (smd.isOk()) {
                        Registry.getDefault().getServiceManager().saveSampleMessage(entry.message);
                        sampleMessagesCombo.repaint();
                    }
                } catch (SaveException ex) {
                    throw new RuntimeException("Couldn't save SampleMessage", ex);
                }
            }
        });

        removeSampleButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                SampleMessageComboEntry entry = (SampleMessageComboEntry)sampleMessagesCombo.getSelectedItem();
                if (entry == USE_AUTOGEN) return;
                int resp = JOptionPane.showConfirmDialog(XpathBasedAssertionPropertiesDialog.this,
                        "Are you sure you want to remove '" + entry.toString() + "'?",
                        "Remove Sample Message",
                        JOptionPane.OK_CANCEL_OPTION);

                if (resp == JOptionPane.OK_OPTION) {
                    try {
                        Registry.getDefault().getServiceManager().deleteSampleMessage(entry.message);
                        sampleMessagesCombo.removeItem(entry);
                    } catch (DeleteException e1) {
                        throw new RuntimeException(e1);
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
    }

    private SampleMessageDialog showSampleMessageDialog(SampleMessage sm) {
        SampleMessageDialog smd = new SampleMessageDialog(this, sm, false);
        smd.pack();
        Utilities.centerOnScreen(smd);
        smd.setVisible(true);
        return smd;
    }

    private void populateSampleMessages(String operationName) {
        EntityHeader[] sampleMessages;
        ArrayList messageEntries = new ArrayList();
        messageEntries.add(USE_AUTOGEN);
        try {
            ServiceAdmin serviceManager = Registry.getDefault().getServiceManager();
            sampleMessages = serviceManager.findSampleMessageHeaders(serviceNode.getPublishedService().getOid(), operationName);
            for (int i = 0; i < sampleMessages.length; i++) {
                messageEntries.add(new SampleMessageComboEntry(serviceManager.findSampleMessageById(sampleMessages[i].getOid())));
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't get sample messages", e);
        }

        sampleMessagesComboModel = new DefaultComboBoxModel(messageEntries.toArray(new SampleMessageComboEntry[0]));
        sampleMessagesCombo.setModel(sampleMessagesComboModel);
        sampleMessagesCombo.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                enableSampleButtons();
            }
        });
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

        public String toString() {
            return message.getName();
        }

        private final SampleMessage message;
        private final Map namespaces = new HashMap();
    }


    private void initialize() {
        Actions.setEscKeyStrokeDisposes(this);

        initializeEncryptionConfig();
        initializeResponseSignatureConfig();

        namespaceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                java.util.List requiredNS = new ArrayList(requiredNamespaces.values());
                NamespaceMapEditor nseditor = new NamespaceMapEditor(XpathBasedAssertionPropertiesDialog.this,
                                                                     namespaces,
                                                                     requiredNS);
                nseditor.pack();
                Utilities.centerOnScreen(nseditor);
                nseditor.setVisible(true);
                Map newMap = nseditor.newNSMap();
                if (newMap != null) {
                    namespaces = newMap;
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                XpathBasedAssertionPropertiesDialog.this.dispose();
            }
        });

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
                        xpathmsg = "[empty]";
                    }
                    int rs2 = JOptionPane.showConfirmDialog(okButton, "The path " + xpathmsg + " is not valid (" +
                      res.getShortMessage() + ").\nAre you sure " +
                      "you want to save?");
                    if (rs2 != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                if (xpath == null || "".equals(xpath.trim())) {
                    assertion.setXpathExpression(null);
                } else {
                    assertion.setXpathExpression(new XpathExpression(xpath, namespaces));
                }

                if (assertion instanceof SimpleXpathAssertion) {
                    ((SimpleXpathAssertion)assertion).setVariablePrefix(varPrefixField.getText());
                }

                collectEncryptionConfig();
                collectResponseSignatureConfig();
                XpathBasedAssertionPropertiesDialog.this.dispose();
                if (okActionListener != null) okActionListener.actionPerformed(e);
            }
        });

        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(XpathBasedAssertionPropertiesDialog.this);
            }
        });
        try {
            final Wsdl wsdl = serviceNode.getPublishedService().parsedWsdl();
            final MutableTreeNode root = new DefaultMutableTreeNode();
            final DefaultTreeModel operationsTreeModel = new DefaultTreeModel(root);
            populateOperations(wsdl, operationsTreeModel, root);
            operationsTree.setModel(operationsTreeModel);
            operationsTree.setRootVisible(false);
            operationsTree.setShowsRootHandles(true);
            operationsTree.setCellRenderer(wsdlTreeRenderer);
            operationsTree.getSelectionModel().addTreeSelectionListener(operationsSelectionListener);

            initializeSoapMessageViewer(blankMessage);

            getContentPane().setLayout(new BorderLayout());
            getContentPane().add(mainPanel);
        } catch (WSDLException e) {
            throw new RuntimeException(e);
        } catch (FindException e) {
            throw new RuntimeException(e);
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXParseException e) {
            throw new RuntimeException(e);
        } catch (DocumentException e) {
            throw new RuntimeException(e);
        }

        // initialize the test evaluator
        try {
            testEvaluator = XpathEvaluator.newEvaluator(XmlUtil.stringToDocument("<blah xmlns=\"http://bzzt.com\"/>"),
                                                        new HashMap());
        } catch (Exception e) {
            final String msg = "cannot setup test evaluator";
            log.log(Level.WARNING, msg, e);
            throw new RuntimeException(msg, e);
        }
        TextComponentPauseListenerManager.registerPauseListener(messageViewerToolBar.getxpathField(), xpathFieldPauseListener, 700);

        String description = null;
        String title = null;
        if (assertion instanceof RequestWssConfidentiality) {
            description = "Select a request element to encrypt:";
            title = "Encrypt Request Element Properties";
        } else if (assertion instanceof ResponseWssConfidentiality) {
            description = "Select a response element to encrypt:";
            title = "Encrypt Response Element Properties";
        } else if (assertion instanceof RequestWssIntegrity) {
            description = "Select a request element to sign:";
            title = "Sign Request Element Properties";
        } else if (assertion instanceof ResponseWssIntegrity) {
            description = "Select a response element to sign:";
            title = "Sign Response Element Properties";
        } else if (assertion instanceof ResponseXpathAssertion) {
            description = "Select the response path to evaluate:";
            title = "Evaluate Response XPath Properties";
        } else if (assertion instanceof RequestXpathAssertion) {
            description = "Select the request path to evaluate:";
            title = "Evaluate Request XPath Properties";
        }

        if (assertion instanceof SimpleXpathAssertion) {
            SimpleXpathAssertion sxa = (SimpleXpathAssertion)assertion;
            varPrefixField.setText(sxa.getVariablePrefix());
            varPrefixField.setVisible(true);
            varPrefixLabel.setVisible(true);
        } else {
            varPrefixField.setVisible(false);
            varPrefixLabel.setVisible(false);
        }

        descriptionLabel.setText(description);
        setTitle(title);
    }

    private void initializeResponseSignatureConfig() {
        if (!(assertion instanceof ResponseWssIntegrity)) {
            signatureResponseConfigPanel.setVisible(false);
            return;
        }
        ResponseWssIntegrity rwssi = (ResponseWssIntegrity)assertion;
        ButtonGroup bg = new ButtonGroup();
        bg.add(bstReferenceRadioButton);
        bg.add(skiReferenceRadioButton);
        if (KeyReference.BST.getName().equals(rwssi.getKeyReference())) {
            bstReferenceRadioButton.setSelected(true);
        } else {
            skiReferenceRadioButton.setSelected(true);
        }
    }

    private void initializeEncryptionConfig() {
        if (!isEncryption) {
            encryptionConfigPanel.setVisible(false);
            return;
        }
        ButtonGroup bg = new ButtonGroup();
        bg.add(aes128radioButton);
        bg.add(aes192radioButton);
        bg.add(aes256radioButton);
        bg.add(tripleDESradioButton);

        String xencAlgorithm = null;

        if (assertion instanceof ResponseWssConfidentiality) {
            ResponseWssConfidentiality responseWssConfidentiality = (ResponseWssConfidentiality)assertion;
            xencAlgorithm = responseWssConfidentiality.getXEncAlgorithm();
        } else if (assertion instanceof RequestWssConfidentiality) {
            RequestWssConfidentiality requestWssConfidentiality = (RequestWssConfidentiality)assertion;
            xencAlgorithm = requestWssConfidentiality.getXEncAlgorithm();
        }
        if (xencAlgorithm == null) {
            xencAlgorithm = XencAlgorithm.AES_128_CBC.getXEncName();
        }
        aes128radioButton.setSelected(XencAlgorithm.AES_128_CBC.getXEncName().equals(xencAlgorithm));
        aes192radioButton.setSelected(XencAlgorithm.AES_192_CBC.getXEncName().equals(xencAlgorithm));
        aes256radioButton.setSelected(XencAlgorithm.AES_256_CBC.getXEncName().equals(xencAlgorithm));
        tripleDESradioButton.setSelected(XencAlgorithm.TRIPLE_DES_CBC.getXEncName().equals(xencAlgorithm));
    }

    private void collectEncryptionConfig() {
        if (!isEncryption) {
            return;
        }
        String xencAlgorithm = null;
        if (aes128radioButton.isSelected()) {
            xencAlgorithm = XencAlgorithm.AES_128_CBC.getXEncName();
        } else if (aes192radioButton.isSelected()) {
            xencAlgorithm = XencAlgorithm.AES_192_CBC.getXEncName();
        } else if (aes256radioButton.isSelected()) {
            xencAlgorithm = XencAlgorithm.AES_256_CBC.getXEncName();
        } else if (tripleDESradioButton.isSelected()) {
            xencAlgorithm = XencAlgorithm.TRIPLE_DES_CBC.getXEncName();
        }
        if (xencAlgorithm == null) {
            xencAlgorithm = XencAlgorithm.AES_128_CBC.getXEncName();
        }
        if (assertion instanceof ResponseWssConfidentiality) {
            ResponseWssConfidentiality responseWssConfidentiality = (ResponseWssConfidentiality)assertion;
            responseWssConfidentiality.setXEncAlgorithm(xencAlgorithm);
        } else if (assertion instanceof RequestWssConfidentiality) {
            RequestWssConfidentiality requestWssConfidentiality = (RequestWssConfidentiality)assertion;
            requestWssConfidentiality.setXEncAlgorithm(xencAlgorithm);
        }
    }

    private void collectResponseSignatureConfig() {
        if (!(assertion instanceof ResponseWssIntegrity)) {
            return;
        }
        ResponseWssIntegrity rwssi = (ResponseWssIntegrity)assertion;
        if (bstReferenceRadioButton.isSelected()) {
            rwssi.setKeyReference(KeyReference.BST.getName());
        } else if (skiReferenceRadioButton.isSelected()) {
            rwssi.setKeyReference(KeyReference.SKI.getName());
        }
    }

    /**
     * initialize the blank message tha is displayed on whole message
     * selection. The blank message is created from the first message,
     * without body nodes.
     *
     * @param soapMessage
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
        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
            Binding b = (Binding)iterator.next();
            if (showBindings) {
                final BindingTreeNode bindingTreeNode = new BindingTreeNode(b, wo);
                treeModel.insertNodeInto(bindingTreeNode, root, bindingsCounter++);
                parentNode = bindingTreeNode;
            }

            java.util.List operations = b.getBindingOperations();
            int index = 0;
            for (Iterator itop = operations.iterator(); itop.hasNext();) {
                BindingOperation bo = (BindingOperation)itop.next();
                treeModel.insertNodeInto(new BindingOperationTreeNode(bo, wo), parentNode, index++);
            }
        }
    }

    private void initializeSoapMessageViewer(String msg)
      throws IOException, SAXParseException, DocumentException {
        ConfigurationProperties cp = new ConfigurationProperties();
        exchangerDocument = asExchangerDocument(msg);
        messageViewer = new Viewer(cp.getViewer(), exchangerDocument, false);
        messageViewerToolBar = new ViewerToolBar(cp.getViewer(), messageViewer);
        com.intellij.uiDesigner.core.GridConstraints gridConstraints = new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 7, 7, null, null, null);
        messageViewerToolbarPanel.add(messageViewerToolBar, gridConstraints);
        com.intellij.uiDesigner.core.GridConstraints gridConstraints2 = new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 7, 7, null, null, null);
        messageViewerPanel.add(messageViewer, gridConstraints2);
    }

    private ExchangerDocument asExchangerDocument(String content)
      throws IOException, DocumentException, SAXParseException {

        ExchangerDocument exchangerDocument = new ExchangerDocument(asTempFileURL(content), false);
        exchangerDocument.load();
        return exchangerDocument;
    }

    private URL asTempFileURL(String content)
      throws IOException, DocumentException {
        final File file = File.createTempFile("Temp", ".xml");
        Document doc = DocumentUtilities.createReader(false).read(new StringReader(content));
        DocumentUtilities.writeDocument(doc, file.toURL());
        file.deleteOnExit();
        return file.toURL();
    }

    /**
     * @return whether we are editing the request
     */
    private boolean isEditingRequest() {
        return (node instanceof RequestWssIntegrityTreeNode ||
          node instanceof RequestWssConfidentialityTreeNode ||
          node instanceof RequestXpathPolicyTreeNode);
    }

    private final
    TreeCellRenderer wsdlTreeRenderer = new DefaultTreeCellRenderer() {
        /**
         * Sets the value of the current tree cell to <code>value</code>.
         *
         * @return the <code>Component</code> that the renderer uses to draw the value
         */
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
            } else {
                final Object lpc = path.getLastPathComponent();
                if (!((lpc instanceof BindingOperationTreeNode) || (lpc instanceof BindingTreeNode))) {
                    messageViewerToolBar.setToolbarEnabled(false);
                    return;
                }
                if (lpc instanceof BindingTreeNode) {
                    messageViewerToolBar.setToolbarEnabled(false);
                    try {
                        URL url = asTempFileURL("<all/>");
                        exchangerDocument.setProperties(url, null);
                        exchangerDocument.load();

                        return;
                    } catch (Exception e1) {
                        throw new RuntimeException(e1);
                    }
                }
                final JTextField xpf = messageViewerToolBar.getxpathField();

                BindingOperationTreeNode boperation = (BindingOperationTreeNode)lpc;
                currentOperation = boperation.getOperation();
                populateSampleMessages(currentOperation.getName());
                Message sreq = forOperation(boperation.getOperation());
                messageViewerToolBar.setToolbarEnabled(true);
                enableSampleButtons();
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
                xpathFieldPauseListener.textEntryResumed(xpf);
            }
        }
    };

    /**
     * Display soap message into the message viewer
     *
     * @param soapMessage
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
     * @param soapMessage
     * @throws RuntimeException wrapping the originasl exception
     */
    private void displayMessage(String soapMessage)
      throws RuntimeException {
        try {
            org.w3c.dom.Document doc;
            try {
                doc = XmlUtil.stringToDocument(soapMessage);
                Map docns = XmlUtil.findAllNamespaces(doc.getDocumentElement());
                for (Iterator i = docns.keySet().iterator(); i.hasNext();) {
                    String prefix = (String)i.next();
                    String uri = (String)docns.get(prefix);
                    if (!namespaces.containsValue(uri)) {
                        namespaces.put(prefix, uri);
                    }
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Couldn't get namespaces from non-XML document", e);
            }
            URL url = asTempFileURL(soapMessage);
            exchangerDocument.setProperties(url, null);
            exchangerDocument.load();
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

        for (int i = 0; i < soapMessages.length; i++) {
            Message soapRequest = soapMessages[i];
            if (opName.equals(soapRequest.getOperation()) &&
              bindingName.equals(soapRequest.getBinding())) {
                return soapRequest;
            }
        }
        return null;
    }


    final PauseListener xpathFieldPauseListener = new PauseListener() {
        public void textEntryPaused(JTextComponent component, long msecs) {
            final JTextField xpathField = (JTextField)component;
            XpathFeedBack feedBack = getFeedBackMessage(namespaces, xpathField);
            processFeedBack(feedBack, xpathField);
        }

        public void textEntryResumed(JTextComponent component) {
//                final JTextField xpathField = (JTextField)component;
//                XpathFeedBack feedBack = getFeedBackMessage(xpathField);
//                processFeedBack(feedBack, xpathField);
        }

        /*private XpathFeedBack getFeedBackMessage(JTextField xpathField) {
            String xpath = xpathField.getText();
            if (xpath == null) return XpathFeedBack.EMPTY;
            xpath = xpath.trim();
            if (xpath.length() < 1) return XpathFeedBack.EMPTY;
            if (isEncryption && xpath.equals("/soapenv:Envelope")) {
                return new XpathFeedBack(-1, xpath, "The path " + xpath + " is not valid for XML encryption", null);
            }
            try {
                testEvaluator.evaluate(xpath);
                return XpathFeedBack.OK;
            } catch (XPathSyntaxException e) {
                log.log(Level.FINE, e.getMessage(), e);
                return new XpathFeedBack(e.getPosition(), xpath, e.getMessage(), e.getMultilineMessage());
            } catch (JaxenException e) {
                log.log(Level.FINE, e.getMessage(), e);
                return new XpathFeedBack(-1, xpath, e.getMessage(), e.getMessage());
            } catch (RuntimeException e) { // sometimes NPE, sometimes NFE
                log.log(Level.WARNING, e.getMessage(), e);
                return new XpathFeedBack(-1, xpath, "XPath expression error '" + xpath + "'", null);
            }
        }*/

        private void processHardwareFeedBack(XpathFeedBack hardwareFeedBack, JTextField xpathField) {
            if (hardwareFeedBack == null) {
                hardwareAccelStatusLabel.setText(" ");
                hardwareAccelStatusLabel.setToolTipText(null);
            } else {
                hardwareAccelStatusLabel.setText("This expression cannot be hardware accelerated (reason: " +
                  hardwareFeedBack.getShortMessage() + "); it  will be " +
                  "processed in the software layer instead.");
                hardwareAccelStatusLabel.setToolTipText(hardwareFeedBack.getDetailedMessage());

                if (xpathField instanceof SquigglyTextField) {
                    SquigglyTextField squigglyTextField = (SquigglyTextField)xpathField;
                    int pos = hardwareFeedBack.errorPosition;
                    if (pos >= 0)
                        squigglyTextField.setRange(pos - 1, pos + 1);
                    else
                        squigglyTextField.setAll();
                    squigglyTextField.setStraight();
                    squigglyTextField.setColor(Color.BLUE);
                }
            }
        }

        private void processFeedBack(XpathFeedBack feedBack, JTextField xpathField) {
            if (feedBack == null) feedBack = new XpathFeedBack(-1, null, null, null); // NPE guard

            if (feedBack.valid() || feedBack.isEmpty()) {
                if (xpathField instanceof SquigglyTextField) {
                    SquigglyTextField squigglyTextField = (SquigglyTextField)xpathField;
                    squigglyTextField.setNone();
                }
                xpathField.setToolTipText(null);
                processHardwareFeedBack(feedBack.hardwareAccelFeedback, xpathField);
                return;
            }

            processHardwareFeedBack(feedBack.hardwareAccelFeedback, xpathField);
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

            if (xpathField instanceof SquigglyTextField) {
                SquigglyTextField squigglyTextField = (SquigglyTextField)xpathField;
                final String expr = feedBack.getXpathExpression();
                squigglyTextField.setAll();
                squigglyTextField.setSquiggly();
                squigglyTextField.setColor(Color.RED);
            }
        }

    };

    private XpathFeedBack getFeedBackMessage(Map nsMap, JTextField xpathField) {
        String xpath = xpathField.getText();
        if (xpath == null) return new XpathFeedBack(-1, null, XpathFeedBack.EMPTY_MSG, XpathFeedBack.EMPTY_MSG);
        xpath = xpath.trim();
        if (xpath.length() < 1) return new XpathFeedBack(-1, null, XpathFeedBack.EMPTY_MSG, XpathFeedBack.EMPTY_MSG);
        if (isEncryption && xpath.equals("/soapenv:Envelope")) {
            return new XpathFeedBack(-1, xpath, "The path " + xpath + " is not valid for XML encryption", null);
        }
        try {
            testEvaluator.evaluate(xpath);
            XpathFeedBack feedback = new XpathFeedBack(-1, xpath, null, null);
            feedback.hardwareAccelFeedback = getHardwareAccelFeedBack(nsMap, xpath);
            return feedback;
        } catch (XPathSyntaxException e) {
            log.log(Level.FINE, e.getMessage(), e);
            return new XpathFeedBack(e.getPosition(), xpath, e.getMessage(), e.getMultilineMessage());
        } catch (JaxenException e) {
            log.log(Level.FINE, e.getMessage(), e);
            return new XpathFeedBack(-1, xpath, e.getMessage(), e.getMessage());
        } catch (RuntimeException e) { // sometimes NPE, sometimes NFE
            log.log(Level.WARNING, e.getMessage(), e);
            return new XpathFeedBack(-1, xpath, "XPath expression error '" + xpath + "'", null);
        }
    }

    /**
     * @return feedback for hardware accel problems, or null if no hardware accel problems detected.
     */
    private XpathFeedBack getHardwareAccelFeedBack(Map nsMap, String xpath) {
        XpathFeedBack hardwareFeedback;
        // Check if hardware accel is known not to work with this xpath
        String convertedXpath = xpath;
        try {
            convertedXpath = TarariXpathConverter.convertToTarariXpath(nsMap, xpath);
            testEvaluator.evaluate(convertedXpath);
            hardwareFeedback = null;
        } catch (ParseException e) {
            hardwareFeedback = new XpathFeedBack(e.getErrorOffset(), convertedXpath, e.getMessage(), e.getMessage());
        } catch (XPathSyntaxException e) {
            hardwareFeedback = new XpathFeedBack(e.getPosition(), convertedXpath, e.getMessage(), e.getMultilineMessage());
        } catch (JaxenException e) {
            hardwareFeedback = new XpathFeedBack(-1, convertedXpath, e.getMessage(), e.getMessage());
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
