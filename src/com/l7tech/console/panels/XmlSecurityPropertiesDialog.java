package com.l7tech.console.panels;

import com.l7tech.common.util.SoapUtil;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.SoapMessageGenerator.Message;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.console.action.Actions;
import com.l7tech.console.table.SecuredMessagePartsTableModel;
import com.l7tech.console.table.SecuredMessagePartsTableModel.SecuredMessagePart;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.XmlRequestSecurityTreeNode;
import com.l7tech.console.tree.policy.XmlResponseSecurityTreeNode;
import com.l7tech.console.tree.policy.XmlSecurityTreeNode;
import com.l7tech.console.tree.wsdl.BindingOperationTreeNode;
import com.l7tech.console.tree.wsdl.BindingTreeNode;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.xmlviewer.ExchangerDocument;
import com.l7tech.console.xmlviewer.Viewer;
import com.l7tech.console.xmlviewer.ViewerToolBar;
import com.l7tech.console.xmlviewer.properties.ConfigurationProperties;
import com.l7tech.console.xmlviewer.util.DocumentUtilities;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.xmlsec.ElementSecurity;
import com.l7tech.policy.assertion.xmlsec.XmlSecurityAssertion;
import org.apache.xml.utils.NameSpace;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.xml.sax.SAXParseException;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.Output;
import javax.wsdl.WSDLException;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.rmi.RemoteException;
import java.text.MessageFormat;
import java.util.*;


/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class XmlSecurityPropertiesDialog extends JDialog {
    private JPanel mainPanel;
    private JPanel messageViewerPanel;
    private JPanel messageViewerToolbarPanel;
    private JLabel operationsLabel;
    private JRadioButton entireMessage;
    private JRadioButton messageParts;
    private JTable securedItemsTable;
    private JTree operationsTree;
    private JButton okButton;
    private JButton cancelButton;
    private JButton addButton;
    private JButton removeButton;
    private JButton helpButton;
    private XmlSecurityTreeNode node;
    private XmlSecurityAssertion xmlSecAssertion;
    private ServiceNode serviceNode;
    private Wsdl serviceWsdl;
    private JScrollPane tableScrollPane;
    private JScrollPane treeScrollPane;
    private JScrollPane messageViewerScrollPane;
    private SecuredMessagePartsTableModel securedMessagePartsTableModel;
    private SecuredMessagePartsTableModel memoTableModelEntireMessage;
    private SecuredMessagePartsTableModel memoTableModelMessageParts;
    private Message[] soapMessages;
    private Map namespaces = new HashMap();
    private Viewer messageViewer;
    private ViewerToolBar messageViewerToolBar;
    private ExchangerDocument exchangerDocument;

    /**
     * @param owner this panel owner
     * @param modal is this modal dialog or not
     * @param n     the xml security node
     */
    public XmlSecurityPropertiesDialog(JFrame owner, boolean modal, XmlSecurityTreeNode n) {
        super(owner, modal);
        if (n == null) {
            throw new IllegalArgumentException();
        }
        if (!(n instanceof XmlRequestSecurityTreeNode ||
          n instanceof XmlResponseSecurityTreeNode)) {
            throw new IllegalArgumentException("Unsupported security node: " + n.getClass());
        }
        node = n;
        setTitle("XML security properties");

        xmlSecAssertion = (XmlSecurityAssertion)node.asAssertion();
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
            for (int i = 0; i < soapMessages.length; i++) {
                Message soapRequest = soapMessages[i];
                namespaces.putAll(XpathEvaluator.getNamespaces(soapRequest.getSOAPMessage()));
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to parse the service WSDL " + serviceNode.getName(), e);
        }

        initialize();
    }

    private void initialize() {
        ButtonGroup bg = new ButtonGroup();
        bg.add(entireMessage);
        bg.add(messageParts);
        tableScrollPane.getViewport().setBackground(securedItemsTable.getBackground());

        entireMessage.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                boolean selected = entireMessage.isSelected();
                if (selected) {
                    operationsTree.setEnabled(!selected);
                    messageViewer.setViewerEnabled(!selected);
                    messageViewerToolBar.setToolbarEnabled(!selected);
                    securedMessagePartsTableModel = memoTableModelEntireMessage;
                    if (securedMessagePartsTableModel == null) {
                        securedMessagePartsTableModel = new SecuredMessagePartsTableModel();
                        SecuredMessagePart sp = new SecuredMessagePart();
                        sp.setOperation(null);
                        sp.setXpathExpression(SoapUtil.SOAP_ENVELOPE_XPATH);
                        securedMessagePartsTableModel.addPart(sp);
                    }
                    securedItemsTable.setModel(securedMessagePartsTableModel);
                } else {
                    memoTableModelEntireMessage = securedMessagePartsTableModel;
                }
            }
        });

        messageParts.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getSource() != messageParts) return;
                boolean selected = messageParts.isSelected();
                if (selected) {
                    operationsTree.setEnabled(selected);
                    messageViewer.setViewerEnabled(selected);
                    messageViewerToolBar.setToolbarEnabled(selected);
                    securedMessagePartsTableModel = memoTableModelMessageParts;
                    if (securedMessagePartsTableModel == null) {
                        securedMessagePartsTableModel = new SecuredMessagePartsTableModel();
                    }
                    securedItemsTable.setModel(securedMessagePartsTableModel);
                } else {
                    memoTableModelMessageParts = securedMessagePartsTableModel;
                }
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                XmlSecurityPropertiesDialog.this.dispose();
            }
        });

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {

                try {
                    java.util.List elements = new ArrayList();
                    Iterator it = securedMessagePartsTableModel.getSecuredMessageParts().iterator();
                    for (; it.hasNext();) {
                        SecuredMessagePart sp = (SecuredMessagePartsTableModel.SecuredMessagePart)it.next();
                        elements.add(toElementSecurity(sp));
                    }
                    xmlSecAssertion.setElements((ElementSecurity[])elements.toArray(new ElementSecurity[]{}));
                    XmlSecurityPropertiesDialog.this.dispose();
                } catch (SOAPException e1) {
                    throw new RuntimeException(e1);
                }
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TreePath path = operationsTree.getSelectionPath();
                if (path == null) {
                    throw new IllegalStateException("No message/part selected (path is null)");
                }
                WsdlTreeNode node = (WsdlTreeNode)path.getLastPathComponent();

                if (node instanceof BindingOperationTreeNode) {
                    BindingOperationTreeNode bn = (BindingOperationTreeNode)node;
                    BindingOperation bo = bn.getOperation();
                    String xpathExpression = SoapUtil.SOAP_BODY_XPATH;
                    xpathExpression = messageViewerToolBar.getXPath();
                    SecuredMessagePart p = new SecuredMessagePart();
                    p.setOperation(bo);
                    p.setXpathExpression(xpathExpression);
                    addSecuredPart(p);
                }
            }
        });

        removeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int row = securedItemsTable.getSelectedRow();
                SecuredMessagePart p = securedMessagePartsTableModel.getPartAt(row);
                securedMessagePartsTableModel.removePart(p);
            }
        });

        helpButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Actions.invokeHelp(XmlSecurityPropertiesDialog.this);
            }
        });
        try {
            final Wsdl wsdl = serviceNode.getPublishedService().parsedWsdl();
            final MutableTreeNode root = new DefaultMutableTreeNode();
            final DefaultTreeModel treeModel = new DefaultTreeModel(root);

            populateOperations(wsdl, treeModel, root);

            operationsTree.setModel(treeModel);
            operationsTree.setRootVisible(false);
            operationsTree.setShowsRootHandles(true);
            operationsTree.setCellRenderer(wsdlTreeRenderer);
            operationsTree.getSelectionModel().addTreeSelectionListener(treeSelectionListener);
            final ListSelectionModel selectionModel = securedItemsTable.getSelectionModel();
            selectionModel.addListSelectionListener(tableSelectionListener);
            selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            final ElementSecurity[] elements = xmlSecAssertion.getElements();
            final boolean envelopeAllOperations = isEnvelopeAllOperations(elements);
            initializeSoapMessageViewer();

            entireMessage.setSelected(envelopeAllOperations);
            messageParts.setSelected(!envelopeAllOperations);
            messageViewer.setViewerEnabled(!envelopeAllOperations);
            messageViewerToolBar.setToolbarEnabled(!envelopeAllOperations);

            for (int i = 0; !envelopeAllOperations && i < elements.length; i++) {
                ElementSecurity elementSecurity = elements[i];
                securedMessagePartsTableModel.addPart(toSecureMessagePart(elementSecurity));
            }
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

    private void initializeSoapMessageViewer() throws IOException, SAXParseException, DocumentException {
        ConfigurationProperties cp = new ConfigurationProperties();
        exchangerDocument = asExchangerDocument("<empty/>");
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

    private ElementSecurity toElementSecurity(SecuredMessagePart sp) throws SOAPException {
        ElementSecurity es = new ElementSecurity();
        es.setEncryption(sp.isEncrypt());
        es.setCipher(sp.getAlgorithm());
        es.setKeyLength(sp.getKeyLength());
        BindingOperation bn = sp.getOperation();
        if (bn != null) {
            es.setPreconditionXPath(xpathForOperation(bn));
        }

        XpathExpression xe = new XpathExpression(sp.getXpathExpression(), namespaces);
        es.setxPath(xe);
        return es;
    }

    private SecuredMessagePart toSecureMessagePart(ElementSecurity es) {
        SecuredMessagePart sp = new SecuredMessagePart();

        if (es.getPreconditionXPath() != null) {
            String expression = es.getPreconditionXPath().getExpression();
            String opname = null;
            int index = expression.lastIndexOf(":");
            if (index != -1) {
                opname = expression.substring(index + 1);
            }
            if (opname != null) {
            }
            for (int i = 0; i < soapMessages.length; i++) {
                Message soapRequest = soapMessages[i];
                if (isEditingRequest()) {
                    if (soapRequest.getOperation().equals(opname)) {
                        sp.setOperation(getBindingOperation(soapRequest));
                    }
                } else {
                    sp.setOperation(getBindingOperationByOutput(opname));
                }
            }
        }

        sp.setAlgorithm(es.getCipher());
        sp.setXpathExpression(es.getxPath().getExpression());
        sp.setEncrypt(es.isEncryption());
        sp.setKeyLength(es.getKeyLength());
        return sp;
    }

    private boolean isEnvelopeAllOperations(ElementSecurity[] es) {
        return (es.length == 1 && SoapUtil.SOAP_ENVELOPE_XPATH.equals(es[0].getxPath().getExpression()));
    }

    /**
     * @return whether we are editing the request
     */
    private boolean isEditingRequest() {
        return (node instanceof XmlRequestSecurityTreeNode);
    }

    private void addSecuredPart(SecuredMessagePart p) {
        java.util.List sparts = securedMessagePartsTableModel.getSecuredMessageParts();
        boolean alreadySelected = false;
        for (Iterator iterator = sparts.iterator(); iterator.hasNext();) {
            SecuredMessagePart securedMessagePart = (SecuredMessagePart)iterator.next();
            if (p.implies(securedMessagePart) || securedMessagePart.implies(p)) {
                alreadySelected = true;
            }
        }
        if (alreadySelected) {
            JFrame f = Registry.getDefault().getComponentRegistry().getMainWindow();
            final String msg = "<html><center>The element <i><b>{0}</b></i><br>" +
              "for operation <i><b>{1}</i></b> has already been included in previous selection.<br>" +
              "Overlapping elements in signatures and encryptions are currently not supported.</center></html>";
            final Object[] params = new Object[]{p.getXpathExpression() == null ? "" : p.getXpathExpression(),
                                                 p.getOperationName() == null ? "" : p.getOperationName()};
            JOptionPane.showMessageDialog(f, MessageFormat.format(msg, params),
              "Element already selected",
              JOptionPane.WARNING_MESSAGE);
            return;
        }
        securedMessagePartsTableModel.addPart(p);
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
      treeSelectionListener = new TreeSelectionListener() {
          public void valueChanged(TreeSelectionEvent e) {
              TreePath path = e.getNewLeadSelectionPath();
              if (path == null) {
                  addButton.setEnabled(false);
              } else {
                  final Object lpc = path.getLastPathComponent();
                  if (!((lpc instanceof BindingOperationTreeNode) || (lpc instanceof BindingTreeNode))) {
                      messageViewerToolBar.setToolbarEnabled(false);
                      addButton.setEnabled(false);
                      return;
                  }
                  if (lpc instanceof BindingTreeNode) {
                      messageViewerToolBar.setToolbarEnabled(false);
                      addButton.setEnabled(false);
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
                      addButton.setEnabled(false);
                  } else {
                      addButton.setEnabled(true);
                      try {
                          ByteArrayOutputStream bos = new ByteArrayOutputStream();
                          sreq.getSOAPMessage().writeTo(bos);
                          URL url = asTempFileURL(bos.toString());
                          exchangerDocument.setProperties(url, null);
                          exchangerDocument.load();
                      } catch (Exception e1) {
                          throw new RuntimeException(e1);
                      }

                  }
              }
          }
      };


    private final ListSelectionListener tableSelectionListener = new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
            final int selectedRow = securedItemsTable.getSelectedRow();
            if (selectedRow == -1) {
                removeButton.setEnabled(false);
                return;
            }
            String xe = (String)securedItemsTable.getModel().getValueAt(selectedRow, 1);

            messageViewerToolBar.getXpathComboBox().getEditor().setItem(xe);
            removeButton.setEnabled(true);
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

    private BindingOperation getBindingOperation(Message sreq) {
        String opName = sreq.getOperation();
        String bindingName = sreq.getBinding();

        Iterator it = serviceWsdl.getBindings().iterator();
        while (it.hasNext()) {
            Binding binding = (Binding)it.next();
            if (binding.getQName().getLocalPart().equals(bindingName)) {
                Iterator itop = binding.getBindingOperations().iterator();
                while (itop.hasNext()) {
                    BindingOperation bop = (BindingOperation)itop.next();
                    if (bop.getName().equals(opName)) {
                        return bop;
                    }
                }
            }
        }
        return null;
    }

    private BindingOperation getBindingOperationByOutput(String outputName) {
        Iterator it = serviceWsdl.getBindings().iterator();
        while (it.hasNext()) {
            Binding binding = (Binding)it.next();
            Iterator itop = binding.getBindingOperations().iterator();
            while (itop.hasNext()) {
                BindingOperation bop = (BindingOperation)itop.next();
                Output output = bop.getOperation().getOutput();
                if (output == null) continue;
                javax.wsdl.Message message = output.getMessage();
                if (outputName.equals(message.getQName().getLocalPart())) {
                    return bop;
                }
            }
        }
        return null;
    }

    private Binding getBinding(Message sreq) {
        Iterator it = serviceWsdl.getBindings().iterator();
        while (it.hasNext()) {
            Binding binding = (Binding)it.next();
            if (binding.getQName().getLocalPart().equals(sreq.getBinding())) {
                return binding;
            }
        }

        return null;
    }

    private NameSpace getOperationNamespace(BindingOperationTreeNode bnode) throws SOAPException {
        Message req = forOperation(bnode.getOperation());
        if (req == null) return null;
        Iterator it = req.getSOAPMessage().getSOAPPart().getEnvelope().getBody().getChildElements();
        if (!it.hasNext()) return null;
        SOAPElement se = (SOAPElement)it.next();
        final Name elementName = se.getElementName();
        if (!elementName.getLocalName().equals(bnode.getOperation().getName())) return null;
        NameSpace ns = new NameSpace(elementName.getPrefix(), elementName.getURI());
        return ns;
    }

    private XpathExpression xpathForOperation(BindingOperation bop) throws SOAPException {
        Message req = forOperation(bop);
        if (req == null) return null;
        Map namespaces = XpathEvaluator.getNamespaces(req.getSOAPMessage());
        XpathExpression xp = new XpathExpression();
        xp.setNamespaces(namespaces);
        Iterator it = req.getSOAPMessage().getSOAPPart().getEnvelope().getBody().getChildElements();
        if (!it.hasNext()) {
            xp.setExpression(SoapUtil.SOAP_BODY_XPATH);
        } else {
            SOAPElement se = (SOAPElement)it.next();
            final Name elementName = se.getElementName();

            String prefix = elementName.getPrefix();
            String localName = elementName.getLocalName();
            String xpathForName = localName;
            if (prefix != null) {
                xpathForName = prefix + ":" + localName;
            }
            xp.setExpression(SoapUtil.SOAP_BODY_XPATH + "/" + xpathForName);
        }
        return xp;
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
        _1.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(5, 1, new Insets(5, 10, 0, 10), -1, -1));
        final JPanel _2;
        _2 = new JPanel();
        _2.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(5, 0, 0, 0), -1, -1));
        _1.add(_2, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 1, 7, 1, null, null, null));
        final JRadioButton _3;
        _3 = new JRadioButton();
        entireMessage = _3;
        _3.setText("Sign entire message, all operations");
        _2.add(_3, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JRadioButton _4;
        _4 = new JRadioButton();
        messageParts = _4;
        _4.setText("Sign selected message elements and operations");
        _2.add(_4, new com.intellij.uiDesigner.core.GridConstraints(2, 0, 1, 1, 8, 0, 3, 0, null, null, null));
        final JLabel _5;
        _5 = new JLabel();
        _5.setText("Signature and ecryption scope");
        _2.add(_5, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _6;
        _6 = new com.intellij.uiDesigner.core.Spacer();
        _2.add(_6, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JPanel _7;
        _7 = new JPanel();
        _7.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(10, 0, 0, 0), -1, -1));
        _1.add(_7, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 3, 7, 7, null, null, null));
        final JSplitPane _8;
        _8 = new JSplitPane();
        _8.setDividerSize(4);
        _7.add(_8, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 7, 3, null, new Dimension(200, 200), null));
        final JPanel _9;
        _9 = new JPanel();
        _9.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        _8.setLeftComponent(_9);
        final JLabel _10;
        _10 = new JLabel();
        operationsLabel = _10;
        _10.setText("Operations");
        _9.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 1, 7, 0, null, null, null));
        final JScrollPane _11;
        _11 = new JScrollPane();
        treeScrollPane = _11;
        _11.setAutoscrolls(false);
        _9.add(_11, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 3, 7, 7, null, null, null));
        final JTree _12;
        _12 = new JTree();
        operationsTree = _12;
        _12.setRootVisible(false);
        _12.setShowsRootHandles(false);
        _11.setViewportView(_12);
        final JScrollPane _13;
        _13 = new JScrollPane();
        _8.setRightComponent(_13);
        final JPanel _14;
        _14 = new JPanel();
        _14.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        _13.setViewportView(_14);
        final JPanel _15;
        _15 = new JPanel();
        messageViewerToolbarPanel = _15;
        _15.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        _14.add(_15, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 1, 7, 1, null, null, null));
        final JPanel _16;
        _16 = new JPanel();
        messageViewerPanel = _16;
        _16.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        _14.add(_16, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 3, 7, 7, null, null, null));
        final JPanel _17;
        _17 = new JPanel();
        _17.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(3, 7, new Insets(5, 0, 5, 0), -1, -1));
        _1.add(_17, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, 0, 3, 7, 3, null, null, null));
        final JButton _18;
        _18 = new JButton();
        okButton = _18;
        _18.setText("OK");
        _17.add(_18, new com.intellij.uiDesigner.core.GridConstraints(2, 4, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _19;
        _19 = new com.intellij.uiDesigner.core.Spacer();
        _17.add(_19, new com.intellij.uiDesigner.core.GridConstraints(2, 3, 1, 1, 0, 1, 6, 1, null, null, null));
        final JButton _20;
        _20 = new JButton();
        helpButton = _20;
        _20.setText("Help");
        _17.add(_20, new com.intellij.uiDesigner.core.GridConstraints(2, 6, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _21;
        _21 = new JButton();
        cancelButton = _21;
        _21.setText("Cancel");
        _17.add(_21, new com.intellij.uiDesigner.core.GridConstraints(2, 5, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _22;
        _22 = new com.intellij.uiDesigner.core.Spacer();
        _17.add(_22, new com.intellij.uiDesigner.core.GridConstraints(0, 3, 1, 1, 0, 2, 1, 6, null, null, null));
        final JButton _23;
        _23 = new JButton();
        addButton = _23;
        _23.setText("Add");
        _17.add(_23, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _24;
        _24 = new JButton();
        removeButton = _24;
        _24.setText("Remove");
        _17.add(_24, new com.intellij.uiDesigner.core.GridConstraints(0, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final JScrollPane _25;
        _25 = new JScrollPane();
        tableScrollPane = _25;
        _17.add(_25, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 4, 0, 3, 7, 7, new Dimension(-1, 150), null, null));
        final JTable _26;
        _26 = new JTable();
        securedItemsTable = _26;
        _26.setAutoResizeMode(2);
        _26.setPreferredScrollableViewportSize(new Dimension(-1, -1));
        _25.setViewportView(_26);
        final JPanel _27;
        _27 = new JPanel();
        _27.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 5, 0, 0), -1, -1));
        _1.add(_27, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 7, 1, null, null, null));
        _27.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        final JLabel _28;
        _28 = new JLabel();
        _28.setText("Signature and encryption properties");
        _27.add(_28, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
    }


}
