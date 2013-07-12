package com.l7tech.console.panels;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.console.action.Actions;
import com.l7tech.console.policy.SsmPolicyVariableUtils;
import com.l7tech.console.tree.EntityWithPolicyNode;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.XpathBasedAssertionTreeNode;
import com.l7tech.console.tree.wsdl.BindingOperationTreeNode;
import com.l7tech.console.tree.wsdl.BindingTreeNode;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.XmlViewer;
import com.l7tech.gateway.common.cluster.ClusterStatusAdmin;
import com.l7tech.gateway.common.service.SampleMessage;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.gui.util.*;
import com.l7tech.gui.widgets.SpeedIndicator;
import com.l7tech.gui.widgets.SquigglyField;
import com.l7tech.gui.widgets.SquigglyTextField;
import com.l7tech.gui.widgets.TextListCellRenderer;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.assertion.*;
import com.l7tech.policy.assertion.xmlsec.RequireWssEncryptedElement;
import com.l7tech.policy.assertion.xmlsec.RequireWssSignedElement;
import com.l7tech.policy.assertion.xmlsec.WssEncryptElement;
import com.l7tech.policy.assertion.xmlsec.WssSignElement;
import com.l7tech.policy.variable.DataType;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.policy.variable.VariableMetadata;
import com.l7tech.security.xml.KeyReference;
import com.l7tech.security.xml.SupportedDigestMethods;
import com.l7tech.security.xml.SupportedSignatureMethods;
import com.l7tech.security.xml.XencAlgorithm;
import com.l7tech.util.*;
import com.l7tech.wsdl.Wsdl;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.soap.SoapMessageGenerator;
import com.l7tech.xml.soap.SoapUtil;
import com.l7tech.xml.soap.SoapVersion;
import com.l7tech.xml.tarari.util.TarariXpathConverter;
import com.l7tech.xml.xpath.*;
import org.dom4j.DocumentException;
import org.jaxen.XPathSyntaxException;
import org.jaxen.saxpath.SAXPathException;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.text.JTextComponent;
import javax.swing.tree.*;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import javax.xml.soap.SOAPConstants;
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
public class XpathBasedAssertionPropertiesDialog extends AssertionPropertiesEditorSupport<XpathBasedAssertion> {
    static final Logger log = Logger.getLogger(XpathBasedAssertionPropertiesDialog.class.getName());

    public static final boolean SHOW_WHOLE_ELEMENT_ENCRYPTION_CONFIG =
            ConfigFactory.getBooleanProperty( "com.l7tech.console.panels.XpathBasedAssertionPropertiesDialog.showWholeElementEncryption", false );

    private JPanel mainPanel;
    private JPanel messageViewerPanel;
    private JPanel messageViewerToolbarPanel;
    private JTree operationsTree;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private JLabel descriptionLabel;
    private JPanel xmlMsgSrcPanel;
    private JComboBox xmlMsgSrcComboBox;
    private XpathBasedAssertion assertion;
    private ServiceNode serviceNode;
    private EntityWithPolicyNode policyNode;
    private Wsdl serviceWsdl;
    private SoapMessageGenerator.Message[] soapMessages;
    private String blankMessage = "<empty />";
    private Map<String, String> namespaces = new HashMap<String, String>();
    private Map<String, String> allOperationNamespaces = new HashMap<String, String>();
    private XmlViewer messageViewer;
    private XpathToolBar messageViewerToolBar;
    private ActionListener okActionListener;
    private boolean isEncryption;
    private boolean haveTarari;
    private org.w3c.dom.Document testEvaluator;
    private JButton namespaceButton;
    private JPanel speedIndicatorPanel;
    private JPanel encryptionAlgorithmsPanel;
    private JPanel signatureResponseConfigPanel;
    private JComboBox signatureDigestComboBox;
    private JPanel signatureDigestAlgorithmPanel;
    private JCheckBox aes128CheckBox;
    private JCheckBox aes192CheckBox;
    private JCheckBox aes256CheckBox;
    private JCheckBox tripleDESCheckBox;
    private JPanel encryptionCheckBoxesPanel;
    private JComboBox encryptionMethodComboBox;
    private JRadioButton bstReferenceRadioButton;
    private JRadioButton skiReferenceRadioButton;
    private JRadioButton issuerSerialReferenceRadioButton;
    private JCheckBox signedBstCheckBox;
    private JPanel varPrefixFieldPanel;
    private TargetVariablePanel varPrefixField;
    private JLabel varPrefixLabel;
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
    private JRadioButton encBstReferenceRadioButton;
    private JRadioButton encSkiReferenceRadioButton;
    private JRadioButton encIssuerSerialReferenceRadioButton;
    private JRadioButton encKeyNameReferenceRadioButton;
    private JPanel encryptionResponseConfigPanel;
    private JPanel signaturesAceptedPanel;
    private JCheckBox acceptSha1;
    private JCheckBox acceptSha256;
    private JCheckBox acceptSha384;
    private JCheckBox acceptSha512;
    private JPanel encryptionConfigPanel;
    private JRadioButton encryptElementContentsOnlyRadioButton;
    private JRadioButton encryptEntireElementIncludingRadioButton;
    private JCheckBox aes128GcmCheckBox;
    private JCheckBox aes256GcmCheckBox;
    private JComboBox xpathVersionComboBox;
    private List<JCheckBox> acceptedDigestCheckboxes;
    private final boolean showHardwareAccelStatus;
    private boolean wasOk = false;

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

    // It is OK to silently redeclare a SOAP 1.1 prefix as SOAP 1.2 and vice versa when automatically generating an XPath
    // in response to a click on an element within a sample message.
    private static final Map<String, Set<String>> SUBSTITUTABLE_NAMESPACES;
    static {
        Set<String> soaps = new HashSet<String>();
        soaps.add(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE);
        soaps.add(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE);
        soaps = Collections.unmodifiableSet(soaps);
        Map<String, Set<String>> subs = new HashMap<String, Set<String>>();
        subs.put(SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE, soaps);
        subs.put(SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE, soaps);
        SUBSTITUTABLE_NAMESPACES = Collections.unmodifiableMap(subs);
    }

    /**
     * @param owner this panel owner
     * @param modal is this modal dialog or not
     * @param n     the xml security node
     */
    public XpathBasedAssertionPropertiesDialog(final Window owner,
                                               final boolean modal,
                                               final XpathBasedAssertionTreeNode<? extends XpathBasedAssertion> n,
                                               final ActionListener okListener,
                                               final boolean showHardwareAccelStatus,
                                               final boolean readOnly) {
        super(owner, "", modal ? XpathBasedAssertionPropertiesDialog.DEFAULT_MODALITY_TYPE : ModalityType.MODELESS);
        if (n == null) {
            throw new IllegalArgumentException();
        }
        this.showHardwareAccelStatus = showHardwareAccelStatus;
        construct(n.asAssertion(), okListener, readOnly);
    }

    public XpathBasedAssertionPropertiesDialog( final Window owner, final XpathBasedAssertion assertion ) {
        super( owner, "" );
        this.assertion = assertion;
        this.showHardwareAccelStatus = shouldShowHardwareAccelStatus(assertion);
    }


    @Override
    public XpathBasedAssertion getData(final XpathBasedAssertion assertion) {
        return this.assertion;
    }

    @Override
    public void setData(final XpathBasedAssertion assertion) {
        this.assertion = assertion;
    }

    @Override
    public boolean isConfirmed() {
        return wasOk;
    }

    @Override
    public JDialog getDialog() {
        construct( assertion, null, isReadOnly() );
        return super.getDialog();
    }

    private boolean shouldShowHardwareAccelStatus(XpathBasedAssertion xpathBasedAssertion) {
        return xpathBasedAssertion instanceof RequestXpathAssertion || xpathBasedAssertion instanceof ResponseXpathAssertion;
    }

    private void construct(final XpathBasedAssertion assertion, @Nullable final ActionListener okListener, final boolean readOnly) {
        okActionListener = okListener;

        this.assertion = assertion;
        if (assertion.getXpathExpression() != null) {
            final Map<String, String> nsmap = assertion.getXpathExpression().getNamespaces();
            if (nsmap != null)
                namespaces.putAll(nsmap);
        }
        isEncryption = assertion instanceof RequireWssEncryptedElement ||
                assertion instanceof WssEncryptElement;
        final EntityWithPolicyNode entityWithPolicyNode = TopComponents.getInstance().getPolicyEditorPanel().getPolicyNode();
        serviceNode = entityWithPolicyNode instanceof ServiceNode ? (ServiceNode) entityWithPolicyNode : null;
        if ( serviceNode == null ) {
            policyNode = entityWithPolicyNode;
            if ( policyNode == null ) {
                throw new IllegalStateException("Unable to determine the PolicyEntityNode for " + assertion);
            }
        }
        XpathVersion version = assertion.getXpathExpression().getXpathVersion();
        if (version == null || XpathVersion.UNSPECIFIED.equals(version))
            version = XpathVersion.XPATH_1_0;
        xpathVersionComboBox.setSelectedItem(version.getVersionString());
        xpathVersionComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateXpathFeedback(messageViewerToolBar.getxpathField());
            }
        });

        if (assertion instanceof ResponseXpathAssertion) {
            final ResponseXpathAssertion ass = (ResponseXpathAssertion)assertion;
            xmlMsgSrcPanel.setVisible(true);

            // Populates xml message source combo box, and selects according to assertion.
            xmlMsgSrcComboBox.addItem(new MsgSrcComboBoxItem(null, "Default Response"));

            Map<String, VariableMetadata> predecessorVariables = SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion);

            final SortedSet<String> predecessorVariableNames = new TreeSet<String>(predecessorVariables.keySet());
            for (String variableName: predecessorVariableNames) {
                if (predecessorVariables.get(variableName).getType() == DataType.MESSAGE) {
                    final MsgSrcComboBoxItem item = new MsgSrcComboBoxItem(variableName, "Context Variable: " + Syntax.SYNTAX_PREFIX + variableName + Syntax.SYNTAX_SUFFIX);
                    xmlMsgSrcComboBox.addItem(item);
                    if (variableName.equals(ass.getXmlMsgSrc())) {
                        xmlMsgSrcComboBox.setSelectedItem(item);
                    }
                }
            }
        } else {
            xmlMsgSrcPanel.setVisible(false);
        }

        if (!(assertion instanceof RequestXpathAssertion || assertion instanceof ResponseXpathAssertion)) {
            xpathVersionComboBox.setEnabled(false);
        }

        final boolean isSignature = assertion instanceof WssSignElement;
        signatureResponseConfigPanel.setVisible(isSignature);
        signatureDigestAlgorithmPanel.setVisible(isSignature);
        if (isSignature) {
            String[] digests = SupportedDigestMethods.getDigestNames();
            digests = ArrayUtils.unshift(digests, "Automatic");
            signatureDigestComboBox.setModel(new DefaultComboBoxModel(digests));
        }
        if (serviceNode != null) {
            try {
                serviceWsdl = serviceNode.getEntity().parsedWsdl();
                if (serviceWsdl != null) {
                    serviceWsdl.setShowBindings(Wsdl.SOAP_BINDINGS);
                    SoapMessageGenerator sg = new SoapMessageGenerator(null, new Wsdl.UrlGetter() {
                        @Override
                        public String get(String url) throws IOException {
                            return Registry.getDefault().getServiceManager().resolveWsdlTarget(url);
                        }
                    });
                    if (isEditingRequest()) {
                        soapMessages = sg.generateRequests(serviceWsdl);
                    } else {
                        soapMessages = sg.generateResponses(serviceWsdl);
                    }
                    if (soapMessages.length > 0) {
                        // Try to find a message that matches the service's SOAP version for the default
                        SoapVersion soapVersion = serviceNode.getEntity().getSoapVersion();
                        if (soapVersion != null && soapVersion.getNamespaceUri() != null) {
                            for (SoapMessageGenerator.Message soapMessage : soapMessages) {
                                if (soapVersion.getNamespaceUri().equals(soapMessage.getSOAPMessage().getSOAPPart().getEnvelope().getNamespaceURI())) {
                                    initializeBlankMessage(soapMessage);
                                    allOperationNamespaces.putAll(XpathUtil.getNamespaces(soapMessage.getSOAPMessage()));
                                    break;
                                }
                            }
                        } else {
                            initializeBlankMessage(soapMessages[0]);
                            allOperationNamespaces.putAll(XpathUtil.getNamespaces(soapMessages[0].getSOAPMessage()));
                        }
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException("Unable to parse the service WSDL " + serviceNode.getName(), e);
            }
        } else {
            try {
                // policyNode is guaranteed not to be null, since it's been set up in the method construct(...).
                Policy policy = policyNode.getPolicy();
                if (policy.isSoap()) {
                    blankMessage = SoapMessageGenerator.SOAP_1_1_TEMPLATE;
                }
            } catch (FindException e) {
                throw new RuntimeException("Couldn't find the policy", e);
            }
        }

        speedIndicator = new SpeedIndicator(0);
        speedIndicatorPanel.setLayout(new BorderLayout());
        speedIndicatorPanel.add(speedIndicator, BorderLayout.CENTER);

        initialize(readOnly);

        // display the existing xpath expression
        String initialvalue = null;
        if (assertion.getXpathExpression() != null) {
            initialvalue = assertion.getXpathExpression().getExpression();
        }
        messageViewerToolBar.getxpathField().setText(initialvalue);

        populateSampleMessages(null, null);
        enableSampleButtons();

        addSampleButton.addActionListener(new ActionListener() {
            @Override
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
                    long objectId = (serviceNode == null)? -1 : serviceNode.getEntity().getOid();
                    sm = new SampleMessage(objectId, name, name, xml);
                } catch (Exception ex) {
                    throw new RuntimeException("Couldn't find PublishedService", ex);
                }

                showSampleMessageDialog(sm, new Functions.UnaryVoid<SampleMessageDialog>() {
                    @Override
                    public void call(SampleMessageDialog smd) {
                        if (smd.isOk()) {
                            try {
                                Goid goid = Registry.getDefault().getServiceManager().saveSampleMessage(sm);
                                populateSampleMessages(currentOperation == null ? null : currentOperation.getName(), goid);
                                sampleMessagesCombo.repaint();
                                refreshDialog();
                            } catch (SaveException ex) {
                                throw new RuntimeException("Couldn't save SampleMessage", ex);
                            }
                        }
                    }
                });
            }
        });

        editSampleButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final SampleMessageComboEntry entry = (SampleMessageComboEntry)sampleMessagesCombo.getSelectedItem();
                final SampleMessage tempMsg = new SampleMessage(entry.message.getServiceOid(), entry.message.getName(), entry.message.getOperationName(), entry.message.getXml());
                tempMsg.copyFrom(entry.message);
                if (entry == USE_AUTOGEN) return;
                showSampleMessageDialog(tempMsg, new Functions.UnaryVoid<SampleMessageDialog>() {
                    @Override
                    public void call(SampleMessageDialog smd) {
                        if (smd.isOk()) {
                            try {
                                Registry.getDefault().getServiceManager().saveSampleMessage(tempMsg);
                                entry.message.copyFrom(tempMsg);
                            } catch (SaveException ex) {
                                throw new RuntimeException("Couldn't save SampleMessage", ex);
                            }
                            sampleMessagesCombo.repaint();
                            displayMessage(smd.getMessage().getXml());
                            refreshDialog();
                        }
                    }
                });
            }
        });

        removeSampleButton.addActionListener(new ActionListener() {
            @Override
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
                        throw new RuntimeException("Couldn't delete SampleMessage", e1);
                    }
                }
            }
        });

        sampleMessagesCombo.addActionListener(new ActionListener() {
            @Override
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
    }

    /**
     * Resize the dialog due to some components getting extended.
     */
    private void refreshDialog() {
        if (getSize().width < mainPanel.getMinimumSize().width) {
            setSize(mainPanel.getMinimumSize().width, getSize().height);
        }
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
        Utilities.centerOnParentWindow(smd);
        DialogDisplayer.display(smd, new Runnable() {
            @Override
            public void run() {
                result.call(smd);
            }
        });
    }

    private void populateSampleMessages(@Nullable String operationName, Goid whichToSelect) {
        EntityHeader[] sampleMessages;
        ArrayList<SampleMessageComboEntry> messageEntries = new ArrayList<SampleMessageComboEntry>();
        messageEntries.add(USE_AUTOGEN);
        SampleMessageComboEntry whichEntryToSelect = null;
        try {
            ServiceAdmin serviceManager = Registry.getDefault().getServiceManager();
            long objectId = (serviceNode == null)? -1 : serviceNode.getEntity().getOid();
            sampleMessages = serviceManager.findSampleMessageHeaders(objectId, operationName);
            for (EntityHeader sampleMessage : sampleMessages) {
                Goid thisGoid = sampleMessage.getGoid();
                SampleMessageComboEntry entry = new SampleMessageComboEntry(serviceManager.findSampleMessageById(thisGoid));
                if (thisGoid.equals(whichToSelect)) whichEntryToSelect = entry;
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
            @Override
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
        private SampleMessageComboEntry(SampleMessage message) {
            this.message = message;
            if (message == null) return;
            try {
                org.w3c.dom.Document doc = XmlUtil.stringToDocument(message.getXml());
                DomUtils.findAllNamespaces(doc.getDocumentElement());
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

        initializeEncryptionConfig();
        initializeResponseEncryptionConfig();
        initializeRequireSignatureConfig();
        initializeResponseSignatureConfig();
        setModal(true);
        namespaceButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final NamespaceMapEditor namespaceMapEditor = new NamespaceMapEditor(
                        XpathBasedAssertionPropertiesDialog.this,
                        namespaces,
                        null,
                        new Functions.Nullary<Set<String>>() {
                            @Override
                            public Set<String> call() {
                                return findUnusedDeclarations(namespaces);
                            }
                        });
                namespaceMapEditor.pack();
                Utilities.centerOnScreen(namespaceMapEditor);
                DialogDisplayer.display(namespaceMapEditor, new Runnable() {
                    @Override
                    public void run() {
                        Map<String,String> newMap = namespaceMapEditor.newNSMap();
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
            @Override
            public void actionPerformed(ActionEvent e) {
                XpathBasedAssertionPropertiesDialog.this.dispose();
            }
        });

        okButton.setEnabled( !readOnly );
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // get xpath from control and the namespace map
                // then save it in assertion
                JTextField xpathTextField = messageViewerToolBar.getxpathField();
                String xpath = xpathTextField.getText();
                XpathFeedBack res = getFeedBackMessage(namespaces, xpathTextField);
                if (res != null && !res.valid()) {
                    if (xpath == null || xpath.trim().equals("")) {
                        DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null,
                                "Please enter an XPath value.", null);
                        return;
                    }
                    int rs2 = JOptionPane.showConfirmDialog(okButton, "The path " + xpath + " is not valid (" +
                      res.getShortMessage() + ").\nAre you sure " +
                      "you want to save?");
                    if (rs2 != JOptionPane.YES_OPTION) {
                        return;
                    }
                }
                if (xpath == null || "".equals(xpath.trim())) {
                    assertion.setXpathExpression(null);
                } else {
                    String version = (String) xpathVersionComboBox.getSelectedItem();
                    assertion.setXpathExpression(new XpathExpression(xpath, XpathVersion.fromVersionString(version), namespaces));
                }

                if (assertion instanceof SimpleXpathAssertion) {
                    ((SimpleXpathAssertion)assertion).setVariablePrefix(varPrefixField.getVariable());
                }

                if (assertion instanceof RequireWssSignedElement) {
                    if ( varPrefixField.getVariable()!= null && varPrefixField.getVariable().length() > 0 ) {
                        ((RequireWssSignedElement)assertion).setVariablePrefix( varPrefixField.getVariable() );
                    } else {
                        ((RequireWssSignedElement)assertion).setVariablePrefix( null );
                    }
                    ((RequireWssSignedElement)assertion).setAcceptedDigestAlgorithms(collectAcceptedDigests());
                }

                if (assertion instanceof ResponseXpathAssertion) {
                    final ResponseXpathAssertion ass = (ResponseXpathAssertion)assertion;
                    ass.setXmlMsgSrc(((MsgSrcComboBoxItem)xmlMsgSrcComboBox.getSelectedItem()).getVariableName());
                }

                if (!collectEncryptionConfig()) { // check if there exists invalid configurations or not.
                    return;
                }
                collectResponseEncryptionConfig();
                collectResponseSignatureConfig();
                XpathBasedAssertionPropertiesDialog.this.dispose();
                wasOk = true;
                if (okActionListener != null) okActionListener.actionPerformed(e);
            }
        });

        helpButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(XpathBasedAssertionPropertiesDialog.this);
            }
        });
        try {
            final MutableTreeNode root = new DefaultMutableTreeNode();
            final DefaultTreeModel operationsTreeModel = new DefaultTreeModel(root);

            if (serviceNode != null) {
                final Wsdl wsdl = serviceNode.getEntity().parsedWsdl();
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

        String description = null;
        String title = null;
        if (assertion instanceof RequireWssEncryptedElement) {
            description = "Select encrypted elements:";
            title = "Encrypted " + ((RequireWssEncryptedElement)assertion).getTargetName() + " Element Properties";
        } else if (assertion instanceof WssEncryptElement) {
            description = "Select elements to encrypt:";
            title = "Encrypt " + ((WssEncryptElement)assertion).getTargetName() + " Element Properties";
        } else if (assertion instanceof RequireWssSignedElement) {
            description = "Select signed elements:";
            title = "Signed " + ((RequireWssSignedElement)assertion).getTargetName() + " Element Properties";
        } else if (assertion instanceof WssSignElement) {
            description = "Select elements to sign:";
            title = "Sign " + ((WssSignElement)assertion).getTargetName() + " Element Properties";
        } else if (assertion instanceof ResponseXpathAssertion) {
            description = "Select the response path to evaluate:";
            title = assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString();
        } else if (assertion instanceof RequestXpathAssertion) {
            description = "Select the request path to evaluate:";
            title = assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_NAME).toString();
        }

        varPrefixField = new TargetVariablePanel();
        varPrefixFieldPanel.setLayout(new BorderLayout());
        varPrefixFieldPanel.add(varPrefixField, BorderLayout.CENTER);
        varPrefixField.setAcceptEmpty(true);
        varPrefixField.addChangeListener(new ChangeListener(){
            @Override
            public void stateChanged(ChangeEvent e) {
                okButton.setEnabled(!varPrefixField.isVisible() || varPrefixField.isEntryValid());
            }
        });

        boolean processPrefix = false;
        String prefix = null;
        if ( assertion instanceof SimpleXpathAssertion ) {
            SimpleXpathAssertion sxa = (SimpleXpathAssertion)assertion;
            processPrefix = true;
            prefix = sxa.getVariablePrefix();
            varPrefixField.setSuffixes(sxa.getVariableSuffixes());
        } else if ( assertion instanceof RequireWssSignedElement) {
            RequireWssSignedElement sxa = (RequireWssSignedElement)assertion;
            processPrefix = true;
            prefix = sxa.getVariablePrefix();
            varPrefixField.setAcceptEmpty(true);
            varPrefixField.setSuffixes(sxa.getVariableSuffixes());
        }

        if ( processPrefix ) {
            varPrefixField.setVariable(prefix);
            varPrefixField.setVisible(true);
            varPrefixLabel.setVisible(true);
            varPrefixField.setAssertion(assertion,getPreviousAssertion());
        } else {
            varPrefixField.setVisible(false);
            varPrefixLabel.setVisible(false);
        }

        descriptionLabel.setText(description);
        setTitle(title);
    }

    private void initializeRequireSignatureConfig() {
        acceptedDigestCheckboxes = new ArrayList<JCheckBox>();
        if (assertion instanceof RequireWssSignedElement) {
            signaturesAceptedPanel.setVisible(true);
            acceptSha1.setText(SupportedSignatureMethods.RSA_SHA1.getDigestAlgorithmName());
            acceptedDigestCheckboxes.add(acceptSha1);
            acceptSha256.setText(SupportedSignatureMethods.RSA_SHA256.getDigestAlgorithmName());
            acceptedDigestCheckboxes.add(acceptSha256);
            acceptSha384.setText(SupportedSignatureMethods.RSA_SHA384.getDigestAlgorithmName());
            acceptedDigestCheckboxes.add(acceptSha384);
            acceptSha512.setText(SupportedSignatureMethods.RSA_SHA512.getDigestAlgorithmName());
            acceptedDigestCheckboxes.add(acceptSha512);
            for(JCheckBox digest : acceptedDigestCheckboxes) {
                digest.setSelected(((RequireWssSignedElement)assertion).acceptsDigest(SupportedDigestMethods.fromAlias(digest.getText()).getIdentifier()));
            }
        } else {
            signaturesAceptedPanel.setVisible(false);
        }
    }

    private void initializeResponseSignatureConfig() {
        if (!(assertion instanceof WssSignElement)) {
            signatureResponseConfigPanel.setVisible(false);
            return;
        }
        WssSignElement rwssi = (WssSignElement)assertion;
        signedBstCheckBox.setSelected(rwssi.isProtectTokens());
        if (KeyReference.BST.getName().equals(rwssi.getKeyReference())) {
            bstReferenceRadioButton.setSelected(true);
        } else if (KeyReference.ISSUER_SERIAL.getName().equals(rwssi.getKeyReference())) {
            issuerSerialReferenceRadioButton.setSelected(true);
        } else {
            skiReferenceRadioButton.setSelected(true);
        }
        signatureDigestComboBox.setSelectedIndex(0);
        String digAlg = rwssi.getDigestAlgorithmName();
        if (digAlg != null)
            signatureDigestComboBox.setSelectedItem(digAlg);
    }

    private void initializeResponseEncryptionConfig() {
        if (!(assertion instanceof WssEncryptElement)) {
            encryptionResponseConfigPanel.setVisible(false);
            return;
        }
        WssEncryptElement wee = (WssEncryptElement)assertion;
        if (KeyReference.ISSUER_SERIAL.getName().equals(wee.getKeyReference())) {
            encIssuerSerialReferenceRadioButton.setSelected(true);
        } else if (KeyReference.BST.getName().equals(wee.getKeyReference())) {
            encBstReferenceRadioButton.setSelected(true);
        } else if (KeyReference.KEY_NAME.getName().equals(wee.getKeyReference())) {
            encKeyNameReferenceRadioButton.setSelected(true);
        } else {
            encSkiReferenceRadioButton.setSelected(true);
        }
    }

    private void initializeEncryptionConfig() {
        if (!isEncryption) {
            encryptionAlgorithmsPanel.setVisible(false);
            encryptionConfigPanel.setVisible(false);
            return;
        }

        if (!SHOW_WHOLE_ELEMENT_ENCRYPTION_CONFIG) {
            encryptionConfigPanel.setVisible(false);
        }

        if (assertion instanceof WssEncryptElement) {
            encryptionCheckBoxesPanel.setVisible( false );

            WssEncryptElement responseWssConfidentiality = (WssEncryptElement)assertion;
            String xencAlgorithm = responseWssConfidentiality.getXEncAlgorithm();

            if (xencAlgorithm == null) {
                xencAlgorithm = XencAlgorithm.AES_128_CBC.getXEncName();
            }

            final Map<String,String> nameMap = CollectionUtils.<String,String>treeMapBuilder()
                .put( XencAlgorithm.AES_128_CBC.getXEncName(), "AES 128 CBC" )
                .put( XencAlgorithm.AES_192_CBC.getXEncName(), "AES 192 CBC" )
                .put( XencAlgorithm.AES_256_CBC.getXEncName(), "AES 256 CBC" )
                .put( XencAlgorithm.TRIPLE_DES_CBC.getXEncName(), "Triple DES" )
                .put( XencAlgorithm.AES_128_GCM.getXEncName(), "AES 128 GCM" )
                .put( XencAlgorithm.AES_256_GCM.getXEncName(), "AES 256 GCM" )
                .unmodifiableMap();
            encryptionMethodComboBox.setModel( new DefaultComboBoxModel( nameMap.keySet().toArray() ) );
            encryptionMethodComboBox.setRenderer( new TextListCellRenderer<String>( new Functions.Unary<String,String>(){
                @Override
                public String call( final String algorithmName ) {
                    final String displayName = nameMap.get( algorithmName );
                    return displayName == null ? "" : displayName;
                }
            } ) );
            encryptionMethodComboBox.setSelectedItem( xencAlgorithm );

            boolean contentsOnly = responseWssConfidentiality.isEncryptContentsOnly();
            encryptElementContentsOnlyRadioButton.setSelected(contentsOnly);
            encryptEntireElementIncludingRadioButton.setSelected(!contentsOnly);

        } else if (assertion instanceof RequireWssEncryptedElement) {
            encryptionMethodComboBox.setVisible( false );

            RequireWssEncryptedElement requestWssConfidentiality = (RequireWssEncryptedElement)assertion;
            List<String> xencAlgorithmlist = requestWssConfidentiality.getXEncAlgorithmList();

            if (xencAlgorithmlist == null) {
                xencAlgorithmlist = new ArrayList<String>();
                xencAlgorithmlist.add(requestWssConfidentiality.getXEncAlgorithm());
            }

            aes128CheckBox.setSelected(xencAlgorithmlist.contains(XencAlgorithm.AES_128_CBC.getXEncName()));
            aes192CheckBox.setSelected(xencAlgorithmlist.contains(XencAlgorithm.AES_192_CBC.getXEncName()));
            aes256CheckBox.setSelected(xencAlgorithmlist.contains(XencAlgorithm.AES_256_CBC.getXEncName()));
            tripleDESCheckBox.setSelected(xencAlgorithmlist.contains(XencAlgorithm.TRIPLE_DES_CBC.getXEncName()));
            aes128GcmCheckBox.setSelected(xencAlgorithmlist.contains(XencAlgorithm.AES_128_GCM.getXEncName()));
            aes256GcmCheckBox.setSelected(xencAlgorithmlist.contains(XencAlgorithm.AES_256_GCM.getXEncName()));

            boolean contentsOnly = requestWssConfidentiality.isEncryptContentsOnly();
            encryptElementContentsOnlyRadioButton.setSelected(contentsOnly);
            encryptEntireElementIncludingRadioButton.setSelected(!contentsOnly);
        }
    }

    /**
     * Collect encryption configuration.
     * @return true if there are no invalid configurations.
     */
    private boolean collectEncryptionConfig() {
        if (!isEncryption) {
            return true;
        }
        if (assertion instanceof WssEncryptElement) {
            WssEncryptElement wssEncryptElement = (WssEncryptElement)assertion;
            wssEncryptElement.setXEncAlgorithm((String)encryptionMethodComboBox.getSelectedItem());
            wssEncryptElement.setEncryptContentsOnly(encryptElementContentsOnlyRadioButton.isSelected());
        } else if (assertion instanceof RequireWssEncryptedElement) {
            List<String> xencAlgorithmList = new ArrayList<String>();
            if (aes128GcmCheckBox.isSelected())
                xencAlgorithmList.add(XencAlgorithm.AES_128_GCM.getXEncName());
            if (aes256GcmCheckBox.isSelected())
                xencAlgorithmList.add(XencAlgorithm.AES_256_GCM.getXEncName());
            if (aes128CheckBox.isSelected()) {
                xencAlgorithmList.add(XencAlgorithm.AES_128_CBC.getXEncName());
            }
            if (aes192CheckBox.isSelected()) {
                xencAlgorithmList.add(XencAlgorithm.AES_192_CBC.getXEncName());
            }
            if (aes256CheckBox.isSelected()) {
                xencAlgorithmList.add(XencAlgorithm.AES_256_CBC.getXEncName());
            }
            if (tripleDESCheckBox.isSelected()) {
                xencAlgorithmList.add(XencAlgorithm.TRIPLE_DES_CBC.getXEncName());
            }

            // Check if there are invalid configurations.
            if (xencAlgorithmList.isEmpty()) {
                DialogDisplayer.showMessageDialog(TopComponents.getInstance().getTopParent(), null,
                                "Selection of an encryption method is required. \n" +
                                "Please select an encryption method.", null);
                return false;
            }

            RequireWssEncryptedElement requestWssConfidentiality = (RequireWssEncryptedElement)assertion;
            requestWssConfidentiality.setXEncAlgorithmList(xencAlgorithmList);
            requestWssConfidentiality.setEncryptContentsOnly(encryptElementContentsOnlyRadioButton.isSelected());
        }
        return true;
    }

    private String[] collectAcceptedDigests() {
        List<String> result = new ArrayList<String>();
        for(JCheckBox maybeAccepted : acceptedDigestCheckboxes) {
            if (maybeAccepted.isSelected())
                result.add(SupportedDigestMethods.fromAlias(maybeAccepted.getText()).getIdentifier());
        }
        return result.toArray(new String[result.size()]);
    }

    private void collectResponseSignatureConfig() {
        if (!(assertion instanceof WssSignElement)) {
            return;
        }
        WssSignElement rwssi = (WssSignElement)assertion;
        rwssi.setProtectTokens(signedBstCheckBox.isSelected());
        if (bstReferenceRadioButton.isSelected()) {
            rwssi.setKeyReference(KeyReference.BST.getName());
        } else if (skiReferenceRadioButton.isSelected()) {
            rwssi.setKeyReference(KeyReference.SKI.getName());
        } else if (issuerSerialReferenceRadioButton.isSelected()) {
            rwssi.setKeyReference(KeyReference.ISSUER_SERIAL.getName());
        }
        if (0 == signatureDigestComboBox.getSelectedIndex()) {
            rwssi.setDigestAlgorithmName(null);
        } else {
            String digAlg = (String)signatureDigestComboBox.getSelectedItem();
            rwssi.setDigestAlgorithmName(digAlg);
        }

    }

    private void collectResponseEncryptionConfig() {
        if (!(assertion instanceof WssEncryptElement)) {
            return;
        }
        WssEncryptElement wee = (WssEncryptElement)assertion;
        if (encSkiReferenceRadioButton.isSelected()) {
            wee.setKeyReference(KeyReference.SKI.getName());
        } else if (encBstReferenceRadioButton.isSelected()) {
            wee.setKeyReference(KeyReference.BST.getName());
        } else if (encIssuerSerialReferenceRadioButton.isSelected()) {
            wee.setKeyReference(KeyReference.ISSUER_SERIAL.getName());
        } else if (encKeyNameReferenceRadioButton.isSelected()) {
            wee.setKeyReference(KeyReference.KEY_NAME.getName());
        }

    }

    /**
     * initialize the blank message tha is displayed on whole message
     * selection. The blank message is created from the first message,
     * without body nodes.
     */
    private void initializeBlankMessage(SoapMessageGenerator.Message soapMessage) throws IOException, SOAPException, SAXException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        soapMessage.getSOAPMessage().writeTo(bo);
        String s = bo.toString();
        org.w3c.dom.Document document = XmlUtil.stringToDocument(s);
        Element body;
        try {
            body = SoapUtil.getBodyElement(document);
        } catch ( InvalidDocumentFormatException e) {
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
        messageViewerToolBar = new XpathToolBar(messageViewer, new Functions.Nullary<Map<String, String>>() {
            @Override
            public Map<String, String> call() {
                return getNamespacesWithOperationNamespaces();
            }
        });
        messageViewerToolBar.setXpathBuiltListener(new Functions.UnaryVoid<String>() {
            @Override
            public void call(String newExpression) {
                // A node has been clicked on to generate a sample XPath.
                // We'll take this as permission to commit any pending automatically-collected namespace declarations.
                // If possible, we'll try to add only the new declarations that ended up being used by the generated XPath.
                Map<String,String> toAdd = new HashMap<String,String>(allOperationNamespaces);
                try {
                    Set<String> usedPrefixes = XpathUtil.getNamespacePrefixesUsedByXpath(newExpression, getXpathVersion(), true);
                    toAdd.keySet().retainAll(usedPrefixes);
                } catch (ParseException e) {
                    // Fallthrough and just add them all, to be safe
                }
                namespaces.putAll(toAdd);
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
            log.log(Level.WARNING, "Unable to parse XML for message viewer: " + ExceptionUtils.getMessage(e), e);
            messageViewer.setDocument(XmlUtil.createEmptyDocument());
        }
    }

    private XpathVersion getXpathVersion() {
        return XpathVersion.fromVersionString((String) xpathVersionComboBox.getSelectedItem());
    }

    /**
     * @return whether we are editing the request
     */
    private boolean isEditingRequest() {
        return !Assertion.isResponse(assertion); // !isResponse so that message variable messages show requests
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
        @Override
        public void valueChanged(TreeSelectionEvent e) {
            TreePath path = e.getNewLeadSelectionPath();
            if (path == null) {
                populateSampleMessages(null, null);
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
                    populateSampleMessages(currentOperation.getName(), null);
                    SoapMessageGenerator.Message sreq = forOperation(boperation.getOperation());
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
                    populateSampleMessages(null, null);
                    displayMessage(blankMessage);
                    currentOperation = null;
                }
                messageViewerToolBar.setToolbarEnabled(true);
                xpathFieldPauseListener.textEntryResumed(xpf);
            }
        }
    };

    /**
     * Find prefixes of namespace declarations which do not appear to be visibly used by the current xpath expression.
     *
     * @param namespaces the namespace map to prune.
     * @return a set of prefixes that do not appear to be in use in the current expression.  Never null.  May be empty
     *         if the expression is not currently parseable.
     */
    private Set<String> findUnusedDeclarations(Map<String, String> namespaces) {
        JTextField xpathTextField = messageViewerToolBar.getxpathField();
        String xpath = xpathTextField.getText();

        try {
            Set<String> used = XpathUtil.getNamespacePrefixesUsedByXpath(xpath, getXpathVersion(), true);
            Set<String> declared = new HashSet<String>(namespaces.keySet());
            declared.removeAll(used);
            return declared;

        } catch (ParseException e) {
            log.log(Level.INFO, "Unable to parse expression: " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
            return Collections.emptySet();
        }
    }

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
            displayMessage( bos.toString() );
        } catch (SOAPException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * display the string soap message onto the viewer
     *
     * @throws RuntimeException wrapping the original exception
     */
    private void displayMessage(String soapMessage)
      throws RuntimeException {
        try {
            org.w3c.dom.Document doc;
            try {
                allOperationNamespaces.clear();
                doc = XmlUtil.stringToDocument(soapMessage);
                Map<String,String> docns = DomUtils.findAllNamespaces(doc.getDocumentElement());
                for ( final Map.Entry<String,String> prefixNSEntry : docns.entrySet() ) {
                    String prefix = prefixNSEntry.getKey();
                    final String uri = prefixNSEntry.getValue();
                    prefix = getPreferredPrefixForNsUri(uri, prefix);
                    if ( !namespaces.containsKey(prefix) || (!allOperationNamespaces.containsKey(prefix) && isSubstitutable(uri, namespaces.get(prefix)))) {
                        allOperationNamespaces.put(prefix, uri);
                    } else {
                        for ( int i=0; i<1000; i++ ) {
                            String generatedPrefix = prefix + i;
                            if ( !namespaces.containsKey(generatedPrefix) ) {
                                allOperationNamespaces.put(generatedPrefix, uri);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.log(Level.WARNING, "Couldn't get namespaces from non-XML document", e);
            }
            JTextField xpathTextField = messageViewerToolBar.getxpathField();
            xpathFieldPauseListener.textEntryPaused(xpathTextField, 0);
            setMessageViewerText(soapMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Check if it is OK to automatically substitute one NS URI for another if they both want to use the same prefix.
     *
     * @param uri the URI we want to add to the namespace map at its preferred prefix.
     * @param uri2  the URI of the namespace that is already present in the map under the prefix we want to use.
     * @return true iff. it is OK to just replace uri with uri2 instead of declaring a new entry.
     */
    private boolean isSubstitutable(String uri, String uri2) {
        Set<String> subs = SUBSTITUTABLE_NAMESPACES.get(uri);
        return subs != null && subs.contains(uri2);
    }

    private String getPreferredPrefixForNsUri(String uri, String origPrefix) {
        return SOAPConstants.URI_NS_SOAP_1_1_ENVELOPE.equals(uri) ||
               SOAPConstants.URI_NS_SOAP_1_2_ENVELOPE.equals(uri)
                        ? "s"
                        : origPrefix;
    }

    private SoapMessageGenerator.Message forOperation(BindingOperation bop) {
        String opName = bop.getOperation().getName();
        Binding binding = serviceWsdl.getBinding(bop);
        if (binding == null) {
            throw new IllegalArgumentException("Bindiong operation without binding " + opName);
        }
        String bindingName = binding.getQName().getLocalPart();

        for( SoapMessageGenerator.Message soapRequest : soapMessages ) {
            if( opName.equals( soapRequest.getOperation() ) &&
                    bindingName.equals( soapRequest.getBinding() ) ) {
                return soapRequest;
            }
        }
        return null;
    }


    final PauseListener xpathFieldPauseListener = new PauseListenerAdapter() {
        @Override
        public void textEntryPaused(JTextComponent component, long msecs) {
            updateXpathFeedback((JTextField)component);
        }
    };

    private void updateXpathFeedback(JTextField xpathField) {
        XpathFeedBack feedBack = getFeedBackMessage(getNamespacesWithOperationNamespaces(), xpathField);
        processFeedBack(feedBack, xpathField);
    }

    private void updateHardwareAccelSpeedIndicator(XpathFeedBack hardwareFeedBack) {
        if (!showHardwareAccelStatus) {
            speedIndicator.setVisible(false);
            return;
        }

        if (!haveTarari) {
            speedIndicator.setSpeed(SpeedIndicator.SPEED_FAST);
            String n = hardwareFeedBack == null ? "" : " be too complex to";
            speedIndicator.setToolTipText("Accelerated XPath not present on Gateway, but if it were, this expression would" + n + " be accelerated at full speed");
            return;
        }

        if (hardwareFeedBack == null) {
            speedIndicator.setSpeed(SpeedIndicator.SPEED_FASTEST);
            speedIndicator.setToolTipText("Expression will be accelerated at full speed");
        } else {
            speedIndicator.setSpeed(SpeedIndicator.SPEED_FASTER);
            speedIndicator.setToolTipText("Expression will be accelerated, but is too complex to run at full speed");
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
            updateHardwareAccelSpeedIndicator(feedBack.hardwareAccelFeedback);
            return;
        }

        updateHardwareAccelSpeedIndicator(feedBack.hardwareAccelFeedback);
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

    private Map<String, String> getNamespacesWithOperationNamespaces() {
        Map<String, String> namespacesWithAuto = new HashMap<String, String>(namespaces);
        namespacesWithAuto.putAll(allOperationNamespaces);
        return namespacesWithAuto;
    }

    private XpathFeedBack getFeedBackMessage(Map nsMap, JTextField xpathField) {
        String xpath = xpathField.getText();
        if (xpath == null) return new XpathFeedBack(-1, null, XpathFeedBack.EMPTY_MSG, XpathFeedBack.EMPTY_MSG);
        xpath = xpath.trim();
        if (xpath.length() < 1) return new XpathFeedBack(-1, null, XpathFeedBack.EMPTY_MSG, XpathFeedBack.EMPTY_MSG);
        if (isEncryption && (xpath.equals("/soapenv:Envelope") || xpath.equals("/s:Envelope"))) {
            return new XpathFeedBack(-1, xpath, "The path " + xpath + " is not valid for XML encryption", null);
        }
        try {
            final Set<String> variables = SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet();
            final XpathVersion xpathVersion = getXpathVersion();
            XpathUtil.validate( xpath, xpathVersion, namespaces );
            final List<String> varsUsed = XpathUtil.getUnprefixedVariablesUsedInXpath(xpath, xpathVersion);
            //test the expression if it does not use any context vars.
            if(varsUsed.isEmpty()){
                XpathUtil.testXpathExpression(testEvaluator, xpath, xpathVersion, namespaces, buildXpathVariableFinder(variables));
            }
            XpathFeedBack feedback = new XpathFeedBack(-1, xpath, null, null);
            feedback.hardwareAccelFeedback = getHardwareAccelFeedBack(nsMap, xpath);
            return feedback;
        } catch (InvalidXpathException e) {
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

            log.log(Level.FINE, e.getMessage(), e);
            return new XpathFeedBack(-1, xpath, ExceptionUtils.getMessage( e ), ExceptionUtils.getMessage(e));
        } catch (RuntimeException e) { // sometimes NPE, sometimes NFE
            log.log(Level.WARNING, e.getMessage(), e);
            return new XpathFeedBack(-1, xpath, "XPath expression error '" + xpath + "'", null);
        }
    }

    /**
     * @return feedback for hardware accel problems, or null if no hardware accel problems detected.
     */
    private XpathFeedBack getHardwareAccelFeedBack(Map nsMap, String xpath) {
        if (XpathVersion.XPATH_2_0.equals(getXpathVersion())) {
            return new XpathFeedBack(-1, xpath, "Parallel XPath processing not available for XPath 2.0", null);
        }

        XpathFeedBack hardwareFeedback;
        // Check if hardware accel is known not to work with this xpath
        String convertedXpath = xpath;
        try {
            final Set<String> variables = SsmPolicyVariableUtils.getVariablesSetByPredecessors(assertion).keySet();
            FastXpath fastXpath = TarariXpathConverter.convertToFastXpath(nsMap, xpath);
            convertedXpath = fastXpath.getExpression();
            XpathUtil.testXpathExpression(testEvaluator, convertedXpath, XpathVersion.XPATH_1_0, namespaces, buildXpathVariableFinder(variables));
            hardwareFeedback = null;
        } catch (ParseException e) {
            return new XpathFeedBack(e.getErrorOffset(), convertedXpath, e.getMessage(), e.getMessage());
        } catch (InvalidXpathException e) {
            @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
            XPathSyntaxException pe = ExceptionUtils.getCauseIfCausedBy(e, XPathSyntaxException.class);
            if (pe != null) {
                return new XpathFeedBack(pe.getPosition(), convertedXpath, pe.getMessage(), pe.getMessage());
            } else {
                hardwareFeedback = new XpathFeedBack(-1, convertedXpath, "XPath expression error '" + convertedXpath + "'", null);
            }
        } catch (RuntimeException e) { // sometimes NPE, sometimes NFE
            hardwareFeedback = new XpathFeedBack(-1, convertedXpath, "XPath expression error '" + convertedXpath + "'", null);
        }
        return hardwareFeedback;
    }

    private XpathVariableFinder buildXpathVariableFinder( final Set<String> variables ) {
        return new XpathVariableFinder(){
            @Override
            public Object getVariableValue( final String namespaceUri,
                                            final String variableName ) throws NoSuchXpathVariableException {
                if ( namespaceUri != null && namespaceUri.length() > 0 )
                    throw new NoSuchXpathVariableException("Unsupported XPath variable namespace '"+namespaceUri+"'.");

                if ( !variables.contains(variableName) )
                    throw new NoSuchXpathVariableException("Unsupported XPath variable name '"+variableName+"'.");

                return "";
            }
        };
    }

    private static class XpathFeedBack {
        private static final String EMPTY_MSG = "Empty XPath expression";
        int errorPosition = -1;
        String shortMessage = null;
        String detailedMessage = null;
        String xpathExpression = null;
        XpathFeedBack hardwareAccelFeedback = null;

        private XpathFeedBack(int errorPosition, String expression, String sm, String lm) {
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

    private static class MsgSrcComboBoxItem {
        private final String _variableName;
        private final String _displayName;
        private MsgSrcComboBoxItem(String variableName, String displayName) {
            _variableName = variableName;
            _displayName = displayName;
        }
        public String getVariableName() { return _variableName; }
        @Override
        public String toString() { return _displayName; }
    }

    public static class XpathToolBar extends JToolBar {
        private JTextField xpathField = null;
        private Functions.UnaryVoid<String> xpathBuiltListener = null;

        public XpathToolBar(final XmlViewer v, final Functions.Nullary<Map<String, String>> namespaceMapFactory) {
            setFloatable(false);

            xpathField = new SquigglyTextField();

            JLabel xpathLabel = new JLabel("XPath:");
            xpathLabel.setForeground(new Color(102, 102, 102));

            JPanel xpathPanel = new JPanel(new BorderLayout(5, 0));
            xpathPanel.setBorder(new EmptyBorder(2, 5, 2, 2));
            JPanel xpanel = new JPanel(new BorderLayout());
            xpanel.setBorder(new EmptyBorder(1, 0, 1, 0));

            xpathField.setFont(xpathField.getFont().deriveFont(Font.PLAIN, 12));
            xpathField.setPreferredSize(new Dimension(100, 19));
            xpathField.setEditable(true);

            xpanel.add(xpathField, BorderLayout.CENTER);
            xpathPanel.add(xpathLabel, BorderLayout.WEST);
            xpathPanel.add(xpanel, BorderLayout.CENTER);

            add(xpathPanel, BorderLayout.CENTER);

            v.setSelectionListener(new XmlViewer.NodeSelectionListener() {
                @Override
                public void nodeSelected(@Nullable Node node) {
                    if (node != null) {
                        StringBuilder builder = new StringBuilder();
                        buildPath(builder, namespaceMapFactory == null ? new HashMap<String, String>() : namespaceMapFactory.call(), node);
                        xpathField.setText(builder.toString());
                        if (xpathBuiltListener != null)
                            xpathBuiltListener.call(xpathField.getText());
                    }
                }
            });
        }

        public Functions.UnaryVoid<String> getXpathBuiltListener() {
            return xpathBuiltListener;
        }

        /**
         * Set a single listener that will be notified when a new xpath expresion is built.
         *
         * @param xpathBuiltListener the new listener, or null.
         */
        public void setXpathBuiltListener(Functions.UnaryVoid<String> xpathBuiltListener) {
            this.xpathBuiltListener = xpathBuiltListener;
        }

        /**
         * Access the xpath combo box component. This accesses
         *
         * @return the xpath combo box
         */
        public JTextField getxpathField() {
            return xpathField;
        }

        /**
         * Sets whether or not this component controls are enabled.
         *
         * @param enabled true if this component controls should be enabled, false otherwise
         */
        public void setToolbarEnabled(boolean enabled) {
            xpathField.setEnabled(enabled);
        }

        private void buildPath( final StringBuilder builder,
                                final Map<String, String> namespaces,
                                final Node element ) {

            if ( element.getParentNode() != null ) {
                buildPath( builder, namespaces, element.getParentNode() );
            }
            // currently we only support building a path to an element
            // but in the future we could support element values as well
            if (element.getNodeType() == Node.ELEMENT_NODE) {
                builder.append( '/' );

                String prefix = element.getPrefix();
                String namespace = element.getNamespaceURI();

                String nodeName = element.getLocalName();
                String nodeQname = element.getNodeName();

                if ( namespace == null ) {
                    builder.append( nodeName );
                } else if ( prefix == null || !namespace.equals(namespaces.get(prefix)) ) {
                    if ( namespaces.containsValue( namespace )) {
                        builder.append( findPrefix(namespaces, namespace) );
                        builder.append( ':' );
                        builder.append( nodeName );
                    } else {
                        builder.append( "*[local-name()='" );
                        builder.append( nodeName ); // name cannot contain "'"
                        builder.append( "' and namespace-uri()=" );
                        builder.append( XpathUtil.literalExpression(namespace) );
                        builder.append( ']' );
                    }
                } else {
                    builder.append( nodeQname );
                }
            }
        }


        private String findPrefix( final Map<String,String> namespaces, final String namespace ) {
            String prefix = null;

            for ( final Map.Entry<String,String> entry : namespaces.entrySet() ) {
                if ( namespace.equals(entry.getValue()) ) {
                    prefix = entry.getKey();
                    break;
                }
            }

            return prefix;
        }
    }
}
