package com.l7tech.console.panels;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.wsdl.BindingInfo;
import com.l7tech.common.wsdl.BindingOperationInfo;
import com.l7tech.common.wsdl.MimePartInfo;
import com.l7tech.common.xml.Wsdl;
import com.l7tech.common.xml.SoapMessageGenerator;
import com.l7tech.common.xml.XpathEvaluator;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.tree.ServiceNode;
import com.l7tech.console.util.SortedSingleColumnTableModel;
import com.l7tech.console.table.MimePartsTable;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.AssertionPath;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestSwAAssertion;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.Spacer;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.wsdl.Binding;
import javax.wsdl.BindingOperation;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.mime.MIMEContent;
import javax.wsdl.extensions.mime.MIMEMultipartRelated;
import javax.wsdl.extensions.mime.MIMEPart;
import javax.wsdl.extensions.soap.SOAPBody;
import javax.xml.soap.SOAPException;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.apache.axis.message.SOAPBodyElement;


/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertionDialog extends JDialog {
    static final Logger log = Logger.getLogger(RequestSwAAssertionDialog.class.getName());
    private RequestSwAAssertion assertion;
    private JButton cancelButton;
    private JButton okButton;
    private JPanel mainPanel;
    private JComboBox bindingsListComboxBox;
    private JScrollPane operationsScrollPane;
    private JScrollPane multipartScrollPane;
    private ServiceNode serviceNode;
    private EventListenerList listenerList = new EventListenerList();
    private Map bindings = new HashMap();
    private SortedSingleColumnTableModel bindingOperationsTableModel = null;
    private JTable bindingOperationsTable = null;
    private MimePartsTable mimePartsTable = null;
    private Wsdl serviceWsdl = null;
    private SoapMessageGenerator.Message[] soapMessages;
    private Map namespaces = new HashMap();
    SoapMessageGenerator.Message soapRequest;

    private static ResourceBundle resources = ResourceBundle.getBundle("com.l7tech.console.resources.RequestSwAPropertiesDialog", Locale.getDefault());
    private static Logger logger = Logger.getLogger(RequestSwAAssertionDialog.class.getName());

    public RequestSwAAssertionDialog(JFrame parent, RequestSwAAssertion assertion, ServiceNode sn) {
        super(parent, resources.getString("window.title"), true);
        this.assertion = assertion;
        this.serviceNode = sn;

        initialize();
        populateData();
        pack();
        Utilities.centerOnScreen(this);
    }

    private void initialize() {
        Container p = getContentPane();
        p.setLayout(new BorderLayout());
        p.add(mainPanel, BorderLayout.CENTER);

        bindingOperationsTable = getBindingOperationsTable();
        bindingOperationsTable.setModel(getBindingOperationsTableModel());
        bindingOperationsTable.setShowHorizontalLines(false);
        bindingOperationsTable.setShowVerticalLines(false);
        bindingOperationsTable.setDefaultRenderer(Object.class, bindingOperationsTableRenderer);
        operationsScrollPane.getViewport().setBackground(bindingOperationsTable.getBackground());
        operationsScrollPane.setViewportView(bindingOperationsTable);

        bindingsListComboxBox.setRenderer(bindingListRender);
        bindingsListComboxBox.addItemListener(new ItemListener() {
            /**
             * Invoked when an item has been selected or deselected.
             * The code written for this method performs the operations
             * that need to occur when an item is selected (or deselected).
             */
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    populateBindingOperationsData((BindingInfo) e.getItem());
                }
            }
        });

        multipartScrollPane.setViewportView(getMimePartsTable());
        multipartScrollPane.getViewport().setBackground(Color.white);

        okButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                fireEventAssertionChanged(assertion);
                dispose();
            }
        });

        cancelButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                dispose();
            }
        });

        Utilities.equalizeButtonSizes(new JButton[]{cancelButton, okButton});

    }

    private void initializeXPath() {

        if(bindings == null) throw new IllegalStateException("bindings is NULL");

        getServiceWsdl().setShowBindings(Wsdl.SOAP_BINDINGS);
        SoapMessageGenerator sg = new SoapMessageGenerator();
        try {
            soapMessages = sg.generateRequests(getServiceWsdl());

            //initializeBlankMessage(soapMessages[0]);
            for (int i = 0; i < soapMessages.length; i++) {
                soapRequest = soapMessages[i];

                String soapEnvLocalName = soapRequest.getSOAPMessage().getSOAPPart().getEnvelope().getElementName().getLocalName();
                String soapEnvNamePrefix = soapRequest.getSOAPMessage().getSOAPPart().getEnvelope().getElementName().getPrefix();
                String soapBodyLocalName = soapRequest.getSOAPMessage().getSOAPPart().getEnvelope().getBody().getElementName().getLocalName();
                String soapBodyNamePrefix = soapRequest.getSOAPMessage().getSOAPPart().getEnvelope().getBody().getElementName().getPrefix();

                if(soapBodyNamePrefix.length() == 0) {
                    soapBodyNamePrefix = soapEnvNamePrefix;
                }
                Iterator soapBodyElements = soapRequest.getSOAPMessage().getSOAPPart().getEnvelope().getBody().getChildElements();

                // get the first element
                SOAPBodyElement operation = (SOAPBodyElement) soapBodyElements.next();
                String operationName = operation.getName();
                String operationPrefix = operation.getPrefix();

                Iterator bindingsItr = bindings.keySet().iterator();

                BindingInfo binding = null;
                while(bindingsItr.hasNext()) {
                    String bindingName = (String) bindingsItr.next();
                    if(bindingName.equals(soapRequest.getBinding())) {
                        binding = (BindingInfo) bindings.get(bindingName);
                        break;
                    }
                }

                BindingOperationInfo bo = null;
                if(binding != null) {
                    Iterator boItr = binding.getBindingOperations().keySet().iterator();
                    while(boItr.hasNext()) {
                        String boName = (String) boItr.next();
                        if(boName.equals(soapRequest.getOperation())) {
                            bo = (BindingOperationInfo) binding.getBindingOperations().get(boName);
                            break;
                        }
                    }
                }

                String xpathExpression = "/" + soapEnvNamePrefix +
                        ":" + soapEnvLocalName +
                        "/" + soapBodyNamePrefix +
                        ":" + soapBodyLocalName +
                        "/" + operationPrefix +
                        ":" + operationName;

                if(bo != null) {
                    bo.setXpath(xpathExpression);
                }
                
                logger.finest("Xpath for the operation " + "\"" + soapRequest.getOperation() + "\" is " + xpathExpression);

                namespaces.putAll(XpathEvaluator.getNamespaces(soapRequest.getSOAPMessage()));
            }
        } catch (SOAPException e) {
            logger.log(Level.WARNING, "Caught SAXException when retrieving xml document from the generated request", e);
        }
    }

    private Wsdl getServiceWsdl() {
        if(serviceWsdl == null) {
            try {
                serviceWsdl = serviceNode.getPublishedService().parsedWsdl();
            } catch (Exception e) {
                throw new RuntimeException("Unable to parse the service WSDL " + serviceNode.getName(), e);
            }
        }
        return serviceWsdl;
    }

    private void populateData() {

        if (assertion.getBindings().size() == 0) {
            // this is the first time, load data from WSDL
            loadMIMEPartsInfoFromWSDL();
            initializeXPath();
            populateData(bindings);
            assertion.setBindings(bindings);
            assertion.setNamespaceMap(namespaces);
        } else {
            // populate the data from the assertion
            populateData(assertion.getBindings());
        }
    }

    private void populateData(Map bindings) {

        // populate the binding operation table
        if(bindings == null) throw new RuntimeException("bindings map is NULL");

        boolean firstEntry = true;
        Iterator bindingsItr = bindings.keySet().iterator();
        while (bindingsItr.hasNext()) {
            String bindingName = (String) bindingsItr.next();
            BindingInfo binding = (BindingInfo) bindings.get(bindingName);

            // add the entry the the binding list
            bindingsList.add(binding);
            bindingsListComboxBox.addItem(binding);

            if(firstEntry) {
                populateBindingOperationsData(binding);
                firstEntry = false;
            }
        }

    }

    private void saveData(BindingOperationInfo bo) {
        if(bo == null) throw new RuntimeException("bindingOperation is NULL");

        Vector dataSet = getMimePartsTable().getTableSorter().getAllData();
        for (int i = 0; i < dataSet.size(); i++) {
            MimePartInfo mimePart = (MimePartInfo) dataSet.elementAt(i);
            MimePartInfo mimePartFound = (MimePartInfo) bo.getMultipart().get(mimePart.getName());
            if(mimePartFound != null) {
                mimePartFound.setMaxLength(mimePart.getMaxLength());
            }            
        }
    }

    private void populateBindingOperationsData(BindingInfo binding) {
        if(binding == null) throw new RuntimeException("binding info is NULL");

        // save the mime part data to the assertion before populating the new data
        int selectedOperation = getBindingOperationsTable().getSelectedRow();
        if(selectedOperation >= 0) {
            BindingOperationInfo bo = (BindingOperationInfo) getBindingOperationsTableModel().getValueAt(selectedOperation, 0);
            saveData(bo);
        }

        // clear the operation table
        getBindingOperationsTableModel().removeRows(getBindingOperationsTableModel().getDataSet());
        getBindingOperationsTableModel().clearDataSet();

        Iterator bindingOperationsItr = binding.getBindingOperations().keySet().iterator();
        while(bindingOperationsItr.hasNext()) {
            String bindingOperationName = (String) bindingOperationsItr.next();
            // add the entry to the binding operation table
            BindingOperationInfo bo = (BindingOperationInfo) binding.getBindingOperations().get(bindingOperationName);
            getBindingOperationsTableModel().addRow(bo);
        }
        getBindingOperationsTableModel().fireTableDataChanged();

        // show the mime parts of the first operation
        if(getBindingOperationsTableModel().getRowCount() > 0) {
            getBindingOperationsTable().setRowSelectionInterval(0,0);
            getBindingOperationsTableModel().fireTableCellUpdated(0,0);
            populateMimePartsData(((BindingOperationInfo) getBindingOperationsTableModel().getDataSet()[0]).getMultipart());
        }
    }

    private void populateMimePartsData(Map mimeParts) {
        if(mimeParts == null) throw new RuntimeException("mimeParts is NULL");

        // clear the MIME part table
        getMimePartsTable().removeAll();

        Iterator parts = mimeParts.keySet().iterator();
        Vector pv = new Vector();
        while (parts.hasNext()) {
            String partName = (String) parts.next();
            pv.add(mimeParts.get(partName));

        }
        getMimePartsTable().getTableSorter().setData(pv);
    }

    private void loadMIMEPartsInfoFromWSDL() {
        try {
            Wsdl parsedWsdl = serviceNode.getPublishedService().parsedWsdl();

            Collection bindingList = parsedWsdl.getBindings();

            // for each binding in WSDL
            for (Iterator iterator = bindingList.iterator(); iterator.hasNext();) {
                Binding binding = (Binding) iterator.next();

                //todo: should filter out non-SOAP binding
                Collection boList = binding.getBindingOperations();
                HashMap operations = new HashMap();
                
                // for each operation in WSDL
                for (Iterator iterator1 = boList.iterator(); iterator1.hasNext();) {
                    BindingOperation bo = (BindingOperation) iterator1.next();

                    HashMap partList = new HashMap();
                    Collection elements = parsedWsdl.getInputParameters(bo);

                    // for each input parameter of the operation in WSDL
                    for (Iterator itr = elements.iterator(); itr.hasNext();) {

                        Object o = (Object) itr.next();
                        if (o instanceof MIMEMultipartRelated) {

                            MIMEMultipartRelated multipart = (MIMEMultipartRelated) o;

                            List parts = multipart.getMIMEParts();

                            // for each MIME part of the input parameter of the operation in WSDL
                            for (Iterator partsItr = parts.iterator(); partsItr.hasNext();) {

                                MIMEPart mimePart = (MIMEPart) partsItr.next();
                                Collection mimePartSubElements = parsedWsdl.getMimePartSubElements(mimePart);

                                // for each extensible part of the MIME part of the input parameter of the operation in WSDL
                                for (Iterator subElementItr = mimePartSubElements.iterator(); subElementItr.hasNext();) {
                                    Object subElement = (Object) subElementItr.next();

                                    if (subElement instanceof MIMEContent) {
                                        MIMEContent mimeContent = (MIMEContent) subElement;

                                        //concat the content type if the part alreay exists
                                        MimePartInfo retrievedPart = (MimePartInfo) partList.get(mimeContent.getPart());
                                        if (retrievedPart != null) {
                                            retrievedPart.addContentType(mimeContent.getType());
                                        } else {
                                            MimePartInfo newPart = new MimePartInfo(mimeContent.getPart(), mimeContent.getType());

                                            // default length 1000 Kbytes
                                            newPart.setMaxLength(1000);

                                            // add the new part
                                            partList.put(mimeContent.getPart(), newPart);
                                        }

                                        // add the new part
                                    } else if (subElement instanceof SOAPBody) {
                                        // don't care about soapPart for now
                                        //SOAPBody soapBody = (SOAPBody) subElement;
                                    }
                                }
                            }
                        }
                        // create BindingOperationInfo
                        BindingOperationInfo operation = new BindingOperationInfo(bo.getOperation().getName(), partList);
                        operations.put(bo.getOperation().getName(), operation);
                    }
                }
                BindingInfo bindingInfo = new BindingInfo(binding.getQName().getLocalPart(), operations);
                bindings.put(bindingInfo.getBindingName(), bindingInfo);
            }

        } catch (FindException e) {
            logger.warning("The service not found: " + serviceNode.getName());
        } catch (RemoteException re) {
            logger.severe("Remote exception");
        } catch (WSDLException e) {
            logger.warning("Unable to retrieve parse the WSDL of the service " + serviceNode.getName());
        }
    }

    /**
     * add the PolicyListener
     *
     * @param listener the PolicyListener
     */
    public void addPolicyListener(PolicyListener listener) {
        listenerList.add(PolicyListener.class, listener);
    }

    /**
     * remove the the PolicyListener
     *
     * @param listener the PolicyListener
     */
    public void removePolicyListener(PolicyListener listener) {
        listenerList.remove(PolicyListener.class, listener);
    }

    /**
     * notfy the listeners
     *
     * @param a the assertion
     */
    private void fireEventAssertionChanged(final Assertion a) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int[] indices = new int[a.getParent().getChildren().indexOf(a)];
                PolicyEvent event = new
                        PolicyEvent(this, new AssertionPath(a.getPath()), indices, new Assertion[]{a});
                EventListener[] listeners = listenerList.getListeners(PolicyListener.class);
                for (int i = 0; i < listeners.length; i++) {
                    ((PolicyListener) listeners[i]).assertionsChanged(event);
                }
            }
        });
    }

    private MimePartsTable getMimePartsTable() {

        if(mimePartsTable != null) return mimePartsTable;

        mimePartsTable = new MimePartsTable();
        return mimePartsTable;
    }

    private JTable getBindingOperationsTable() {
        if(bindingOperationsTable != null) {
            return bindingOperationsTable;
        }

        bindingOperationsTable = new JTable();
        bindingOperationsTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        bindingOperationsTable.getSelectionModel().
                addListSelectionListener(new ListSelectionListener() {
                    /**
                     * Called whenever the value of the selection changes.
                     * @param e the event that characterizes the change.
                     */
                    public void valueChanged(ListSelectionEvent e) {
                        int row = bindingOperationsTable.getSelectedRow();
                        if(row >= 0) {
                            BindingOperationInfo boInfo = (BindingOperationInfo) bindingOperationsTable.getModel().getValueAt(row, 0);
                            populateMimePartsData(boInfo.getMultipart());
                        }
                    }
                });

        return bindingOperationsTable;
    }

    private SoapMessageGenerator.Message forOperation(BindingOperation bop) {
        String opName = bop.getOperation().getName();
        Binding binding = serviceWsdl.getBinding(bop);
        if (binding == null) {
            throw new IllegalArgumentException("Bindiong operation without binding " + opName);
        }
        String bindingName = binding.getQName().getLocalPart();

        for (int i = 0; i < soapMessages.length; i++) {
            SoapMessageGenerator.Message soapRequest = soapMessages[i];
            if (opName.equals(soapRequest.getOperation()) &&
                    bindingName.equals(soapRequest.getBinding())) {
                return soapRequest;
            }
        }
        return null;
    }

    /**
     * @return the table model representing the binding operations specified in WSDL
     */
    private SortedSingleColumnTableModel getBindingOperationsTableModel() {
        if (bindingOperationsTableModel != null)
            return bindingOperationsTableModel;

        bindingOperationsTableModel = new SortedSingleColumnTableModel(new Comparator() {
            public int compare(Object o1, Object o2) {
                BindingOperationInfo e1 = (BindingOperationInfo)o1;
                BindingOperationInfo e2 = (BindingOperationInfo)o2;

                return e1.getName().compareToIgnoreCase(e2.getName());
            }

            public boolean isCellEditable(int row, int col) {
                return false;
            }
        });

        // add a new column without a column title
        bindingOperationsTableModel.addColumn("");
        return bindingOperationsTableModel;
    }

    private final ListCellRenderer bindingListRender = new DefaultListCellRenderer() {
        public Component getListCellRendererComponent(
                JList list,
                Object value,
                int index,
                boolean isSelected,
                boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            }
            else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            BindingInfo p = (BindingInfo)value;
            setText(p.getBindingName());
            setToolTipText(null);

            return this;
        }
    };

    private final TableCellRenderer bindingOperationsTableRenderer = new DefaultTableCellRenderer() {
        /* This is the only method defined by ListCellRenderer.  We just
        * reconfigure the Jlabel each time we're called.
        */
        public Component
                getTableCellRendererComponent(JTable table,
                                              Object value,
                                              boolean isSelected,
                                              boolean hasFocus,
                                              int row, int column) {
            if (isSelected) {
                this.setBackground(table.getSelectionBackground());
                this.setForeground(table.getSelectionForeground());
            } else {
                this.setBackground(table.getBackground());
                this.setForeground(table.getForeground());
            }

            this.setFont(new Font("Dialog", Font.PLAIN, 12));
            BindingOperationInfo p = (BindingOperationInfo)value;
            setText(p.getName());
            setToolTipText(null);
            return this;
        }
    };

    private final TableCellRenderer inputParametersTableRenderer = new DefaultTableCellRenderer() {
        /* This is the only method defined by ListCellRenderer.  We just
        * reconfigure the Jlabel each time we're called.
        */
        public Component
                getTableCellRendererComponent(JTable table,
                                              Object value,
                                              boolean iss,
                                              boolean hasFocus,
                                              int row, int column) {
            if (!table.isEnabled()) {
                this.setEnabled(false);
            } else {
                this.setEnabled(true);
                if (iss) {
                    this.setBackground(table.getSelectionBackground());
                    this.setForeground(table.getSelectionForeground());
                } else {
                    this.setBackground(table.getBackground());
                    this.setForeground(table.getForeground());
                }
            }

            this.setFont(new Font("Dialog", Font.PLAIN, 12));
            MimePartInfo p = (MimePartInfo)value;
            setText(p.getName());
            setToolTipText(null);
            return this;
        }
    };

    private Set bindingsList = new TreeSet(new Comparator() {
        public int compare(Object o1, Object o2) {
            BindingInfo p1 = (BindingInfo)o1;
            BindingInfo p2 = (BindingInfo)o2;
            return p1.getBindingName().compareToIgnoreCase(p2.getBindingName());
        }
    });

    {
// GUI initializer generated by IntelliJ IDEA GUI Designer
// !!! IMPORTANT !!!
// DO NOT EDIT OR ADD ANY CODE HERE!
        $$$setupUI$$$();
    }

    /**
     * Method generated by IntelliJ IDEA GUI Designer
     * !!! IMPORTANT !!!
     * DO NOT edit this method OR call it in your code!
     */
    private void $$$setupUI$$$() {
        mainPanel = new JPanel();
        mainPanel.setLayout(new GridLayoutManager(2, 1, new Insets(10, 10, 10, 10), -1, -1));
        final JPanel panel1 = new JPanel();
        panel1.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel2 = new JPanel();
        panel2.setLayout(new GridLayoutManager(1, 2, new Insets(0, 0, 0, 0), -1, -1));
        panel1.add(panel2, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JPanel panel3 = new JPanel();
        panel3.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JLabel label1 = new JLabel();
        label1.setText("Operations:");
        panel3.add(label1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        operationsScrollPane = new JScrollPane();
        panel3.add(operationsScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(150, -1), new Dimension(150, 200), new Dimension(200, -1)));
        final JPanel panel4 = new JPanel();
        panel4.setLayout(new GridLayoutManager(2, 1, new Insets(0, 0, 0, 0), -1, -1));
        panel2.add(panel4, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        final JLabel label2 = new JLabel();
        label2.setText("Input parameters (only those bound to an attachment are shown):");
        panel4.add(label2, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        multipartScrollPane = new JScrollPane();
        panel4.add(multipartScrollPane, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_WANT_GROW, new Dimension(500, -1), new Dimension(500, 200), null));
        final JPanel panel5 = new JPanel();
        panel5.setLayout(new GridLayoutManager(2, 1, new Insets(10, 0, 10, 0), -1, -1));
        panel1.add(panel5, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final JLabel label3 = new JLabel();
        label3.setText("Bindings:");
        panel5.add(label3, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_NONE, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        bindingsListComboxBox = new JComboBox();
        panel5.add(bindingsListComboxBox, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_WEST, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_FIXED, GridConstraints.SIZEPOLICY_FIXED, null, new Dimension(200, -1), new Dimension(300, -1)));
        final JPanel panel6 = new JPanel();
        panel6.setLayout(new GridLayoutManager(1, 3, new Insets(0, 0, 0, 0), -1, -1));
        mainPanel.add(panel6, new GridConstraints(1, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_BOTH, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, null, null, null));
        cancelButton = new JButton();
        cancelButton.setText("Canel");
        panel6.add(cancelButton, new GridConstraints(0, 2, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
        final Spacer spacer1 = new Spacer();
        panel6.add(spacer1, new GridConstraints(0, 0, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_WANT_GROW, 1, null, null, null));
        okButton = new JButton();
        okButton.setText("OK");
        panel6.add(okButton, new GridConstraints(0, 1, 1, 1, GridConstraints.ANCHOR_CENTER, GridConstraints.FILL_HORIZONTAL, GridConstraints.SIZEPOLICY_CAN_SHRINK | GridConstraints.SIZEPOLICY_CAN_GROW, GridConstraints.SIZEPOLICY_FIXED, null, null, null));
    }
}
