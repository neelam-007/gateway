package com.l7tech.console.panels;

import com.l7tech.common.gui.util.PauseListener;
import com.l7tech.common.gui.util.TextComponentPauseListenerManager;
import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.widgets.SquigglyTextField;
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
import com.l7tech.console.xmlviewer.ExchangerDocument;
import com.l7tech.console.xmlviewer.Viewer;
import com.l7tech.console.xmlviewer.ViewerToolBar;
import com.l7tech.console.xmlviewer.properties.ConfigurationProperties;
import com.l7tech.console.xmlviewer.util.DocumentUtilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.RequestXpathAssertion;
import com.l7tech.policy.assertion.ResponseXpathAssertion;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.policy.assertion.xmlsec.RequestWssIntegrity;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import com.l7tech.policy.assertion.xmlsec.ResponseWssIntegrity;
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
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
    private XpathBasedAssertion xmlSecAssertion;
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

    /**
     * @param owner this panel owner
     * @param modal is this modal dialog or not
     * @param sn    the ServiceNode
     * @param n     the xml security node
     */
    public XpathBasedAssertionPropertiesDialog(JFrame owner, boolean modal, ServiceNode sn, XpathBasedAssertionTreeNode n, ActionListener okListener) {
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
    public XpathBasedAssertionPropertiesDialog(JFrame owner, boolean modal, XpathBasedAssertionTreeNode n, ActionListener okListener) {
        super(owner, modal);
        if (n == null) {
            throw new IllegalArgumentException();
        }
        construct(null, n, okListener);
    }

    private void construct(ServiceNode sn, XpathBasedAssertionTreeNode n, ActionListener okListener) {
        node = n;
        okActionListener = okListener;

        xmlSecAssertion = (XpathBasedAssertion)node.asAssertion();
        if (xmlSecAssertion.getXpathExpression() != null) {
            namespaces = xmlSecAssertion.getXpathExpression().getNamespaces();
        } else {
            namespaces = new HashMap();
        }
        if (xmlSecAssertion instanceof RequestWssConfidentiality ||
          xmlSecAssertion instanceof ResponseWssConfidentiality) {
            isEncryption = true;
        } else
            isEncryption = false;
        if (sn != null)
            serviceNode = sn;
        else
            serviceNode = AssertionTreeNode.getServiceNode(node);
        if (serviceNode == null) {
            throw new IllegalStateException("Unable to determine the service node for " + xmlSecAssertion);
        }
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
        if (xmlSecAssertion.getXpathExpression() != null) {
            initialvalue = xmlSecAssertion.getXpathExpression().getExpression();
        }
        messageViewerToolBar.getxpathField().setText(initialvalue);
    }


    private void initialize() {
        Actions.setEscKeyStrokeDisposes(this);

        namespaceButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                java.util.List requiredNS = new ArrayList(requiredNamespaces.values());
                NamespaceMapEditor nseditor = new NamespaceMapEditor(XpathBasedAssertionPropertiesDialog.this,
                                                                     namespaces,
                                                                     requiredNS);
                nseditor.pack();
                Utilities.centerOnScreen(nseditor);
                nseditor.show();
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
                    xmlSecAssertion.setXpathExpression(null);
                } else {
                    xmlSecAssertion.setXpathExpression(new XpathExpression(xpath, namespaces));
                }
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
        if (xmlSecAssertion instanceof RequestWssConfidentiality) {
            description = "Select a request element to encrypt:";
            title = "Encrypt Request Element Properties";
        } else if (xmlSecAssertion instanceof ResponseWssConfidentiality) {
            description = "Select a response element to encrypt:";
            title = "Encrypt Response Element Properties";
        } else if (xmlSecAssertion instanceof RequestWssIntegrity) {
            description = "Select a request element to sign:";
            title = "Sign Request Element Properties";
        } else if (xmlSecAssertion instanceof ResponseWssIntegrity) {
            description = "Select a response element to sign:";
            title = "Sign Response Element Properties";
        } else if (xmlSecAssertion instanceof ResponseXpathAssertion) {
            description = "Select the response path to evaluate:";
            title = "Evaluate Response XPath Properties";
        } else if (xmlSecAssertion instanceof RequestXpathAssertion) {
            description = "Select the request path to evaluate:";
            title = "Evaluate Request XPath Properties";
        }
        descriptionLabel.setText(description);
        setTitle(title);
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
        Element body = null;
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
            Node item = (Node)nl.item(i);
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
         * @return	the <code>Component</code> that the renderer uses to draw the value
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
                  Message sreq = forOperation(boperation.getOperation());
                  messageViewerToolBar.setToolbarEnabled(true);
                  if (sreq == null) {
                  } else {
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
                hardwareAccelStatusLabel.setText("(No hardware accel: " + hardwareFeedBack.getShortMessage() + ")");
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
                tooltip.append("<html><b>Position : " + feedBack.getErrorPosition() + ", ");
                htmlOpenAdded = true;
            }
            String msg = feedBack.getShortMessage();
            if (msg != null) {
                if (!htmlOpenAdded) {
                    msg = "<html><b>" + msg;
                }
                tooltip.append(msg + "</b></html>");
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

    /** @return feedback for hardware accel problems, or null if no hardware accel problems detected. */
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
