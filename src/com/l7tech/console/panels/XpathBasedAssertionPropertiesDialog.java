package com.l7tech.console.panels;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.xml.SoapMessageGenerator.Message;
import com.l7tech.common.xml.*;
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
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.xmlsec.RequestWssConfidentiality;
import com.l7tech.policy.assertion.xmlsec.ResponseWssConfidentiality;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.jaxen.JaxenException;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;


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
    private XpathBasedAssertionTreeNode node;
    private XpathBasedAssertion xmlSecAssertion;
    private ServiceNode serviceNode;
    private Wsdl serviceWsdl;
    private JScrollPane treeScrollPane;
    private Message[] soapMessages;
    private String blankMessage = "<empty />";
    private Map namespaces = new HashMap();
    private Viewer messageViewer;
    private ViewerToolBar messageViewerToolBar;
    private ExchangerDocument exchangerDocument;
    private ActionListener okActionListener;
    private boolean isEncryption;
    private XpathEvaluator testEvaluator;

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
        node = n;
        StringBuffer title = new StringBuffer("Select element to ");
        if ((n instanceof RequestWssIntegrityTreeNode) || (n instanceof ResponseWssIntegrityTreeNode)) {
            title.append("sign.");
        } else if ((n instanceof RequestWssConfidentialityTreeNode) || (n instanceof ResponseWssConfidentialityTreeNode)) {
            title.append("encrypt.");
        } else if ((n instanceof RequestXpathPolicyTreeNode)) {
            title.append("require.");
        } else if ((n instanceof ResponseXpathPolicyTreeNode)) {
            title.append("require.");
        } else throw new IllegalArgumentException("Unsupported security node: " + n.getClass());
        setTitle(title.toString());

        okActionListener = okListener;

        xmlSecAssertion = (XpathBasedAssertion)node.asAssertion();
        if (xmlSecAssertion instanceof RequestWssConfidentiality ||
            xmlSecAssertion instanceof ResponseWssConfidentiality) {
            isEncryption = true;
        } else isEncryption = false;
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
                Message soapRequest = soapMessages[i];
                namespaces.putAll(XpathEvaluator.getNamespaces(soapRequest.getSOAPMessage()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse the service WSDL " + serviceNode.getName(), e);
        }

        initialize();

        // display the existing xpath expression
        messageViewerToolBar.getxpathField().setText(xmlSecAssertion.getXpathExpression().getExpression());

        // ok button is disabled until a change is made
        okButton.setEnabled(false);

        // initialize the test evaluator
        try {
            testEvaluator = XpathEvaluator.newEvaluator(XmlUtil.stringToDocument("<blah xmlns=\"http://bzzt.com\"/>"),
                                                        new HashMap());
        } catch (IOException e) {
            log.log(Level.WARNING, "cannot setup test evaluator", e);
        } catch (SAXException e) {
            log.log(Level.WARNING, "cannot setup test evaluator", e);
        }
    }

    private boolean isValidValue(String xpath) {
        if (xpath == null) return false;
        xpath = xpath.trim();
        if (xpath.length() < 1) return false;
        if (isEncryption && xpath.equals("/soapenv:Envelope")) return false;
        try {
            testEvaluator.evaluate(xpath);
        } catch (JaxenException e) {
            log.fine(e.getMessage());
            return false;
        } catch (NullPointerException e) {
            log.fine(e.getMessage());
            return false;
        }
        return true;
    }


    private void initialize() {
        Actions.setEscKeyStrokeDisposes(this);
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                XpathBasedAssertionPropertiesDialog.this.dispose();
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                // get xpath from control and the namespace map
                // then save it in assertion
                String xpath = (String)messageViewerToolBar.getxpathField().getText();
                if (isEncryption && xpath.equals("/soapenv:Envelope")) {
                    // dont allow encryption of entire envelope
                    JOptionPane.showMessageDialog(okButton, "The path " + xpath + " is not valid for XML encryption",
                                                  "XPath Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    xmlSecAssertion.setXpathExpression(new XpathExpression(xpath, namespaces));
                    XpathBasedAssertionPropertiesDialog.this.dispose();
                }
            }
        });
        if (okActionListener != null) {
            okButton.addActionListener(okActionListener);
        }
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

        messageViewerToolBar.getxpathField().addKeyListener(messageEditingListener);
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
        messageViewer.addDocumentTreeSelectionListener(messageSelectionListener);
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
                  BindingOperationTreeNode boperation = (BindingOperationTreeNode)lpc;
                  Message sreq = forOperation(boperation.getOperation());
                  messageViewerToolBar.setToolbarEnabled(true);
                  if (sreq == null) {
                  } else {
                      try {
                          SOAPMessage soapMessage = sreq.getSOAPMessage();
                          displayMessage(soapMessage);
                          if (e.getSource() == operationsTree.getSelectionModel()) {
                              messageViewerToolBar.getxpathField().setText("");
                          }
                      } catch (Exception e1) {
                          throw new RuntimeException(e1);
                      }

                  }
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

    private final TreeSelectionListener
      messageSelectionListener = new TreeSelectionListener() {
          public void valueChanged(TreeSelectionEvent e) {
              okButton.setEnabled(isValidValue(messageViewerToolBar.getxpathField().getText()));
          }
      };

    private final KeyListener
      messageEditingListener = new KeyListener() {
          public void keyTyped(KeyEvent e) {}
          public void keyPressed(KeyEvent e) {}
          public void keyReleased(KeyEvent e) {
              okButton.setEnabled(isValidValue(messageViewerToolBar.getxpathField().getText()));
          }
      };

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

}
