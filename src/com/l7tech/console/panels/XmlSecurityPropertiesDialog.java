package com.l7tech.console.panels;

import com.l7tech.common.xml.SoapRequestGenerator;
import com.l7tech.common.xml.SoapRequestGenerator.SOAPRequest;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.common.xml.XpathExpression;
import com.l7tech.console.table.SecuredMessagePartsTableModel;
import com.l7tech.console.table.SecuredMessagePartsTableModel.SecuredMessagePart;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.XmlRequestSecurityTreeNode;
import com.l7tech.console.tree.policy.XmlResponseSecurityTreeNode;
import com.l7tech.console.tree.policy.XmlSecurityTreeNode;
import com.l7tech.console.tree.wsdl.BindingOperationTreeNode;
import com.l7tech.console.tree.wsdl.WsdlTreeNode;
import com.l7tech.console.util.Registry;
import com.l7tech.console.xmlviewer.ExchangerDocument;
import com.l7tech.console.xmlviewer.Viewer;
import com.l7tech.console.xmlviewer.ViewerToolBar;
import com.l7tech.console.xmlviewer.properties.ConfigurationProperties;
import com.l7tech.console.xmlviewer.util.DocumentUtilities;
import com.l7tech.console.action.Actions;
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
    private JTree wsdlMessagesTree;
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
    private SOAPRequest[] soapRequests;
    private static final String SOAP_BODY = "/soapenv:Envelope/soapenv:Body";
    private static final String SOAP_ENVELOPE = "/soapenv:Envelope";
    private Map namespaces = new HashMap();
    private Viewer messageViewer;
    private ViewerToolBar messageToolbar;
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
            SoapRequestGenerator sg = new SoapRequestGenerator();
            soapRequests = sg.generate(serviceWsdl);
            for (int i = 0; i < soapRequests.length; i++) {
                SOAPRequest soapRequest = soapRequests[i];
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
                    wsdlMessagesTree.setEnabled(!selected);
                    messageViewer.setViewerEnabled(!selected);
                    messageToolbar.setToolbarEnabled(!selected);
                    securedMessagePartsTableModel = memoTableModelEntireMessage;
                    if (securedMessagePartsTableModel == null) {
                        securedMessagePartsTableModel = new SecuredMessagePartsTableModel();
                        SecuredMessagePart sp = new SecuredMessagePart();
                        sp.setOperation("*");
                        sp.setXpathExpression(SOAP_ENVELOPE);
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
                    wsdlMessagesTree.setEnabled(selected);
                    messageViewer.setViewerEnabled(selected);
                    messageToolbar.setToolbarEnabled(selected);
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
                java.util.List elements = new ArrayList();
                Iterator it = securedMessagePartsTableModel.getSecuredMessageParts().iterator();
                for (; it.hasNext();) {
                    SecuredMessagePart sp = (SecuredMessagePartsTableModel.SecuredMessagePart)it.next();
                    elements.add(toElementSecurity(sp));
                }
                xmlSecAssertion.setElements((ElementSecurity[])elements.toArray(new ElementSecurity[]{}));

                XmlSecurityPropertiesDialog.this.dispose();
            }
        });

        addButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                TreePath path = wsdlMessagesTree.getSelectionPath();
                if (path == null) {
                    throw new IllegalStateException("No message/part selected (path is null)");
                }
                WsdlTreeNode node = (WsdlTreeNode)path.getLastPathComponent();

                if (node instanceof BindingOperationTreeNode) {
                    BindingOperationTreeNode bn = (BindingOperationTreeNode)node;
                    BindingOperation bo = bn.getOperation();
                    String xpathExpression = SOAP_BODY;
                    try {
                        NameSpace ns = getOperationNamespace(bo.getName());
                        String nameSpacePrefix = "";
                        if (ns != null) {
                            nameSpacePrefix = ns.m_prefix + ":";
                        }
                        xpathExpression = messageToolbar.getXPath();
                        SecuredMessagePart p = new SecuredMessagePart();
                        p.setOperation(bn.getName());
                        p.setXpathExpression(xpathExpression);
                        addSecuredPart(p);
                    } catch (SOAPException e1) {
                        throw new RuntimeException(e1);
                    }
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
            Collection collection = wsdl.getBindings();
            WsdlTreeNode.Options wo = new WsdlTreeNode.Options();
            ;
            for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
                Binding b = (Binding)iterator.next();
                java.util.List operations = b.getBindingOperations();
                int index = 0;
                for (Iterator itop = operations.iterator(); itop.hasNext();) {
                    BindingOperation bo = (BindingOperation)itop.next();
                    treeModel.insertNodeInto(new BindingOperationTreeNode(bo, wo), root, index++);
                }
                break;
            }
            wsdlMessagesTree.setModel(treeModel);
            wsdlMessagesTree.setRootVisible(false);
            wsdlMessagesTree.setShowsRootHandles(true);
            wsdlMessagesTree.setCellRenderer(wsdlTreeRenderer);
            wsdlMessagesTree.getSelectionModel().addTreeSelectionListener(treeSelectionListener);
            final ListSelectionModel selectionModel = securedItemsTable.getSelectionModel();
            selectionModel.addListSelectionListener(tableSelectionListener);
            selectionModel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            final ElementSecurity[] elements = xmlSecAssertion.getElements();
            final boolean envelopeAllOperations = isEnvelopeAllOperations(elements);
            initializeSoapMessageViewer();

            entireMessage.setSelected(envelopeAllOperations);
            messageParts.setSelected(!envelopeAllOperations);
            messageViewer.setViewerEnabled(!envelopeAllOperations);
            messageToolbar.setToolbarEnabled(!envelopeAllOperations);

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

    private void initializeSoapMessageViewer() throws IOException, SAXParseException, DocumentException {
        ConfigurationProperties cp = new ConfigurationProperties();
        exchangerDocument = asExchangerDocument("<empty/>");
        messageViewer = new Viewer(cp.getViewer(), exchangerDocument, false);
        messageToolbar = new ViewerToolBar(cp.getViewer(), messageViewer);
        com.intellij.uiDesigner.core.GridConstraints gridConstraints = new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 7, 7, null, null, null);
        messageViewerToolbarPanel.add(messageToolbar, gridConstraints);
        com.intellij.uiDesigner.core.GridConstraints gridConstraints2 = new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 7, 7, null, null, null);
        messageViewerPanel.add(messageViewer, gridConstraints2);
    }

    private static ExchangerDocument asExchangerDocument(String content)
      throws IOException, DocumentException, SAXParseException {
        URL url = asTempFileURL(content);
        ExchangerDocument exchangerDocument = new ExchangerDocument(url, false);
        exchangerDocument.load();
        return exchangerDocument;
    }

    private static URL asTempFileURL(String content)
      throws IOException, DocumentException {
        final File file = File.createTempFile("Temp", "xml");
        Document doc = DocumentUtilities.createReader(false).read(new StringReader(content));
        DocumentUtilities.writeDocument(doc, file.toURL());
        file.deleteOnExit();
        return file.toURL();
    }

    private ElementSecurity toElementSecurity(SecuredMessagePart sp) {
        ElementSecurity es = new ElementSecurity();
        es.setEncryption(sp.isEncrypt());
        es.setCipher(sp.getAlgorithm());
        es.setKeyLength(sp.getKeyLength());
        XpathExpression xe = new XpathExpression(sp.getXpathExpression(), namespaces);
        es.setXpathExpression(xe);
        es.setOperation(sp.getOperation());
        return es;
    }

    private SecuredMessagePart toSecureMessagePart(ElementSecurity es) {
        SecuredMessagePart sp = new SecuredMessagePart();
        sp.setAlgorithm(es.getCipher());
        sp.setXpathExpression(es.getXpathExpression().getExpression());
        sp.setEncrypt(es.isEncryption());
        sp.setKeyLength(es.getKeyLength());
        sp.setOperation(es.getOperation());
        return sp;
    }

    private boolean isEnvelopeAllOperations(ElementSecurity[] es) {
        return (es.length == 1 && SOAP_ENVELOPE.equals(es[0].getXpathExpression().getExpression()));
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
                                                 p.getOperation() == null ? "" : p.getOperation()};
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
                  BindingOperationTreeNode boperation = (BindingOperationTreeNode)path.getLastPathComponent();
                  SOAPRequest sreq = forOperation(boperation.getName());
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
            if (securedItemsTable.getSelectedRow() == -1) {
                removeButton.setEnabled(false);
                return;
            }
            removeButton.setEnabled(true);
        }
    };

    private SOAPRequest forOperation(String opName) {
        for (int i = 0; i < soapRequests.length; i++) {
            SOAPRequest soapRequest = soapRequests[i];
            if (opName.equals(soapRequest.getSOAPOperation())) {
                return soapRequest;
            }
        }
        return null;
    }

    private NameSpace getOperationNamespace(String opName) throws SOAPException {
        SOAPRequest req = forOperation(opName);
        if (req == null) return null;
        Iterator it = req.getSOAPMessage().getSOAPPart().getEnvelope().getBody().getChildElements();
        if (!it.hasNext()) return null;
        SOAPElement se = (SOAPElement)it.next();
        final Name elementName = se.getElementName();
        if (!elementName.getLocalName().equals(opName)) return null;
        NameSpace ns = new NameSpace(elementName.getPrefix(), elementName.getURI());
        return ns;
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
        _7.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(10, 0, 0, 0), -1, -1));
        _1.add(_7, new com.intellij.uiDesigner.core.GridConstraints(3, 0, 1, 1, 0, 3, 7, 7, null, null, null));
        final JSplitPane _8;
        _8 = new JSplitPane();
        _8.setDividerLocation(350);
        _8.setDividerSize(4);
        _7.add(_8, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 3, 7, 7, null, null, null));
        final JPanel _9;
        _9 = new JPanel();
        _9.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        _8.setRightComponent(_9);
        final JScrollPane _10;
        _10 = new JScrollPane();
        tableScrollPane = _10;
        _9.add(_10, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 3, 3, 7, null, null, null));
        final JTable _11;
        _11 = new JTable();
        securedItemsTable = _11;
        _11.setAutoResizeMode(2);
        _11.setPreferredScrollableViewportSize(new Dimension(-1, -1));
        _10.setViewportView(_11);
        final JPanel _12;
        _12 = new JPanel();
        _12.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(4, 2, new Insets(0, 0, 0, 0), -1, -1));
        _9.add(_12, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 1, 7, null, null, null));
        final JButton _13;
        _13 = new JButton();
        removeButton = _13;
        _13.setText("Remove");
        _12.add(_13, new com.intellij.uiDesigner.core.GridConstraints(2, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _14;
        _14 = new JButton();
        addButton = _14;
        _14.setText("Add");
        _12.add(_14, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _15;
        _15 = new com.intellij.uiDesigner.core.Spacer();
        _12.add(_15, new com.intellij.uiDesigner.core.GridConstraints(3, 1, 1, 1, 0, 2, 1, 6, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _16;
        _16 = new com.intellij.uiDesigner.core.Spacer();
        _12.add(_16, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 2, 1, 6, new Dimension(-1, 20), new Dimension(-1, 20), null));
        final com.intellij.uiDesigner.core.Spacer _17;
        _17 = new com.intellij.uiDesigner.core.Spacer();
        _12.add(_17, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 6, 1, new Dimension(10, -1), null, null));
        final JPanel _18;
        _18 = new JPanel();
        _18.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        _8.setLeftComponent(_18);
        final JSplitPane _19;
        _19 = new JSplitPane();
        _19.setDividerSize(4);
        _18.add(_19, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 3, 7, 3, null, new Dimension(200, 200), null));
        final JPanel _20;
        _20 = new JPanel();
        _20.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        _19.setLeftComponent(_20);
        final JLabel _21;
        _21 = new JLabel();
        operationsLabel = _21;
        _21.setText("Operations");
        _20.add(_21, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 1, 7, 0, null, null, null));
        final JScrollPane _22;
        _22 = new JScrollPane();
        treeScrollPane = _22;
        _22.setAutoscrolls(false);
        _20.add(_22, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 3, 7, 7, null, null, null));
        final JTree _23;
        _23 = new JTree();
        wsdlMessagesTree = _23;
        _23.setShowsRootHandles(false);
        _23.setRootVisible(false);
        _22.setViewportView(_23);
        final JScrollPane _24;
        _24 = new JScrollPane();
        _19.setRightComponent(_24);
        final JPanel _25;
        _25 = new JPanel();
        _25.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        _24.setViewportView(_25);
        final JPanel _26;
        _26 = new JPanel();
        messageViewerToolbarPanel = _26;
        _26.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        _25.add(_26, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 1, 7, 1, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _27;
        _27 = new com.intellij.uiDesigner.core.Spacer();
        _26.add(_27, new com.intellij.uiDesigner.core.GridConstraints(0, 1, 1, 1, 0, 1, 6, 1, null, null, null));
        final JPanel _28;
        _28 = new JPanel();
        messageViewerPanel = _28;
        _28.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 0, 0, 0), -1, -1));
        _25.add(_28, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 8, 3, 7, 7, null, null, null));
        final JPanel _29;
        _29 = new JPanel();
        _29.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(2, 4, new Insets(5, 0, 5, 0), -1, -1));
        _1.add(_29, new com.intellij.uiDesigner.core.GridConstraints(4, 0, 1, 1, 0, 3, 7, 3, null, null, null));
        final JButton _30;
        _30 = new JButton();
        okButton = _30;
        _30.setText("OK");
        _29.add(_30, new com.intellij.uiDesigner.core.GridConstraints(1, 1, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _31;
        _31 = new com.intellij.uiDesigner.core.Spacer();
        _29.add(_31, new com.intellij.uiDesigner.core.GridConstraints(1, 0, 1, 1, 0, 1, 6, 1, null, null, null));
        final JButton _32;
        _32 = new JButton();
        helpButton = _32;
        _32.setText("Help");
        _29.add(_32, new com.intellij.uiDesigner.core.GridConstraints(1, 3, 1, 1, 0, 1, 3, 0, null, null, null));
        final JButton _33;
        _33 = new JButton();
        cancelButton = _33;
        _33.setText("Cancel");
        _29.add(_33, new com.intellij.uiDesigner.core.GridConstraints(1, 2, 1, 1, 0, 1, 3, 0, null, null, null));
        final com.intellij.uiDesigner.core.Spacer _34;
        _34 = new com.intellij.uiDesigner.core.Spacer();
        _29.add(_34, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 2, 1, 6, null, null, null));
        final JPanel _35;
        _35 = new JPanel();
        _35.setLayout(new com.intellij.uiDesigner.core.GridLayoutManager(1, 1, new Insets(0, 5, 0, 0), -1, -1));
        _1.add(_35, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 0, 1, 7, 1, null, null, null));
        _35.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), null));
        final JLabel _36;
        _36 = new JLabel();
        _36.setText("Signature and encryption properties");
        _35.add(_36, new com.intellij.uiDesigner.core.GridConstraints(0, 0, 1, 1, 8, 0, 0, 0, null, null, null));
    }


}
