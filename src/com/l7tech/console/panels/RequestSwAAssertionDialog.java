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

    private void initializeXPath(Map bindings) {
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
                

                System.out.println("Xpath for the operation " + "\"" + soapRequest.getOperation() + "\" is " +
                        "/" + soapEnvNamePrefix +
                        ":" + soapEnvLocalName +
                        "/" + soapBodyNamePrefix +
                        ":" + soapBodyLocalName +
                        "/" + operationPrefix +
                        ":" + operationName);

                namespaces.putAll(XpathEvaluator.getNamespaces(soapRequest.getSOAPMessage()));
            }
        } catch (SOAPException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
            initializeXPath(bindings);
            populateData(bindings);
            assertion.setBindings(bindings);
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

    public void populateMimePartsData(Map mimeParts) {
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

            // for each binding
            for (Iterator iterator = bindingList.iterator(); iterator.hasNext();) {
                Binding binding = (Binding) iterator.next();

                //todo: should filter out non-SOAP binding
                Collection boList = binding.getBindingOperations();
                HashMap operations = new HashMap();
                
                // for each operation
                for (Iterator iterator1 = boList.iterator(); iterator1.hasNext();) {
                    BindingOperation bo = (BindingOperation) iterator1.next();

                    HashMap partList = new HashMap();
                    Collection elements = parsedWsdl.getInputParameters(bo);

                    for (Iterator itr = elements.iterator(); itr.hasNext();) {

                        Object o = (Object) itr.next();
                        if (o instanceof MIMEMultipartRelated) {

                            MIMEMultipartRelated multipart = (MIMEMultipartRelated) o;

                            List parts = multipart.getMIMEParts();
                            for (Iterator partsItr = parts.iterator(); partsItr.hasNext();) {

                                MIMEPart mimePart = (MIMEPart) partsItr.next();
                                Collection mimePartSubElements = parsedWsdl.getMimePartSubElements(mimePart);

                                for (Iterator subElementItr = mimePartSubElements.iterator(); subElementItr.hasNext();) {
                                    Object subElement = (Object) subElementItr.next();

                                    if (subElement instanceof MIMEContent) {
                                        MIMEContent mimeContent = (MIMEContent) subElement;
                                        MimePartInfo part = new MimePartInfo(mimeContent.getPart(), mimeContent.getType());

                                        //concat the content type if the part alreay exists
                                        MimePartInfo partInfo = (MimePartInfo) partList.get(mimeContent.getPart());
                                        if (partInfo != null) {
                                            partInfo.setContentType(partInfo.getContentType() + mimeContent.getPart());
                                        } else {
                                            partList.put(mimeContent.getPart(), part);
                                        }
                                    } else if (subElement instanceof SOAPBody) {
                                        // don't care about soapPart for now
                                        //SOAPBody soapBody = (SOAPBody) subElement;
                                    }
                                }
                            }
                        }
                        // create BindingOperationInfo
                        BindingOperationInfo operation = new BindingOperationInfo(bo.getOperation().getName(), partList);
                      //todo:  operation.setXpath();
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

}
