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
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.*;
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
    private JTextField xPathExpressionTxtField;
    private JTree operationsTree;
    private JButton okButton;
    private JButton cancelButton;
    private JButton helpButton;
    private XpathBasedAssertionTreeNode node;
    private XpathBasedAssertion xmlSecAssertion;
    private ServiceNode serviceNode;
    private Wsdl serviceWsdl;
    private JScrollPane tableScrollPane;
    private JScrollPane treeScrollPane;
    private Message[] soapMessages;
    private String blankMessage = "<empty />";
    private Map namespaces = new HashMap();
    private Viewer messageViewer;
    private ViewerToolBar messageViewerToolBar;
    private ExchangerDocument exchangerDocument;
    private ActionListener okActionListener;

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
        } else throw new IllegalArgumentException("Unsupported security node: " + n.getClass());
        setTitle(title.toString());

        okActionListener = okListener;

        xmlSecAssertion = (XpathBasedAssertion)node.asAssertion();
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
        xPathExpressionTxtField.setText(xmlSecAssertion.getXpathExpression().getExpression());

        // ok button is disabled until a change is made
        okButton.setEnabled(false);
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
                String xpath = xPathExpressionTxtField.getText();
                xmlSecAssertion.setXpathExpression(new XpathExpression(xpath, namespaces));
                XpathBasedAssertionPropertiesDialog.this.dispose();
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
     * Select the operation by name
     *
     * @param name the operation name
     */
    private void selectOperation(String name) {
        final DefaultTreeModel operationsTreeModel = (DefaultTreeModel)operationsTree.getModel();
        DefaultMutableTreeNode wsdlTreeNode = (DefaultMutableTreeNode)operationsTreeModel.getRoot();
        Enumeration enum = wsdlTreeNode.preorderEnumeration();
        while (enum.hasMoreElements()) {
            DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode)enum.nextElement();

            if (treeNode instanceof BindingOperationTreeNode) {
                BindingOperation bo = ((BindingOperationTreeNode)treeNode).getOperation();
                if (bo.getName().equals(name)) {
                    log.finest("Operation matched, selecting " + bo.getName());
                    operationsTree.setSelectionPath(new TreePath(treeNode.getPath()));
                    break;
                }
            }
        }
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
        messageViewerToolBar = new ViewerToolBar(cp.getViewer(),
                                                 messageViewer,
                                                 new ViewerToolBar.XPathSelectFeedback() {
                                                    public void selected(String xpathSelected) {
                                                        xPathExpressionTxtField.setText(xpathSelected);
                                                        okButton.setEnabled(true);
                                                    }
                                                 });
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
                  //addButton.setEnabled(false);
              } else {
                  final Object lpc = path.getLastPathComponent();
                  if (!((lpc instanceof BindingOperationTreeNode) || (lpc instanceof BindingTreeNode))) {
                      messageViewerToolBar.setToolbarEnabled(false);
                      //addButton.setEnabled(false);
                      return;
                  }
                  if (lpc instanceof BindingTreeNode) {
                      messageViewerToolBar.setToolbarEnabled(false);
                      //addButton.setEnabled(false);
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
                      //addButton.setEnabled(false);
                  } else {
                      // addButton.setEnabled(true);
                      try {
                          SOAPMessage soapMessage = sreq.getSOAPMessage();
                          displayMessage(soapMessage);
                          if (e.getSource() == operationsTree.getSelectionModel()) {
                              messageViewerToolBar.getXpathComboBox().getEditor().setItem("");
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
              Object o = messageViewerToolBar.getXpathComboBox().getEditor().getItem();
              boolean validXpath = true;
              if (o == null)
                  validXpath = false;
              else if ("".equals(o.toString())) validXpath = false;

              okButton.setEnabled(e.getNewLeadSelectionPath() != null && validXpath);
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

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// >>> IMPORTANT!! <<<
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * >>> IMPORTANT!! <<<
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        final JPanel _1;
        _1 = new JPanel();
        mainPanel = _1;
        _1.setLayout(new GridLayoutManager(5, 1, new Insets(5, 10, 0, 10), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        _1.add(_2, new GridConstraints(3, 0, 1, 1, 0, 3, 7, 7, null, null, null));
        final JSplitPane _3;
        _3 = new JSplitPane();
        _3.setDividerSize(4);
        _2.add(_3, new GridConstraints(0, 0, 1, 1, 0, 3, 7, 3, null, new Dimension(200, 200), null));
        final JPanel _4;
        _4 = new JPanel();
        _4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        _3.setLeftComponent(_4);
        final JLabel _5;
        _5 = new JLabel();
        operationsLabel = _5;
        _5.setHorizontalTextPosition(0);
        _5.setText("Web Service Operations");
        _4.add(_5, new GridConstraints(0, 0, 1, 1, 0, 3, 7, 0, null, null, null));
        final JScrollPane _6;
        _6 = new JScrollPane();
        treeScrollPane = _6;
        _6.setAutoscrolls(false);
        _4.add(_6, new GridConstraints(1, 0, 1, 1, 8, 3, 7, 7, null, null, null));
        final JTree _7;
        _7 = new JTree();
        operationsTree = _7;
        _7.setShowsRootHandles(false);
        _7.setRootVisible(false);
        _6.setViewportView(_7);
        final JPanel _8;
        _8 = new JPanel();
        _8.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        _3.setRightComponent(_8);
        final JPanel _9;
        _9 = new JPanel();
        messageViewerToolbarPanel = _9;
        _9.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        _8.add(_9, new GridConstraints(0, 0, 1, 1, 8, 1, 7, 1, null, null, null));
        final JScrollPane _10;
        _10 = new JScrollPane();
        _8.add(_10, new GridConstraints(1, 0, 1, 1, 0, 3, 7, 7, null, null, null));
        final JPanel _11;
        _11 = new JPanel();
        messageViewerPanel = _11;
        _11.setLayout(new GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        _10.setViewportView(_11);
        final JPanel _12;
        _12 = new JPanel();
        _12.setLayout(new GridLayoutManager(3, 7, new Insets(5, 0, 5, 0), -1, -1));
        _1.add(_12, new GridConstraints(4, 0, 1, 1, 0, 3, 7, 3, null, null, null));
        final JButton _13;
        _13 = new JButton();
        okButton = _13;
        _13.setText("OK");
        _12.add(_13, new GridConstraints(2, 4, 1, 1, 0, 1, 3, 0, null, null, null));
        final Spacer _14;
        _14 = new Spacer();
        _12.add(_14, new GridConstraints(2, 3, 1, 1, 0, 1, 6, 1, null, null, null));
        final JButton _15;
        _15 = new JButton();
        helpButton = _15;
        _15.setText("Help");
        _12.add(_15, new GridConstraints(2, 6, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _16;
        _16 = new JButton();
        cancelButton = _16;
        _16.setText("Cancel");
        _12.add(_16, new GridConstraints(2, 5, 1, 1, 0, 1, 3, 0, null, null, null));
        final Spacer _17;
        _17 = new Spacer();
        _12.add(_17, new GridConstraints(0, 3, 1, 1, 0, 2, 1, 6, null, null, null));
        final JTextField _18;
        _18 = new JTextField();
        xPathExpressionTxtField = _18;
        _18.setToolTipText("The Xpath to save in the assertion");
        _18.setText("blahblahs");
        _18.setFocusable(true);
        _18.setEditable(false);
        _18.setFocusCycleRoot(false);
        _12.add(_18, new GridConstraints(1, 1, 1, 6, 8, 1, 6, 0, null, new Dimension(150, 25), null));
    }
}
